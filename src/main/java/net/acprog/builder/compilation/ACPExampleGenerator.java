package net.acprog.builder.compilation;

import java.util.*;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;
import net.acprog.builder.components.*;
import net.acprog.builder.modules.*;
import net.acprog.builder.project.*;
import net.acprog.builder.utils.FileUtils;

/**
 * Generator of a sketch with example of usage (and also a skeleton for arduino
 * code).
 */
public class ACPExampleGenerator extends ACPContentGenerator {

    /**
     * Separator line.
     */
    private static final String SEPARATOR_LINE = "//----------------------------------------------------------------------";

    /**
     * Generate callback event stubs for program events.
     * 
     * @param compilationContext
     *            the compilation context.
     * @param callbackCodeLines
     *            the list with code lines.
     */
    private void generateProgramCallbacks(CompilationContext compilationContext, List<String> callbackCodeLines) {
	Project project = compilationContext.getProject();

	// Add OnStart event call
	if (project.getProgramEvents().containsKey("OnStart")) {
	    String handler = project.getProgramEvents().get("OnStart");
	    callbackCodeLines.add(SEPARATOR_LINE);
	    callbackCodeLines.add("// Event callback for Program.OnStart");
	    callbackCodeLines.add("void " + handler + "() {");
	    callbackCodeLines.add("  // TODO Auto-generated callback stub");
	    callbackCodeLines.add("}");
	    callbackCodeLines.add("");
	}

	// Add OnLoop event call
	if (project.getProgramEvents().containsKey("OnLoop")) {
	    String handler = project.getProgramEvents().get("OnLoop");
	    callbackCodeLines.add(SEPARATOR_LINE);
	    callbackCodeLines.add("// Event callback for Program.OnLoop");
	    callbackCodeLines.add("void " + handler + "() {");
	    callbackCodeLines.add("  // TODO Auto-generated callback stub");
	    callbackCodeLines.add("}");
	    callbackCodeLines.add("");
	}
    }

    /**
     * Generate callback event stubs for events defined by controllers.
     * 
     * @param compilationContext
     *            the compilation context.
     * @param callbackCodeLines
     *            the list with code lines.
     */
    private void generateControllersCallbacks(CompilationContext compilationContext, List<String> callbackCodeLines) {
	Map<String, Module> projectModules = compilationContext.getProjectModules();

	for (Component component : compilationContext.getProject().getComponents()) {
	    ComponentType componentType = (ComponentType) projectModules.get(component.getType());
	    for (Map.Entry<String, Event> eventEntry : componentType.getEvents().entrySet()) {
		String nameOfEvent = eventEntry.getKey();
		Event eventDesc = eventEntry.getValue();
		if (eventDesc.getBinding() != null) {
		    String eventHandlerName = component.getEvents().get(nameOfEvent);
		    if (eventHandlerName != null) {
			callbackCodeLines.add(SEPARATOR_LINE);
			callbackCodeLines.add("// Event callback for " + component.getName() + "." + nameOfEvent);
			callbackCodeLines.add(eventDesc.generateHandlerHeader(eventHandlerName, true) + " {");
			callbackCodeLines.add("  // TODO Auto-generated callback stub");
			callbackCodeLines.add("}");
			callbackCodeLines.add("");
		    }
		}
	    }
	}
    }

    /**
     * Generate summary of available objects.
     * 
     * @param compilationContext
     *            the compilation context.
     * @param publicObjectsSummary
     *            the list with code lines.
     */
    private void generateComponentViewsSummary(CompilationContext compilationContext,
	    List<String> publicObjectsSummary) {
	// Create list of available views
	Map<String, Module> projectModules = compilationContext.getProjectModules();
	for (Component component : compilationContext.getProject().getComponents()) {
	    ComponentType componentType = (ComponentType) projectModules.get(component.getType());
	    Instance viewDescription = componentType.getView();
	    if (viewDescription != null) {
		publicObjectsSummary.add("// " + component.getName() + " (" + component.getType() + ")");
		String desc = component.getDescription();
		if (desc != null) {
		    desc = desc.trim();
		    String lines[] = desc.split("\\r?\\n");
		    for (String line : lines) {
			line = line.trim();
			if (!line.isEmpty()) {
			    publicObjectsSummary.add("//   " + line);
			}
		    }
		}
	    }
	}

	// Create list of eeprom variables
	for (EepromItem item : compilationContext.getProject().getEepromItems()) {
	    if (item.isArray()) {
		publicObjectsSummary.add("// " + item.getName() + " (eeprom array of type " + item.getType()
			+ " with length " + item.getLengthOfArray() + ")");
	    } else {
		publicObjectsSummary.add("// " + item.getName() + " (eeprom variable of type " + item.getType() + ")");
	    }

	    String desc = item.getDescription();
	    if (desc != null) {
		desc = desc.trim();
		String lines[] = desc.split("\\r?\\n");
		for (String line : lines) {
		    line = line.trim();
		    if (!line.isEmpty()) {
			publicObjectsSummary.add("//   " + line);
		    }
		}
	    }
	}
    }

    @Override
    protected void prepare(CompilationContext compilationContext, Map<String, String> output) {
	// Generate includes
	List<String> includes = new ArrayList<String>();

	// Add required arduino libraries
	for (Module module : compilationContext.getProjectModules().values()) {
	    for (String arduinoLibrary : module.getRequiredArduinoLibIncludes()) {
		includes.add("#include <" + arduinoLibrary + ">");
	    }
	}

	// Include EEPROM.h, if eeprom memory is used
	if ((Integer) compilationContext.getData().get("EepromUsage") > 0) {
	    includes.add("#include <EEPROM.h>");
	}

	// Remove include duplicates
	Set<String> uniqueIncludes = new LinkedHashSet<String>(includes);
	includes.clear();
	includes.add("#include <" + compilationContext.getSettings().getLibraryName() + ".h>");
	includes.addAll(uniqueIncludes);

	// Generate event callbacks
	List<String> callbackCodeLines = new ArrayList<String>();
	generateProgramCallbacks(compilationContext, callbackCodeLines);
	generateControllersCallbacks(compilationContext, callbackCodeLines);

	// Generate summary of available public objects
	List<String> publicObjectsSummary = new ArrayList<String>();
	generateComponentViewsSummary(compilationContext, publicObjectsSummary);
	if (!publicObjectsSummary.isEmpty()) {
	    publicObjectsSummary.add(0, SEPARATOR_LINE);
	    publicObjectsSummary.add(1, "// Summary of available objects:");
	    publicObjectsSummary.add(SEPARATOR_LINE);
	}

	// Prepare replacements for template
	output.put("includes", FileUtils.mergeLines(includes));
	output.put("objectSummary", FileUtils.mergeLines(publicObjectsSummary));
	output.put("callbacks", FileUtils.mergeLines(callbackCodeLines));
    }

    @Override
    protected void generate(CompilationContext compilationContext, Map<String, String> output) {
	generateOutputFromResourceTemplate("example.ino", output, compilationContext.getSettings().getExampleFile());
    }
}
