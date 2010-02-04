package com.comundus.opencms;


import org.opencms.file.CmsObject;

import org.opencms.main.CmsException;

import org.opencms.report.I_CmsReport;
import org.opencms.synchronize.CmsSynchronizeException;
import org.opencms.synchronize.CmsSynchronizeSettings;
import org.opencms.synchronize.I_CmsSynchronize;
import org.opencms.synchronize.Messages;

import java.io.File;


/**
 * Contains all methods to synchronize the VFS with the "real" FS.
 * <p>
 * This is the comundus overwritten version of this OpenCms class that links
 * OpenCms synchronization to the CmOpencmsShell metadata synchronization
 * format.
 */
//CHANGED BY COMUNDUS
//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class CmsSynchronizeMaven implements I_CmsSynchronize {
    /** The CmsObject. */
    private CmsObject cms;

    /** The report to write the output to. */
    private I_CmsReport report;

    /**
     * Creates a new CmsSynchronize object which automatically start the
     * synchronisation process.
     * <p>
     *
     * @param cmsobj
     *            the current CmsObject
     * @param settings
     *            the synchonization settings to use
     * @param rprt
     *            the report to write the output to
     *
     * @throws CmsException
     *             if anything goes wrong
     */
    public void synchronize(final CmsObject cmsobj,
        final CmsSynchronizeSettings settings, final I_CmsReport rprt)
        throws CmsException {
        this.cms = cmsobj;
        this.report = rprt;

        // do the synchronization only if the synchronization folders in the VFS
        // and the FS are valid
        if ((settings != null) && (settings.isSyncEnabled())) {
            // store the current site root
            // OpenCms no longer does so since 6.2.3
            //this.cms.getRequestContext().saveSiteRoot();
            // set site to root site
            this.cms.getRequestContext().setSiteRoot("/");

            // get the destination folder
            final String destinationPathInRfs = settings.getDestinationPathInRfs();

            // check if target folder exists and is writeable
            final File destinationFolder = new File(destinationPathInRfs);

            if (!destinationFolder.exists() ||
                    !destinationFolder.isDirectory()) {
                // destination folder does not exist
                throw new CmsSynchronizeException(Messages.get()
                                                          .container(Messages.ERR_RFS_DESTINATION_NOT_THERE_1,
                        destinationPathInRfs));
            }

            if (!destinationFolder.canWrite()) {
                // destination folder can't be written to
                throw new CmsSynchronizeException(Messages.get()
                                                          .container(Messages.ERR_RFS_DESTINATION_NO_WRITE_1,
                        destinationPathInRfs));
            }

            final VfsSync mysync = new VfsSync();
            mysync.setDestinationPathInRfs(destinationPathInRfs);
            mysync.setMetadataPathInRfs(destinationPathInRfs + "-metadata");
            mysync.setCms(this.cms);
            mysync.setReport(this.report);
            mysync.doTheSync(settings.getSourceListInVfs());
        }
    }
}
