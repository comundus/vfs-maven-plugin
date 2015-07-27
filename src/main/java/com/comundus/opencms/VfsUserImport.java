package com.comundus.opencms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
import org.opencms.security.I_CmsPasswordHandler;
import org.opencms.util.CmsDataTypeUtil;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import org.opencms.xml.CmsXmlUtils;


/**
 * Imports OpenCms user/group data.
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class VfsUserImport extends XmlHandling {
    /** Groups to create during import are stored here. */
    private Stack groupsToCreate;

    /**
     * Imports user/group-data from the given sources folder to OpenCms. Already
     * existing users/groups are skipped.
     *
     * @see VfsUserExport
     *
     * taken from org.opencms.importexport.CmsImportVersion6
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param adminPassword
     *            password of user "Admin" performing the operation
     * @param ugSourceDirectory
     *            the directory to read userdata from
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

        // code taken from org.opencms.importexport.CmsImportVersion4
        final CmsRequestContext requestcontext = getCms().getRequestContext();
        setReport(new CmsShellReport(requestcontext.getLocale()));
        setUsergroupsSourceDirectory(ugSourceDirectory);
        setExportFileName(ugSourceDirectory + File.separatorChar +
            "usergroups.xml");

        final File exportFile = new File(getExportFileName());

        if (exportFile.exists()) {
            // read the xml-config file
            setDocXml(CmsXmlUtils.unmarshalHelper(CmsFileUtil.readFile(
                        exportFile), null));
            this.initialize();
            this.importGroups();
            this.importUsers();
        }
    }

    /**
     * Imports a single group.
     * <p>
     *
     * @param name
     *            the name of the group
     * @param description
     *            group description
     * @param flags
     *            group flags
     * @param parentgroupName
     *            name of the parent group
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private void importGroup(final String name, final String description,
        final String flags, final String parentgroupName) {
        String notnulldescription = description;

        if (notnulldescription == null) {
            notnulldescription = "";
        }

        CmsGroup parentGroup = null;

        if (CmsStringUtil.isNotEmpty(parentgroupName)) {
            try {
                parentGroup = getCms().readGroup(parentgroupName);
            } catch (final CmsException exc) {
                // parentGroup will be null
            }
        }

        if (CmsStringUtil.isNotEmpty(parentgroupName) && (parentGroup == null)) {
            // cannot create group, put on stack and try to create later
            final Map groupData = new HashMap();
            groupData.put(CmsImportExportManager.N_NAME, name);
            groupData.put(CmsImportExportManager.N_DESCRIPTION,
                notnulldescription);
            groupData.put(CmsImportExportManager.N_FLAGS, flags);
            groupData.put(CmsImportExportManager.N_PARENTGROUP, parentgroupName);
            this.groupsToCreate.push(groupData);
        } else {
            try {
                getReport()
                    .print(Messages.get().container(Messages.RPT_IMPORT_GROUP_0),
                    I_CmsReport.FORMAT_NOTE);
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        name));
                getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_DOTS_0));
                getCms()
                    .createGroup(name, notnulldescription,
                    Integer.parseInt(flags), parentgroupName);
                getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            } catch (final CmsException exc) {
                getReport()
                    .println(Messages.get().container(Messages.RPT_NOT_CREATED_0),
                    I_CmsReport.FORMAT_OK);
            }
        }
    }

    /**
     * Imports the OpenCms groups.
     * <p>
     *
     * @throws CmsImportExportException
     *             if something goes wrong
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private void importGroups() throws CmsImportExportException {
        List groupNodes;
        Element currentElement;
        String name;
        String description;
        String flags;
        String parentgroup;

        try {
            // getAll group nodes
            groupNodes = getDocXml()
                             .selectNodes("//" +
                    CmsImportExportManager.N_GROUPDATA);

            // walk through all groups in manifest
            for (int i = 0; i < groupNodes.size(); i++) {
                currentElement = (Element) groupNodes.get(i);
                name = XmlHandling.getChildElementTextValue(currentElement,
                        CmsImportExportManager.N_NAME);
                name = OpenCms.getImportExportManager().translateGroup(name);
                description = XmlHandling.getChildElementTextValue(currentElement,
                        CmsImportExportManager.N_DESCRIPTION);
                flags = XmlHandling.getChildElementTextValue(currentElement,
                        CmsImportExportManager.N_FLAGS);
                parentgroup = XmlHandling.getChildElementTextValue(currentElement,
                        CmsImportExportManager.N_PARENTGROUP);

                if ((parentgroup != null) && (parentgroup.length() > 0)) {
                    parentgroup = OpenCms.getImportExportManager()
                                         .translateGroup(parentgroup);
                }

                // import this group
                this.importGroup(name, description, flags, parentgroup);
            }

            // now try to import the groups in the stack
            while (!this.groupsToCreate.empty()) {
                final Stack tempStack = this.groupsToCreate;
                this.groupsToCreate = new Stack();

                while (tempStack.size() > 0) {
                    final Map groupdata = (HashMap) tempStack.pop();
                    name = (String) groupdata.get(CmsImportExportManager.N_NAME);
                    description = (String) groupdata.get(CmsImportExportManager.N_DESCRIPTION);
                    flags = (String) groupdata.get(CmsImportExportManager.N_FLAGS);
                    parentgroup = (String) groupdata.get(CmsImportExportManager.N_PARENTGROUP);
                    // try to import the group
                    this.importGroup(name, description, flags, parentgroup);
                }
            }

            // } catch (CmsImportExportException e) {
            // throw e;
        } catch (final Exception e) {
            getReport().println(e);

            final CmsMessageContainer message = Messages.get()
                                                        .container(Messages.ERR_IMPORTEXPORT_ERROR_IMPORTING_GROUPS_0);
            throw new CmsImportExportException(message, e);
        }
    }

    /**
     * Imports a single user.<p>
     *
     * @param name user name
     * @param flags user flags
     * @param password user password
     * @param firstname firstname of the user
     * @param lastname lastname of the user
     * @param email user email
     * @param dateCreated creation date
     * @param userInfo user info
     * @param userGroups user groups
     *
     */

    // code taken from org.opencms.importexport.A_CmsImport
    protected final void importUser(final String name, final String flags,
        final String password, final String firstname, final String lastname,
        final String email, final long dateCreated, final Map userInfo,
        final List userGroups) {
	
        try {
            CmsUser user = getCms().readUser(name);
            // if successful the user already exists and might be updated
            user.setFlags(Integer.parseInt(flags));
            if(!"".equals(firstname)) user.setPassword(password);
            if(!"".equals(firstname)) user.setFirstname(firstname);
            if(!"".equals(lastname)) user.setLastname(lastname);
            if(!"".equals(email)) user.setEmail(email);
            // the method is not visible - possibly it doesn't make sense anyway to change the type of an existing user user.setType(Integer.parseInt(type));
            user.setAdditionalInfo(userInfo); 
            // currently for existing users we don't care about: final List userGroups 
            getCms().writeUser(user);
            return;
        } catch(CmsException e) {
            // intentionally left blank
        }

        // create a new user id
        final String id = new CmsUUID().toString();

        try {
            getReport()
                .print(Messages.get().container(Messages.RPT_IMPORT_USER_0),
                I_CmsReport.FORMAT_NOTE);
            getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                    name));
            getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_DOTS_0));
            getCms()
                .importUser(id, name, password, firstname, lastname, email,
                Integer.parseInt(flags), dateCreated, userInfo);
        } catch (final CmsException exc) {
            getReport()
                .println(Messages.get().container(Messages.RPT_NOT_CREATED_0),
                I_CmsReport.FORMAT_OK);
        }

        // add user to all groups list
        for (int i = 0; i < userGroups.size(); i++) {
            String groupName = (String) userGroups.get(i);
            try {
                CmsGroup group = getCms().readGroup(groupName);
                if (group.isVirtual() || group.isRole()) {
                    final CmsRole role = CmsRole.valueOf(group);
                    OpenCms.getRoleManager().addUserToRole(getCms(), role, name);
                } else {
                    getCms().addUserToGroup(name, groupName);
                }
             // parent group checking for roles ADDED BY COMUNDUS
                CmsGroup parentGroup = this.getCms().getParent(groupName);
                while(parentGroup != null) {
                    group = parentGroup;
                    groupName = group.getName();
                    if (group.isVirtual() || group.isRole()) {
                        final CmsRole role = CmsRole.valueOf(group);
                        OpenCms.getRoleManager().addUserToRole(getCms(), role, name);
                    }
                    parentGroup = this.getCms().getParent(groupName);
                }
            } catch (final CmsException exc) {
                getReport()
                    .println(Messages.get()
                                     .container(Messages.RPT_USER_COULDNT_BE_ADDED_TO_GROUP_2,
                        name, groupName), I_CmsReport.FORMAT_WARNING);
            }
        }

        getReport()
            .println(org.opencms.report.Messages.get()
                                                .container(org.opencms.report.Messages.RPT_OK_0),
            I_CmsReport.FORMAT_OK);
    }

    /**
     * Imports the OpenCms users.
     *
     * @throws IOException if extra info data cannot ber parsed
     * @throws ClassNotFoundException if a type for extra infos is not found
     *
     */

    // code taken from org.opencms.importexport.CmsImportVersion6 - Adapted to work with Version 7 (OpenCms 9.5.1)
    private void importUsers() throws IOException, ClassNotFoundException {
        // getAll user nodes
        @SuppressWarnings("unchecked")
        final List<Element> userNodes = getDocXml()
                                   .selectNodes("//" + CmsImportVersion7.N_USER);

        // walk threw all groups in manifest
        for (int i = 0; i < userNodes.size(); i++) {
            final Element currentElement = (Element) userNodes.get(i);

            String name = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_NAME);
            name = OpenCms.getImportExportManager().translateUser(name);

            // decode passwords using base 64 decoder
            final String pwd = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_PASSWORD);
            final String password = new String(Base64.decodeBase64(
                        pwd.trim().getBytes()));

            final String flags = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_FLAGS);
            final String firstname = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_FIRSTNAME);
            final String lastname = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_LASTNAME);
            final String email = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_EMAIL);
            final long dateCreated = Long.parseLong(XmlHandling.getChildElementTextValue(
                        currentElement, CmsImportExportManager.N_DATECREATED));

            // get the userinfo and put it into the additional info map
            final Map userInfo = new HashMap();
            final Iterator itInfoNodes = currentElement.selectNodes("./" +
                    CmsImportExportManager.N_USERINFO + "/" +
                    CmsImportExportManager.N_USERINFO_ENTRY).iterator();

            while (itInfoNodes.hasNext()) {
                final Element infoEntryNode = (Element) itInfoNodes.next();
                final String key = infoEntryNode.attributeValue(CmsImportExportManager.A_NAME);
                final String type = infoEntryNode.attributeValue(CmsImportExportManager.A_TYPE);
                final String value = infoEntryNode.getTextTrim();
                userInfo.put(key, CmsDataTypeUtil.dataImport(value, type));
            }

            // get the groups of the user and put them into the list
            final List groupNodes = currentElement.selectNodes("*/" +
                    CmsImportVersion7.N_USERGROUP);
            final List userGroups = new ArrayList();

            for (int j = 0; j < groupNodes.size(); j++) {
                final Element currentGroup = (Element) groupNodes.get(j);
                String userInGroup = XmlHandling.getChildElementTextValue(currentGroup,
                        CmsImportExportManager.N_NAME);
                userInGroup = OpenCms.getImportExportManager()
                                     .translateGroup(userInGroup);
                userGroups.add(userInGroup);
            }

            // import this user
            importUser5(name, flags, password, firstname, lastname, email,
                dateCreated, userInfo, userGroups);
        }
    }

    /**
     * ImportUser taken from CmsImportVersion5.
     * @see org.opencms.importexport.A_CmsImport#importUser(String, String, String, String, String, String, long, Map, List)
     */

    // code taken from CmsImportVersion5
    protected void importUser5(final String name, final String flags,
        String password, final String firstname, final String lastname,
        final String email, final long dateCreated, final Map userInfo,
        final List userGroups) {
        boolean convert = false;

        final Map config = OpenCms.getPasswordHandler().getConfiguration();

        if ((config != null) &&
                config.containsKey(I_CmsPasswordHandler.CONVERT_DIGEST_ENCODING)) {
            convert = Boolean.valueOf((String) config.get(
                        I_CmsPasswordHandler.CONVERT_DIGEST_ENCODING))
                             .booleanValue();
        }

        if (convert) {
            password = convertDigestEncoding(password);
        }

        importUser(name, flags, password, firstname, lastname, email,
            dateCreated, userInfo, userGroups);
    }

    /**
     * Converts a given digest to base64 encoding.<p>
     *
     * @param value the digest value in the legacy encoding
     * @return the digest in the new encoding
     */

    // code taken from org.opencms.importexport.A_CmsImport
    public String convertDigestEncoding(final String value) {
        final byte[] data = new byte[value.length() / 2];

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Integer.parseInt(value.substring(i * 2,
                        (i * 2) + 2), 16) - 128);
        }

        return new String(Base64.encodeBase64(data));
    }

    /**
     * Initializes all member variables before the import is started.
     * <p>
     *
     * This is required since there is only one instance for each import version
     * that is kept in memory and reused.
     * <p>
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private void initialize() {
        this.groupsToCreate = new Stack();
    }
}
