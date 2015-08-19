package net.acprog.builder.compilation;

import java.io.File;
import java.util.*;

import net.acprog.builder.compilation.ACPCompiler.CompilationContext;

/**
 * Generator of header file for EEPROM variables support.
 */
public class ACPEepromVarsGenerator extends ACPContentGenerator {

    /**
     * Default name of the generated header file.
     */
    public static final String EEPROMVARS_HEADER_FILENAME = "acp/eeprom_vars.h";

    /**
     * Default name of the generated source file.
     */
    public static final String EEPROMVARS_SOURCE_FILENAME = "eeprom_vars.cpp";

    @Override
    protected void prepare(CompilationContext compilationContext, Map<String, String> output) {
	output.put("privateNamespace", (String) compilationContext.getData().get("PrivateNamespace"));
	output.put("acpEepromHeaderFile", EEPROMVARS_HEADER_FILENAME);
    }

    @Override
    protected void generate(CompilationContext compilationContext, Map<String, String> output) {
	if (((Number) compilationContext.getData().get("EepromUsage")).intValue() == 0) {
	    return;
	}

	generateOutputFromResourceTemplate("acp_eeprom_vars.h", output, new File(compilationContext.getSettings()
		.getOutputIncludePath(), EEPROMVARS_HEADER_FILENAME));
	generateOutputFromResourceTemplate("acp_eeprom_vars.cpp", output, new File(compilationContext.getSettings()
		.getOutputSourcePath(), EEPROMVARS_SOURCE_FILENAME));
    }
}
