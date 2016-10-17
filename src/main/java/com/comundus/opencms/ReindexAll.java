package com.comundus.opencms;

import java.io.File;

import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsHtmlReport;

/**
 * Performs all index reindexing.
 */
public class ReindexAll extends XmlHandling {
	/**
	 * Reindex all indexes
	 * 
	 * @param webappDirectory
	 *            path to WEB-INF of the OpenCms installation
	 * @param adminPassword
	 *            password of user "Admin" performing the operation
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public final void execute(final String webappDirectory,

	final String adminPassword) throws Exception {

		final String webinfdir = webappDirectory + File.separatorChar
				+ "WEB-INF";
		final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir,
				"Admin", adminPassword);

		final CmsHtmlReport report = new CmsHtmlReport(cmsshell.getCmsObject()
				.getRequestContext().getLocale(), cmsshell.getCmsObject()
				.getRequestContext().getSiteRoot());

		OpenCms.getSearchManager().rebuildAllIndexes(report);
	}
}
