package com.comundus.opencms.vfs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.VfsOrgunits;


/**
 * A Maven2 plugin Goal to create organizaional units from their previously created folders.
 *
 * @goal createorgunits
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class OrgunitsMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsOrgunits";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETER = new Class[] {
            String.class, String.class
        };

    /**
     * Calls creation of organizational units.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
    	if (this.isSkipVfs()){
    		this.getLog().info("Skipping VFS plugin");
    	}

//        ClassLoader originalClassLoader = Thread.currentThread()
//                                                .getContextClassLoader();
//        ClassLoader classloader = this.getClassLoader();
//        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
//            Class invokeMeClass = classloader.loadClass(OrgunitsMojo.SHELLCLASS);
//            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
//            Object o = constr.newInstance(new Object[] {  });
//            Method main = invokeMeClass.getMethod(AbstractVfsMojo.SHELLMETHOD,
//                    OrgunitsMojo.SHELLPARAMETER);
            VfsOrgunits orgUnits = new VfsOrgunits();
            orgUnits.execute(
                    getWebappDirectory(), getAdminPassword()
                );
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                OrgunitsMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                OrgunitsMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + OrgunitsMojo.SHELLCLASS, e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + OrgunitsMojo.SHELLCLASS, e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " + OrgunitsMojo.SHELLCLASS, e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" + OrgunitsMojo.SHELLCLASS, e);
        } catch (Exception e) {
        	 throw new MojoExecutionException(
                     "Failed to instantiate (abstract!)" + OrgunitsMojo.SHELLCLASS, e);
		} finally {
           // Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
