package com.comundus.opencms.vfs.conf;


/**
 * Value object holding module exportpoint configuration data.
 *
 * (C) comundus GmbH, D-71332 WAIBLINGEN, www.comundus.com
 *
 */
public class Exportpoint {
    /**
     * Exportpoint URI.
     */
    private String uri;

    /**
     * Exportpoint RFS destination.
     */
    private String destination;

    /**
     * Gets this exportpoints URI.
     * @return URI
     */
    public final String getUri() {
        return this.uri;
    }

    /**
     * Sets this exportpoints URI.
     * @param u URI
     */
    public final void setUri(final String u) {
        this.uri = u;
    }

    /**
     * Gets this exportpoints destination.
     * @return destination
     */
    public final String getDestination() {
        return this.destination;
    }

    /**
     * Sets this exportpoints destination.
     * @param d destination
     */
    public final void setDestination(final String d) {
        this.destination = d;
    }
}
