package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.VfsPublish;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.List;


/**
 * A Maven2 plugin Goal to publish synchronized content and writing
 * ExportPoints.
 *
 * @goal publish
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class PublishMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsPublish";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, List.class, String.class
        };

    /**
     * The List of folder paths in VFS to publish.
     *
     * @parameter
     */
    private List syncVFSPaths;

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
     * Publishes resources in VFS.
     *
     * Only if VFS synchronization paths are configured; otherwise it's assumed
     * to be a non-VFS project in a multi project build.
     *
     * @throws MojoExecutionException in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
    	if (this.isSkipVfs()){
    		this.getLog().info("Skipping VFS plugin");
    		return;
    	}

    	if (this.syncVFSPaths == null && this.syncResources==null) {
            this.getLog().info("Skipping non-vfs project");

            return; // it's ok, nothing to publish
        }

//        ClassLoader originalClassLoader = Thread.currentThread()
//                                                .getContextClassLoader();
//        ClassLoader classloader = this.getClassLoader();
//        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
//            Class invokeMeClass = classloader.loadClass(PublishMojo.SHELLCLASS);
//            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
//            Object o = constr.newInstance(new Object[] {  });
//            Method main = invokeMeClass.getMethod("execute",
//                    PublishMojo.SHELLPARAMETERS);
        	VfsPublish publish = new VfsPublish();
        	publish.execute(
                    getWebappDirectory(), this.syncVFSPaths,this.syncResources, getAdminPassword()
                );
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                PublishMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                PublishMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                PublishMojo.SHELLCLASS, e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                PublishMojo.SHELLCLASS, e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                PublishMojo.SHELLCLASS, e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" + PublishMojo.SHELLCLASS, e);
        } catch (Exception e) {
        	throw new MojoExecutionException(
                    "Failed to instantiate (abstract!)" + PublishMojo.SHELLCLASS, e);
		} finally {
            // Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
