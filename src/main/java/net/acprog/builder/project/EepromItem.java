package net.acprog.builder.project;

import net.acprog.builder.components.ConfigurationException;
import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Data item stored in EEPROM memory (a simple variable or an array).
 */
public class EepromItem {

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Name of item.
     */
    private String name;

    /**
     * Type of content.
     */
    private String type;

    /**
     * Initial value of variable (if null or empty, no initial value is set).
     */
    private String value;

    /**
     * Description of the variable.
     */
    private String description;

    /**
     * Indicates whether variable value is cached to eliminate slow access to
     * EEPROM memory.
     */
    private boolean cached;

    /**
     * Length of array. Negative value for simple variables.
     */
    private int arrayLength;

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public String getName() {
	return name;
    }

    public void setName(String name) {
	if ((name == null) || (name.trim().isEmpty())) {
	    throw new ConfigurationException("Name of an EEPROM item cannot be null or an empty string.");
	}

	this.name = name;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	if ((type == null) || (type.trim().isEmpty())) {
	    throw new ConfigurationException("Type of an EEPROM item cannot be null or an empty string.");
	}

	this.type = type;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	this.value = value;
    }

    public boolean isCached() {
	return cached;
    }

    public void setCached(boolean cached) {
	this.cached = cached;
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    public boolean isArray() {
	return arrayLength >= 0;
    }

    public boolean isVariable() {
	return arrayLength < 0;
    }

    public void setLengthOfArray(int length) {
	this.arrayLength = length;
    }

    public int getLengthOfArray() {
	return arrayLength;
    }

    // ---------------------------------------------------------------------------
    // XML parsing and validation
    // ---------------------------------------------------------------------------

    /**
     * Reads configuration of a variable from an xml element.
     * 
     * @param xmlElement
     *            the xml element.
     */
    public void readFromXml(Element xmlElement) {
	try {
	    arrayLength = -1;
	    if ("array".equals(xmlElement.getNodeName())) {
		arrayLength = Integer.parseInt(xmlElement.getAttribute("length"));
		if (arrayLength < 0) {
		    throw new ConfigurationException("Length of array must be a nonnegative integer.");
		}
	    }

	    // Read name
	    name = "";
	    setName(XmlUtils.getSimplePropertyValue(xmlElement, "name", "").trim());

	    // Read type
	    setType(XmlUtils.getSimplePropertyValue(xmlElement, "type", "").trim());

	    // Read initial value
	    setValue(XmlUtils.getSimplePropertyValue(xmlElement, "value", "").trim());

	    // Read description
	    setDescription(XmlUtils.getSimplePropertyValue(xmlElement, "description", "").trim());

	    // Read cached flag
	    cached = "true".equals(xmlElement.getAttribute("cached"));
	} catch (ConfigurationException e) {
	    throw new ConfigurationException("Configuration of component " + name + " contains errors.", e);
	}
    }

    public Element writeToXml(Document doc)
    {
        Element xmlEepromItem = doc.createElement(getLengthOfArray() == -1 ? "variable" : "array");
        return xmlEepromItem;
    }

}
