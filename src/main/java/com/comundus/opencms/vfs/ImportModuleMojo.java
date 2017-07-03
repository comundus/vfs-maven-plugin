package com.comundus.opencms.vfs;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.opencms.main.CmsException;
import org.xml.sax.SAXException;

import com.comundus.opencms.VfsImportModule;

/**
 * A Maven2 plugin Goal to import a modules from module ZIP - files.<br/>
 *
 * Configuration sample:
 * 
 * <configuration>
 *     <moduleFileNames>
 *         <moduleFileName>${project.basedir}/src/main/modules/single_module_1.zip</moduleFileName>
 *         <moduleFileName>${project.basedir}/src/main/modules/single_module_2.zip</moduleFileName>
 *     </moduleFileNames>
 *     <moduleDirectoryNames>
 *         <moduleDirectoryName>${project.basedir}/src/main/modules1/</moduleDirectoryName>
 *         <moduleDirectoryName>${project.basedir}/src/main/modules2/</moduleDirectoryName>         
 *     </moduleDirectoryNames>
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
     * Module absolute file name list
     * 
     * @parameter
     */
    private List<String> moduleFileNames;

    /**
     * Module directory absolute name list (to import several modules)
     * 
     * @parameter
     */
    private List<String> moduleDirectoryNames;

    /**
     * Extracts a module from the targeted OpenCms.
     *
     * @throws MojoExecutionException
     *             in case anything goes wrong
     */
    public final void execute() throws MojoExecutionException {

        try {
            final VfsImportModule module = new VfsImportModule();
            module.execute(getWebappDirectory(), getAdminPassword(), this.moduleFileNames, this.moduleDirectoryNames);
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
