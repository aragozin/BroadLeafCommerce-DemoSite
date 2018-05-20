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

import java.io.File;
import java.io.FilenameFilter;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.persist.HsqlProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import info.ragozin.hsql.HsqlStarter;

/**
 * Adapted from https://www.javacodegeeks.com/2012/11/embedding-hsqldb-server-instance-in-spring.html by Allen Chee
 *
 * @author Jeff Fischer
 */
public class HSQLDBServer implements SmartLifecycle {

    private static final Log LOG = LogFactory.getLog(HSQLDBServer.class);
    protected HsqlProperties props;

    public HSQLDBServer(final HSQLDBProperties autoProps, Environment environment) {
        clearState(autoProps, environment);
        Properties databaseConfig = new Properties();
        databaseConfig.setProperty("server.database.0", "file:" + autoProps.getWorkingDirectory() + autoProps.getDbName());
        databaseConfig.setProperty("server.dbname.0", autoProps.getDbName());
        databaseConfig.setProperty("server.remote_open", "true");
        databaseConfig.setProperty("hsqldb.reconfig_logging", "false");
        databaseConfig.setProperty("server.port", Integer.toString(autoProps.getPort()));
        
        this.props = new HsqlProperties(databaseConfig);
        
        // start on construction since we need this to be active immediately
        start();
    }

    @Override
    public boolean isRunning() {
        boolean isRunning = false;
        final int port = props.getIntegerProperty("server.port", 0);
        final String url = "jdbc:hsqldb:hsql://127.0.0.1:" + port + "/"
                           + props.getProperty("server.dbname.0", "");
        final String username = "SA";
        final String password = "";
        
        try (Connection ignored = DriverManager.getConnection(url, username, password)) {
            isRunning = true;
        } catch (SQLException e) {
            try (Socket ignored = new Socket(InetAddress.getByName(null), port)) {
                // see if the port is being used already (by something other than HSQL)
                LOG.error("Port," + port + ", is already in use but not by HSQL. "
                          + "To find out the ID of the process using that port, open a terminal. Then, "
                          + "if on Mac OS or Linux, use `lsof -i :" + port + "`, "
                          + "or, if on Windows, use `netstat -ano | findstr " + port + "`.");
                isRunning = true;
            } catch (Exception ignored) {
                // otherwise, it's not in use, yet
                LOG.info("HSQL server is not running.");
            }
        }
        
        return isRunning;
    }

    @Override
    public void start() {
        // Extra isRunning() check since this is invoked on construction
        final boolean isRunning = props != null && isRunning();
        
        if (!isRunning) {
            LOG.info("Starting HSQL server...");
            HsqlStarter.start();
        } else if (isRunning) {
            LOG.info("HSQL server is already running. Will not start a new instance.");
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable runnable) {
        stop();
        runnable.run();
    }

    protected void clearState(final HSQLDBProperties autoProps, Environment environment) {
        File dbFile = new File(autoProps.getWorkingDirectory());
        boolean clearState = autoProps.getClearState();
        boolean isPropertyClear = autoProps.getClearStateOnPropertyOnly();
        if (isPropertyClear) {
            if (StringUtils.isEmpty(autoProps.getClearStateProperty())) {
                LOG.warn("clearStateOnPropertyOnly was set to true, but a clearStateProperty was not defined. Not clearing database state based on the property.");
                clearState = false;
            } else {
                String propVal = environment.getProperty(autoProps.getClearStateProperty());
                if (StringUtils.isEmpty(propVal)) {
                    LOG.warn(String.format("Unable to find the %s property in the Spring environment. Not clearing database state based on the property.", autoProps.getClearStateProperty()));
                    clearState = false;
                } else {
                    if (!StringUtils.isEmpty(autoProps.getClearStatePropertyValues())) {
                        String[] vals = autoProps.getClearStatePropertyValues().split(";");
                        Arrays.sort(vals);
                        if (Arrays.binarySearch(vals, propVal) < 0) {
                            clearState = false;
                        }
                    }
                }
            }
        }
        if (dbFile.exists() && dbFile.isDirectory() && clearState) {
            File[] myDBContents = dbFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(autoProps.getDbName() + ".");
                }
            });
            for (File item : myDBContents) {
                boolean deleted = FileSystemUtils.deleteRecursively(item);
                if (!deleted) {
                    LOG.warn(String.format("Unable to clear previous temporary database file (%s). As a result, previous, unwanted values may be utilized during this run.", item.getAbsolutePath()));
                }
            }
        }
    }
}
