/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.packageview.EmptyInnerPackageFilter;

/**
 *	Page 1 of the JAR Package wizard
 */
public class JarPackageWizardPage extends WizardExportResourcesPage implements IJarPackageWizardPage {

	private JarPackage fJarPackage;
	private IStructuredSelection fInitialSelection;
	private CheckboxTreeAndListGroup fInputGroup;

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
	private final static String PAGE_NAME= "JarPackageWizardPage"; //$NON-NLS-1$
	
	private final static String STORE_EXPORT_CLASS_FILES= PAGE_NAME + ".EXPORT_CLASS_FILES"; //$NON-NLS-1$
	private final static String STORE_EXPORT_JAVA_FILES= PAGE_NAME + ".EXPORT_JAVA_FILES"; //$NON-NLS-1$
	
	private final static String STORE_DESTINATION_NAMES= PAGE_NAME + ".DESTINATION_NAMES_ID"; //$NON-NLS-1$
	
	private final static String STORE_COMPRESS= PAGE_NAME + ".COMPRESS"; //$NON-NLS-1$
	private final static String STORE_OVERWRITE= PAGE_NAME + ".OVERWRITE"; //$NON-NLS-1$
	private final static String STORE_SAVE_DESCRIPTION= PAGE_NAME + ".SAVE_DESCRIPTION"; //$NON-NLS-1$
	private final static String STORE_DESCRIPTION_LOCATION= PAGE_NAME + ".DESCRIPTION_LOCATION"; //$NON-NLS-1$

	// other constants
	private final static int SIZING_SELECTION_WIDGET_WIDTH= 400;
	private final static int SIZING_SELECTION_WIDGET_HEIGHT= 150;
	
	/**
	 *	Create an instance of this class
	 */
	public JarPackageWizardPage(JarPackage jarPackage, IStructuredSelection selection) {
		super(PAGE_NAME, selection);
		setTitle(JarPackagerMessages.getString("JarPackageWizardPage.title")); //$NON-NLS-1$
		setDescription(JarPackagerMessages.getString("JarPackageWizardPage.description")); //$NON-NLS-1$
		fJarPackage= jarPackage;
		fInitialSelection= selection;
	}
	/*
	 * Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(
			new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createPlainLabel(composite, JarPackagerMessages.getString("JarPackageWizardPage.whatToExport.label")); //$NON-NLS-1$
		createInputGroup(composite);

//		createButtonsGroup(composite);
		createExportTypeGroup(composite);

//		createSpacer(composite);
		new Label(composite, SWT.NONE); // vertical spacer


		createPlainLabel(composite, JarPackagerMessages.getString("JarPackageWizardPage.whereToExport.label")); //$NON-NLS-1$
		createDestinationGroup(composite);

//		createSpacer(composite);

		createPlainLabel(composite, JarPackagerMessages.getString("JarPackageWizardPage.options.label")); //$NON-NLS-1$
		createOptionsGroup(composite);

		restoreResourceSpecificationWidgetValues(); // superclass API defines this hook
		restoreWidgetValues();

		if (fInitialSelection != null)
			setupBasedOnInitialSelections();

		setControl(composite);

		updateModel();
		updateWidgetEnablements();
		updatePageCompletion();

		giveFocusToDestination();
		
		WorkbenchHelp.setHelp(composite, new DialogPageContextComputer(this, IJavaHelpContextIds.JARPACKAGER_WIZARD_PAGE));								
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
		fCompressCheckbox.setText(JarPackagerMessages.getString("JarPackageWizardPage.compress.text")); //$NON-NLS-1$
		fCompressCheckbox.addListener(SWT.Selection, this);

		fOverwriteCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fOverwriteCheckbox.setText(JarPackagerMessages.getString("JarPackageWizardPage.overwrite.text")); //$NON-NLS-1$
		fOverwriteCheckbox.addListener(SWT.Selection, this);
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
		return JarPackagerMessages.getString("JarPackageWizardPage.destination.label"); //$NON-NLS-1$
	}
	/**
	 *	Answer the suffix that files exported from this wizard must have.
	 *	If this suffix is a file extension (which is typically the case)
	 *	then it must include the leading period character.
	 *
	 *	@return java.lang.String
	 */
	protected String getOutputSuffix() {
		return "." + JarPackage.EXTENSION; //$NON-NLS-1$
	}
	/**
	 * Returns an iterator over this page's collection of currently-specified 
	 * elements to be exported. This is the primary element selection facility
	 * accessor for subclasses.
	 *
	 * @return an iterator over the collection of elements currently selected for export
	 */
	protected Iterator getSelectedResourcesIterator() {
		return fInputGroup.getAllCheckedListItems();
	}
	/**
	 * Returns this page's collection of containers in which at least one element
	 * is selected for export.
	 *
	 * @return a set containers that contain at least one element currently selected for export
	 */
	protected Set getSelectedContainers() {
		return fInputGroup.getAllCheckedTreeItems();
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
		if (!fJarPackage.isUsedToInitialize())
			initializeJarPackage();

		fExportClassFilesCheckbox.setSelection(fJarPackage.areClassFilesExported());
		fExportJavaFilesCheckbox.setSelection(fJarPackage.areJavaFilesExported());

		// destination
		if (fJarPackage.getJarLocation().isEmpty())
			fDestinationNamesCombo.setText(""); //$NON-NLS-1$
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
	}
	/**
	 *	Initializes the JAR package from last used wizard page values.
	 */
	protected void initializeJarPackage() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			// source
			fJarPackage.setSelectedElements(getSelectedResources());
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
		}
	}
	/**
	 *	Stores the widget values in the JAR package.
	 */
	protected void updateModel() {
		if (getControl() == null)
			return;
		
		// source
		fJarPackage.setSelectedElements(getSelectedResources());
		fJarPackage.setExportClassFiles(fExportClassFilesCheckbox.getSelection());
		fJarPackage.setExportJavaFiles(fExportJavaFilesCheckbox.getSelection());

		// destination
		fJarPackage.setJarLocation(getPathFromString(fDestinationNamesCombo.getText()));

		// options
		fJarPackage.setCompress(fCompressCheckbox.getSelection());
		fJarPackage.setOverwrite(fOverwriteCheckbox.getSelection());
		/*
		fJarPackage.setSaveDescription(fSaveDescriptionCheckbox.getSelection());
		fJarPackage.setDescriptionLocation(new Path(fDescriptionFileText.getText()));
		*/
	}

	protected IPath getPathFromString(String text) {
		return new Path(text).makeAbsolute();
	}
	/**
	 * Returns a boolean indicating whether the passed File handle is
	 * is valid and available for use.
	 *
	 * @return boolean
	 */
	protected boolean ensureTargetFileIsValid(File targetFile) {
		if (targetFile.exists() && targetFile.isDirectory()) {
			setErrorMessage(JarPackagerMessages.getString("JarPackageWizardPage.error.exportDestinationMustNotBeDirectory")); //$NON-NLS-1$
			fDestinationNamesCombo.setFocus();
			return false;
		}
		if (targetFile.exists()) {
			if (!targetFile.canWrite()) {
				setErrorMessage(JarPackagerMessages.getString("JarPackageWizardPage.error.jarFileExistsAndNotWritable")); //$NON-NLS-1$
				fDestinationNamesCombo.setFocus();
				return false;
			}
		}
		return true;
	}
	/*
	 * Overrides method from WizardExportPage
	 */
	protected void createDestinationGroup(Composite parent) {
		
		initializeDialogUnits(parent);
		
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
		fDestinationBrowseButton.setText(JarPackagerMessages.getString("JarPackageWizardPage.browseButton.text")); //$NON-NLS-1$
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
		dialog.getShell().setText(JarPackagerMessages.getString("JarPackageWizardPage.saveAsDialog.title")); //$NON-NLS-1$
		dialog.setMessage(JarPackagerMessages.getString("JarPackageWizardPage.saveAsDialog.message")); //$NON-NLS-1$
		dialog.setOriginalFile(createFileHandle(fJarPackage.getDescriptionLocation()));
		if (dialog.open() == dialog.OK) {
			IPath path= dialog.getResult();
			path= path.removeFileExtension().addFileExtension(JarPackage.DESCRIPTION_EXTENSION);
			fDescriptionFileText.setText(path.toString());
		}
	}
	/**
	 *	Open an appropriate destination browser so that the user can specify a source
	 *	to import from
	 */
	protected void handleDestinationBrowseButtonPressed() {
		FileDialog dialog= new FileDialog(getContainer().getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.jar"}); //$NON-NLS-1$

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
				selectedFileName= ""; //$NON-NLS-1$
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
	 * Creates the checkbox tree and list for selecting resources.
	 *
	 * @param parent the parent control
	 */
	protected void createInputGroup(Composite parent) {
		int labelFlags= JavaElementLabelProvider.SHOW_BASICS
						| JavaElementLabelProvider.SHOW_OVERLAY_ICONS
						| JavaElementLabelProvider.SHOW_SMALL_ICONS;
		fInputGroup= new CheckboxTreeAndListGroup(
					parent,
					JavaCore.create(JavaPlugin.getDefault().getWorkspace().getRoot()),
					new JavaElementContentProvider(),
					new JavaElementLabelProvider(labelFlags),
					new JavaElementModalListContentProvider(),
					new JavaElementLabelProvider(labelFlags),
					SWT.NONE,
					SIZING_SELECTION_WIDGET_WIDTH,
					SIZING_SELECTION_WIDGET_HEIGHT);
		fInputGroup.addTreeFilter(new EmptyInnerPackageFilter());
		fInputGroup.addTreeFilter(ContainerFilter.getNotContainersFilter());
		fInputGroup.addTreeFilter(new LibraryFilter());
		fInputGroup.addListFilter(ContainerFilter.getContainersFilter());
		fInputGroup.getTree().addListener(SWT.MouseUp, this);
		fInputGroup.getTable().addListener(SWT.MouseUp, this);
	}
	/**
	 * Creates the export type controls.
	 *
	 * @param parent the parent control
	 */
	protected void createExportTypeGroup(Composite parent) {		
		Composite optionsGroup= new Composite(parent, SWT.NONE);
		GridLayout optionsLayout= new GridLayout();
		optionsLayout.marginHeight= 0;
		optionsGroup.setLayout(optionsLayout);
		fExportClassFilesCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fExportClassFilesCheckbox.setText(JarPackagerMessages.getString("JarPackageWizardPage.exportClassFiles.text")); //$NON-NLS-1$
		fExportClassFilesCheckbox.addListener(SWT.Selection, this);

		fExportJavaFilesCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fExportJavaFilesCheckbox.setText(JarPackagerMessages.getString("JarPackageWizardPage.exportJavaFiles.text")); //$NON-NLS-1$
		fExportJavaFilesCheckbox.addListener(SWT.Selection, this);
	}
	/**
	 * Updates the enablements of this page's controls. Subclasses may extend.
	 */
	protected void updateWidgetEnablements() {
	}
	/*
	 * Overrides method from IJarPackageWizardPage
	 */
	public boolean computePageCompletion() {
		setErrorMessage(null);
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
		fDescriptionFileLabel.setText(JarPackagerMessages.getString("JarPackageWizardPage.descriptionFile.text")); //$NON-NLS-1$

		// destination name entry field
		fDescriptionFileText= new Text(fDescriptionFileGroup, SWT.SINGLE | SWT.BORDER);
		fDescriptionFileText.addListener(SWT.Modify, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fDescriptionFileText.setLayoutData(data);

		// destination browse button
		fDescriptionFileBrowseButton= new Button(fDescriptionFileGroup, SWT.PUSH);
		fDescriptionFileBrowseButton.setText(JarPackagerMessages.getString("JarPackageWizardPage.browseButton.text")); //$NON-NLS-1$
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
		if (fDestinationNamesCombo.getText().length() == 0) {
//			setErrorMessage("Invalid JAR location.");
			return false;
		}
		if (fJarPackage.getJarLocation().toString().endsWith("/")) { //$NON-NLS-1$
			setErrorMessage(JarPackagerMessages.getString("JarPackageWizardPage.error.exportDestinationMustNotBeDirectory")); //$NON-NLS-1$
			fDestinationNamesCombo.setFocus();
			return false;
		}
		return ensureTargetFileIsValid(fJarPackage.getJarLocation().toFile());
	}
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected boolean validateOptionsGroup() {
		return true;
	}
	/*
	 * Overrides method from WizardDataTransferPage
	 */
	protected boolean validateSourceGroup() {
		if (!fExportClassFilesCheckbox.getSelection()
				&& !fExportJavaFilesCheckbox.getSelection()) {
			setErrorMessage(JarPackagerMessages.getString("JarPackageWizardPage.error.noExportTypeChecked")); //$NON-NLS-1$
			return false;
		}
		if (getSelectedResources().size() == 0) {
			return false;
		}
		return true;
	}
	/*
	 * Overwrides method from WizardExportPage
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
	 * Overrides method from WizardExportResourcePage
	 */
	protected void setupBasedOnInitialSelections() {
		Iterator enum = fInitialSelection.iterator();
		while (enum.hasNext()) {
			Object selectedElement= enum.next();
			if (selectedElement instanceof ICompilationUnit || selectedElement instanceof IFile)
				fInputGroup.initialCheckListItem(selectedElement);
			else
				fInputGroup.initialCheckTreeItem(selectedElement);
		}
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
