//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
package com.comundus.opencms.vfs;

/**
 *
 * Value class representing a VFS resource which may exclude child resources if it is a folder.
 */
public class SyncResource {

    /**
     * File or Folder path in VFS.
     */
    private String resource;

    /**
     * May be '*', used to exclude every resource in a folder.
     */
    private String[] excludes = new String[0];

    /**
     *
     * @param resourcePath Path of the VFS resource
     */
    public SyncResource(String resourcePath) {
	this.resource = resourcePath;
    }

    /**
     *
     */
    public SyncResource() {

    }

    /**
     *
     * @param resourcePath Path of the resource
     * @param excludes List of excluded paths
     */
    public SyncResource(String resourcePath, String[] excludes) {
	this.resource = resourcePath;
	this.excludes = excludes;
    }

    /**
     *
     * @param resource
     */
    public void setResource(String resource) {
	this.resource = resource;
    }

    /**
     *
     * @return
     */
    public String getResource() {
	return resource;
    }

    /**
     *
     * @param excludes
     */
    public void setExcludes(String[] excludes) {
	this.excludes = excludes;
    }

    /**
     *
     * @return
     */
    public String[] getExcludes() {
	return excludes;
    }

    @Override
    public String toString() {
	return "[SyncResource]" + this.resource;
    }
}
