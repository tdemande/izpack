/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2004 Klaus Bartz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.data.ResourceManager;
import com.izforge.izpack.api.exception.InstallerException;
import com.izforge.izpack.api.exception.NativeLibException;
import com.izforge.izpack.api.exception.WrappedNativeLibException;
import com.izforge.izpack.api.handler.AbstractUIProgressHandler;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.api.substitutor.VariableSubstitutor;
import com.izforge.izpack.core.os.RegistryDefaultHandler;
import com.izforge.izpack.core.os.RegistryHandler;
import com.izforge.izpack.installer.data.UninstallData;
import com.izforge.izpack.installer.unpacker.IUnpacker;
import com.izforge.izpack.util.CleanupClient;
import com.izforge.izpack.util.Housekeeper;
import com.izforge.izpack.util.IoHelper;
import com.izforge.izpack.util.file.FileUtils;
import com.izforge.izpack.util.helper.SpecHelper;

/**
 * Installer custom action for handling registry entries on Windows. On Unix nothing will be done.
 * The actions which should be performed are defined in a resource file named "RegistrySpec.xml".
 * This resource should be declared in the installation definition file (install.xml), else an
 * exception will be raised during execution of this custom action. The related DTD is
 * appl/install/IzPack/resources/registry.dtd.
 *
 * @author Klaus Bartz
 */
public class RegistryInstallerListener extends NativeInstallerListener implements CleanupClient
{
    private static final Logger logger = Logger.getLogger(RegistryInstallerListener.class.getName());

    /**
     * The name of the XML file that specifies the registry entries.
     */
    private static final String SPEC_FILE_NAME = "RegistrySpec.xml";

    private static final String REG_KEY = "key";

    private static final String REG_VALUE = "value";

    private static final String REG_ROOT = "root";

    private static final String REG_BASENAME = "name";

    private static final String REG_KEYPATH = "keypath";

    private static final String REG_DWORD = "dword";

    private static final String REG_STRING = "string";

    private static final String REG_MULTI = "multi";

    private static final String REG_BIN = "bin";

    private static final String REG_DATA = "data";

    private static final String REG_OVERRIDE = "override";

    private static final String SAVE_PREVIOUS = "saveprevious";


    private List registryModificationLog;

    /**
     * The unpacker.
     */
    private IUnpacker unpacker;

    /**
     * The variable substituter.
     */
    private VariableSubstitutor substituter;

    /**
     * The installation data.
     */
    private final AutomatedInstallData installData;

    /**
     * The uninstallation data.
     */
    private final UninstallData uninstallData;

    /**
     * The rules.
     */
    private final RulesEngine rules;

    /**
     * The house-keeper.
     */
    private final Housekeeper housekeeper;

    /**
     * The registry handler. May be <tt>null</tt>
     */
    private final RegistryHandler registry;

    /**
     * The uninstaller icon.
     */
    private static final String UNINSTALLER_ICON = "UninstallerIcon";


    /**
     * Constructs a <tt>RegistryInstallerListener</tt>.
     *
     * @param unpacker      the unpacker
     * @param substituter   the variable substituter
     * @param installData   the installation data
     * @param uninstallData the uninstallation data
     * @param rules         the rules
     * @param resources     the resource manager
     * @param housekeeper   the housekeeper
     * @param handler       the registry handler reference
     */
    public RegistryInstallerListener(IUnpacker unpacker, VariableSubstitutor substituter,
                                     AutomatedInstallData installData, UninstallData uninstallData,
                                     ResourceManager resources, RulesEngine rules, Housekeeper housekeeper,
                                     RegistryDefaultHandler handler)
    {
        super(resources, true);
        this.substituter = substituter;
        this.unpacker = unpacker;
        this.installData = installData;
        this.uninstallData = uninstallData;
        this.rules = rules;
        this.housekeeper = housekeeper;
        this.registry = handler.getInstance();
    }

    @Override
    public void afterInstallerInitialization(AutomatedInstallData data) throws Exception
    {
        super.afterInstallerInitialization(data);
        initializeRegistryHandler(installData);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.izforge.izpack.compiler.InstallerListener#afterPacks(com.izforge.izpack.installer.AutomatedInstallData,
     * com.izforge.izpack.api.handler.AbstractUIProgressHandler)
     */

    public void afterPacks(AutomatedInstallData idata, AbstractUIProgressHandler handler)
            throws Exception
    {
        if (registry != null)
        {
            try
            {
                afterPacks();
            }
            catch (NativeLibException e)
            {
                throw new WrappedNativeLibException(e, installData.getMessages());
            }
        }
    }

    /**
     * Remove all registry entries on failed installation.
     */

    public void cleanUp()
    {
        // installation was not successful now rewind all registry changes
        if (!installData.isInstallSuccess() && registryModificationLog != null && !registryModificationLog.isEmpty())
        {
            try
            {
                registry.activateLogging();
                registry.setLoggingInfo(registryModificationLog);
                registry.rewind();
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private void afterPacks() throws NativeLibException, InstallerException
    {
        // Register for cleanup
        housekeeper.registerForCleanup(this);

        // Start logging
        IXMLElement uninstallerPack = null;
        // No interrupt desired after writing registry entries.
        // TODO - this is a bit strange. There should be a listener method that is invoked after the unpacker has
        // completed, to avoid this hack
        unpacker.setDisableInterrupt(true);
        registry.activateLogging();

        if (getSpecHelper().getSpec() != null)
        {
            // Get the special pack "UninstallStuff" which contains values
            // for the uninstaller entry.
            uninstallerPack = getSpecHelper().getPackForName("UninstallStuff");
            performPack(uninstallerPack);

            // Now perform the selected packs.
            for (Pack selectedPack : installData.getSelectedPacks())
            {
                // Resolve data for current pack.
                IXMLElement pack = getSpecHelper().getPackForName(selectedPack.getName());
                performPack(pack);

            }
        }
        String uninstallSuffix = installData.getVariable("UninstallKeySuffix");
        if (uninstallSuffix != null)
        {
            registry.setUninstallName(registry.getUninstallName() + " " + uninstallSuffix);
        }
        // Generate uninstaller key automatically if not defined in spec.
        if (uninstallerPack == null)
        {
            registerUninstallKey();
        }
        // Get the logging info from the registry class and put it into
        // the uninstaller. The RegistryUninstallerListener loads that data
        // and rewind the made entries.
        // This is the common way to transport informations from an
        // installer CustomAction to the corresponding uninstaller
        // CustomAction.
        List<Object> info = registry.getLoggingInfo();
        if (info != null)
        {
            uninstallData.addAdditionalData("registryEntries", info);
        }
        // Remember all registry info to rewind registry modifications in case of failed installation
        registryModificationLog = info;
    }

    /**
     * Performs the registry settings for the given pack.
     *
     * @param pack XML element which contains the registry settings for one pack
     * @throws InstallerException if a required attribute is missing
     * @throws NativeLibException for any native library error
     */
    private void performPack(IXMLElement pack) throws InstallerException, NativeLibException
    {
        if (pack == null)
        {
            return;
        }
        String packCondition = pack.getAttribute("condition");
        if (packCondition != null)
        {
            logger.fine("Condition \"" + packCondition + "\" found for pack of registry entries");
            if (!rules.isConditionTrue(packCondition))
            {
                // condition not fulfilled, continue with next element.
                logger.fine("Condition \"" + packCondition + "\" not true");
                return;
            }
        }

        // Get all entries for registry settings.
        List<IXMLElement> regEntries = pack.getChildren();
        if (regEntries == null)
        {
            return;
        }
        for (IXMLElement regEntry : regEntries)
        {
            String condition = regEntry.getAttribute("condition");
            if (condition != null)
            {
                logger.fine("Condition " + condition + " found for registry entry");
                if (!rules.isConditionTrue(condition))
                {
                    // condition not fulfilled, continue with next element.
                    logger.fine("Condition \"" + condition + "\" not true");
                    continue;
                }
            }

            // Perform one registry entry.
            String type = regEntry.getName();
            if (type.equalsIgnoreCase(REG_KEY))
            {
                performKeySetting(regEntry);
            }
            else if (type.equalsIgnoreCase(REG_VALUE))
            {
                performValueSetting(regEntry);
            }
            else
            {
                // No valid type.
                getSpecHelper().parseError(regEntry,
                                           "Non-valid type of entry; only 'key' and 'value' are allowed.");
            }

        }

    }

    /**
     * Perform the setting of one value.
     *
     * @param regEntry element which contains the description of the value to be set
     * @throws InstallerException if a required attribute is missing
     * @throws NativeLibException for any native library error
     */
    private void performValueSetting(IXMLElement regEntry) throws InstallerException, NativeLibException
    {
        SpecHelper specHelper = getSpecHelper();
        String name = specHelper.getRequiredAttribute(regEntry, REG_BASENAME);
        name = substituter.substitute(name);
        String keypath = specHelper.getRequiredAttribute(regEntry, REG_KEYPATH);
        keypath = substituter.substitute(keypath);
        String root = specHelper.getRequiredAttribute(regEntry, REG_ROOT);
        int rootId = resolveRoot(regEntry, root);

        registry.setRoot(rootId);

        String override = regEntry.getAttribute(REG_OVERRIDE, "true");
        if (!"true".equalsIgnoreCase(override))
        { // Do not set value if override is not true and the value exist.

            if (registry.getValue(keypath, name, null) != null)
            {
                return;
            }
        }

        //set flag for logging previous contents if "saveprevious"
        // attribute not specified or specified as 'true':
        registry.setLogPrevSetValueFlag("true".equalsIgnoreCase(regEntry.getAttribute(SAVE_PREVIOUS, "true")));

        String value = regEntry.getAttribute(REG_DWORD);
        if (value != null)
        { // Value type is DWord; placeholder possible.
            value = substituter.substitute(value);
            registry.setValue(keypath, name, Long.parseLong(value));
            return;
        }
        value = regEntry.getAttribute(REG_STRING);
        if (value != null)
        { // Value type is string; placeholder possible.
            value = substituter.substitute(value);
            registry.setValue(keypath, name, value);
            return;
        }
        List<IXMLElement> values = regEntry.getChildrenNamed(REG_MULTI);
        if (values != null && !values.isEmpty())
        { // Value type is REG_MULTI_SZ; placeholder possible.
            Iterator<IXMLElement> multiIter = values.iterator();
            String[] multiString = new String[values.size()];
            for (int i = 0; multiIter.hasNext(); ++i)
            {
                IXMLElement element = multiIter.next();
                multiString[i] = specHelper.getRequiredAttribute(element, REG_DATA);
                multiString[i] = substituter.substitute(multiString[i]);
            }
            registry.setValue(keypath, name, multiString);
            return;
        }
        values = regEntry.getChildrenNamed(REG_BIN);
        if (values != null && !values.isEmpty())
        { // Value type is REG_BINARY; placeholder possible or not ??? why not
            // ...
            Iterator<IXMLElement> multiIter = values.iterator();

            StringBuilder buf = new StringBuilder();
            while (multiIter.hasNext())
            {
                IXMLElement element = multiIter.next();
                String tmp = specHelper.getRequiredAttribute(element, REG_DATA);
                buf.append(tmp);
                if (!tmp.endsWith(",") && multiIter.hasNext())
                {
                    buf.append(",");
                }
            }
            byte[] bytes = extractBytes(regEntry, substituter.substitute(buf.toString()));
            registry.setValue(keypath, name, bytes);
            return;
        }
        specHelper.parseError(regEntry, "No data found.");

    }

    private byte[] extractBytes(IXMLElement element, String byteString) throws InstallerException
    {
        StringTokenizer st = new StringTokenizer(byteString, ",");
        byte[] retval = new byte[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens())
        {
            byte value = 0;
            String token = st.nextToken().trim();
            try
            {
                // Unfortunately byte is signed ...
                int tval = Integer.parseInt(token, 16);
                if (tval < 0 || tval > 0xff)
                {
                    throw new InstallerException("Byte value out of range: " + tval);
                }
                if (tval > 0x7f)
                {
                    tval -= 0x100;
                }
                value = (byte) tval;
            }
            catch (NumberFormatException nfe)
            {
                getSpecHelper()
                        .parseError(element,
                                    "Bad entry for REG_BINARY; a byte should be written as 2 digit hexvalue followed by a ','.");
            }
            retval[i++] = value;
        }
        return (retval);

    }

    /**
     * Perform the setting of one key.
     *
     * @param regEntry element which contains the description of the key to be created
     * @throws InstallerException if a required attribute is missing
     * @throws NativeLibException for any native library error
     */
    private void performKeySetting(IXMLElement regEntry) throws InstallerException, NativeLibException
    {
        String path = getSpecHelper().getRequiredAttribute(regEntry, REG_KEYPATH);
        path = substituter.substitute(path);
        String root = getSpecHelper().getRequiredAttribute(regEntry, REG_ROOT);
        int rootId = resolveRoot(regEntry, root);
        registry.setRoot(rootId);
        if (!registry.keyExist(path))
        {
            registry.createKey(path);
        }
    }

    private int resolveRoot(IXMLElement regEntry, String root) throws InstallerException
    {
        String root1 = substituter.substitute(root);
        Integer tmp = RegistryHandler.ROOT_KEY_MAP.get(root1);
        if (tmp != null)
        {
            return (tmp);
        }
        getSpecHelper().parseError(regEntry, "Unknown value (" + root1 + ") for registry root.");
        return 0;
    }

    private void initializeRegistryHandler(AutomatedInstallData installData) throws Exception
    {
        if (registry != null)
        {
            String uninstallName = installData.getVariable("APP_NAME") + " " + installData.getVariable("APP_VER");
            registry.setUninstallName(uninstallName);
            getSpecHelper().readSpec(SPEC_FILE_NAME);
        }
    }

    /**
     * Registers the uninstaller.
     *
     * @throws NativeLibException for any native library exception.
     */
    private void registerUninstallKey() throws NativeLibException
    {
        String uninstallName = registry.getUninstallName();
        if (uninstallName == null)
        {
            return;
        }
        String keyName = RegistryHandler.UNINSTALL_ROOT + uninstallName;
        String cmd = "\"" + installData.getVariable("JAVA_HOME") + "\\bin\\javaw.exe\" -jar \""
                + installData.getVariable("INSTALL_PATH") + "\\uninstaller\\uninstaller.jar\"";
        String appVersion = installData.getVariable("APP_VER");
        String appUrl = installData.getVariable("APP_URL");

        try
        {
            registry.setRoot(RegistryHandler.HKEY_LOCAL_MACHINE);
            registry.setValue(keyName, "DisplayName", uninstallName);
        }
        catch (NativeLibException exception)
        { // Users without administrative rights should be able to install the app for themselves
            logger.warning(
                    "Failed to register uninstaller in HKEY_LOCAL_MACHINE hive, trying HKEY_CURRENT_USER: " + exception.getMessage());
            registry.setRoot(RegistryHandler.HKEY_CURRENT_USER);
            registry.setValue(keyName, "DisplayName", uninstallName);
        }
        registry.setValue(keyName, "UninstallString", cmd);
        registry.setValue(keyName, "DisplayVersion", appVersion);
        if (appUrl != null && appUrl.length() > 0)
        {
            registry.setValue(keyName, "HelpLink", appUrl);
        }
        // Try to write the uninstaller icon out.
        InputStream in = null;
        FileOutputStream out = null;
        try
        {
            in = getResources().getInputStream(UNINSTALLER_ICON);
            String iconPath = installData.getVariable("INSTALL_PATH") + File.separator
                    + "Uninstaller" + File.separator + "UninstallerIcon.ico";
            out = new FileOutputStream(iconPath);
            IoHelper.copyStream(in, out);
            registry.setValue(keyName, "DisplayIcon", iconPath);
        }
        catch (Exception e)
        {
            // May be no icon resource defined; ignore it
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        finally
        {
            FileUtils.close(in);
            FileUtils.close(out);
        }
    }


}
