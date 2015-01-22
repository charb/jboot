
package jboot.loader.boot.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				This elements describes all that pertains to
 * 				distribution for a project. It is primarily used for
 * 				deployment of artifacts and the site produced by the
 * 				build.
 * 			
 * 
 * <p>Java class for DistributionManagement complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DistributionManagement">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="repository" type="{http://maven.apache.org/POM/4.0.0}DeploymentRepository" minOccurs="0"/>
 *         &lt;element name="snapshotRepository" type="{http://maven.apache.org/POM/4.0.0}DeploymentRepository" minOccurs="0"/>
 *         &lt;element name="site" type="{http://maven.apache.org/POM/4.0.0}Site" minOccurs="0"/>
 *         &lt;element name="downloadUrl" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="relocation" type="{http://maven.apache.org/POM/4.0.0}Relocation" minOccurs="0"/>
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DistributionManagement", propOrder = {

})
public class DistributionManagement {

    @XmlElement
    protected DeploymentRepository repository;
    @XmlElement
    protected DeploymentRepository snapshotRepository;
    @XmlElement
    protected Site site;
    @XmlElement
    protected String downloadUrl;
    @XmlElement
    protected Relocation relocation;
    @XmlElement
    protected String status;

    /**
     * Gets the value of the repository property.
     * 
     * @return
     *     possible object is
     *     {@link DeploymentRepository }
     *     
     */
    public DeploymentRepository getRepository() {
        return repository;
    }

    /**
     * Sets the value of the repository property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeploymentRepository }
     *     
     */
    public void setRepository(DeploymentRepository value) {
        this.repository = value;
    }

    /**
     * Gets the value of the snapshotRepository property.
     * 
     * @return
     *     possible object is
     *     {@link DeploymentRepository }
     *     
     */
    public DeploymentRepository getSnapshotRepository() {
        return snapshotRepository;
    }

    /**
     * Sets the value of the snapshotRepository property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeploymentRepository }
     *     
     */
    public void setSnapshotRepository(DeploymentRepository value) {
        this.snapshotRepository = value;
    }

    /**
     * Gets the value of the site property.
     * 
     * @return
     *     possible object is
     *     {@link Site }
     *     
     */
    public Site getSite() {
        return site;
    }

    /**
     * Sets the value of the site property.
     * 
     * @param value
     *     allowed object is
     *     {@link Site }
     *     
     */
    public void setSite(Site value) {
        this.site = value;
    }

    /**
     * Gets the value of the downloadUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets the value of the downloadUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDownloadUrl(String value) {
        this.downloadUrl = value;
    }

    /**
     * Gets the value of the relocation property.
     * 
     * @return
     *     possible object is
     *     {@link Relocation }
     *     
     */
    public Relocation getRelocation() {
        return relocation;
    }

    /**
     * Sets the value of the relocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Relocation }
     *     
     */
    public void setRelocation(Relocation value) {
        this.relocation = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

}
