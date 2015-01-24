package jboot.repository.server;

import java.io.File;
import java.io.RandomAccessFile;

import java.lang.management.ManagementFactory;
import java.net.URL;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;


public class Runner implements Runnable {
    private static Logger log = Logger.getLogger(Runner.class.getName());
    private URL serverUrl;
    private Server server;
    private Set<Object> singletons;

    private String readyFilePath;
    private File readyFile;
    private FileChannel readyFileChannel;
    private FileLock readyFileLock;


    public Runner(String[] args) throws Exception {
        this();
        for (String arg : args) {
            if (arg.startsWith("-url:")) {
                serverUrl = new URL(arg.substring("-url:".length()));
            }
            if (arg.startsWith("-ready.file.name:")) {
                readyFilePath = arg.substring("-ready.file.name:".length());
            }
        }
        if (serverUrl == null) {
            throw new IllegalArgumentException("Cannot start server without a server url.");
        }
    }

    public Runner(URL serverUrl) throws Exception {
        this();
        this.serverUrl = serverUrl;
    }

    protected Runner() {
        this.singletons = new HashSet<Object>();
    }

    public void start() throws Exception {
        if (log.isLoggable(Level.INFO)) {
            log.info("Starting server at " + serverUrl);
        }
        String host = serverUrl.getHost();
        int port = serverUrl.getPort();

        server = new Server();
        MBeanContainer mbeanContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.getContainer().addEventListener(mbeanContainer);
        server.addBean(mbeanContainer);
        mbeanContainer.addBean(org.eclipse.jetty.util.log.Log.getRootLogger());

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(host);
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector });

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        Application application = new DefaultResourceConfig();
        application.getSingletons().addAll(singletons);

        ServletHolder servletHolder = new ServletHolder(new ServletContainer(application));
        context.addServlet(servletHolder, "/*");

        server.setHandler(context);

        server.start();
        if (readyFilePath != null) {
            if (log.isLoggable(Level.INFO)) {
                log.info("Locking ready file: " + readyFilePath);
            }
            lockReadyFile(readyFilePath);
            if (log.isLoggable(Level.INFO)) {
                log.info("Locked ready file: " + readyFilePath);
            }
        }
        if (log.isLoggable(Level.INFO)) {
            log.info("Server ready.");
        }
    }

    public void stop() throws Exception {
        if (log.isLoggable(Level.INFO)) {
            log.info("Stopping server at " + serverUrl);
        }
        server.stop();
        if (log.isLoggable(Level.INFO)) {
            log.info("Server stopped.");
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    protected void addResource(Object resource) {
        singletons.add(resource);
    }

    private void lockReadyFile(String readyFileName) throws Exception {
        readyFile = new File(readyFileName);
        if (!readyFile.exists()) {
            readyFile.getAbsoluteFile().getParentFile().mkdirs();
            readyFile.createNewFile();
        }
        readyFileChannel = new RandomAccessFile(readyFile, "rw").getChannel();
        readyFileLock = readyFileChannel.lock();
        readyFileLock.isValid();
    }
}
