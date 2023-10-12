package com.comundus.opencms;

import org.apache.commons.logging.Log;
import org.opencms.main.CmsLog;

/**
 * Class to override {@link CmsFormatterConfigurationCache#performUpdate()} 
 * because method creates exceptions while running vfs-maven-plugin
 */
public class FormatterConfigurationCacheInterceptor {

    /** The logger instance for this class. */
    private static final Log LOG = CmsLog.getLog(FormatterConfigurationCacheInterceptor.class);
    
    private FormatterConfigurationCacheInterceptor() {
        
    }
    
    /**
     * The method called by the scheduled update action to update the cache.<p>
     */
    public static void performUpdate() {
        LOG.debug("Suppress updating cache.");
    }
}
