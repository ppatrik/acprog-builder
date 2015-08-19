package net.acprog.builder.components;

import net.acprog.builder.modules.ComponentType;
import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Element;

/**
 * Description of a component looper.
 */
public class Looper {

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Name of the looper method, i.e., a method of a controller instance that
     * is called within a predefined loop.
     */
    private String looperMethod;

    /**
     * Interval in milliseconds in which the looper method will be called. It
     * can be property name or a fixed value. If not provided, the method should
     * return delay for next call.
     */
    private String interval;

    /**
     * Time in milliseconds when the looper starts its activity. It can be
     * property name or a fixed value. If not provided, the value is set to 0.
     */
    private String initialDelay;

    /**
     * Binding definition for binding id of the looper. The id enables to
     * control the looper (for instance to disable or reenable the looper).
     */
    private Binding idBinding;

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public String getLooperMethod() {
	return looperMethod;
    }

    public void setLooperMethod(String looperMethod) {
	this.looperMethod = looperMethod;
    }

    public String getInterval() {
	return interval;
    }

    public void setInterval(String interval) {
	this.interval = interval;
    }

    public String getInitialDelay() {
	return initialDelay;
    }

    public void setInitialDelay(String initialDelay) {
	this.initialDelay = initialDelay;
    }

    public Binding getIdBinding() {
	return idBinding;
    }

    public void setIdBinding(Binding idBinding) {
	this.idBinding = idBinding;
    }

    // ---------------------------------------------------------------------------
    // XML parsing and validation
    // ---------------------------------------------------------------------------

    /**
     * Reads configuration of the looper from an xml element.
     * 
     * @param xmlElement
     *            the xml element.
     */
    public void readFromXml(Element xmlElement) {
	// Read method of controller that is executed in the looper.
	looperMethod = XmlUtils.getSimplePropertyValue(xmlElement, "method", "").trim();
	if (looperMethod.isEmpty()) {
	    throw new ConfigurationException("Looper must have a looper method.");
	}

	// Read loop interval
	interval = XmlUtils.getSimplePropertyValue(xmlElement, "interval", "").trim();

	// Read initial delay
	initialDelay = XmlUtils.getSimplePropertyValue(xmlElement, "initial-delay", "").trim();

	// Read id binding
	idBinding = null;
	Element xmlBinding = XmlUtils.getChildElement(xmlElement, "id-binding");
	if (xmlBinding != null) {
	    idBinding = new Binding();
	    try {
		idBinding.readFromXml(xmlBinding);
	    } catch (ConfigurationException e) {
		throw new ConfigurationException("Looper with method " + looperMethod
			+ " contains error in the id-binding.", e);
	    }
	}

    }

    /**
     * Validates configuration with respect to given component type description.
     */
    public void validate(ComponentType component) {
	// Empty interval means auto-generated interval.
	if (interval.isEmpty()) {
	    return;
	}

	// Check whether interval is a number
	try {
	    Integer.parseInt(interval);
	    return;
	} catch (Exception ignore) {

	}

	// Check whether interval is a property reference
	if (!component.getProperties().containsKey(interval)) {
	    throw new ConfigurationException("Interval of looper with method " + looperMethod
		    + " does not contain an integer value or property name.");
	}

	// Empty initial delay means 0 (i.e. a valid setting).
	if (initialDelay.isEmpty()) {
	    return;
	}

	// Check whether initial delay is a number
	try {
	    Integer.parseInt(initialDelay);
	    return;
	} catch (Exception ignore) {

	}

	// Check whether initial delay is a property reference
	if (!component.getProperties().containsKey(initialDelay)) {
	    throw new ConfigurationException("Initial delay of looper with method " + looperMethod
		    + " does not contain an integer value or property name.");
	}
    }
}
