package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.VfsUserImport;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * A Maven2 plugin Goal to import user/group data into OpenCms from a source
 * folder.
 *
 * @goal importusers
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class UserImportMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsUserImport";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, String.class, String.class
        };

    /**
     * Source folder with OpenCms userdata.
     *
     * @parameter expression="${basedir}/src/main/opencms-usergroups"
     * @required
     */
    private String usergroupsSourceDirectory;

    /**
     * Imports user/groups data from the source folder into OpenCms.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
    	if (this.isSkipVfs()){
    		this.getLog().info("Skipping VFS plugin");
    	}
    	this.getLog().info("Executing UserImportMojo");
//        ClassLoader originalClassLoader = Thread.currentThread()
//                                                .getContextClassLoader();
//        ClassLoader classloader = this.getClassLoader();
//        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
//            Class invokeMeClass = classloader.loadClass(UserImportMojo.SHELLCLASS);
//            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
//            Object o = constr.newInstance(new Object[] {  });
//            Method main = invokeMeClass.getMethod("execute",
//                    UserImportMojo.SHELLPARAMETERS);
            VfsUserImport userImport = new VfsUserImport();
            userImport.execute(
                    getWebappDirectory(), getAdminPassword(),
                    this.usergroupsSourceDirectory
                );
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                UserImportMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                UserImportMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                UserImportMojo.SHELLCLASS, e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                UserImportMojo.SHELLCLASS, e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                UserImportMojo.SHELLCLASS, e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" +
                UserImportMojo.SHELLCLASS, e);
        } catch (Exception e) {
        	 throw new MojoExecutionException(
                     "Failed to instantiate (abstract!)" +
                     UserImportMojo.SHELLCLASS, e);
		} finally {
			this.getLog().info("Finished UserImportMojo");
           // Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
