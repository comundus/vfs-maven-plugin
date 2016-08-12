//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
package com.comundus.opencms;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.opencms.db.CmsDbIoException;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.flex.CmsFlexCache;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.importexport.CmsImportExportException;
import org.opencms.importexport.CmsImportExportManager;
import org.opencms.importexport.CmsImportVersion7;
import org.opencms.loader.CmsLoaderException;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.relations.CmsRelationType;
import org.opencms.relations.I_CmsLinkParseable;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsRole;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.synchronize.CmsSynchronizeException;
import org.opencms.synchronize.CmsSynchronizeList;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsUUID;
import org.opencms.xml.CmsXmlUtils;
import org.xml.sax.SAXException;

import com.comundus.opencms.vfs.SyncResource;

/**
 * Performs VFS synchronisation.
 *
 * methods taken from org.opencms.synchronize.CmsSynchronize and from
 * org.opencms.importexport.CmsExport
 */
public class VfsSync extends XmlHandling {
    /** Flag to export a resource from the VFS to the FS. */
    static final int EXPORT_FROM_VFS = 1;

    /** Flag to import a resource from the FS to the VFS. */
    static final int UPDATE_IN_VFS = 2;

    /** Flag to import a deleted resource in the VFS. */
    static final int DELETE_FROM_VFS = 3;

    /** Filename of the synclist file on the server FS. */
    static final String SYNCLIST_FILENAME = "#synclist.txt";

    /** The files and directories in the RFS with these name patterns will be ignored. The original list is taken from ANT:
     * http://ant.apache.org/manual/dirtasks.html#defaultexcludes */
    private static final String[]  DEFAULT_IGNORED_NAMES = new String[] {
	"*~", "#*#", ".#*", "%*%", "._*", "CVS", ".cvsignore", "SCCS", "vssver.scc", ".svn", ".DS_Store",
	".git", ".gitattributes", ".gitignore", ".gitmodules", ".hg", ".hgignore", ".hgsub", ".hgsubstate", ".hgtags",
	".bzr", ".bzrignore"};

    /**
     * The path in the "real" file system where the resources have to be
     * synchronized to.
     */
    private String destinationPathInRfs;

    /**
     * The path in the "real" file system where the metadata have to be
     * synchronized to.
     */
    private String metadataPathInRfs;

    /** Counter for logging. */
    private int count;

    /** Hashmap for the synchronisation list of the last sync process. */
    private Map syncList;

    /** Hashmap for the new synchronisation list of the current sync process. */
    private Map newSyncList;

    /**
     * as we do not remove files from RFS we need to keep this List of
     * removables.
     */
    private List removeRfsList;

    /** Stores all relations defined in the import file to be created after all resources has been imported. */
    private Map m_importedRelations;

    /** Stores all resources of any type that implements the {@link I_CmsLinkParseable} interface. */
    private List m_parseables;

    private FileFilter ignoredFilesFilter;

    /**
     * Synchronizes a given List of paths in VFS with a path in RFS; a second
     * path in RFS stores metadata for the VFS files. Metadata folder structure
     * and content folder structure reflect the structure in VFS. Metadata for
     * files are stored in files with their name equivalent to the content file
     * plus an additional ".xml" extension. Metadata for folders is stored
     * inside of them in a file "~folder.xml". Like the original OpenCms
     * synchronization data about the last synchronization status get stored in
     * a file "#synclist.txt" in the uppermost content folder. "#synclist.txt"
     * is NOT stored in version control as it reflects the local
     * synchronisation state which must not be transfered to other developers.
     * XML Metadata corresponds to the OpenCms Import/Export format version 4.
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param dPathInRfs
     *            path in RFS for file contents
     * @param mPathInRfs
     *            path in RFS for metadata files
     * @param syncVFSPaths
     *            List of paths in VFS to synchronize
     * @param syncResources
     *            List of {@code <syncResource>} with resources to synchronize
     * @param ignoredNames
     *            List of name patterns to add to the ignored list
     * @param notIgnoredNames
     *            List of name patterns to remove from the ignored list
     * @param adminPassword
     *            password of user "Admin" performing the operation
     * @throws Exception
     *             if anything goes wrong
     */
    public final void execute(final String webappDirectory,
	    final String dPathInRfs, final String mPathInRfs,
	    final List<String> syncVFSPaths, List<SyncResource> syncResources,
	    final List<String> ignoredNames, List<String> notIgnoredNames,
	    final String adminPassword)
	    throws Exception {

	this.destinationPathInRfs = dPathInRfs;
	this.metadataPathInRfs = mPathInRfs;
	this.count = 1;

	this.m_parseables = new ArrayList();
	this.m_importedRelations = new HashMap();

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
	// do the synchronization only if the synchronization folders in
	// the VFS and the FS are valid
	// store the current site root
	// I think we don't need that here
	// requestcontext.saveSiteRoot();
	// set site to root site
	requestcontext.setSiteRoot("/");

	// code taken from org.opencms.synchronize.CmsSynchronize
	// OpenCms.fireCmsEvent(new
	// CmsEvent(I_CmsEventListener.EVENT_CLEAR_CACHES,
	// Collections.EMPTY_MAP));
	// OpenCms.fireCmsEvent(new
	// CmsEvent(I_CmsEventListener.EVENT_CLEAR_OFFLINE_CACHES,
	// Collections.EMPTY_MAP));
	// check if target folder exists and is writeable
	final File destinationFolder = new File(this.destinationPathInRfs);

	if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
	    // destination folder does not exist
	    throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
		    .container(org.opencms.synchronize.Messages.ERR_RFS_DESTINATION_NOT_THERE_1,
			    this.destinationPathInRfs));
	}

	if (!destinationFolder.canWrite()) {
	    // destination folder can't be written to
	    throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
		    .container(org.opencms.synchronize.Messages.ERR_RFS_DESTINATION_NO_WRITE_1,
			    this.destinationPathInRfs));
	}

	// clear all caches
	// m_report.println(Messages.get().container(org.opencms.importexport.Messages.RPT_CLEARCACHE_0),
	// I_CmsReport.FORMAT_NOTE);
	// OpenCms.fireCmsEvent(new
	// CmsEvent(I_CmsEventListener.EVENT_CLEAR_CACHES,
	// Collections.EMPTY_MAP));
	syncResources = mergeSyncResourceLists(syncVFSPaths, syncResources);

	computeIgnoredNames(ignoredNames, notIgnoredNames);

	doTheSync(syncResources);
	rewriteParseables();
	importRelations();

	// clear all OpenCms caches
	clearAllCaches();

	this.getCms().unlockProject(offlineProject.getUuid());
    }

	private void clearAllCaches() {
		OpenCms.fireCmsEvent(I_CmsEventListener.EVENT_CLEAR_CACHES,
				Collections.<String, Object> emptyMap());
		OpenCms.fireCmsEvent(new CmsEvent(
				I_CmsEventListener.EVENT_FLEX_PURGE_JSP_REPOSITORY, Collections
						.<String, Object> emptyMap()));
		OpenCms.fireCmsEvent(new CmsEvent(
				I_CmsEventListener.EVENT_FLEX_CACHE_CLEAR, Collections
						.<String, Object> singletonMap("action", new Integer(
								CmsFlexCache.CLEAR_ENTRIES))));
	}

    /*
    Methodenabfolge der Synchronisation (pro konfiguriertem VFS Pfad):
    syncVfsToRfs(sourcePathInVfs);          (recursive)
       exportToRfs(res);   (folders & files)
       deleteFromVfs(res); (folders & files)
       updateFromRfs(res); (files only)
    removeFromRfs(m_destinationPathInRfs);  (recursive)
    copyFromRfs((String) i.next());         (recursive)
       importToVfs()
    */

    /**
     * Executes the synchronization after all necessary fields have been filled
     * and checks have been done. This is the entry point for the integration of
     * this synchronization in OpenCms.
     *
     * @param syncResources
     *            List of paths in VFS to synchronize
     * @throws CmsException
     *             if anything goes wrong
     */
    public final void doTheSync(final List<SyncResource> syncResources)
        throws CmsException {

        // create the sync list for this run
        this.syncList = this.readSyncList();
        this.newSyncList = new HashMap();
        this.removeRfsList = new ArrayList();

        for (SyncResource sourcePathInVfs:syncResources) {
            // iterate through all configured VFS folders
            //final String sourcePathInVfs = (String) i.next();
            final String destPath = this.destinationPathInRfs +
                sourcePathInVfs.getResource().replace('/', File.separatorChar);
            this.getReport()
                .println(org.opencms.workplace.threads.Messages.get()
                                            .container(org.opencms.workplace.threads.Messages.RPT_SYNCHRONIZE_FOLDERS_2,
                    sourcePathInVfs, destPath), I_CmsReport.FORMAT_HEADLINE);
            // iterating thru VFS
            // possible actions: exportToRfs(res), updateFromRfs(res),
            // deleteFromVfs(res)
            // any entry touched so far is moved from m_syncList to
            // m_newSyncList
            // so, entries remaining in m_synclist afterwards
            // do no longer exist in VFS
            this.syncVfsToRfs(sourcePathInVfs, true);
        }

        // iterating thru RFS
        // deleting all RFS files from m_synclist
        // so, during a fresh import nothing ever gets deleted from RFS!
        report("---- Starting search for deleted resources", I_CmsReport.FORMAT_HEADLINE);
        this.removeFromRfs(this.destinationPathInRfs, syncResources);

        // now checking for all files that might be new in RFS
        for (SyncResource vfsPath:syncResources) {
            final String sourcePath = this.destinationPathInRfs +
            	vfsPath.getResource().replace('/', File.separatorChar);

            report("---- Synchronizing From RFS(" + sourcePath + ") into VFS (" + vfsPath.getResource() + ")",
        	    I_CmsReport.FORMAT_HEADLINE);
            // iterating thru RFS
            // possible action: importToVfs()
            this.copyFromRfs(vfsPath);
        }

        // write out the new sync list
        this.writeSyncList();

        //Purge JSP repository. That will not work if the parameter webappDirectory is not pointing to
        //the right directory in the Tomcat webapp. That means: it will not work with the default
        //configuration
        OpenCms.fireCmsEvent(new CmsEvent(I_CmsEventListener.EVENT_FLEX_PURGE_JSP_REPOSITORY, new HashMap(0)));
    }

    /**
     * Copies all resources from the FS which are not existing in the VFS yet.
     * <p>
     *
     * @param syncResource
     *            the folder or file in the VFS to be synchronized with the FS
     * @throws CmsException
     *             if something goes wrong
     */
    private void copyFromRfs(final SyncResource syncResource) throws CmsException {

    	// get the corresponding resource in the FS
        File[] res;
        final File fsFile = this.getFileInRfs(syncResource.getResource());

        if (isIgnorableFile(fsFile)) {
            debugReport("copyFromRFS. Ignore: " + fsFile.getName());
            return;
        }
        boolean doRecursion = true;
        // Only for directories
        if (fsFile.isDirectory()) {
            // first of all, test if this folder exists in the VFS. If not, create
            // it
        	try {
        		this.getCms()
        		.readFolder(this.translate(syncResource.getResource()),
        				CmsResourceFilter.IGNORE_EXPIRATION);
        	} catch (final CmsException e) {
        		// the folder could not be read, so create it
        		final String resourceName = this.translate(syncResource.getResource());

        		reportSuccession(fsFile, resourceName);

        		CmsResource newFolder;

        		// XML STUFF
        		final File metadataFile = this.getMetadataFolderInRfs(syncResource.getResource());

        		if (metadataFile.exists()) {
        			try {
        				// code taken from org.opencms.importexport.CmsImport
        				// read the xml-config file
        				this.setDocXml(CmsXmlUtils.unmarshalHelper(
        						CmsFileUtil.readFile(metadataFile), null));
        				newFolder = this.readResourcesFromManifest(null);
        				// resource gets last modified from metadata
        				// no content for folder
        			} catch (final IOException ex) {
        				throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
        						.container(org.opencms.synchronize.Messages.ERR_READING_FILE_1,
        								fsFile.getName()), ex);
        			}

        			// we have to read the new resource again, to get the correct
        			// timestamp
        			// old stuff newFolder = this.getCms()
        			//   .readFolder(foldername,
        			//CmsResourceFilter.IGNORE_EXPIRATION);
        		} else {
        			this.getReport()
        			.println(org.opencms.report.Messages.get()
        					.container(org.opencms.report.Messages.RPT_ARGUMENT_1,
        							"ERROR: unable to read " +
        							metadataFile.getAbsolutePath()),
        							I_CmsReport.FORMAT_ERROR);
        			throw new CmsSynchronizeException(org.opencms.report.Messages.get()
        					.container(org.opencms.report.Messages.RPT_ARGUMENT_1,
        							"ERROR: unable to read " +
        							metadataFile.getAbsolutePath()));
        		}

        		final String resourcename = this.getCms().getSitePath(newFolder);

        		// add the folder to the sync list
        		final CmsSynchronizeList sync = new CmsSynchronizeList(syncResource.getResource(),
        				resourcename, newFolder.getDateLastModified(),
        				fsFile.lastModified());
        		this.newSyncList.put(resourcename, sync);
        		this.getReport()
        		.println(org.opencms.report.Messages.get()
        				.container(org.opencms.report.Messages.RPT_OK_0),
        				I_CmsReport.FORMAT_OK);
        	}

        	// For the next step, get all resources in this folder
            res = fsFile.listFiles();

        } else {
        	// For the next step we put our file as the only element of the list to be processed
        	// but we will not do a recursion on it
        	res = new File[1];
        	res[0] = fsFile;
        	doRecursion = false;
        }

        // now loop through all resources
        for (int i = 0; i < res.length; i++) {
                debugReport("VfsSync.copyFromRfs: " + res[i].getName());

        	if (isIgnorableFile(res[i])) {
        	    debugReport("copyFromRFS(recursing). Ignore: " + res[i].getName());
        	    continue;
                } else {
                    debugReport("copyFromRFS(recursing). Accept: " + res[i].getName());
                }

        	// get the relative filename
        	String resname = res[i].getAbsolutePath();

		if (resourceIsInExcludesArray(this.getFilenameInVfs(res[i]), syncResource.getExcludes())) {
		    debugReport("VfsSync.copyFromRfs: Not checking " + resname + " in copyFromRfs because it is in the excludes list");
		    continue;
		}

        	if (!this.removeRfsList.contains(resname)) {
        		// do not reimport deletables
        		resname = resname.substring(this.destinationPathInRfs.length());
        		// translate the folder seperator if nescessary
        		resname = resname.replace(File.separatorChar, '/');

        		// now check if this resource was already processed, by
        		// looking up the new sync list
        		if (res[i].isFile()) {
        			if (!this.newSyncList.containsKey(this.translate(
        					resname))) {
        				// this file does not exist in the VFS, so import it from RFS to VFS
        				this.importToVfs(res[i], resname /*, folder*/);
        			}
        		} else {
        			// do a recursion if the current resource is a folder
        			// and the resource is not included in the excludeslist
        			if (doRecursion) {
        				debugReport("VfsSync.copyFromRfs: Recursion over " + resname + "/");
        				this.copyFromRfs(new SyncResource(resname + "/", syncResource.getExcludes()));
        			} else {
        				debugReport("VfsSync.copyFromRfs: Not doing recursion over " + resname + "/");
        			    	throw new RuntimeException("Not doing recursion over " + resname +
        			    		                   ". Please check syncVfsPaths and syncResources");
        			}
        		}
        	} else {
        		debugReport("VfsSync.copyFromRfs: Not copying " + resname + " because it is in the remove list");
        	}
        }

    }

    private void reportSuccession(File fsFile, String resourceName) {
        //Reporting stuff
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_SUCCESSION_1,
                String.valueOf(this.count++)), I_CmsReport.FORMAT_NOTE);
        this.getReport()
            .print(org.opencms.synchronize.Messages.get()
                                                   .container(org.opencms.synchronize.Messages.RPT_IMPORT_FOLDER_0),
            I_CmsReport.FORMAT_NOTE);
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                fsFile.getAbsolutePath().replace('\\', '/')));
        this.getReport()
            .print(org.opencms.synchronize.Messages.get()
                                                   .container(org.opencms.synchronize.Messages.RPT_FROM_FS_TO_0),
            I_CmsReport.FORMAT_NOTE);
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                resourceName));
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_DOTS_0));
        //End of reporting stuff

	}

	/**
     * Creates a new file in the local real file system.
     * <p>
     *
     * @param newFile
     *            the file that has to be created
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void createNewLocalFile(final File newFile)
        throws CmsException {
        if (newFile.exists()) {
            throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                        .container(org.opencms.synchronize.Messages.ERR_EXISTENT_FILE_1,
                    newFile.getPath()));
        }

        FileOutputStream fOut = null;

        try {
            final File parentFolder = new File(newFile.getPath()
                                                      .replace('/',
                        File.separatorChar)
                                                      .substring(0,
                        newFile.getPath().lastIndexOf(File.separator)));
            parentFolder.mkdirs();

            if (parentFolder.exists()) {
                fOut = new FileOutputStream(newFile);
            } else {
                throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                           .container(org.opencms.synchronize.Messages.ERR_CREATE_DIR_1,
                        newFile.getPath()));
            }
        } catch (final IOException e) {
            throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                          .container(org.opencms.synchronize.Messages.ERR_CREATE_FILE_1,
                    this.getClass().getName(), newFile.getPath()), e);
        } finally {
            if (fOut != null) {
                try {
                    fOut.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Deletes a resource in the VFS and updates the synchronisation lists.
     * <p>
     *
     * @param res
     *            The resource to be deleted
     *
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void deleteFromVfs(final CmsResource res) throws CmsException {
        final String resourcename = this.getCms().getSitePath(res);
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_SUCCESSION_1,
                String.valueOf(this.count++)), I_CmsReport.FORMAT_NOTE);

        if (res.isFile()) {
            this.getReport()
                .print(org.opencms.synchronize.Messages.get()
                                                       .container(org.opencms.synchronize.Messages.RPT_DEL_FILE_0),
                I_CmsReport.FORMAT_NOTE);
        } else {
            this.getReport()
                .print(org.opencms.synchronize.Messages.get()
                                                       .container(org.opencms.synchronize.Messages.RPT_DEL_FOLDER_0),
                I_CmsReport.FORMAT_NOTE);
        }

        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                resourcename));
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_DOTS_0));
        // lock the file in the VFS, so that it can be updated
        this.getCms().lockResource(resourcename);
        this.getCms()
            .deleteResource(resourcename, CmsResource.DELETE_PRESERVE_SIBLINGS);
        // Remove it from the sync list
        this.syncList.remove(this.translate(resourcename));
        this.getReport()
            .println(org.opencms.report.Messages.get()
                                                .container(org.opencms.report.Messages.RPT_OK_0),
            I_CmsReport.FORMAT_OK);

        File metadataFile;

        if (res.isFolder()) {
            metadataFile = this.getMetadataFolderInRfs(this.getCms()
                                                           .getSitePath(res));
        } else {
            metadataFile = this.getMetadataFileInRfs(this.getCms()
                                                         .getSitePath(res));
        }

        if (metadataFile.exists()) {
            // we have metadata remaining for a deleted content file or folder
            this.getReport()
                .println(org.opencms.report.Messages.get()
                                                    .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                    "WARNING: please remove " + metadataFile.getAbsolutePath()),
                I_CmsReport.FORMAT_WARNING);
        }
    }

    /**
     * Exports a resource from the VFS to the RFS and updates the synchronisation
     * lists.
     * <p>
     *
     * @param res
     *            the resource to be exported
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    // OpenCms names it exportToRfs()
    private void exportFromVFS(final CmsResource res) throws CmsException {
        CmsFile vfsFile;
        File fsFile;
        File metadataFile;
        String resourcename;

        // to get the name of the file in the FS, we must look it up in the
        // sync list. This is nescessary, since the VFS could use a tranlated
        // filename.
        final CmsSynchronizeList sync = (CmsSynchronizeList) this.syncList.get(this.translate(
                    this.getCms().getSitePath(res)));

        // if no entry in the sync list was found, its a new resource and we
        // can use the name of the VFS resource.
        if (sync == null) {
            // otherwise use the original non-translated name
            resourcename = this.getCms().getSitePath(res);

            // the parent folder could contain a translated names as well, so
            // make a lookup in the sync list ot get its original
            // non-translated name
            final String parent = CmsResource.getParentFolder(resourcename);
            final CmsSynchronizeList parentSync = (CmsSynchronizeList) this.newSyncList.get(parent);

            // use the non-translated pathname
            if (parentSync != null) {
                resourcename = parentSync.getResName() + res.getName();
            }
        } else {
            resourcename = sync.getResName();
        }

        if ((res.isFolder()) && (!resourcename.endsWith("/"))) {
            resourcename += "/";
        }

        fsFile = this.getFileInRfs(resourcename);

        try {
            // if the resource is marked for deletion, do not export it!
            if ((!res.getState().isDeleted()) &&
                    (!res.getName().startsWith("~"))) {
                // ~ taken from CmsExport
                // if its a file, create export the file to the FS
                this.getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_SUCCESSION_1,
                        String.valueOf(this.count++)), I_CmsReport.FORMAT_NOTE);

                if (res.isFile()) {
                    metadataFile = this.getMetadataFileInRfs(resourcename);
                    this.getReport()
                        .print(org.opencms.synchronize.Messages.get()
                                                               .container(org.opencms.synchronize.Messages.RPT_EXPORT_FILE_0),
                        I_CmsReport.FORMAT_NOTE);
                    this.getReport()
                        .print(org.opencms.report.Messages.get()
                                                          .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                            this.getCms().getSitePath(res)));
                    this.getReport()
                        .print(org.opencms.synchronize.Messages.get()
                                                               .container(org.opencms.synchronize.Messages.RPT_TO_FS_AS_0),
                        I_CmsReport.FORMAT_NOTE);
                    this.getReport()
                        .print(org.opencms.report.Messages.get()
                                                          .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                            fsFile.getAbsolutePath().replace('\\', '/')));
                    this.getReport()
                        .print(org.opencms.report.Messages.get()
                                                          .container(org.opencms.report.Messages.RPT_DOTS_0));

                    // create the resource if necessary
                    if (!fsFile.exists()) {
                        this.createNewLocalFile(fsFile);
                    }

                    // write the file content to the FS
                    vfsFile = this.getCms()
                                  .readFile(this.getCms().getSitePath(res),
                            CmsResourceFilter.IGNORE_EXPIRATION);

                    try {
                        this.writeFileByte(vfsFile.getContents(), fsFile);
                    } catch (final IOException e) {
                        throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_WRITE_FILE_0));
                    }

                } else { // not a file but a folder
                    metadataFile = this.getMetadataFolderInRfs(resourcename);
                    this.getReport()
                        .print(org.opencms.synchronize.Messages.get()
                                                               .container(org.opencms.synchronize.Messages.RPT_EXPORT_FOLDER_0),
                        I_CmsReport.FORMAT_NOTE);
                    this.getReport()
                        .print(org.opencms.report.Messages.get()
                                                          .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                            this.getCms().getSitePath(res)));
                    this.getReport()
                        .print(org.opencms.synchronize.Messages.get()
                                                               .container(org.opencms.synchronize.Messages.RPT_TO_FS_AS_0),
                        I_CmsReport.FORMAT_NOTE);
                    this.getReport()
                        .print(org.opencms.report.Messages.get()
                                                          .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                            fsFile.getAbsolutePath().replace('\\', '/')));
                    this.getReport()
                        .print(org.opencms.report.Messages.get()
                                                          .container(org.opencms.report.Messages.RPT_DOTS_0));
                    // its a folder, so create a folder in the RFS
                    fsFile.mkdirs();
                }
                fsFile.setLastModified(res.getDateLastModified());
                // XML STUFF
                try {
                    final Element exportNode = this.openExportFile(metadataFile);
                    this.appendResourceToManifest(res, false, exportNode);
                    this.closeExportFile(exportNode);
                    metadataFile.setLastModified(fsFile.lastModified());
                } catch (final SAXException e) {
                    throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_WRITE_FILE_0));
                } catch (final IOException e) {
                    throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_WRITE_FILE_0));
                }

                // add resource to synchronisation list
                final CmsSynchronizeList sList = new CmsSynchronizeList(resourcename,
                        this.translate(resourcename),
                        res.getDateLastModified(), fsFile.lastModified());
                this.newSyncList.put(this.translate(resourcename), sList);
                // and remove it from the old one
                this.syncList.remove(this.translate(resourcename));
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            }
        } catch (final CmsException e) {
            throw new CmsSynchronizeException(e.getMessageContainer(), e);
        }
    }

    /**
     * Gets the corresponding filename of the VFS to a resource in the RFS.
     * <p>
     *
     * @param res
     *            the resource in the FS
     * @return the corresponding filename in the VFS
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private String getFilenameInVfs(final File res) {
        String resname = res.getAbsolutePath();

        if (res.isDirectory()) {
            resname += "/";
        }

        // translate the folder seperator if nescessary
        resname = resname.replace(File.separatorChar, '/');

        return resname.substring(this.destinationPathInRfs.length());
    }

    /**
     * Imports a new resource from the RFS into the VFS and updates the
     * synchronisation lists.
     * <p>
     *
     * @param fsFile
     *            the file in the RFS - really called for files only
     * @param resName
     *            the name of the resource in the VFS
     * @throws CmsException
     *             if something goes wrong
     */
    // code taken from org.opencms.synchronize.CmsSynchronize
    private void importToVfs(final File fsFile,
        final String resName) throws CmsException {
        try {
            // XML STUFF
            final File metadataFile = this.getMetadataFileInRfs(resName);

            // get the content of the FS file
            final byte[] content = CmsFileUtil.readFile(fsFile);

            // create the file
            // final String filename = this.translate(fsFile.getName());
            this.getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_SUCCESSION_1,
                    String.valueOf(this.count++)), I_CmsReport.FORMAT_NOTE);

            if (fsFile.isFile()) {
                this.getReport()
                    .print(org.opencms.synchronize.Messages.get()
                                                           .container(org.opencms.synchronize.Messages.RPT_IMPORT_FILE_0),
                    I_CmsReport.FORMAT_NOTE);
            } else {
                this.getReport()
                    .print(org.opencms.synchronize.Messages.get()
                                                           .container(org.opencms.synchronize.Messages.RPT_IMPORT_FOLDER_0),
                    I_CmsReport.FORMAT_NOTE);
            }

            this.getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                    fsFile.getAbsolutePath().replace('\\', '/')));
            this.getReport()
                .print(org.opencms.synchronize.Messages.get()
                                                       .container(org.opencms.synchronize.Messages.RPT_FROM_FS_TO_0),
                I_CmsReport.FORMAT_NOTE);

            //CmsResource newRes;
            CmsResource newFile;

            if (metadataFile.exists()) {
                // read the xml-config file
                this.setDocXml(CmsXmlUtils.unmarshalHelper(CmsFileUtil.readFile(
                            metadataFile), null));
                newFile = this.readResourcesFromManifest(content);
                // resource gets last modified from metadata

                // old stuff newRes = this.getCms()
                            // .readResource(this.getCms().getSitePath(newFile));

                this.getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        this.getCms().getSitePath(newFile)));
                this.getReport()
                    .print(org.opencms.report.Messages.get()
                                                      .container(org.opencms.report.Messages.RPT_DOTS_0));
            } else {
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        "ERROR: unable to read " +
                        metadataFile.getAbsolutePath()),
                    I_CmsReport.FORMAT_ERROR);
                throw new CmsSynchronizeException(org.opencms.report.Messages.get()
                                                                             .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        "ERROR: unable to read " +
                        metadataFile.getAbsolutePath()));
            }

            // add resource to synchronisation list
            final CmsSynchronizeList sList = new CmsSynchronizeList(resName,
                    this.translate(resName), newFile.getDateLastModified(),
                    fsFile.lastModified());
            this.newSyncList.put(this.translate(resName), sList);
            this.getReport()
                .println(org.opencms.report.Messages.get()
                                                    .container(org.opencms.report.Messages.RPT_OK_0),
                I_CmsReport.FORMAT_OK);
        } catch (final IOException e) {
            throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_READING_FILE_1,
                    fsFile.getName()), e);
        }
    }

    /**
     * Reads the synchronisation list from the last sync process from the file
     * system and stores the information in a HashMap. If the file does
     * not exist in the file system an empty HashMap is returned.
     * <p>
     *
     * Filenames are stored as keys, CmsSynchronizeList objects as values.
     *
     * @return HashMap with synchronisation information of the last sync process
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private Map readSyncList() throws CmsException {
        final Map sList = new HashMap();

        // the sync list file in the server fs
        File syncListFile;
        syncListFile = new File(this.destinationPathInRfs,
                VfsSync.SYNCLIST_FILENAME);

        // try to read the sync list file if it is there
        if (syncListFile.exists()) {
            // prepare the streams to write the data
            FileReader fIn = null;
            LineNumberReader lIn = null;

            try {
                fIn = new FileReader(syncListFile);
                lIn = new LineNumberReader(fIn);

                // read one line from the file
                String line = lIn.readLine();

                while (line != null) {
                    line = lIn.readLine();

                    // extract the data and create a CmsSychronizedList object
                    // from it
                    if (line != null) {
                        final StringTokenizer tok = new StringTokenizer(line,
                                ":");

                        //if (tok != null) {
                        final String resName = tok.nextToken();
                        final String tranResName = tok.nextToken();
                        final long modifiedVfs = new Long(tok.nextToken()).longValue();
                        final long modifiedFs = new Long(tok.nextToken()).longValue();
                        final CmsSynchronizeList sync = new CmsSynchronizeList(resName,
                                tranResName, modifiedVfs, modifiedFs);
                        sList.put(this.translate(resName), sync);

                        //}
                    }
                }
            } catch (final IOException e) {
                throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                      .container(org.opencms.synchronize.Messages.ERR_READ_SYNC_LIST_0),
                    e);
            } finally {
                // close all streams that were used
                try {
                    if (lIn != null) {
                        lIn.close();
                    }

                    if (fIn != null) {
                        fIn.close();
                    }
                } catch (final IOException e) {
                    // ignore
                }
            }
        }

        return sList;
    }

    /**
     * Removes all resources in the RFS which are deleted in the VFS.
     * <p>
     * This synchronization version does not really remove any files from RFS,
     * but just lists the files to remove. This is in order to not disturb
     * version control. Deletable files must be deleted manually with methods
     * corresponding to the SCM in use (i.e. TortoiseSVN). The internal List of
     * deletable files is also used to avoid re-importing those files still
     * existing in RFS later.
     *
     * @param folder
     *            the folder in the FS to check
     * @param syncResources
     *            the list of sync resources to recognize files to be separately treated
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void removeFromRfs(final String folder, List<SyncResource> syncResources) throws CmsException {

        // get the corresponding folder in the FS
        File[] res;
        final File rfsFile = new File(folder);
        boolean removingFolder = true;

        // get all resources in this folder
        if (rfsFile.isDirectory()) {

            res = rfsFile.listFiles();
        } else {
            removingFolder = false;
            res = new File[1];
            res[0] = rfsFile;
        }

        // now loop through all resources
        for (int i = 0; i < res.length; i++) {
            // get the corrsponding name in the VFS
            final String vfsFile = this.getFilenameInVfs(res[i]);

        	//Do not check if the resource is in the excludes list
        	if (syncResourcesContainsExclude(syncResources, res[i])) {
        		debugReport("Not recursing deletion into "+res[i]+" because it is in the excludes list");
        		continue;
        	}

            // recurse if it is a directory, we must go depth first to delete
            // files
            final String abspath = res[i].getAbsolutePath();
            debugReport("removeFromRFS: " + abspath);

            if ((res[i].isDirectory()) && (!res[i].isHidden()) &&
                    (!isIgnorableFile(new File(abspath)))) {
            	this.removeFromRfs(abspath, syncResources);

            // Also recurse if the file is in the syncResources
            } else if (res[i].isFile() && removingFolder && syncResourcesContainsResource(syncResources, res[i])) {
            	this.removeFromRfs(abspath, syncResources);
            }

            // now check if this resource is still in the old sync list.
            // if so, then it does not exist in the FS anymore and must be
            // deleted
            final CmsSynchronizeList sync = (CmsSynchronizeList) this.syncList.get(this.translate(
                        vfsFile));

            // there is an entry, so delete the resource
            if (sync != null) {
                /*
                 * we currently do not really delete RFS files as for not
                 * disturbing version control we only warn
                 */
                this.syncList.remove(this.translate(vfsFile));
                this.removeRfsList.add(abspath);

                // do not reimport deletables
                if (!isIgnorableFile(res[i])) {
                    this.getReport()
                        .println(org.opencms.report.Messages.get()
                            .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                            "WARNING: please remove " + abspath),
                            I_CmsReport.FORMAT_WARNING);

                    final File metadataFile = this.getMetadataFileInRfs(vfsFile);

                    // I think we really only have files here, no
                    // subdirectories
                    if (metadataFile.exists()) {
                        this.getReport()
                            .println(org.opencms.report.Messages.get()
                                                                .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                                "WARNING: please remove " +
                                metadataFile.getAbsolutePath()),
                            I_CmsReport.FORMAT_WARNING);
                    }
                }
            }
        }
    }

    /**
     *
     * @param syncResources
     * @param file
     * @return
     */
    private boolean syncResourcesContainsExclude(
			List<SyncResource> syncResources, File file) {
    	String vfsPath = this.getFilenameInVfs(file);
    	for (SyncResource syncRes:syncResources) {
    		if (resourceIsInExcludesArray(vfsPath, syncRes.getExcludes())) {
    			return true;
    		}
    	}
    	return false;
	}

    /**
     *
     * @param syncResources
     * @param file
     * @return true if the file corresponds to one of the resources in the list
     */
    private boolean syncResourcesContainsResource(List<SyncResource> syncResources, File file) {
    	String vfsName = getFilenameInVfs(file);
    	for (SyncResource res:syncResources) {
    		if (res.getResource().equals(vfsName)) {
    			return true;
    		}
    	}
    	return false;
	}

    /**
     * Returns true if the resource in {@code str} can be found in the list in {@code strArray}.
     * @param str resource
     * @param strArray list of resources
     * @return true if the resource in {@code str} can be found in the list in {@code strArray}.
     */
    boolean resourceIsInExcludesArray(String str, String[] strArray) {
	if (str.endsWith("/")) {
	    str = str.substring(0, str.length() - 1);
	}
	String [] sortedArray = new String[strArray.length];
	int i = 0;
	for (String elem:strArray) {
	    if (elem.endsWith("/")) {
		elem = elem.substring(0, elem.length() - 1);
	    }
	    sortedArray[i++] = elem;

	}
	//System.arraycopy(strArray, 0, sortedArray, 0, strArray.length);
	Arrays.sort(sortedArray);

	int posAst = Arrays.binarySearch(sortedArray, "*");
	int posStr = Arrays.binarySearch(sortedArray, str);
	boolean b = posAst >= 0 || posStr >= 0;

	return b;
    }

    /**
     * Updates the synchronisation lists if a resource is not used during the
     * synchronisation process.
     * <p>
     *
     * @param res
     *            the resource whose entry must be updated
     */
    // code taken from org.opencms.synchronize.CmsSynchronize
    private void skipResource(final CmsResource res) {
	// add the file to the new sync list...
	final String resname = this.getCms().getSitePath(res);
    final CmsSynchronizeList sync = (CmsSynchronizeList) this.syncList.get(this.translate(
    		resname));
    final File fsFile = this.getFileInRfs(sync.getResName());
    final File metadataFile;
    if (res.isFolder()) {
    	metadataFile = this.getMetadataFolderInRfs(sync.getResName());
    } else {
    	metadataFile = this.getMetadataFileInRfs(sync.getResName());
    }
    final long rfslastmod = Math.max(fsFile.lastModified(),
	    metadataFile.lastModified());
	final CmsSynchronizeList sList = new CmsSynchronizeList(sync.getResName(),
            this.translate(resname), sync.getModifiedVfs(), rfslastmod);
	this.newSyncList.put(this.translate(resname), sList);
	// .. and remove it from the old one
	this.syncList.remove(this.translate(resname));
	// update the report
	this.getReport()
	.print(org.opencms.report.Messages.get()
		.container(org.opencms.report.Messages.RPT_SUCCESSION_1,
			String.valueOf(this.count++)), I_CmsReport.FORMAT_NOTE);
	this.getReport()
	.print(org.opencms.synchronize.Messages.get()
		.container(org.opencms.synchronize.Messages.RPT_SKIPPING_0),
		I_CmsReport.FORMAT_NOTE);
	this.getReport()
	.println(org.opencms.report.Messages.get()
		.container(org.opencms.report.Messages.RPT_ARGUMENT_1,
			resname));
	
    }

    /**
     * Synchronizes resources from the VFS to the RFS.
     * <p>
     *
     * During the synchronization process, the following actions will be done:
     * <p>
     *
     * <ul>
     * <li>Export modified resources from the VFS to the FS</li>
     * <li>Update resources in the VFS if the corresponding resource in the FS
     * has changed</li>
     * <li>Delete resources in the VFS if the corresponding resource in the FS
     * has been deleted</li>
     * </ul>
     *
     * @param sourcePathInVfs
     *            The folder in the VFS to be synchronized with the FS
     * @param startfolder
     *            true only if called with the outermost folder, from the List
     *            of VFS folders to synchronize, false for all recursive calls
     *            on subfolders
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void syncVfsToRfs(final SyncResource sourcePathInVfs, final boolean startfolder)
        throws CmsException {
        int action = 0;

        // in contrast to plain OpenCms Sync, we need to export the start
        // folders ~folder.xml -- as well as single files
        // first check if this folder is a folder and must be synchronised

        CmsResource res = null;
        if (startfolder) {
            try {
                res = this.getCms()
                          .readResource(sourcePathInVfs.getResource(),
                        CmsResourceFilter.IGNORE_EXPIRATION);
                action = this.testSyncVfs(res);

                // do the correct action according to the test result
                switch (action) {
                case EXPORT_FROM_VFS:
                    this.exportFromVFS(res); // OpenCms names it exportToRfs()

                    break;

                case UPDATE_IN_VFS:
                    this.updateInVfs(res);

                    break;

                //case DELETE_FROM_VFS:
                // we're not going to delete our designated start folder
                // we just par default skipresource
                //  break;
                default:
                    this.skipResource(res);
                }
            } catch (final CmsException e) {
            	simpleReport("VfsSync.syncVfsToRfs: Could not read resource " + sourcePathInVfs.getResource() +
            		". copyFromRfs should copy it");
                // in case the start folder could not be read stop syncing
                // VFS=>RFS and leave it to copyFromRfs() to create the folder
                return;
            }
        }

        // get all resources in the given folder or fill the list with the file given
        final List<CmsResource> resources;
        if (res == null) {
            res = this.getCms()
        	    .readResource(sourcePathInVfs.getResource(),
        		    CmsResourceFilter.IGNORE_EXPIRATION);
        }
        if (res.isFolder()) {
            resources = this.getCms()
        	    .getResourcesInFolder(sourcePathInVfs.getResource(),
        		    CmsResourceFilter.IGNORE_EXPIRATION);
        } else {
            resources = new ArrayList<CmsResource>();
            //resources.add(res); //This resource has been already correctly synchronized
        }

        // now look through all resources in the folder
        for (int i = 0; i < resources.size(); i++) {
            res = (CmsResource) resources.get(i);

            // test if the resource is marked as deleted. if so,
            // do nothing, the corresponding file in the RFS will be removed later
            // ~ code taken from org.opencms.importexport.CmsExport
            if ((!res.getState().isDeleted()) &&
        	    (!res.getName().startsWith("~"))) {

        	String childResourcePath = this.getCms().getSitePath(res);
        	if (resourceIsInExcludesArray(childResourcePath, sourcePathInVfs.getExcludes())) {
        	    //simpleReport("Not doing VfsToRfs of "+childResourcePath+" because it is in the excludes list");
        	} else {
        	    //simpleReport("Doing VfsToRfs of "+childResourcePath+" because it is NOT in the excludes list");
        	    //simpleReport("Excludes: "+Arrays.toString(sourcePathInVfs.getExcludes()));

        	    // do a recursion if the current resource is a folder
        	    if (res.isFolder()) {
        		// first check if this folder must be synchronised
        		action = this.testSyncVfs(res);

        		// do the correct action according to the test result
        		switch (action) {
        		case EXPORT_FROM_VFS:
        		    this.exportFromVFS(res); // OpenCms names it exportToRfs()

        		    break;

        		case UPDATE_IN_VFS:
        		    this.updateInVfs(res);

        		    break;

        		case DELETE_FROM_VFS:

        		    // passiert unten nach der Rekursion: this.deleteFromVfs(res);
        		    // thus, here, do nothing
        		    break;

        		default:
        		    this.skipResource(res);
        		}

        		// recurse into the subfolders. This must be done before
        		// the folder might be deleted!
        		this.syncVfsToRfs(new SyncResource(childResourcePath, sourcePathInVfs.getExcludes()), false);

        		if (action == DELETE_FROM_VFS) {
        		    this.deleteFromVfs(res);
        		}
        	    } else {
        		// if the current resource is a file, check if it has to
        		// be synchronized
        		action = this.testSyncVfs(res);

        		// do the correct action according to the test result
        		switch (action) {
        		case EXPORT_FROM_VFS:
        		    this.exportFromVFS(res); // OpenCms names it exportToRfs()

        		    break;

        		case UPDATE_IN_VFS:
        		    this.updateInVfs(res);

        		    break;

        		case DELETE_FROM_VFS:
        		    this.deleteFromVfs(res);

        		    break;

        		default:
        		    this.skipResource(res);
        		}
        	    }
        	} //if is in excludes
            }
        }
    }

    /**
     * Determines the synchronisation status of a VFS resource.
     *
     * @param res
     *            the VFS resource to check
     * @return integer value for the action to be done for this VFS resource
     * one of:
     * 0 no action
     * VfsSync.UPDATE_IN_VFS
     * VfsSync.EXPORT_FROM_VFS
     * VfsSync.DELETE_FROM_VFS
     */
    // code taken from org.opencms.synchronize.CmsSynchronize
    private int testSyncVfs(final CmsResource res) {
	int action = 0;

	// data from sync list
	final String resourcename = this.getCms().getSitePath(res);

	if (this.syncList.containsKey(this.translate(resourcename))) {
	    // this resource was already used in a previous syncprocess
	    final CmsSynchronizeList sync = (CmsSynchronizeList) this.syncList.get(this.translate(
		    resourcename));

	    // get the corresponding resource from the FS
	    final File fsFile = this.getFileInRfs(sync.getResName());
	    final File metadataFile;

	    if (res.isFolder()) {
		metadataFile = this.getMetadataFolderInRfs(sync.getResName());
	    } else {
		metadataFile = this.getMetadataFileInRfs(sync.getResName());
	    }

		try {
			this.setDocXml(CmsXmlUtils.unmarshalHelper(
					CmsFileUtil.readFile(metadataFile), null));
		} catch (Exception e) {
			//do nothing
		}
	    
	    final long vfslastmod = res.getDateLastModified();

	    // in a subversion team environment it may happen that *only* the metadata file is newer
	    final long rfslastmod = Math.max(fsFile.lastModified(),
		    metadataFile.lastModified());

	    // now check what to do with this resource.
	    // if the modification date is newer than the logged modification
	    // date in the sync list, this resource must be exported too
	    if (vfslastmod > sync.getModifiedVfs()) {
		// VFS new compared to last sync
		// now check if the resource in the FS is newer, then the
		// resource from the FS must be imported
		// check if it has been modified since the last sync process
		// and its newer than the resource in the VFS, only then this
		// resource must be imported form the FS
		if ((rfslastmod > sync.getModifiedFs()) &&
			(rfslastmod > vfslastmod)) {
		    // RFS neuer als Sync und VFS
			if (isFileWasChanged(fsFile, res)) {
				action = UPDATE_IN_VFS; // RFS => VFS
			}
		} else if (isFileWasChanged(fsFile, res)) {
		    action = EXPORT_FROM_VFS; // VFS => RFS
		}
	    } else { // nicht neu im VFS
		// test if the resource in the FS does not exist anymore.
		// if so, remove the resource in the VFS

		if (fsFile.exists()) {
		    // now check if the resource in the FS might have changed
		    if (rfslastmod > sync.getModifiedFs() && isFileWasChanged(fsFile, res)) {
			action = UPDATE_IN_VFS;
		    } // else action remains 0
		} else {
		    action = DELETE_FROM_VFS;
		}
	    }
	} else {
	    // the resource name was not found in the sync list
	    // this is a new resource
	    action = EXPORT_FROM_VFS; // VFS => RFS
	}

	return action;
    }

	/**
	 * Check if the file in VFS or RFS was changed
	 * 
	 * Required to prevent unnecessary metadata updates
	 * 
	 * @param fsFile
	 *            - file in File System
	 * @param resource
	 *            - CmsResource in VFS
	 * 
	 * @return true if file was changed
	 * 
	 */
	private boolean isFileWasChanged(final File fsFile,
			final CmsResource resource) {
		final Map<String, Object> fsFileParams = new HashMap<String, Object>();
		final Map<String, Object> vfsFileParams = new HashMap<String, Object>();
		try {
			fillFileParamMap(fsFileParams, null, false);
			fillFileParamMap(vfsFileParams, resource, true);
			if (isFileContentsDiffer(fsFile, resource)) {
				return true;
			}
			if (isParamsDiffer(fsFileParams, vfsFileParams)) {
				return true;
			}
		} catch (final Exception e) {
			this.getReport().println(e);
			return true;
		}
		return false;
	}

	private boolean isFileContentsDiffer(final File fsFile,
			final CmsResource resource) throws IOException, CmsException {
		if (null == resource || null == fsFile) {
			return true;
		}
		if (OpenCms.getResourceManager().getResourceType(resource).isFolder()) {
			return false;
		}
		return !Arrays.equals(this.getCms().readFile(resource).getContents(),
				CmsFileUtil.readFile(fsFile));
	}

	private boolean isParamsDiffer(final Map<String, Object> fsFileParams,
			final Map<String, Object> vfsFileParams) {
		for (String key : fsFileParams.keySet()) {
			if (vfsFileParams.get(key) instanceof String
					&& vfsFileParams.get(key) != null
					&& !((String) vfsFileParams.get(key))
							.equals((String) fsFileParams.get(key))) {
				return true;
			}
			if (vfsFileParams.get(key) instanceof Long
					&& !((Long) vfsFileParams.get(key))
							.equals((Long) fsFileParams.get(key))) {
				return true;
			}
			if (CmsImportVersion7.N_PROPERTIES.equals(key)
					&& !isListEquals((List<CmsProperty>) fsFileParams.get(key),
							(List<CmsProperty>) vfsFileParams.get(key),
							propertyComparator)) {
				return true;
			}
			if (CmsImportVersion7.N_ACCESSCONTROL_ENTRIES.equals(key)
					&& !isListEquals((List<CmsAccessControlEntry>) fsFileParams
							.get(key),
							(List<CmsAccessControlEntry>) vfsFileParams
									.get(key),
							CmsAccessControlEntry.COMPARATOR_ACE)) {
				return true;
			}
			if (CmsImportVersion7.N_RELATIONS.equals(key)
					&& !isListEquals((List<CmsRelation>) fsFileParams.get(key),
							(List<CmsRelation>) vfsFileParams.get(key),
							CmsRelation.COMPARATOR)) {
				return true;
			}
			if (CmsImportVersion7.N_TYPE.equals(key)
					&& !((I_CmsResourceType) vfsFileParams.get(key))
							.isIdentical((I_CmsResourceType) fsFileParams
									.get(key))) {
				return true;
			}
		}
		return false;
	}

	private <T> boolean isListEquals(final List<T> vfsList,
			final List<T> fsList, final Comparator<? super T> comparator) {
		if (isListsEmpty(vfsList, fsList)) {
			return true;
		}
		if (isListSizeDifferent(vfsList, fsList)) {
			return false;
		}
		Collections.sort(vfsList, comparator);
		Collections.sort(fsList, comparator);
		for (int i = 0; i < vfsList.size(); i++) {
			if (vfsList.get(i).getClass() == CmsProperty.class
					&& !((CmsProperty) vfsList.get(i))
							.isIdentical((CmsProperty) fsList.get(i))) {
				return false;
			}
			if (vfsList.get(i).getClass() == CmsAccessControlEntry.class
					&& !isACEEquals((CmsAccessControlEntry) vfsList.get(i),
							(CmsAccessControlEntry) fsList.get(i))) {
				return false;
			}
			if (vfsList.get(i).getClass() == CmsRelation.class
					&& !((CmsRelation) vfsList.get(i))
							.equals((CmsRelation) vfsList.get(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * compare two ACE without checking resourceId.
	 * 
	 * @param aceOne
	 * @param aceTwo
	 * @return true if ACE equals
	 */
	private boolean isACEEquals(CmsAccessControlEntry aceOne,
			CmsAccessControlEntry aceTwo) {
		if (aceOne.getFlags() != aceTwo.getFlags()) {
			return false;
		}
		if (aceOne.getPermissions().getAllowedPermissions() != aceTwo
				.getPermissions().getAllowedPermissions()) {
			return false;
		}
		if (aceOne.getPermissions().getDeniedPermissions() != aceTwo
				.getPermissions().getDeniedPermissions()) {
			return false;
		}
		if (!aceOne.getPrincipal().equals(aceTwo.getPrincipal())) {
			return false;
		}
		return true;
	}

	private static final Comparator<CmsProperty> propertyComparator = new Comparator<CmsProperty>() {
		@Override
		public int compare(CmsProperty o1, CmsProperty o2) {
			return o1.compareTo(o2);
		}
	};

	private boolean isListsEmpty(List<? extends Object> listOne,
			List<? extends Object> listTwo) {
		if ((listOne == null && listTwo == null)
				|| (listOne.isEmpty() && listTwo.isEmpty())) {
			return true;
		}

		return false;
	}

	private boolean isListSizeDifferent(List<? extends Object> listOne,
			List<? extends Object> listTwo) {
		if ((listOne == null && listTwo != null) || listOne != null
				&& listTwo == null || listOne.size() != listTwo.size()) {
			return true;
		}
		return false;
	}

	private void fillFileParamMap(final Map<String, Object> fileParamMap,
			final CmsResource resource, final boolean readParamFromVfsFile)
			throws Exception {
		List ignoredProperties = OpenCms.getImportExportManager()
				.getIgnoredProperties();
		if (ignoredProperties == null) {
			ignoredProperties = Collections.EMPTY_LIST;
		}
		final Element currentElement = (Element) this.getDocXml()
				.selectNodes("//" + CmsImportVersion7.N_FILE).get(0);

		// <type>
		final I_CmsResourceType type = readParamFromVfsFile ? OpenCms
				.getResourceManager().getResourceType(resource) : OpenCms
				.getResourceManager().getResourceType(
						XmlHandling.getChildElementTextValue(currentElement,
								CmsImportVersion7.N_TYPE));
		fileParamMap.put(CmsImportVersion7.N_TYPE, type);

		// <destination>
		final String destination = XmlHandling.getChildElementTextValue(
				currentElement, CmsImportVersion7.N_DESTINATION);
		fileParamMap.put(CmsImportVersion7.N_DESTINATION,
				readParamFromVfsFile ? this.getCms().getSitePath(resource)
						: "/" + destination + (type.isFolder() ? "/" : ""));

		// <uuidstructure>
		final String uuidstructure = readParamFromVfsFile ? resource
				.getStructureId().toString() : XmlHandling
				.getChildElementTextValue(currentElement,
						CmsImportVersion7.N_UUIDSTRUCTURE);
		fileParamMap.put(CmsImportVersion7.N_UUIDSTRUCTURE, uuidstructure);

		// <uuidresource>
		if (!type.isFolder()) {
			fileParamMap.put(
					CmsImportVersion7.N_UUIDRESOURCE,
					readParamFromVfsFile ? resource.getResourceId().toString()
							: XmlHandling.getChildElementTextValue(
									currentElement,
									CmsImportVersion7.N_UUIDRESOURCE));
		} else {
			fileParamMap.put(CmsImportVersion7.N_UUIDRESOURCE, null);
		}

		// <userlastmodified>
		fileParamMap.put(
				CmsImportVersion7.N_USERLASTMODIFIED,
				readParamFromVfsFile ? this.getCms()
						.readUser(resource.getUserLastModified()).getName()
						: getUserFieldFromMetadata(currentElement,
								CmsImportVersion7.N_USERLASTMODIFIED));

		// <usercreated>
		fileParamMap.put(
				CmsImportVersion7.N_USERCREATED,
				readParamFromVfsFile ? this.getCms()
						.readUser(resource.getUserLastModified()).getName()
						: getUserFieldFromMetadata(currentElement,
								CmsImportVersion7.N_USERCREATED));

		// <datecreated>
		fileParamMap.put(
				CmsImportVersion7.N_DATECREATED,
				readParamFromVfsFile ? resource.getDateCreated() / 1000
						: getDateFieldFromMetadata(currentElement,
								CmsImportVersion7.N_DATECREATED,
								System.currentTimeMillis()) / 1000);

		// <datereleased>
		fileParamMap.put(
				CmsImportVersion7.N_DATERELEASED,
				readParamFromVfsFile ? resource.getDateReleased() / 1000
						: getDateFieldFromMetadata(currentElement,
								CmsImportVersion7.N_DATERELEASED,
								CmsResource.DATE_RELEASED_DEFAULT) / 1000);

		// <dateexpired>
		fileParamMap.put(
				CmsImportVersion7.N_DATEEXPIRED,
				readParamFromVfsFile ? resource.getDateExpired() / 1000
						: getDateFieldFromMetadata(currentElement,
								CmsImportVersion7.N_DATEEXPIRED,
								CmsResource.DATE_EXPIRED_DEFAULT) / 1000);

		// <flags>
		fileParamMap.put(
				CmsImportVersion7.N_FLAGS,
				readParamFromVfsFile ? String.valueOf(resource.getFlags())
						: XmlHandling.getChildElementTextValue(currentElement,
								CmsImportVersion7.N_FLAGS));

		// <properties>
		fileParamMap.put(
				CmsImportVersion7.N_PROPERTIES,
				readParamFromVfsFile ? this.getCms().readPropertyObjects(
						resource, false) : this.readPropertiesFromManifest(
						currentElement, ignoredProperties));

		// <accesscontrol>
		fileParamMap
				.put(CmsImportVersion7.N_ACCESSCONTROL_ENTRIES,
						readParamFromVfsFile ? this.getCms()
								.getAccessControlEntries(
										this.getCms().getSitePath(resource),
										false)
								: getACEList(
										uuidstructure,
										currentElement
												.selectNodes("*/"
														+ CmsImportVersion7.N_ACCESSCONTROL_ENTRY)));
		// <relations>
		fileParamMap.put(
				CmsImportVersion7.N_RELATIONS,
				readParamFromVfsFile ? this.getCms().getRelationsForResource(
						this.getCms().getSitePath(resource),
						CmsRelationFilter.TARGETS.filterNotDefinedInContent())
						: getRelationsForElement(new CmsUUID(uuidstructure),
								destination, currentElement));
	}

	private long getDateFieldFromMetadata(final Element currentElement,
			final String fieldname, final long defaultValue) {
		String timestamp = XmlHandling.getChildElementTextValue(currentElement,
				fieldname);
		if (timestamp != null) {
			return this.convertTimestamp(timestamp);
		} else {
			return defaultValue;
		}
	}

	private String getUserFieldFromMetadata(final Element currentElement,
			final String fieldname) {
		return OpenCms.getImportExportManager().translateUser(
				XmlHandling.getChildElementTextValue(currentElement,
						CmsImportVersion7.N_USERLASTMODIFIED));
	}

	private List<CmsAccessControlEntry> getACEList(final String uuidresource,
			final List acentryNodes) {
		final List<CmsAccessControlEntry> aceList = new ArrayList<CmsAccessControlEntry>();
		Element currentEntry;
		for (int j = 0; j < acentryNodes.size(); j++) {
			currentEntry = (Element) acentryNodes.get(j);

			// get the data of the access control entry
			final String id = XmlHandling.getChildElementTextValue(
					currentEntry, CmsImportVersion7.N_ACCESSCONTROL_PRINCIPAL);
			String principalId = new CmsUUID().toString();
			String principal = id.substring(id.indexOf('.') + 1, id.length());

			try {
				if (id.startsWith(I_CmsPrincipal.PRINCIPAL_GROUP)) {
					principal = OpenCms.getImportExportManager()
							.translateGroup(principal);
					principalId = this.getCms().readGroup(principal).getId()
							.toString();
				} else if (id.startsWith(I_CmsPrincipal.PRINCIPAL_USER)) {
					principal = OpenCms.getImportExportManager().translateUser(
							principal);
					principalId = this.getCms().readUser(principal).getId()
							.toString();
				} else if (id.startsWith(CmsRole.PRINCIPAL_ROLE)) {
					principalId = CmsRole.valueOfRoleName(principal).getId()
							.toString();
				} else if (id
						.equalsIgnoreCase(CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_NAME)) {
					principalId = CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_ID
							.toString();
				} else if (id
						.equalsIgnoreCase(CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_NAME)) {
					principalId = CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_ID
							.toString();
				}

				final String acflags = XmlHandling.getChildElementTextValue(
						currentEntry, CmsImportVersion7.N_FLAGS);
				final String allowed = ((Element) currentEntry
						.selectNodes(
								"./"
										+ CmsImportVersion7.N_ACCESSCONTROL_PERMISSIONSET
										+ "/"
										+ CmsImportVersion7.N_ACCESSCONTROL_ALLOWEDPERMISSIONS)
						.get(0)).getTextTrim();
				final String denied = ((Element) currentEntry
						.selectNodes(
								"./"
										+ CmsImportVersion7.N_ACCESSCONTROL_PERMISSIONSET
										+ "/"
										+ CmsImportVersion7.N_ACCESSCONTROL_DENIEDPERMISSIONS)
						.get(0)).getTextTrim();
				aceList.add(new CmsAccessControlEntry(
						new CmsUUID(uuidresource), new CmsUUID(principalId),
						Integer.parseInt(allowed), Integer.parseInt(denied),
						Integer.parseInt(acflags)));
			} catch (final CmsException e) {
				this.getReport().println(e);
			}
		}
		return aceList;
	}
    
    
    /**
     * Translates the resource name.
     * <p>
     *
     * This is nescessary since the server RFS does allow different naming
     * conventions than the VFS.
     *
     * @param name
     *            the resource name to be translated
     * @return the translated resource name
     */
    // code taken from org.opencms.synchronize.CmsSynchronize
    private String translate(final String name) {
        String translation; // = null;
        translation = this.getCms().getRequestContext().getFileTranslator()
                          .translateResource(name);

        return translation;
    }

    /**
     * Imports a resource from the RFS to the VFS and updates the synchronisation
     * lists.
     * <p>
     *
     * @param res the resource to be updated
     *
     * @throws CmsException
     *             if anything goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void updateInVfs(final CmsResource res) throws CmsException {
        CmsFile vfsFile;

        // to get the name of the file in the RFS, we must look it up in the
        // sync list. This is necessary, since the VFS could use a tranlated
        // filename.
        final String resourcename = this.getCms().getSitePath(res);
        final CmsSynchronizeList sync = (CmsSynchronizeList) this.syncList.get(this.translate(
                    resourcename));
        final File fsFile = this.getFileInRfs(sync.getResName());
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_SUCCESSION_1,
                String.valueOf(this.count++)), I_CmsReport.FORMAT_NOTE);
        this.getReport()
            .print(org.opencms.synchronize.Messages.get()
                                                   .container(org.opencms.synchronize.Messages.RPT_UPDATE_FILE_0),
            I_CmsReport.FORMAT_NOTE);
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                resourcename));
        this.getReport()
            .print(org.opencms.report.Messages.get()
                                              .container(org.opencms.report.Messages.RPT_DOTS_0));
        // lock the file in the VFS, so that it can be updated
        this.getCms().lockResource(resourcename);
        File metadataFile;
        if (res.isFile()) {
            // read the file in the VFS
            vfsFile = this.getCms()
                          .readFile(resourcename,
                    CmsResourceFilter.IGNORE_EXPIRATION);

            // import the content from the RFS
            try {
                vfsFile.setContents(CmsFileUtil.readFile(fsFile));
            } catch (final IOException e) {
                throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                               .container(org.opencms.synchronize.Messages.ERR_IMPORT_1,
                        fsFile.getName()));
            }

            this.getCms().writeFile(vfsFile);

            metadataFile = this.getMetadataFileInRfs(sync.getResName());

            if (metadataFile.exists()) {
                try {
                    // code taken from org.opencms.importexport.CmsImport
                    // read the xml-config file
                    this.setDocXml(CmsXmlUtils.unmarshalHelper(
                            CmsFileUtil.readFile(metadataFile), null));
                    this.readResourcesFromManifest(null);
                 // resource gets last modified from metadata
                    // no content for folder
                } catch (final IOException ex) {
                    throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_READING_FILE_1,
                            fsFile.getName()), ex);
                }
            } else {
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        "ERROR: unable to read " +
                        metadataFile.getAbsolutePath()),
                    I_CmsReport.FORMAT_ERROR);
                throw new CmsSynchronizeException(org.opencms.report.Messages.get()
                                                                             .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        "ERROR: unable to read " +
                        metadataFile.getAbsolutePath()));
            }
        } else {
            metadataFile = this.getMetadataFolderInRfs(sync.getResName());

            if (metadataFile.exists()) {
                try {
                    // code taken from org.opencms.importexport.CmsImport
                    // read the xml-config file
                    this.setDocXml(CmsXmlUtils.unmarshalHelper(
                            CmsFileUtil.readFile(metadataFile), null));
                    this.readResourcesFromManifest(null);

                    // no content for folder
                } catch (final IOException ex) {
                    throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_READING_FILE_1,
                            fsFile.getName()), ex);
                }
            } else {
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        "ERROR: unable to read " +
                        metadataFile.getAbsolutePath()),
                    I_CmsReport.FORMAT_ERROR);
                throw new CmsSynchronizeException(org.opencms.report.Messages.get()
                                                                             .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        "ERROR: unable to read " +
                        metadataFile.getAbsolutePath()));
            }
        }

        getCms().setDateLastModified(resourcename, fsFile.lastModified(), false);
        final CmsResource readres = getCms().readResource(resourcename);
        // hier nochmal die aktualisierten Metadaten rausschreiben mit dem neuen Datemodified
        // XML STUFF
        try {
            final Element exportNode = this.openExportFile(metadataFile);
            this.appendResourceToManifest(readres, false, exportNode);
            this.closeExportFile(exportNode);
            metadataFile.setLastModified(fsFile.lastModified());
        } catch (final SAXException e) {
            throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_WRITE_FILE_0));
        } catch (final IOException e) {
            throw new CmsSynchronizeException(org.opencms.synchronize.Messages.get()
                                                         .container(org.opencms.synchronize.Messages.ERR_WRITE_FILE_0));
        }

        // add resource to synchronisation list
        final CmsSynchronizeList sList = new CmsSynchronizeList(sync.getResName(),
                this.translate(resourcename), readres.getDateLastModified(), fsFile.lastModified());
        this.newSyncList.put(this.translate(resourcename), sList);
        // and remove it from the old one
        this.syncList.remove(this.translate(resourcename));
        this.getReport()
            .println(org.opencms.report.Messages.get()
                                                .container(org.opencms.report.Messages.RPT_OK_0),
            I_CmsReport.FORMAT_OK);

    }

    /**
     * This writes the byte content of a resource to the RFS.
     * <p>
     *
     * @param content
     *            the content of the file in the VFS
     * @param file
     *            the file in SFS that has to be updated with content
     *
     * @throws IOException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void writeFileByte(final byte[] content, final File file)
        throws IOException {
        FileOutputStream fOut = null;
        DataOutputStream dOut = null;

        try {
            // write the content to the file in server filesystem
            fOut = new FileOutputStream(file);
            dOut = new DataOutputStream(fOut);
            dOut.write(content);
            dOut.flush();
        } catch (final IOException e) {
            throw e;
        } finally {
            try {
                if (fOut != null) {
                    fOut.close();
                }
            } catch (final IOException e) {
                // ignore
            }

            try {
                if (dOut != null) {
                    dOut.close();
                }
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * Writes the synchronisation list of the current sync process to the RFS.
     * <p>
     *
     * The file can be found in the synchronization folder
     *
     * @throws CmsException
     *             if something goes wrong
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private void writeSyncList() throws CmsException {
        // the sync list file in the server fs
        File syncListFile;
        syncListFile = new File(this.destinationPathInRfs,
                VfsSync.SYNCLIST_FILENAME);

        // prepare the streams to write the data
        FileOutputStream fOut = null;
        PrintWriter pOut = null;

        try {
            fOut = new FileOutputStream(syncListFile);
            pOut = new PrintWriter(fOut);
            pOut.println(CmsSynchronizeList.getFormatDescription());

            // get all keys from the hashmap and make an iterator on it
            final Iterator values = this.newSyncList.values().iterator();

            // loop throush all values and write them to the sync list file in
            // a human readable format
            while (values.hasNext()) {
                final CmsSynchronizeList sync = (CmsSynchronizeList) values.next();
                // fOut.write(sync.toString().getBytes());
                pOut.println(sync.toString());
            }
        } catch (final IOException e) {
            throw new CmsDbIoException(org.opencms.synchronize.Messages.get()
                                               .container(org.opencms.synchronize.Messages.ERR_IO_WRITE_SYNCLIST_0), e);
        } finally {
            // close all streams that were used
            try {
                if (pOut != null) {
                    pOut.flush();
                    pOut.close();
                }

                if (fOut != null) {
                    fOut.flush();
                    fOut.close();
                }
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * Gets the "real" file corresponding to a resource in the VFS.
     * <p>
     *
     * @param res
     *            path to the resource inside the VFS
     * @return the corresponding file in the FS
     */

    // code taken from org.opencms.synchronize.CmsSynchronize
    private File getFileInRfs(final String res) {
        final String path = this.destinationPathInRfs +
            res.substring(0, res.lastIndexOf('/'));
        final String fileName = res.substring(res.lastIndexOf('/') + 1);

        return new File(path, fileName);
    }

    /**
     * Gets the corresponding metadata file to a file resource in the VFS.
     * <p>
     *
     * @param res
     *            path to the resource inside the VFS
     * @return the corresponding file in the FS
     */
    private File getMetadataFileInRfs(final String res) {
        final String path = this.metadataPathInRfs +
            res.substring(0, res.lastIndexOf('/'));
        final String fileName = res.substring(res.lastIndexOf('/') + 1);

        return new File(path, fileName + ".xml");
    }

    /**
     * Gets the corresponding metadata file to a folder resource in the VFS.
     * <p>
     *
     * @param res
     *            path to the resource inside the VFS
     * @return the corresponding file in the FS
     */
    private File getMetadataFolderInRfs(final String res) {
        final String path = this.metadataPathInRfs + res;

        return new File(path, "~folder.xml");
    }

    /**
     * Writes the data for a resource (like access-rights) to the XML metadata
     * file.
     * <p>
     *
     * @param resource
     *            the resource to get the data from
     * @param source
     *            flag to show if the source information in the xml file must be
     *            written
     * @param resourceNode
     *            the node that embeds this resource, typically the "export"
     *            node
     * @throws CmsImportExportException
     *             if something goes wrong
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     */

    // code taken from org.opencms.importexport.CmsExport
    private void appendResourceToManifest(final CmsResource resource,
        final boolean source, final Element resourceNode)
        throws CmsImportExportException, SAXException {
        try {
            // define the file node
            final Element fileElement = resourceNode.addElement(CmsImportExportManager.N_FILE);

            // only write <source> if resource is a file
            final String sitepath = this.getCms().getSitePath(resource);
            final String fileName = this.trimResourceName(sitepath);

            if (resource.isFile() && source) {
                fileElement.addElement(CmsImportExportManager.N_SOURCE)
                           .addText(fileName);
            }

            // <destination>
            fileElement.addElement(CmsImportExportManager.N_DESTINATION)
                       .addText(fileName);
            // <type>
            fileElement.addElement(CmsImportExportManager.N_TYPE)
                       .addText(OpenCms.getResourceManager()
                                       .getResourceType(resource.getTypeId())
                                       .getTypeName());
            //  <uuidstructure>
            fileElement.addElement(CmsImportExportManager.N_UUIDSTRUCTURE)
                       .addText(resource.getStructureId().toString());

            if (resource.isFile()) {
                // <uuidresource>
                fileElement.addElement(CmsImportExportManager.N_UUIDRESOURCE)
                           .addText(resource.getResourceId().toString());
            }

            // <datelastmodified>
            fileElement.addElement(CmsImportExportManager.N_DATELASTMODIFIED)
                       .addText(CmsDateUtil.getHeaderDate(
                    resource.getDateLastModified()));

            // <userlastmodified>
            String userNameLastModified = null;

            try {
                userNameLastModified = this.getCms()
                                           .readUser(resource.getUserLastModified())
                                           .getName();
            } catch (final CmsException e) {
                userNameLastModified = OpenCms.getDefaultUsers().getUserAdmin();
            }

            // in OpenCms 6.2.3 fällt das escapeXml weg,
            // weil es allgemein im CmsXmlSaxWriter geregelt wird
            // siehe XmlHandling
            fileElement.addElement(CmsImportExportManager.N_USERLASTMODIFIED)
                       .addText(CmsEncoder.escapeXml(userNameLastModified));
            // <datecreated>
            fileElement.addElement(CmsImportExportManager.N_DATECREATED)
                       .addText(CmsDateUtil.getHeaderDate(
                    resource.getDateCreated()));

            // <usercreated>
            String userNameCreated = null;

            try {
                userNameCreated = this.getCms()
                                      .readUser(resource.getUserCreated())
                                      .getName();
            } catch (final CmsException e) {
                userNameCreated = OpenCms.getDefaultUsers().getUserAdmin();
            }

            // in OpenCms 6.2.3 f�llt das escapeXml weg,
            // weil es allgemein im CmsXmlSaxWriter geregelt wird
            // siehe XmlHandling
            fileElement.addElement(CmsImportExportManager.N_USERCREATED)
                       .addText(CmsEncoder.escapeXml(userNameCreated));

            // <release>
            if (resource.getDateReleased() != CmsResource.DATE_RELEASED_DEFAULT) {
                fileElement.addElement(CmsImportExportManager.N_DATERELEASED)
                           .addText(CmsDateUtil.getHeaderDate(
                        resource.getDateReleased()));
            }

            // <expire>
            if (resource.getDateExpired() != CmsResource.DATE_EXPIRED_DEFAULT) {
                fileElement.addElement(CmsImportExportManager.N_DATEEXPIRED)
                           .addText(CmsDateUtil.getHeaderDate(
                        resource.getDateExpired()));
            }

            // <flags>
            int resFlags = resource.getFlags();
            resFlags &= ~CmsResource.FLAG_LABELED;
            fileElement.addElement(CmsImportExportManager.N_FLAGS)
                       .addText(Integer.toString(resFlags));

            // write the properties to the manifest
            final Element propertiesElement = fileElement.addElement(CmsImportExportManager.N_PROPERTIES);
            final List properties = this.getCms()
                                        .readPropertyObjects(sitepath, false);

            // sort the properties for a well defined output order
            Collections.sort(properties);

            for (int i = 0, n = properties.size(); i < n; i++) {
                final CmsProperty property = (CmsProperty) properties.get(i);

                if (this.isIgnoredProperty(property)) {
                    continue;
                }

                this.addPropertyNode(propertiesElement, property.getName(),
                    property.getStructureValue(), false);
                this.addPropertyNode(propertiesElement, property.getName(),
                    property.getResourceValue(), true);
            }

            // Write the relations to the manifest
            final List relations = this.getCms()
                                       .getRelationsForResource(this.getCms()
                                                                    .getSitePath(resource),
                    CmsRelationFilter.TARGETS.filterNotDefinedInContent());
            CmsRelation relation = null;
            final Element relationsElement = fileElement.addElement(CmsImportExportManager.N_RELATIONS);

            // iterate over the relations
            for (final Iterator iter = relations.iterator(); iter.hasNext();) {
                relation = (CmsRelation) iter.next();

                final CmsResource target = relation.getTarget(this.getCms(),
                        CmsResourceFilter.ALL);
                final String structureId = target.getStructureId().toString();
                final String sitePath = this.getCms().getSitePath(target);
                final String relationType = relation.getType().getName();

                this.addRelationNode(relationsElement, structureId, sitePath,
                    relationType);
            }

            // append the nodes for access control entries
            final Element acl = fileElement.addElement(CmsImportExportManager.N_ACCESSCONTROL_ENTRIES);

            // read the access control entries
            final List fileAcEntries = this.getCms()
                                           .getAccessControlEntries(sitepath,
                    false);
            final Iterator i = fileAcEntries.iterator();

            // create xml elements for each access control entry
            while (i.hasNext()) {
                final CmsAccessControlEntry ace = (CmsAccessControlEntry) i.next();
                final Element a = acl.addElement(CmsImportExportManager.N_ACCESSCONTROL_ENTRY);

                // now check if the principal is a group or a user
                final int flags = ace.getFlags();
                String acePrincipalName = "";
                final CmsUUID acePrincipal = ace.getPrincipal();

                if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_ALLOTHERS) > 0) {
                    acePrincipalName = CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_NAME;
                } else if ((flags &
                        CmsAccessControlEntry.ACCESS_FLAGS_OVERWRITE_ALL) > 0) {
                    acePrincipalName = CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_NAME;
                } else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_GROUP) > 0) {
                    // the principal is a group
                    acePrincipalName = this.getCms().readGroup(acePrincipal)
                                           .getPrefixedName();
                } else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_USER) > 0) {
                    // the principal is a user
                    acePrincipalName = this.getCms().readUser(acePrincipal)
                                           .getPrefixedName();
                } else {
                    // the principal is a role
                    acePrincipalName = CmsRole.PRINCIPAL_ROLE + "." +
                        CmsRole.valueOfId(acePrincipal).getRoleName();
                }

                // in OpenCms 6.2.3 f�llt das escapeXml weg,
                // weil es allgemein im CmsXmlSaxWriter geregelt wird
                // siehe XmlHandling
                a.addElement(CmsImportExportManager.N_ACCESSCONTROL_PRINCIPAL)
                 .addText(CmsEncoder.escapeXml(acePrincipalName));
                a.addElement(CmsImportExportManager.N_FLAGS)
                 .addText(Integer.toString(flags));

                final Element b = a.addElement(CmsImportExportManager.N_ACCESSCONTROL_PERMISSIONSET);
                b.addElement(CmsImportExportManager.N_ACCESSCONTROL_ALLOWEDPERMISSIONS)
                 .addText(Integer.toString(ace.getAllowedPermissions()));
                b.addElement(CmsImportExportManager.N_ACCESSCONTROL_DENIEDPERMISSIONS)
                 .addText(Integer.toString(ace.getDeniedPermissions()));
            }

            // write the XML
            this.digestElement(resourceNode, fileElement);
        } catch (final CmsImportExportException e) {
            throw e;
        } catch (final CmsException e) {
            final CmsMessageContainer message = org.opencms.importexport.Messages.get()
                   .container(org.opencms.importexport.Messages.ERR_IMPORTEXPORT_ERROR_APPENDING_RESOURCE_TO_MANIFEST_1,
                    resource.getRootPath());
            throw new CmsImportExportException(message, e);
        }
    }

    /**
     * Adds a property node to the manifest.xml.<p>
     *
     * @param propertiesElement the parent element to append the node to
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     * @param shared if <code>true</code>, add a shared property attribute to the generated property node
     */

    // code taken from CmsExport
    private void addPropertyNode(final Element propertiesElement,
        final String propertyName, final String propertyValue,
        final boolean shared) {
        if (propertyValue != null) {
            final Element propertyElement = propertiesElement.addElement(CmsImportExportManager.N_PROPERTY);

            if (shared) {
                // add "type" attribute to the property node in case of a shared/resource property value
                propertyElement.addAttribute(CmsImportExportManager.N_PROPERTY_ATTRIB_TYPE,
                    CmsImportExportManager.N_PROPERTY_ATTRIB_TYPE_SHARED);
            }

            propertyElement.addElement(CmsImportExportManager.N_NAME)
                           .addText(propertyName);
            propertyElement.addElement(CmsImportExportManager.N_VALUE)
                           .addCDATA(propertyValue);
        }
    }

    /**
     * Adds a relation node to the <code>manifest.xml</code>.<p>
     *
     * @param relationsElement the parent element to append the node to
     * @param structureId the structure id of the target relation
     * @param sitePath the site path of the target relation
     * @param relationType the type of the relation
     */

    // code taken from CmsExport
    private void addRelationNode(final Element relationsElement,
        final String structureId, final String sitePath,
        final String relationType) {
        if ((structureId != null) && (sitePath != null) &&
                (relationType != null)) {
            final Element relationElement = relationsElement.addElement(CmsImportExportManager.N_RELATION);

            relationElement.addElement(CmsImportExportManager.N_RELATION_ATTRIBUTE_ID)
                           .addText(structureId);
            relationElement.addElement(CmsImportExportManager.N_RELATION_ATTRIBUTE_PATH)
                           .addText(sitePath);
            relationElement.addElement(CmsImportExportManager.N_RELATION_ATTRIBUTE_TYPE)
                           .addText(relationType);
        }
    }

    /**
     * Cuts leading and trailing '/' from the given resource name.
     * <p>
     *
     * @param resourceName
     *            the absolute path of a resource
     * @return the trimmed resource name
     */

    // code taken from org.opencms.importexport.CmsExport
    private String trimResourceName(final String resourceName) {
        String result = resourceName;

        if (result.startsWith("/")) {
            result = result.substring(1);
        }

        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * Checks if a property should be written to the export or not.
     * <p>
     *
     * @param property
     *            the property to check
     * @return if true, the property is to be ignored, otherwise it should be
     *         exported
     */

    // code taken from org.opencms.importexport.CmsExport
    private boolean isIgnoredProperty(final CmsProperty property) {
        if (property == null) {
            return true;
        }

        // default implementation is to export all properties not null
        return false;
    }

    /**
     * Reads all file nodes plus their meta-information (properties, ACL) from
     * the metadata XML and imports them as Cms resources to the VFS.
     * <p>
     *
     * @param content
     *            content of the resource to import to VFS
     * @return the CmsResource after importing
     * @throws CmsImportExportException
     *             if something goes wrong
     */
    // code taken from org.opencms.importexport.CmsImportVersion5
    private CmsResource readResourcesFromManifest(final byte[] content)
        throws CmsException {
        CmsResource res = null;
        String destination = null;
        String uuidresource = null;
        String uuidstructure = null;
        String userlastmodified = null;
        String usercreated = null;
        String flags = null;
        String timestamp = null;
        long datelastmodified = 0;
        long datecreated = 0;
        long datereleased = 0;
        long dateexpired = 0;
        List acentryNodes = null;
        Element currentElement = null;
        Element currentEntry = null;
        List properties = null;
        final CmsImportExportManager iomanager = OpenCms.getImportExportManager();

        // get list of immutable resources
        List immutableResources = iomanager.getImmutableResources();

        if (immutableResources == null) {
            immutableResources = Collections.EMPTY_LIST;
        }

        // get list of ignored properties
        List ignoredProperties = iomanager.getIgnoredProperties();

        if (ignoredProperties == null) {
            ignoredProperties = Collections.EMPTY_LIST;
        }

        // get the desired page type for imported pages
        // not used otherwise m_convertToXmlPage = iomanager.convertToXmlPage();
        try {
            currentElement = (Element) this.getDocXml()
                                           .selectNodes("//" +
                    CmsImportExportManager.N_FILE).get(0);
            // <source>
            // DET source = CmsImport.getChildElementTextValue(currentElement,
            // CmsImportExportManager.N_SOURCE);
            // <destination>
            destination = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_DESTINATION);

            // <type>
            final String typeName = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_TYPE);
            final I_CmsResourceType type = OpenCms.getResourceManager()
                                                  .getResourceType(typeName);

            // <uuidstructure>
            uuidstructure = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_UUIDSTRUCTURE);

            // <uuidresource>
            if (!type.isFolder()) {
                uuidresource = XmlHandling.getChildElementTextValue(currentElement,
                        CmsImportExportManager.N_UUIDRESOURCE);
            } else {
                uuidresource = null;
            }

            // <datelastmodified>
            timestamp = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_DATELASTMODIFIED);

            if (timestamp != null) {
                datelastmodified = this.convertTimestamp(timestamp);
            } else {
                datelastmodified = System.currentTimeMillis();
            }

            // <userlastmodified>
            userlastmodified = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_USERLASTMODIFIED);
            userlastmodified = iomanager.translateUser(userlastmodified);

            // <datecreated>
            timestamp = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_DATECREATED);

            if (timestamp != null) {
                datecreated = this.convertTimestamp(timestamp);
            } else {
                datecreated = System.currentTimeMillis();
            }

            // <usercreated>
            usercreated = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_USERCREATED);
            usercreated = iomanager.translateUser(usercreated);

            // <datereleased>
            timestamp = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_DATERELEASED);

            if (timestamp != null) {
                datereleased = this.convertTimestamp(timestamp);
            } else {
                datereleased = CmsResource.DATE_RELEASED_DEFAULT;
            }

            // <dateexpired>
            timestamp = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_DATEEXPIRED);

            if (timestamp != null) {
                dateexpired = this.convertTimestamp(timestamp);
            } else {
                dateexpired = CmsResource.DATE_EXPIRED_DEFAULT;
            }

            // <flags>
            flags = XmlHandling.getChildElementTextValue(currentElement,
                    CmsImportExportManager.N_FLAGS);

            // apply name translation and import path
            String translatedName = this.getCms().getRequestContext()
                                        .addSiteRoot(destination); // m_importPath

            if (type.isFolder() && !CmsResource.isFolder(translatedName)) {
                translatedName += "/";
            }

            // check if this resource is immutable
            final boolean resourceNotImmutable = this.checkImmutable(translatedName,
                    immutableResources);
            translatedName = this.getCms().getRequestContext()
                                 .removeSiteRoot(translatedName);

            // if the resource is not immutable and not on the exclude list,
            // import it
            if (resourceNotImmutable) {

                // get all properties
                properties = this.readPropertiesFromManifest(currentElement,
                        ignoredProperties);
                // import the resource
                res = this.importResource(content, translatedName, type,
                        uuidstructure, uuidresource, datelastmodified,
                        userlastmodified, datecreated, usercreated,
                        datereleased, dateexpired, flags, properties);

                // if the resource was imported add the access control entrys if
                // available
                if (res == null) {

                    // resource import failed, since no CmsResource was created
                    this.getReport()
                        .print(org.opencms.importexport.Messages.get()
                                                                .container(org.opencms.importexport.Messages.RPT_SKIPPING_0),
                        I_CmsReport.FORMAT_NOTE);
                    this.getReport()
                        .println(org.opencms.report.Messages.get()
                                                            .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                            translatedName));
                } else {

                    final List aceList = new ArrayList();
                    // write all imported access control entries for this file
                    acentryNodes = currentElement.selectNodes("*/" +
                            CmsImportExportManager.N_ACCESSCONTROL_ENTRY);

                    // collect all access control entries
                    for (int j = 0; j < acentryNodes.size(); j++) {
                        currentEntry = (Element) acentryNodes.get(j);

                        // get the data of the access control entry
                        final String id = XmlHandling.getChildElementTextValue(currentEntry,
                                CmsImportExportManager.N_ACCESSCONTROL_PRINCIPAL);
                        String principalId = new CmsUUID().toString();
                        String principal = id.substring(id.indexOf('.') + 1,
                                id.length());

                        try {
                            if (id.startsWith(I_CmsPrincipal.PRINCIPAL_GROUP)) {
                                principal = OpenCms.getImportExportManager()
                                                   .translateGroup(principal);
                                principalId = this.getCms().readGroup(principal)
                                                  .getId().toString();
                            } else if (id.startsWith(
                                        I_CmsPrincipal.PRINCIPAL_USER)) {
                                principal = OpenCms.getImportExportManager()
                                                   .translateUser(principal);
                                principalId = this.getCms().readUser(principal)
                                                  .getId().toString();
                            } else if (id.startsWith(CmsRole.PRINCIPAL_ROLE)) {
                                principalId = CmsRole.valueOfRoleName(principal)
                                                     .getId().toString();
                            } else if (id.equalsIgnoreCase(
                                        CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_NAME)) {
                                principalId = CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_ID.toString();
                            } else if (id.equalsIgnoreCase(
                                        CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_NAME)) {
                                principalId = CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_ID.toString();
                            }

                            final String acflags = XmlHandling.getChildElementTextValue(currentEntry,
                                    CmsImportExportManager.N_FLAGS);
                            final String allowed = ((Element) currentEntry.selectNodes(
                                    "./" +
                                    CmsImportExportManager.N_ACCESSCONTROL_PERMISSIONSET +
                                    "/" +
                                    CmsImportExportManager.N_ACCESSCONTROL_ALLOWEDPERMISSIONS)
                                                                          .get(0)).getTextTrim();
                            final String denied = ((Element) currentEntry.selectNodes(
                                    "./" +
                                    CmsImportExportManager.N_ACCESSCONTROL_PERMISSIONSET +
                                    "/" +
                                    CmsImportExportManager.N_ACCESSCONTROL_DENIEDPERMISSIONS)
                                                                         .get(0)).getTextTrim();
                            // add the entry to the list
                            aceList.add(this.getImportAccessControlEntry(res,
                                    principalId, allowed, denied, acflags));
                        } catch (final CmsException e) {
                            // user or group of ACE might not exist in target
                            // system, ignore ACE
                            this.getReport().println(e);
                        }
                    }

                    this.importAccessControlEntries(res, aceList);

                    // Add the relations for the resource.
                    this.importRelations(res, currentElement);

                    if (OpenCms.getResourceManager()
                                   .getResourceType(res.getTypeId()) instanceof I_CmsLinkParseable) {
                        // store for later use
                        this.m_parseables.add(res);
                    }
                }
            } else { // immutable
                     // skip the file import, just print out the information to the
                     // report

                this.getReport()
                    .print(org.opencms.importexport.Messages.get()
                                                            .container(org.opencms.importexport.Messages.RPT_SKIPPING_0),
                    I_CmsReport.FORMAT_NOTE);
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_ARGUMENT_1,
                        translatedName));
            }
        } catch (final CmsLoaderException e) {
            this.getReport().println(e);

            final CmsMessageContainer message = org.opencms.importexport.Messages.get()
                             .container(org.opencms.importexport.Messages.ERR_IMPORTEXPORT_ERROR_IMPORTING_RESOURCES_0);
            throw new CmsImportExportException(message, e);
        }

        return res;
    }

    /**
     * Reads all the relations of the resource from the <code>manifest.xml</code> file
     * and adds them to the according resource.<p>
     *
     * @param resource the resource to import the relations for
     * @param parentElement the current element
     */

    // code taken from CmsImportVersion5
	protected void importRelations(final CmsResource resource,
			final Element parentElement) {
		final List<CmsRelation> relations = getRelationsForElement(
				resource.getResourceId(), resource.getRootPath(), parentElement);
		if (!relations.isEmpty()) {
			this.m_importedRelations.put(resource.getRootPath(), relations);
		}
	}
    
	private List<CmsRelation> getRelationsForElement(
			final CmsUUID resourceUUID, final String resourcePath,
			final Element parentElement) {
		// Get the nodes for the relations
		final List relationElements = parentElement.selectNodes("./"
				+ CmsImportExportManager.N_RELATIONS + "/"
				+ CmsImportExportManager.N_RELATION);

		final List<CmsRelation> relations = new ArrayList<CmsRelation>();

		// iterate over the nodes
		final Iterator itRelations = relationElements.iterator();

		while (itRelations.hasNext()) {
			final Element relationElement = (Element) itRelations.next();
			final String structureID = XmlHandling.getChildElementTextValue(
					relationElement,
					CmsImportExportManager.N_RELATION_ATTRIBUTE_ID);
			final String targetPath = XmlHandling.getChildElementTextValue(
					relationElement,
					CmsImportExportManager.N_RELATION_ATTRIBUTE_PATH);
			final String relationType = XmlHandling.getChildElementTextValue(
					relationElement,
					CmsImportExportManager.N_RELATION_ATTRIBUTE_TYPE);
			final CmsUUID targetId = new CmsUUID(structureID);
			final CmsRelationType type = CmsRelationType.valueOf(relationType);

			final CmsRelation relation = new CmsRelation(resourceUUID,
					resourcePath, targetId, targetPath, type);

			relations.add(relation);
		}
		return relations;
	}

    /**
     * Convert a given timestamp from a String format to a long value.
     * <p>
     *
     * The timestamp is either the string representation of a long value (old
     * export format) or a user-readable string format.
     *
     * @param timestamp
     *            timestamp to convert
     * @return long value of the timestamp
     */

    // code taken from org.opencms.importexport.CmsImportVersion4
    private long convertTimestamp(final String timestamp) {
        long value = 0;

        // try to parse the timestamp string
        // if it successes, its an old style long value
        try {
            value = Long.parseLong(timestamp);
        } catch (final NumberFormatException e) {
            // the timestamp was in in a user-readable string format, create the
            // long value form it
            try {
                value = CmsDateUtil.parseHeaderDate(timestamp);
            } catch (final ParseException pe) {
                value = System.currentTimeMillis();
            }
        }

        return value;
    }

    /**
     * Reads all properties below a specified parent element from the metadata
     * XML.
     * <p>
     *
     * @param parentElement
     *            the current file node
     * @param ignoredPropertyKeys
     *            a list of properies to be ignored
     *
     * @return a list with all properties
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private List readPropertiesFromManifest(final Element parentElement,
        final List ignoredPropertyKeys) {
        // all imported Cms property objects are collected in map first
        // for faster access
        final Map properties = new HashMap();
        CmsProperty property = null;
        final List propertyElements = parentElement.selectNodes("./" +
                CmsImportExportManager.N_PROPERTIES + "/" +
                CmsImportExportManager.N_PROPERTY);
        Element propertyElement = null;
        String key = null;
        String value = null;
        Attribute attrib = null;

        // iterate over all property elements
        for (int i = 0, n = propertyElements.size(); i < n; i++) {
            propertyElement = (Element) propertyElements.get(i);
            key = XmlHandling.getChildElementTextValue(propertyElement,
                    CmsImportExportManager.N_NAME);

            if ((key == null) || ignoredPropertyKeys.contains(key)) {
                // continue if the current property (key) should be ignored or
                // is null
                continue;
            }

            // all Cms properties are collected in a map keyed by their property
            // keys
            property = (CmsProperty) properties.get(key);

            if (property == null) {
                property = new CmsProperty();
                property.setName(key);
                property.setAutoCreatePropertyDefinition(true);
                properties.put(key, property);
            }

            value = XmlHandling.getChildElementTextValue(propertyElement,
                    CmsImportExportManager.N_VALUE);

            if (value == null) {
                value = "";
            }

            attrib = propertyElement.attribute(CmsImportExportManager.N_PROPERTY_ATTRIB_TYPE);

            if ((attrib != null) &&
                    attrib.getValue()
                              .equals(CmsImportExportManager.N_PROPERTY_ATTRIB_TYPE_SHARED)) {
                // it is a shared/resource property value
                property.setResourceValue(value);
            } else {
                // it is an individual/structure value
                property.setStructureValue(value);
            }
        }

        return new ArrayList(properties.values());
    }

    /**
     * Checks if the resources is in the list of immutable resources.
     * <p>
     *
     * @param translatedName
     *            the name of the resource
     * @param immutableResources
     *            the list of the immutable resources
     * @return true or false
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private boolean checkImmutable(final String translatedName,
        final List immutableResources) {
        boolean resourceNotImmutable = true;

        if (immutableResources.contains(translatedName)) {
            // this resource must not be modified by an import if it already
            // exists
            // our siteroot is and remains "/"
            // m_cms.getRequestContext().saveSiteRoot();
            try {
                // m_cms.getRequestContext().setSiteRoot("/");
                this.getCms().readResource(translatedName);
                resourceNotImmutable = false;
            } catch (final CmsException e) {
                // resourceNotImmutable will be true
                // } finally {
                // m_cms.getRequestContext().restoreSiteRoot();
            }
        }

        return resourceNotImmutable;
    }

    /**
     * Imports a resource (file or folder) into the cms.<p>
     *
     * @param source the path to the source-file
     * @param destination the path to the destination-file in the cms
     * @param type the resource type name of the file
     * @param uuidstructure the structure uuid of the resource
     * @param uuidresource the resource uuid of the resource
     * @param datelastmodified the last modification date of the resource
     * @param userlastmodified the user who made the last modifications to the resource
     * @param datecreated the creation date of the resource
     * @param usercreated the user who created
     * @param datereleased the release date of the resource
     * @param dateexpired the expire date of the resource
     * @param flags the flags of the resource
     * @param properties a list with properties for this resource
     *
     * @return imported resource
     */

    // code taken from org.opencms.importexport.CmsImportVersion5
    private CmsResource importResource(final byte[] content,
        final String destination, final I_CmsResourceType type,
        final String uuidstructure, final String uuidresource,
        final long datelastmodified, final String userlastmodified,
        final long datecreated, final String usercreated,
        final long datereleased, final long dateexpired, final String flags,
        final List properties) throws CmsException {

        CmsResource result = null;

            int size = 0;

            if (content != null) {
                size = content.length;
            }

            // get UUIDs for the user
            CmsUUID newUserlastmodified;
            CmsUUID newUsercreated;

            // check if user created and user lastmodified are valid users in this system.
            // if not, use the current user
            try {
                newUserlastmodified = this.getCms().readUser(userlastmodified)
                                          .getId();
            } catch (final CmsException e) {
                newUserlastmodified = this.getCms().getRequestContext()
                                          .currentUser().getId();

                // datelastmodified = System.currentTimeMillis();
            }

            try {
                newUsercreated = this.getCms().readUser(usercreated).getId();
            } catch (final CmsException e) {
                newUsercreated = this.getCms().getRequestContext().currentUser()
                                     .getId();

                // datecreated = System.currentTimeMillis();
            }

            // get UUID for the structure
            CmsUUID newUuidstructure = null;

            if (uuidstructure != null) {
                // create a UUID from the provided string
                newUuidstructure = new CmsUUID(uuidstructure);
            } else {
                // if null generate a new structure id
                newUuidstructure = new CmsUUID();
            }

            // get UUIDs for the resource and content
            CmsUUID newUuidresource = null;

            if ((uuidresource != null) && (!type.isFolder())) {
                // create a UUID from the provided string
                newUuidresource = new CmsUUID(uuidresource);
            } else {
                // folders get always a new resource record UUID
                newUuidresource = new CmsUUID();
            }

            // create a new CmsResource
            final CmsResource resource = new CmsResource(newUuidstructure,
                    newUuidresource, destination, type.getTypeId(),
                    type.isFolder(), Integer.valueOf(flags).intValue(),
                    this.getCms().getRequestContext().currentProject().getUuid(),
                    CmsResource.STATE_NEW, datecreated, newUsercreated,
                    datelastmodified, newUserlastmodified, datereleased,
                    dateexpired, 1, size, System.currentTimeMillis(), 0);

            // import this resource in the VFS
            result = this.getCms()
                         .importResource(destination, resource, content,
                    properties);

            if (result != null) {
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            }

        return result;
    }

    /**
     * Creates a new access control entry and stores it for later write out.
     *
     * @param res
     *            the resource
     * @param id
     *            the id of the principal
     * @param allowed
     *            the allowed permissions
     * @param denied
     *            the denied permissions
     * @param flags
     *            the flags
     *
     * @return the created ACE
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private CmsAccessControlEntry getImportAccessControlEntry(
        final CmsResource res, final String id, final String allowed,
        final String denied, final String flags) {
        return new CmsAccessControlEntry(res.getResourceId(), new CmsUUID(id),
            Integer.parseInt(allowed), Integer.parseInt(denied),
            Integer.parseInt(flags));
    }

    /**
     * Writes alread imported access control entries for a given resource.
     *
     * @param resource
     *            the resource assigned to the access control entries
     * @param aceList
     *            the access control entries to create
     */

    // code taken from org.opencms.importexport.A_CmsImport
    private void importAccessControlEntries(final CmsResource resource,
        final List aceList) {
        if (aceList.isEmpty()) {
            // no ACE in the list
            return;
        }

        try {
            this.getCms().importAccessControlEntries(resource, aceList);
        } catch (final CmsException exc) {
            this.getReport()
                .println(org.opencms.importexport.Messages.get()
                                                          .container(org.opencms.importexport.Messages.RPT_IMPORT_ACL_DATA_FAILED_0),
                I_CmsReport.FORMAT_WARNING);
        }
    }

    /**
     * Rewrites all parseable files, to assure link check.<p>
     */

    // code taken from org.opencms.importexport.CmsImportVersion5
    protected void rewriteParseables() {
        if (this.m_parseables.isEmpty()) {
            return;
        }

        getReport()
            .println(org.opencms.importexport.Messages.get()
                                                      .container(org.opencms.importexport.Messages.RPT_START_PARSE_LINKS_0),
            I_CmsReport.FORMAT_HEADLINE);

        int i = 0;
        final Iterator it = this.m_parseables.iterator();

        while (it.hasNext()) {
            final CmsResource res = (CmsResource) it.next();

            getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_SUCCESSION_2,
                    String.valueOf(i + 1),
                    String.valueOf(this.m_parseables.size())),
                I_CmsReport.FORMAT_NOTE);

            getReport()
                .print(org.opencms.importexport.Messages.get()
                                                        .container(org.opencms.importexport.Messages.RPT_PARSE_LINKS_FOR_1,
                    getCms().getSitePath(res)), I_CmsReport.FORMAT_NOTE);
            getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_DOTS_0));

            try {
                // make sure the date last modified is kept...
                final CmsFile file = getCms().readFile(res);
                file.setDateLastModified(res.getDateLastModified());
                getCms().writeFile(file);

                getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            } catch (final Throwable e) {
                getReport().addWarning(e);
                getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_FAILED_0),
                    I_CmsReport.FORMAT_ERROR);
            }

            i++;
        }

        getReport()
            .println(org.opencms.importexport.Messages.get()
                                                      .container(org.opencms.importexport.Messages.RPT_END_PARSE_LINKS_0),
            I_CmsReport.FORMAT_HEADLINE);
    }

    /**
     * Imports the relations.<p>
     */

    // code taken from org.opencms.importexport.CmsImportVersion5
    protected void importRelations() {
        if (this.m_importedRelations.isEmpty()) {
            return;
        }

        this.getReport()
            .println(org.opencms.importexport.Messages.get()
                                                      .container(org.opencms.importexport.Messages.RPT_START_IMPORT_RELATIONS_0),
            I_CmsReport.FORMAT_HEADLINE);

        int i = 0;
        final Iterator it = this.m_importedRelations.entrySet().iterator();

        while (it.hasNext()) {
            final Map.Entry entry = (Map.Entry) it.next();
            final String resourcePath = (String) entry.getKey();
            final List relations = (List) entry.getValue();

            this.getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_SUCCESSION_2,
                    String.valueOf(i + 1),
                    String.valueOf(this.m_importedRelations.size())),
                I_CmsReport.FORMAT_NOTE);

            this.getReport()
                .print(org.opencms.importexport.Messages.get()
                                                        .container(org.opencms.importexport.Messages.RPT_IMPORTING_RELATIONS_FOR_2,
                    resourcePath, Integer.valueOf(relations.size())),
                I_CmsReport.FORMAT_NOTE);
            this.getReport()
                .print(org.opencms.report.Messages.get()
                                                  .container(org.opencms.report.Messages.RPT_DOTS_0));

            boolean withErrors = false;
            final Iterator itRelations = relations.iterator();

            while (itRelations.hasNext()) {
                final CmsRelation relation = (CmsRelation) itRelations.next();

                try {
                    // Add the relation to the resource
                    this.getCms()
                        .importRelation(this.getCms()
                                            .getSitePath(relation.getSource(
                                this.getCms(), CmsResourceFilter.ALL)),
                        this.getCms()
                            .getSitePath(relation.getTarget(this.getCms(),
                                CmsResourceFilter.ALL)),
                        relation.getType().getName());
                } catch (final CmsException e) {
                    this.getReport().addWarning(e);
                    withErrors = true;
                }
            }

            if (!withErrors) {
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            } else {
                this.getReport()
                    .println(org.opencms.report.Messages.get()
                                                        .container(org.opencms.report.Messages.RPT_FAILED_0),
                    I_CmsReport.FORMAT_ERROR);
            }

            i++;
        }

        this.getReport()
            .println(org.opencms.importexport.Messages.get()
                                                      .container(org.opencms.importexport.Messages.RPT_END_IMPORT_RELATIONS_0),
            I_CmsReport.FORMAT_HEADLINE);
    }

    /**
     * Set the path in RFS to use for content synchronisation.
     *
     * Used when OpenCms integrated synchronisation is called.
     *
     * @param pathInRfs
     *            path in RFS
     */
    public final void setDestinationPathInRfs(final String pathInRfs) {
        this.destinationPathInRfs = pathInRfs;
    }

    /**
     * Set the path in RFS to use for metadata synchronisation.
     *
     * Used when OpenCms integrated synchronisation is called.
     *
     * @param pathInRfs
     *            path in RFS
     */
    public final void setMetadataPathInRfs(final String pathInRfs) {
        this.metadataPathInRfs = pathInRfs;
    }

    private boolean isIgnorableFile(File file) {

    	if (file == null) {
    		return true;
    	}
        if (!file.exists()) {
            return true;
        }

        return this.ignoredFilesFilter.accept(file);
    }

    private void computeIgnoredNames(List<String> ignoredNames, List<String> notIgnoredNames) {

        Set<String> ignoredGlobPatterns = new LinkedHashSet<String>();
        for (String name : DEFAULT_IGNORED_NAMES) {
            ignoredGlobPatterns.add(name);
        }
        if (ignoredNames != null) {
            ignoredGlobPatterns.addAll(ignoredNames);
        }
        if (notIgnoredNames != null) {
            ignoredGlobPatterns.removeAll(notIgnoredNames);
        }

        debugReport("VfsSync configuration. Ignored filename patterns: " + ignoredGlobPatterns);

        this.ignoredFilesFilter = new WildcardFileFilter(new ArrayList(ignoredGlobPatterns));
    }
}
