/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
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

package com.izforge.izpack.api.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.api.event.InstallerListener;
import com.izforge.izpack.api.resource.Messages;
import com.izforge.izpack.api.rules.RulesEngine;

/**
 * Encloses information about the install process. This implementation is not thread safe.
 *
 * @author Julien Ponge <julien@izforge.com>
 * @author Johannes Lehtinen <johannes.lehtinen@iki.fi>
 */
public abstract class AutomatedInstallData
{

    // --- Static members -------------------------------------------------
    public static final String MODIFY_INSTALLATION = "modify.izpack.install";
    public static final String INSTALLATION_INFORMATION = ".installationinformation";

    /**
     * The path for multi-volume installation media.
     */
    public static final String MEDIA_PATH = "MEDIA_PATH";


    // --- Instance members -----------------------------------------------

    private RulesEngine rules;

    /**
     * The language code.
     */
    private String localeISO3;

    /**
     * The used locale.
     */
    private Locale locale;

    /**
     * The messages.
     */
    private Messages messages;

    /**
     * The inforamtions.
     */
    private Info info;

    /**
     * The complete list of packs.
     */
    private List<Pack> allPacks;

    /**
     * The available packs.
     */
    private List<Pack> availablePacks;

    /**
     * The selected packs.
     */
    private List<Pack> selectedPacks;

    /**
     * The panels list.
     */
    private List panels;

    /**
     * The panels order.
     */
    private List<Panel> panelsOrder;

    /**
     * The current panel.
     */
    private int curPanelNumber;

    /**
     * Can we close the installer ?
     */
    private boolean canClose = false;

    /**
     * Did the installation succeed ?
     */
    private boolean installSuccess = true;

    /**
     * Is a reboot necessary to complete the installation ?
     */
    private boolean rebootNecessary = false;

    /**
     * The xmlData for automated installers.
     */
    private IXMLElement xmlData;

    /**
     * Custom data.
     */
    private Map<String, List> customData;

    /**
     * The variables.
     */
    private final Variables variables;

    /**
     * Dynamic variables
     */
    private Map<String, List<DynamicVariable>> dynamicvariables;

    /**
     * Dynamic conditions
     */
    private List<DynamicInstallerRequirementValidator> dynamicinstallerrequirements;

    /**
     * List of install requirements
     */
    private List<InstallerRequirement> installerrequirements;

    /**
     * The attributes used by the panels
     */
    private Map<String, Object> attributes;

    /**
     * The install path
     */
    public final static String INSTALL_PATH = "INSTALL_PATH";
    /**
     * The default install path
     */
    public final static String DEFAULT_INSTALL_PATH = "INSTALL_PATH";
    /**
     * The install drive (Windows only, otherwise not set)
     */
    public final static String INSTALL_DRIVE = "INSTALL_DRIVE";
    /**
     * The default install drive (Windows only, otherwise not set)
     */
    public final static String DEFAULT_INSTALL_DRIVE = "INSTALL_DRIVE";
    /**
     * Helps information
     */
    public final static String HELP_TAG = "help";
    public final static String ISO3_ATTRIBUTE = "iso3";
    public final static String SRC_ATTRIBUTE = "src";

    /**
     * The listeners.
     */
    private List<InstallerListener> installerListener = new ArrayList<InstallerListener>();

    /**
     * Constructs an <tt>AutomatedInstallData</tt>.
     *
     * @param variables the variables
     */
    public AutomatedInstallData(Variables variables)
    {
        this.variables = variables;
        setAvailablePacks(new ArrayList<Pack>());
        setSelectedPacks(new ArrayList<Pack>());
        panels = new ArrayList();
        setPanelsOrder(new ArrayList<Panel>());
        setXmlData(new XMLElementImpl("AutomatedInstallation"));
        setAttributes(new HashMap<String, Object>());
        setCustomData(new HashMap<String, List>());
    }

    /**
     * Returns the variables.
     *
     * @return the variables
     */
    public Variables getVariables()
    {
        return variables;
    }

    /**
     * Sets a variable to the specified value. This is short hand for
     * {@code getVariables().set(name, value)}.
     *
     * @param name  the name of the variable
     * @param value the new value of the variable
     * @see #getVariable
     */
    public void setVariable(String name, String value)
    {
        variables.set(name, value);
    }

    /**
     * Returns the current value of the specified variable. This is short hand for
     * {@code getVariables().get(name)}.
     *
     * @param var the name of the variable
     * @return the value of the variable or null if not set
     * @see #setVariable
     */
    public String getVariable(String var)
    {
        return variables.get(var);
    }

    /**
     * Refreshes dynamic variables. This is short hand for {@code getVariables().refresh()}.
     */
    public void refreshVariables()
    {
        variables.refresh();
    }

    /**
     * Sets the install path.
     *
     * @param path the new install path
     * @see #getInstallPath
     */
    public abstract void setInstallPath(String path);

    /**
     * Returns the install path.
     *
     * @return the current install path or null if none set yet
     * @see #setInstallPath
     */
    public abstract String getInstallPath();

    /**
     * Sets the default install path.
     *
     * @param path the default install path
     * @see #getDefaultInstallPath
     */
    public abstract void setDefaultInstallPath(String path);

    /**
     * Returns the default install path.
     *
     * @return the default install path or null if none set yet
     * @see #setDefaultInstallPath
     */
    public abstract String getDefaultInstallPath();

    /**
     * Sets the media path for multi-volume installation.
     *
     * @param path the media path. May be <tt>null</tt>
     */
    public void setMediaPath(String path)
    {
        setVariable(MEDIA_PATH, path);
    }

    /**
     * Returns the media path for multi-volume installation.
     *
     * @return the media path. May be <tt>null</tt>
     */
    public String getMediaPath()
    {
        return getVariable(MEDIA_PATH);
    }

    /**
     * Returns the value of the named attribute.
     *
     * @param attr the name of the attribute
     * @return the value of the attribute or null if not set
     * @see #setAttribute
     */
    public Object getAttribute(String attr)
    {
        return getAttributes().get(attr);
    }

    /**
     * Sets a named attribute. The panels and other IzPack components can attach custom attributes
     * to InstallData to communicate with each other. For example, a set of co-operating custom
     * panels do not need to implement a common data storage but can use InstallData singleton. The
     * name of the attribute should include the package and class name to prevent name space
     * collisions.
     *
     * @param attr the name of the attribute to set
     * @param val  the value of the attribute or null to unset the attribute
     * @see #getAttribute
     */
    public void setAttribute(String attr, Object val)
    {
        if (val == null)
        {
            getAttributes().remove(attr);
        }
        else
        {
            getAttributes().put(attr, val);
        }
    }

    /**
     * Set Locale in xml, installdata, and langpack.
     *
     * @param locale         Locale to set
     * @param localeDatabase database containing the desired locale
     */
    public void setAndProcessLocal(String locale, LocaleDatabase localeDatabase)
    {
        // We add an xml data information
        getXmlData().setAttribute("langpack", locale);
        // We load the langpack
        setLocaleISO3(locale);
        setVariable(ScriptParserConstant.ISO3_LANG, getLocaleISO3());
        this.messages = localeDatabase;
    }

    public RulesEngine getRules()
    {
        return rules;
    }


    public void setRules(RulesEngine rules)
    {
        this.rules = rules;
    }

    public String getLocaleISO3()
    {
        return localeISO3;
    }

    public void setLocaleISO3(String localeISO3)
    {
        this.localeISO3 = localeISO3;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    /**
     * Returns the localised messages.
     *
     * @return the localised messages
     */
    public Messages getMessages()
    {
        return messages;
    }

    public LocaleDatabase getLangpack()
    {
        return (LocaleDatabase) messages;
    }

    public void setLangpack(LocaleDatabase langpack)
    {
        this.messages = langpack;
    }

    public Info getInfo()
    {
        return info;
    }

    public void setInfo(Info info)
    {
        this.info = info;
    }

    public List<Pack> getAllPacks()
    {
        return allPacks;
    }

    public void setAllPacks(List<Pack> allPacks)
    {
        this.allPacks = allPacks;
    }

    public List<Pack> getAvailablePacks()
    {
        return availablePacks;
    }

    public void setAvailablePacks(List<Pack> availablePacks)
    {
        this.availablePacks = availablePacks;
    }

    public List<Pack> getSelectedPacks()
    {
        return selectedPacks;
    }

    public void setSelectedPacks(List<Pack> selectedPacks)
    {
        this.selectedPacks = selectedPacks;
    }

    public List getPanels()
    {
        return panels;
    }

    public List<Panel> getPanelsOrder()
    {
        return panelsOrder;
    }

    public void setPanelsOrder(List<Panel> panelsOrder)
    {
        this.panelsOrder = panelsOrder;
    }

    public int getCurPanelNumber()
    {
        return curPanelNumber;
    }

    public void setCurPanelNumber(int curPanelNumber)
    {
        this.curPanelNumber = curPanelNumber;
    }

    public boolean isCanClose()
    {
        return canClose;
    }

    public void setCanClose(boolean canClose)
    {
        this.canClose = canClose;
    }

    public boolean isInstallSuccess()
    {
        return installSuccess;
    }

    public void setInstallSuccess(boolean installSuccess)
    {
        this.installSuccess = installSuccess;
    }

    public boolean isRebootNecessary()
    {
        return rebootNecessary;
    }

    public void setRebootNecessary(boolean rebootNecessary)
    {
        this.rebootNecessary = rebootNecessary;
    }

    public IXMLElement getXmlData()
    {
        return xmlData;
    }

    public void setXmlData(IXMLElement xmlData)
    {
        this.xmlData = xmlData;
    }

    public Map<String, List> getCustomData()
    {
        return customData;
    }

    public void setCustomData(Map<String, List> customData)
    {
        this.customData = customData;
    }

    @Deprecated
    public void setVariables(Variables variables)
    {
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes)
    {
        this.attributes = attributes;
    }

    @Deprecated
    public Map<String, List<DynamicVariable>> getDynamicvariables()
    {
        return this.dynamicvariables;
    }

    @Deprecated
    public void setDynamicvariables(Map<String, List<DynamicVariable>> dynamicvariables)
    {
        this.dynamicvariables = dynamicvariables;
    }

    public List<DynamicInstallerRequirementValidator> getDynamicinstallerrequirements()
    {
        return this.dynamicinstallerrequirements;
    }

    public void setDynamicinstallerrequirements(List<DynamicInstallerRequirementValidator> dynamicinstallerrequirements)
    {
        this.dynamicinstallerrequirements = dynamicinstallerrequirements;
    }

    public void setInstallerrequirements(List<InstallerRequirement> installerrequirements)
    {
        this.installerrequirements = installerrequirements;
    }

    public List<InstallerRequirement> getInstallerrequirements()
    {
        return installerrequirements;
    }

    @Deprecated
    public List<InstallerListener> getInstallerListener()
    {
        return installerListener;
    }

    @Deprecated
    public void setInstallerListener(List<InstallerListener> installerListener)
    {
        this.installerListener = installerListener;
    }
}
