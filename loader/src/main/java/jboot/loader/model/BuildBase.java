
package jboot.loader.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 *  3.0.0+
 * 
 * <p>Java class for BuildBase complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BuildBase">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="defaultGoal" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resources" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="resource" type="{http://maven.apache.org/POM/4.0.0}Resource" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="testResources" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="testResource" type="{http://maven.apache.org/POM/4.0.0}Resource" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="directory" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="finalName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="filters" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="filter" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="pluginManagement" type="{http://maven.apache.org/POM/4.0.0}PluginManagement" minOccurs="0"/>
 *         &lt;element name="plugins" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="plugin" type="{http://maven.apache.org/POM/4.0.0}Plugin" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BuildBase", propOrder = {

})
public class BuildBase {

    @XmlElement
    protected String defaultGoal;
    @XmlElement
    protected Resources resources;
    @XmlElement
    protected TestResources testResources;
    @XmlElement
    protected String directory;
    @XmlElement
    protected String finalName;
    @XmlElement
    protected Filters filters;
    @XmlElement
    protected PluginManagement pluginManagement;
    @XmlElement
    protected Plugins plugins;

    /**
     * Gets the value of the defaultGoal property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultGoal() {
        return defaultGoal;
    }

    /**
     * Sets the value of the defaultGoal property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultGoal(String value) {
        this.defaultGoal = value;
    }

    /**
     * Gets the value of the resources property.
     * 
     * @return
     *     possible object is
     *     {@link Resources }
     *     
     */
    public Resources getResources() {
        return resources;
    }

    /**
     * Sets the value of the resources property.
     * 
     * @param value
     *     allowed object is
     *     {@link Resources }
     *     
     */
    public void setResources(Resources value) {
        this.resources = value;
    }

    /**
     * Gets the value of the testResources property.
     * 
     * @return
     *     possible object is
     *     {@link TestResources }
     *     
     */
    public TestResources getTestResources() {
        return testResources;
    }

    /**
     * Sets the value of the testResources property.
     * 
     * @param value
     *     allowed object is
     *     {@link TestResources }
     *     
     */
    public void setTestResources(TestResources value) {
        this.testResources = value;
    }

    /**
     * Gets the value of the directory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Sets the value of the directory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDirectory(String value) {
        this.directory = value;
    }

    /**
     * Gets the value of the finalName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFinalName() {
        return finalName;
    }

    /**
     * Sets the value of the finalName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFinalName(String value) {
        this.finalName = value;
    }

    /**
     * Gets the value of the filters property.
     * 
     * @return
     *     possible object is
     *     {@link Filters }
     *     
     */
    public Filters getFilters() {
        return filters;
    }

    /**
     * Sets the value of the filters property.
     * 
     * @param value
     *     allowed object is
     *     {@link Filters }
     *     
     */
    public void setFilters(Filters value) {
        this.filters = value;
    }

    /**
     * Gets the value of the pluginManagement property.
     * 
     * @return
     *     possible object is
     *     {@link PluginManagement }
     *     
     */
    public PluginManagement getPluginManagement() {
        return pluginManagement;
    }

    /**
     * Sets the value of the pluginManagement property.
     * 
     * @param value
     *     allowed object is
     *     {@link PluginManagement }
     *     
     */
    public void setPluginManagement(PluginManagement value) {
        this.pluginManagement = value;
    }

    /**
     * Gets the value of the plugins property.
     * 
     * @return
     *     possible object is
     *     {@link Plugins }
     *     
     */
    public Plugins getPlugins() {
        return plugins;
    }

    /**
     * Sets the value of the plugins property.
     * 
     * @param value
     *     allowed object is
     *     {@link Plugins }
     *     
     */
    public void setPlugins(Plugins value) {
        this.plugins = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="filter" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "filter"
    })
    public static class Filters {

        @XmlElement(required = true)
        protected List<String> filter;

        /**
         * Gets the value of the filter property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the filter property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getFilter().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getFilter() {
            if (filter == null) {
                filter = new ArrayList<String>();
            }
            return this.filter;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="plugin" type="{http://maven.apache.org/POM/4.0.0}Plugin" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "plugin"
    })
    public static class Plugins {

        @XmlElement(required = true)
        protected List<Plugin> plugin;

        /**
         * Gets the value of the plugin property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the plugin property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getPlugin().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Plugin }
         * 
         * 
         */
        public List<Plugin> getPlugin() {
            if (plugin == null) {
                plugin = new ArrayList<Plugin>();
            }
            return this.plugin;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="resource" type="{http://maven.apache.org/POM/4.0.0}Resource" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "resource"
    })
    public static class Resources {

        @XmlElement(required = true)
        protected List<Resource> resource;

        /**
         * Gets the value of the resource property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the resource property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getResource().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource }
         * 
         * 
         */
        public List<Resource> getResource() {
            if (resource == null) {
                resource = new ArrayList<Resource>();
            }
            return this.resource;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="testResource" type="{http://maven.apache.org/POM/4.0.0}Resource" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "testResource"
    })
    public static class TestResources {

        @XmlElement(required = true)
        protected List<Resource> testResource;

        /**
         * Gets the value of the testResource property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the testResource property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getTestResource().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource }
         * 
         * 
         */
        public List<Resource> getTestResource() {
            if (testResource == null) {
                testResource = new ArrayList<Resource>();
            }
            return this.testResource;
        }

    }

}
