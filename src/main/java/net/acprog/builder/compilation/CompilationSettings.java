package net.acprog.builder.compilation;

import java.io.File;

/**
 * Settings of a compilation task.
 */
public class CompilationSettings {

    // ---------------------------------------------------------------------------
    // Looper strategy
    // ---------------------------------------------------------------------------

    public static enum LooperStrategy {
	/**
	 * Array based priority queue
	 */
	ARRAY
    }

    // ---------------------------------------------------------------------------
    // Instance variables
    // ---------------------------------------------------------------------------

    /**
     * File with project configuration
     */
    private File projectConfigurationFile;

    /**
     * Directory where the compiled library will be created.
     */
    private File outputLibraryPath;

    /**
     * Name of generated library. This name is used to generated name of the
     * main header file.
     */
    private String libraryName;

    /**
     * Indicates whether merging all source file to a single directory is
     * enabled.
     */
    private boolean sourceFilesDirectoryMerging;

    /**
     * Indicates whether the debug mode is active.
     */
    private boolean debugMode;

    /**
     * Algorithms and strategy used by generated looper code.
     */
    private LooperStrategy looperStrategy = LooperStrategy.ARRAY;

    // ---------------------------------------------------------------------------
    // Setters and getters
    // ---------------------------------------------------------------------------

    public File getProjectConfigurationFile() {
	return projectConfigurationFile;
    }

    public void setProjectConfigurationFile(File projectConfigurationFile) {
	this.projectConfigurationFile = projectConfigurationFile;
    }

    public LooperStrategy getLooperStrategy() {
	return looperStrategy;
    }

    public void setLooperStrategy(LooperStrategy looperStrategy) {
	this.looperStrategy = looperStrategy;
    }

    public File getOutputLibraryPath() {
	return outputLibraryPath;
    }

    public void setOutputLibraryPath(File outputLibraryPath) {
	this.outputLibraryPath = outputLibraryPath;
    }

    public String getLibraryName() {
	return libraryName;
    }

    public void setLibraryName(String libraryName) {
	this.libraryName = libraryName;
    }

    public boolean isSourceFilesDirectoryMerging() {
	return sourceFilesDirectoryMerging;
    }

    public void setSourceFilesDirectoryMerging(boolean sourceFilesDirectoryMerging) {
	this.sourceFilesDirectoryMerging = sourceFilesDirectoryMerging;
    }

    public boolean isDebugMode() {
	return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
	this.debugMode = debugMode;
    }

    // ---------------------------------------------------------------------------
    // Specific getters for arduino 1.6 compatible libraries.
    // ---------------------------------------------------------------------------

    /**
     * Returns the library directory where all library files are generated.
     * 
     * @return the directory with generated library.
     */
    public File getLibraryDirectory() {
	return new File(outputLibraryPath, libraryName);
    }

    /**
     * Returns the directory where all includes are generated.
     * 
     * @return the directory to generate includes.
     */
    public File getOutputIncludePath() {
	return new File(getLibraryDirectory(), "src");
    }

    /**
     * Returns the directory where all used and generated source files are
     * placed.
     * 
     * @return the directory where all
     */
    public File getOutputSourcePath() {
	return new File(getLibraryDirectory(), "src/sources");
    }

    /**
     * Returns the header file that is the main header file to be included in an
     * arduino project.
     * 
     * @return the main include file of generated library.
     */
    public File getProjectHeaderFile() {
	return new File(getOutputIncludePath(), libraryName + ".h");
    }

    /**
     * Returns the file with generated example (skeleton) sketch.
     * 
     * @return the example sketch file.
     */
    public File getExampleFile() {
	return new File(getLibraryDirectory(), "examples/" + libraryName + "Skeleton/" + libraryName + "Skeleton.ino");
    }
}
