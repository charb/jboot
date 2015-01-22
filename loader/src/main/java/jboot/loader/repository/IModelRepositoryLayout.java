package jboot.loader.boot.repository;

import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.node.resource.Resource;

public interface IModelRepositoryLayout {
    public String[] getVersions(String groupId, String artifactId);

    public FileResource getPomResource(String groupId, String artifactId, String version);

    public Resource getArtifactResource(String groupId, String artifactId, String version, String classifier, String type);

    public void fillAllArtifacts(ModelNode modelNode);
}
