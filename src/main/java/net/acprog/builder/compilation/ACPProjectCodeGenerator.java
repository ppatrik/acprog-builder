package net.acprog.builder.compilation;

import java.io.File;
import java.util.*;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;
import net.acprog.builder.compilation.CompilationSettings.LooperStrategy;
import net.acprog.builder.components.*;
import net.acprog.builder.modules.ComponentType;
import net.acprog.builder.modules.Module;
import net.acprog.builder.platform.Platform;
import net.acprog.builder.project.Component;
import net.acprog.builder.project.EepromVariable;
import net.acprog.builder.project.Project;
import net.acprog.builder.utils.FileUtils;

/**
 * Generator of ACP autogenerated source code.
 */
public class ACPProjectCodeGenerator extends ACPContentGenerator {

    /**
     * Default name of the acp file with core source codes.
     */
    public static final String ACP_CORE_SOURCE_FILE = "core.cpp";

    /**
     * Basic indent for autogenerated source code.
     */
    private static final String BASIC_INDENT = "  ";

    /**
     * Prefix of name of a handler for a looper.
     */
    private static final String LOOPER_HANDLER_PREFIX = "looper_handler_";

    // ---------------------------------------------------------------------------
    // Data containers
    // ---------------------------------------------------------------------------

    /**
     * Details about a looper to be used in the project.
     */
    private static class LooperRecord {
	/**
	 * Name of component for which this looper works.
	 */
	String fullComponentName;

	/**
	 * Name of the looper method.
	 */
	String looperMethod;

	/**
	 * Looper interval.
	 */
	int interval;

	/**
	 * Initial delay of looper
	 */
	int initialDelay;
    }

    /**
     * Context in which the generation process is executed.
     */
    private static class Context {
	/**
	 * Compilation context
	 */
	final CompilationContext compilationContext;

	/**
	 * List of user defined event handlers referenced from the generated
	 * code.
	 */
	final List<String> eventHandlers = new ArrayList<String>();

	/**
	 * List of header files to be included.
	 */
	final List<String> includes = new ArrayList<String>();

	/**
	 * Lines of code creating objects accessible for user defined code.
	 */
	final List<String> publicObjects = new ArrayList<String>();

	/**
	 * Lines of code creating objects that are not accessible for user
	 * defined code.
	 */
	final List<String> privateObjects = new ArrayList<String>();

	/**
	 * Lines of code to be generated in the setup subroutine.
	 */
	final List<String> setupCode = new ArrayList<String>();

	/**
	 * Lines of code to be generated in the loop subroutine.
	 */
	final List<String> loopCode = new ArrayList<String>();

	/**
	 * List of loopers used in the project.
	 */
	final List<LooperRecord> loopers = new ArrayList<LooperRecord>();

	/**
	 * Lines of code defining method wrappers
	 */
	final List<String> methodWrappersCode = new ArrayList<String>();

	/**
	 * Lines of code defining eeprom layoyt.
	 */
	final List<String> eepromVarDefinitions = new ArrayList<String>();

	/**
	 * Lines of code initializing the eeprom variables
	 */
	final List<String> eepromInitializationCode = new ArrayList<String>();

	/**
	 * Namespace for non-public objects.
	 */
	final String privateNamespace;

	/**
	 * Construct code generation context.
	 * 
	 * @param compilationContext
	 *            the compilation context.
	 */
	Context(CompilationContext compilationContext) {
	    this.compilationContext = compilationContext;
	    privateNamespace = (String) compilationContext.getData().get("PrivateNamespace");
	}
    }

    /**
     * Generates subroutines and data structures for implementing loopers using
     * arrays.
     * 
     * @param context
     *            the context
     * @return the generated source code
     */
    private String generateArrayBasedLoopersCode(Context context) {
	final List<LooperRecord> looperRecords = context.loopers;
	if (looperRecords.isEmpty()) {
	    return "";
	}

	// Looper handlers
	List<String> looperHandlersCode = new ArrayList<String>();
	for (int i = 0; i < looperRecords.size(); i++) {
	    if (i != 0) {
		looperHandlersCode.add("");
	    }

	    LooperRecord lr = looperRecords.get(i);
	    looperHandlersCode.add("unsigned long " + LOOPER_HANDLER_PREFIX + i + "() {");
	    if (lr.interval >= 0) {
		looperHandlersCode.add(BASIC_INDENT + lr.fullComponentName + "." + lr.looperMethod + "();");
		looperHandlersCode.add(BASIC_INDENT + "return " + lr.interval + ";");
	    } else {
		looperHandlersCode.add(BASIC_INDENT + "return " + lr.fullComponentName + "." + lr.looperMethod + "();");
	    }
	    looperHandlersCode.add("}");
	}

	// Loopers - initialization of array
	List<String> loopersInit = new ArrayList<String>();
	for (int i = 0; i < looperRecords.size(); i++) {
	    String line = "{" + looperRecords.get(i).initialDelay + ", ENABLED, " + LOOPER_HANDLER_PREFIX + i + "}";
	    if (i != looperRecords.size() - 1) {
		line = line + ",";
	    }
	    loopersInit.add(line);
	}

	// Sort looopers indices by initial delay
	Integer[] idxOrder = new Integer[looperRecords.size()];
	for (int i = 0; i < idxOrder.length; i++) {
	    idxOrder[i] = i;
	}

	Arrays.sort(idxOrder, new Comparator<Integer>() {
	    @Override
	    public int compare(Integer idx1, Integer idx2) {
		return Integer.compare(looperRecords.get(idx1).initialDelay, looperRecords.get(idx2).initialDelay);
	    }
	});

	// pqInit - initialization of array
	StringBuilder pqInit = new StringBuilder();
	pqInit.append("{");
	for (int i = 0; i < idxOrder.length; i++) {
	    if (i != 0) {
		pqInit.append(", ");
	    }

	    pqInit.append("loopers + " + idxOrder[i]);
	}
	pqInit.append("}");

	Map<String, String> replacements = new HashMap<String, String>();
	replacements.put("privateNamespace", context.privateNamespace);
	replacements.put("numberOfLoopers", Integer.toString(looperRecords.size()));
	replacements.put("looperHandlers", FileUtils.mergeLines(looperHandlersCode));
	replacements.put("loopersInit", FileUtils.mergeLines(loopersInit, BASIC_INDENT));
	replacements.put("pqInit", pqInit.toString());

	String templateResource = ACPCompiler.TEMPLATES_RESOURCE_DIR + "acp_core_loopers_array.cpp";
	String result = FileUtils.loadTemplateResource(templateResource, replacements);
	if (result == null) {
	    throw new CompilationException("Unavailable resource file: " + templateResource);
	}

	context.loopCode.add("// Process loopers");
	context.loopCode.add(context.privateNamespace + "::processLoopers();");

	return result;
    }

    /**
     * Generates source code that invokes user defined event handlers for
     * program events.
     * 
     * @param context
     *            the context.
     */
    private void generateProgramEvents(Context context) {
	Project project = context.compilationContext.getProject();

	// Add OnStart event call
	if (project.getProgramEvents().containsKey("OnStart")) {
	    String handler = project.getProgramEvents().get("OnStart");
	    context.setupCode.add("// Call of the OnStart event");
	    context.setupCode.add(handler + "();");
	    context.eventHandlers.add("void " + handler + "()");
	}

	// Add OnLoop event call
	if (project.getProgramEvents().containsKey("OnLoop")) {
	    String handler = project.getProgramEvents().get("OnLoop");
	    context.loopCode.add("// Call of the OnLoop event");
	    context.loopCode.add(handler + "();");
	    context.eventHandlers.add("void " + handler + "()");
	}
    }

    /**
     * Generates source code that invokes user defined event handlers for
     * program events.
     * 
     * @param context
     *            the context.
     */
    private void generateComponentObjects(Context context) {
	Project project = context.compilationContext.getProject();
	Map<String, Module> projectModules = context.compilationContext.getProjectModules();
	Platform platform = context.compilationContext.getPlatform();

	int controllerIdGenerator = 0;
	int methodWrapperIdGenerator = 0;

	// Generate code for user defined components
	for (Component component : project.getComponents()) {
	    ComponentType componentType = (ComponentType) projectModules.get(component.getType());
	    Instance viewDescription = componentType.getView();
	    Instance controllerDescription = componentType.getController();
	    String componentIncludePrefix = componentType.getName().replace('.', '/') + "/";

	    // Generate controller (if necessary)
	    if (controllerDescription != null) {
		for (String include : controllerDescription.getIncludes()) {
		    context.includes.add("#include <" + componentIncludePrefix + include + ">");
		}

		String controllerName = "controller_" + controllerIdGenerator;
		String fullControllerName = context.privateNamespace + "::" + controllerName;
		controllerIdGenerator++;

		// Set autogenerated controller name
		component.getAutogeneratedProperties().put("controller", fullControllerName);

		context.setupCode.add("// Controller for " + component.getName());
		boolean controllerBindingGenerated = false;

		// Set looper identifiers of controller
		for (Looper looper : componentType.getLoopers()) {
		    // Bind looper id (if necessary)
		    Binding idBinding = looper.getIdBinding();
		    if (idBinding != null) {
			context.setupCode.add(idBinding.generateBindingCommand(fullControllerName,
				Integer.toString(context.loopers.size())));
			controllerBindingGenerated = true;
		    }

		    LooperRecord looperRecord = new LooperRecord();
		    looperRecord.fullComponentName = fullControllerName;
		    looperRecord.looperMethod = looper.getLooperMethod();

		    try {
			looperRecord.interval = readIntegerValueOrProperty(looper.getInterval(), component,
				componentType, -1);
		    } catch (Exception e) {
			throw new CompilationException("Invalid value of interval of looper of the component '"
				+ component.getName() + "'.");
		    }

		    try {
			looperRecord.initialDelay = Math.max(
				readIntegerValueOrProperty(looper.getInitialDelay(), component, componentType, -1), 0);
		    } catch (Exception e) {
			throw new CompilationException("Invalid value of initial delay of looper of the component '"
				+ component.getName() + "'.");
		    }

		    context.loopers.add(looperRecord);
		}

		// Set controller properties using bindings
		for (Map.Entry<String, PropertyType> propEntry : componentType.getProperties().entrySet()) {
		    String propName = propEntry.getKey();
		    PropertyType propType = propEntry.getValue();
		    Binding binding = propType.getBinding();
		    if (binding != null) {
			String effectivePropertyValue = propType.getEffectiveValue(component.getProperties().get(
				propName));
			String escapedPropertyValue = platform.escapeValue(propEntry.getValue().getType(),
				effectivePropertyValue);
			if (escapedPropertyValue == null) {
			    throw new CompilationException("Invalid or undefined value of property "
				    + propEntry.getKey() + " of component " + component.getName() + ".");
			}

			context.setupCode.add(binding.generateBindingCommand(fullControllerName, escapedPropertyValue));
			controllerBindingGenerated = true;
		    }
		}

		// Set event handlers for controller
		for (Map.Entry<String, Event> eventEntry : componentType.getEvents().entrySet()) {
		    String nameOfEvent = eventEntry.getKey();
		    Event eventDesc = eventEntry.getValue();
		    Binding binding = eventDesc.getBinding();
		    if (binding != null) {
			String eventHandlerName = component.getEvents().get(nameOfEvent);
			if (eventHandlerName != null) {
			    context.setupCode.add(binding.generateBindingCommand(fullControllerName, eventHandlerName));
			    // Export extern for event handler
			    context.eventHandlers.add(eventDesc.generateHandlerHeader(eventHandlerName, false));
			} else {
			    context.setupCode.add(binding.generateBindingCommand(fullControllerName, "NULL"));
			}
			controllerBindingGenerated = true;
		    }
		}

		// Generate and bind method wrappers for controller
		for (MethodWrapper methodWrapper : componentType.getMethodWrappers()) {
		    String wrappingFunction = "method_wrapper_" + methodWrapperIdGenerator;
		    methodWrapperIdGenerator++;

		    // Generate code of wrapping function
		    context.methodWrappersCode.add(methodWrapper.generateWrappingFunctionHeader(wrappingFunction, true)
			    + " {");

		    String invocationCommand = methodWrapper.generateInvocation(fullControllerName + "."
			    + methodWrapper.getWrappedMethod());

		    String resultType = methodWrapper.getResultType();
		    if ((resultType == null) || "void".equals(resultType) || resultType.isEmpty()) {
			context.methodWrappersCode.add(BASIC_INDENT + invocationCommand + ";");
		    } else {
			context.methodWrappersCode.add(BASIC_INDENT + "return " + invocationCommand + ";");
		    }

		    context.methodWrappersCode.add("}");
		    context.methodWrappersCode.add("");

		    // Set autogenerated-property
		    String autogeneratedPropertyName = methodWrapper.getAutogeneratedPropertyName();
		    if ((autogeneratedPropertyName != null) && (!autogeneratedPropertyName.trim().isEmpty())) {
			component.getAutogeneratedProperties().put(autogeneratedPropertyName,
				context.privateNamespace + "::" + wrappingFunction);
		    }

		    // Generate binding
		    if (methodWrapper.getBinding() != null) {
			context.setupCode.add(methodWrapper.getBinding().generateBindingCommand(fullControllerName,
				wrappingFunction));
		    }
		}

		// Bind view and controller
		if ((viewDescription != null) && (componentType.getViewBinding() != null)) {
		    Binding binding = componentType.getViewBinding();

		    // Bind view and controller
		    context.setupCode.add(binding.generateBindingCommand(fullControllerName, component.getName()));
		    controllerBindingGenerated = true;
		}

		// Generate init call
		if (controllerDescription.getInitMethod() != null) {
		    String invocationCode = controllerDescription.getInitMethod().generateInvocationCode(
			    fullControllerName, component, platform);
		    context.setupCode.add(invocationCode);
		    controllerBindingGenerated = true;
		}

		// Generate loop call
		if (controllerDescription.getLoopMethod() != null) {
		    String invocationCode = controllerDescription.getLoopMethod().generateInvocationCode(
			    fullControllerName, component, platform);
		    context.loopCode.add(invocationCode);
		    controllerBindingGenerated = true;
		}

		// Remove line with generated comment, if no line of source code
		// has been generated.
		if (!controllerBindingGenerated) {
		    context.setupCode.remove(context.setupCode.size() - 1);
		}

		// Generate definition of controller
		context.privateObjects.add("// Controller for " + component.getName());
		context.privateObjects.add(controllerDescription.generateClassType(component, platform) + " "
			+ controllerName + controllerDescription.generateConstructorArguments(component, platform)
			+ ";");
	    }

	    // Generate view (if necessary)
	    if (viewDescription != null) {
		for (String include : viewDescription.getIncludes()) {
		    context.includes.add("#include <" + componentIncludePrefix + include + ">");
		}

		context.publicObjects
			.add(viewDescription.generateClassType(component, platform) + " " + component.getName()
				+ viewDescription.generateConstructorArguments(component, platform) + ";");
	    }
	}
    }

    /**
     * Generates declarations of eeprom variables and other stuff managing the
     * eeprom layout.
     * 
     * @param context
     *            the context.
     */
    private void generateEepromVariables(Context context) {
	Project project = context.compilationContext.getProject();
	Platform platform = context.compilationContext.getPlatform();

	// Compute offset for each variable
	Map<EepromVariable, Integer> variableOffsets = new HashMap<EepromVariable, Integer>();
	// We start counting offset from 4, since 4 bytes are reserved for
	// eeprom layout version
	int offset = 4;
	for (EepromVariable variable : project.getEepromVariables()) {
	    int sizeof = platform.getSizeOf(variable.getType());
	    if (sizeof == 0) {
		throw new CompilationException("Type '" + variable.getName() + "' of variable '" + variable.getName()
			+ "' is not supported as a type of an eeprom variable.");
	    }

	    variableOffsets.put(variable, offset);
	    offset += sizeof;
	}

	if (!project.getEepromVariables().isEmpty()) {
	    context.includes.add("#include <" + ACPEepromVarsGenerator.EEPROMVARS_HEADER_FILENAME + ">");
	} else {
	    offset = 0;
	}

	// Store eeprom memory usage
	context.compilationContext.getData().put("EepromUsage", offset);

	// Generate eeprom variables declaration
	List<String> externsForHeaderFile = new ArrayList<String>();
	for (EepromVariable variable : project.getEepromVariables()) {
	    StringBuilder codeline = new StringBuilder();
	    codeline.append(platform.getEepromWrapperClass(variable.getType(), variableOffsets.get(variable),
		    variable.isCached()));
	    codeline.append(" ");
	    codeline.append(variable.getName());
	    codeline.append(";");
	    context.eepromVarDefinitions.add(codeline.toString());
	    externsForHeaderFile.add("extern " + codeline.toString());

	    String variableValue = variable.getValue();
	    if ((variableValue != null) && (!variableValue.isEmpty())) {
		if (!platform.checkValue(variable.getType(), variableValue)) {
		    throw new CompilationException("The value '" + variableValue
			    + "' is not valid initialization value for the eeprom variable '" + variable.getName()
			    + "'.");
		}

		context.eepromInitializationCode.add(variable.getName() + ".setValue("
			+ platform.escapeValue(variable.getType(), variableValue) + ");");
	    }
	}

	// Version of the eeprom memory layout (random, hash, or a fixed number)
	String memoryLayoutVersion = project.getEepromLayoutVersion();
	if (memoryLayoutVersion != null) {
	    memoryLayoutVersion = memoryLayoutVersion.trim();
	}

	// Generate the project code, if management of eeprom memory is used.
	if (offset != 0) {
	    // Compute layout version according to project settings.
	    long eepromLayoutVersion = 0;
	    if ("random".equals(memoryLayoutVersion)) {
		eepromLayoutVersion = (int) (Math.random() * 256 * 256 * 256);
	    } else if ("".equals(memoryLayoutVersion) || "hash".equals(memoryLayoutVersion)) {
		StringBuilder hashedContent = new StringBuilder();
		for (String line : context.eepromVarDefinitions) {
		    hashedContent.append(line);
		}
		eepromLayoutVersion = Math.abs(hashedContent.toString().hashCode());
	    } else {
		try {
		    eepromLayoutVersion = Math.abs(Long.parseLong(memoryLayoutVersion));
		} catch (Exception e) {
		    throw new CompilationException("Invalid layout version of eeprom memory.");
		}
	    }

	    // Initialize eeprom variables
	    context.setupCode.add("// Initialize eeprom variables");
	    context.setupCode.add("eeprom_busy_wait();");
	    for (EepromVariable variable : project.getEepromVariables()) {
		context.setupCode.add(variable.getName() + ".init();");
	    }

	    // Generate code that initializes eeprom variables in case when the
	    // layout of eeprom memory is changed.
	    context.setupCode.add("// Set default value of eeprom variables (if necessary)");
	    context.setupCode.add("if (!" + context.privateNamespace + "::checkEepromVersion(" + eepromLayoutVersion
		    + ")) {");
	    context.setupCode.add(BASIC_INDENT + context.privateNamespace + "::initializeEepromVars();");
	    context.setupCode.add(BASIC_INDENT + context.privateNamespace + "::writeEepromVersion("
		    + eepromLayoutVersion + ");");
	    context.setupCode.add("}");
	}

	// Store generated externs for variables
	context.compilationContext.getData().put("EepromExterns", externsForHeaderFile);
    }

    /**
     * Converts a string value that can be an integer value or reference to a
     * property value to an integer value.
     * 
     * @param valueOrProperty
     *            the string to be converted.
     * @param component
     *            the component for which the conversion is realized.
     * @param componentType
     *            the type of component for which the conversion is realized.
     * @param unsetValue
     *            the value to return if the property is not set or is an empty
     *            string.
     * @return the conversion result.
     */
    private int readIntegerValueOrProperty(String valueOrProperty, Component component, ComponentType componentType,
	    int unsetValue) {
	if ((valueOrProperty == null) || (valueOrProperty.trim().isEmpty())) {
	    return unsetValue;
	}

	try {
	    return Integer.parseInt(valueOrProperty);
	} catch (Exception ignore) {

	}

	PropertyType propertyType = componentType.getProperties().get(valueOrProperty);
	if (propertyType == null) {
	    throw new CompilationException("Undefined property '" + valueOrProperty + "'.");
	}

	String strValue = propertyType.getEffectiveValue(component.getProperties().get(valueOrProperty));
	if ((strValue == null) || (strValue.trim().isEmpty())) {
	    return unsetValue;
	}

	try {
	    return Integer.parseInt(strValue.trim());
	} catch (Exception e) {
	    throw new CompilationException("The value of property '" + valueOrProperty
		    + "' cannot be converted to an integer value.");
	}
    }

    @Override
    protected void prepare(CompilationContext compilationContext, Map<String, String> output) {
	// Create context
	Context context = new Context(compilationContext);

	// Clean autogenerated properties
	for (Component component : compilationContext.getProject().getComponents()) {
	    component.getAutogeneratedProperties().clear();
	}

	// Generate eeprom variables
	generateEepromVariables(context);

	// Generate component objects (views and controllers)
	generateComponentObjects(context);

	// Generate code that constructs looper handlers and all management code
	String loopersSection = "";
	if (compilationContext.getSettings().getLooperStrategy() == LooperStrategy.ARRAY) {
	    loopersSection = generateArrayBasedLoopersCode(context);
	}

	// Add program events
	generateProgramEvents(context);

	// Setup watchdog
	if (compilationContext.getProject().getWatchdogLevel() >= 0) {
	    int wl = Math.min(compilationContext.getProject().getWatchdogLevel(), compilationContext.getPlatform()
		    .getMaxWatchdogLevel());

	    context.includes.add("#include <avr/wdt.h>");
	    context.setupCode.add(0, "wdt_disable();");
	    context.setupCode.add("wdt_enable(" + wl + ");");
	    context.loopCode.add(0, "wdt_reset();");
	}

	// Postprocess user defined event handlers
	Set<String> uniqueEventHandlers = new HashSet<String>(context.eventHandlers);
	context.eventHandlers.clear();
	for (String eventHandler : uniqueEventHandlers) {
	    context.eventHandlers.add("extern " + eventHandler + ";");
	}

	// Make includes unique
	Set<String> uniqueIncludes = new LinkedHashSet<String>(context.includes);
	context.includes.clear();
	context.includes.addAll(uniqueIncludes);

	// Prepare replacements for template
	output.put("includes", FileUtils.mergeLines(context.includes));
	output.put("handlers", FileUtils.mergeLines(context.eventHandlers));
	output.put("publicObjects", FileUtils.mergeLines(context.publicObjects));
	output.put("privateNamespace", context.privateNamespace);
	output.put("privateObjects", FileUtils.mergeLines(context.privateObjects, BASIC_INDENT));
	output.put("loopersSection", loopersSection);
	output.put("methodWrappersSection", FileUtils.mergeLines(context.methodWrappersCode));
	output.put("eepromVars", FileUtils.mergeLines(context.eepromVarDefinitions));
	output.put("eepromVarsInitialization", FileUtils.mergeLines(context.eepromInitializationCode, BASIC_INDENT));

	output.put("setup", FileUtils.mergeLines(context.setupCode, BASIC_INDENT));
	output.put("loop", FileUtils.mergeLines(context.loopCode, BASIC_INDENT));

    }

    @Override
    protected void generate(CompilationContext compilationContext, Map<String, String> output) {
	generateOutputFromResourceTemplate("acp_core.cpp", output, new File(compilationContext.getSettings()
		.getOutputSourcePath(), ACP_CORE_SOURCE_FILE));
    }
}
