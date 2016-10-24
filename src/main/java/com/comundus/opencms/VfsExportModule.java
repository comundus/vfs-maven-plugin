//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
package com.comundus.opencms;

import java.io.File;
import java.io.IOException;

import org.opencms.db.CmsDbEntryNotFoundException;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsRequestContext;
import org.opencms.i18n.CmsMessages;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsException;
import org.opencms.main.Messages;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.module.CmsModuleImportExportHandler;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
import org.xml.sax.SAXException;

/**
 * Adds a module to WEB-INF/config/opencms-modules.xml.
 */
public class VfsExportModule {
    /** The CmsObject. */
    private CmsObject cms;

    private String moduleVersion;

    private I_CmsReport report;

    /**
     *
     * Exports a module from the configured OpenCms
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param adminPassword
     *            password of user "Admin" performing the operation
     * @param moduleName
     *            name of the module
     * @param targetPath
     *            path of the folder where the module will be exported
     * @throws CmsException
     *             if anything OpenCms goes wrong
     * @throws IOException
     *             in case configuration files cannot be read
     * @throws SAXException
     *             in case configuration files cannot be parsed
     */
    public final void execute(final String webappDirectory,
        final String adminPassword, final String moduleName, final String targetPath)
        throws IOException, CmsException, SAXException {
    	if (null == moduleName) {
    		System.err.println("[WARN]VfsExportModule.execute(): moduleName param not defined");
    		return;
    	}
    	
        final String webinfdir = webappDirectory + File.separatorChar +
            "WEB-INF";
        final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir,
                "Admin", adminPassword);

        if (cmsshell != null) {
            this.cms = cmsshell.getCmsObject();

            final CmsRequestContext requestcontext = this.cms.getRequestContext();
            requestcontext.setCurrentProject(this.cms.readProject("Offline"));

            this.setReport(new CmsShellReport(requestcontext.getLocale()));

            exportModule(moduleName, targetPath, cmsshell.getCmsObject());
        } else {
            System.err.println("[WARN]VfsExportModule.execute(): CmsShell not available");
        }
    }

    /**
     * Taken from org.opencms.main.CmsShellCommands<p>
     * Exports the module with the given name to the default location.<p>
     *
     * @param moduleName the name of the module to export
     * @param targetFolder target folder
     * @param cms initialized {@code CmsObject}
     * @throws CmsException if something goes wrong
     */
    public void exportModule(String moduleName, String targetFolder, CmsObject cms) throws CmsException  {

    	I_CmsReport report = new CmsShellReport(cms.getRequestContext().getLocale());

        CmsModule module = OpenCms.getModuleManager().getModule(moduleName);

        if (module == null) {
            throw new CmsDbEntryNotFoundException(Messages.get().container(Messages.ERR_UNKNOWN_MODULE_1, moduleName));
        }

        createFolder(targetFolder);
        String filename = OpenCms.getSystemInfo().getAbsoluteRfsPathRelativeToWebInf(
        		targetFolder + moduleName + "_" +
        		OpenCms.getModuleManager().getModule(moduleName).getVersion().toString());

        String[] resources = new String[module.getResources().size()];
        System.arraycopy(module.getResources().toArray(), 0, resources, 0, resources.length);

        // generate a module export handler
        CmsModuleImportExportHandler moduleExportHandler = new CmsModuleImportExportHandler();
        moduleExportHandler.setFileName(filename);
        moduleExportHandler.setAdditionalResources(resources);
        moduleExportHandler.setModuleName(module.getName().replace('\\', '/'));
        moduleExportHandler.setDescription(getMessages().key(

            Messages.GUI_SHELL_IMPORTEXPORT_MODULE_HANDLER_NAME_1,
            new Object[] {moduleExportHandler.getModuleName()}));

        // export the module
        OpenCms.getImportExportManager().exportData(
            cms,
            moduleExportHandler, report);
        report("Module " + moduleName + " exported to: " + filename, I_CmsReport.FORMAT_HEADLINE);
    }

    private void createFolder(String folder) throws CmsException {

	File fFolder = new File(folder);
	if (fFolder.exists()) {
	    if (fFolder.isDirectory()) {
		//The folder already exists
		return;
	    } else {
		//A file with the name of the requested folder already exists
		throw new CmsException(Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1,
			"vfs:export-module: A file with the name of the requested folder already exists: " + fFolder));
	    }
	} else {
	    //Create the folder
	    if (!fFolder.mkdirs()) {
		throw new CmsException(Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1,
			"vfs:export-module: Could not create folder: "+fFolder));
	    }
	}
    }

    /**
     *
     * @param msg Message
     * @param format Format from the constants in {@link I_CmsReport}
     */
    protected void report(String msg, int format) {
	this.getReport()
	.println(org.opencms.report.Messages.get()
		.container(org.opencms.report.Messages.RPT_ARGUMENT_1,
			msg, format));
    }

    private CmsMessages getMessages() {
	return Messages.get().getBundle();
    }

    /**
     * gets the Cms Report.
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
