package net.acprog.builder.compilation;

import java.io.File;
import java.util.*;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;

/**
 * Generator of file with arduino library properties.
 */
public class ACPLibraryPropGenerator extends ACPContentGenerator {

    @Override
    protected void prepare(CompilationContext compilationContext, Map<String, String> output) {
	output.put("libraryName", compilationContext.getSettings().getLibraryName());
    }

    @Override
    protected void generate(CompilationContext compilationContext, Map<String, String> output) {
	generateOutputFromResourceTemplate("library.properties", output, new File(compilationContext.getSettings()
		.getLibraryDirectory(), "library.properties"));

    }
}
