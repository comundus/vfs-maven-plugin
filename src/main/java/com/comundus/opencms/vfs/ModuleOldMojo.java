package com.comundus.opencms.vfs;

import com.comundus.opencms.vfs.conf.Exportpoint;
import com.comundus.opencms.vfs.conf.Module;
import com.comundus.opencms.vfs.conf.Param;
import com.comundus.opencms.vfs.conf.Resource;

import org.apache.maven.plugin.MojoExecutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A Maven2 plugin Goal to install a module description in the targeted OpenCms.
 * Deprecated since 6.9.2, use goal "VfsModule" instead.
 * @goal module-old
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class ModuleOldMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsModuleOld";

    /**
     * Signature of method called in _opencmsshell class.
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, String.class, String.class, String.class, String.class,
            String.class, String.class, String.class, String.class, String.class,
            String.class, String.class, List.class, List.class, List.class,
            List.class, Map.class
        };

    /**
     * Value object holding the "module" configuration parameters.
     *
     * @parameter
     *
     */
    private Module module;

    /**
     * Installs a module description in the targeted OpenCms.
     *
     * Only if a "module" configuration is present; otherwise it's assumed to be
     * a non-VFS project in a multi project build.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {
        if (this.module == null) {
            this.getLog().info("Skipping non-vfs-module project");

            return; // it's ok, we're not in a module
        }

        // as we're switching to a new ClassLoader we need to restrict our
        // parameter passing to simple Java classes
        // so we convert our configuration as such
        // resources is a List of Strings containing the URIs
        List resources = new ArrayList();
        List configresources = this.module.getResources();

        if (configresources != null) {
            Iterator it = configresources.iterator();

            while (it.hasNext()) {
                Resource r = (Resource) it.next();
                resources.add(r.getUri());
            }
        }

        // exportpointsuris and exportpointsdestinations are Lists containing
        // Strings
        List exportpointsuris = new ArrayList();
        List exportpointsdestinations = new ArrayList();
        List configexportpoints = this.module.getExportpoints();

        if (configexportpoints != null) {
            Iterator it = configexportpoints.iterator();

            while (it.hasNext()) {
                Exportpoint e = (Exportpoint) it.next();
                exportpointsuris.add(e.getUri());
                exportpointsdestinations.add(e.getDestination());
            }
        }

        // we get a List of Param and make a map from it
        Map mparameters = new HashMap();
        Iterator it = this.module.getParameters().iterator();

        while (it.hasNext()) {
            Param p = (Param) it.next();
            mparameters.put(p.getName(), p.getValue());
        }

        ClassLoader originalClassLoader = Thread.currentThread()
                                                .getContextClassLoader();
        ClassLoader classloader = this.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);

        // now we're running inside our own classloader
        // the target class MUST NOT have been loaded already,
        // so we have to invoke it via Reflection
        try {
            Class invokeMeClass = classloader.loadClass(ModuleOldMojo.SHELLCLASS);
            Constructor constr = invokeMeClass.getConstructor(AbstractVfsMojo.EMPTY);
            Object o = constr.newInstance(new Object[] {  });
            Method main = invokeMeClass.getMethod("execute",
                    ModuleOldMojo.SHELLPARAMETERS);
            main.invoke(o,
                new Object[] {
                    getWebappDirectory(), getAdminPassword(),
                    this.module.getName(), this.module.getNicename(),
                    this.module.getClazz(), this.module.getDescription(),
                    this.module.getVersion(), this.module.getAuthorname(),
                    this.module.getAuthoremail(), this.module.getDatecreated(),
                    this.module.getUserinstalled(),
                    this.module.getDateinstalled(),
                    this.module.getDependencies(), exportpointsuris,
                    exportpointsdestinations, resources, mparameters
                });
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                ModuleOldMojo.SHELLCLASS, e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Failed to load " +
                ModuleOldMojo.SHELLCLASS, e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Failed to find " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                ModuleOldMojo.SHELLCLASS, e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Failure while executing " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                ModuleOldMojo.SHELLCLASS, e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Failed to access " +
                AbstractVfsMojo.SHELLMETHOD + "() in " +
                ModuleOldMojo.SHELLCLASS, e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException(
                "Failed to instantiate (abstract!)" + ModuleOldMojo.SHELLCLASS,
                e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
