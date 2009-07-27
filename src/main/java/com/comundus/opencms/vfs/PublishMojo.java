package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

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
     * Publishes resources in VFS.
     *
     * Only if VFS synchronization paths are configured; otherwise it's assumed
     * to be a non-VFS project in a multi project build.
     *
     * @throws MojoExecutionException in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
        if (this.syncVFSPaths == null) {
            this.getLog().info("Skipping non-vfs project");

            return; // it's ok, nothing to publish
        }

        ClassLoader originalClassLoader = Thread.currentThread()
                                                .getContextClassLoader();
        ClassLoader classloader = this.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
            Class invokeMeClass = classloader.loadClass(PublishMojo.SHELLCLASS);
            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
            Object o = constr.newInstance(new Object[] {  });
            Method main = invokeMeClass.getMethod("execute",
                    PublishMojo.SHELLPARAMETERS);
            main.invoke(o,
                new Object[] {
                    getWebappDirectory(), this.syncVFSPaths, getAdminPassword()
                });
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
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
