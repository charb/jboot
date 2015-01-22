package jboot.repository.server.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.repository.client.IRepository;
import jboot.repository.client.checksum.CRC32Calculator;
import jboot.repository.client.checksum.IChecksumCalculator;
import jboot.repository.client.info.model.ArtifactInfo;
import jboot.repository.client.info.model.FileInfo;
import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.model.ModelNodeInfos;
import jboot.repository.client.info.model.RepositoryException;
import jboot.repository.client.info.model.VersionsInfo;
import jboot.repository.server.ResourceRepository;

@Path("/repository/v1/")
public class ResourceProxyRepository implements IRepository {
	private IRepository remoteRepository;
	private DefaultModelRepositoryLayout localRepository;
	private IChecksumCalculator checksumCalculator;
	private Map<String, String> checksumCache; // key = file's absolute path, value = checksum.
	private Marshaller marshaller;

	public ResourceProxyRepository(IRepository remoteRepository, DefaultModelRepositoryLayout localRepository) throws Exception {
		this.remoteRepository = remoteRepository;
		this.localRepository = localRepository;
		this.checksumCalculator = new CRC32Calculator();
		this.checksumCache = new HashMap<String, String>();
		File localCache = new File(localRepository.getRepositoryRootPath());
		if (!localCache.exists()) {
			localCache.mkdirs();
		}
		JAXBContext ctx = JAXBContext.newInstance(RepositoryException.class);
		marshaller = ctx.createMarshaller();
	}

	@Override
	@Path("/ping")
	@POST
	@Consumes("application/xml")
	public void ping() throws Exception {
		try {
			remoteRepository.ping();
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	};

	@Override
	@Path("/refreshModelNodeInfos")
	@POST
	@Consumes("application/xml")
	public void refreshModelNodeInfos() throws Exception {
		try {
			synchronized (this) {
				checksumCache.clear();
			}
			remoteRepository.refreshModelNodeInfos();
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	}

	@GET
	@Path("/getModelNodeInfos/{groupId}/{artifactId}/{version}")
	@Produces("application/xml")
	public ModelNodeInfos getModelNodeInfos(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version) throws Exception {
		try {
			return remoteRepository.getModelNodeInfos(groupId, artifactId, version);
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
		return null;
	}

	@GET
	@Path("/getModelNodeInfo/{groupId}/{artifactId}/{version}")
	@Produces("application/xml")
	public ModelNodeInfo getModelNodeInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version) throws Exception {
		try {
			return remoteRepository.getModelNodeInfo(groupId, artifactId, version);
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@GET
	@Path("/getVersionsInfo/{groupId}/{artifactId}")
	@Produces("application/xml")
	public VersionsInfo getVersionsInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId) throws Exception {
		try {
			return remoteRepository.getVersionsInfo(groupId, artifactId);
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
			return null;
		}
	}

	@Override
	@GET
	@Path("/getArtifact/{groupId}/{artifactId}/{version}")
	@Produces("application/zip")
	public synchronized InputStream getArtifact(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version, @QueryParam("classifier") String classifier, @QueryParam("type") String type) throws Exception {
		try {
			FileResource artifactFileResource = (FileResource) localRepository.getArtifactResource(groupId, artifactId, version, classifier, type);
			if (artifactFileResource != null) {
				ModelNodeInfo modelNodeInfo = remoteRepository.getModelNodeInfo(groupId, artifactId, version);
				if (modelNodeInfo != null) {
					ArtifactInfo artifactInfo = modelNodeInfo.getArtifact(classifier, type);
					if (artifactInfo != null) {
						if (getChecksum(artifactFileResource.getFile()).equals(artifactInfo.getChecksum())) {
							return new FileInputStream(artifactFileResource.getFile());
						}
					}
				}
			}
			InputStream in = remoteRepository.getArtifact(groupId, artifactId, version, classifier, type);
			if (in != null) {
				File artifactFile = new File(localRepository.getArtifactPath(groupId, artifactId, version, classifier, type));
				writeFile(artifactFile, in);
				updateChecksumCache(artifactFile);
				return new FileInputStream(artifactFile);
			}
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@GET
	@Path("/getPom/{groupId}/{artifactId}/{version}")
	@Produces("application/xml")
	public InputStream getPom(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version) throws Exception {
		try {
			return remoteRepository.getPom(groupId, artifactId, version);
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@Path("/deployPom/{groupId}/{artifactId}/{version}")
	@Consumes("application/zip")
	@POST
	public void deployPom(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version, InputStream pom) throws Exception {
		try {
			remoteRepository.deployPom(groupId, artifactId, version, pom);
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	}

	@Override
	@Path("/deployArtifact/{groupId}/{artifactId}/{version}")
	@Consumes("application/zip")
	@POST
	public synchronized void deployArtifact(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version, @QueryParam("classifier") String classifier, @QueryParam("type") String type, InputStream artifact) throws Exception {
		try {
			String artifactFileName = localRepository.getArtifactPath(groupId, artifactId, version, classifier, type);
			File file = new File(artifactFileName);
			writeFile(file, artifact);
			updateChecksumCache(file);
			remoteRepository.deployArtifact(groupId, artifactId, version, classifier, type, new FileInputStream(file));
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	}

	@Override
	@GET
	@Path("/getfile/{path:.*}")
	@Produces("application/zip")
	public synchronized InputStream getFile(@PathParam("path") String path) throws Exception {
		try {
			File file = new File(localRepository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			if (!file.exists()) {
				InputStream in = remoteRepository.getFile(path);
				if (in != null) {
					writeFile(file, in);
				}
			}
			if (file.exists()) {
				return new FileInputStream(file);
			}
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@Path("/setfile/{path:.+}")
	@Consumes("application/zip")
	@POST
	public void setFile(@PathParam("path") String path, InputStream inputStream) throws Exception {
		try {
			File file = new File(localRepository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			writeFile(file, inputStream);
			remoteRepository.setFile(path, new FileInputStream(file));
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	}

	@Override
	@GET
	@Path("/file_ls/{path:.*}")
	@Produces("application/xml")
	public FileInfo ls(@PathParam("path") String path) throws Exception {
		try {
			return remoteRepository.ls(path);
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@Path("/file_mkdir/{path:.+}")
	@Consumes("application/zip")
	@POST
	public void mkdir(@PathParam("path") String path) throws Exception {
		try {
			File file = new File(localRepository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			remoteRepository.mkdir(path);
			file.mkdirs();
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	}

	@Override
	@Path("/file_delete/{path:.+}")
	@Consumes("application/zip")
	@POST
	public void delete(@PathParam("path") String path) throws Exception {
		try {
			File file = new File(localRepository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			remoteRepository.delete(path);
			file.delete();
		} catch (Throwable throwable) {
			ResourceRepository.throwException(throwable, marshaller);
		}
	}

	private void writeFile(File file, InputStream in) throws Exception {
		file.getParentFile().mkdirs();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				fos.write(buf, 0, len);
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
			if (in != null) {
				in.close();
			}
		}
	}

	private String getChecksum(File file) throws Exception {
		String strChecksum = checksumCache.get(file.getAbsolutePath());
		if (strChecksum == null) {
			strChecksum = checksumCalculator.getChecksum(file);
			checksumCache.put(file.getAbsolutePath(), strChecksum);
		}
		return strChecksum;
	}

	private void updateChecksumCache(File file) throws Exception {
		String strChecksum = checksumCalculator.getChecksum(file);
		checksumCache.put(file.getAbsolutePath(), strChecksum);
	}

}