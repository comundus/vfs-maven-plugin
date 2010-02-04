package com.comundus.opencms;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.opencms.db.generic.Messages;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsException;
import org.opencms.main.CmsInitException;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.report.CmsShellReport;
import org.opencms.security.CmsOrganizationalUnit;
import org.opencms.security.CmsRole;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.util.CmsUUID;


/**
 * Creates OrgUnits from the folders in "/system/orgunits/".
 *
 */

// (C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class VfsOrgunits extends XmlHandling {
    /** The root path for organizational units. */
    private static final String ORGUNIT_BASE_FOLDER = "/system/orgunits/";

    /** The name of the offline project. */
    private static final String OFFLINE_PROJECT_NAME = "Offline";

    /** Property for the organizational unit description. */
    // private static final String ORGUNIT_PROPERTY_DESCRIPTION =
    // CmsPropertyDefinition.PROPERTY_DESCRIPTION;
    /** Property for the organizational unit default project id. */
    private static final String ORGUNIT_PROPERTY_PROJECTID = CmsPropertyDefinition.PROPERTY_KEYWORDS;

    /**
     * Creates organizational units from their already existant folders.
     *
     * @param webappDirectory
     *                path to WEB-INF of the OpenCms installation
     * @param adminPassword
     *                password of user "Admin" performing the operation
     * @throws Exception
     *                 if anything goes wrong
     */

    // code taken from CmsUserDriver.createOrganizationalUnit und
    // CmsUserDriver.addResourceToOrganizationalUnit
    public final void execute(final String webappDirectory,
        final String adminPassword) throws Exception {
        final String webinfdir = webappDirectory + File.separatorChar +
            "WEB-INF";
        final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir,
                "Admin", adminPassword);
        this.setCms(cmsshell.getCmsObject());

        final CmsRequestContext requestcontext = this.getCms()
                                                     .getRequestContext();
        this.setReport(new CmsShellReport(requestcontext.getLocale()));

        final CmsProject offlineProject = this.getCms().readProject("Offline");
        requestcontext.setCurrentProject(offlineProject);
        requestcontext.setSiteRoot("/");

        createorgunits(requestcontext, offlineProject, ORGUNIT_BASE_FOLDER, "");

        this.getCms().unlockProject(offlineProject.getUuid());
    }

    private void createorgunits(CmsRequestContext requestcontext,
        CmsProject offlineProject, String baserespath, String baseouname)
        throws Exception {
        final List resources = this.getCms()
                                   .getResourcesInFolder(baserespath,
                CmsResourceFilter.IGNORE_EXPIRATION);

        for (int i = 0; i < resources.size(); i++) {
            CmsResource res = (CmsResource) resources.get(i);

            if ((!res.getState().isDeleted()) && (res.isFolder())) {
                Locale locale = requestcontext.getLocale();

                if (locale == null) {
                    locale = CmsLocaleManager.getDefaultLocale();
                }

                String ouname = baseouname + res.getName() + "/";
                //if (!ou.hasFlagWebuser()) {
				if (!hasFlag(res,CmsOrganizationalUnit.FLAG_WEBUSERS)) {
					internalCreateDefaultGroups(ouname, "", false);
	                // create default project
	                CmsProject project = this.getCms()
	                                         .createProject(ouname +
	                        OFFLINE_PROJECT_NAME, "",
	                        ouname + OpenCms.getDefaultUsers().getGroupUsers(),
	                        ouname + OpenCms.getDefaultUsers().getGroupUsers(),
	                        CmsProject.PROJECT_TYPE_NORMAL);
	                // write project id property
	                // lock the file in the VFS, so that it can be updated
	                this.getCms().lockResource(res.getRootPath());
	                this.getCms()
	                    .writePropertyObject(res.getRootPath(),
	                    new CmsProperty(ORGUNIT_PROPERTY_PROJECTID,
	                        project.getUuid().toString(), null));
	                // as in addResourceToOrganizationalUnit
	                // wir müssen uns alle Relations für den OU-Folder holen
	                List relations = this.getCms()
	                                     .getRelationsForResource(res.getRootPath(),
	                        CmsRelationFilter.TARGETS);
	                requestcontext.setCurrentProject(project);

	                Iterator it = relations.iterator();

	                while (it.hasNext()) {
	                    CmsRelation rel = (CmsRelation) it.next();
	                    this.getCms().copyResourceToProject(rel.getTargetPath());
	                }
				} else {
					internalCreateDefaultGroups(ouname, "", true);
	                // write project id property
	                // lock the file in the VFS, so that it can be updated
	                this.getCms().lockResource(res.getRootPath());
	                this.getCms()
	                    .writePropertyObject(res.getRootPath(),
	                    new CmsProperty(ORGUNIT_PROPERTY_PROJECTID,
	                        CmsUUID.getNullUUID().toString(), null));
				}

                requestcontext.setCurrentProject(offlineProject);
/*
                List ougroups = OpenCms.getOrgUnitManager().getGroups(
                        this.getCms(), ouname, false);
                it = ougroups.iterator();
                while (it.hasNext()) {
                    CmsGroup group = (CmsGroup) it.next();
                    // code taken from CmsDriverManager.createGroup()
                    // if group is virtualizing a role, initialize it
                    if (group.isVirtual()) {
                        // get all users that have the given role (in the OU)
                        String rolename = CmsRole.valueOf(group).getGroupName();
                        List users = this.getCms().getUsersOfGroup(
                                group.getName());
                        Iterator it2 = users.iterator();
                        while (it2.hasNext()) {
                            CmsUser user = (CmsUser) it2.next();
                            // put them in the new group
                            this.getCms().addUserToGroup(user.getName(),
                                    rolename);
                        }
                    }
                }
*/
                // after creating a parent OU watch out for child OUs
                createorgunits(requestcontext, offlineProject,
                    res.getRootPath(), ouname);
            }
        }
    }

	    /**
     * 
     * @param res the resource to check
     * @param flag the flag to check
     * 
     * @return <code>true</code> if the resource has the given flag set
     */
    public boolean hasFlag(CmsResource res, int flag) {

        return (res.getFlags() & flag) == flag;
    }

    protected void internalCreateDefaultGroups(String ouFqn,
        String ouDescription, boolean webuser) throws CmsException {
        // create roles
        // String rootAdminRole = CmsRole.ROOT_ADMIN.getGroupName();
        try {
            // only do something if really needed
            // if ((CmsOrganizationalUnit.getParentFqn(ouFqn) != null)
            // || ((CmsOrganizationalUnit.getParentFqn(ouFqn) == null) &&
            // !existsGroup(rootAdminRole))) {
            // System.out.println("create the roles in the given ou " + ouFqn);
            Iterator itRoles = CmsRole.getSystemRoles().iterator();

            while (itRoles.hasNext()) {
                CmsRole role = (CmsRole) itRoles.next();
				if (webuser && (role != CmsRole.ACCOUNT_MANAGER)) {
					// if webuser ou and not account manager role
					continue;
				}
				if (role.isOrganizationalUnitIndependent() && (CmsOrganizationalUnit.getParentFqn(ouFqn) != null)) {
					// if role is ou independent and not in the root ou
					continue;
				}
				String groupName = ouFqn + role.getGroupName();
				int flags = I_CmsPrincipal.FLAG_ENABLED |
					I_CmsPrincipal.FLAG_GROUP_ROLE;

				if ((role == CmsRole.WORKPLACE_USER) ||
						(role == CmsRole.PROJECT_MANAGER)) {
					flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_USER;
				}

				if (role == CmsRole.PROJECT_MANAGER) {
					flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER;
				}

				// System.out.println("creating group " + groupName);
				this.getCms()
					.createGroup(groupName, "A system role group", flags,
					null);
            }
        } catch (CmsException e) {
            throw new CmsInitException(Messages.get()
                                               .container(Messages.ERR_INITIALIZING_USER_DRIVER_0),
                e);
        }

        // create groups
        // String administratorsGroup = ouFqn
        // + OpenCms.getDefaultUsers().getGroupAdministrators();
        // String guestGroup = ouFqn +
        // OpenCms.getDefaultUsers().getGroupGuests();
        // String usersGroup = ouFqn +
        // OpenCms.getDefaultUsers().getGroupUsers();
        // String projectmanagersGroup = ouFqn
        // + OpenCms.getDefaultUsers().getGroupProjectmanagers();
        // String guestUser = ouFqn + OpenCms.getDefaultUsers().getUserGuest();
        // String adminUser = ouFqn + OpenCms.getDefaultUsers().getUserAdmin();
        // String exportUser = ouFqn +
        // OpenCms.getDefaultUsers().getUserExport();
        // String deleteUser = ouFqn
        // + OpenCms.getDefaultUsers().getUserDeletedResource();
        /*
         * if (existsGroup(dbc, administratorsGroup)) { if
         * (CmsOrganizationalUnit.getParentFqn(ouFqn) == null) { // check the
         * flags of existing groups, for compatibility checks
         * internalUpdateRoleGroup(dbc, administratorsGroup,
         * CmsRole.ROOT_ADMIN); internalUpdateRoleGroup(dbc, usersGroup,
         * CmsRole.WORKPLACE_USER .forOrgUnit(ouFqn));
         * internalUpdateRoleGroup(dbc, projectmanagersGroup,
         * CmsRole.PROJECT_MANAGER.forOrgUnit(ouFqn)); } return; }
         */

        /*
         * should exist from usergroups already String parentOu =
         * CmsOrganizationalUnit.getParentFqn(ouFqn); String parentGroup = null;
         * if (parentOu != null) { parentGroup = parentOu +
         * OpenCms.getDefaultUsers().getGroupUsers(); } String groupDescription =
         * (CmsStringUtil .isNotEmptyOrWhitespaceOnly(ouDescription) ?
         * CmsMacroResolver .localizedKeyMacro(
         * Messages.GUI_DEFAULTGROUP_OU_USERS_DESCRIPTION_1, new String[] {
         * ouDescription }) : CmsMacroResolver .localizedKeyMacro(
         * Messages.GUI_DEFAULTGROUP_ROOT_USERS_DESCRIPTION_0, null));
         * this.getCms().createGroup(usersGroup, groupDescription,
         * I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_PROJECT_USER |
         * CmsRole.WORKPLACE_USER.getVirtualGroupFlags(), parentGroup);
         */
    }
}
