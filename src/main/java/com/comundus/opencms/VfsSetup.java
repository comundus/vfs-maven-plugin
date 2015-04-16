package com.comundus.opencms;

import java.io.File;
import java.util.List;

import org.opencms.db.CmsDefaultUsers;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsShell;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsRole;


/**
 * Executes the OpenCms setup.
 *
 * It executes the same steps like OpenCms in its own setup, as declared in the
 * CmsShell script WEB-INF/setupdata/cmssetup.txt.
 * Additionally the Admin password gets set.
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class VfsSetup {
    /**
     * The OpenCms default Admin login. Used to log in to a fresh installation.
     */
    private static final String ADMIN_LOGIN = "Admin";

    /**
     * The OpenCms default Admin password. Used to log in to a fresh
     * installation.
     */
    private static final String ADMIN_DEFAULT_PASSWORD = "admin";

    /**
     * Performs the initial setup of a fresh OpenCms.
     *
     * First the operations like in WEB-INF/setupdata/cmssetup.txt are
     * performed, then all listed standard OpenCms module ZIPs get imported;
     * last but not least the Admin password is changed from the default one to
     * the given one.
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param opencmsmoduleSourceDirectory
     *            path to the standard OpenCms module ZIPs
     * @param opencmsModules
     *            List of standard OpenCms module ZIPs to import
     * @param adminPassword
     *            the new Admin password to set
     * @throws Exception
     *             if anything goes wrong
     */
    public final void execute(final String webappDirectory,
        final String opencmsmoduleSourceDirectory, final List opencmsModules,
        final String adminPassword) throws Exception {

        final String webinfdir = webappDirectory + File.separatorChar +
            "WEB-INF";
        final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir,
                VfsSetup.ADMIN_LOGIN, VfsSetup.ADMIN_DEFAULT_PASSWORD);
        final CmsObject cmsobject = cmsshell.getCmsObject();
        final CmsRequestContext requestcontext = cmsobject.getRequestContext();
        //final CmsImportExportManager impexpmanager = OpenCms.getImportExportManager();
        System.out.println("OpenCms: initial setup");
        this.executeSetupTxt(cmsobject, requestcontext /*, impexpmanager*/);
        System.out.println("OpenCms: executeSetupTxt finished");
        if ((adminPassword != null) && (adminPassword.length() > 0)) {
            System.out.println("OpenCms: setting Admin password");
            cmsobject.setPassword(VfsSetup.ADMIN_LOGIN, adminPassword);
        }
    }

    /**
     * org.opencms.setup.CmsSetupWorkplaceImportThread performs these steps from
     * WEB-INF/setupdata/cmssetup.txt.
     *
     * @param cmsobject
     *            the CmsObject
     * @param requestcontext
     *            and the CmsObjects RequestContext
     * @param iomanager
     *            CmsImportExportManager for translating initial group names
     * @throws Exception
     *             if anything goes wrong
     */
    private void executeSetupTxt(final CmsObject cmsobject,
        final CmsRequestContext requestcontext /*,
    final CmsImportExportManager iomanager*/) throws Exception {
        // comments come from the cmssetup.txt
        // # Create the setup project
        // createTempfileProject
        cmsobject.createTempfileProject();

        // # Switch to the setup project
        // setCurrentProject "_setupProject"
        final CmsProject setupproject = cmsobject.readProject("_setupProject");
        requestcontext.setCurrentProject(setupproject);
        // # Initialize the default property definitions
        // createPropertyDefinition Title
        // createPropertyDefinition Description
        // createPropertyDefinition Keywords
        // createPropertyDefinition NavText
        // createPropertyDefinition NavPos
        // createPropertyDefinition export
        // createPropertyDefinition exportname
        // createPropertyDefinition default-file
        // createPropertyDefinition content-encoding
        // createPropertyDefinition content-conversion
        // createPropertyDefinition cache
        // createPropertyDefinition template
        // createPropertyDefinition template-elements
        // createPropertyDefinition locale
        // createPropertyDefinition config.sitemap
        cmsobject.createPropertyDefinition("Title");
        cmsobject.createPropertyDefinition("Description");
        cmsobject.createPropertyDefinition("Keywords");
        cmsobject.createPropertyDefinition("NavText");
        cmsobject.createPropertyDefinition("NavPos");
        cmsobject.createPropertyDefinition("export");
        cmsobject.createPropertyDefinition("exportname");
        cmsobject.createPropertyDefinition("default-file");
        cmsobject.createPropertyDefinition("content-encoding");
        cmsobject.createPropertyDefinition("content-conversion");
        cmsobject.createPropertyDefinition("cache");
        cmsobject.createPropertyDefinition("template");
        cmsobject.createPropertyDefinition("template-elements");
        cmsobject.createPropertyDefinition("locale");
        cmsobject.createPropertyDefinition("config.sitemap");
        // # Switch to the root context
        // setSiteRoot "/"
        requestcontext.setSiteRoot("/");
        // # Create folder structure
        // createFolder "/" "system/"	<= no longer in OpenCms 7
        // createFolder "/" "sites/"
        // createFolder "/" "channels/"	<= no longer in OpenCms 7
        // createFolder "/sites/" "default/"
        // createFolder "/system/" "lost-found/"
	// createFolder "/system/" "modules/"
        // createFolder "/" "shared/"

        I_CmsResourceType resourceTypeFolder = 
                OpenCms.getResourceManager().getResourceType(CmsResourceTypeFolder.RESOURCE_TYPE_ID);
        cmsobject.createResource("/sites/", resourceTypeFolder);

        // in order to avoid problems while synchronizing,
        // we do not create the folder here but have it created
        // from synchronizing VFS /sites/default/ in the webapp
        //cmsobject.createResource("/sites/default/",
        //CmsResourceTypeFolder.RESOURCE_TYPE_ID);
        cmsobject.createResource("/system/lost-found/", resourceTypeFolder);

        cmsobject.createResource("/system/modules/", resourceTypeFolder);
        
        cmsobject.createResource("/shared/", resourceTypeFolder);

        // # Apply folder permissions
        /* http://www.opencms.org/export/javadoc/core/org/opencms/security/CmsPermissionSet.html says:
         * PERMISSION_READ (r) the right to read the contents of a resource
         * PERMISSION_WRITE (w) the right to write the contents of a resource
         * PERMISSION_VIEW (v) the right to see a resource in listings (workplace)
         * PERMISSION_CONTROL (c) the right to set permissions of a resource
         * PERMISSION_DIRECT_PUBLISH (d) the right direct publish a resource even without publish project permissions
         */
        cmsobject.lockResource("/");
        // we'll have that created by the "system" project

        // like org.opencms.main.CmsShellCommands.chacc()
        // chacc "/" "user" "ALL_OTHERS" "+v+r+i"
        cmsobject.chacc("/", "user", "ALL_OTHERS", "+v+r+i");

        // it may look funny, but according to CmsShellCommands everything not a group is translated as a user, well, if translated at all?
        // Det thinks that it is not necessary to translate Group "Users", thus removed the ImportExportManager completely 
        // chacc "/" "role" "ROOT_ADMIN" "+v+w+r+c+d+i"
        cmsobject.chacc("/", "role", "ROOT_ADMIN", "+v+w+r+c+d+i");
        // chacc "/system" "role" "WORKPLACE_USER" "+r+i"
        cmsobject.chacc("/system", "role", "WORKPLACE_USER", "+r+i");
        // chacc "/system/lost-found" "role" "WORKPLACE_MANAGER" "+v+w+r+c+d+i"
        cmsobject.chacc("/system/lost-found", "role", "WORKPLACE_MANAGER",
            "+v+w+r+c+d+i");
		// chacc "/system/modules" "role" "WORKPLACE_USER" "+v+r+i"
        cmsobject.chacc("/system/modules", "role", "WORKPLACE_USER",
            "+v+r+i");
        // chacc "/system/orgunits" "role" "ACCOUNT_MANAGER" "+v+w+r+c+d+i"
        // the folder orgunit is created automatically, as it is basically needed for the system to function
        // that's one reason why we can't remove it here and have it recreated later ...
        // thus, we can synchronize only subfolders of "/system/orgunits"
        cmsobject.chacc("/system/orgunits", "role", "ACCOUNT_MANAGER",
            "+v+w+r+c+d+i");
        // chacc "/sites/default" "group" "Users" "+v+w+r+i"
        // we don't do that here but during vfs:sync
        //cmsobject.chacc("/sites/default", "group", /*iomanager.translateGroup(*/"Users"/*)*/,
        //"+v+w+r+i");

        // chacc "/shared" "group" "Users" "+v+w+r+i" 
        cmsobject.chacc("/shared", "group", "Users",
                "+v+w+r+i");
        
        // # Publish the project
        // unlockCurrentProject
        cmsobject.unlockProject(cmsobject.getRequestContext().getCurrentProject()
                                         .getUuid());
        
        System.out.println("Project is going to be published");
        // publishProjectAndWait
        OpenCms.getPublishManager().publishProject(cmsobject);
        System.out.println("Publish sent. Waiting.");
       	OpenCms.getPublishManager().waitWhileRunning();
        System.out.println("Publish finished.");
        
        //# Create the default "Offline" project
        //createDefaultProject "Offline" "The Offline Project"
        createDefaultProject(cmsobject, "Offline", "The Offline Project");
        
        // The rest is not needed
        //# Import the modules that have been selected in the setup wizard to the default site
        //setSiteRoot "/sites/default/"
        //importModulesFromSetupBean

        //# Rebuild search indexes
        //rebuildAllIndexes

        //exit
    }
    
    /**
     * Copied from {@link CmsShellCommands#createDefaultProject}
     * Creates a default project.<p>
     * 
     * This created project has the following properties:<ul>
     * <li>The users group is the default user group
     * <li>The users group is also the default project manager group
     * <li>All resources are contained in the project
     * <li>The project will remain after publishing</ul>
     * 
     * @param name the name of the project to create
     * @param description the description for the new project
     * @throws Exception if something goes wrong
     */
    public void createDefaultProject(CmsObject cms, String name, String description) throws Exception {
        System.out.println("Creating Offline project.");        

        CmsProject project = cms.createProject(
            name,
            description,
            OpenCms.getDefaultUsers().getGroupUsers(),
            OpenCms.getDefaultUsers().getGroupUsers(),
            CmsProject.PROJECT_TYPE_NORMAL);
        cms.getRequestContext().setCurrentProject(project);
        cms.copyResourceToProject("/");
        
        System.out.println("Created Offline project.");
        
        // new with OpenCms 6.2.3, but I think we don't need that for a freshly setup system
        // if (OpenCms.getRoleManager().hasRole(cms, CmsRole.WORKPLACE_MANAGER)) {
        //    // re-initialize the search indexes after default project generation
        //    OpenCms.getSearchManager().initialize(cms);
        //}
    }

}
