package jboot.loader.boot.node.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JarResourceDaemon extends Thread {
    private static Logger log = Logger.getLogger(JarResourceDaemon.class.getName()); 
    public static final String JBOOT_JAR_RESOURCE_DAEMON_SLEEP_INTERVAL = "jboot.jarResourceDaemon.sleepInterval";
    public static final String JBOOT_JAR_RESOURCE_DAEMON_TIMEOUT = "jboot.jarResourceDaemon.timeout";
    
    private static final long SLEEP_INTERVAL = 300000L;
    private static final long TIMEOUT = 300000L;
    
    private Map<JarResource, Long> resources = Collections.synchronizedMap(new HashMap<JarResource, Long>());
    private long sleepInterval;
    private long timeout;
    
    public JarResourceDaemon() {
        String strSleepInterval = System.getProperty(JBOOT_JAR_RESOURCE_DAEMON_SLEEP_INTERVAL);
        String strTimeout = System.getProperty(JBOOT_JAR_RESOURCE_DAEMON_TIMEOUT);
        if (strSleepInterval != null) {
            try {
                sleepInterval = new Long(strSleepInterval).longValue();
            } catch (NumberFormatException e) {
                log.info(strSleepInterval + " is not a valid long");
            }
        }
        if (strTimeout != null) {
            try {
                timeout = new Long(strTimeout).longValue();
            } catch (NumberFormatException e) {
                log.info(strTimeout + " is not a valid long");
            }
        }
        if (sleepInterval < 10000L) {
            sleepInterval = SLEEP_INTERVAL;
        }
        
        if (timeout < 10000L) {
            timeout = TIMEOUT;
        }
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            closeAgingResources();
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINEST, "JarResourceDaemon interrupted", e);
                }
                running = false;
            }
        }
    }

    private void closeAgingResources() {
        Entry<JarResource, Long>[] array = resources.entrySet().toArray(new Entry[0]);
        for (Entry<JarResource, Long> entry: array) {
            JarResource resource = entry.getKey();
            synchronized (resource) {
                if (System.currentTimeMillis() - entry.getValue().longValue() > timeout) {
                    resource.close();
                    resources.remove(resource);
                }
            }
        }
    }
    public void resourceUsed(JarResource jarResource) {
        resources.put(jarResource, new Long(System.currentTimeMillis()));
    }
    public void resourceFreed(JarResource jarResource) {
        resources.remove(jarResource);
    }    
}
