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
import org.opencms.importexport.Messages;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
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
     * Exports one single user with all its data.
     * <p>
     *
     * @param parent
     *            the parent node to add the users to
     * @param user
     *            the user to be exported
     * @throws CmsImportExportException
     *             if something goes wrong
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     */

    // code taken from org.opencms.importexport.CmsExport
    private void exportUser(final Element parent, final CmsUser user)
        throws CmsImportExportException, SAXException {
        try {
            // add user node to the manifest.xml
            final Element e = parent.addElement(CmsImportExportManager.N_USERDATA);
            e.addElement(CmsImportExportManager.N_NAME).addText(user.getName());

            // encode the password, using a base 64 decoder
            final String passwd = new String(Base64.encodeBase64(
                        user.getPassword().getBytes()));
            e.addElement(CmsImportExportManager.N_PASSWORD).addCDATA(passwd);
            //            e.addElement(CmsImportExportManager.N_DESCRIPTION)
            //             .addCDATA(user.getDescription());
            e.addElement(CmsImportExportManager.N_FIRSTNAME)
             .addText(user.getFirstname());
            e.addElement(CmsImportExportManager.N_LASTNAME)
             .addText(user.getLastname());
            e.addElement(CmsImportExportManager.N_EMAIL).addText(user.getEmail());
            e.addElement(CmsImportExportManager.N_FLAGS)
             .addText(Integer.toString(user.getFlags()));
            e.addElement(CmsImportExportManager.N_DATECREATED)
             .addText(Long.toString(user.getDateCreated()));

            final Element userInfoNode = e.addElement(CmsImportExportManager.N_USERINFO);
            final List keys = new ArrayList(user.getAdditionalInfo().keySet());
            Collections.sort(keys);

            final Iterator itInfoKeys = keys.iterator();

            while (itInfoKeys.hasNext()) {
                final String key = (String) itInfoKeys.next();

                if (key == null) {
                    continue;
                }

                final Object value = user.getAdditionalInfo(key);

                if (value == null) {
                    continue;
                }

                final Element entryNode = userInfoNode.addElement(CmsImportExportManager.N_USERINFO_ENTRY);
                entryNode.addAttribute(CmsImportExportManager.A_NAME, key);
                entryNode.addAttribute(CmsImportExportManager.A_TYPE,
                    value.getClass().getName());

                try {
                    // serialize the user info and write it into a file
                    entryNode.addCDATA(CmsDataTypeUtil.dataExport(value));
                } catch (final IOException ioe) {
                    getReport().println(ioe);
                }
            }

            // append the node for groups of user
            final List userGroups = getCms().getGroupsOfUser(user.getName(), true);
            final Element g = e.addElement(CmsImportExportManager.N_USERGROUPS);

            for (int i = 0; i < userGroups.size(); i++) {
                final String groupName = ((CmsGroup) userGroups.get(i)).getName();
                g.addElement(CmsImportExportManager.N_GROUPNAME)
                 .addElement(CmsImportExportManager.N_NAME).addText(groupName);
            }

            // write the XML
            digestElement(parent, e);
        } catch (final CmsException e) {
            throw new CmsImportExportException(e.getMessageContainer(), e);
        }
    }
}
