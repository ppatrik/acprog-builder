package net.acprog.builder.components;

import org.w3c.dom.Element;

/**
 * Description of a binding.
 */
public class Binding {

    // ---------------------------------------------------------------------------
    // Binding type
    // ---------------------------------------------------------------------------

    /**
     * Binding type.
     */
    public static enum BindingType {
	/**
	 * Binding by assigning a value to an attribute.
	 */
	ATTRIBUTE,

	/**
	 * Binding by invoking a method.
	 */
	METHOD
    }

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Binding type.
     */
    private BindingType type;

    /**
     * Detailed description (e.g. name) of a binding target (attribute, method)
     * used by this binding.
     */
    private String target;

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public BindingType getType() {
	return type;
    }

    public void setType(BindingType type) {
	this.type = type;
    }

    public String getTarget() {
	return target;
    }

    public void setTarget(String target) {
	this.target = target;
    }

    // ---------------------------------------------------------------------------
    // XML parsing
    // ---------------------------------------------------------------------------

    /**
     * Reads configuration of the binding from an xml element.
     * 
     * @param xmlElement
     *            the xml element.
     */
    public void readFromXml(Element xmlElement) {
	String bindingTypeCode = xmlElement.getAttribute("type").trim();
	try {
	    setType(BindingType.valueOf(bindingTypeCode.toUpperCase()));
	} catch (Exception e) {
	    throw new ConfigurationException("Unknown binding type: " + bindingTypeCode);
	}

	String targetValue = xmlElement.getTextContent().trim();
	if (targetValue.isEmpty()) {
	    throw new ConfigurationException("Binding target cannot be empty.");
	}
	setTarget(targetValue);
    }

    // ---------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------

    /**
     * Generates command that binds a given value to a given object using
     * predefined way.
     * 
     * @param objectName
     *            the name of an object to which the value is bound.
     * @param value
     *            the value to be bound.
     * @return the command that executes the binding.
     */
    public String generateBindingCommand(String objectName, String value) {
	if (type == BindingType.METHOD) {
	    return objectName + "." + target + "(" + value + ");";
	}

	if (type == BindingType.ATTRIBUTE) {
	    return objectName + "." + target + " = " + value + ";";
	}

	return "";
    }
}
