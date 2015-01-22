package jboot.repository.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.ModelNodeArtifact;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.loader.boot.resolver.ExceptionCollector;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.repository.client.IRepository;
import jboot.repository.client.builder.IProjectInfoResolver;
import jboot.repository.client.info.ModelNodeInfoResult;
import jboot.repository.client.info.model.FileInfo;
import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.model.ModelNodeInfos;
import jboot.repository.client.info.model.RepositoryException;
import jboot.repository.client.info.model.VersionsInfo;

@Path("/repository/v1/")
public class ResourceRepository implements IRepository {
	private IProjectInfoResolver projectInfoResolver;
	private DefaultModelRepositoryLayout repository;
	private ModelNodeResult modelNodeResult;
	private ModelNodeInfoResult modelNodeInfoResult;
	private Marshaller marshaller;

	public ResourceRepository(IProjectInfoResolver projectInfoResolver, DefaultModelRepositoryLayout repository, ModelNodeResult modelNodeResult, ModelNodeInfoResult modelNodeInfoResult) throws Exception {
		this.projectInfoResolver = projectInfoResolver;
		this.repository = repository;
		this.modelNodeInfoResult = modelNodeInfoResult;
		this.modelNodeResult = modelNodeResult;
		JAXBContext ctx = JAXBContext.newInstance(RepositoryException.class);
		marshaller = ctx.createMarshaller();
	}

	public static void throwException(Throwable throwable, Marshaller marshaller) throws Exception {
		ResponseBuilder builder = Response.status(Response.Status.ACCEPTED); //TODO: verify the use of the ACCEPTED HTTP status code.
		builder.type("text/xml");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		marshaller.marshal((Object) new RepositoryException(throwable.toString(), sw.toString()), os);
		builder.entity(os.toString());
		throw new WebApplicationException(builder.build());
	}

	@Override
	@Path("/ping")
	@POST
	@Consumes("application/xml")
	public void ping() throws Exception {
	}

	@Override
	@Path("/refreshModelNodeInfos")
	@POST
	@Consumes("application/xml")
	public synchronized void refreshModelNodeInfos() throws Exception {
		try {
			this.modelNodeResult.clear();
			this.modelNodeInfoResult.clear();
			this.projectInfoResolver.clear();
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
	}

	@Override
	@GET
	@Path("/getModelNodeInfos/{groupId}/{artifactId}/{version}")
	@Produces("application/xml")
	public synchronized ModelNodeInfos getModelNodeInfos(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version) throws Exception {
		try {
			ExceptionCollector collector = projectInfoResolver.resolve(groupId, artifactId, version);
			collector.log();
			return modelNodeInfoResult.getModelNodeInfos(ModelNode.getId(groupId, artifactId, version));
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
			return null;
		}
	}

	@GET
	@Path("/getModelNodeInfo/{groupId}/{artifactId}/{version}")
	@Produces("application/xml")
	public synchronized ModelNodeInfo getModelNodeInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version) throws Exception {
		try {
			ExceptionCollector collector = projectInfoResolver.resolve(groupId, artifactId, version);
			collector.log();
			ModelNodeInfo modelNodeInfo = modelNodeInfoResult.getModelNodeInfo(ModelNode.getId(groupId, artifactId, version));
			return modelNodeInfo;
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
			return null;
		}
	}

	@Override
	@GET
	@Path("/getVersionsInfo/{groupId}/{artifactId}")
	@Produces("application/xml")
	public VersionsInfo getVersionsInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId) throws Exception {
		try {
			String[] versions = repository.getVersions(groupId, artifactId);
			if (versions != null) {
				VersionsInfo versionsInfo = new VersionsInfo(versions);
				return versionsInfo;
			}
			return new VersionsInfo();
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
			return null;
		}
	}

	@Override
	@GET
	@Path("/getArtifact/{groupId}/{artifactId}/{version}")
	@Produces("application/zip")
	public synchronized InputStream getArtifact(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version, @QueryParam("classifier") String classifier, @QueryParam("type") String type) throws Exception {
		try {
			ExceptionCollector collector = this.projectInfoResolver.resolve(groupId, artifactId, version);
			collector.log();
			ModelNode modelNode = modelNodeResult.getModelNode(ModelNode.getId(groupId, artifactId, version));
			if (modelNode != null) {
				ModelNodeArtifact modelNodeArtifact = modelNode.getArtifact(classifier);
				FileResource fileResource = (FileResource) modelNodeArtifact.getResource();
				if (fileResource != null) {
					return new FileInputStream(fileResource.getFile());
				}
			}
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@GET
	@Path("/getPom/{groupId}/{artifactId}/{version}")
	@Produces("application/xml")
	public synchronized InputStream getPom(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version) throws Exception {
		try {
			ExceptionCollector collector = this.projectInfoResolver.resolve(groupId, artifactId, version);
			collector.log();
			ModelNode modelNode = modelNodeResult.getModelNode(ModelNode.getId(groupId, artifactId, version));
			if (modelNode != null) {
				FileResource fileResource = modelNode.getPom();
				if (fileResource != null) {
					return new FileInputStream(fileResource.getFile());
				}
			}
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
		return null;
	}

	@Override
	@Path("/deployPom/{groupId}/{artifactId}/{version}")
	@Consumes("application/zip")
	@POST
	public void deployPom(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version, InputStream pom) throws Exception {
		try {
			String pomFileName = repository.getPomPath(groupId, artifactId, version);
			File pomFile = new File(pomFileName);
			writeFile(pomFile, pom);
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
	}

	@Override
	@Path("/deployArtifact/{groupId}/{artifactId}/{version}")
	@Consumes("application/zip")
	@POST
	public void deployArtifact(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("version") String version, @QueryParam("classifier") String classifier, @QueryParam("type") String type, InputStream artifact) throws Exception {
		try {
			String artifactFileName = repository.getArtifactPath(groupId, artifactId, version, classifier, type);
			File file = new File(artifactFileName);
			writeFile(file, artifact);
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
	}

	@Override
	@GET
	@Path("/getfile/{path:.+}")
	@Produces("application/zip")
	public InputStream getFile(@PathParam("path") String path) throws Exception {
		try {
			return new FileInputStream(new File(repository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8")));
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
			return null;
		}
	}

	@Override
	@Path("/setfile/{path:.+}")
	@Consumes("application/zip")
	@POST
	public void setFile(@PathParam("path") String path, InputStream inputStream) throws Exception {
		try {
			File file = new File(repository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			writeFile(file, inputStream);
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
	}

	@Override
	@GET
	@Path("/file_ls/{path:.*}")
	@Produces("application/xml")
	public FileInfo ls(@PathParam("path") String path) throws Exception {
		try {
			File file = new File(repository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			return new FileInfo(file);
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
			return null;
		}
	}

	@Override
	@Path("/file_mkdir/{path:.+}")
	@Consumes("application/zip")
	@POST
	public void mkdir(@PathParam("path") String path) throws Exception {
		try {
			File file = new File(repository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			file.mkdirs();
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
		}
	}

	@Override
	@Path("/file_delete/{path:.+}")
	@Consumes("application/zip")
	@POST
	public void delete(@PathParam("path") String path) throws Exception {
		try {
			File file = new File(repository.getRepositoryRootPath() + File.separator + URLDecoder.decode(path, "UTF-8"));
			file.delete();
		} catch (Throwable throwable) {
			throwException(throwable, marshaller);
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

}
