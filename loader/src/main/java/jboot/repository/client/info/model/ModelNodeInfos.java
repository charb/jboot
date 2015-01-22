package jboot.repository.client.info.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ModelNodeInfos {
    @XmlElement
    private List<ModelNodeInfo> modelNodeInfo;

    public ModelNodeInfos() {
        modelNodeInfo = new ArrayList<ModelNodeInfo>();
    }

    public ModelNodeInfos(List<ModelNodeInfo> modelNodeInfos) {
        this.modelNodeInfo = modelNodeInfos;
    }

    public List<ModelNodeInfo> getModelNodeInfos() {
        return modelNodeInfo;
    }

    public void addModelNodeInfo(ModelNodeInfo modelNodeInfo) {
        this.modelNodeInfo.add(modelNodeInfo);
    }

    public void addModelNodeInfos(List<ModelNodeInfo> modelNodeInfos) {
        if (modelNodeInfos != null) {
            this.modelNodeInfo.addAll(modelNodeInfos);
        }
    }

    @Override
    public String toString() {
        return "[ModelNodeInfos:" + modelNodeInfo + "]";
    }
}
