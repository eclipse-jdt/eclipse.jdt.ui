package org.eclipse.jdt.internal.ui.jarpackager;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 *	Page 1 of the JAR Package wizard
 */
class JarPackageWizardPage extends WizardExportResourcesPage implements IJarPackageWizardPage {

	private JarPackage fJarPackage;
	private IStructuredSelection fSelection;
	private Set fSelectedContainers;

	// widgets
	private Text	fSourceNameField;	
	private Button	fSourceBrowseButton;
	private Button	fExportClassFilesCheckbox;
	private Button	fExportJavaFilesCheckbox;	
	
	private Combo	fDestinationNamesCombo;
	private Button	fDestinationBrowseButton;
	
	private Button		fCompressCheckbox;
	private Button		fOverwriteCheckbox;
	private Composite	fDescriptionFileGroup;
	private Button		fSaveDescriptionCheckbox;
	private Label		fDescriptionFileLabel;
	private Text		fDescriptionFileText;
	private Button		fDescriptionFileBrowseButton;

	// dialog store id constants
	private final static String PAGE_NAME= "JarPackageWizardPage";
	
	private final static String STORE_EXPORT_CLASS_FILES= PAGE_NAME + ".EXPORT_CLASS_FILES";
	private final static String STORE_EXPORT_JAVA_FILES= PAGE_NAME + ".EXPORT_JAVA_FILES";
	
	private final static String STORE_DESTINATION_NAMES= PAGE_NAME + ".DESTINATION_NAMES_ID";
	
	private final static String STORE_COMPRESS= PAGE_NAME + ".COMPRESS";
	private final static String STORE_OVERWRITE= PAGE_NAME + ".OVERWRITE";
	private final static String STORE_SAVE_DESCRIPTION= PAGE_NAME + ".SAVE_DESCRIPTION";
	private final static String STORE_DESCRIPTION_LOCATION= PAGE_NAME + ".DESCRIPTION_LOCATION";
	
	/**
	 *	Create an instance of this class
	 */
	public JarPackageWizardPage(JarPackage jarPackage, IStructuredSelection selection) {
		super("jarPackageWizardPage", selection);
		setTitle("JAR Package Specification");
		setDescription("Define what resources to package into which JAR");
		fJarPackage= jarPackage;
		fSelection= selection;
		fSelectedContainers= new HashSet();
	}
	/*
	 * Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(
			new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createPlainLabel(composite, "What do you want to export?");
		createResourcesGroup(composite);
		
		// Only way to add listener and reuse source selection dialog
		if (composite.getChildren().length == 2) {
			Control sourceSelectionControl= composite.getChildren()[1];
			if (sourceSelectionControl instanceof Composite) {
				Control[] sourceSelectionSubControls= ((Composite)sourceSelectionControl).getChildren();
				if (sourceSelectionSubControls.length > 1) {
					sourceSelectionSubControls[0].addListener(SWT.MouseDown, this);
					sourceSelectionSubControls[1].addListener(SWT.MouseDown, this);
				}
			}
		}
		createButtonsGroup(composite);
		createResourceOptionsGroup(composite);

//		createSpacer(composite);

		createPlainLabel(composite, "Where do you want to export resources to?");
		createDestinationGroup(composite);

//		createSpacer(composite);

		createPlainLabel(composite, "Options:");
		createOptionsGroup(composite);

		restoreResourceSpecificationWidgetValues(); // superclass API defines this hook
		restoreWidgetValues();
		if (fSelection != null)
			setupBasedOnInitialSelections();

		setControl(composite);

		updateModel();
		updateWidgetEnablements();
		updatePageCompletion();

		giveFocusToDestination();
	}
	/**
	 *	Create the export options specification widgets.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 */
	protected void createOptionsGroup(Composite parent) {
		Composite optionsGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		optionsGroup.setLayout(layout);

		fCompressCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fCompressCheckbox.setText("Compress the contents of the JAR file");
		fCompressCheckbox.addListener(SWT.Selection, this);

		fOverwriteCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fOverwriteCheckbox.setText("Overwrite existing files without warning");
		fOverwriteCheckbox.addListener(SWT.Selection, this);

		fSaveDescriptionCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fSaveDescriptionCheckbox.setText("Save the description of this JAR in the workspace");
		fSaveDescriptionCheckbox.addListener(SWT.Selection, this);

		createDescriptionFileGroup(parent);
	}
	/**
	 *	Answer the contents of the destination specification widget. If this
	 *	value does not have the required suffix then add it first.
	 *
	 *	@return java.lang.String
	 */
	protected String getDestinationValue() {
		String requiredSuffix= getOutputSuffix();
		String destinationText= fDestinationNamesCombo.getText().trim();
		if (!destinationText.toLowerCase().endsWith(requiredSuffix.toLowerCase()))
			destinationText += requiredSuffix;
	return destinationText;
	}
	/**
	 *	Answer the string to display in self as the destination type
	 *
	 *	@return java.lang.String
	 */
	protected String getDestinationLabel() {
		return "JAR file:";
	}
	/**
	 *	Answer the suffix that files exported from this wizard must have.
	 *	If this suffix is a file extension (which is typically the case)
	 *	then it must include the leading period character.
	 *
	 *	@return java.lang.String
	 */
	protected String getOutputSuffix() {
		return "." + JarPackage.EXTENSION;
	}
	/**
	 * Persists resource specification control setting that are to be restored
	 * in the next instance of this page. Subclasses wishing to persist
	 * settings for their controls should extend the hook method 
	 * <code>internalSaveWidgetValues</code>.
	 */
	public final void saveWidgetValues() {
		// update directory names history
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			String[] directoryNames= settings.getArray(STORE_DESTINATION_NAMES);
			if (directoryNames == null)
				directoryNames= new String[0];
			directoryNames= addToHistory(directoryNames, getDestinationValue());
			settings.put(STORE_DESTINATION_NAMES, directoryNames);

			settings.put(STORE_EXPORT_CLASS_FILES, fJarPackage.areClassFilesExported());
			settings.put(STORE_EXPORT_JAVA_FILES, fJarPackage.areJavaFilesExported());

			// options
			settings.put(STORE_COMPRESS, fJarPackage.isCompressed());
			settings.put(STORE_OVERWRITE, fJarPackage.allowOverwrite());
			settings.put(STORE_SAVE_DESCRIPTION, fJarPackage.isDescriptionSaved());
			settings.put(STORE_DESCRIPTION_LOCATION, fJarPackage.getDescriptionLocation().toString());
		}
		// Allow subclasses to save values
		internalSaveWidgetValues();
	}
	/**
	 * Hook method for subclasses to persist their settings.
	 */
	protected void internalSaveWidgetValues() {
	}
	/**
	 *	Hook method for restoring widget values to the values that they held
	 *	last time this wizard was used to completion.
	 */
	protected void restoreWidgetValues() {
		if (fJarPackage.isInitializedFromDialog())
			initializeJarPackage();

		fExportClassFilesCheckbox.setSelection(fJarPackage.areClassFilesExported());
		fExportJavaFilesCheckbox.setSelection(fJarPackage.areJavaFilesExported());

		// destination
		if (fJarPackage.getJarLocation().isEmpty())
			fDestinationNamesCombo.setText("");
		else
			fDestinationNamesCombo.setText(fJarPackage.getJarLocation().toOSString());
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			String[] directoryNames= settings.getArray(STORE_DESTINATION_NAMES);
			if (directoryNames == null)
				return; // ie.- no settings stored
			if (! fDestinationNamesCombo.getText().equals(directoryNames[0]))
				fDestinationNamesCombo.add(fDestinationNamesCombo.getText());
			for (int i= 0; i < directoryNames.length; i++)
				fDestinationNamesCombo.add(directoryNames[i]);
		}
		
		// options
		fCompressCheckbox.setSelection(fJarPackage.isCompressed());
		fOverwriteCheckbox.setSelection(fJarPackage.allowOverwrite());
		fSaveDescriptionCheckbox.setSelection(fJarPackage.isDescriptionSaved());
		fDescriptionFileText.setText(fJarPackage.getDescriptionLocation().toString());
	}
	/**
	 *	Initializes the JAR package from last used wizard page values.
	 */
	protected void initializeJarPackage() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			// source
			fJarPackage.setSelectedResources(getSelectedResources());
			fJarPackage.setExportClassFiles(settings.getBoolean(STORE_EXPORT_CLASS_FILES));
			fJarPackage.setExportJavaFiles(settings.getBoolean(STORE_EXPORT_JAVA_FILES));
						
			// destination
			String[] directoryNames= settings.getArray(STORE_DESTINATION_NAMES);
			if (directoryNames == null)
				return; // ie.- no settings stored
			fJarPackage.setJarLocation(getPathFromString(directoryNames[0]));

			// options
			fJarPackage.setCompress(settings.getBoolean(STORE_COMPRESS));
			fJarPackage.setOverwrite(settings.getBoolean(STORE_OVERWRITE));
			fJarPackage.setSaveDescription(settings.getBoolean(STORE_SAVE_DESCRIPTION));
			fJarPackage.setDescriptionLocation(getPathFromString(settings.get(STORE_DESCRIPTION_LOCATION)));
		}
	}
	/**
	 *	Stores the widget values in the JAR package.
	 */
	protected void updateModel() {
		if (getControl() == null)
			return;
		
		// source
		fJarPackage.setSelectedResources(getSelectedResources());
		fJarPackage.setExportClassFiles(fExportClassFilesCheckbox.getSelection());
		fJarPackage.setExportJavaFiles(fExportJavaFilesCheckbox.getSelection());

		// destination
		fJarPackage.setJarLocation(getPathFromString(fDestinationNamesCombo.getText()));

		// options
		fJarPackage.setCompress(fCompressCheckbox.getSelection());
		fJarPackage.setOverwrite(fOverwriteCheckbox.getSelection());
		fJarPackage.setSaveDescription(fSaveDescriptionCheckbox.getSelection());
		fJarPackage.setDescriptionLocation(new Path(fDescriptionFileText.getText()));
	}

	protected IPath getPathFromString(String text) {
		return new Path(text).makeAbsolute();
	}
	/**
	 * Returns a boolean indicating whether the directory portion of the
	 * passed pathname is valid and available for use.
	 *
	 * @return boolean

	protected boolean ensureTargetDirectoryIsValid(String fullPathname) {
		int separatorIndex= fullPathname.lastIndexOf(File.separator);

		if (separatorIndex == -1) // ie.- default dir, which is fine
			return true;

		return ensureTargetIsValid(new File(fullPathname.substring(0, separatorIndex)));
	}
		 */
	/**
	 *	If the target for export does not exist then attempt to create it.
	 *	Answer a boolean indicating whether the target exists (ie.- if it
	 *	either pre-existed or this method was able to create it)
	 *
	 *	@return boolean
	 *
	protected boolean ensureTargetIsValid(File targetDirectory) {
		if (targetDirectory.exists() && !targetDirectory.isDirectory()) {
			displayErrorDialog("Target directory already exists as a file.");
			fDestinationNamesCombo.setFocus();
			return false;
		}

		return ensureDirectoryExists(targetDirectory);
	}
	 */
	
	/**
	 * Attempts to ensure that the specified directory exists on the local file system.
	 * Answers a boolean indicating success.
	 *
	 * @return boolean
	 * @param directory java.io.File
	protected boolean ensureDirectoryExists(File directory) {
		if (!directory.exists()) {
			if (!queryYesNoQuestion("Target directory does not exist.  Would you like to create it?"))
				return false;

			if (!directory.mkdirs()) {
				displayErrorDialog("Target directory could not be created.");
				fDestinationNamesCombo.setFocus();
				return false;
			}
		}

		return true;
	}
	*/
	/**
	 * Returns a boolean indicating whether the passed File handle is
	 * is valid and available for use.
	 *
	 * @return boolean
	 */
	protected boolean ensureTargetFileIsValid(File targetFile) {
		if (targetFile.exists() && targetFile.isDirectory()) {
			setErrorMessage("Export destination must be a JAR file, not a directory.");
			fDestinationNamesCombo.setFocus();
			return false;
		}
		if (targetFile.exists()) {
			if (!targetFile.canWrite()) {
				setErrorMessage("JAR file already exists and cannot be overwritten.");
				fDestinationNamesCombo.setFocus();
				return false;
			}
		}
		return true;
	}
	/**
	 * Ensures that the target output file and its containing directory are
	 * both valid and able to be used.  Answer a boolean indicating these.
	 *
	 * @return boolean
	protected boolean ensureTargetIsValid() {
		String targetPath= getDestinationValue();
		if (!ensureTargetDirectoryIsValid(targetPath))
			return false;
		if (!ensureTargetFileIsValid(new File(targetPath)))
			return false;
		return true;
	}
	*/
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected boolean allowNewContainerName() {
		return true;
	}
	/*
	 * Overrides method from WizardExportPage
	 */
	protected void createDestinationGroup(Composite parent) {
		// destination specification group
		Composite destinationSelectionGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		destinationSelectionGroup.setLayout(layout);
		destinationSelectionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		new Label(destinationSelectionGroup, SWT.NONE).setText(getDestinationLabel());

		// destination name entry field
		fDestinationNamesCombo= new Combo(destinationSelectionGroup, SWT.SINGLE | SWT.BORDER);
		fDestinationNamesCombo.addListener(SWT.Modify, this);
		fDestinationNamesCombo.addListener(SWT.Selection, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fDestinationNamesCombo.setLayoutData(data);

		// destination browse button
		fDestinationBrowseButton= new Button(destinationSelectionGroup, SWT.PUSH);
		fDestinationBrowseButton.setText("Browse...");
		fDestinationBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fDestinationBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDestinationBrowseButtonPressed();
			}
		});

	//	new Label(parent, SWT.NONE); // vertical spacer
	}
	/**
	 *	Open an appropriate destination browser so that the user can specify a source
	 *	to import from
	 */
	protected void handleDescriptionFileBrowseButtonPressed() {
		SaveAsDialog dialog= new SaveAsDialog(getContainer().getShell());
		dialog.create();
		dialog.getShell().setText("Save As");
		dialog.setMessage("Select location and name for the descripton");
		dialog.setOriginalFile(createFileHandle(fJarPackage.getDescriptionLocation()));
		if (dialog.open() == dialog.OK)
			fDescriptionFileText.setText(dialog.getResult().toString());
	}
	/**
	 *	Open an appropriate destination browser so that the user can specify a source
	 *	to import from
	 */
	protected void handleDestinationBrowseButtonPressed() {
		FileDialog dialog= new FileDialog(getContainer().getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.jar"});

		String currentSourceString= getDestinationValue();
		int lastSeparatorIndex= currentSourceString.lastIndexOf(File.separator);
		if (lastSeparatorIndex != -1) {
			dialog.setFilterPath(currentSourceString.substring(0, lastSeparatorIndex));
			dialog.setFileName(currentSourceString.substring(lastSeparatorIndex + 1, currentSourceString.length()));
		}
		else
			dialog.setFileName(currentSourceString);
		String selectedFileName= dialog.open();

		if (selectedFileName != null) {
			IPath path= getPathFromString(selectedFileName);
			if (path.lastSegment().equals(getOutputSuffix()))
				selectedFileName= "";
			fDestinationNamesCombo.setText(selectedFileName);
		}
	}
	/**
	 * Returns the resource for the specified path.
	 *
	 * @param path	the path for which the resource should be returned
	 * @return the resource specified by the path or <code>null</code>
	 */
	protected IResource findResource(IPath path) {
		IWorkspace workspace = JavaPlugin.getWorkspace();
		IStatus result= workspace.validatePath(
							path.toString(),
							IResource.ROOT | IResource.PROJECT | IResource.FOLDER | IResource.FILE);
		if (result.isOK() && workspace.getRoot().exists(path))
			return workspace.getRoot().findMember(path);
		return null;
	}
	/**
	 * Creates the export resource options specification controls.
	 *
	 * @param parent the parent control
	 */
	protected void createResourceOptionsGroup(Composite parent) {
		Composite optionsGroup= new Composite(parent, SWT.NONE);
		GridLayout optionsLayout= new GridLayout();
		optionsLayout.marginHeight= 0;
		optionsGroup.setLayout(optionsLayout);

		fExportClassFilesCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fExportClassFilesCheckbox.setText("Export generated class files and resources");
		fExportClassFilesCheckbox.addListener(SWT.Selection, this);

		fExportJavaFilesCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fExportJavaFilesCheckbox.setText("Export java source files and resources");
		fExportJavaFilesCheckbox.addListener(SWT.Selection, this);

//		fExportResourcesCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
//		fExportResourcesCheckbox.setText("Export resources");
//		fExportResourcesCheckbox.addListener(SWT.Selection, this);
	}
	/**
	 * Updates the enablements of this page's controls. Subclasses may extend.
	 */
	protected void updateWidgetEnablements() {
		boolean saveDescription= fSaveDescriptionCheckbox.getSelection();
		fDescriptionFileGroup.setEnabled(saveDescription);
		fDescriptionFileBrowseButton.setEnabled(saveDescription);
		fDescriptionFileText.setEnabled(saveDescription);
		fDescriptionFileLabel.setEnabled(saveDescription);
	}
	/*
	 * Overrides method from IJarPackageWizardPage
	 */
	public boolean computePageCompletion() {
		return super.determinePageCompletion();
	}
	/*
	 * Implements method from Listener
	 */	
	public void handleEvent(Event e) {
		if (getControl() == null)
			return;
		updateModel();
		updateWidgetEnablements();
		updatePageCompletion();
	}
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected void createDescriptionFileGroup(Composite parent) {
		// destination specification group
		fDescriptionFileGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		fDescriptionFileGroup.setLayout(layout);
		fDescriptionFileGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		fDescriptionFileLabel= new Label(fDescriptionFileGroup, SWT.NONE);
		fDescriptionFileLabel.setText("Description file:");

		// destination name entry field
		fDescriptionFileText= new Text(fDescriptionFileGroup, SWT.SINGLE | SWT.BORDER);
		fDescriptionFileText.addListener(SWT.Modify, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fDescriptionFileText.setLayoutData(data);

		// destination browse button
		fDescriptionFileBrowseButton= new Button(fDescriptionFileGroup, SWT.PUSH);
		fDescriptionFileBrowseButton.setText("Browse...");
		fDescriptionFileBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fDescriptionFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDescriptionFileBrowseButtonPressed();
			}
		});

//		new Label(parent, SWT.NONE); // vertical spacer
	}
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected boolean validateDestinationGroup() {
		if (fDestinationNamesCombo.getText().length() == 0){
			setErrorMessage("Invalid JAR location.");
			return false;
		}
		return ensureTargetFileIsValid(fJarPackage.getJarLocation().toFile());
	}
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected boolean validateOptionsGroup() {
		if (fSaveDescriptionCheckbox.getSelection() && fDescriptionFileText.getText().length() == 0) {
			setErrorMessage("Invalid description file.");
			return false;
		}
		String fileExtension= fJarPackage.getDescriptionLocation().getFileExtension();
		if (fileExtension == null || !fileExtension.equals(JarPackage.DESCRIPTION_EXTENSION)) {
			setErrorMessage("Description file extension must be '.jardesc'");
			return false;
		}
		return true;
	}
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected boolean validateSourceGroup() {
		if (!fExportClassFilesCheckbox.getSelection()
				&& !fExportJavaFilesCheckbox.getSelection()) {
			setErrorMessage("No export type checked.");
			return false;
		}
		return true;
	}
	/*
	 * Overwrites method from WizardExportPage
	 */
	protected IPath getResourcePath() {
		return getPathFromText(fSourceNameField);
	}
	/**
	 * Creates a file resource handle for the file with the given workspace path.
	 * This method does not create the file resource; this is the responsibility
	 * of <code>createFile</code>.
	 *
	 * @param filePath the path of the file resource to create a handle for
	 * @return the new file resource handle
	 * @see #createFile
	 */
	protected IFile createFileHandle(IPath filePath) {
		if (filePath.isValidPath(filePath.toString()) && filePath.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(filePath);
		else
			return null;
	}
	/**
	 * Set the current input focus to self's destination entry field
 	 */
	protected void giveFocusToDestination() {
		fDestinationNamesCombo.setFocus();
	}
	/* 
	 * Method declared on IWizardPage.
	 */
	public void setPreviousPage(IWizardPage page) {
		super.setPreviousPage(page);
		if (getControl() != null)
			updatePageCompletion();
	}
}
