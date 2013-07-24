package com.comundus.opencms.vfs;

import java.io.File;

import java.util.List;


/**
 * A Maven2 plugin Goal to clean #synclist.txt from synchronisation source
 * folder.
 *
 * @goal clean
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class CleanMojo extends AbstractVfsMojo {
    /** Filename of the synclist file on the server FS. */
    static final String SYNCLIST_FILENAME = "#synclist.txt";

    /**
     * The source directory for VFS data.
     *
     * Contains a #synclist.txt which is NOT checked into version control.
     *
     * @parameter expression="${basedir}/src/main/vfs"
     * @required
     */
    private String syncSourceDirectory;

    /**
     * The List of folder paths in VFS to synchronize is used here only to make
     * sure it's an appropriate VFS project.
     *
     * @parameter
     */
    private List syncVFSPaths;

    /**
     * Removes #synclist.txt from source folder.
     *
     * Only if VFS synchronization paths are configured; otherwise it's assumed
     * to be a non-VFS project in a multi project build.
     */
    public final void execute() {
    	
    	if (this.isSkipVfs()){
    		this.getLog().info("Skipping VFS plugin");
    		return;
    	}
        if (this.syncVFSPaths == null) {
            this.getLog().info("Skipping non-vfs project");

            return; // it's ok, nothing to sync
        }

        File rfsFile = new File(this.syncSourceDirectory + File.separator +
                CleanMojo.SYNCLIST_FILENAME);
        rfsFile.delete();
    }
}
