//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.VfsSetup;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.List;

/**
 * A Maven2 plugin Goal to initialize VFS after creating the tables.
 *
 * @goal setup
 */
public class SetupMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsSetup";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, String.class, List.class, String.class
        };

    /**
     * Path to the standard OpenCms module ZIPs.
     *
     * @parameter default-value="${basedir}/src/main/opencms-modules"
     * @required
     */
    private String opencmsmoduleSourceDirectory;

    /**
     * List of standard OpenCms module ZIPs to import.
     *
     * @parameter
     */
    private List opencmsModules;

    /**
     * Executes the OpenCms setup.
     *
     * Only if "opencmsModules" are configured; otherwise it's assumed
     * to be a non-VFS project in a multi project build.
     *
     * @throws MojoExecutionException in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
    	if (this.isSkipVfs()) {
    		this.getLog().info("Skipping VFS plugin");
    		return;
    	}

    	this.getLog().info("Executing SetupMojo");

        ClassLoader originalClassLoader = Thread.currentThread()
                                                .getContextClassLoader();
        // ClassLoader classloader = this.getClassLoader();
        // Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
            // Class invokeMeClass = classloader.loadClass(SetupMojo.SHELLCLASS);
            //Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
            // Object o = constr.newInstance(new Object[] {  });
//            Method main = invokeMeClass.getMethod(AbstractVfsMojo.SHELLMETHOD,
//                    SetupMojo.SHELLPARAMETERS);
            VfsSetup vfsSetup = new VfsSetup();
            vfsSetup.execute(getWebappDirectory(), this.opencmsmoduleSourceDirectory, this.opencmsModules, getAdminPassword());
//            main.invoke(o,
//                new Object[] {
//                    getWebappDirectory(), this.opencmsmoduleSourceDirectory,
//                    this.opencmsModules, getAdminPassword()
//                });
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                SetupMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                SetupMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + SetupMojo.SHELLCLASS, e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + SetupMojo.SHELLCLASS, e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + SetupMojo.SHELLCLASS, e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" + SetupMojo.SHELLCLASS, e);
        } catch (Exception e) {
        	throw new MojoExecutionException(
                    "Failed to instantiate (abstract!)" + SetupMojo.SHELLCLASS, e);
		} finally {
			this.getLog().info("Finished SetupMojo");
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
