package jboot.repository.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import jboot.repository.client.info.model.RepositoryException;
import jboot.repository.client.info.model.FileInfo;
import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.model.ModelNodeInfos;
import jboot.repository.client.info.model.VersionsInfo;

public class RepositoryAccess implements IRepository {
    private static Logger   log = Logger.getLogger(RepositoryAccess.class.getName());
    private List<URL>       locations;
    private JAXBContext     jaxbContext;

    public RepositoryAccess(List<URL> locations) throws Exception {
        this.locations = locations;
        jaxbContext = JAXBContext.newInstance(ModelNodeInfo.class, ModelNodeInfos.class, VersionsInfo.class, FileInfo.class, RepositoryException.class);
    }

    @Override
    public void ping() throws Exception {
		URI resourceURI = new URI("repository/v1/ping");
		postEntityStream(resourceURI, null, "application/xml");
    }

	@Override
	public void refreshModelNodeInfos() throws Exception {
		URI resourceURI = new URI("repository/v1/refreshModelNodeInfos");
		postEntityStream(resourceURI, null, "application/xml");
	}

	@Override
	public ModelNodeInfos getModelNodeInfos(String groupId, String artifactId, String version) throws Exception {
		URI resourceURI = new URI("repository/v1/getModelNodeInfos/" + groupId + "/" + artifactId + "/" + version);
		return getEntityResource(ModelNodeInfos.class, resourceURI);
	}

	@Override
	public ModelNodeInfo getModelNodeInfo(String groupId, String artifactId, String version) throws Exception {
		URI resourceURI = new URI("repository/v1/getModelNodeInfo/" + groupId + "/" + artifactId + "/" + version);
		return getEntityResource(ModelNodeInfo.class, resourceURI);
	}

	@Override
	public VersionsInfo getVersionsInfo(String groupId, String artifactId) throws Exception {
		URI resourceURI = new URI("repository/v1/getVersionsInfo/" + groupId + "/" + artifactId);
		return getEntityResource(VersionsInfo.class, resourceURI);
	}

	@Override
	public InputStream getArtifact(String groupId, String artifactId, String version, String classifier, String type) throws Exception {
		URI resourceURI = new URI("repository/v1/getArtifact/" + groupId + "/" + artifactId + "/" + version + (classifier != null ? "?classifier=" + classifier + "&" : "?") + (type != null ? "type=" + type : ""));
		return getEntityStream(resourceURI);
	}

	@Override
	public InputStream getPom(String groupId, String artifactId, String version) throws Exception {
		URI resourceURI = new URI("repository/v1/getPom/" + groupId + "/" + artifactId + "/" + version);
		return getEntityStream(resourceURI);
	}

	@Override
	public void deployPom(String groupId, String artifactId, String version, InputStream pom) throws Exception {
		URI resourceURI = new URI("repository/v1/deployPom/" + groupId + "/" + artifactId + "/" + version);
		postEntityStream(resourceURI, pom, "application/zip");
	}

	@Override
	public void deployArtifact(String groupId, String artifactId, String version, String classifier, String type, InputStream jar) throws Exception {
		URI resourceURI = new URI("repository/v1/deployArtifact/" + groupId + "/" + artifactId + "/" + version + (classifier != null ? "?classifier=" + classifier + "&" : "?") + (type != null ? "type=" + type : ""));
		postEntityStream(resourceURI, jar, "application/zip");
	}

	@Override
	public InputStream getFile(String path) throws Exception {
		URI resourceURI = new URI("repository/v1/getfile/" + encode(path));
		return getEntityStream(resourceURI);
	}

	@Override
	public void setFile(String path, InputStream inputStream) throws Exception {
		URI resourceURI = new URI("repository/v1/setfile/" + encode(path));
		postEntityStream(resourceURI, inputStream, "application/zip");
	}

	@Override
	public FileInfo ls(String path) throws Exception {
		URI resourceURI = new URI("repository/v1/file_ls/" + encode(path));
		return getEntityResource(FileInfo.class, resourceURI);
	}

	@Override
	public void mkdir(String path) throws Exception {
		URI resourceURI = new URI("repository/v1/file_mkdir/" + encode(path));
		postEntityStream(resourceURI, null, "application/zip");
	}

	@Override
	public void delete(String path) throws Exception {
		URI resourceURI = new URI("repository/v1/file_delete/" + encode(path));
		postEntityStream(resourceURI, null, "application/zip");
	}

	private void postEntityStream(URI uri, InputStream is, String contentType) throws Exception {
		URL absoluteUrl = null;
		HttpURLConnection connection = null;
		OutputStream httpOut = null;
		if (contentType == null || contentType.trim().isEmpty()) {
			contentType = "application/zip";
		}
		for (int i=0;i<locations.size();i++) {
			URL location = locations.get(i);
			try {
				absoluteUrl = location.toURI().resolve(uri).toURL();
				connection = (HttpURLConnection)absoluteUrl.openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", contentType);
				connection.setDoOutput(true);
				connection.connect();
				if (is != null) {
	                httpOut = connection.getOutputStream();
	                copy(is, httpOut);
				}
		        if (connection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {//TODO: regerder return code
		        	unmarshal(RepositoryException.class, connection);
		        }
		        connection.getInputStream().close();
		        return;
			} catch (Throwable th) {
				if (log.isLoggable(Level.SEVERE)) {
	                log.log(Level.SEVERE, "Error posting resource " + ((absoluteUrl!=null)?absoluteUrl:uri) + " to repository " + location, th);
				}
                if (i == locations.size()-1) { //throw the last exception
                	throw new Exception(th);
                }
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		}
		throw new Exception("Cannot post resource: " + uri + " to any repository location.");
	}

	private InputStream getEntityStream(URI uri) throws Exception {
		URL absoluteUrl = null;
		HttpURLConnection connection = null;
		for (int i=0;i<locations.size();i++) {
			URL location = locations.get(i);
			try {
				absoluteUrl = location.toURI().resolve(uri).toURL();
				connection = (HttpURLConnection)absoluteUrl.openConnection();
		        if (connection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {
		        	unmarshal(RepositoryException.class, connection);
		        }
		        return connection.getInputStream();
			} catch (Throwable th) {
				if (log.isLoggable(Level.SEVERE)) {
	                log.log(Level.SEVERE, "Error getting resource " + ((absoluteUrl!=null)?absoluteUrl:uri) + " from repository " + location, th);
				}
                if (i == locations.size()-1) { //throw the last exception
                	throw new Exception(th);
                }
			}
		}
		throw new Exception("Cannot get resource: " + uri + " from any repository location.");
	}

	private <T> T getEntityResource(Class<T> resourceType, URI uri) throws Exception {
		URL absoluteUrl = null;
		HttpURLConnection connection = null;
		T entity = null;
		for (int i=0;i<locations.size();i++) {
			URL location = locations.get(i);
			try {
				absoluteUrl = location.toURI().resolve(uri).toURL();
				connection = (HttpURLConnection)absoluteUrl.openConnection();
				entity = unmarshal(resourceType, connection);
			} catch (Throwable th) {
				if (log.isLoggable(Level.SEVERE)) {
	                log.log(Level.SEVERE, "Error getting resource " + ((absoluteUrl!=null)?absoluteUrl:uri) + " from repository " + location, th);
				}
                if (i == locations.size()-1) { //throw the last exception
                	throw new Exception(th);
                }
			}
			if (entity != null) {
				return entity;
			}
		}
		throw new Exception("Cannot get resource: " + uri + " from any repository location.");
	}

    private <T> T unmarshal(Class<T> clazz, HttpURLConnection httpURLConnection) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = httpURLConnection.getInputStream();
        copy(is, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Object object = jaxbContext.createUnmarshaller().unmarshal(bais);
        if (object instanceof RepositoryException) {
        	RepositoryException customException = (RepositoryException)object;
        	customException.throwException();
        }
        return clazz.cast(object);
    }

    private void copy(InputStream in, OutputStream out) throws Exception {
		try {
			if (in != null && out != null) {
				byte[] buf = new byte[4096];
				int len = -1;
				while ((len = in.read(buf)) >= 0) {
					out.write(buf, 0, len);
				}
				out.flush();
			}
		} finally {
			if (out != null)
				out.close();
			if (in != null)
				in.close();
		}
	}

    //TODO: re-write this... why don't we just use URLEncoder directly on the url string?
    private String encode(String url) throws Exception {
        StringTokenizer stringTokenizer = new StringTokenizer(url, "/");
        String result = "";
        while (stringTokenizer.hasMoreElements()) {
            result = result + URLEncoder.encode(stringTokenizer.nextToken(), "UTF-8") + "/";
        }
        return result;
    }

}
