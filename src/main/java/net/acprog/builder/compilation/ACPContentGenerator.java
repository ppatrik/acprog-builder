package net.acprog.builder.compilation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;
import net.acprog.builder.utils.FileUtils;

/**
 * Base class for customized content generators used during compilation process.
 */
public abstract class ACPContentGenerator {

    /**
     * Content generators that must be executed before executing this content
     * generator.
     */
    private final List<ACPContentGenerator> dependencies = new ArrayList<ACPContentGenerator>();

    /**
     * Adds a content generator to the dependency list of this content
     * generator.
     * 
     * @param dependency
     *            the content generator to be executed before this content
     *            generator.
     */
    public void addDependecy(ACPContentGenerator dependency) {
	dependencies.add(dependency);
    }

    /**
     * Prepares the output.
     * 
     * @param compilationContext
     *            the compilation (content generation) context.
     * @param output
     *            the map storing output of content generation.
     */
    protected abstract void prepare(CompilationContext compilationContext, Map<String, String> output);

    /**
     * Generates the output.
     * 
     * @param compilationContext
     *            the compilation (content generation) context.
     * @param output
     *            the map storing output of content generation.
     */
    protected abstract void generate(CompilationContext compilationContext, Map<String, String> output);

    /**
     * Generates an output file from a resource template applying given
     * replacements.
     * 
     * @param templateName
     *            the name of template resource.
     * @param replacements
     *            the map with replacements.
     * @param outputFile
     *            the output file.
     */
    protected void generateOutputFromResourceTemplate(String templateName, Map<String, String> replacements,
	    File outputFile) {
	String templateResource = ACPCompiler.TEMPLATES_RESOURCE_DIR + templateName;
	String fileContent = FileUtils.loadTemplateResource(templateResource, replacements);

	if (fileContent == null) {
	    throw new CompilationException("Unavailable resource file: " + templateResource);
	}

	if (!FileUtils.saveToFile(outputFile, fileContent)) {
	    throw new CompilationException("File " + outputFile.getAbsolutePath() + " cannot be created.");
	}
    }

    /**
     * Executes and manages the process of generating content by all given
     * content generators.
     * 
     * @param contentGenerators
     *            the list of content generators.
     * @param compilationContext
     *            the compilaton context.
     */
    public static void generateContent(List<ACPContentGenerator> contentGenerators,
	    CompilationContext compilationContext) {

	// Compute execution order
	List<ACPContentGenerator> orderedGenerators = new ArrayList<ACPContentGenerator>();
	Set<ACPContentGenerator> lockedGenerators = new HashSet<ACPContentGenerator>();
	for (ACPContentGenerator generator : contentGenerators) {
	    orderDependencies(generator, orderedGenerators, lockedGenerators);
	}

	// Create additional data storage objects
	Map<ACPContentGenerator, Map<String, String>> outputs = new HashMap<ACPContentGenerator, Map<String, String>>();
	for (ACPContentGenerator generator : orderedGenerators) {
	    outputs.put(generator, new HashMap<String, String>());
	}

	// Prepare phase
	for (ACPContentGenerator generator : orderedGenerators) {
	    generator.prepare(compilationContext, outputs.get(generator));
	}

	// Generate phase
	for (ACPContentGenerator generator : orderedGenerators) {
	    generator.generate(compilationContext, outputs.get(generator));
	}
    }

    /**
     * Recursively computes an execution order execution of generators that
     * satisfies specified dependencies.
     * 
     * @param cg
     *            the content generator to be processed.
     * @param orderedGenerators
     *            the ordered list of content generators.
     * @param lockedGenerators
     *            the locked content generators used to check circular
     *            dependencies.
     */
    private static void orderDependencies(ACPContentGenerator cg, List<ACPContentGenerator> orderedGenerators,
	    Set<ACPContentGenerator> lockedGenerators) {
	if (cg == null) {
	    return;
	}

	if (orderedGenerators.contains(cg)) {
	    return;
	}

	if (!lockedGenerators.add(cg)) {
	    throw new CompilationException("Circular dependencies of content generators.");
	}

	for (ACPContentGenerator dependency : cg.dependencies) {
	    orderDependencies(dependency, orderedGenerators, lockedGenerators);
	}

	orderedGenerators.add(cg);
	lockedGenerators.remove(cg);
    }
}
