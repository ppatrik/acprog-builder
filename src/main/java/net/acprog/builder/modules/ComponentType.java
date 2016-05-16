package net.acprog.builder.modules;

import java.util.*;

import net.acprog.builder.components.*;
import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Element;

/**
 * Description of a component type that can be used to construct a component in
 * an Arduino program.
 */
public class ComponentType extends Module {

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Description of the view instance.
     */
    private Instance view;

    /**
     * Description of the controller instance.
     */
    private Instance controller;

    /**
     * Binding description for binding view and controller.
     */
    private Binding viewBinding;

    /**
     * Component properties.
     */
    private final Map<String, PropertyType> properties = new HashMap<String, PropertyType>();

    /**
     * Events supported by the component.
     */
    private final Map<String, Event> events = new HashMap<String, Event>();

    /**
     * List of loopers.
     */
    private final List<Looper> loopers = new ArrayList<Looper>();

    /**
     * List of method wrapper.
     */
    private final List<MethodWrapper> methodWrappers = new ArrayList<MethodWrapper>();

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public Instance getView() {
	return view;
    }

    public void setView(Instance view) {
	this.view = view;
    }

    public Instance getController() {
	return controller;
    }

    public void setController(Instance controller) {
	this.controller = controller;
    }

    public Binding getViewBinding() {
	return viewBinding;
    }

    public void setViewBinding(Binding viewBinding) {
	this.viewBinding = viewBinding;
    }

    public Map<String, PropertyType> getProperties() {
	return properties;
    }

    public Map<String, Event> getEvents() {
	return events;
    }

    public List<Looper> getLoopers() {
	return loopers;
    }

    public List<MethodWrapper> getMethodWrappers() {
	return methodWrappers;
    }

    // ---------------------------------------------------------------------------
    // XML parsing and validation
    // ---------------------------------------------------------------------------

    /**
     * Reads component type description from an xml element.
     * 
     * @param xmlComponent
     *            the xml element with description of a component.
     * 
     * @throws ConfigurationException
     *             if a component misconfiguration is detected.
     * 
     */
    protected void readConfiguration(Element xmlComponent) throws ConfigurationException {
	// Read view instance description
	view = null;
	List<Element> viewElements = XmlUtils.getChildElements(xmlComponent, "view");
	if (viewElements.size() > 1) {
	    throw new ConfigurationException("Component can expose at most one view instance.");
	}

	if (!viewElements.isEmpty()) {
	    view = new Instance(this);
	    try {
		view.readFromXml(viewElements.get(0));
	    } catch (ConfigurationException e) {
		throw new ConfigurationException("View description contains errors.", e);
	    }
	}

	// Read controller instance description
	controller = null;
	viewBinding = null;
	List<Element> controllerElements = XmlUtils.getChildElements(xmlComponent, "controller");
	if (controllerElements.size() > 1) {
	    throw new ConfigurationException("Component can have at most one controller instance.");
	}

	if (!controllerElements.isEmpty()) {
	    controller = new Instance(this);
	    try {
		Element xmlController = controllerElements.get(0);
		controller.readFromXml(xmlController);

		Element xmlViewBinding = XmlUtils.getChildElement(xmlController, "view-binding");
		if (xmlViewBinding != null) {
		    viewBinding = new Binding();
		    viewBinding.readFromXml(xmlViewBinding);
		}
	    } catch (ConfigurationException e) {
		throw new ConfigurationException("Controller description contains errors.", e);
	    }
	}

	// Read properties
	properties.clear();
	Element propertiesElement = XmlUtils.getChildElement(xmlComponent, "properties");
	if (propertiesElement != null) {
	    for (Element propertyElement : XmlUtils.getChildElements(propertiesElement, "property")) {
		String propertyName = XmlUtils.getSimplePropertyValue(propertyElement, "name", "").trim();
		if (propertyName.isEmpty()) {
		    throw new ConfigurationException("Each component property must have a non-empty name.");
		}

		if (properties.containsKey(propertyName)) {
		    throw new ConfigurationException("Duplicated property name (" + propertyName + ").");
		}

		PropertyType ptd = new PropertyType();
		try {
		    ptd.readFromXml(propertyElement);
		} catch (ConfigurationException e) {
		    throw new ConfigurationException("Description of property " + propertyName + " contains errors.", e);
		}

		properties.put(propertyName, ptd);
	    }
	}

	// Read events
	events.clear();
	Element eventsElement = XmlUtils.getChildElement(xmlComponent, "events");
	if (eventsElement != null) {
	    for (Element eventElement : XmlUtils.getChildElements(eventsElement, "event")) {
		String eventName = XmlUtils.getSimplePropertyValue(eventElement, "name", "").trim();
		if (eventName.isEmpty()) {
		    throw new ConfigurationException("Each component event must have a non-empty name.");
		}

		if (events.containsKey(eventName)) {
		    throw new ConfigurationException("Duplicated event name (" + eventName + ").");
		}

		Event ed = new Event();
		try {
		    ed.readFromXml(eventElement);
		} catch (ConfigurationException e) {
		    throw new ConfigurationException("Description of event " + eventName + " contains errors.", e);
		}

		events.put(eventName, ed);
	    }
	}

	// Read loopers
	loopers.clear();
	Element loopersElement = XmlUtils.getChildElement(xmlComponent, "loopers");
	if (loopersElement != null) {
	    for (Element looperElement : XmlUtils.getChildElements(loopersElement, "looper")) {
		Looper ld = new Looper();
		try {
		    ld.readFromXml(looperElement);
		} catch (ConfigurationException e) {
		    throw new ConfigurationException("Looper description contains errors.", e);
		}

		loopers.add(ld);
	    }
	}

	// Read method wrappers
	methodWrappers.clear();
	Element methodWrappersElement = XmlUtils.getChildElement(xmlComponent, "method-wrappers");
	if (methodWrappersElement != null) {
	    for (Element methodWrapperElement : XmlUtils.getChildElements(methodWrappersElement, "wrapper")) {
		MethodWrapper mw = new MethodWrapper();
		try {
		    mw.readFromXml(methodWrapperElement);
		} catch (ConfigurationException e) {
		    throw new ConfigurationException("Description of a method wrapper contains errors.", e);
		}

		methodWrappers.add(mw);
	    }
	}
	
	// Validate configuration
	validate();
    }

    /**
     * Validate description of the component type description.
     * 
     * @throws ConfigurationException
     *             if the description is not valid.
     */
    public void validate() throws ConfigurationException {
	// View-binding
	if ((viewBinding != null) && ((controller == null) || (controller == null))) {
	    throw new ConfigurationException(
		    "View binding can be defined only if the component a has controller and a view.");
	}

	// Validate view instance
	if (view != null) {
	    try {
		view.validate(this);
	    } catch (ConfigurationException e) {
		throw new ConfigurationException("Invalid view instance description.", e);
	    }

	    if (view.getInitMethod() != null) {
		throw new ConfigurationException("Init method for views is not allowed.");
	    }

	    if (view.getLoopMethod() != null) {
		throw new ConfigurationException("Loop method for views is not allowed.");
	    }
	}

	// Validate controller instance
	if (controller != null) {
	    try {
		controller.validate(this);
	    } catch (ConfigurationException e) {
		throw new ConfigurationException("Invalid controller instance description.", e);
	    }
	}

	// Validate loopers
	for (Looper looperDesc : loopers) {
	    looperDesc.validate(this);
	}
    }
}
