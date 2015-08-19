package net.acprog.builder.project;

import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
    private final List<EepromVariable> eepromVariables = new ArrayList<EepromVariable>();

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

    public List<EepromVariable> getEepromVariables() {
	return eepromVariables;
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
		    throw new ConfigurationException("Program event " + eventName
			    + " is not set to any function of procedure.");
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
	eepromVariables.clear();
	if (xmlEeprom == null) {
	    return;
	}

	// Read variables mapped to eeprom
	for (Element xmlVariable : XmlUtils.getChildElements(xmlEeprom, "variable")) {
	    EepromVariable variable = new EepromVariable();
	    variable.readFromXml(xmlVariable);
	    eepromVariables.add(variable);
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
}
