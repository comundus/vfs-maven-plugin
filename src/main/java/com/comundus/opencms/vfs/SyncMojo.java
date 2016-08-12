//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
package com.comundus.opencms.vfs;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.VfsSync;

/**
 * A Maven2 plugin Goal to synchronise VFS content and metadata with source
 * folder(s).
 *
 * @goal sync
 * @requiresDependencyResolution 
 */

public class SyncMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsSync";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETER = new Class[] {
            String.class, String.class, String.class, List.class, String.class
        };

    /**
     * Source directory storing synchronized VFS content. Includes a
     * #synclist.txt file which is NOT to be checked into version control.
     *
     * @parameter default-value="${basedir}/src/main/vfs"
     * @required
     */
    private String syncSourceDirectory;

    /**
     * Source directory storing the metadata of synchronized VFS content. Folder
     * metadata are stored in a file "~folder.xml" within that folder. File
     * metadata are stored in a file with ".xml" appended to the original content
     * file name.
     *
     * @parameter default-value="${basedir}/src/main/vfs-metadata"
     * @required
     */
    private String syncMetadataDirectory;

    /**
     * List of VFS folder paths to synchronize.
     *
     * @parameter
     */
    private List<String> syncVFSPaths;

    /**
     * List of VFS resources to synchronize.<br/>
     * Folder resources allow exclude entries
     * <br/><br/>
     *
     * Example Call:<pre>
     * &lt;syncResources&gt;
     *   &lt;syncResource&gt;
     *     &lt;resource&gt;/system/workplace/&lt;/resource&gt;
     *       &lt;excludes&gt;
     *         &lt;exclude&gt;/system/workplace/tools/&lt;/exclude&gt;
     *         &lt;exclude&gt;/system/workplace/resources/&lt;/exclude&gt;
     *       &lt;/excludes&gt;
     *   &lt;/syncResource&gt;
     *   &lt;syncResource&gt;
     *     &lt;resource&gt;/system/workplace/tools/picture.gif&lt;/resource&gt;&lt;!-- This is a file --&gt;
     *   &lt;/syncResource&gt;
     * &lt;/syncResources&gt;
     *</pre>
     *
     * @parameter
     */
    private List <SyncResource>syncResources;

    /**
     * List of name patterns to add to the ignored list.
     * <p>The original list is taken from ANT and contains, among others, .git, .svn and CVS: 
     * http://ant.apache.org/manual/dirtasks.html#defaultexcludes
     * @parameter
     */
    private List <String>ignoredNames;

    /**
     * List of name patterns to remove from the ignored list
     * @parameter
     */
    private List <String> notIgnoredNames;
    
    /**
     * Delete RFS resources.
     * 
     * <p>To avoid problems with version control systems, the default behaviour when files have been deleted from
     * the VFS, is to print a warning for each resource. If this parameter is set to {@code true}, the files and folders
     * will be deleted from the RFS. 
     * @parameter default-value="false"
     */
    private boolean deleteRFSResources;

    /**
     * Performs VFS synchronisation.
     *
     * Only if VFS synchronization paths are configured; otherwise it's assumed
     * to be a non-VFS project in a multi project build.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {

    	if (this.isSkipVfs()) {
    		this.getLog().info("Skipping VFS plugin");
    		return;
    	}

        if (this.syncVFSPaths == null && this.syncResources == null) {
            this.getLog().info("Skipping non-vfs project");

            return; // it's ok, nothing to sync
        }

//        ClassLoader originalClassLoader = Thread.currentThread()
//                                                .getContextClassLoader();
//        ClassLoader classloader = this.getClassLoader();
//        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
//            Class invokeMeClass = classloader.loadClass(SyncMojo.SHELLCLASS);
//            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
//            Object o = constr.newInstance(new Object[] {  });
//            Method main = invokeMeClass.getMethod(AbstractVfsMojo.SHELLMETHOD,
//                    SyncMojo.SHELLPARAMETER);
        	if (getLog().isDebugEnabled()) {
        		printSyncResources();
        	}
        	VfsSync sync = new VfsSync();
        	sync.setDebugEnabled(this.getLog().isDebugEnabled());
                sync.execute(
                    getWebappDirectory(), this.syncSourceDirectory, this.syncMetadataDirectory, this.syncVFSPaths,
                    this.syncResources, this.ignoredNames, this.notIgnoredNames, this.deleteRFSResources, getAdminPassword());
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                SyncMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                SyncMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + SyncMojo.SHELLCLASS, e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + SyncMojo.SHELLCLASS, e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + SyncMojo.SHELLCLASS, e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate " + SyncMojo.SHELLCLASS, e);
        } catch (Exception e) {
        	throw new MojoExecutionException(
                    "Undetermined error executing " + SyncMojo.SHELLCLASS, e);
		} finally {
            // Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private void printSyncResources() {

	if (this.syncResources != null) {
	    getLog().debug("SyncResources: " + this.syncResources.size() + " elements");
	    for (SyncResource res:(List<SyncResource>) this.syncResources) {
		String resTxt = "  Resource: " + res;
		if (res.getExcludes() != null && res.getExcludes().length > 0) {
		    resTxt += "(";
		    for (String exclude:res.getExcludes()) {
			resTxt += ("-" + exclude + " ");
		    }
		    resTxt += ")";
		}
		getLog().debug(resTxt);
	    }
	} else {
	    getLog().debug("No SyncResources elements");
	}

	if (this.syncVFSPaths != null) {
	    getLog().debug("SyncVFSPaths: " + this.syncVFSPaths.size() + " elements");
	    for (String res:(List<String>) this.syncVFSPaths) {
		getLog().debug("  VFSPath: " + res);
	    }
	}
    }
}
