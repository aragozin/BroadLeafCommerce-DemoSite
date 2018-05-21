/*
 * #%L
 * BroadleafCommerce Solr Starter
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package com.broadleafcommerce.solr.autoconfigure;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jeff Fischer
 */
@ConfigurationProperties("solr.server")
public class SolrProperties {

    /**
     * The version of solr to use.
     */
    protected String version = "5.3.1";
    /**
     * The working directory in the filesystem where solr will be downloaded and expanded for execution.
     */
    protected String workingDirectory = System.getProperty("java.io.tmpdir") + "/solr-" + version;
    /**
     * The url from which to download the solr executable archive. There should be two params (using String.format %s syntax) declared in the url pattern. These params take the solr version.
     */
    protected String downloadUrl = "http://archive.apache.org/dist/lucene/solr/%s/solr-%s.%s";
    /**
     * The name of the solr directory. Includes a param (using String.format %s syntax) to hold ther version.
     */
    protected String name = "solr-%s";
    /**
     * Proxy host should a proxy be required to download solr
     */
    protected String downloadProxyHost;
    /**
     * Port to use to access a proxy for downloading solr
     */
    protected String downloadProxyPort;
    /**
     * User name to use to access a proxy for downloading solr
     */
    protected String downloadProxyUserName;
    /**
     * Password to use to access a proxy for downloading solr
     */
    protected String downloadProxyPassword;
    /**
     * Port on which solr will listen for requests
     */
    protected int port = 8983;
    /**
     * The resource path (on the classpath) where solr configuration is found at runtime. Used during solr initial expansion and is synched on each subsequent solr start.
     */
    protected String configSourcePath = "solr/standalone/solrhome";
    /**
     * Allows explicit declaration via property for whether or not solr is allowed to launch via auto configuration.
     */
    protected Boolean autoConfigEnabled = true;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDownloadProxyHost() {
        return downloadProxyHost;
    }

    public void setDownloadProxyHost(String downloadProxyHost) {
        this.downloadProxyHost = downloadProxyHost;
    }

    public String getDownloadProxyPort() {
        return downloadProxyPort;
    }

    public void setDownloadProxyPort(String downloadProxyPort) {
        this.downloadProxyPort = downloadProxyPort;
    }

    public String getDownloadProxyUserName() {
        return downloadProxyUserName;
    }

    public void setDownloadProxyUserName(String downloadProxyUserName) {
        this.downloadProxyUserName = downloadProxyUserName;
    }

    public String getDownloadProxyPassword() {
        return downloadProxyPassword;
    }

    public void setDownloadProxyPassword(String downloadProxyPassword) {
        this.downloadProxyPassword = downloadProxyPassword;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConfigSourcePath() {
        return configSourcePath;
    }

    public void setConfigSourcePath(String configSourcePath) {
        this.configSourcePath = configSourcePath;
    }

    public Boolean getAutoConfigEnabled() {
        return autoConfigEnabled;
    }

    public void setAutoConfigEnabled(Boolean autoConfigEnabled) {
        this.autoConfigEnabled = autoConfigEnabled;
    }
}
