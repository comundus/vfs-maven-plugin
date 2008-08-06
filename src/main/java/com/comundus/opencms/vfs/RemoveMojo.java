package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.List;


/**
 * A Maven2 plugin Goal to remove VFS resources.
 *
 * @goal remove
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class RemoveMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsRemove";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, List.class, String.class
        };

    /**
     * List of VFS paths to remove from VFS. May contain file paths as well as
     * folder paths.
     *
     * @parameter
     */
    private List removes;

    /**
     * Removes resources from VFS.
     *
     * Only if "remove" paths are configured; otherwise it's assumed
     * to be a non-VFS project in a multi project build.
     *
     * @throws MojoExecutionException in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
        if (this.removes == null) {
            this.getLog().info("Skipping non-vfs-remove project");

            return; // it's ok, nothing to remove
        }

        ClassLoader originalClassLoader = Thread.currentThread()
                                                .getContextClassLoader();
        ClassLoader classloader = this.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
            Class invokeMeClass = classloader.loadClass(RemoveMojo.SHELLCLASS);
            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
            Object o = constr.newInstance(new Object[] {  });
            Method main = invokeMeClass.getMethod(AbstractVfsMojo.SHELLMETHOD,
                    RemoveMojo.SHELLPARAMETERS);
            main.invoke(o,
                new Object[] {
                    getWebappDirectory(), this.removes, getAdminPassword()
                });
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                RemoveMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                RemoveMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + RemoveMojo.SHELLCLASS,
                e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + RemoveMojo.SHELLCLASS,
                e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + RemoveMojo.SHELLCLASS,
                e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" + RemoveMojo.SHELLCLASS, e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
