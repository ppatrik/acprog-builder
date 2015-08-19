package net.acprog.builder.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.acprog.builder.components.ConfigurationException;
import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Description of a ACP module. A module can be a component (type) or a library.
 */
abstract public class Module {

    // ---------------------------------------------------------------------------
    // Configuration constants
    // ---------------------------------------------------------------------------

    /**
     * Path to subdirectory containing include files of the module.
     */
    public static final String INCLUDE_SUBDIR = "include/";

    /**
     * Path to subdirectory containing source files of the module.
     */
    public static final String SRC_SUBDIR = "src/";

    /**
     * Name of xml file with description of a module.
     */
    public static final String DESCRIPTION_FILE = "description.xml";

    /**
     * Name of root element for component type modules.
     */
    private static final String COMPONENT_TYPE_XML_ROOT = "component-type";

    /**
     * Name of root element for library modules.
     */
    private static final String LIBRARY_XML_ROOT = "library";

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Directory containing module resources.
     */
    private File directory;

    /**
     * Full (dot based) name of the component type.
     */
    private String name;

    /**
     * Names of modules that are required for this module.
     */
    private final List<String> requiredModules = new ArrayList<String>();

    /**
     * Header files of arduino libraries to be included in the main sketch file.
     */
    private final List<String> requiredArduinoLibIncludes = new ArrayList<String>();

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public File getDirectory() {
	return directory;
    }

    public void setDirectory(File directory) {
	this.directory = directory;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public List<String> getRequiredModules() {
	return requiredModules;
    }

    public List<String> getRequiredArduinoLibIncludes() {
	return requiredArduinoLibIncludes;
    }

    // ---------------------------------------------------------------------------
    // XML parsing and validation
    // ---------------------------------------------------------------------------

    /**
     * Loads a module configuration from an xml file.
     * 
     * @param filename
     *            the xml file with description of a module.
     * @return the constructed module description.
     * @throws ConfigurationException
     *             if loading of module description failed.
     */
    public static Module loadFromFile(File xmlFile) throws ConfigurationException {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setIgnoringComments(true);
	dbf.setCoalescing(true);

	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.parse(xmlFile);

	    Module result = null;
	    Element xmlRoot = doc.getDocumentElement();
	    String moduleType = xmlRoot.getNodeName();

	    if (COMPONENT_TYPE_XML_ROOT.equals(moduleType)) {
		result = new ComponentType();
	    } else if (LIBRARY_XML_ROOT.equals(moduleType)) {
		result = new Library();
	    }

	    if (result == null) {
		throw new ConfigurationException("Unknown module type '" + moduleType
			+ "' (root element of the module description).");
	    }

	    result.directory = xmlFile.getParentFile();
	    result.readModuleConfiguration(xmlRoot);
	    result.readConfiguration(xmlRoot);
	    return result;
	} catch (Exception e) {
	    System.err.println("Loading of a file " + xmlFile + " with module description failed:");
	    String prefix = "  ";
	    Throwable t = e;
	    while (t != null) {
		System.err.println(prefix + t.getMessage());
		t = t.getCause();
		prefix += "  ";
	    }
	    throw new ConfigurationException("Loading of description of a module from file "
		    + xmlFile.getAbsolutePath() + " failed.", e);
	}
    }

    /**
     * Reads module description common for all module types from an xml element.
     * 
     * @param xmlModule
     *            the xml element with description of a module.
     * 
     * @throws ConfigurationException
     *             if a module misconfiguration is detected.
     * 
     */
    private void readModuleConfiguration(Element xmlModule) throws ConfigurationException {
	// Read dependencies
	requiredModules.clear();
	requiredArduinoLibIncludes.clear();

	Element xmlDependencies = XmlUtils.getChildElement(xmlModule, "dependencies");
	if (xmlDependencies != null) {
	    // Required modules
	    for (Element xmlRequiredModule : XmlUtils.getChildElements(xmlDependencies, "module")) {
		String dependency = xmlRequiredModule.getTextContent().trim();
		if (!dependency.isEmpty()) {
		    requiredModules.add(dependency);
		}
	    }

	    // Required arduino libraries
	    for (Element xmlRequiredAL : XmlUtils.getChildElements(xmlDependencies, "arduino-library")) {
		String includes = xmlRequiredAL.getAttribute("include").trim();
		if (includes.isEmpty()) {
		    includes = xmlRequiredAL.getTextContent().trim() + ".h";
		}

		for (String include : includes.split(",")) {
		    include = include.trim();
		    if (!include.isEmpty()) {
			requiredArduinoLibIncludes.add(include);
		    }
		}
	    }
	}

	// Read name
	name = xmlModule.getAttribute("name").trim();
	if (name.isEmpty()) {
	    throw new ConfigurationException("Name of the module cannot be empty.");
	}
    }

    /**
     * Reads specific module description from an xml element.
     * 
     * @param xmlModule
     *            the xml element with description of a module.
     * 
     * @throws ConfigurationException
     *             if a module misconfiguration is detected.
     * 
     */
    protected abstract void readConfiguration(Element xmlModule) throws ConfigurationException;

}
