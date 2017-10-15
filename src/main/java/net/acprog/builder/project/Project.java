package net.acprog.builder.project;

import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.acprog.builder.components.ConfigurationException;
import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Configuration of an Arduino project.
 */
public class Project {

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Name of target compilation platform.
     */
    private String platformName;

    /**
     * Watchdog level
     */
    private int watchdogLevel = -1;

    /**
     * Configurations of components used in the project.
     */
    private final List<Component> components = new ArrayList<Component>();

    /**
     * List with library modules to be imported and used in the arduino sketch.
     */
    private final List<String> libraryImports = new ArrayList<String>();

    /**
     * User defined program events.
     */
    private final Map<String, String> programEvents = new HashMap<String, String>();

    /**
     * User defined EEPROM variables.
     */
    private final List<EepromItem> eepromItems = new ArrayList<EepromItem>();

    /**
     * Version of eeprom memory layout.
     */
    private String eepromLayoutVersion;

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public List<Component> getComponents() {
	return components;
    }

    public Map<String, String> getProgramEvents() {
	return programEvents;
    }

    public List<String> getLibraryImports() {
	return libraryImports;
    }

    public List<EepromItem> getEepromItems() {
	return eepromItems;
    }

    public String getPlatformName() {
	return platformName;
    }

    public void setPlatformName(String platformName) {
	this.platformName = platformName;
    }

    public int getWatchdogLevel() {
	return watchdogLevel;
    }

    public void setWatchdogLevel(int watchdogLevel) {
	this.watchdogLevel = watchdogLevel;
    }

    public String getEepromLayoutVersion() {
	return eepromLayoutVersion;
    }

    public void setEepromLayoutVersion(String eepromLayoutVersion) {
	this.eepromLayoutVersion = eepromLayoutVersion;
    }

    // ---------------------------------------------------------------------------
    // XML parsing
    // ---------------------------------------------------------------------------

    /**
     * Reads project configuration from an xml element.
     * 
     * @param xmlProject
     *            the xml element with project configuration.
     * 
     * @throws ConfigurationException
     *             if the project is misconfiguration.
     * 
     */
    public void readConfiguration(Element xmlProject) throws ConfigurationException {
	// Read name of target platform
	platformName = xmlProject.getAttribute("platform");

	// Read program
	readProgramConfiguration(XmlUtils.getChildElement(xmlProject, "program"));

	// Read eeprom configuration
	readEepromConfiguration(XmlUtils.getChildElement(xmlProject, "eeprom"));

	// Read components
	components.clear();
	Element xmlComponents = XmlUtils.getChildElement(xmlProject, "components");
	if (xmlComponents != null) {
	    for (Element xmlComponent : XmlUtils.getChildElements(xmlComponents, "component")) {
		Component componentConfig = new Component();
		componentConfig.readFromXml(xmlComponent);
		components.add(componentConfig);
	    }
	}
    }

    /**
     * Reads configuration of program from an xml element.
     * 
     * @param xmlProgram
     *            the xml element with program configuration.
     * @throws ConfigurationException
     *             if the program element is misconfigured.
     */
    private void readProgramConfiguration(Element xmlProgram) throws ConfigurationException {
	programEvents.clear();
	libraryImports.clear();

	if (xmlProgram == null) {
	    return;
	}

	watchdogLevel = -1;
	if (xmlProgram.hasAttribute("watchdog-level")) {
	    try {
		watchdogLevel = Integer.parseInt(xmlProgram.getAttribute("watchdog-level"));
	    } catch (Exception e) {
		throw new ConfigurationException("Watchdog level must be a non-negative integer.");
	    }

	    if (watchdogLevel < 0) {
		throw new ConfigurationException("Watchdog level must be a non-negative integer.");
	    }
	}

	Element xmlEvents = XmlUtils.getChildElement(xmlProgram, "events");
	if (xmlEvents != null) {
	    for (Element xmlEvent : XmlUtils.getChildElements(xmlEvents, "event")) {
		String eventName = xmlEvent.getAttribute("name").trim();
		String eventBinding = xmlEvent.getTextContent().trim();
		if (eventName.isEmpty()) {
		    throw new ConfigurationException("Program contains event with empty name.");
		}

		if (eventBinding.isEmpty()) {
		    throw new ConfigurationException(
			    "Program event " + eventName + " is not set to any function of procedure.");
		}

		programEvents.put(eventName, eventBinding);
	    }
	}

	Element xmlImports = XmlUtils.getChildElement(xmlProgram, "imports");
	if (xmlImports != null) {
	    for (Element xmlLibraryImport : XmlUtils.getChildElements(xmlImports, "library")) {
		String libraryModuleName = xmlLibraryImport.getTextContent().trim();
		if (libraryModuleName.isEmpty()) {
		    throw new ConfigurationException("Program contains an empty import of a library module.");
		}

		libraryImports.add(libraryModuleName);
	    }
	}
    }

    /**
     * Reads configuration of eeprom from an xml element.
     * 
     * @param xmlEeprom
     *            the xml element with program configuration.
     * @throws ConfigurationException
     *             if the program element is misconfigured.
     */
    private void readEepromConfiguration(Element xmlEeprom) throws ConfigurationException {
	eepromItems.clear();
	if (xmlEeprom == null) {
	    return;
	}

	// Read items mapped to eeprom
	for (Element xmlEepromItem : XmlUtils.getChildElements(xmlEeprom)) {
	    String elementName = xmlEepromItem.getNodeName();
	    EepromItem item = null;
	    if ("variable".equals(elementName)) {
		item = new EepromItem();
	    } else if ("array".equals(elementName)) {
		item = new EepromItem();
	    }

	    if (item != null) {
		item.readFromXml(xmlEepromItem);
		eepromItems.add(item);
	    }
	}

	// Read layout version
	eepromLayoutVersion = xmlEeprom.getAttribute("layout-version");
    }

    /**
     * Loads the project configuration from an xml file.
     * 
     * @param filename
     *            the xml file with configuration of a project.
     * @return the constructed project configuration.
     */
    public static Project loadFromFile(File xmlFile) {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setIgnoringComments(true);
	dbf.setCoalescing(true);

	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.parse(xmlFile);

	    Project result = new Project();
	    Element xmlRoot = doc.getDocumentElement();
	    if (!"project".equals(xmlRoot.getNodeName())) {
		throw new ConfigurationException(
			"Root element of a project configuration must be an element with name 'project'.");
	    }
	    result.readConfiguration(xmlRoot);
	    return result;
	} catch (Exception e) {
	    throw new ConfigurationException("Loading of project configuration failed.", e);
	}
    }

	private Element writeProgramConfiguration(Element xmlProgram) throws ConfigurationException {
		Document doc = xmlProgram.getOwnerDocument();
		boolean empty = true;
		if (xmlProgram != null) {
			if (this.getWatchdogLevel() != -1) { // todo magicka konstanta
				xmlProgram.setAttribute("watchdog-level", Integer.toString(getWatchdogLevel()));
				empty = false;
			}
			Element xmlEvents = writeEventProgramConfiguration(doc.createElement("events"));
			if (xmlEvents != null) {
				xmlProgram.appendChild(xmlEvents);
				empty = false;
			}
			Element xmlImports = writeImportsProgramConfiguration(doc.createElement("imports"));
			if (xmlImports != null) {
				xmlProgram.appendChild(xmlImports);
				empty = false;
			}
		}
		if (empty) {
			return null;
		}
		return xmlProgram;
	}

	private Element writeImportsProgramConfiguration(Element xmlImports) {
		if (getLibraryImports().size() == 0) {
			return null;
		}
		Document doc = xmlImports.getOwnerDocument();
		for (String libraryImport : getLibraryImports()) {
			Element xmlImport = doc.createElement("library");
			xmlImport.setTextContent(libraryImport);
			xmlImports.appendChild(xmlImport);
		}
		return xmlImports;
	}

	private Element writeEventProgramConfiguration(Element xmlEvents) {
		if (getProgramEvents().size() == 0) {
			return null;
		}
		Document doc = xmlEvents.getOwnerDocument();
		for (Map.Entry<String, String> entry : getProgramEvents().entrySet()) {
			Element xmlEvent = doc.createElement("event");
			xmlEvent.setAttribute("name", entry.getKey());
			xmlEvent.setTextContent(entry.getValue());
			xmlEvents.appendChild(xmlEvent);
		}
		return xmlEvents;
	}

	private Element writeEepromConfiguration(Element xmlEeproms) throws ConfigurationException {
		if (getEepromItems().size() == 0) {
			return null;
		}
		if (xmlEeproms != null) {
			Document doc = xmlEeproms.getOwnerDocument();
			for (EepromItem eepromItem : getEepromItems()) {
				Element xmlEeprom = eepromItem.writeToXml(doc);
				xmlEeproms.appendChild(xmlEeprom);
			}
		}
		return xmlEeproms;
	}

	private Element writeComponents(Element xmlComponents) {
		Document doc = xmlComponents.getOwnerDocument();
		if (xmlComponents != null) {
			for (Component component : getComponents()) {
				Element xmlComponent = doc.createElement("component");
				component.writeToXml(xmlComponent);
				xmlComponents.appendChild(xmlComponent);
			}
		}
		return xmlComponents;
	}

	public Element writeConfiguration(Element xmlProject) throws ConfigurationException {
    	Document doc = xmlProject.getOwnerDocument();
		xmlProject.setAttribute("platform", this.getPlatformName());

		Element xmlProgram = writeProgramConfiguration(doc.createElement("program"));
		if (xmlProgram != null) {
			xmlProject.appendChild(xmlProgram);
		}
		Element xmlEeprom = writeEepromConfiguration(doc.createElement("eeprom"));
		if (xmlEeprom != null) {
			xmlProject.appendChild(xmlEeprom);
		}
		xmlProject.appendChild(writeComponents(doc.createElement("components")));
		return xmlProject;
	}

	public boolean saveToFile(File xmlFile) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		dbf.setCoalescing(true);

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			// Write actual configuration to root node
			Element xmlRoot = writeConfiguration(doc.createElement("project"));
			doc.appendChild(xmlRoot);

			// Save configuration to XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(xmlFile);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, result);
			return true;
		} catch (Exception e) {
			throw new ConfigurationException("Loading of project configuration failed.", e);
		}
	}

}
