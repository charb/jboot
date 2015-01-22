package jboot.loader.boot.resolver;

public interface IProjectResolver {
    public ExceptionCollector resolve(String groupId, String artifactId, String version) throws Exception;
    public ExceptionCollector resolve(String groupId, String artifactId) throws Exception;
}
