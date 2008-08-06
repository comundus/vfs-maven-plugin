package com.comundus.opencms.vfs.conf;


/**
 * Value object holding module parameters.
 *
 * (C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
 *
 */
public class Param {
    /**
     * Parameter name.
     */
    private String name;

    /**
     * Parameter value.
     */
    private String value;

    /**
     * Gets the parameters name.
     *
     * @return name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Sets the parameters name.
     *
     * @param n
     *            name
     */
    public final void setName(final String n) {
        this.name = n;
    }

    /**
     * Gets the parameters value.
     *
     * @return value
     */
    public final String getValue() {
        return this.value;
    }

    /**
     * Sets the parameters value.
     *
     * @param v
     *            value
     */
    public final void setValue(final String v) {
        this.value = v;
    }
}
