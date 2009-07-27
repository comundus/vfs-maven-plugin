package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.VfsSync;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.List;


/**
 * A Maven2 plugin Goal to synchronise VFS content and metadata with source
 * folder(s).
 *
 * @goal sync
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
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
     * @parameter expression="${basedir}/src/main/vfs"
     * @required
     */
    private String syncSourceDirectory;

    /**
     * Source directory storing the metadata of synchronized VFS content. Folder
     * metadata are stored in a file "~folder.xml" within that folder. File
     * metadata are stored in a file with ".xml" appended to the original content
     * file name.
     *
     * @parameter expression="${basedir}/src/main/vfs-metadata"
     * @required
     */
    private String syncMetadataDirectory;

    /**
     * List of VFS folder paths to synchronize.
     *
     * @parameter
     */
    private List syncVFSPaths;

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
        if (this.syncVFSPaths == null) {
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
        	VfsSync sync = new VfsSync();
            sync.execute(
                    getWebappDirectory(), this.syncSourceDirectory,
                    this.syncMetadataDirectory, this.syncVFSPaths,
                    getAdminPassword()
                );
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
                "Failed to instantiate (abstract!)" + SyncMojo.SHELLCLASS, e);
        } catch (Exception e) {
        	throw new MojoExecutionException(
                    "Failed to instantiate (abstract!)" + SyncMojo.SHELLCLASS, e);
		} finally {
            // Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
