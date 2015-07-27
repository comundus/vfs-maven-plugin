package com.comundus.opencms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Element;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.importexport.CmsImportExportException;
import org.opencms.importexport.CmsImportExportManager;
import org.opencms.importexport.CmsImportVersion7;
import org.opencms.importexport.Messages;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
import org.opencms.security.CmsRole;
import org.opencms.util.CmsDataTypeUtil;
import org.xml.sax.SAXException;


/**
 * Exports OpenCms user/group data.
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class VfsUserExport extends XmlHandling {
    /**
     * Exports user/group-data from OpenCms to a "usergroups.xml" file in the given sources folder.
     *
     * taken from org.opencms.importexport.CmsExport.
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param adminPassword
     *            password of user "Admin" performing the operation
     * @param ugSourceDirectory
     *            the directory to create the userdata in
     * @throws Exception
     *             if anything goes wrong
     */
    public final void execute(final String webappDirectory,
        final String adminPassword, final String ugSourceDirectory)
        throws Exception {
        final String webinfdir = webappDirectory + File.separatorChar +
            "WEB-INF";
        final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir,
                "Admin", adminPassword);
        setCms(cmsshell.getCmsObject());

        final CmsRequestContext requestcontext = getCms().getRequestContext();
        setReport(new CmsShellReport(requestcontext.getLocale()));
        setUsergroupsSourceDirectory(ugSourceDirectory);
        setExportFileName(ugSourceDirectory + File.separatorChar +
            "usergroups.xml");

        // code taken from org.opencms.importexport.CmsExport
        try {
            final Element exportNode = this.openExportFile(new File(
                        getExportFileName()));
            final Element userGroupData = exportNode.addElement(CmsImportExportManager.N_USERGROUPDATA);
            this.getSaxWriter().writeOpen(userGroupData);
            this.exportGroups(userGroupData);
            this.exportUsers(userGroupData);
            this.getSaxWriter().writeClose(userGroupData);
            this.closeExportFile(exportNode);
        } catch (final SAXException se) {
            getReport().println(se);

            final CmsMessageContainer message = Messages.get()
                                                        .container(Messages.ERR_IMPORTEXPORT_ERROR_EXPORTING_TO_FILE_1,
                    this.getExportFileName());
            throw new CmsImportExportException(message, se);
        } catch (final IOException ioe) {
            getReport().println(ioe);

            final CmsMessageContainer message = Messages.get()
                                                        .container(Messages.ERR_IMPORTEXPORT_ERROR_EXPORTING_TO_FILE_1,
                    this.getExportFileName());
            throw new CmsImportExportException(message, ioe);
        }
    }

    /**
     * Exports all groups with all data.
     * <p>
     *
     * @param parent
     *            the parent node to add the groups to
     * @throws CmsImportExportException
     *             if something goes wrong
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     */

    // code taken from org.opencms.importexport.CmsExport
    private void exportGroups(final Element parent)
        throws CmsImportExportException, SAXException {
        try {
            final List allGroups = OpenCms.getOrgUnitManager()
                                    .getGroups(getCms(), "/", true);

            for (int i = 0, l = allGroups.size(); i < l; i++) {
                final CmsGroup group = (CmsGroup) allGroups.get(i);
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_SUCCESSION_2,
                        String.valueOf(i + 1), String.valueOf(l)),
                    I_CmsReport.FORMAT_NOTE);
                getReport()
                    .print(Messages.get().container(Messages.RPT_EXPORT_GROUP_0),
                    I_CmsReport.FORMAT_NOTE);
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        group.getName()));
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_DOTS_0));
                this.exportGroup(parent, group);
                getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            }
        } catch (final CmsImportExportException e) {
            throw e;
        } catch (final CmsException e) {
            throw new CmsImportExportException(e.getMessageContainer(), e);
        }
    }

    /**
     * Exports all users with all data.
     * <p>
     *
     * @param parent
     *            the parent node to add the users to
     * @throws CmsImportExportException
     *             if something goes wrong
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     */

    // code taken from org.opencms.importexport.CmsExport
    private void exportUsers(final Element parent)
        throws CmsImportExportException, SAXException {
        try {
            final List allUsers = OpenCms.getOrgUnitManager()
                                         .getUsers(getCms(), "", true);

            for (int i = 0, l = allUsers.size(); i < l; i++) {
                final CmsUser user = (CmsUser) allUsers.get(i);
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_SUCCESSION_2,
                        String.valueOf(i + 1), String.valueOf(l)),
                    I_CmsReport.FORMAT_NOTE);
                getReport()
                    .print(Messages.get().container(Messages.RPT_EXPORT_USER_0),
                    I_CmsReport.FORMAT_NOTE);
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        user.getName()));
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_DOTS_0));
                this.exportUser(parent, user);
                getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            }
        } catch (final CmsImportExportException e) {
            throw e;
        } catch (final CmsException e) {
            throw new CmsImportExportException(e.getMessageContainer(), e);
        }
    }

    /**
     * Exports one single group with all it's data.
     * <p>
     *
     * @param parent
     *            the parent node to add the groups to
     * @param group
     *            the group to be exported
     * @throws CmsImportExportException
     *             if something goes wrong
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     */

    // code taken from org.opencms.importexport.CmsExport
    private void exportGroup(final Element parent, final CmsGroup group)
        throws CmsImportExportException, SAXException {
        try {
            String parentgroup;

            if (group.getParentId().isNullUUID()) {
                parentgroup = "";
            } else {
                parentgroup = getCms().getParent(group.getName()).getName();
            }

            final Element e = parent.addElement(CmsImportExportManager.N_GROUPDATA);
            e.addElement(CmsImportExportManager.N_NAME).addText(group.getName());
            e.addElement(CmsImportExportManager.N_DESCRIPTION)
             .addCDATA(group.getDescription());
            e.addElement(CmsImportExportManager.N_FLAGS)
             .addText(Integer.toString(group.getFlags()));
            e.addElement(CmsImportExportManager.N_PARENTGROUP)
             .addText(parentgroup);
            // write the XML
            this.digestElement(parent, e);
        } catch (final CmsException e) {
            final CmsMessageContainer message = org.opencms.db.Messages.get()
                                                                       .container(org.opencms.db.Messages.ERR_GET_PARENT_GROUP_1,
                    group.getName());
            throw new CmsImportExportException(message, e);
        }
    }
    
    /**
     * Exports one single user with all its data.<p>
     * 
     * @param parent the parent node to add the users to
     * @param user the user to be exported
     * 
     * @throws CmsImportExportException if something goes wrong
     * @throws SAXException if something goes wrong processing the manifest.xml
     */
    // code taken from org.opencms.importexport.CmsExport - OpenCms Version 9.5.1
    protected void exportUser(Element parent, CmsUser user) throws CmsImportExportException, SAXException {

        try {
            // add user node to the manifest.xml
            Element e = parent.addElement(CmsImportVersion7.N_USER);
            e.addElement(CmsImportVersion7.N_NAME).addText(user.getSimpleName());
            // encode the password, using a base 64 decoder
            String passwd = new String(Base64.encodeBase64(user.getPassword().getBytes()));
            e.addElement(CmsImportVersion7.N_PASSWORD).addCDATA(passwd);
            e.addElement(CmsImportVersion7.N_FIRSTNAME).addText(user.getFirstname());
            e.addElement(CmsImportVersion7.N_LASTNAME).addText(user.getLastname());
            e.addElement(CmsImportVersion7.N_EMAIL).addText(user.getEmail());
            e.addElement(CmsImportVersion7.N_FLAGS).addText(Integer.toString(user.getFlags()));
            e.addElement(CmsImportVersion7.N_DATECREATED).addText(Long.toString(user.getDateCreated()));

            Element userInfoNode = e.addElement(CmsImportVersion7.N_USERINFO);
            List<String> keys = new ArrayList<String>(user.getAdditionalInfo().keySet());
            Collections.sort(keys);
            Iterator<String> itInfoKeys = keys.iterator();
            while (itInfoKeys.hasNext()) {
                String key = itInfoKeys.next();
                if (key == null) {
                    continue;
                }
                Object value = user.getAdditionalInfo(key);
                if (value == null) {
                    continue;
                }
                Element entryNode = userInfoNode.addElement(CmsImportVersion7.N_USERINFO_ENTRY);
                entryNode.addAttribute(CmsImportVersion7.A_NAME, key);
                entryNode.addAttribute(CmsImportVersion7.A_TYPE, value.getClass().getName());
                try {
                    // serialize the user info and write it into a file
                    entryNode.addCDATA(CmsDataTypeUtil.dataExport(value));
                } catch (IOException ioe) {
                    getReport().println(ioe);
                    if (LOG.isErrorEnabled()) {
                        LOG.error(
                            Messages.get().getBundle().key(
                                Messages.ERR_IMPORTEXPORT_ERROR_EXPORTING_USER_1,
                                user.getName()),
                            ioe);
                    }
                }
            }

            // append node for roles of user
            Element userRoles = e.addElement(CmsImportVersion7.N_USERROLES);
            List<CmsRole> roles = OpenCms.getRoleManager().getRolesOfUser(
                getCms(),
                user.getName(),
                "",
                true,
                true,
                true);
            for (int i = 0; i < roles.size(); i++) {
                String roleName = roles.get(i).getFqn();
                userRoles.addElement(CmsImportVersion7.N_USERROLE).addText(roleName);
            }
            // append the node for groups of user
            Element userGroups = e.addElement(CmsImportVersion7.N_USERGROUPS);
            List<CmsGroup> groups = getCms().getGroupsOfUser(user.getName(), true, true);
            for (int i = 0; i < groups.size(); i++) {
                String groupName = groups.get(i).getName();
                userGroups.addElement(CmsImportVersion7.N_USERGROUP).addText(groupName);
            }
            // write the XML
            digestElement(parent, e);
        } catch (CmsException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getLocalizedMessage(), e);
            }
            throw new CmsImportExportException(e.getMessageContainer(), e);
        }
    }
    
    private ConsoleLog LOG = new ConsoleLog();

    private class ConsoleLog {
        
        public boolean isDebugEnabled() {
            return true;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public void debug(String message, Throwable t) {
            VfsUserExport.this.reportException(message, t);
        }
        
        public void error(String message, Throwable t) {
            VfsUserExport.this.reportException(message, t);
        }
    }
}
