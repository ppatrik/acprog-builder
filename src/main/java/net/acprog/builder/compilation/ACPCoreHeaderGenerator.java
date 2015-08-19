package net.acprog.builder.compilation;

import java.io.File;
import java.util.*;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;

public class ACPCoreHeaderGenerator extends ACPContentGenerator {

    /**
     * Default name of the acp header (used by all acp components)
     */
    public static final String ACP_HEADER_FILENAME = "acp/core.h";

    @Override
    protected void prepare(CompilationContext compilationContext, Map<String, String> output) {
	output.put("debugMode", compilationContext.getSettings().isDebugMode() ? "1" : "0");
    }

    @Override
    protected void generate(CompilationContext compilationContext, Map<String, String> output) {
	generateOutputFromResourceTemplate("acp_core.h", output, new File(compilationContext.getSettings()
		.getOutputIncludePath(), ACP_HEADER_FILENAME));
    }
}
