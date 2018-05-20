package com.broadleafcommerce.solr.autoconfigure;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import info.ragozin.solr.SolrStarter;

/**
 * @author Jeff Fischer
 */
public class SolrServer implements SmartLifecycle {

    private static final Log LOG = LogFactory.getLog(SolrServer.class);

    protected final SolrProperties props;
    protected boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
    
    protected static final String WINDOWS_EXT = "zip";
    protected static final String UNIX_EXT = "tgz";
    
    /**
     * Indicates that Solr was created within this lifecycle and should be stopped. If not started from here,
     * it should also not be stopped from here
     */
    protected boolean created;

    public SolrServer(SolrProperties solrProperties) {
        this.props = solrProperties;
    }

    @Override
    public boolean isRunning() {
        try (Socket ignored = new Socket(InetAddress.getByName(null), props.getPort())) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public void start() {
        startSolr();
    }

    @Override
    public void stop() {
        stopSolr();
    }

    @Override
    public int getPhase() {
        return 0;
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

    protected String getWorkingDirectory() {
        return props.getWorkingDirectory();
    }

    protected String getSolrCommand() {
        String command = isWin ? "solr.cmd" : "solr";
        String workingDirectory = getWorkingDirectory();
        return new File(workingDirectory, String.format(props.getName(), props.getVersion()) +
                    File.separator + "bin" + File.separator + command).getAbsolutePath();
    }

//    protected void startSolr() {
//        if (!isRunning()) {
//            if (!downloadSolrIfApplicable()) {
//                throw new IllegalStateException("Could not download or expand Solr, see previous logs for more information");
//            }
//            stopSolr();
//            synchConfig();
//            {
//                CommandLine cmdLine = new CommandLine(getSolrCommand());
//                cmdLine.addArgument("start");
//                cmdLine.addArgument("-p");
//                cmdLine.addArgument(Integer.toString(props.getPort()));
//                Executor executor = new DefaultExecutor();
//                executor.setStreamHandler(new PumpStreamHandler(System.out));
//                try {
//                    executor.execute(cmdLine);
//                    created = true;
//                    checkCoreStatus();
//                } catch (IOException e) {
//                    LOG.error("Problem starting Solr", e);
//                }
//            }
//        }
//    }

    protected void startSolr() {
        if (!isRunning()) {
            if (!downloadSolrIfApplicable()) {
                throw new IllegalStateException("Could not download or expand Solr, see previous logs for more information");
            }
            stopSolr();
            synchConfig();
            {
            	SolrStarter.startSolr();
            }
        }
    }
    
    protected void checkCoreStatus() {
        boolean allCoresOnline = false;
        String workingDirectory = getWorkingDirectory();
        File serverDir = new File(workingDirectory, String.format(props.getName(), props.getVersion()) +
                            File.separator + "server");
        File home = new File(serverDir, "solr");
        File coreConfigDir = new File(home, "cores");
        if (coreConfigDir.isDirectory()) {
            File[] cores = coreConfigDir.listFiles();
            for (int j=0;j<20;j++) {
                allCoresOnline = true;
                try {
                    URL testUrl = new URL("http://localhost:" + props.getPort() + "/solr/admin/cores?action=STATUS&indexInfo=false");
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    DocumentBuilder builder = dbf.newDocumentBuilder();
                    Document doc = builder.parse(testUrl.openStream());
                    XPathFactory factory = XPathFactory.newInstance();
                    XPath xPath = factory.newXPath();
                    for (File core : cores) {
                        if (core.isDirectory()) {
                            String expression = "response/lst[@name='status']/lst[@name='" + core.getName() + "']";
                            NodeList temp1 = (NodeList) xPath.evaluate(expression, doc, XPathConstants.NODESET);
                            if (temp1 != null) {
                                Node node = temp1.item(0);
                                NodeList temp2 = (NodeList) xPath.evaluate("long[@name='uptime']", node, XPathConstants.NODESET);
                                if (temp2 == null) {
                                    allCoresOnline = false;
                                    break;
                                }
                            } else {
                                allCoresOnline = false;
                                break;
                            }
                        }
                    }
                    if (allCoresOnline) {
                        break;
                    }
                } catch (Exception e) {
                    LOG.error(String.format("Problem while checking core status: %s", e.getMessage()));
                    allCoresOnline = false;
                    break;
                }
                try {
                    Thread.sleep(1000L);
                } catch (Throwable e) {
                    //do nothing
                }
            }
        }
        if (!allCoresOnline) {
            LOG.error("Unable to verify solr core status. Proceeding with startup.");
        }
    }

//    protected void stopSolr() {
//        if (created) {
//            String command = isWin ? "solr.cmd" : "solr";
//            {
//                String workingDirectory = getWorkingDirectory();
//                CommandLine cmdLine = new CommandLine(new File(workingDirectory, String.format(props.getName(), props.getVersion()) +
//                        File.separator + "bin" + File.separator + command).getAbsolutePath());
//                cmdLine.addArgument("stop");
//                cmdLine.addArgument("-p");
//                cmdLine.addArgument(Integer.toString(props.getPort()));
//                Executor executor = new DefaultExecutor();
//                executor.setStreamHandler(new PumpStreamHandler(System.out));
//                try {
//                    executor.execute(cmdLine);
//                } catch (IOException e) {
//                    //do nothing
//                }
//            }
//        }
//    }

    protected void stopSolr() {
    	SolrStarter.startSolr();
    }
    
    protected boolean synchConfig() {
        String workingDirectory = getWorkingDirectory();
        File serverDir = new File(workingDirectory, String.format(props.getName(), props.getVersion()) +
                            File.separator + "server");
        File home = new File(serverDir, "solr");
        if (home.exists()) {
            File oldHome = new File(serverDir, "originalSolr");
            if (!oldHome.exists()) {
                home.renameTo(oldHome);
            }
        }
        home = new File(serverDir, "solr");
        if (!home.exists()) {
            home.mkdir();
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:"+props.getConfigSourcePath()+"/**/*");
            Resource[] targetResources = resolver.getResources(home.toURI().toURL().toExternalForm() + "/**/*");
            LOG.info(String.format("Syncing solr configuration to %s", home.getAbsolutePath()));
            for (Resource targetResource : targetResources) {
                if (targetResource.getFile().isDirectory()) {
                    String targetPath = targetResource.getURL().toExternalForm();
                    if (!targetPath.contains("/data/")) {
                        String targetName = targetResource.getFilename();
                        boolean targetObsolete = true;
                        for (Resource resource : resources) {
                            String fileName = resource.getFilename();
                            if (targetName.equals(fileName)) {
                                targetObsolete = false;
                                break;
                            }
                        }
                        if (targetObsolete) {
                            LOG.info(String.format("Deleting obsolete directory detected during sync: %s", targetResource.getFile().getAbsolutePath()));
                            FileUtils.deleteDirectory(targetResource.getFile());
                        }
                    }
                }
            }
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName.contains(".")) {
                    String filePath = resource.getURL().toExternalForm();
                    filePath = filePath.substring(filePath.lastIndexOf(props.getConfigSourcePath()) + props.getConfigSourcePath().length(), filePath.length());
                    File targetFile = new File(home, filePath);
                    targetFile.getParentFile().mkdirs();
                    LOG.info(String.format("Syncing solr config file: %s", targetFile.getAbsolutePath()));
                    FileUtils.copyInputStreamToFile(resource.getInputStream(), targetFile);
                }
            }
        } catch (IOException e) {
            LOG.error("Problem syncing Solr configuration", e);
        }
        return true;
    }

    protected boolean downloadSolrIfApplicable() {
        boolean response = true;
        File workingDirectory = new File(getWorkingDirectory());
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        File destination = new File(workingDirectory, String.format(props.getName() + "." + getExtension(), props.getVersion()));
        File command = new File(getSolrCommand());
        if (!command.exists()) {
            OutputStream out = null;
            InputStream in = null;
            try {
                HttpClient client = new HttpClient();
                if (!StringUtils.isEmpty(props.getDownloadProxyHost())) {
                    client.getHostConfiguration().setProxy(props.getDownloadProxyHost(), Integer.parseInt(props.getDownloadProxyPort()));
                    if (!StringUtils.isEmpty(props.getDownloadProxyUserName())) {
                        Credentials cred = new UsernamePasswordCredentials(props.getDownloadProxyUserName(), props.getDownloadProxyPassword());
                        client.getState().setProxyCredentials(AuthScope.ANY, cred);
                    }
                }
                String downloadUrl = String.format(props.getDownloadUrl(), props.getVersion(), props.getVersion(), getExtension());
                GetMethod method = new GetMethod(downloadUrl);
                int statusCode = client.executeMethod(method);
                if (statusCode == HttpStatus.SC_OK) {
                    Long contentLength = method.getResponseContentLength();
                    in = new BufferedInputStream(method.getResponseBodyAsStream());
                    out = new BufferedOutputStream(new FileOutputStream(destination));
                    LOG.info(String.format("Downloading Solr from %s to %s", downloadUrl, destination.getAbsolutePath()));
                    copyLarge(in, out, contentLength);
                } else {
                    // Unsuccessful download
                    LOG.error(String.format("Could not download Solr from %s, response code was %s", downloadUrl, statusCode));
                    response = false;
                }
            } catch (IOException e) {
                LOG.error(String.format("Unable to download solr. If you need to connect through a proxy, the 'solr.dowload.proxyHost', " +
                        "'solr.download.proxyPort', 'solr.download.proxyUserName' and 'solr.download.proxyPassword' " +
                        "properties are available. Also, make sure the user running this application has write priveleges " +
                        "to the download directory %s. A different download directory may be specified via the 'solr.server.workingDirectory' " +
                        "property.", workingDirectory), e);
                response = false;
                try {
                    destination.delete();
                } catch (Exception e2) {
                    LOG.error(e);
                }
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        }
        if (response) {
            File expanded = new File(getSolrCommand());
            if (!expanded.exists()) {
                response = expandDownload(destination, workingDirectory);
            }
        }
        return response;
    }
    
    protected String getExtension() {
        return isWin ? WINDOWS_EXT : UNIX_EXT;
    }

    protected boolean expandDownload(File downloadFile, File workingDirectory) {
        LOG.info(String.format("Expanding %s", downloadFile.getAbsolutePath()));
        boolean response = false;
        if (!isWin) {
            response = expandNix(downloadFile, workingDirectory, response);
        } else {
            response = expandWin(downloadFile, workingDirectory, response);
        }
        if (response) {
            LOG.info(String.format("Finished expanding %s", downloadFile.getAbsolutePath()));
        }
        return response;
    }

    protected boolean expandWin(File downloadFile, File workingDirectory, boolean response) {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(downloadFile));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(workingDirectory, fileName);
                if (ze.isDirectory()) {
                    newFile.mkdir();
                } else {
                    LOG.info("File unzip : " + newFile.getAbsoluteFile());
                    FileOutputStream fos = new FileOutputStream(newFile);
                    try {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                }
                ze = zis.getNextEntry();
            }
            response = true;
        } catch (IOException e) {
            LOG.error(String.format("Unable to unpack %s. Make sure the running user has appropriate permissions to unpack the file" +
                    "in the same directory.", downloadFile.getAbsolutePath()), e);
        } finally {
            if (zis != null) {
                try {
                    zis.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                IOUtils.closeQuietly(zis);
            }
        }
        return response;
    }

    protected boolean expandNix(File downloadFile, File workingDirectory, boolean response) {
        CommandLine cmdLine = new CommandLine("tar");
        cmdLine.addArgument("-zxvf");
        cmdLine.addArgument(downloadFile.getName());
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDirectory);
        executor.setStreamHandler(new PumpStreamHandler(System.out));
        try {
            executor.execute(cmdLine);
            response = true;
        } catch (IOException e) {
            LOG.error(String.format("Unable to unpack %s. Make sure the running user has appropriate permissions to unpack the file" +
                    "in the same directory. Also, make sure the 'tar' command is available at the command prompt.", downloadFile.getAbsolutePath()), e);
        }
        return response;
    }

    protected long copyLarge(InputStream input, OutputStream output, long contentLength) throws IOException {
        byte[] buffer = new byte[4096];
        long count = 0L;
        long interim = 0L;
        long interim2 = 0L;
        int n1;
        for (boolean n = false; -1 != (n1 = input.read(buffer)); count += n1) {
            output.write(buffer, 0, n1);
            interim++;
            if (interim % 20L == 0L) {
                System.out.print(String.format("%sK of %sK >>  ", (count / 1000L), (contentLength / 1000L)));
                interim2++;
                if (interim2 % 5L == 0L) {
                    System.out.print("\n");
                }
            }
        }
        return count;
    }

}
