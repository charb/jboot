package jboot.repository.client.info.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class FileInfo {
//	@XmlElement
//	private List<FileInfo> subFiles;
	@XmlElement
	private List<String> subFiles;
	@XmlElement
	private String fileName;
	@XmlElement
	private String filePath;
	@XmlElement
	private long fileLength;
	@XmlElement
	private boolean isWritable;
	@XmlElement
	private boolean isReadable;
	@XmlElement
	private boolean isExecutable;
	@XmlElement
	private boolean isDirectory;
	@XmlElement
	private boolean exists;

	public FileInfo() {
	}

	public FileInfo(File file) throws Exception {
		setExecutable(file.canExecute());
		setWritable(file.canWrite());
		setFileLength(file.length());
		setReadable(file.canRead());
		setDirectory(file.isDirectory());
		setFileName(file.getName());
		setFilePath(file.getCanonicalPath());
		setExists(file.exists());
		subFiles = new ArrayList<String>();
		if (file.isDirectory()) {
			for (String child : file.list()) {
				//FileInfo childFileInfo = new FileInfo(child);
				addChild(child);
			}
		}
	}

	public List<String> getChildren() {
		return subFiles;
	}

	public void addChild(String filename) {
		subFiles.add(filename);
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public long getFileLength() {
		return fileLength;
	}

	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

	public boolean isWritable() {
		return isWritable;
	}

	public void setWritable(boolean isWritable) {
		this.isWritable = isWritable;
	}

	public boolean isReadable() {
		return isReadable;
	}

	public void setReadable(boolean isReadable) {
		this.isReadable = isReadable;
	}

	public boolean isExecutable() {
		return isExecutable;
	}

	public void setExecutable(boolean isExecutable) {
		this.isExecutable = isExecutable;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public boolean isExists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}

	@Override
	public String toString() {
		return "File[Name:" + fileName + "]" + (subFiles != null ? subFiles : "");
	}
}
