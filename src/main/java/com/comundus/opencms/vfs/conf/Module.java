package com.comundus.opencms.vfs.conf;

import java.util.List;


/**
 * Value object holding module exportpoint configuration data.
 *
 * (C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
 *
 */
public class Module {
    /**
     * Formal name of the module to configure.
     */
    private String name;

    /**
     * Descriptive name of the module to configure.
     */
    private String nicename;

    /**
     * Content of class tag of module configuration.
     */
    private String clazz;

    /**
     * Module description.
     */
    private String description;

    /**
     * Module version.
     */
    private String version;

    /**
     * Name of the author of the module.
     */
    private String authorname;

    /**
     * EMail address of the author of the module.
     */
    private String authoremail;

    /**
     * Date the module was created.
     */
    private String datecreated;

    /**
     * Login of the user who installed the module.
     */
    private String userinstalled;

    /**
     * Date when the module was installed.
     */
    private String dateinstalled;

    /**
     * List of other modules this module is dependent from.
     */
    private List dependencies;

    /**
     * List of Exportpoint value objects.
     */
    private List exportpoints;

    /**
     * List of module resources.
     */
    private List resources;

    /**
     * List of module parameters.
     */
    private List parameters;

    /**
     * Gets email of module author.
     *
     * @return email address
     */
    public final String getAuthoremail() {
        return this.authoremail;
    }

    /**
     * Sets email of module author.
     *
     * @param a
     *            email address
     */
    public final void setAuthoremail(final String a) {
        this.authoremail = a;
    }

    /**
     * Gets login of module author.
     *
     * @return author login
     */
    public final String getAuthorname() {
        return this.authorname;
    }

    /**
     * Sets login of module author.
     *
     * @param a
     *            author login
     */
    public final void setAuthorname(final String a) {
        this.authorname = a;
    }

    /**
     * Gets the date when the module was created.
     *
     * @return creation date
     */
    public final String getDatecreated() {
        return this.datecreated;
    }

    /**
     * Sets the date when the module was created.
     *
     * @param d
     *            creation date
     */
    public final void setDatecreated(final String d) {
        this.datecreated = d;
    }

    /**
     * Gets the date the module was installed.
     *
     * @return installation date
     */
    public final String getDateinstalled() {
        return this.dateinstalled;
    }

    /**
     * Sets the date the module was installed.
     *
     * @param d
     *            installation date
     */
    public final void setDateinstalled(final String d) {
        this.dateinstalled = d;
    }

    /**
     * Gets the List of module dependencies (containing Strings).
     *
     * @return dependency List
     */
    public final List getDependencies() {
        return this.dependencies;
    }

    /**
     * Sets the List of dependencies.
     *
     * @param d
     *            dependencies
     */
    public final void setDependencies(final List d) {
        this.dependencies = d;
    }

    /**
     * Gets the modules description.
     *
     * @return description
     */
    public final String getDescription() {
        return this.description;
    }

    /**
     * Sets the modules decription.
     *
     * @param d
     *            description
     */
    public final void setDescription(final String d) {
        this.description = d;
    }

    /**
     * Gets the modules Exportpoints List.
     *
     * @return Exportpoints List
     */
    public final List getExportpoints() {
        return this.exportpoints;
    }

    /**
     * Sets the modules Exportpoints List.
     *
     * @param e
     *            Exportpoints List
     */
    public final void setExportpoints(final List e) {
        this.exportpoints = e;
    }

    /**
     * Gets the modules name.
     *
     * @return module name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Sets the modules name.
     *
     * @param n
     *            module name
     */
    public final void setName(final String n) {
        this.name = n;
    }

    /**
     * Gets the modules descriptive name.
     *
     * @return descriptive name
     */
    public final String getNicename() {
        return this.nicename;
    }

    /**
     * Sets the modules descriptive name.
     *
     * @param n
     *            descriptive name
     */
    public final void setNicename(final String n) {
        this.nicename = n;
    }

    /**
     * Gets the modules Parameters List.
     *
     * @return Parameters List
     */
    public final List getParameters() {
        return this.parameters;
    }

    /**
     * Sets the modules Parameters List.
     *
     * @param p
     *            Parameters List
     */
    public final void setParameters(final List p) {
        this.parameters = p;
    }

    /**
     * Gets the modules Resources List.
     *
     * @return Resources List
     */
    public final List getResources() {
        return this.resources;
    }

    /**
     * Sets the modules Resources List.
     *
     * @param r
     *            Resources List
     */
    public final void setResources(final List r) {
        this.resources = r;
    }

    /**
     * Gets the login of the user who installed the module.
     *
     * @return installing user login
     */
    public final String getUserinstalled() {
        return this.userinstalled;
    }

    /**
     * Sets the login of the user who installed the module.
     *
     * @param u
     *            installing user login
     */
    public final void setUserinstalled(final String u) {
        this.userinstalled = u;
    }

    /**
     * Gets the version of the module.
     *
     * @return module version
     */
    public final String getVersion() {
        return this.version;
    }

    /**
     * Sets the version of the module.
     *
     * @param v
     *            version
     */
    public final void setVersion(final String v) {
        this.version = v;
    }

    /**
     * Gets the class of the module. (without overriding Object.getClass())
     *
     * @return class
     */
    public final String getClazz() {
        return this.clazz;
    }

    /**
     * Sets the class of the module.
     *
     * @param c
     *            class
     */
    public final void setClass(final String c) {
        this.clazz = c;
    }
}
