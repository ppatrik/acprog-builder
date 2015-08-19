package net.acprog.builder.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.acprog.builder.App;
import net.acprog.builder.compilation.ACPCompiler;
import net.acprog.builder.compilation.CompilationSettings;
import net.acprog.builder.utils.FileUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private JPanel contentPane;

    /**
     * Text field with path to directory containing acp modules.
     */
    private JTextField acpModulesPathTextField;

    /**
     * Text field with path to arduino library directory.
     */
    private JTextField arduinoLibraryPathTextField;

    /**
     * Text field with path to xml file with project description
     * (configuration).
     */
    private JTextField projectXmlTextField;

    /**
     * Name of the generated library.
     */
    private JTextField libraryNameTextField;

    /**
     * Directory with xml file of the the last open ACP project.
     */
    private File lastProjectDirectory;
    private JButton cleanAndBuildButton;
    private JButton buildButton;
    private JButton projectXmlChangeButton;
    private JTextArea exampleInoTextArea;
    private JCheckBox debugModeCheckBox;

    /**
     * Create the frame.
     */
    public MainFrame() {
	// Create and initialize gui components
	initializeComponents();

	// Load last setting values
	loadPaths();
	setButtons();
    }

    private void initializeComponents() {
	setTitle("ACP Builder");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		savePaths();
	    }
	});

	setBounds(100, 100, 446, 426);
	contentPane = new JPanel();
	contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
	setContentPane(contentPane);
	contentPane.setLayout(new MigLayout("insets 0", "[grow,fill]", "[][][][grow]"));

	JPanel acpSettingsPanel = new JPanel();
	acpSettingsPanel.setBorder(new TitledBorder(null, "Settings", TitledBorder.LEADING, TitledBorder.TOP, null,
		null));
	contentPane.add(acpSettingsPanel, "cell 0 0,growx,aligny top");
	acpSettingsPanel.setLayout(new MigLayout("", "[][grow,fill][]", "[][]"));

	JLabel acpModulesPathLabel = new JLabel("ACP modules path:");
	acpSettingsPanel.add(acpModulesPathLabel, "cell 0 0,alignx left,aligny center");

	acpModulesPathTextField = new JTextField();
	acpModulesPathTextField.setEditable(false);
	acpSettingsPanel.add(acpModulesPathTextField, "cell 1 0,growx,aligny center");
	acpModulesPathTextField.setColumns(10);

	JButton acpModulesPathChangeButton = new JButton("Change");
	acpModulesPathChangeButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		changeAcpModulesPath();
	    }
	});
	acpSettingsPanel.add(acpModulesPathChangeButton, "cell 2 0,growx,aligny center");

	JLabel arduinoLibraryPathLabel = new JLabel("Arduino library path:");
	acpSettingsPanel.add(arduinoLibraryPathLabel, "cell 0 1,alignx left,aligny center");

	arduinoLibraryPathTextField = new JTextField();
	arduinoLibraryPathTextField.setEditable(false);
	acpSettingsPanel.add(arduinoLibraryPathTextField, "cell 1 1,growx,aligny center");
	arduinoLibraryPathTextField.setColumns(10);

	JButton arduinoLibraryPathChangeButton = new JButton("Change");
	arduinoLibraryPathChangeButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		changeArduinoLibraryPath();
	    }
	});
	acpSettingsPanel.add(arduinoLibraryPathChangeButton, "cell 2 1,growx,aligny baseline");

	JPanel projectSettingsPanel = new JPanel();
	projectSettingsPanel.setBorder(new TitledBorder(null, "Project", TitledBorder.LEADING, TitledBorder.TOP, null,
		null));
	contentPane.add(projectSettingsPanel, "cell 0 1,grow");
	projectSettingsPanel.setLayout(new MigLayout("", "[][grow,fill][]", "[][]"));

	JLabel projectXmlLabel = new JLabel("Project xml:");
	projectSettingsPanel.add(projectXmlLabel, "cell 0 0,alignx left");

	projectXmlTextField = new JTextField();
	projectXmlTextField.setEditable(false);
	projectSettingsPanel.add(projectXmlTextField, "cell 1 0,growx");
	projectXmlTextField.setColumns(10);

	projectXmlChangeButton = new JButton("Change");
	projectXmlChangeButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		changeProjectFile();
	    }
	});
	projectSettingsPanel.add(projectXmlChangeButton, "cell 2 0");

	JLabel libraryNameLabel = new JLabel("Library name:");
	projectSettingsPanel.add(libraryNameLabel, "cell 0 1,alignx left");

	libraryNameTextField = new JTextField();
	libraryNameTextField.setEditable(false);
	projectSettingsPanel.add(libraryNameTextField, "cell 1 1,growx");
	libraryNameTextField.setColumns(10);

	JPanel actionPanel = new JPanel();
	contentPane.add(actionPanel, "cell 0 2,grow");
	actionPanel.setLayout(new MigLayout("", "[grow,fill][][]", "[]"));

	cleanAndBuildButton = new JButton("Clean & Build");
	cleanAndBuildButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		buildProject(true);
	    }
	});

	debugModeCheckBox = new JCheckBox("Debug mode");
	actionPanel.add(debugModeCheckBox, "cell 0 0");
	actionPanel.add(cleanAndBuildButton, "cell 1 0");

	buildButton = new JButton("Build");
	buildButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		buildProject(false);
	    }
	});
	actionPanel.add(buildButton, "cell 2 0");

	JPanel examplePanel = new JPanel();
	contentPane.add(examplePanel, "cell 0 3,grow");
	examplePanel.setLayout(new BorderLayout(0, 0));

	JScrollPane scrollPane = new JScrollPane();
	examplePanel.add(scrollPane);

	exampleInoTextArea = new JTextArea();
	exampleInoTextArea.setEditable(false);
	scrollPane.setViewportView(exampleInoTextArea);

	JLabel exampleInoLabel = new JLabel("Example:");
	examplePanel.add(exampleInoLabel, BorderLayout.NORTH);
    }

    /**
     * Loads initial values for paths.
     */
    private void loadPaths() {
	Preferences pref = Preferences.userNodeForPackage(App.class);

	// Directory with ACP modules
	acpModulesPathTextField.setText(pref.get("acp-modules-path", ""));
	if (!isDirectory(acpModulesPathTextField.getText())) {
	    acpModulesPathTextField.setText("");
	}

	// Directory with Arduino libraries
	arduinoLibraryPathTextField.setText(pref.get("arduino-library-path", ""));
	if (!isDirectory(arduinoLibraryPathTextField.getText())) {
	    arduinoLibraryPathTextField.setText("");
	}

	// Directory with last open project
	String lastProjectDirectoryPath = pref.get("last-project-path", "");
	if (lastProjectDirectoryPath.trim().isEmpty()) {
	    lastProjectDirectory = new File(".");
	} else {
	    lastProjectDirectory = new File(lastProjectDirectoryPath);
	    if (!lastProjectDirectory.exists() || !lastProjectDirectory.isDirectory()) {
		lastProjectDirectory = new File(".");
	    }
	}
    }

    /**
     * Saves currently used paths for later use.
     */
    private void savePaths() {
	Preferences pref = Preferences.userNodeForPackage(App.class);
	pref.put("acp-modules-path", acpModulesPathTextField.getText());
	pref.put("arduino-library-path", arduinoLibraryPathTextField.getText());
	pref.put("last-project-path", lastProjectDirectory.getAbsolutePath().toString());
    }

    /**
     * Checks whether given path represents an existing directory.
     * 
     * @param directoryPath
     *            the path to a directory.
     * @return true, if the path represents an existing directory, false
     *         otherwise.
     */
    private boolean isDirectory(String directoryPath) {
	if ((directoryPath == null) || (directoryPath.trim().isEmpty())) {
	    return false;
	}

	File dirFile = new File(directoryPath);
	return dirFile.exists() && dirFile.isDirectory();
    }

    /**
     * Changes path to acp modules.
     */
    private void changeAcpModulesPath() {
	JFileChooser chooser = new JFileChooser();
	chooser.setDialogTitle("Choose directory with ACP modules");
	chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	chooser.setSelectedFile(new File(acpModulesPathTextField.getText()));
	int returnVal = chooser.showOpenDialog(this);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    acpModulesPathTextField.setText(chooser.getSelectedFile().toString());
	}

	setButtons();
    }

    /**
     * Changes path to a directory with arduino libraries.
     */
    private void changeArduinoLibraryPath() {
	JFileChooser chooser = new JFileChooser();
	chooser.setDialogTitle("Choose directory with Arduino libraries");
	chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	chooser.setSelectedFile(new File(arduinoLibraryPathTextField.getText()));
	int returnVal = chooser.showOpenDialog(this);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    arduinoLibraryPathTextField.setText(chooser.getSelectedFile().toString());
	}

	setButtons();
    }

    /**
     * Changes the xml file with project description.
     */
    private void changeProjectFile() {
	JFileChooser chooser = new JFileChooser();
	chooser.setDialogTitle("Choose xml file with ACP project");
	FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
	chooser.setFileFilter(filter);
	if (projectXmlTextField.getText().isEmpty()) {
	    chooser.setCurrentDirectory(lastProjectDirectory);
	} else {
	    chooser.setSelectedFile(new File(projectXmlTextField.getText()));
	}

	int returnVal = chooser.showOpenDialog(this);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    File selectedFile = chooser.getSelectedFile();
	    projectXmlTextField.setText(selectedFile.toString());
	    lastProjectDirectory = selectedFile.getParentFile();
	    libraryNameTextField.setEditable(true);
	    String projectFilename = selectedFile.getName();
	    int dotPos = projectFilename.indexOf('.');
	    if (dotPos > 0) {
		libraryNameTextField.setText(projectFilename.substring(0, dotPos).replace(" ", "."));
	    }
	}

	setButtons();
    }

    /**
     * Checks whether build buttons can be enabled.
     */
    private void setButtons() {
	boolean canBuild = true;
	if (!isDirectory(acpModulesPathTextField.getText())) {
	    canBuild = false;
	}

	if (canBuild && !isDirectory(arduinoLibraryPathTextField.getText())) {
	    canBuild = false;
	}

	if (!canBuild) {
	    projectXmlChangeButton.setEnabled(canBuild);
	}

	if (!projectXmlTextField.getText().trim().isEmpty()) {
	    File projectFile = new File(projectXmlTextField.getText());
	    if (!projectFile.exists() || !projectFile.isFile()) {
		canBuild = false;
	    }
	} else {
	    canBuild = false;
	}

	buildButton.setEnabled(canBuild);
	cleanAndBuildButton.setEnabled(canBuild);
    }

    /**
     * Builds the project.
     * 
     * @param clean
     *            true, if the output directory should be cleaned before the
     *            build process, false otherwise.
     */
    private void buildProject(boolean clean) {
	// Check preconditions
	String libraryName = libraryNameTextField.getText().trim();
	if (libraryName.isEmpty()) {
	    JOptionPane.showMessageDialog(this, "Library name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
	    return;
	}

	if (libraryName.matches("^.*[^a-zA-Z0-9 ].*$")) {
	    JOptionPane.showMessageDialog(this, "Library name cannot contain nonalphanumeric characters.", "Error",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}

	File acpModulesDirectory = new File(acpModulesPathTextField.getText());
	if (!acpModulesDirectory.exists() || !acpModulesDirectory.isDirectory()) {
	    JOptionPane.showMessageDialog(this, "Directory with ACP modules does not exist.", "Error",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}

	File arduinoLibraryDirectory = new File(arduinoLibraryPathTextField.getText());
	if (!arduinoLibraryDirectory.exists() || !arduinoLibraryDirectory.isDirectory()) {
	    JOptionPane.showMessageDialog(this, "Directory with Arduino libraries does not exist.", "Error",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}

	File projectFile = new File(projectXmlTextField.getText());
	if (!projectFile.exists() || !projectFile.isFile()) {
	    JOptionPane.showMessageDialog(this, "File with project configuration does not exist.", "Error",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}

	// Clean (if required)
	if (clean) {
	    File libraryDir = new File(arduinoLibraryDirectory, libraryName);
	    FileUtils.removeDirectory(libraryDir);
	}

	// Build
	try {
	    ACPCompiler compiler = new ACPCompiler(acpModulesDirectory);
	    CompilationSettings settings = new CompilationSettings();
	    settings.setProjectConfigurationFile(projectFile);
	    settings.setLibraryName(libraryName);
	    settings.setOutputLibraryPath(arduinoLibraryDirectory);
	    settings.setDebugMode(debugModeCheckBox.isSelected());
	    compiler.compile(settings);

	    File exampleFile = settings.getExampleFile();
	    if (exampleFile.exists()) {
		exampleInoTextArea.setText(FileUtils.readFile(exampleFile));
	    } else {
		exampleInoTextArea.setText("");
	    }

	    exampleInoTextArea.setCaretPosition(0);
	} catch (Exception e) {
	    JOptionPane.showMessageDialog(this, "Build failed: " + e.getLocalizedMessage(), "Error",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}

	JOptionPane.showMessageDialog(this, "Build completed.");
    }
}
