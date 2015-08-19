package net.acprog.builder.compilation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import net.acprog.builder.components.ConfigurationException;
import net.acprog.builder.components.PropertyType;
import net.acprog.builder.modules.ComponentType;
import net.acprog.builder.modules.Library;
import net.acprog.builder.modules.Module;
import net.acprog.builder.platform.Platform;
import net.acprog.builder.project.Component;
import net.acprog.builder.project.Project;

/**
 * Compiler of ACP projects.
 */
public class ACPCompiler {

    // ---------------------------------------------------------------------------
    // Compilation context
    // ---------------------------------------------------------------------------

    /**
     * Bundle of all objects required to complete compilation.
     */
    public static class CompilationContext {
	/**
	 * Compilation settings.
	 */
	private CompilationSettings settings;

	/**
	 * Compiled project.
	 */
	private Project project;

	/**
	 * Target compilation platform.
	 */
	private Platform platform;

	/**
	 * Preloaded project modules (including all dependencies).
	 */
	private final Map<String, Module> projectModules;

	/**
	 * Storage for storing intermediate outputs.
	 */
	private final Map<String, Object> data;

	/**
	 * Returns the compilation settings for the compilation context.
	 * 
	 * @return the compilation settings.
	 */
	public CompilationSettings getSettings() {
	    return settings;
	}

	/**
	 * Returns the compiled project.
	 * 
	 * @return the project.
	 */
	public Project getProject() {
	    return project;
	}

	/**
	 * Returns the map with preloaded project modules.
	 * 
	 * @return the project modules.
	 */
	public Map<String, Module> getProjectModules() {
	    return projectModules;
	}

	/**
	 * Returns the map storing intermediate outputs of the compilation.
	 * 
	 * @return the storage for intermediate outputs.
	 */
	public Map<String, Object> getData() {
	    return data;
	}

	/**
	 * Returns the target (compilation) platform.
	 * 
	 * @return the compilation platform.
	 */
	public Platform getPlatform() {
	    return platform;
	}

	/**
	 * Constructs the compilation context.
	 */
	private CompilationContext() {
	    projectModules = new HashMap<String, Module>();
	    data = new HashMap<String, Object>();
	}
    }

    // ---------------------------------------------------------------------------
    // Static constants
    // ---------------------------------------------------------------------------

    /**
     * Resource directory with templates.
     */
    public static final String TEMPLATES_RESOURCE_DIR = "/templates/";

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * Path to the modules directory.
     */
    private final File modulesPath;

    // ---------------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------------

    /**
     * Constructs a compiler.
     * 
     * @param modulesPath
     *            the directory that is a root of directories containig ACP
     *            modules.
     */
    public ACPCompiler(File modulesPath) {
	this.modulesPath = modulesPath;
    }

    // ---------------------------------------------------------------------------
    // Compilation methods
    // ---------------------------------------------------------------------------

    /**
     * Compiles a project.
     * 
     * @param settings
     *            the compilation settings.
     */
    public void compile(CompilationSettings settings) {
	// Create compilation context
	CompilationContext context = new CompilationContext();
	context.settings = settings;

	// Load project configuration
	try {
	    context.project = Project.loadFromFile(settings.getProjectConfigurationFile());
	} catch (ConfigurationException e) {
	    throw new CompilationException("Project configuration contains errors.", e);
	}

	// Load description of target hardware (compilation) platform
	String platformName = context.project.getPlatformName();
	if ((platformName == null) || (platformName.trim().isEmpty())) {
	    platformName = "Arduino";
	}

	context.platform = Platform.loadPlatform(platformName);
	if (context.platform == null) {
	    throw new CompilationException("Unsupported hardware platfrom '" + platformName + "'.");
	}

	// Check component names
	checkComponentNames(context.project.getComponents());

	// Construct a set of names of all required modules
	Set<String> namesOfRequiredModules = new HashSet<String>();
	for (Component component : context.project.getComponents()) {
	    namesOfRequiredModules.add(component.getType());
	}
	namesOfRequiredModules.addAll(context.project.getLibraryImports());

	// Find all modules referenced from the project (modules that are
	// imported in the program or required component types).
	context.projectModules.putAll(loadModulesWithDependencies(namesOfRequiredModules));

	// Check whether each component is properly configured with respect to
	// component description
	for (Component component : context.project.getComponents()) {
	    Module componentTypeModule = context.projectModules.get(component.getType());
	    if ((componentTypeModule == null) || (!(componentTypeModule instanceof ComponentType))) {
		throw new CompilationException("Invalid component type (" + component.getType() + ") of component "
			+ component.getName() + ".");
	    }

	    checkComponentConfiguration(component, (ComponentType) componentTypeModule, context.platform);
	}

	// Check whether all program imports are library modules
	for (String libraryImport : context.project.getLibraryImports()) {
	    Module libraryModule = context.projectModules.get(libraryImport);
	    if ((libraryModule == null) || (!(libraryModule instanceof Library))) {
		throw new CompilationException("Library " + libraryImport
			+ " imported by program is not a library module.");
	    }
	}

	// Prepare output directories
	File outIncludes = settings.getOutputIncludePath();
	outIncludes.mkdirs();
	if (!(outIncludes.exists() && outIncludes.isDirectory())) {
	    throw new CompilationException("Output directory for includes (" + outIncludes.getAbsolutePath()
		    + ") cannot be created.");
	}

	File outSources = settings.getOutputSourcePath();
	outSources.mkdirs();
	if (!(outSources.exists() && outSources.isDirectory())) {
	    throw new CompilationException("Output directory for source files (" + outIncludes.getAbsolutePath()
		    + ") cannot be created.");
	}

	// Copy include and source files for each referenced module
	for (Module module : context.projectModules.values()) {
	    exportFilesOfModule(module, settings);
	}

	// Initialize context
	context.data.put("PrivateNamespace", "acp_private");

	// Create content generators
	List<ACPContentGenerator> contentGenerators = new ArrayList<ACPContentGenerator>();

	// Generate source file with the project code.
	ACPProjectCodeGenerator acpProjectCodeGenerator = new ACPProjectCodeGenerator();
	contentGenerators.add(acpProjectCodeGenerator);

	// Generate the main header file of the project included in the main
	// arduino sketch file.
	ACPProjectHeaderGenerator projectHeaderGenerator = new ACPProjectHeaderGenerator();
	projectHeaderGenerator.addDependecy(acpProjectCodeGenerator);
	contentGenerators.add(projectHeaderGenerator);

	// Generate acp core header file (this header file is included by each
	// acp module).
	ACPCoreHeaderGenerator coreHeaderGenerator = new ACPCoreHeaderGenerator();
	contentGenerators.add(coreHeaderGenerator);

	// Generate header file for supporting EEPROM variables
	ACPEepromVarsGenerator acpEEPROMVarsGenerator = new ACPEepromVarsGenerator();
	acpEEPROMVarsGenerator.addDependecy(acpProjectCodeGenerator);
	contentGenerators.add(acpEEPROMVarsGenerator);

	// Generate example sketch for the generated library.
	ACPExampleGenerator acpExampleGenerator = new ACPExampleGenerator();
	acpExampleGenerator.addDependecy(acpProjectCodeGenerator);
	contentGenerators.add(acpExampleGenerator);

	// Generate library.properties file
	ACPLibraryPropGenerator acpLibraryPropGenerator = new ACPLibraryPropGenerator();
	contentGenerators.add(acpLibraryPropGenerator);

	// Generate all autogenerated files.
	ACPContentGenerator.generateContent(contentGenerators, context);
    }

    /**
     * Loads modules with names in the given set of module names.
     * 
     * @param namesOfModules
     *            the names of modules forming an initial set of modules
     *            required by the project.
     */
    private Map<String, Module> loadModulesWithDependencies(Set<String> namesOfModules) {
	Map<String, Module> result = new HashMap<String, Module>();
	Set<String> missingModules = new HashSet<String>(namesOfModules);
	while (!missingModules.isEmpty()) {
	    Map<String, Module> newModules = new HashMap<String, Module>();

	    // Load missing modules
	    for (String requiredModule : missingModules) {
		// Skip modules with empty name
		requiredModule = requiredModule.trim();
		if (requiredModule.isEmpty()) {
		    continue;
		}

		// Load module description
		Module module;
		try {
		    module = Module.loadFromFile(new File(ensureModule(requiredModule), Module.DESCRIPTION_FILE));
		    if (!requiredModule.equals(module.getName())) {
			throw new CompilationException("Invalid name of module in module description: "
				+ requiredModule);
		    }
		} catch (Exception e) {
		    throw new CompilationException("Invalid description file of module " + requiredModule, e);
		}

		// Add module to newly loaded modules.
		newModules.put(requiredModule, module);
	    }
	    result.putAll(newModules);
	    missingModules.clear();

	    // Find missing dependencies of newly loaded modules
	    for (Module newModule : newModules.values()) {
		for (String requiredModule : newModule.getRequiredModules()) {
		    if (!result.containsKey(requiredModule)) {
			missingModules.add(requiredModule);
		    }
		}
	    }
	}

	return result;
    }

    /**
     * Ensures that module with given name is available and returns the path to
     * directory containing the module.
     * 
     * @param moduleName
     *            the name of a module.
     * @return the directory with the module description and data.
     * @throws CompilationException
     *             if the module is not available.
     */
    private File ensureModule(String moduleName) throws CompilationException {
	File componentPath = new File(modulesPath, moduleName.replace('.', '/'));
	if (!(componentPath.exists() && componentPath.isDirectory())) {
	    throw new CompilationException("Unavailable module " + moduleName);
	}

	return componentPath;
    }

    /**
     * Checks whether names of components fulfill given constrains.
     * 
     * @param components
     *            the list of investigated components.
     */
    private void checkComponentNames(List<Component> components) {
	Set<String> names = new HashSet<String>();
	for (Component component : components) {
	    if (!names.add(component.getName())) {
		throw new CompilationException("Duplicated component name: " + component.getName());
	    }
	}
    }

    /**
     * Checks whether a component is properly configured with respect to its
     * component type.
     * 
     * @param component
     *            the component configuration
     * @param componentType
     *            the description of component type.
     * @param platform
     *            the target hardware and compilation platform.
     */
    private void checkComponentConfiguration(Component component, ComponentType componentType, Platform platform) {
	// Check whether all properties required by component type are properly
	// set
	for (Map.Entry<String, PropertyType> entry : componentType.getProperties().entrySet()) {
	    String propertyName = entry.getKey();
	    PropertyType propertyType = entry.getValue();
	    String effectiveValue = propertyType.getEffectiveValue(component.getProperties().get(propertyName));
	    if (!(platform.checkValue(propertyType.getType(), effectiveValue) && propertyType
		    .checkRestrictions(effectiveValue))) {
		throw new CompilationException("Invalid or undefined value of property '" + propertyName
			+ "' of component '" + component.getName() + "'.");
	    }
	}

	// Check whether component contains properties that are not valid for
	// given component type
	for (String propertyName : component.getProperties().keySet()) {
	    if (!componentType.getProperties().containsKey(propertyName)) {
		throw new CompilationException("Property '" + propertyName + "' is not supported in component '"
			+ component.getName() + "'.");
	    }
	}

	// Check whether defined events are valid events for given component
	// type
	for (String eventName : component.getEvents().keySet()) {
	    if (!componentType.getEvents().containsKey(eventName)) {
		throw new CompilationException("Event '" + eventName + "' is not supported in component '"
			+ component.getName() + "'.");
	    }
	}
    }

    /**
     * Export all files required by given module.
     * 
     * @param module
     *            the module description.
     * @param settings
     *            the compilation settings.
     */
    private void exportFilesOfModule(Module module, CompilationSettings settings) {
	// Path to files the module
	String modulePath = module.getName().replace('.', '/');

	// Export include files
	File moduleIncludeDir = new File(module.getDirectory(), Module.INCLUDE_SUBDIR);
	if (moduleIncludeDir.exists() && moduleIncludeDir.isDirectory()) {
	    File exportDir = new File(settings.getOutputIncludePath(), modulePath);
	    exportDir.mkdirs();
	    if (!(exportDir.exists() && exportDir.isDirectory())) {
		throw new CompilationException("Output directory for include files of module '" + module.getName()
			+ "' cannot be created: " + exportDir.getAbsolutePath());
	    }
	    copyDirectory(moduleIncludeDir, exportDir);
	}

	// Export source files
	File moduleSrcDir = new File(module.getDirectory(), Module.SRC_SUBDIR);
	if (moduleSrcDir.exists() && moduleSrcDir.isDirectory()) {
	    if (settings.isSourceFilesDirectoryMerging()) {
		// Export all source files to a single directory
		String modulePrefix = underscoreEscape(module.getName()).replace('.', '_');
		copyDirectoryMerged(moduleSrcDir, settings.getOutputSourcePath(), modulePrefix + "_");
	    } else {
		// Export with directory structure
		File exportDir = new File(settings.getOutputSourcePath(), modulePath);
		exportDir.mkdirs();
		if (!(exportDir.exists() && exportDir.isDirectory())) {
		    throw new CompilationException("Output directory for source files of module '" + module.getName()
			    + "' cannot be created: " + exportDir.getAbsolutePath());
		}
		copyDirectory(moduleSrcDir, exportDir);
	    }
	}
    }

    /**
     * Copies all files from source directory to destination directory.
     * 
     * @param source
     *            the source directory
     * @param dest
     *            the destination directory
     */
    private void copyDirectory(File source, File dest) {
	// Prepare dest directory
	dest.mkdirs();
	if (!(dest.exists() && dest.isDirectory())) {
	    throw new CompilationException("Directory " + dest.getAbsolutePath()
		    + " does not exists or cannot be created.");
	}

	// Copy files and directories
	for (File file : source.listFiles()) {
	    if (file.isDirectory()) {
		copyDirectory(file, new File(dest, file.getName()));
	    }

	    if (file.isFile()) {
		copyFile(file, new File(dest, file.getName()));
	    }
	}
    }

    /**
     * Copies all files from source directory to destination directory in such a
     * way that all files will be merged in the destination directory.
     * 
     * @param source
     *            the source directory.
     * @param dest
     *            the destination directory.
     * @param filePrefix
     *            the prefix added to all files from the source directory.
     */
    private void copyDirectoryMerged(File source, File dest, String filePrefix) {
	if (!source.exists()) {
	    return;
	}

	// Prepare dest directory
	dest.mkdirs();
	if (!(dest.exists() && dest.isDirectory())) {
	    throw new CompilationException("Directory " + dest.getAbsolutePath()
		    + " does not exists or cannot be created.");
	}

	// Copy files and directories
	for (File file : source.listFiles()) {
	    if (file.isDirectory()) {
		copyDirectoryMerged(file, dest, filePrefix + underscoreEscape(file.getName()) + "_");
	    }

	    if (file.isFile()) {
		copyFile(file, new File(dest, filePrefix + underscoreEscape(file.getName())));
	    }
	}
    }

    /**
     * Copies regular file.
     * 
     * @param source
     *            the source file.
     * @param dest
     *            the destination file.
     */
    private void copyFile(File source, File dest) {
	/*
	 * if (dest.exists()) { return; }
	 */

	try {
	    Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
	} catch (IOException e) {
	    throw new CompilationException("File " + source.getAbsolutePath() + " cannot be copied to "
		    + dest.getAbsolutePath() + ".");
	}
    }

    /**
     * Escapes string in order to use underscore as a path separator.
     * 
     * @param s
     *            the string to be escaped.
     * @return the escaped string.
     */
    private String underscoreEscape(String s) {
	return s.replace("_", "__");
    }
}
