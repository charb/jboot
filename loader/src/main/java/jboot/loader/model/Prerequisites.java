package jboot.loader.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * 				Describes the prerequisites a project can have.
 * 			
 * 
 * <p>Java class for Prerequisites complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Prerequisites">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="maven" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Prerequisites", propOrder = {})
public class Prerequisites {
    @XmlElement(defaultValue = "2.0")
    protected String maven;


    /**
     * Gets the value of the maven property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMaven() {
        return maven;
    }


    /**
     * Sets the value of the maven property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMaven(String value) {
        this.maven = value;
    }
}
