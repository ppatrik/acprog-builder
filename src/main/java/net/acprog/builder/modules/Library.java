package net.acprog.builder.modules;

import java.util.*;

import net.acprog.builder.components.ConfigurationException;
import net.acprog.builder.utils.XmlUtils;

import org.w3c.dom.Element;

/**
 * Description of a library module.
 */
public class Library extends Module {

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * List with header files to be included in case of using this library.
     */
    private final List<String> includes = new ArrayList<String>();

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public List<String> getIncludes() {
	return includes;
    }

    // ---------------------------------------------------------------------------
    // XML parsing and validation
    // ---------------------------------------------------------------------------

    /**
     * Reads library description from an xml element.
     * 
     * @param xmlLibrary
     *            the xml element with description of a library module.
     * 
     * @throws ConfigurationException
     *             if a library misconfiguration is detected.
     * 
     */
    protected void readConfiguration(Element xmlLibrary) throws ConfigurationException {
	// Read includes required to use this library.
	includes.clear();
	Element xmlIncludes = XmlUtils.getChildElement(xmlLibrary, "includes");
	if (xmlIncludes != null) {
	    for (Element xmlInclude : XmlUtils.getChildElements(xmlIncludes, "include")) {
		String include = xmlInclude.getTextContent().trim();
		if (!include.isEmpty()) {
		    includes.add(include);
		}
	    }
	}
    }
}
