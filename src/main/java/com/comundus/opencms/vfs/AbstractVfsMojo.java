package com.comundus.opencms.vfs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Abstract base class for VFS mojos.
 */

//(C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
public abstract class AbstractVfsMojo extends AbstractMojo {
    /**
     * The custom classloader. With WEB-INF/lib jars plus Servlet and JSP.
     * _opencmsshell functions get executed within this classloader.
     */
    private static URLClassLoader classLoader = null;

    /**
     * The method to call within the instantiated _opencmsshell class.
     */
    protected static final String SHELLMETHOD = "execute";

    /**
     * Signature of default constructor.
     */
    protected static final Class[] EMPTY = new Class[] {  };

    /**
     * The password for login "Admin". In SetupMojo this is the password that
     * will be set for login "Admin" at the end of the setup process.
     *
     * @parameter
     */
    private String adminPassword;

    /**
     * The directory where the webapp is built.
     *
     * @parameter expression="${basedir}/../webapp/target/webapp"
     * @required
     */
    private String webappDirectory;

    /**
     * Servlet API version to add to classpath.
     *
     * @parameter expression="2.4"
     * @required
     */
    private String servletVersion;

    /**
     * JSP API version to add to classpath.
     *
     * @parameter expression="2.0"
     * @required
     */
    private String jspVersion;

    /**
     * Artifact factory, needed to download source jars for inclusion in
     * classpath.
     *
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * Artifact resolver, needed to download source jars for inclusion in
     * classpath.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * Local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Remote repositories which will be searched for Servlet and JSP API.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteArtifactRepositories;

    /**
     *
     * Singleton-like optimization to only get the ClassLoader once, prevents
     * OutOfMemoryExceptions from PermGenSpace running out, from the
     * ClassLoaders containing OpenCms Singletons.
     *
     * This way we use exactly one and the same ClassLoader in all Mojos in one
     * Maven run.
     *
     * @return ClassLoader readily prepared with the classpath to instantiate an
     *         OpenCms
     */
    protected final ClassLoader getClassLoader() {
        if (AbstractVfsMojo.classLoader == null) {
            // first we need to set up a class loader for the OpenCms instance
            String webinfclassesdir = this.webappDirectory +
                File.separatorChar + "WEB-INF" + File.separatorChar +
                "classes" + File.separatorChar;
            String webinflibdir = this.webappDirectory + File.separatorChar +
                "WEB-INF" + File.separatorChar + "lib";
            
            File fWebinfLibDir= new File(webinflibdir);
            if(!fWebinfLibDir.isDirectory()){
        	throw new RuntimeException("WEB-INF/lib could not be found or it is not a directory");
            }
            
            File[] res=fWebinfLibDir.listFiles();
            Arrays.sort(res); // in order to get our "underscore"-jars first in the search path

            ArrayList list = new ArrayList();

            try {
                list.add(new File(webinfclassesdir).toURI().toURL());
                list.add(this.resolveArtifact("javax.servlet", "servlet-api",
                        this.servletVersion).toURI().toURL());
                list.add(this.resolveArtifact("javax.servlet", "jsp-api",
                        this.jspVersion).toURI().toURL());

                for (int i = 0; i < res.length; i++) {
                    if (res[i].exists() && res[i].canRead()) {
                        list.add(res[i].toURI().toURL());
                    }
                }
            } catch (MalformedURLException e) {
                this.getLog().error(e);
            }

            URL[] array = (URL[]) list.toArray(new URL[list.size()]);

            AbstractVfsMojo.classLoader = (URLClassLoader) AccessController.doPrivileged(new ClassLoaderFactory(
                        array));
        }

        return AbstractVfsMojo.classLoader;
    }

    /**
     * Resolves the named artifact from local or remote Maven 2 repositories.
     *
     * @param groupId
     *            groupId of artifact to resolve (i.e. "javax.servlet")
     * @param artifactId
     *            artifactId of artifact to resolve (i.e. "servlet-api")
     * @param version
     *            version of artifact to resolve (i.e. "2.4")
     * @return File pointing to the resolved artifact (i.e. servlet-api-2.4.jar)
     */
    private File resolveArtifact(final String groupId, final String artifactId,
        final String version) {
        Artifact resolvedArtifact = this.artifactFactory.createArtifact(groupId,
                artifactId, version, "runtime", "jar");

        try {
            this.artifactResolver.resolve(resolvedArtifact,
                this.remoteArtifactRepositories, this.localRepository);
        } catch (ArtifactNotFoundException e) {
            // ignore, the jar has not been found
        } catch (ArtifactResolutionException e) {
            // ignore, the jar has not been found
        }

        return resolvedArtifact.getFile();
    }

    /**
     * gets the path to the web application directory.
     * @return the path to the web application directory
     */
    public final String getWebappDirectory() {
        return this.webappDirectory;
    }

    /**
     * gets the new password for admin user.
     * @return the new password for admin user
     */
    public final String getAdminPassword() {
        return this.adminPassword;
    }

    /**
    * @see org.apache.maven.plugin.Mojo
    * @throws MojoExecutionException An exception occuring during the execution of a plugin
    * @throws MojoFailureException  An exception occuring during the execution of a plugin
    */
    public abstract void execute()
        throws MojoExecutionException, MojoFailureException;

    /**
     * PrivilegedAction to create a new ClassLoader.
     *
     * (C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
     *
     */
    private static class ClassLoaderFactory implements PrivilegedAction {
        /**
         * URLs to be included in the new URLClassLoader.
         */
        private final URL[] array;

        /**
         * Construct the URLClassLoader with the given URLs.
         * @param a the URL array
         */
        ClassLoaderFactory(final URL[] a) {
            this.array = a;
        }

        /**
         * Creates the URLClassLoader with the given URL array.
         * @return the URLClassLoader
         */
        public Object run() {
            return new URLClassLoader(this.array, null);
        }
    }
}
