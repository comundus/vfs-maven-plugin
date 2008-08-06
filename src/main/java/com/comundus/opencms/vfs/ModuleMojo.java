package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * A Maven2 plugin Goal to install a module description in the targeted OpenCms.
 *
 * @goal module
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class ModuleMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsModule";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, String.class, String.class, String.class
        };
    
    /**
     * Path to source file with module XML configuration as in manifest.xml or opencms-modules.xml.
     *
     * @parameter expression="${basedir}/src/main/opencms-module/opencms-module.xml"
     * @required
     */
    private String moduleSourcePath;
    /**
    * @parameter
    */
    private String moduleVersion;
    
    /**
     * Installs a module description in the targeted OpenCms.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
        ClassLoader originalClassLoader = Thread.currentThread()
                                                .getContextClassLoader();
        ClassLoader classloader = this.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
            Class invokeMeClass = classloader.loadClass(ModuleMojo.SHELLCLASS);
            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
            Object o = constr.newInstance(new Object[] {  });
            Method main = invokeMeClass.getMethod("execute",
                    ModuleMojo.SHELLPARAMETERS);
            main.invoke(o,
                new Object[] {
                    getWebappDirectory(), getAdminPassword(),
                    this.moduleSourcePath, this.moduleVersion
                });
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                ModuleMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                ModuleMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + ModuleMojo.SHELLCLASS,
                e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + ModuleMojo.SHELLCLASS,
                e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + ModuleMojo.SHELLCLASS,
                e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" + ModuleMojo.SHELLCLASS, e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
