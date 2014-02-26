//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;
import org.opencms.main.CmsException;
import org.xml.sax.SAXException;

import com.comundus.opencms.VfsExportModule;

import java.io.IOException;

/**
 * A Maven2 plugin Goal to export a module description and files from the targeted OpenCms.
 * in the OpenCms style
 *
 * @goal export-module
 */
public class ExportModuleMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsExportModule";

    /**
     * Signature of method called in _opencmsshell class.
     * String webappDirectory
     * String adminPassword
     * String moduleSourcePath
     * String moduleVersion
     */
    private static final Class[] SHELLPARAMETERS = new Class[] {
            String.class, String.class, String.class, String.class
        };

    /**
     * Path to target directory to place the export.
     *
     * @parameter property="targetPath" default-value="${basedir}/target/opencms-module/"
     * @required
     */
    private String targetPath;

    /**
     * Name of the module to export.
     * @parameter property="cmsModuleName"
     * @required
     */
    private String moduleName;

    /**
     * Extracts a module from the targeted OpenCms.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {

    	try {
        	VfsExportModule module = new VfsExportModule();
            module.execute(
                    getWebappDirectory(), getAdminPassword(),
                    this.moduleName, this.targetPath
                );
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " +
                ExportModuleMojo.SHELLCLASS, e);
        } catch (IOException e) {
        	throw new MojoExecutionException(
                    "Failed to instantiate (abstract!)" + ExportModuleMojo.SHELLCLASS, e);
		} catch (CmsException e) {
			throw new MojoExecutionException(
	                "Failed to instantiate (abstract!)" + ExportModuleMojo.SHELLCLASS, e);
		} catch (SAXException e) {
			throw new MojoExecutionException(
	                "Failed to instantiate (abstract!)" + ExportModuleMojo.SHELLCLASS, e);
		} finally {
            // Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
