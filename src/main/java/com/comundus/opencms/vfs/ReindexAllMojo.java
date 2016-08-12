package com.comundus.opencms.vfs;

import org.apache.maven.plugin.MojoExecutionException;

import com.comundus.opencms.ReindexAll;

/**
 * A Maven2 plugin Goal to reindex all indexes.
 * 
 * @goal reindex-all
 */
public class ReindexAllMojo extends AbstractVfsMojo {
	/**
	 * The _opencmsshell class to instantiate within our custom ClassLoader.
	 */
	private static final String SHELLCLASS = "com.comundus.opencms.ReindexAll";

	/**
	 * Performs all index reindexing
	 * 
	 * @throws MojoExecutionException
	 *             in case anything goes wrong
	 */
	public final void execute() throws MojoExecutionException {
		try {
			ReindexAll sync = new ReindexAll();
			sync.execute(getWebappDirectory(), getAdminPassword());
		} catch (Exception e) {
			throw new MojoExecutionException("Undetermined error executing "
					+ ReindexAllMojo.SHELLCLASS, e);
		} finally {
		}
	}
}
