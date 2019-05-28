package com.comundus.opencms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester3.Digester;
import org.opencms.configuration.CmsConfigurationException;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.CmOpenCmsShell;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.module.CmsModuleXmlHandler;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
import org.opencms.security.CmsSecurityException;
import org.opencms.xml.CmsXmlErrorHandler;
import org.xml.sax.SAXException;


/**
 * Adds a module to WEB-INF/config/opencms-modules.xml.
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class VfsModule {
    /** The CmsObject. */
    private CmsObject cms;

    private String moduleVersion;

	private I_CmsReport report;
    
    /**
     * Installs a module description in the target OpenCms. Module description
     * comes from the path given in the parameter configurationXml. The XML file
     * there contains module description(s) in the format as in WEB-INF/config/opencms-modules.xml
     * or manifest.xml of an exported module. It may contain multiple modules,
     * but it must be well formed, say, in case of multiple modules it must have one root node
     * containing the module entries. These module configuration(s) get added to
     * WEB-INF/config/opencms-modules.xml of the target installation.
     * Module content gets sync'ed and published afterwards by the respective plugin goals.
     *
     * @param webappDirectory
     *            path to WEB-INF of the OpenCms installation
     * @param adminPassword
     *            password of user "Admin" performing the operation
     * @param moduleSourcePath
     *            path to module xml configuration file
     * @throws CmsException
     *             if anything OpenCms goes wrong
     * @throws IOException
     *             in case configuration files cannot be read
     * @throws SAXException
     *             in case configuration files cannot be parsed
     */
    public final void execute(final String webappDirectory,
        final String adminPassword, final String moduleSourcePath, final String moduleVersion)
        throws IOException, CmsException, SAXException {
        final String webinfdir = webappDirectory + File.separatorChar +
            "WEB-INF";
        final CmOpenCmsShell cmsshell = CmOpenCmsShell.getInstance(webinfdir,
                "Admin", adminPassword);

        this.moduleVersion = moduleVersion;
        
        if (cmsshell != null) {
            this.cms = cmsshell.getCmsObject();

            final CmsRequestContext requestcontext = this.cms.getRequestContext();
            requestcontext.setCurrentProject(this.cms.readProject("Offline"));
            
            this.setReport(new CmsShellReport(requestcontext.getLocale()));
            
            // code taken from org.opencms.module.CmsModuleImportExportHandler (readModuleFromImport)
            final Digester digester = new Digester();
            digester.setUseContextClassLoader(true);
            digester.setValidating(false);
            digester.setRuleNamespaceURI(null);
            digester.setErrorHandler(new CmsXmlErrorHandler());
            digester.push(this);
            CmsModuleXmlHandler.addXmlDigesterRules(digester);
            
            simpleReport("Parsing module file: "+moduleSourcePath+". Version: "+moduleVersion);            
            digester.parse(new FileInputStream(new File(moduleSourcePath)));
            simpleReport("End of parsing module file: "+moduleSourcePath+". Version: "+moduleVersion);
        }else{
        	System.err.println("[WARN]VfsModule.execute(): CmsShell not available");
        }
    }

    /**
     * Will be called by the digester if a module was imported.<p>
     *
     * @param moduleHandler contains the imported module
     * @throws CmsConfigurationException if an Exceptions occurs during the XML configuration process.
     * @throws CmsSecurityException if an security issue arises
     */
    public final void setModule(final CmsModuleXmlHandler moduleHandler)
        throws CmsConfigurationException, CmsSecurityException {
        // code taken from org.opencms.module.CmsModuleImportExportHandler (importModule)
        final CmsModule importedModule = moduleHandler.getModule();
        
        simpleReport("Module found: "+importedModule.getName());
        
        // check if the module is already installed
        if (OpenCms.getModuleManager().hasModule(importedModule.getName())) {
            throw new CmsConfigurationException(org.opencms.module.Messages.get()
                                                                           .container(org.opencms.module.Messages.ERR_MOD_ALREADY_INSTALLED_1,
                    importedModule.getName()));
        }
        simpleReport("Module is not installed, going on: "+importedModule.getName());
        /*/ check the module dependencies - DET: we don't do that during Maven style system setup
        List dependencies = OpenCms.getModuleManager().checkDependencies(
            importedModule,
            CmsModuleManager.DEPENDENCY_MODE_IMPORT);
        if (dependencies.size() > 0) {
            // some dependencies not fulfilled
            StringBuffer missingModules = new StringBuffer();
            Iterator it = dependencies.iterator();
            while (it.hasNext()) {
                CmsModuleDependency dependency = (CmsModuleDependency)it.next();
                missingModules.append("  ").append(dependency.getName()).append(", Version ").append(
                    dependency.getVersion()).append("\r\n");
            }
            throw new CmsConfigurationException(org.opencms.module.Messages.get().container(
                            org.opencms.module.Messages.ERR_MOD_DEPENDENCY_INFO_2,
                importedModule.getName() + ", Version " + importedModule.getVersion(),
                missingModules));
        }
        */

        // check the imported resource types for name / id conflicts
        final List checkedTypes = new ArrayList();
        final Iterator i = importedModule.getResourceTypes().iterator();

        while (i.hasNext()) {
            final I_CmsResourceType type = (I_CmsResourceType) i.next();

            // first check against the already configured resource types
            final int externalConflictIndex = OpenCms.getResourceManager()
                                               .getResourceTypes().indexOf(type);

            if (externalConflictIndex >= 0) {
                final I_CmsResourceType conflictingType = (I_CmsResourceType) OpenCms.getResourceManager()
                                                                               .getResourceTypes()
                                                                               .get(externalConflictIndex);

                if (!type.isIdentical(conflictingType)) {
                    // if name and id are identical, we assume this is a module replace operation
                    throw new CmsConfigurationException(org.opencms.loader.Messages.get()
                                                                                   .container(org.opencms.loader.Messages.ERR_CONFLICTING_MODULE_RESOURCE_TYPES_5,
                            new Object[] {
                                type.getTypeName(),
                                Integer.valueOf(type.getTypeId()),
                                importedModule.getName(),
                                conflictingType.getTypeName(),
                                Integer.valueOf(conflictingType.getTypeId())
                            }));
                }
            }

            // now check against the other resource types of the imported module
            final int internalConflictIndex = checkedTypes.indexOf(type);

            if (internalConflictIndex >= 0) {
                final I_CmsResourceType conflictingType = (I_CmsResourceType) checkedTypes.get(internalConflictIndex);
                throw new CmsConfigurationException(org.opencms.loader.Messages.get()
                                                                               .container(org.opencms.loader.Messages.ERR_CONFLICTING_RESTYPES_IN_MODULE_5,
                        new Object[] {
                            importedModule.getName(), type.getTypeName(),
                            Integer.valueOf(type.getTypeId()),
                            conflictingType.getTypeName(),
                            Integer.valueOf(conflictingType.getTypeId())
                        }));
            }

            // add the resource type for the next check
            checkedTypes.add(type);
        }

        if(this.moduleVersion != null) {
            importedModule.getVersion().setVersion(this.moduleVersion);
        }
        simpleReport("Adding Module: "+importedModule.getName());
        OpenCms.getModuleManager().addModule(this.cms, importedModule);
        simpleReport("Module added: "+importedModule.getName());
        // reinitialize the resource manager with additional module
        // resourcetypes if necessary
        if (importedModule.getResourceTypes() != Collections.EMPTY_LIST) {
            OpenCms.getResourceManager().initialize(this.cms);
        }

        // reinitialize the workplace manager with addititonal module
        // explorertypes if necessary
        if (importedModule.getExplorerTypes() != Collections.EMPTY_LIST) {
            OpenCms.getWorkplaceManager().addExplorerTypeSettings(importedModule);
        }
        
        
        //Recheck if the module was succesfully imported
        if (!OpenCms.getModuleManager().hasModule(importedModule.getName())) {
        	report("[WARN] Module " +importedModule.getName()+" has not been imported",I_CmsReport.FORMAT_WARNING);
        }
    }
    
    protected void simpleReport(String msg) {

    	report("[DEBUG] "+msg,I_CmsReport.FORMAT_DEFAULT);
	}
    
    /**
     * 
     * @param msg
     * @param format
     */
    protected void report(String msg, int format) {
		this.getReport()
			.println(org.opencms.report.Messages.get()
					.container(org.opencms.report.Messages.RPT_ARGUMENT_1,
							msg,format));		
	}

    /**
     * gets the Cms Report.
     * @return the Cms Report
     */
    public final I_CmsReport getReport() {
        return this.report;
    }

    /**
     * Set the report to use for synchronisation.
     *
     * Used when OpenCms integrated synchronisation is called.
     *
     * @param preport
     *            the report to use
     */
    public final void setReport(final I_CmsReport preport) {
        this.report = preport;
    }

}
