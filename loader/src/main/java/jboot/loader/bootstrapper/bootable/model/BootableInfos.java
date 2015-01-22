package jboot.loader.bootstrapper.bootable.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "bootables")
public class BootableInfos {

	@XmlElementRef
	List<BootableInfo> bootableInfos;

	public List<BootableInfo> getBootableInfos() {
		if (bootableInfos == null) {
			setBootableInfos(new ArrayList<BootableInfo>());
		}
		return bootableInfos;
	}

	public void setBootableInfos(List<BootableInfo> bootableInfos) {
		this.bootableInfos = bootableInfos;
	}

}
