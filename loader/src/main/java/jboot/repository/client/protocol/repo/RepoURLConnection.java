package jboot.repository.client.protocol.repo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import jboot.repository.client.IRepository;

public class RepoURLConnection extends URLConnection {

	private IRepository repository;
	

	protected RepoURLConnection(URL url, IRepository repository) {
		super(url);
		this.repository = repository;
	}

	@Override
	public void connect() throws IOException {
		connected = true;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		connect();
		try {
			return repository.getFile(getURL().getPath());
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

}
