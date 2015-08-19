package net.acprog.builder.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

/**
 * Helper methods for reading and writing text files.
 */
public class FileUtils {

    /**
     * Reads the content of a text file in UTF-8 encoding.
     * 
     * @param file
     *            the file.
     * @return the file content.
     */
    public static String readFile(File file) {
	try {
	    return new String(Files.readAllBytes(file.toPath()), Charset.forName("utf-8"));
	} catch (Exception e) {
	    return null;
	}
    }

    /**
     * Loads template file and applies specified replacements.
     * 
     * @param file
     *            the file with template.
     * @param replacements
     *            the replacement to be applied.
     * @return the loaded file after applying replacements.
     */
    public static String loadTemplateFile(File file, Map<String, String> replacements) {
	String fileContent;
	try {
	    fileContent = new String(Files.readAllBytes(file.toPath()), Charset.forName("utf-8"));
	} catch (Exception e) {
	    return null;
	}

	return applyTemplateReplacements(fileContent, replacements);
    }

    /**
     * Loads template file and applies specified replacements.
     * 
     * @param resource
     *            the path to a resource with template
     * @param replacements
     *            the replacement to be applied.
     * @return the loaded template resource after applying replacements.
     */
    public static String loadTemplateResource(String resource, Map<String, String> replacements) {
	String resourceContent;
	try (Scanner sc = new Scanner(FileUtils.class.getResourceAsStream(resource))) {
	    sc.useDelimiter("\\A");
	    resourceContent = sc.hasNext() ? sc.next() : "";
	} catch (Exception e) {
	    return null;
	}

	return applyTemplateReplacements(resourceContent, replacements);
    }

    /**
     * Applies replacements to a template content.
     * 
     * @param templateContent
     *            the string containing a template definition.
     * @param replacements
     *            the replacements to be applied.
     * @return the template content after applying replacements.
     */
    private static String applyTemplateReplacements(String templateContent, Map<String, String> replacements) {
	StringBuilder output = new StringBuilder();
	StringBuilder keyBuilder = null;
	for (int i = 0; i < templateContent.length(); i++) {
	    final char c = templateContent.charAt(i);
	    if (keyBuilder != null) {
		if ((('a' <= c) && (c <= 'z')) || (('A' <= c) && (c <= 'Z')) || (('0' <= c) && (c <= '9'))
			|| (c == '_')) {
		    keyBuilder.append(c);
		    continue;
		}

		String key = keyBuilder.toString();
		if (replacements.containsKey(key)) {
		    output.append(replacements.get(key));
		}
		keyBuilder = null;
	    }

	    if (c == '$') {
		keyBuilder = new StringBuilder();
		continue;
	    }

	    output.append(c);
	}

	if (keyBuilder != null) {
	    String key = keyBuilder.toString();
	    if (replacements.containsKey(key)) {
		output.append(replacements.get(key));
	    }
	}

	return output.toString();
    }

    /**
     * Save content to a file.
     * 
     * @param file
     *            the file where the content will be stored.
     * @param content
     *            the content.
     */
    public static boolean saveToFile(File file, String content) {
	// Try to create parent directory for the destination file
	File parentDir = file.getParentFile();
	if (!(parentDir.exists() && parentDir.isDirectory())) {
	    if (!parentDir.mkdirs()) {
		return false;
	    }
	}

	// Write content.
	try (Writer fw = new BufferedWriter(new FileWriter(file))) {
	    fw.write(content);
	} catch (Exception e) {
	    return false;
	}

	return true;
    }

    /**
     * Merges multiple slashes to a single slash character.
     * 
     * @param s
     *            the string to process
     * @return the string with merged slashes.
     */
    public static String mergeSlashes(String s) {
	StringBuilder sb = new StringBuilder();
	boolean wasSlash = false;
	for (int i = 0; i < s.length(); i++) {
	    if (s.charAt(i) == '/') {
		if (!wasSlash) {
		    sb.append('/');
		}
		wasSlash = true;
	    } else {
		sb.append(s.charAt(i));
		wasSlash = false;
	    }
	}

	return sb.toString();
    }

    /**
     * Merges lines to a string.
     * 
     * @param lines
     *            the lines to be merged
     * @return merged lines.
     */
    public static String mergeLines(Collection<String> lines) {
	return mergeLines(lines, null);
    }

    /**
     * Merges lines to a string.
     * 
     * @param lines
     *            the lines to be merged.
     * @param linePrefix
     *            the prefix to be added at the beginning of each line.
     * @return merged lines.
     */
    public static String mergeLines(Collection<String> lines, String linePrefix) {
	final String lineSeparator = System.getProperty("line.separator");
	StringBuilder sb = new StringBuilder();
	boolean firstLine = true;
	for (String line : lines) {
	    if (!firstLine) {
		sb.append(lineSeparator);
	    } else {
		firstLine = false;
	    }

	    if (linePrefix != null) {
		sb.append(linePrefix);
	    }

	    sb.append(line);
	}

	return sb.toString();
    }

    /**
     * Removes a directory.
     * 
     * @param directory
     *            the directory
     */
    public static void removeDirectory(File directory) {
	if (!directory.exists() || !directory.isDirectory()) {
	    return;
	}

	File[] dirFiles = directory.listFiles();
	for (File f : dirFiles) {
	    if (f.isDirectory()) {
		removeDirectory(f);
	    }

	    f.delete();
	}
    }

    /**
     * Private constructor disallowing instantiation of this class.
     */
    private FileUtils() {

    }
}
