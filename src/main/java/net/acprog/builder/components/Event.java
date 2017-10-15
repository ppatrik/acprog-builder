package net.acprog.builder.components;

import java.util.*;

import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Element;

/**
 * Description of an event provided by a component.
 */
public class Event {

    // ---------------------------------------------------------------------------
    // Parameter type
    // ---------------------------------------------------------------------------

    /**
     * Description of a parameter of an event.
     */
    public static class ParameterType {
	/**
	 * Type of parameter (int, long, etc.)
	 */
	private final String type;

	/**
	 * Suggested name for the parameter.
	 */
	private final String name;

	public String getType() {
	    return type;
	}

	public String getName() {
	    return name;
	}

	/**
	 * Constructs new parameter description of an event.
	 * 
	 * @param type
	 *            the type of parameter
	 * @param name
	 *            the suggester name of parameter
	 */
	public ParameterType(String type, String name) {
	    if ((type == null) || (type.trim().isEmpty())) {
		throw new ConfigurationException("Parameter type cannot by empty.");
	    }

	    this.type = type;
	    this.name = name;
	}
    }

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Binding type.
     */
    private Binding binding;

    /**
     * Type of result returned by event handler.
     */
    private String resultType;

	private String description;

    /**
     * Ordered list of parameters of the event.
     */
    private final List<ParameterType> parameters = new ArrayList<ParameterType>();

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public Binding getBinding() {
	return binding;
    }

    public String getResultType() {
	return resultType;
    }

    public List<ParameterType> getParameters() {
	return parameters;
    }

	public String getDescription() {
		return description;
	}

	// ---------------------------------------------------------------------------
    // XML parsing
    // ---------------------------------------------------------------------------

    /**
     * Reads configuration of the event from an xml element.
     * 
     * @param xmlElement
     *            the xml element.
     */
    public void readFromXml(Element xmlElement) {
	String eventName = XmlUtils.getSimplePropertyValue(xmlElement, "name", "").trim();

	Element xmlBinding = XmlUtils.getChildElement(xmlElement, "binding");
	if (xmlBinding == null) {
	    throw new ConfigurationException("Event " + eventName + " has undefined binding.");
	}

	binding = new Binding();
	try {
	    binding.readFromXml(xmlBinding);
	} catch (ConfigurationException e) {
	    throw new ConfigurationException("Binding of event " + eventName + " contains errors.", e);
	}

	Element xmlParameters = XmlUtils.getChildElement(xmlElement, "parameters");
	if (xmlParameters != null) {
	    for (Element xmlParameter : XmlUtils.getChildElements(xmlParameters, "parameter")) {
		String parameterType = xmlParameter.getTextContent().trim();
		if (parameterType.isEmpty()) {
		    throw new ConfigurationException("Empty parameter type in event " + eventName + ".");
		}

		parameters.add(new ParameterType(parameterType, xmlParameter.getAttribute("name").trim()));
	    }
	}

	resultType = null;
	Element xmlResultType = XmlUtils.getChildElement(xmlElement, "result");
	if (xmlResultType != null) {
	    resultType = xmlResultType.getTextContent().trim();
	}

	// Read description
	description = XmlUtils.getSimplePropertyValue(xmlElement, "description", "");
    }

    // ---------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------

    /**
     * Generates header of handler.
     * 
     * @param handlerName
     *            the name of event handler.
     * @param generateParameterNames
     *            true, if the names of parameters are generated, false
     *            otherwise.
     * @return the subroutine header.
     */
    public String generateHandlerHeader(String handlerName, boolean generateParameterNames) {
	StringBuilder sb = new StringBuilder();
	if ((resultType == null) || (resultType.isEmpty())) {
	    sb.append("void");
	} else {
	    sb.append(resultType.trim());
	}

	sb.append(" ");
	sb.append(handlerName);
	sb.append("(");
	boolean first = true;
	for (ParameterType parameter : parameters) {
	    if (!first) {
		sb.append(", ");
	    } else {
		first = false;
	    }

	    sb.append(parameter.getType().trim());
	    if (generateParameterNames) {
		sb.append(" " + parameter.getName());
	    }
	}
	sb.append(")");
	return sb.toString();
    }
}
