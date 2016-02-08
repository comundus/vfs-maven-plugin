# Updating vfs-maven-plugin and opencms-basic to a new OpenCms version

This document describes how to upgrade the projects vfs-maven-plugin and opencms-basic to be used with a new OpenCms 
version. For the examples, the original  version **9.0.1** will be updated to the target version **9.5.1**. The database 
used through the process is MySQL.

Although it is not essential, the use of the Eclipse IDE is assumed throughout this guide. The project settings files
contained in the VCS require the use of the plugin m2e for the Eclipse IDE which eases the development with Maven-based
projects.

It is assumed that the reader knows how to use VFS-Plugin. Refer to the plugin documentation for an explanation of its use:

. <https://opencms-maven.comundus.com/documentation/setting-up-opencms/>  
. <https://opencms-maven.comundus.com/documentation/using-opencms-maven/>

## Check out projects

As a first step, following repositories must be obtained from <http://github.com/comundus>:

 * `vfs-maven-plugin`
 * `opencms-basic`

`vfs-maven-plugin` contains only the one project for the Maven Plugin.

`opencms-basic` contains a multi-module Maven project where the root and parent project resides in the `parent`
folder.

After importing the projects contained in the two repositories in Eclipse, our workspace should look like following 
image:

![Projects in Package explorer][projects] 

[projects]: projects.png "Projects in package explorer"

## Update mvn-vfs-plugin

For each OpenCms version, a new version of the VFS-Plugin will be created, because at least the dependency version to 
OpenCms changes. It is possible that changes in OpenCms make necessary to change the source code of the plugin.

These are the steps to follow:

1) Modify `vfs-maven-plugin/pom.xml` to set the version of the plugin and the dependency to OpenCms.

  * VFS-Plugin in `<version>` 
  * OpenCms dependency in the property `opencms.version`
  
This excerpt of the POM shows where to find these values:

    <?xml version="1.0" encoding="ISO-8859-1"?> 
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
      <parent>
        <groupId>com.comundus</groupId>
        <artifactId>comundus</artifactId>
        <version>1.4</version>
      </parent>
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.comundus.maven</groupId>
      <artifactId>vfs-maven-plugin</artifactId>
      <packaging>maven-plugin</packaging>
      <version>9.5.1</version>
      <name>VFS Maven Plugin</name>
      
    ...
      
      <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <opencms.version>9.5.1</opencms.version>
      </properties>
      
    ...
      
    </project> 

2) After modifying the POM, a build must be attempted. For this call `mvn install` in the folder `vfs-maven-plugin`.
If the command is successful, the new version of the plugin is ready to be used by subsequent builds of opencms-basic in
our local computer. We can proceed to the next step, although we still don't know if the VFS-Plugin works correctly.

In case the build of the VFS-Plugin fails, it will probably be because of a compile error caused by a change in the 
OpenCms API. All the errors must be corrected in order for the build to work. 

3) Adapt to modifications made in the new OpenCms version.

The classes in `src/main/java/com.comundus.opencms` contain code originally based on OpenCms 7.0, and contain 
comments detailing which OpenCms classes and methods they are based on. These are some classes that can be checked to 
see if there are relevant changes: 

    org.opencms.main.CmsShellCommands
    org.opencms.synchronize.CmsSynchronize
    org.opencms.importexport.CmsExport
    org.opencms.module.CmsModuleImportExportHandler
    org.opencms.importexport.CmsImportVersion6
    
The method `executeSetupTxt()` in `com.comundus.opencms.VfsSetup` has to mimic the commands called in
the OpenCms-Shell script in:
    
    WEB-INF/setupdata/cmssetup.txt

## Build OpenCms-Basic

In order to compare it to the target version, the original version has to be built. These are the steps

* Configure the database connection in the file `opencms-basic/parent/pom.xml` under the `<properties>` 
  section.

* Create a database schema for the installation. The default is `ocbasic_mvn`. 

* Run the following Maven command from `opencms-basic/parent`:

        > mvn clean package

This will generate a basic OpenCms 9.0.1 webapp in `opencms-basic/webapp/target/webapp`, and an OpenCms 
database will be initialized in the schema `ocbasic_mvn`.

## Install the new OpenCms version

The next step is to make a normal installation of OpenCms using the binary distribution from **opencms.org**. If using
the default configuration of Apache Tomcat and OpenCms, the setup process can be started on <http://localhost:8080/opencms/setup/>.
During the step "Module selection", only the OpenCms Workplace should be left selected. See the image: 

![Module selection][module-selection] 

If using the defaults, this will generate an OpenCms 9.5.1 inside Tomcat (`tomcat/webapps/opencms`) and the base data in
the database schema named `opencms`.

[module-selection]: oc-wizard-module-selection.png "Module selection"

## Changes in configuration files

In the following two locations, the XML configuration files in the first will be copied to the latter, with the exception
of `opencms-modules.xml`. The file `opencms.properties` must also be compared and the changes taken, but 
not deleting the placeholders for configurations defined in pom.xml. e.g. `${opencms.db.jdbcDriver}`. 

* `tomcat/webapps/opencms/WEB-INF/config`
* [project webapp]`/src/main/webapp/WEB-INF/config/`

The changes in the configuration file `opencms-modules.xml` will be transferred to the system subproject. It is 
important to compare the files first, in order to know which modules were deleted or added in the target version.

* `tomcat/webapps/opencms/WEB-INF/config/opencms-modules.xml`
* [project system]`/src/main/opencms-module/opencms-module.xml`
 
Please note, that `opencms-module.xml` does not contain an XML Prolog, while `opencms-modules.xml` does.

Knowing which modules were added or deleted, we can proceed to update the file `pom.xml` in the subproject `system`.
This is done adding the paths under `<resources>` of the module in the configuration parameter `syncVFSPaths` 
of the `vfs-maven-plugin`. 

Only paths under `/system/modules` have to be added. e.g.:

    <module>
        <name>org.opencms.ade.config</name>
        <nicename><![CDATA[OpenCms 9 ADE Configuration]]></nicename>
        <group>OpenCms ADE</group>
        <class/>
        ...
        <dependencies />
        <exportpoints />
        <resources>
            <resource uri="/system/modules/org.opencms.ade.config/"/>
        </resources>
        <parameters/>
        ...
    </module>

The following XML-code helps to locate where to add the new paths in`/pom.xml` of the subproject `system`:

    <project>
      <build>
        <plugins>
          <plugin>
            <groupId>com.comundus.maven</groupId>
            <artifactId>vfs-maven-plugin</artifactId>
            <configuration>
              <syncVFSPaths>                                
                <syncVFSPath>/system/categories/</syncVFSPath>
                ...
                <!-- AN ADDED MODULE -->
                <syncVFSPath>/system/modules/org.opencms.ade.config/</syncVFSPath>
              </syncVFSPaths>
				</configuration>
          </plugin>
        </plugins>
      </build>
    </project>

## Check and copy if there are other new files in the webapp

Compare the directories generated in the sections **"Install the new OpenCms version"** and **"Build OpenCms-Basic"**. These are: 

 * `tomcat/webapps/opencms` (Original)
 * `opencms-basic/webapp/target/webapp` (Target)

If there are new files in the source directory, they should be copied to the target directory, and also to the sources 
of the `webapp` subproject. This is:

  * `opencms-basic/webapp/src/main/webapp`
  
The changes in the directory WEB-INF/lib have to be handled using the dependencies in the file `pom.xml` of the 
`webapp` project.

For example, in the update to OpenCms 8.5, these new files and directories were found:

* `WEB-INF/config/solr`
* `WEB-INF/config/wsdl`
* `WEB-INF/config/sun-jaxws.xml`
* `WEB-INF/classes/META-INF/persistence.xml`
* `WEB-INF/classes/ehcache.xml`


## Update versions in opencms-basic

Now is the moment to update the version numbers in the Maven files of opencms-basic. These are:

    - parent
      - general-pom.xml
      - pom.xml
    - webapp
      - pom.xml
    - system
      - pom.xml
    - content
      - pom.xml
    - orgunits
      - pom.xml

First, to update the `pom.xml` files, run the goal `versions:set` command from `parent`:

    mvn –DnewVersion=9.5.1 –DgenerateBackupPoms=false versions:set
    
Then update the version number in `general-pom.xml` manually, as well as the reference to `general-pom.xml` as
the parent project in `parent/pom.xml`

Additionally, the property `opencms.version` in `parent/pom.xml` must also be changed. In the case that a 
bugfix version of the VFS-Plugin will be used, then the property `vfs-plugin.version` should also be set. (e.g. to
`9.5.1a`)

## Update VFS resources

The `system` subproject contains the resources in the OpenCms VFS. These are the steps to update these resources with
the target version. 

1) Change the database connection in **[project webapp]**`/target/webapp/WEB-INF/config/opencms.properties`

Modify the JDBC URL in the propety `db.pool.default.jdbcUrl` to point to the database `opencms` or the one used
to install OpenCms 9.5.1 in the section **"Install the new OpenCms version"**.

Optionally modify the values of the other properties related with the database connection in `opencms.properties`

**-- opencms.properties --**


    #
    # Configuration of the default database pool
    #################################################################################
    # name of the JDBC driver
    db.pool.default.jdbcDriver=com.mysql.jdbc.Driver

    # URL of the JDBC driver
    db.pool.default.jdbcUrl=jdbc:mysql://localhost:3306/opencms

    # optional parameters for the URL of the JDBC driver
    db.pool.default.jdbcUrl.params=?characterEncoding\=UTF-8

    # user name to connect to the database
    db.pool.default.user=root

    # password to connect to the database
    db.pool.default.password=root


2) Ensure that the admin password of OpenCms is correctly configured in `parent/pom.xml`, in case other than the 
   default was configured in the installation of OpenCms 9.5.1.

**-- Extract from parent/pom.xml --**

        <pluginManagement>
            <plugins>
        		<plugin>
                    <groupId>com.comundus.maven</groupId>
                    <artifactId>vfs-maven-plugin</artifactId>
                    <version>${vfs-plugin.version}</version>
                    <configuration>
                    	<adminPassword>admin</adminPassword>
                    	<jspVersion>2.0</jspVersion>
                    	<servletVersion>2.4</servletVersion>
                    </configuration>
                    <dependencies>
                        <dependency>
							<groupId>${jdbcDriver.groupId}</groupId>
							<artifactId>${jdbcDriver.artifactId}</artifactId>
							<version>${jdbcDriver.version}</version>
                        </dependency>                
                    </dependencies>
	        	</plugin>
	        	...
            </plugins>
        </pluginManagement>

3) Delete the original VFS resources
 
Delete the contents of the folders:

* **[project system]**`/src/main/vfs`
* **[project system]**`/src/main/vfs-metadata`

Bear in mind, that following files will have to be recreated after the VFS-synchronization. They can be saved now,
or just take them later from the VCS.

* **[project system]**`/src/main/vfs/system/info/version`
* **[project system]**`/src/main/vfs-metadata/system/info/~folder.xml`
* **[project system]**`/src/main/vfs-metadata/system/info/version.xml`
* **[project system]**`/src/main/vfs/system/.gitignore`
* **[project system]**`/src/main/vfs/system/categories/.gitignore`
* **[project system]**`/src/main/vfs/system/galleries/.gitignore`

4) Execute the synchronisation

Run this command from the project `system`:

    mvn vfs:sync

This will fill the subproject system with the resources of the target version.
    
In case there are execution errors, all the resources in the directories `vfs` and `vfs-metadata` have to be
deleted, correct the error, and run `mvn vfs:sync` again. It is of special importance to delete the file 
`/src/main/vfs/system/#synclist.txt` for the synchronization to update the local files with the contents 
from the OpenCms VFS, and not to modify the VFS.


There are several possible kinds of errors, here are some hints to help solving them:

* **Missing configuration**  
Check which changes were done in the configuration files (under `WEB-INF/config`) and if these changes were 
taken in the sources of the `webapp` project or the file `opencms-module` in the `system` project.

* **Missing libraries**
In case of `NoClassDefFoundError`, `NoSuchMethodError`, similar problems or derived from classloading issues,
the solution might be include or replace a dependency of the VFS-Plugin or of the `webapp` subproject.

* **Changes in the OpenCms behavior or API**  
This might involve modifying the source code of the VFS-Plugin, for which checking the source code of OpenCms will be
helpful. After making the modifications in the VFS-Plugin it has to be installed again in the local repository 
(`mvn install` on `vfs-maven-plugin`).

For the resolution of these problems, it could be necessary to debug the VFS-Plugin, for which the utility `mvnDebug`,
present in all Maven installations since 2.0.8 might be useful.

## Test the new version

With the update of the VFS resources, the version update is ready to be used, and the tests can start, beginning with
a first installation. From the `parent` project, run:
      
    mvn clean package
    mvn tomcat:exploded

Refer to the last section for hints on troubleshooting any problems that might appear.

### What to test

* Compare the contents of the webapp directory, where this subdirectories can be ignored:  
  .`export`
  .`setup`
  .`imagecache`
  .`config/backup`
  .`index`
  .`packages`
  .`setupdata`
  .`logs`
  .`jsp`
  
* Use the synchronize functionality of OpenCms to write the VFS resources to the hard drive and compare the contents.  
* Test all the goals of the plugin.

## Make the new version available

In order to make the VFS-Maven-Plugin available without having to install it in the local Maven repository, it has to
be deployed to a public repository. The plugin has to be made available in the repository in 
`https://comundus.github.io/maven2-repository`. This is done following the instructions in 
<https://github.com/comundus/maven2-repository>. This is, in short:

* Obtain the repository `maven2-repository` from <http://github.com/comundus> and checkout the branch `gh-pages`.

* On `maven-vfs-plugin`, call `mvn clean deploy` indicating the path to this repository in the system property
 `repo.path`. E.g.:

    mvn clean deploy -Drepo.path={path to maven2-repository}

* Commit and push the changes on `maven2-repository`
	
