package org.opencms.synchronize;

import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
import org.opencms.report.I_CmsReport;

public interface I_CmsSynchronize {

    public void synchronize(CmsObject cms, CmsSynchronizeSettings syncSettings, I_CmsReport report) throws CmsSynchronizeException, CmsException;
}