package org.opencms.main;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.LogFactory;
import org.opencms.configuration.CmsParameterConfiguration;
import org.opencms.db.CmsLoginMessage;
import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.main.OpenCmsCore;

//import org.opencms.util.CmsPropertyUtils;


import java.io.IOException;


/**
 * Instantiates an embedded Opencms.
 *
 * Based on org.opencms.main.CmsShell.
 *
 * Needs to be in package org.opencms.main as to access protected methods from
 * OpenCms.
 */
//ADDED BY COMUNDUS
//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public final class CmOpenCmsShell {
    /**
     * The Singleton instance.
     */
    private static CmOpenCmsShell instance = null;

    /** The OpenCms system object. */
    private OpenCmsCore opencmsCore;

    /** The OpenCms context object. */
    private CmsObject cmsObject;

    /**
     * Path to the OpenCms instances WEB-INF directory.
     */
    private String webinfPath;

    /**
     * Private constructor ensures that Singleton is never created externally
     * with new.
     *
     */
    private CmOpenCmsShell() {
        // this Singleton should never be instantiated with new
    }

    /**
     * Constructor that instantiates OpenCms with the given user logged in.
     *
     * @param webinfpath
     *            path to OpenCms instances WEB-INF directory
     * @param login
     *            user login
     * @param password
     *            user password
     *
     * @throws IOException
     *             in case the OpenCms configuration can not be loaded
     * @throws CmsException
     *             in case anything goes wrong with OpenCms
     */
    private CmOpenCmsShell(final String webinfpath, final String login,
        final String password) throws IOException, CmsException {
        this.webinfPath = webinfpath;
        this.initialize();
        this.login(login, password);
    }

    /**
     * Get a Singleton CmOpencmsShell instance.
     *
     * @param webinfpath
     *            path to OpenCms instances WEB-INF directory
     * @param login
     *            user login
     * @param password
     *            user password
     * @return CmOpencmsShell Singleton instance with the given user logged in
     * @throws CmsException
     *             if anything OpenCms goes wrong
     * @throws IOException
     *             in case configuration files cannot be read
     */
    public static CmOpenCmsShell getInstance(final String webinfpath,
        final String login, final String password)
        throws CmsException, IOException {
        if (CmOpenCmsShell.instance == null) {
            CmOpenCmsShell.instance = new CmOpenCmsShell(webinfpath, login,
                    password);
        }

        return CmOpenCmsShell.instance;
    }

    /**
     * Instantiates OpenCms and logs in as Guest user.
     *
     * The typical incantation to work with the OpenCms instance is:
     *
     * CmOpencmsShell mycmsshell = new CmOpencmsShell(webinfpath);
     * mycmsshell.initialize(); mycmsshell.login(login,password);
     *
     * or call the constructor with three parameters that performs these steps
     * internally.
     *
     * Call exit() to shut down the system
     *
     * @throws IOException
     *             in case the OpenCms configuration can not be loaded
     * @throws CmsException
     *             in case the OpenCms system can not be initialized
     */

    // code taken from org.opencms.main.CmsShell
    private void initialize() throws IOException, CmsException {
        //Hack: Initialize CmsLog.INIT
        if (CmsLog.INIT == null) {  
            CmsLog.INIT = LogFactory.getLog("org.opencms.init");
        }
        System.out.println(CmsLog.INIT);
        
        // first initialize runlevel 1
        this.opencmsCore = OpenCmsCore.getInstance();
        // set the path to the WEB-INF folder
        // the other parameters are just dummies for servletPath and
        // webappName
        CmsServletContainerSettings settings = new CmsServletContainerSettings(
                this.webinfPath,                
                "ROOT",
                "/opencms/*",
                null,
                null);
        this.opencmsCore.getSystemInfo().init(settings);        
        //                .init(this.webinfPath, "/opencms/*", null, "opencms", null/*, true*/);//Last parameter was removed in 7.5
        											//Long signature was removed in 7.5.2

        // now read the configuration properties
        final String propertyPath = this.opencmsCore.getSystemInfo()
                                                    .getConfigurationFileRfsPath();
        //final ExtendedProperties configuration = CmsPropertyUtils.loadProperties(propertyPath);
        final CmsParameterConfiguration configuration=new CmsParameterConfiguration(propertyPath);
        // now upgrade to runlevel 2
        // requires servlet-api indirectly
        // this adds some rows to an empty database:
        // CMS_GROUPS: Guests, Projectmanagers, Administrators, User
        // CMS_GROUPUSERS: 3 rows
        // CMS_OFFLINE_RESOURCES: 1 row
        // CMS_OFFLINE_STRUCTURE: /
        // CMS_ONLINE_RESOURCES: 1 row
        // CMS_ONLINE_STRUCTURE: /
        // CMS_PROJECTRESOURCES: 2 * /
        // CMS_PROJECTS: Online, _setupProject
        // CMS_SYSTEMID: 3 * 11
        // CMS_TASK: Online, _setupProject
        // CMS_TASKTYPE: Ad-Hoc
        // CMS_USERS: Export, Guest, Admin
        this.opencmsCore = this.opencmsCore.upgradeRunlevel(configuration);
        // create a context object with 'Guest' permissions
        this.cmsObject = this.opencmsCore.initCmsObject(this.opencmsCore.getDefaultUsers()
                                                                        .getUserGuest());

        // initialize the settings of the user
        // initSettings(); // sets userSettings
    }

    /**
     * Log a user in to the the CmsSell.
     *
     * @param username
     *            the name of the user to log in
     * @param password
     *            the password of the user
     */

    // code taken from org.opencms.main.CmsShellCommands
    private void login(final String username, final String password) {
        final String translatedUsername = OpenCms.getImportExportManager()
                                                 .translateUser(username);

        try {
            this.cmsObject.loginUser(translatedUsername, password);

            // reset the settings, this will switch the startup site root etc.
            // initSettings(); // sets userSettings
            final CmsLoginMessage message = OpenCms.getLoginManager()
                                                   .getLoginMessage();

            if ((message != null) && (message.isActive())) {
                System.out.println(message.getMessage());
            }
        } catch (final CmsException exc) {
            // TODO System.out.println(getMessages().key(Messages.GUI_SHELL_LOGIN_FAILED_0));
        }
    }

    /**
     * Private internal helper for localisation to the current user's locale
     * within OpenCms.
     *
     * @return the current user's <code>Locale</code>.
     */

    // code taken from org.opencms.main.CmsShellCommands
    //private Locale getLocale() {
    //  return CmsLocaleManager.getDefaultLocale();
    //}

    /**
     * Gets the CmsObject.
     *
     * Call initialize() first!
     *
     * @return the CmsObject
     */
    public CmsObject getCmsObject() {
        return this.cmsObject;
    }
}
