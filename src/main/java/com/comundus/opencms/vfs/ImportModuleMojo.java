package com.comundus.opencms.vfs;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.opencms.main.CmsException;
import org.xml.sax.SAXException;

import com.comundus.opencms.VfsImportModule;

/**
 * A Maven2 plugin Goal to import a module from module ZIP - file.<br/>
 *
 * Configuration sample:
 * 
 * <configuration>
 *     <moduleFileName>${project.basedir}/src/main/modules/single_module.zip</moduleFileName>
 *     <moduleDirectoryName>${project.basedir}/src/main/modules/</moduleDirectoryName>
 * </configuration>
 *
 * @goal import-module
 */
public class ImportModuleMojo extends AbstractVfsMojo {
    /**
     * The _opencmsshell class to instantiate within our custom ClassLoader.
     */
    private static final String SHELLCLASS = "com.comundus.opencms.VfsImportModule";

    private static final String ERROR_MESSAGE = "Failed to instantiate (abstract!)" + ImportModuleMojo.SHELLCLASS;

    /**
     * Single module absolute file name
     * 
     * @parameter expression="${moduleFileName}"
     */
    private String moduleFileName;

    /**
     * Module directory absolute name (to import several modules)
     * 
     * @parameter expression="${moduleDirectoryName}"
     */
    private String moduleDirectoryName;

    /**
     * Extracts a module from the targeted OpenCms.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {

        try {
            final VfsImportModule module = new VfsImportModule();
            module.execute(getWebappDirectory(), getAdminPassword(), this.moduleFileName, this.moduleDirectoryName);
        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException("Failed to load " + ImportModuleMojo.SHELLCLASS, e);
        } catch (IOException e) {
            throw new MojoExecutionException(ERROR_MESSAGE, e);
        } catch (CmsException e) {
            throw new MojoExecutionException(ERROR_MESSAGE, e);
        } catch (SAXException e) {
            throw new MojoExecutionException(ERROR_MESSAGE, e);
        }
    }
}
