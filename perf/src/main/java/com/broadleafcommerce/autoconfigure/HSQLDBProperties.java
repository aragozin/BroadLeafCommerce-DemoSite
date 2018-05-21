/*
 * #%L
 * BroadleafCommerce Database Starter
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
package com.broadleafcommerce.autoconfigure;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

/**
 * @author Jeff Fischer
 */
@ConfigurationProperties("demo.database")
public class HSQLDBProperties {

    /**
     * Explicitly declare via property whether or not this HSQL database auto configuration should run
     */
    protected Boolean autoConfigEnabled = true;
    /**
     * The name of the database to create
     */
    protected String dbName = "broadleaf";
    /**
     * The working directory in the local file system where the database related files will be stored, ending in a trailing
     * slash
     */
    protected String workingDirectory = (System.getProperty("java.io.tmpdir") + File.separator + "broadleaf-hsqldb" + File.separator).replace("//", "/").replace("\\/", "\\");
    /**
     * Whether or not any database related files stored in the file system are wiped before launch. This guarantees a fresh environment.
     */
    protected Boolean clearState = false;
    /**
     * Whether or not any database related files stored in the file system are wiped before launch. Only true if the "clearStateProperty" and "clearStatePropertyValues" are confirmed.
     */
    protected Boolean clearStateOnPropertyOnly = true;
    /**
     * Used in conjunction with "clearStateOnPropertyOnly", defines the Spring environment property to check.
     */
    protected String clearStateProperty = "blPU.hibernate.hbm2ddl.auto";
    /**
     * Used in conjunction with "clearStateOnPropertyOnly", defines the value(s) [semicolon delimited] that the "clearStateProperty" should be equal to to satisfy the condition. Leaving this property empty signifies that any value for the property is accepted.
     */
    protected String clearStatePropertyValues = "create;create-drop";
    /**
     * The port on which the database listens for jdbc connections.
     */
    protected int port = 9001;

    public Boolean getAutoConfigEnabled() {
        return autoConfigEnabled;
    }

    public void setAutoConfigEnabled(Boolean autoConfigEnabled) {
        this.autoConfigEnabled = autoConfigEnabled;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }

    public Boolean getClearState() {
        return clearState;
    }

    public void setclearState(Boolean clearState) {
        this.clearState = clearState;
    }

    public Boolean getClearStateOnPropertyOnly() {
        return clearStateOnPropertyOnly;
    }

    public void setClearStateOnPropertyOnly(Boolean clearStateOnPropertyOnly) {
        this.clearStateOnPropertyOnly = clearStateOnPropertyOnly;
    }

    public String getClearStateProperty() {
        return clearStateProperty;
    }

    public void setClearStateProperty(String clearStateProperty) {
        this.clearStateProperty = clearStateProperty;
    }

    public String getClearStatePropertyValues() {
        return clearStatePropertyValues;
    }

    public void setClearStatePropertyValues(String clearStatePropertyValues) {
        this.clearStatePropertyValues = clearStatePropertyValues;
    }
}
