package com.izforge.izpack.panels.licence;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.ResourceManager;
import com.izforge.izpack.installer.console.AbstractTextPanelConsole;
import com.izforge.izpack.util.Console;

/**
 * Abstract panel for displaying license text to the console.
 *
 * @author Tim Anderson
 */
public abstract class AbstractLicensePanelConsole extends AbstractTextPanelConsole
{
    private static final Logger logger = Logger.getLogger(AbstractLicensePanelConsole.class.getName());

    /**
     * The resources.
     */
    private final ResourceManager resources;

    /**
     * Constructs a <tt>AbstractLicensePanelConsole</tt>.
     *
     * @param resources the resources
     */
    public AbstractLicensePanelConsole(ResourceManager resources)
    {
        this.resources = resources;
    }

    /**
     * Returns the named text resource
     *
     * @param resourceName the resource name
     * @return the text resource, or <tt>null</tt> if it cannot be found
     */
    protected String getText(String resourceName)
    {
        String result = null;
        try
        {
            result = resources.getTextResource(resourceName);
        }
        catch (IOException e)
        {
            logger.log(Level.WARNING,
                    "Could not load the licence text for resource: " + resourceName,
                    e);
        }
        return result;
    }

    /**
     * Prompts to end the license panel.
     * <p/>
     * This displays a prompt to accept, reject, or redisplay. On redisplay, it invokes
     * {@link #runConsole(AutomatedInstallData, Console)}.
     *
     * @param installData the installation date
     * @param console     the console to use
     * @return <tt>true</tt> to accept, <tt>false</tt> to reject. If redisplaying the panel, the result of
     *         {@link #runConsole(AutomatedInstallData, Console)} is returned
     */
    @Override
    protected boolean promptEndPanel(AutomatedInstallData installData, Console console)
    {
        boolean result;
        int value = console.prompt("Press 1 to accept, 2 to reject, 3 to redisplay", 1, 3, 2);
        result = value == 1 || value != 2 && runConsole(installData, console);
        return result;
    }

}
