package jboot.repository.server.proxy;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.loader.boot.repository.ModelRepositoryLayoutFactory;
import jboot.repository.client.IRepository;
import jboot.repository.client.RepositoryAccess;
import jboot.repository.server.Runner;

public class RepositoryProxyRunner extends Runner {
	private static Logger log = Logger.getLogger(RepositoryProxyRunner.class.getName());
	private String localPath;
	private List<URL> locations;

	public RepositoryProxyRunner(String[] args, String[] bootArgs) throws Exception {
		super(args);
		locations = new ArrayList<URL>();
		for (String arg : args) {
			if (arg.startsWith("-cachepath:")) {
				localPath = arg.substring("-cachepath:".length());
			}
		}
		for (String arg : bootArgs) {
			 if (arg.startsWith("-remoterepo:")) {
				String[] repoInfo = arg.substring("-remoterepo:".length()).split(":", 5);
				String strUrl = "http://";
				if (repoInfo.length >= 3) {
					strUrl += repoInfo[2];
				}
				if (repoInfo.length >= 4) {
					strUrl += ":" + repoInfo[3];
				}
				strUrl += "/";
				URL url = new URL(strUrl);
				locations.add(url);
			}
		}
		if (localPath == null || localPath.trim().isEmpty()) {
			localPath = System.getProperty("jboot.home", ".jboot") + "/repoproxy/cache";
		}
		File file = new File(localPath);
		if (!file.exists()) {
			file.mkdirs();
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("The repository proxy cache path is not a directory. " + localPath);			
		}
		if (locations.size() == 0) {
			throw new IllegalArgumentException("Cannot start repository proxy server without at least one remote repository specified.");
		}
	}

	public RepositoryProxyRunner(List<URL> locations, URL proxyServerUrl, String localPath) throws Exception {
		super(proxyServerUrl);
		this.localPath = localPath;
		this.locations = locations;
	}

	public void start() throws Exception {
		if (log.isLoggable(Level.INFO)) {
			log.info("Starting repository proxy...");
			log.info("Repository cache path: " + localPath);
			for (URL url : locations) {
				log.info("Proxied remote repository: " + url.toString());
			}
		}
		IRepository remoteRepository = new RepositoryAccess(locations);
		DefaultModelRepositoryLayout localRepository = (DefaultModelRepositoryLayout) new ModelRepositoryLayoutFactory().addRepositoryRootPath(localPath).createModelRepositoryLayout();
		final ResourceProxyRepository resourceProxyRepository = new ResourceProxyRepository(remoteRepository, localRepository);
		addResource(resourceProxyRepository);
		super.start();
	}

}
