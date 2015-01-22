package jboot.repository.client.builder;

import jboot.loader.boot.resolver.ExceptionCollector;

public interface IProjectInfoResolver {
	public ExceptionCollector resolve(String groupId, String artifactId, String version) throws Exception;
	public ExceptionCollector resolve(String groupId, String artifactId) throws Exception;
	public void clear();
}
