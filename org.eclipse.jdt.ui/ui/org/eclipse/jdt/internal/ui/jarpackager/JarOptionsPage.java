/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.resource.JFaceResources;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.dialogs.SaveAsDialog;

/**
 *	Page 2 of the JAR Package wizard
 */
public class JarOptionsPage extends WizardPage implements Listener, IJarPackageWizardPage {

	// Constants
	protected static final int SIZING_TEXT_FIELD_WIDTH = 250;

	private JarPackage fJarPackage;

	// widgets
	private Button		fShowErrorsCheckbox;
	private Button		fShowWarningsCheckbox;
	private Button		fExportErrorsCheckbox;
	private Button		fExportWarningsCheckbox;
	private Composite	fDescriptionFileGroup;
	private Button		fSaveDescriptionCheckbox;
	private Label		fDescriptionFileLabel;
	private Text		fDescriptionFileText;
	private Button		fDescriptionFileBrowseButton;

	// dialog store id constants
	private final static String PAGE_NAME= "JarPackageWizardPage";
	
	private final static String STORE_SHOW_WARNINGS= PAGE_NAME + ".SHOW_WARNINGS";
	private final static String STORE_SHOW_ERRORS= PAGE_NAME + ".SHOW_ERRORS";
	private final static String STORE_EXPORT_WARNINGS= PAGE_NAME + ".EXPORT_WARNINGS";
	private final static String STORE_EXPORT_ERRORS= PAGE_NAME + ".EXPORT_ERRORS";
	private final static String STORE_SAVE_DESCRIPTION= PAGE_NAME + ".SAVE_DESCRIPTION";
	private final static String STORE_DESCRIPTION_LOCATION= PAGE_NAME + ".DESCRIPTION_LOCATION";

	/**
	 *	Create an instance of this class
	 */
	public JarOptionsPage(JarPackage jarPackage) {
		super("jarOptionsWizardPage");
		setTitle("JAR Packaging Options");
		setDescription("Define the options for the JAR export");
		fJarPackage= jarPackage;
	}
	/*
	 * Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(
			new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createOptionsGroup(composite);

		restoreWidgetValues();

		setControl(composite);

		updateModel();
		updateWidgetEnablements();
		updatePageCompletion();
	}
	/**
	 *	Create the export options specification widgets.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 */
	protected void createOptionsGroup(Composite parent) {
		
		initializeDialogUnits(parent);
		
		Composite optionsGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		optionsGroup.setLayout(layout);

		createLabel(optionsGroup, "How should problems be reported?", false);
		fShowWarningsCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fShowWarningsCheckbox.setText("Show warnings when export is done");
		fShowWarningsCheckbox.addListener(SWT.Selection, this);

		fShowErrorsCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fShowErrorsCheckbox.setText("Show errors when export is done");
		fShowErrorsCheckbox.addListener(SWT.Selection, this);

		createSpacer(optionsGroup);
		createLabel(optionsGroup, "How should class files with problems be treated?", false);

		fExportErrorsCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fExportErrorsCheckbox.setText("Export class files with compile errors");
		fExportErrorsCheckbox.addListener(SWT.Selection, this);

		fExportWarningsCheckbox	= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fExportWarningsCheckbox.setText("Export class files with compile warnings");
		fExportWarningsCheckbox.addListener(SWT.Selection, this);

		createSpacer(optionsGroup);
//		createLabel(optionsGroup, "What do you want to export?", false);

		fSaveDescriptionCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		fSaveDescriptionCheckbox.setText("Save the description of this JAR in the workspace");
		fSaveDescriptionCheckbox.addListener(SWT.Selection, this);

		createDescriptionFileGroup(parent);
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
			settings.put(STORE_EXPORT_WARNINGS, fJarPackage.exportWarnings());
			settings.put(STORE_EXPORT_ERRORS, fJarPackage.exportErrors());
			settings.put(STORE_SHOW_WARNINGS, fJarPackage.logWarnings());
			settings.put(STORE_SHOW_ERRORS, fJarPackage.logErrors());
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
		if (!fJarPackage.isUsedToInitialize())
			initializeJarPackage();

		fShowWarningsCheckbox.setSelection(fJarPackage.logWarnings());
		fShowErrorsCheckbox.setSelection(fJarPackage.logErrors());
		fExportWarningsCheckbox.setSelection(fJarPackage.exportWarnings());
		fExportErrorsCheckbox.setSelection(fJarPackage.exportErrors());
		fSaveDescriptionCheckbox.setSelection(fJarPackage.isDescriptionSaved());
		fDescriptionFileText.setText(fJarPackage.getDescriptionLocation().toString());
	}
	/**
	 *	Initializes the JAR package from last used wizard page values.
	 */
	protected void initializeJarPackage() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			fJarPackage.setLogErrors(settings.getBoolean(STORE_SHOW_WARNINGS));
			fJarPackage.setLogWarnings(settings.getBoolean(STORE_SHOW_ERRORS));
			fJarPackage.setExportWarnings(settings.getBoolean(STORE_EXPORT_WARNINGS));
			fJarPackage.setExportErrors(settings.getBoolean(STORE_EXPORT_ERRORS));
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
		fJarPackage.setExportWarnings(fExportWarningsCheckbox.getSelection());
		fJarPackage.setExportErrors(fExportErrorsCheckbox.getSelection());
		fJarPackage.setLogErrors(fShowErrorsCheckbox.getSelection());
		fJarPackage.setLogWarnings(fShowWarningsCheckbox.getSelection());
		fJarPackage.setSaveDescription(fSaveDescriptionCheckbox.getSelection());
		fJarPackage.setDescriptionLocation(new Path(fDescriptionFileText.getText()));
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
		if (dialog.open() == dialog.OK) {
			IPath path= dialog.getResult();
			path= path.removeFileExtension().addFileExtension("jardesc");
			fDescriptionFileText.setText(path.toString());
		}
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
	 * Implements method from IJarPackageWizardPage
	 */
	public boolean computePageCompletion() {
		if (fJarPackage.isDescriptionSaved()){
			if (fJarPackage.getDescriptionLocation().toString().length() == 0) {
				setErrorMessage(null);
				return false;
			}
			IPath location= fJarPackage.getDescriptionLocation();
			if (!location.toString().startsWith("/")) {
				setErrorMessage("Description file path must be absolute (start with /)");
				return false;
			}			
			IResource resource= findResource(location);
			if (resource != null && resource.getType() != IResource.FILE) {
				setErrorMessage("The description file location must not be an existing container");
				return false;
			}
			resource= findResource(location.removeLastSegments(1));
			if (resource == null || resource.getType() == IResource.FILE) {
				setErrorMessage("Container for description file does not exist");
				return false;
			}
			String fileExtension= fJarPackage.getDescriptionLocation().getFileExtension();
			if (fileExtension == null || !fileExtension.equals(JarPackage.DESCRIPTION_EXTENSION)) {
				setErrorMessage("Description file extension must be '.jardesc'");
				return false;
			}
		}
		return true;
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
	/* 
	 * Method declared on IWizardPage.
	 */
	public void setPreviousPage(IWizardPage page) {
		super.setPreviousPage(page);
		if (getControl() != null)
			updatePageCompletion();
	}
	/**
	 * Creates a new label with a bold font.
	 *
	 * @param parent the parent control
	 * @param text the label text
	 * @return the new label control
	 */
	protected Label createLabel(Composite parent, String text, boolean bold) {
		Label label = new Label(parent, SWT.NONE);
		if (bold)
			label.setFont(JFaceResources.getBannerFont());
		label.setText(text);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Creates a horizontal spacer line that fills the width of its container.
	 *
	 * @param parent the parent control
	 */
	protected void createSpacer(Composite parent) {
		Label spacer = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		spacer.setLayoutData(data);
	}
	/**
	 * Determine if the page is complete and update the page appropriately. 
	 */
	protected void updatePageCompletion() {
		boolean pageComplete= computePageCompletion();
		setPageComplete(pageComplete);
		if (pageComplete) {
			setErrorMessage(null);
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

	protected IPath getPathFromString(String text) {
		return new Path(text).makeAbsolute();
	}
}