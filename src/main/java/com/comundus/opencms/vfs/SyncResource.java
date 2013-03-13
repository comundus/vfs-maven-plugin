package com.comundus.opencms.vfs;

public class SyncResource {

	/**
	 * File or Folder path in VFS
	 */
	private String resource;
	
	/**
	 * May be '*', used to exclude every resource in a folder
	 */
	private String[] excludes=new String[0];

	/**
	 * 
	 * @param resourcePath
	 */
	public SyncResource(String resourcePath) {
		this.resource=resourcePath;
	}
	
	/**
	 * 
	 */
	public SyncResource() {
		
	}

	public SyncResource(String resourcePath, String[] excludes) {
		this.resource=resourcePath;
		this.excludes=excludes;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}
	public String getResource() {
		return resource;
	}
	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}
	public String[] getExcludes() {
		return excludes;
	}
	
	@Override
	public String toString() {
		
		return "[SyncResource]"+this.resource;
	}
}
