package com.comundus.opencms;

import java.io.File;
import java.io.IOException;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsRequestContext;
import org.opencms.importexport.CmsImportParameters;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
import org.xml.sax.SAXException;

/**
 * Import module from ZIP - file to VFS
 */
public class VfsImportModule {
    /** The CmsObject. */
    private CmsObject cms;

    private I_CmsReport report;

    /**
     * 
     * Module a module from ZIP - file to OpenCms VFS.
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param adminPassword
     *            password of user "Admin" performing the operation
     * @param moduleFileName
     *            moduleDirectoryName module ZIP - file name
     * @param moduleFolderName
     *            absolute directory name to import several modules
     * @throws CmsException
     *             if anything OpenCms goes wrong
     * @throws IOException
     *             in case configuration files cannot be read
     * @throws SAXException
     *             in case configuration files cannot be parsed
     */
    public final void execute(final String webappDirectory, final String adminPassword, final String moduleFileName,
            final String moduleDirectoryName) throws IOException, CmsException, SAXException {
        if ((null == moduleFileName) && (null == moduleDirectoryName)) {
            System.err.println(
                    "[WARN]VfsImportModule.execute(): moduleFileName or moduleDirectoryName params not defined");
            return;
        }

        final String webinfdir = webappDirectory + File.separatorChar + "WEB-INF";
        final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir, "Admin", adminPassword);

        if (cmsshell != null) {
            this.cms = cmsshell.getCmsObject();

            final CmsRequestContext requestcontext = this.cms.getRequestContext();
            requestcontext.setCurrentProject(this.cms.readProject("Offline"));

            this.setReport(new CmsShellReport(requestcontext.getLocale()));

            // if single module file name defined
            if (null != moduleFileName) {
                importModule(moduleFileName, cmsshell.getCmsObject());
            }

            // if module directory name defined
            if (null != moduleDirectoryName) {
                final File moduleDir = new File(moduleDirectoryName);
                if (!moduleDir.isDirectory()) {
                    System.err.println("[WARN]VfsImportModule.execute(): moduleFolderName param is not a directory");
                    return;
                }

                for (File moduleFile : moduleDir.listFiles()) {
                    importModule(moduleFile.getAbsolutePath(), cmsshell.getCmsObject());
                }
            }
        } else {
            System.err.println("[WARN]VfsImportModule.execute(): CmsShell not available");
        }
    }

    public void importModule(String importFileName, CmsObject cms) throws CmsException {
        final I_CmsReport report = new CmsShellReport(cms.getRequestContext().getLocale());

        final CmsImportParameters params = new CmsImportParameters(importFileName, "/", true);

        OpenCms.getImportExportManager().importData(cms, report, params);

        report("Module from file \"" + importFileName + " imported", I_CmsReport.FORMAT_HEADLINE);
    }

    /**
     * 
     * @param msg
     * @param format
     */
    protected void report(String msg, int format) {
        this.getReport().println(
                org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1, msg, format));
    }

    /**
     * gets the Cms Report.
     * 
     * @return the Cms Report
     */
    public final I_CmsReport getReport() {
        return this.report;
    }

    /**
     * Set the report to use for synchronisation.
     *
     * Used when OpenCms integrated synchronisation is called.
     *
     * @param preport
     *            the report to use
     */
    public final void setReport(final I_CmsReport preport) {
        this.report = preport;
    }

}
