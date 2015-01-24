package jboot.repository.server;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.boot.node.DefaultModelLoader;
import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.loader.boot.repository.ModelRepositoryLayoutFactory;
import jboot.loader.boot.resolver.DefaultProjectResolver;
import jboot.loader.boot.resolver.IProjectResolver;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.repository.client.builder.DefaultProjectInfoResolver;
import jboot.repository.client.builder.IProjectInfoResolver;
import jboot.repository.client.info.ModelNodeInfoResult;

public class RepositoryRunner extends Runner {
	private static Logger log = Logger.getLogger(RepositoryRunner.class.getName());
	private String localPath;

    public RepositoryRunner(String[] args, String[] bootArgs) throws Exception {
    	super(args);
    	for (String arg : bootArgs) {
    		if (arg.startsWith("-localrepo:")) {
    			localPath = arg.substring("-localrepo:".length());
    		}
    	}
    	if (localPath == null || localPath.trim().isEmpty()) {
    		throw new IllegalArgumentException("Cannot start repository server without a valid path to the repository on the local filesystem.");
    	}
    	File file = new File(localPath);
    	if (!file.exists() || !file.isDirectory()) {
    		throw new IllegalArgumentException("The repository path is invalid (non-existing or not a directory). " + localPath);
    	}
    }

    public RepositoryRunner(URL url, String localPath) throws Exception {
		super(url);
		this.localPath = localPath;
	}

	@Override
	public void start() throws Exception {
		if (log.isLoggable(Level.INFO)) {
			log.info("Starting repository server...");
			log.info("Repository path: " + localPath);
		}
		ModelNodeResult modelNodeResult = new ModelNodeResult();
		ModelNodeInfoResult modelNodeInfoResult = new ModelNodeInfoResult();
		DefaultModelRepositoryLayout localRepository = (DefaultModelRepositoryLayout) new ModelRepositoryLayoutFactory().addRepositoryRootPath(localPath).createModelRepositoryLayout();
		IProjectResolver projectResolver = new DefaultProjectResolver(modelNodeResult, localRepository, new DefaultModelLoader());
		IProjectInfoResolver projectInfoResolver = new DefaultProjectInfoResolver(projectResolver, modelNodeResult, modelNodeInfoResult);
		final ResourceRepository resourceRepository = new ResourceRepository(projectInfoResolver, localRepository, modelNodeResult, modelNodeInfoResult);
		addResource(resourceRepository);
		super.start();
	}
}
