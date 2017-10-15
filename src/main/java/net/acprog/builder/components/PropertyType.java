package net.acprog.builder.components;

import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Element;

/**
 * Description of a property type.
 */
public class PropertyType {

    // ---------------------------------------------------------------------------
    // ValueType
    // ---------------------------------------------------------------------------

    /**
     * Type of predefined property value.
     */
    public static enum ValueType {
	/**
	 * Predefined value is a fixed property value.
	 */
	FIXED,

	/**
	 * Predefined value is a default property value.
	 */
	DEFAULT
    }

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Type of a property.
     */
    private String type;

    /**
     * Predefined property value.
     */
    private String value;

    /**
     * Type of predefined value.
     */
    private ValueType valueType;

    /**
     * Description of a property binding.
     */
    private Binding binding;

    private String description;

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	this.value = value;
    }

    public ValueType getValueType() {
	return valueType;
    }

    public void setValueType(ValueType valueType) {
	this.valueType = valueType;
    }

    public Binding getBinding() {
	return binding;
    }

    public void setBinding(Binding binding) {
	this.binding = binding;
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// ---------------------------------------------------------------------------
    // XML parsing
    // ---------------------------------------------------------------------------

    /**
     * Reads configuration of the property type from an xml element.
     * 
     * @param xmlElement
     *            the xml element.
     */
    public void readFromXml(Element xmlElement) {
	String propertyName = XmlUtils.getSimplePropertyValue(xmlElement, "name", "").trim();

	// Read type
	type = XmlUtils.getSimplePropertyValue(xmlElement, "type", "").trim();

	// Read value
	value = null;
	valueType = null;
	Element xmlValue = XmlUtils.getChildElement(xmlElement, "value");
	if (xmlValue != null) {
	    value = xmlValue.getTextContent();
	    String valueTypeCode = xmlValue.getAttribute("type").trim();
	    if (valueTypeCode.isEmpty()) {
		valueTypeCode = ValueType.DEFAULT.toString();
	    }

	    try {
		valueType = ValueType.valueOf(valueTypeCode.toUpperCase());
	    } catch (Exception e) {
		throw new ConfigurationException("Unknown value type of property " + propertyName + ": "
			+ valueTypeCode);
	    }
	}

	// Read binding
	binding = null;
	Element xmlBinding = XmlUtils.getChildElement(xmlElement, "binding");
	if (xmlBinding != null) {
	    binding = new Binding();
	    try {
		binding.readFromXml(xmlBinding);
	    } catch (ConfigurationException e) {
		throw new ConfigurationException("Binding of property " + propertyName + " contains errors.", e);
	    }
	}

	// Read description
	description = XmlUtils.getSimplePropertyValue(xmlElement, "description", "");
    }

    // ---------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------

    /**
     * Returns effective value of the property, i.e., taking into account
     * predefined property values.
     * 
     * @param configuredValue
     *            the value set by configuration.
     * @return the effective value.
     */
    public String getEffectiveValue(String configuredValue) {
	// If the value is fixed, we use given fixed property value
	if (ValueType.FIXED == valueType) {
	    configuredValue = value;
	}

	// If the configured value is missing and there is default value, we use
	// default value
	if ((configuredValue == null) && (valueType == ValueType.DEFAULT)) {
	    configuredValue = value;
	}

	return configuredValue;
    }

    // ---------------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------------

    /**
     * Tests whether string representation of a value satisfies all restriction
     * defined in the property type.
     * 
     * @param value
     *            the property value.
     * @return true, if the value satisfies all restrictions.
     */
    public boolean checkRestrictions(String value) {
	return true;
    }
}
