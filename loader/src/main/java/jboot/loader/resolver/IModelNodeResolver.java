package jboot.loader.boot.resolver;

public interface IModelNodeResolver {
    public ExceptionCollector resolve(String groupId, String artifactId, String version) throws Exception;
}
