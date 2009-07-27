package com.comundus.opencms;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.dom4j.io.SAXWriter;

import org.opencms.file.CmsObject;

import org.opencms.importexport.CmsImportExportManager;

import org.opencms.main.OpenCms;

import org.opencms.report.I_CmsReport;

import org.opencms.util.CmsXmlSaxWriter;

import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


/**
 * Base class containing some common stuff for VFS and user/groups
 * synchronizing.
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public class XmlHandling {
    /** The CmsObject. */
    private CmsObject cms;

    /** The report to write the output to. */
    private I_CmsReport report;

    /** The xml manifest-file. */
    private Document docXml;

    /** The SAX writer to write the output to. */
    private SAXWriter saxWriter;

    /**
     * Filename for user/groups import/export XML file.
     */
    private String exportFileName;

    /**
     * Directory for user/groups import/export.
     */
    private String usergroupsSourceDirectory;

    /**
     * Opens the metadata file and initializes the internal XML document for the
     * metadata.
     * <p>
     * from org.opencms.importexport.CmsExport
     *
     * @param metadataFile
     *            file to write metadata to
     * @return the node in the XML document where all files are appended to
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     * @throws IOException
     *             if something goes wrong while closing the export file
     */

    // code taken from org.opencms.importexport.CmsExport
    protected final Element openExportFile(final File metadataFile)
        throws IOException, SAXException {
        CmsXmlSaxWriter saxHandler;

        if (metadataFile.isDirectory()) {
            metadataFile.mkdirs();
        } else {
            final File parentFolder = new File(metadataFile.getPath()
                                                           .replace('/',
                        File.separatorChar)
                                                           .substring(0,
                        metadataFile.getPath().lastIndexOf(File.separator)));
            parentFolder.mkdirs();
        }

        final String encoding = OpenCms.getSystemInfo().getDefaultEncoding();
        // saxHandler = new CmsXmlSaxWriter(new BufferedWriter(new
        // FileWriter(metadataFile)),
        // OpenCms.getSystemInfo().getDefaultEncoding());
        // in contrast to original OpenCms using a StringWriter we need to
        // explicitely set the file encoding here - it's expected to be
        // different from the systems default file encoding
        saxHandler = new CmsXmlSaxWriter(new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(metadataFile)),
                    encoding), encoding);
        // new with OpenCms 6.2.3, but not supported by older CmsXmlSaxWriter class:
        //saxHandler.setEscapeXml(true);
        //saxHandler.setEscapeUnknownChars(true);

        // initialize the dom4j writer object as member variable
        this.setSaxWriter(new SAXWriter(saxHandler, saxHandler));

        // the XML document to write the XMl to
        final Document doc = DocumentHelper.createDocument();
        // start the document
        saxHandler.startDocument();

        // the node in the XML document where the file entries are appended to
        final String exportNodeName = this.getExportNodeName();

        // add main export node to XML document
        final Element exportNode = doc.addElement(exportNodeName);
        this.getSaxWriter().writeOpen(exportNode);

        return exportNode;
    }

    /**
     * Closes the export file and saves the metadata XML document.
     * <p>
     *
     * @param exportNode
     *            the export root node
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     * @throws IOException
     *             if something goes wrong while closing the export file
     */

    // code taken from org.opencms.importexport.CmsExport
    protected final void closeExportFile(final Element exportNode)
        throws IOException, SAXException {
        // close the <export> Tag
        this.getSaxWriter().writeClose(exportNode);

        // close the XML document
        final CmsXmlSaxWriter xmlSaxWriter = (CmsXmlSaxWriter) this.getSaxWriter()
                                                                   .getContentHandler();
        xmlSaxWriter.endDocument();
        xmlSaxWriter.getWriter().close();
    }

    /**
     * Sets the SAX based xml writer to write the XML output to.
     * <p>
     *
     * @param sW
     *            the SAX based xml writer to write the XML output to
     */
    private void setSaxWriter(final SAXWriter sW) {
        this.saxWriter = sW;
    }

    /**
     * Returns the SAX based xml writer to write the XML output to.
     * <p>
     *
     * @return the SAX based xml writer to write the XML output to
     */
    protected final SAXWriter getSaxWriter() {
        return this.saxWriter;
    }

    /**
     * Returns the name of the main export node.
     * <p>
     *
     * @return the name of the main export node
     */
    private String getExportNodeName() {
        return CmsImportExportManager.N_EXPORT;
    }

    /**
     * Writes the output element to the XML output writer and detaches it from
     * it's parent element.
     * <p>
     *
     * @param parent
     *            the parent element
     * @param output
     *            the output element
     * @throws SAXException
     *             if something goes wrong procesing the manifest.xml
     */

    // code taken from org.opencms.importexport.CmsExport
    protected final void digestElement(final Element parent,
        final Element output) throws SAXException {
        this.saxWriter.write(output);
        parent.remove(output);
    }

    /**
     * Returns the name of the export file.
     * <p>
     *
     * @return the name of the export file
     */
    protected final String getExportFileName() {
        return this.exportFileName;
    }

    /**
     * Set the CmsObject to use.
     *
     * Used when OpenCms integrated synchronisation is called.
     *
     * @param pcms
     *            the CmsObject to use
     */
    public final void setCms(final CmsObject pcms) {
        this.cms = pcms;
    }

    /**
     * Gets the CmsObject.
     *
     * @return the CmsObject
     */
    protected final CmsObject getCms() {
        return this.cms;
    }

    /**
     * gets the dom4j Document.
     * @return the dom4j Document
     */
    public final Document getDocXml() {
        return this.docXml;
    }

    /**
     * sets the dom4j document.
     * @param pdocXml the dom4j Document
     */
    public final void setDocXml(final Document pdocXml) {
        this.docXml = pdocXml;
    }

    /**
     * sets the xml export file name.
     * @param pexportFileName the export file name
     */
    public final void setExportFileName(final String pexportFileName) {
        this.exportFileName = pexportFileName;
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

    /**
     * gets the directory for user and group data.
     * @return the user and groups data directory
     */
    public final String getUsergroupsSourceDirectory() {
        return this.usergroupsSourceDirectory;
    }

    /**
     * set the directory for user and group data.
     * @param pusergroupsSourceDirectory the directory for user and group data
     */
    public final void setUsergroupsSourceDirectory(
        final String pusergroupsSourceDirectory) {
        this.usergroupsSourceDirectory = pusergroupsSourceDirectory;
    }
    
    /**
     * Copied from CmsImport in OpenCms version 7.0.3
     * Returns the value of a child element with a specified name for a given parent element.<p>
     *
     * @param parentElement the parent element
     * @param elementName the child element name
     * @return the value of the child node, or null if something went wrong
     */
    public static String getChildElementTextValue(Element parentElement, String elementName) {

        try {
            // get the first child element matching the specified name
            Element childElement = (Element)parentElement.selectNodes("./" + elementName).get(0);
            // return the value of the child element
            return childElement.getTextTrim();
        } catch (Exception e) {
            return null;
        }
    }

}
