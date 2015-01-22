package jboot.repository.client.info.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ModelInfos {

	@XmlElement(name = "modelInfo")
	private List<ModelInfo> modelInfos;

	public ModelInfos() {
	}

	public ModelInfos(List<ModelInfo> modelInfos) {
		this.modelInfos = modelInfos;
	}

	public List<ModelInfo> getModelInfos() {
		if (modelInfos == null) {
			modelInfos = new ArrayList<ModelInfo>();
		}
		return modelInfos;
	}

	public void setModelInfos(List<ModelInfo> modelInfos) {
		this.modelInfos = modelInfos;
	}
}
