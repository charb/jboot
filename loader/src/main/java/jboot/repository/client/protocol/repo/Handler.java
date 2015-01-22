package jboot.repository.client.protocol.repo;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jboot.repository.client.IRepository;

public class Handler extends URLStreamHandler {
	private Pattern regexPattern;
	private IRepository repository;

	public Handler(IRepository repository) {
		super();
		regexPattern = Pattern.compile("[\\.\\w]+(/[\\.\\w]+)*/?");
		this.repository = repository;
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return new RepoURLConnection(u, repository);
	}

	@Override
	protected void parseURL(URL u, String spec, int start, int limit) {
		String path = spec.substring(start);
		Matcher m = regexPattern.matcher(path);
		if (m.matches()) {
			setURL(u, u.getProtocol(), null, 0, null, null, path, null, null);
		} else {
			throw new IllegalArgumentException("Invalid repo: url. " + spec);
		}
	}
}
