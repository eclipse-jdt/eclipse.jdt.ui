/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PreferencePageSupport;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * The first page of the <code>SimpleProjectWizard</code>.
 */
public class JavaProjectWizardFirstPage extends WizardPage {


	/**
	 * Request a project name. Fires an event whenever the text field is
	 * changed, regardless of its content.
	 */
	private final class NameGroup extends Observable implements IDialogFieldListener {

		protected final StringDialogField fNameField;

		public NameGroup(Composite composite, String initialName) {
			final Composite nameComposite= new Composite(composite, SWT.NONE);
			nameComposite.setFont(composite.getFont());
			nameComposite.setLayout(initGridLayout(new GridLayout(2, false), false));
			nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// text field for project name
			fNameField= new StringDialogField();
			fNameField.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.NameGroup.label.text")); //$NON-NLS-1$
			fNameField.setDialogFieldListener(this);

			setName(initialName);

			fNameField.doFillIntoGrid(nameComposite, 2);
			LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
		}
		
		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		public String getName() {
			return fNameField.getText().trim();
		}

		public void postSetFocus() {
			fNameField.postSetFocusOnDialogField(getShell().getDisplay());
		}
		
		public void setName(String name) {
			fNameField.setText(name);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			fireEvent();
		}
		
	}

	/**
	 * Request a location. Fires an event whenever the checkbox or the location
	 * field is changed, regardless of whether the change originates from the
	 * user or has been invoked programmatically.
	 */
	private final class LocationGroup extends Observable implements Observer, IStringButtonAdapter, IDialogFieldListener {

		protected final SelectionButtonDialogField fWorkspaceRadio;
		protected final SelectionButtonDialogField fExternalRadio;
		protected final StringButtonDialogField fLocation;
		
		private String fPreviousExternalLocation;
		
		private static final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$

		public LocationGroup(Composite composite) {

			final int numColumns= 3;

			final Group group= new Group(composite, SWT.NONE);
			group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			group.setLayout(initGridLayout(new GridLayout(numColumns, false), true));
			group.setText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LocationGroup.title")); //$NON-NLS-1$

			fWorkspaceRadio= new SelectionButtonDialogField(SWT.RADIO);
			fWorkspaceRadio.setDialogFieldListener(this);
			fWorkspaceRadio.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LocationGroup.workspace.desc")); //$NON-NLS-1$

			fExternalRadio= new SelectionButtonDialogField(SWT.RADIO);
			fExternalRadio.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LocationGroup.external.desc")); //$NON-NLS-1$

			fLocation= new StringButtonDialogField(this);
			fLocation.setDialogFieldListener(this);
			fLocation.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LocationGroup.locationLabel.desc")); //$NON-NLS-1$
			fLocation.setButtonLabel(NewWizardMessages.getString("JavaProjectWizardFirstPage.LocationGroup.browseButton.desc")); //$NON-NLS-1$

			fExternalRadio.attachDialogField(fLocation);
			
			fWorkspaceRadio.setSelection(true);
			fExternalRadio.setSelection(false);
			
			fPreviousExternalLocation= ""; //$NON-NLS-1$

			fWorkspaceRadio.doFillIntoGrid(group, numColumns);
			fExternalRadio.doFillIntoGrid(group, numColumns);
			fLocation.doFillIntoGrid(group, numColumns);
			LayoutUtil.setHorizontalGrabbing(fLocation.getTextControl(null));
		}
				
		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		protected String getDefaultPath(String name) {
			final IPath path= Platform.getLocation().append(name);
			return path.toOSString();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Observer#update(java.util.Observable,
		 *      java.lang.Object)
		 */
		public void update(Observable o, Object arg) {
			if (isInWorkspace()) {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		public IPath getLocation() {
			if (isInWorkspace()) {
				return Platform.getLocation();
			}
			return Path.fromOSString(fLocation.getText().trim());
		}

		public boolean isInWorkspace() {
			return fWorkspaceRadio.isSelected();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			final DirectoryDialog dialog= new DirectoryDialog(getShell());
			dialog.setMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.directory.message")); //$NON-NLS-1$
			String directoryName = fLocation.getText().trim();
			if (directoryName.length() == 0) {
				String prevLocation= JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LAST_EXTERNAL_LOC);
				if (prevLocation != null) {
					directoryName= prevLocation;
				}
			}
		
			if (directoryName.length() > 0) {
				final File path = new File(directoryName);
				if (path.exists())
					dialog.setFilterPath(directoryName);
			}
			final String selectedDirectory = dialog.open();
			if (selectedDirectory != null) {
				fLocation.setText(selectedDirectory);
				JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fWorkspaceRadio) {
				final boolean checked= fWorkspaceRadio.isSelected();
				if (checked) {
					fPreviousExternalLocation= fLocation.getText();
					fLocation.setText(getDefaultPath(fNameGroup.getName()));
				} else {
					fLocation.setText(fPreviousExternalLocation);
				}
			}
			fireEvent();
		}
	}

	/**
	 * Request a project layout.
	 */
	private final class LayoutGroup implements Observer, IDialogFieldListener {

		protected final SelectionButtonDialogField fStdRadio, fSrcBinRadio, fConfigureButton;
		protected final Group fGroup;
		
		public LayoutGroup(Composite composite) {
			
			fGroup= new Group(composite, SWT.NONE);
			fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fGroup.setLayout(initGridLayout(new GridLayout(), true));
			fGroup.setText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LayoutGroup.title")); //$NON-NLS-1$
			
			fStdRadio= new SelectionButtonDialogField(SWT.RADIO);
			fStdRadio.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LayoutGroup.option.oneFolder")); //$NON-NLS-1$
			
			fSrcBinRadio= new SelectionButtonDialogField(SWT.RADIO);
			fSrcBinRadio.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LayoutGroup.option.separateFolders")); //$NON-NLS-1$
			
			fStdRadio.doFillIntoGrid(fGroup, 1);
			fSrcBinRadio.doFillIntoGrid(fGroup, 1);
			
			fConfigureButton= new SelectionButtonDialogField(SWT.PUSH);
			fConfigureButton.setLabelText(NewWizardMessages.getString("JavaProjectWizardFirstPage.LayoutGroup.configure")); //$NON-NLS-1$
			fConfigureButton.setDialogFieldListener(this);

			
			fConfigureButton.doFillIntoGrid(composite, 1);
			fConfigureButton.getSelectionButton(null).setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			
			boolean useSrcBin= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ);
			fSrcBinRadio.setSelection(useSrcBin);
			fStdRadio.setSelection(!useSrcBin);
		}

		public void update(Observable o, Object arg) {
			final boolean detect= fDetectGroup.mustDetect();
			fStdRadio.setEnabled(!detect);
			fSrcBinRadio.setEnabled(!detect);
			fGroup.setEnabled(!detect);
		}
		
		public boolean isSrcBin() {
			return fSrcBinRadio.isSelected();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fConfigureButton) {
				PreferencePageSupport.showPreferencePage(getShell(), NewJavaProjectPreferencePage.ID, new NewJavaProjectPreferencePage());
			}
		}
	}

	/**
	 * Show a warning when the project location contains files.
	 */
	private final class DetectGroup extends Observable implements Observer {

		private final Text fText;
		private boolean fDetect;
		
		public DetectGroup(Composite composite) {
			fText= new Text(composite, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
			final GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
			gd.widthHint= 0;
			gd.heightHint= convertHeightInCharsToPixels(6);
			fText.setLayoutData(gd);
			fText.setFont(composite.getFont());
			fText.setText(NewWizardMessages.getString("JavaProjectWizardFirstPage.DetectGroup.message")); //$NON-NLS-1$
			fText.setVisible(false);
		}

		public void update(Observable o, Object arg) {
			if (fLocationGroup.isInWorkspace()) {
				String name= getProjectName();
				if (name.length() == 0 || JavaPlugin.getWorkspace().getRoot().findMember(name) != null) {
					fDetect= false;
				} else {
					final File directory= fLocationGroup.getLocation().append(getProjectName()).toFile();
					fDetect= directory.isDirectory();
				}
			} else {
				final File directory= fLocationGroup.getLocation().toFile();
				fDetect= directory.isDirectory();
			}
			fText.setVisible(fDetect);
			setChanged();
			notifyObservers();
		}
		
		public boolean mustDetect() {
			return fDetect;
		}
	}

	/**
	 * Validate this page and show appropriate warnings and error NewWizardMessages.
	 */
	private final class Validator implements Observer {

		public void update(Observable o, Object arg) {

			final IWorkspace workspace= JavaPlugin.getWorkspace();

			final String name= fNameGroup.getName();

			// check wether the project name field is empty
			if (name.length() == 0) { //$NON-NLS-1$
				setErrorMessage(null);
				setMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.Message.enterProjectName")); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}

			// check whether the project name is valid
			final IStatus nameStatus= workspace.validateName(name, IResource.PROJECT);
			if (!nameStatus.isOK()) {
				setErrorMessage(nameStatus.getMessage());
				setPageComplete(false);
				return;
			}

			// check whether project already exists
			final IProject handle= getProjectHandle();
			if (handle.exists()) {
				setErrorMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.Message.projectAlreadyExists")); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}

			final String location= fLocationGroup.getLocation().toOSString();

			// check whether location is empty
			if (location.length() == 0) {
				setErrorMessage(null);
				setMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.Message.enterLocation")); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}

			// check whether the location is a syntactically correct path
			if (!Path.EMPTY.isValidPath(location)) { //$NON-NLS-1$
				setErrorMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.Message.invalidDirectory")); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}

			// check whether the location has the workspace as prefix
			IPath projectPath= Path.fromOSString(location);
			if (!fLocationGroup.isInWorkspace() && Platform.getLocation().isPrefixOf(projectPath)) {
				setErrorMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.Message.cannotCreateInWorkspace")); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}

			// If we do not place the contents in the workspace validate the
			// location.
			if (!fLocationGroup.isInWorkspace()) {
				final IStatus locationStatus= workspace.validateProjectLocation(handle, projectPath);
				if (!locationStatus.isOK()) {
					setErrorMessage(locationStatus.getMessage());
					setPageComplete(false);
					return;
				}
			}
			
			setPageComplete(true);

			setErrorMessage(null);
			setMessage(null);
		}

	}

	private NameGroup fNameGroup;
	private LocationGroup fLocationGroup;
	private LayoutGroup fLayoutGroup;
	private DetectGroup fDetectGroup;
	private Validator fValidator;

	private String fInitialName;
	
	private static final String PAGE_NAME= NewWizardMessages.getString("JavaProjectWizardFirstPage.page.pageName"); //$NON-NLS-1$

	/**
	 * Create a new <code>SimpleProjectFirstPage</code>.
	 */
	public JavaProjectWizardFirstPage() {
		super(PAGE_NAME);
		setPageComplete(false);
		setTitle(NewWizardMessages.getString("JavaProjectWizardFirstPage.page.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("JavaProjectWizardFirstPage.page.description")); //$NON-NLS-1$
		fInitialName= ""; //$NON-NLS-1$
	}
	
	public void setName(String name) {
		fInitialName= name;
		if (fNameGroup != null) {
			fNameGroup.setName(name);
		}
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		composite.setLayout(initGridLayout(new GridLayout(1, false), true));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		// create UI elements
		fNameGroup= new NameGroup(composite, fInitialName);
		fLocationGroup= new LocationGroup(composite);
		fLayoutGroup= new LayoutGroup(composite);
		fDetectGroup= new DetectGroup(composite);
		
		// establish connections
		fNameGroup.addObserver(fLocationGroup);
		fDetectGroup.addObserver(fLayoutGroup);
		fLocationGroup.addObserver(fDetectGroup);

		// initialize all elements
		fNameGroup.notifyObservers();
		
		// create and connect validator
		fValidator= new Validator();
		fNameGroup.addObserver(fValidator);
		fLocationGroup.addObserver(fValidator);

		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	/**
	 * Returns the current project location path as entered by the user, or its
	 * anticipated initial value. Note that if the default has been returned
	 * the path in a project description used to create a project should not be
	 * set.
	 * 
	 * @return the project location path or its anticipated initial value.
	 */
	public IPath getLocationPath() {
		return fLocationGroup.getLocation();
	}


	/**
	 * Creates a project resource handle for the current project name field
	 * value.
	 * <p>
	 * This method does not create the project resource; this is the
	 * responsibility of <code>IProject::create</code> invoked by the new
	 * project resource wizard.
	 * </p>
	 * 
	 * @return the new project resource handle
	 */
	public IProject getProjectHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(fNameGroup.getName());
	}
	
	public boolean isInWorkspace() {
		return fLocationGroup.isInWorkspace();
	}
	
	public String getProjectName() {
		return fNameGroup.getName();
	}

	public boolean getDetect() {
		return fDetectGroup.mustDetect();
	}
	
	public boolean isSrcBin() {
		return fLayoutGroup.isSrcBin();
	}
	
	
	/*
	 * see @DialogPage.setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fNameGroup.postSetFocus();
		}
	}
		
	/**
	 * Initialize a grid layout with the default Dialog settings.
	 */
	protected GridLayout initGridLayout(GridLayout layout, boolean margins) {
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		if (margins) {
			layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		} else {
			layout.marginWidth= 0;
			layout.marginHeight= 0;
		}
		return layout;
	}
	
	/**
	 * Set the layout data for a button.
	 */
	protected GridData setButtonLayoutData(Button button) {
		return super.setButtonLayoutData(button);
	}
}
