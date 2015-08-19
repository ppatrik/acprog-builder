package net.acprog.builder.compilation;

import java.util.*;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;
import net.acprog.builder.components.Instance;
import net.acprog.builder.modules.ComponentType;
import net.acprog.builder.modules.Library;
import net.acprog.builder.modules.Module;
import net.acprog.builder.platform.Platform;
import net.acprog.builder.project.Component;
import net.acprog.builder.project.Project;
import net.acprog.builder.utils.FileUtils;

/**
 * Generator of file with acp project header.
 */
public class ACPProjectHeaderGenerator extends ACPContentGenerator {

    @Override
    protected void prepare(CompilationContext compilationContext, Map<String, String> output) {
	Project project = compilationContext.getProject();
	Platform platform = compilationContext.getPlatform();
	Map<String, Module> projectModules = compilationContext.getProjectModules();

	// Filter components with views
	List<Component> componentsWithView = new ArrayList<Component>();
	for (Component component : project.getComponents()) {
	    ComponentType ctd = (ComponentType) projectModules.get(component.getType());
	    if (ctd.getView() != null) {
		componentsWithView.add(component);
	    }
	}

	// Collect header files to include (for views)
	Set<String> includes = new LinkedHashSet<String>();
	for (Component component : componentsWithView) {
	    ComponentType ctd = (ComponentType) projectModules.get(component.getType());
	    Instance view = ctd.getView();
	    for (String include : view.getIncludes()) {
		include = ctd.getName().replace('.', '/') + "/" + include;
		include = "#include <" + include + ">";
		includes.add(FileUtils.mergeSlashes(include));
	    }
	}

	// Collect header files to include (for libraries)
	for (String libraryImport : project.getLibraryImports()) {
	    Library library = (Library) projectModules.get(libraryImport);
	    for (String include : library.getIncludes()) {
		include = library.getName().replace('.', '/') + "/" + include;
		include = "#include <" + include + ">";
		includes.add(FileUtils.mergeSlashes(include));
	    }
	}

	// Generate view externs
	List<String> viewExterns = new ArrayList<String>();
	for (Component component : componentsWithView) {
	    ComponentType ctd = (ComponentType) projectModules.get(component.getType());
	    Instance view = ctd.getView();
	    try {
		String viewDeclaration = "extern " + view.generateClassType(component, platform) + " "
			+ component.getName() + ";";
		viewExterns.add(viewDeclaration);
	    } catch (Exception e) {
		throw new CompilationException("Class type for view of component " + component.getName()
			+ " is invalid.", e);
	    }
	}

	// Generate externs for eeprom variables
	@SuppressWarnings("unchecked")
	List<String> eepromVarExterns = (List<String>) compilationContext.getData().get("EepromExterns");
	if (!eepromVarExterns.isEmpty()) {
	    includes.add("#include <" + ACPEepromVarsGenerator.EEPROMVARS_HEADER_FILENAME + ">");
	}

	// Make includes unique
	Set<String> uniqueIncludes = new LinkedHashSet<String>(includes);
	includes.clear();
	includes.addAll(uniqueIncludes);

	// Prepare replacements for template
	output.put("includes", FileUtils.mergeLines(includes));
	output.put("views", FileUtils.mergeLines(viewExterns));
	output.put("eepromUsage", compilationContext.getData().get("EepromUsage").toString());
	output.put("eepromVars", FileUtils.mergeLines(eepromVarExterns));
    }

    @Override
    protected void generate(CompilationContext compilationContext, Map<String, String> output) {
	generateOutputFromResourceTemplate("acp_project.h", output, compilationContext.getSettings()
		.getProjectHeaderFile());

    }
}
