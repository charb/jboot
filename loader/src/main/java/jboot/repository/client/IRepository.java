package jboot.repository.client;

import java.io.InputStream;

import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.model.ModelNodeInfos;
import jboot.repository.client.info.model.VersionsInfo;
import jboot.repository.client.info.model.FileInfo;

public interface IRepository {
	public void ping() throws Exception;

	public void refreshModelNodeInfos() throws Exception;

	public ModelNodeInfos getModelNodeInfos(String groupId, String artifactId, String version) throws Exception;

	public ModelNodeInfo getModelNodeInfo(String groupId, String artifactId, String version) throws Exception;

	public VersionsInfo getVersionsInfo(String groupId, String artifactId) throws Exception;

	public InputStream getArtifact(String groupId, String artifactId, String version, String classifier, String type) throws Exception;

	public InputStream getPom(String groupId, String artifactId, String version) throws Exception;

	public void deployPom(String groupId, String artifactId, String version, InputStream pom) throws Exception;

	public void deployArtifact(String groupId, String artifactId, String version, String classifier, String type, InputStream jar) throws Exception;

	public InputStream getFile(String path) throws Exception;

	public void setFile(String path, InputStream inputStream) throws Exception;

	public FileInfo ls(String path) throws Exception;

	public void mkdir(String path) throws Exception;

	public void delete(String path) throws Exception;
}
