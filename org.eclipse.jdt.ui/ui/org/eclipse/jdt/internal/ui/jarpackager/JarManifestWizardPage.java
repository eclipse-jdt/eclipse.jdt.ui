/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.ArrayList;import java.util.Arrays;import java.util.Iterator;import java.util.List;import java.util.Set;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Text;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.resource.JFaceResources;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.ui.dialogs.ListSelectionDialog;import org.eclipse.ui.dialogs.ResourceSelectionDialog;import org.eclipse.ui.dialogs.SaveAsDialog;import org.eclipse.ui.dialogs.SelectionDialog;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.SelectionStatusDialog;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 *	Page 3 of the JAR Package wizard
 */
public class JarManifestWizardPage extends WizardPage implements Listener, IJarPackageWizardPage {
	
	// Constants
	protected static final int SIZING_TEXT_FIELD_WIDTH = 250;

	// Model
	private JarPackage fJarPackage;
	
	// Widgets
	private Composite	fManifestGroup;
	private Button		fGenerateManifestRadioButton;
	private Button		fSaveManifestCheckbox;
	private Button		fReuseManifestCheckbox;
	private Text		fNewManifestFileText;
	private Label		fNewManifestFileLabel;
	private Button		fNewManifestFileBrowseButton;
	private Button		fUseManifestRadioButton;
	private Text		fManifestFileText;
	private Label		fManifestFileLabel;
	private Button		fManifestFileBrowseButton;
	
	private Label		fSealingHeaderLabel;
	private Button		fSealJarRadioButton;
	private Button		fSealedPackagesDetailsButton;
	private Button		fSealPackagesRadioButton;	
	private Button		fUnSealedPackagesDetailsButton;
	private Label		fSealingInfoLabel;
	
	private Label		fMainClassHeaderLabel;
	private Label		fMainClassLabel;
	private Text		fMainClassText;
	private Button		fMainClassBrowseButton;
	
	private Label		fDownloadHeaderLabel;
	private Label		fDownloadExtensionLabel;
	private Text		fDownloadExtensionText;
	
	// Dialog store id constants
	private final static String PAGE_NAME= "JarManifestWizardPage";
	
	// Manifest creation
	private final static String STORE_GENERATE_MANIFEST= PAGE_NAME + ".GENERATE_MANIFEST";
	private final static String STORE_SAVE_MANIFEST= PAGE_NAME + ".SAVE_MANIFEST";
	private final static String STORE_REUSE_MANIFEST= PAGE_NAME + ".REUSE_MANIFEST";
	private final static String STORE_MANIFEST_LOCATION= PAGE_NAME + ".MANIFEST_LOCATION";
	
	// Sealing
	private final static String STORE_SEAL_JAR= PAGE_NAME + ".SEAL_JAR";
	private final static String STORE_SEALED_PACKAGES= PAGE_NAME + ".SEALED_PACKAGES";
	private final static String STORE_UNSEALED_PACKAGES= PAGE_NAME + ".UNSEALED_PACKAGES";
	
	// Main class
	private final static String STORE_MAIN_CLASS_NAME= PAGE_NAME + ".MAIN_CLASS_NAME";
	
	// Download extension path

	private final static String STORE_DOWNLOAD_EXTENSIONS= PAGE_NAME + ".DOWNLOAD_EXTENSIONS";

	/**
	 *	Create an instance of this class
	 */
	public JarManifestWizardPage(JarPackage jarPackage) {
		super("jarPackageWizardPage");
		setTitle("JAR Manifest Specification");
		setDescription("Customize the manifest file for the JAR package");
		fJarPackage= jarPackage;
	}

	// ----------- Widget creation  -----------

	/*
	 * Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(
			new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createLabel(composite, "Where does the manifest come from?", false);
		createManifestGroup(composite);

		createSpacer(composite);

		fSealingHeaderLabel= createLabel(composite, "Which packages must be sealed?", false);
		createSealingGroup(composite);

//		createSpacer(composite);
		new Label(composite, SWT.NONE); // vertical spacer

		fMainClassHeaderLabel= createLabel(composite, "Which class is the application's entry point?", false);
		createMainClassGroup(composite);

//		fDownloadHeaderLabel= createLabel(composite, "What are your download extensions?", false);
//		createDownloadExtensionGroup(composite);

		restoreWidgetValues();

		setControl(composite);

		updateModel();
		updateWidgetEnablements();
		setPageComplete(computePageCompletion());
		
	}
	/**
	 *	Create the export options specification widgets.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 */
	protected void createManifestGroup(Composite parent) {
		fManifestGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		fManifestGroup.setLayout(layout);

		fGenerateManifestRadioButton= new Button(fManifestGroup, SWT.RADIO | SWT.LEFT);
		fGenerateManifestRadioButton.setText("Generate the manifest file");
		fGenerateManifestRadioButton.addListener(SWT.Selection, this);

			Composite saveOptions= new Composite(fManifestGroup, SWT.NONE);
			GridLayout saveOptionsLayout= new GridLayout();
			saveOptionsLayout.marginHeight= 0;
			saveOptionsLayout.marginWidth= 20;
			saveOptions.setLayout(saveOptionsLayout);
	
			fSaveManifestCheckbox= new Button(saveOptions, SWT.CHECK | SWT.LEFT);
			fSaveManifestCheckbox.setText("Save the manifest in the workspace");
			fSaveManifestCheckbox.addListener(SWT.MouseUp, this);
	
			fReuseManifestCheckbox= new Button(saveOptions, SWT.CHECK | SWT.LEFT);
			fReuseManifestCheckbox.setText("Reuse and save the manifest in the workspace");
			
			createNewManifestFileGroup(saveOptions);

		fUseManifestRadioButton= new Button(fManifestGroup, SWT.RADIO | SWT.LEFT);
		fUseManifestRadioButton.setText("Use existing manifest from workspace");
			Composite existingManifestGroup= new Composite(fManifestGroup, SWT.NONE);
			GridLayout existingManifestLayout= new GridLayout();
			existingManifestLayout.marginHeight= 0;
			existingManifestLayout.marginWidth= 20;
			existingManifestGroup.setLayout(existingManifestLayout);
			createManifestFileGroup(existingManifestGroup);
	}

	protected void createNewManifestFileGroup(Composite parent) {
		// destination specification group
		Composite manifestFileGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		manifestFileGroup.setLayout(layout);
		manifestFileGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		fNewManifestFileLabel= new Label(manifestFileGroup, SWT.NONE);
		fNewManifestFileLabel.setText("Manifest file:");

		// entry field
		fNewManifestFileText = new Text(manifestFileGroup, SWT.SINGLE | SWT.BORDER);
		fNewManifestFileText.addListener(SWT.Modify, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fNewManifestFileText.setLayoutData(data);

		// browse button
		fNewManifestFileBrowseButton= new Button(manifestFileGroup, SWT.PUSH);
		fNewManifestFileBrowseButton.setText("Browse...");
		fNewManifestFileBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fNewManifestFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleNewManifestFileBrowseButtonPressed();
			}
		});
	}

	protected void createManifestFileGroup(Composite parent) {
		// destination specification group
		Composite manifestFileGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		manifestFileGroup.setLayout(layout);
		manifestFileGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		fManifestFileLabel= new Label(manifestFileGroup, SWT.NONE);
		fManifestFileLabel.setText("Manifest file:");

		// entry field
		fManifestFileText = new Text(manifestFileGroup, SWT.SINGLE | SWT.BORDER);
		fManifestFileText.addListener(SWT.Modify, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fManifestFileText.setLayoutData(data);

		// browse button
		fManifestFileBrowseButton= new Button(manifestFileGroup, SWT.PUSH);
		fManifestFileBrowseButton.setText("Browse...");
		fManifestFileBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fManifestFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleManifestFileBrowseButtonPressed();
			}
		});
	}
	/**
	 * Creates the JAR sealing specification controls.
	 *
	 * @param parent the parent control
	 */
	protected void createSealingGroup(Composite parent) {
		// destination specification group
		Composite sealingGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		sealingGroup.setLayout(layout);
		sealingGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		createSealJarGroup(sealingGroup);
		createSealPackagesGroup(sealingGroup);
		fSealingInfoLabel= new Label(sealingGroup, SWT.NONE);
	}
	/**
	 * Creates the JAR sealing specification controls to seal the whole JAR.
	 *
	 * @param parent the parent control
	 */
	protected void createSealJarGroup(Composite sealGroup) {
		fSealJarRadioButton= new Button(sealGroup, SWT.RADIO);
		fSealJarRadioButton.setText("Seal the JAR");
		fSealJarRadioButton.addListener(SWT.Selection, this);

		fUnSealedPackagesDetailsButton= new Button(sealGroup, SWT.PUSH);
		fUnSealedPackagesDetailsButton.setText("Details...");
		fUnSealedPackagesDetailsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleUnSealPackagesDetailsButtonPressed();
			}
		});
		
	}
	/**
	 * Creates the JAR sealing specification controls to seal packages.
	 *
	 * @param parent the parent control
	 */
	protected void createSealPackagesGroup(Composite sealGroup) {
		fSealPackagesRadioButton= new Button(sealGroup, SWT.RADIO);
		fSealPackagesRadioButton.setText("Seal some packages");

		fSealedPackagesDetailsButton= new Button(sealGroup, SWT.PUSH);
		fSealedPackagesDetailsButton.setText("Details...");
		fSealedPackagesDetailsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSealPackagesDetailsButtonPressed();
			}
		});
	}

	protected void createMainClassGroup(Composite parent) {
		// main type group
		Composite mainClassGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		mainClassGroup.setLayout(layout);
		mainClassGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		fMainClassLabel= new Label(mainClassGroup, SWT.NONE);
		fMainClassLabel.setText("Main-Class:");

		// entry field
		fMainClassText= new Text(mainClassGroup, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fMainClassText.addListener(SWT.Modify, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fMainClassText.setLayoutData(data);

		// browse button
		fMainClassBrowseButton= new Button(mainClassGroup, SWT.PUSH);
		fMainClassBrowseButton.setText("Browse...");
		fMainClassBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fMainClassBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleMainClassBrowseButtonPressed();
			}
		});
	}

	protected void createDownloadExtensionGroup(Composite parent) {
		// main type group
		Composite downloadExtensionGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		downloadExtensionGroup.setLayout(layout);
		downloadExtensionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		fDownloadExtensionLabel= new Label(downloadExtensionGroup, SWT.NONE);
		fDownloadExtensionLabel.setText("Class-Path:");

		// entry field
		fDownloadExtensionText= new Text(downloadExtensionGroup, SWT.SINGLE | SWT.BORDER);
		fDownloadExtensionText.addListener(SWT.Modify, this);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fDownloadExtensionText.setLayoutData(data);
	}
	
	// ----------- Event handers  -----------
		
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
	/**
	 *	Open an appropriate dialog so that the user can specify a manifest
	 *	to save
	 */
	protected void handleNewManifestFileBrowseButtonPressed() {
		// Use Save As dialog to select a new file inside the workspace
		SaveAsDialog dialog= new SaveAsDialog(getContainer().getShell());
		dialog.create();
		dialog.getShell().setText("Save As");
		dialog.setMessage("Select location and name for the manifest");
		dialog.setOriginalFile(createFileHandle(fJarPackage.getManifestLocation()));
		if (dialog.open() == dialog.OK) {
			fJarPackage.setManifestLocation(dialog.getResult());
			fNewManifestFileText.setText(dialog.getResult().toString());
		}
	}

	protected void handleManifestFileBrowseButtonPressed() {
		ResourceSelectionDialog dialog= new ResourceSelectionDialog(getContainer().getShell(), JavaPlugin.getWorkspace().getRoot(), "Select the manifest file");
		if (fJarPackage.doesManifestExist())
			dialog.setInitialSelections(new IResource[] {fJarPackage.getManifestFile()});
		if (dialog.open() ==  dialog.OK) {
			Object[] resources= dialog.getResult();
			if (resources.length != 1)
				setErrorMessage("Only one manifest must be selected");
			else {
				setErrorMessage("");
				fJarPackage.setManifestLocation(((IResource)resources[0]).getFullPath());
				fManifestFileText.setText(fJarPackage.getManifestLocation().toString());
			}
		}
	}

	protected void handleMainClassBrowseButtonPressed() {
		List resources= fJarPackage.getSelectedResources();
		if (resources == null) {
			setErrorMessage("No resources selected");
			return;
		}
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope((IResource[])resources.toArray(new IResource[resources.size()]));
		SelectionStatusDialog dialog= (SelectionStatusDialog)JavaUI.createMainTypeDialog(getContainer().getShell(), getContainer(), searchScope, 0, false);
		dialog.setTitle("Select Main Class");
		dialog.setMessage("Select the class which is the application's entry point");
		IType mainClass= fJarPackage.getMainClass();
		if (mainClass != null)
			dialog.setInitialSelections(new String[] {mainClass.getElementName()});
		else
			dialog.setInitialSelection(null);
		if (dialog.open() == dialog.OK) {
			fJarPackage.setMainClass((IType)dialog.getResult()[0]);
			fMainClassText.setText(fJarPackage.getMainClassName());
		} else if (!fJarPackage.isMainClassValid(getContainer())) {
			// user did not cancel: no types were found
			fJarPackage.setMainClass(null);
			fMainClassText.setText(fJarPackage.getMainClassName());
		}
	}

	protected void handleSealPackagesDetailsButtonPressed() {
		SelectionDialog dialog= createPackageDialog(fJarPackage.getPackagesForSelectedResources());
		dialog.setTitle("Seal Packages");
		dialog.setMessage("Select the packages which should be sealed in the JAR");
		dialog.setInitialSelections(fJarPackage.getPackagesToSeal());
		if (dialog.open() == dialog.OK) {
			fJarPackage.setPackagesToSeal(getPackagesFromDialog(dialog));
		}
		updateSealingInfo();
	}

	protected void handleUnSealPackagesDetailsButtonPressed() {
		SelectionDialog dialog= createPackageDialog(fJarPackage.getPackagesForSelectedResources());
		dialog.setTitle("Unseal Packages");
		dialog.setMessage("Select the packages which should be unsealed in the JAR");
		dialog.setInitialSelections(fJarPackage.getPackagesToUnseal());
		if (dialog.open() == dialog.OK) {
			fJarPackage.setPackagesToUnseal(getPackagesFromDialog(dialog));
		}
		updateSealingInfo();
	}
	/**
	 * Updates the enablements of this page's controls. Subclasses may extend.
	 */
	protected void updateWidgetEnablements() {
		boolean generate= fGenerateManifestRadioButton.getSelection();

		boolean save= generate && fSaveManifestCheckbox.getSelection();
		fSaveManifestCheckbox.setEnabled(generate);
		fReuseManifestCheckbox.setEnabled(fJarPackage.isDescriptionSaved() && save);
		fNewManifestFileText.setEnabled(save);
		fNewManifestFileLabel.setEnabled(save);
		fNewManifestFileBrowseButton.setEnabled(save);

		fManifestFileText.setEnabled(!generate);
		fManifestFileLabel.setEnabled(!generate);
		fManifestFileBrowseButton.setEnabled(!generate);

		fSealingHeaderLabel.setEnabled(generate);
		boolean sealState= fSealJarRadioButton.getSelection();
		fSealJarRadioButton.setEnabled(generate);
		fSealPackagesRadioButton.setEnabled(generate);
		fSealingInfoLabel.setEnabled(generate);
		fSealedPackagesDetailsButton.setEnabled(!sealState && generate);
		fUnSealedPackagesDetailsButton.setEnabled(sealState && generate);

		fMainClassHeaderLabel.setEnabled(generate);
		fMainClassLabel.setEnabled(generate);
		fMainClassText.setEnabled(generate);
		fMainClassBrowseButton.setEnabled(generate);

//		fDownloadHeaderLabel.setEnabled(generate);
//		fDownloadExtensionLabel.setEnabled(generate);
//		fDownloadExtensionText.setEnabled(generate);
		
		updateSealingInfo();
	}
		
	protected void updateSealingInfo() {
		String additionalSpace= "                                             ";
		if (fJarPackage.isJarSealed()) {
			int i= fJarPackage.getPackagesToUnseal().length;
			if (i == 0)
				fSealingInfoLabel.setText("JAR sealed" + additionalSpace);
			else if (i == 1)
				fSealingInfoLabel.setText("JAR sealed, but 1 package unsealed" + additionalSpace);
			else
				fSealingInfoLabel.setText("JAR sealed, but " + i + "packages unsealed" + additionalSpace);
				
		}
		else {
			int i= fJarPackage.getPackagesToSeal().length;
			if (i == 0)
				fSealingInfoLabel.setText("Nothing sealed" + additionalSpace);
			else if (i == 1)
				fSealingInfoLabel.setText("1 package sealed" + additionalSpace);
			else
				fSealingInfoLabel.setText(i + " packages sealed" + additionalSpace);
		}
	}
	/*
	 * Implements method from IJarPackageWizardPage
	 */
	public boolean computePageCompletion() {
		if (fJarPackage.isManifestGenerated() && fJarPackage.isManifestSaved() && fJarPackage.getManifestLocation().toString().length() == 0) {
			setErrorMessage("Invalid manifest file specified");
			return false;
		}
		if (!fJarPackage.isManifestGenerated() && !fJarPackage.doesManifestExist()) {
			setErrorMessage("Invalid manifest file specified");
			return false;
		}
		if (fJarPackage.isJarSealed()
				&& !fJarPackage.getPackagesForSelectedResources().containsAll(Arrays.asList(fJarPackage.getPackagesToUnseal()))) {
			setErrorMessage("Some of the unsealed packages are no longer within the selection");
			return false;
		}
		if (!fJarPackage.isJarSealed()
				&& !fJarPackage.getPackagesForSelectedResources().containsAll(Arrays.asList(fJarPackage.getPackagesToSeal()))) {
			setErrorMessage("Some of the sealed packages are no longer within the selection");
			return false;
		}
		/*
		 * This takes too long
		if (!fJarPackage.isMainClassValid(getContainer())) {
			setErrorMessage("Invalid main class specified");
			return false;
		}
		*/		
		return true;

	}
	/* 
	 * Implements method from IWizardPage.
	 */
	public void setPreviousPage(IWizardPage page) {
		super.setPreviousPage(page);
		if (getContainer() != null)
			updatePageCompletion();
	}

	// ----------- Model handling -----------

	/**
	 * Persists resource specification control setting that are to be restored
	 * in the next instance of this page. Subclasses wishing to persist
	 * settings for their controls should extend the hook method 
	 * <code>internalSaveWidgetValues</code>.
	 */
	public final void saveWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			// Manifest creation
			settings.put(STORE_GENERATE_MANIFEST, fJarPackage.isManifestGenerated());
			settings.put(STORE_SAVE_MANIFEST, fJarPackage.isManifestSaved());
			settings.put(STORE_REUSE_MANIFEST, fJarPackage.isManifestReused());
			settings.put(STORE_MANIFEST_LOCATION, fJarPackage.getManifestLocation().toString());

			// Sealing
			settings.put(STORE_SEAL_JAR, fJarPackage.isJarSealed());
			/*
			 * The following values are not stored in the dialog store
			 */
//			settings.put(STORE_SEALED_PACKAGES, fJarPackage.getPackageNamesToSeal());
//			settings.put(STORE_UNSEALED_PACKAGES, fJarPackage.getPackageNamesToUnseal());
			
			// Main-Class
//			if (fJarPackage.getMainClass() == null)
//				settings.put(STORE_MAIN_CLASS_NAME, "");
//			else
//				settings.put(STORE_MAIN_CLASS_NAME, fJarPackage.getMainClass().getHandleIdentifier());
			
			// Download extension path
//			settings.put(STORE_DOWNLOAD_EXTENSIONS, fJarPackage.getDownloadExtensionsPath());
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

		// Manifest creation
		if (fJarPackage.isManifestGenerated())
			fGenerateManifestRadioButton.setSelection(true);
		else
			fUseManifestRadioButton.setSelection(true);
		fSaveManifestCheckbox.setSelection(fJarPackage.isManifestSaved());
		fReuseManifestCheckbox.setSelection(fJarPackage.isManifestReused());
		fManifestFileText.setText(fJarPackage.getManifestLocation().toString());
		fNewManifestFileText.setText(fJarPackage.getManifestLocation().toString());	

		// Sealing
		if (fJarPackage.isJarSealed())
			fSealJarRadioButton.setSelection(true);
		else
			fSealPackagesRadioButton.setSelection(true);
		
		// Main-Class
		fMainClassText.setText(fJarPackage.getMainClassName());
		
		// Download extension path
//		fDownloadExtensionText.setText(fJarPackage.getDownloadExtensionsPath());
	}
	/**
	 *	Initializes the JAR package from last used wizard page values.
	 */
	protected void initializeJarPackage() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			if (settings.getArray(STORE_SEALED_PACKAGES) == null)
				return; // ie.- no settings stored

			// Manifest creation
			fJarPackage.setGenerateManifest(settings.getBoolean(STORE_GENERATE_MANIFEST));
			fJarPackage.setSaveManifest(settings.getBoolean(STORE_SAVE_MANIFEST));
			fJarPackage.setReuseManifest(settings.getBoolean(STORE_REUSE_MANIFEST));
			fJarPackage.setManifestLocation(new Path(settings.get(STORE_MANIFEST_LOCATION)));
	
			// Sealing
			fJarPackage.setSealJar(settings.getBoolean(STORE_SEAL_JAR));
			/*
			 * The following values are not stored for the dialog
			 */
//			fJarPackage.setPackageNamesToSeal(settings.getArray(STORE_SEALED_PACKAGES));
//			fJarPackage.setPackageNamesToUnseal(settings.getArray(STORE_UNSEALED_PACKAGES));

			// Main-Class
//			fJarPackage.setMainClass((IType)JavaCore.create(settings.get(STORE_MAIN_CLASS_NAME)));
			
			// Download extension path
//			fJarPackage.setDownloadExtensionsPath(settings.get(STORE_DOWNLOAD_EXTENSIONS));
		}
	}
	/**
	 *	Stores the widget values in the JAR package.
	 */
	protected void updateModel() {
		if (getControl() == null)
			return;
		
		// Manifest creation
		fJarPackage.setGenerateManifest(fGenerateManifestRadioButton.getSelection());
		fJarPackage.setSaveManifest(fSaveManifestCheckbox.getSelection());
		fJarPackage.setReuseManifest(fReuseManifestCheckbox.getSelection());
		String path;
		if (fJarPackage.isManifestGenerated())
			path= fNewManifestFileText.getText();
		else
			path= fManifestFileText.getText();
		if (path == null)
			path= "";
		fJarPackage.setManifestLocation(new Path(path));

		// Sealing
		fJarPackage.setSealJar(fSealJarRadioButton.getSelection());
			

		// Packages are updated when dialog is closed
		
		// Main-Class
		// Is updated when dialog is closed
		
		// Download extension path
		// fJarPackage.setDownloadExtensionsPath(fDownloadExtensionText.getText());
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

	// ----------- Utility methods -----------

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
	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param packageFragments the package fragments
	 * @return a new selection dialog
	 */
	protected SelectionDialog createPackageDialog(Set packageFragments) {
		int flags= JavaElementLabelProvider.SHOW_CONTAINER
					| JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION
					| JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION;
		List packages= new ArrayList(packageFragments.size());
		for (Iterator iter= packageFragments.iterator(); iter.hasNext();) {
			IPackageFragment fragment= (IPackageFragment)iter.next();
			boolean containsJavaElements= false;
			int kind;
			try {
				kind= fragment.getKind();
				containsJavaElements= fragment.getChildren().length > 0;
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, getContainer().getShell(), "JAR Package Wizard Error", "The package " + fragment.getElementName() + " seems to be corrupt. It will be ignored");
				continue;
			}
			if (kind != IPackageFragmentRoot.K_BINARY && containsJavaElements)
				packages.add(fragment);
		}
		ListSelectionDialog dialog= new ListSelectionDialog(getContainer().getShell(), packages, new org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider(), new JavaElementLabelProvider(flags), null);
		return dialog;		
	}
	/**
	 * Converts selection dialog results into an array of IPackageFragments.
	 * An empty array is returned in case of errors.
	 * @throws ClassCastException if results are not IPackageFragments
	 */
	protected IPackageFragment[] getPackagesFromDialog(SelectionDialog dialog) {
		if (dialog.getReturnCode() == dialog.OK && dialog.getResult().length > 0)
			return (IPackageFragment[])Arrays.asList(dialog.getResult()).toArray(new IPackageFragment[dialog.getResult().length]);
		else
			return new IPackageFragment[0];
	}
}
