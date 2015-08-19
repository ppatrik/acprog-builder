package net.acprog.builder.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class with helper methods for processing xml.
 */
public final class XmlUtils {

    /**
     * Retrieves child elements of a given element with given element name.
     * 
     * @param element
     *            the element whose child elements are considered.
     * @param childName
     *            the desired name of retrieved child elements.
     * @return the list of child elements with given name.
     */
    public static List<Element> getChildElements(Element element, String childName) {
	List<Element> result = new ArrayList<Element>();

	NodeList nl = element.getChildNodes();
	for (int i = 0; i < nl.getLength(); i++) {
	    Node child = nl.item(i);
	    if (!(child instanceof Element)) {
		continue;
	    }

	    if (childName.equals(child.getNodeName())) {
		result.add((Element) child);
	    }
	}

	return result;
    }

    /**
     * Retrieves the first child element of a given element with given element
     * name.
     * 
     * @param element
     *            the element whose child elements are considered.
     * @param childName
     *            the desired name of retrieved child element.
     * @return the child element with given name or null, if such an element
     *         does not exist.
     */
    public static Element getChildElement(Element element, String childName) {
	NodeList nl = element.getChildNodes();
	for (int i = 0; i < nl.getLength(); i++) {
	    Node child = nl.item(i);
	    if (!(child instanceof Element)) {
		continue;
	    }

	    if (childName.equals(child.getNodeName())) {
		return (Element) child;
	    }
	}

	return null;
    }

    /**
     * Retrieves a value of a property with given name. The property is a child
     * element with given name. Its value is its text content.
     * 
     * @param propElement
     *            the element containing property elements.
     * @param propertyName
     *            the name of property.
     * @param defaultValue
     *            the returned value, if the property with given name is not
     *            found.
     * @return the value of the property.
     */
    public static String getSimplePropertyValue(Element propElement, String propertyName, String defaultValue) {
	NodeList nl = propElement.getChildNodes();
	for (int i = 0; i < nl.getLength(); i++) {
	    Node child = nl.item(i);
	    if (!(child instanceof Element)) {
		continue;
	    }

	    if (propertyName.equals(child.getNodeName())) {
		return ((Element) child).getTextContent();
	    }
	}

	return defaultValue;
    }

    /**
     * Private constructor disallowing instantiation of this class.
     */
    private XmlUtils() {

    }
}
