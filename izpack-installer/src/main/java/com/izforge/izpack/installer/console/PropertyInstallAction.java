package com.izforge.izpack.installer.console;

import java.util.Properties;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.exception.InstallerException;
import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.api.installer.DataValidator;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.installer.data.UninstallDataWriter;
import com.izforge.izpack.util.Console;


/**
 * Performs installation from properties.
 *
 * @author Tim Anderson
 */
class PropertyInstallAction extends AbstractInstallAction
{
    /**
     * The properties to use for installation.
     */
    private final Properties properties;

    /**
     * Constructs a <tt>PropertyInstallAction</tt>.
     *
     * @param factory       the panel console factory
     * @param installData   the installation data
     * @param objectFactory the factory for {@link DataValidator} instances
     * @param rules         the rules engine
     * @param writer        the uninstallation data writer
     * @param properties    the installation properties
     */
    public PropertyInstallAction(PanelConsoleFactory factory, AutomatedInstallData installData,
                                 ObjectFactory objectFactory, RulesEngine rules, UninstallDataWriter writer,
                                 Properties properties)
    {
        super(factory, installData, objectFactory, rules, writer);
        this.properties = properties;
    }

    /**
     * Runs the action for the console panel associated with the specified panel.
     *
     * @param panel        the panel
     * @param panelConsole the console implementation of the panel
     * @param console      the console
     * @return <tt>true</tt> if the action was successful, otherwise <tt>false</tt>
     * @throws InstallerException for any installer error
     */
    @Override
    protected boolean run(Panel panel, PanelConsole panelConsole, Console console) throws InstallerException
    {
        boolean result = panelConsole.runConsoleFromProperties(getInstallData(), properties);
        if (result)
        {
            result = validatePanel(panel, console);
        }
        return result;
    }

}
