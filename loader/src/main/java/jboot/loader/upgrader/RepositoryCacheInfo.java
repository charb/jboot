package jboot.loader.upgrader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import jboot.repository.client.info.ModelNodeInfoResult;
import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.model.ModelNodeInfos;

public class RepositoryCacheInfo extends ModelNodeInfoResult {
	private static Logger log = Logger.getLogger("jboot.upgrader");
	public static final String REPO_INFOS_XML = "repo-infos.xml";

	private Marshaller marshaller;
	private Unmarshaller unmarshaller;
	private boolean bIsDirtyCache;

	public RepositoryCacheInfo() throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(ModelNodeInfo.class, ModelNodeInfos.class);
		unmarshaller = ctx.createUnmarshaller();
		marshaller = ctx.createMarshaller();
	}

	@Override
	public void addModelNodeInfo(ModelNodeInfo modelNodeInfo) {
		bIsDirtyCache = true;
		super.addModelNodeInfo(modelNodeInfo);
	}

	@Override
	public void addModelNodeInfos(Collection<ModelNodeInfo> modelNodeInfos) {
		bIsDirtyCache = true;
		super.addModelNodeInfos(modelNodeInfos);
	}

	private boolean isDirtyCache() {
		return bIsDirtyCache;
	}

	@Override
	public void clear() {
		bIsDirtyCache = false;
		super.clear();
	}

	public void read(File file) {
		this.clear();
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(file));
				ModelNodeInfos modelNodeInfos = (ModelNodeInfos)unmarshaller.unmarshal(in);
				if (modelNodeInfos != null) {
					super.addModelNodeInfos(modelNodeInfos.getModelNodeInfos());
				}
			} catch (Exception ex) {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "An exception occured while loading the repository cache file at: " + file.getAbsolutePath(), ex);
				}
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (Exception ex) {
						if (log.isLoggable(Level.WARNING)) {
							log.log(Level.WARNING, "An exception occured while closing the input stream to: " + file.getAbsolutePath(), ex);
						}						
					}
				}
			}
		}
	}

	public void save(File file) {
		if (isDirtyCache()) {
			OutputStream out = null;
			try {
				ModelNodeInfos modelNodeInfos = new ModelNodeInfos(Arrays.asList(this.getModelNodeInfos().values().toArray(new ModelNodeInfo[0])));
				if (!file.exists()) {
					File parentDir = file.getParentFile();
					if (parentDir != null) {
						parentDir.mkdirs();
					}
					file.createNewFile();
				}
				out = new BufferedOutputStream(new FileOutputStream(file));
				marshaller.marshal(modelNodeInfos, out);
				out.flush();
			} catch (Exception ex) {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "An exception occured while saving the repository cache file at: " + file.getAbsolutePath(), ex);
				}
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception ex) {
						if (log.isLoggable(Level.WARNING)) {
							log.log(Level.WARNING, "An exception occured while closing the output stream to: " + file.getAbsolutePath(), ex);
						}						
					}
				}
			}
		}
	}

}
