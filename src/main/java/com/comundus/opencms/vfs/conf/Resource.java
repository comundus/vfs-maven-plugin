package com.comundus.opencms.vfs.conf;


/**
 * Value object holding module resource configuration.
 *
 * (C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
 *
 */
public class Resource {
    /**
     * The resources URI.
     */
    private String uri;

    /**
     * Gets the resources URI.
     * @return URI
     */
    public final String getUri() {
        return this.uri;
    }

    /**
     * Sets this resources URI.
     * @param u URI
     */
    public final void setUri(final String u) {
        this.uri = u;
    }
}
