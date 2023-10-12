package com.comundus.opencms;

import org.apache.commons.logging.Log;
import org.opencms.loader.CmsTemplateContextManager;
import org.opencms.main.CmsLog;

/**
 * Class to override {@link CmsTemplateContextManager#updateContextMap()} 
 * because method creates exceptions for schema files 
 * when calling {@code OpenCms.getResourceManager().getAllowedContextMap(m_cms)}
 */
public class TemplateContextInterceptor {

    /** The logger instance for this class. */
    private static final Log LOG = CmsLog.getLog(TemplateContextInterceptor.class);
    
    private TemplateContextInterceptor() {
        
    }
    
    /**
     * Updates the cached context map.
     */
    public static void updateContextMap() {
        LOG.debug("Suppress updating cached 'allowed template contexts' map.");
    }
}
