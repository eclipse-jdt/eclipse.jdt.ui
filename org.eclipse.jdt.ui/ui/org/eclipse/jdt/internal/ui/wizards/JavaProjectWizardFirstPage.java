/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WorkingSetGroup;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.workingsets.JavaWorkingSetUpdater;

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
			fNameField.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_NameGroup_label_text); 
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
			group.setText(NewWizardMessages.JavaProjectWizardFirstPage_LocationGroup_title); 

			fWorkspaceRadio= new SelectionButtonDialogField(SWT.RADIO);
			fWorkspaceRadio.setDialogFieldListener(this);
			fWorkspaceRadio.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LocationGroup_workspace_desc); 

			fExternalRadio= new SelectionButtonDialogField(SWT.RADIO);
			fExternalRadio.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LocationGroup_external_desc); 

			fLocation= new StringButtonDialogField(this);
			fLocation.setDialogFieldListener(this);
			fLocation.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LocationGroup_locationLabel_desc); 
			fLocation.setButtonLabel(NewWizardMessages.JavaProjectWizardFirstPage_LocationGroup_browseButton_desc); 

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
			dialog.setMessage(NewWizardMessages.JavaProjectWizardFirstPage_directory_message); 
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
	private final class LayoutGroup implements Observer, SelectionListener {

		private final SelectionButtonDialogField fStdRadio, fSrcBinRadio;
		private final Group fGroup;
		private final Link fPreferenceLink;
		
		public LayoutGroup(Composite composite) {
			
			fGroup= new Group(composite, SWT.NONE);
			fGroup.setFont(composite.getFont());
			fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fGroup.setLayout(initGridLayout(new GridLayout(3, false), true));
			fGroup.setText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_title); 
			
			fStdRadio= new SelectionButtonDialogField(SWT.RADIO);
			fStdRadio.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_option_oneFolder); 
			
			fSrcBinRadio= new SelectionButtonDialogField(SWT.RADIO);
			fSrcBinRadio.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_option_separateFolders); 

			fStdRadio.doFillIntoGrid(fGroup, 3);
			LayoutUtil.setHorizontalGrabbing(fStdRadio.getSelectionButton(null));
			
			fSrcBinRadio.doFillIntoGrid(fGroup, 2);
			
			fPreferenceLink= new Link(fGroup, SWT.NONE);
			fPreferenceLink.setText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_link_description);
			fPreferenceLink.setLayoutData(new GridData(GridData.END, GridData.END, false, false));
			fPreferenceLink.addSelectionListener(this);
						
			boolean useSrcBin= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ);
			fSrcBinRadio.setSelection(useSrcBin);
			fStdRadio.setSelection(!useSrcBin);
		}

		public void update(Observable o, Object arg) {
			final boolean detect= fDetectGroup.mustDetect();
			fStdRadio.setEnabled(!detect);
			fSrcBinRadio.setEnabled(!detect);
			fPreferenceLink.setEnabled(!detect);
			fGroup.setEnabled(!detect);
		}
		
		public boolean isSrcBin() {
			return fSrcBinRadio.isSelected();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e) {
			String id= NewJavaProjectPreferencePage.ID;
			PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, null).open();
			fDetectGroup.handlePossibleJVMChange();
			fJREGroup.handlePossibleJVMChange();
		}
	}
	
	private final class JREGroup implements Observer, SelectionListener, IDialogFieldListener {

		private static final String LAST_SELECTED_EE_SETTINGS_KEY= JavaUI.ID_PLUGIN + ".last.selected.execution.enviroment"; //$NON-NLS-1$
		private static final String LAST_SELECTED_JRE_SETTINGS_KEY= JavaUI.ID_PLUGIN + ".last.selected.project.jre"; //$NON-NLS-1$
		private static final String LAST_SELECTED_JRE_KIND= JavaUI.ID_PLUGIN + ".last.selected.jre.kind"; //$NON-NLS-1$
		
		private static final int DEFAULT_JRE= 0;
		private static final int PROJECT_JRE= 1;
		private static final int EE_JRE= 2;
		
		private final SelectionButtonDialogField fUseDefaultJRE, fUseProjectJRE, fUseEEJRE;
		private final ComboDialogField fJRECombo;
		private final ComboDialogField fEECombo;
		private final Group fGroup;
		private final Link fPreferenceLink;
		private IVMInstall[] fInstalledJVMs;
		private String[] fJRECompliance;
		private IExecutionEnvironment[] fInstalledEEs;
		private String[] fEECompliance;
		
		public JREGroup(Composite composite) {
			fGroup= new Group(composite, SWT.NONE);
			fGroup.setFont(composite.getFont());
			fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fGroup.setLayout(initGridLayout(new GridLayout(3, false), true));
			fGroup.setText(NewWizardMessages.JavaProjectWizardFirstPage_JREGroup_title); 
						
			fUseDefaultJRE= new SelectionButtonDialogField(SWT.RADIO);
			fUseDefaultJRE.setLabelText(getDefaultJVMLabel());
			fUseDefaultJRE.doFillIntoGrid(fGroup, 2);
			
			fPreferenceLink= new Link(fGroup, SWT.NONE);
			fPreferenceLink.setFont(fGroup.getFont());
			fPreferenceLink.setText(NewWizardMessages.JavaProjectWizardFirstPage_JREGroup_link_description);
			fPreferenceLink.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
			fPreferenceLink.addSelectionListener(this);
		
			fUseProjectJRE= new SelectionButtonDialogField(SWT.RADIO);
			fUseProjectJRE.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_JREGroup_specific_compliance);
			fUseProjectJRE.doFillIntoGrid(fGroup, 1);
						
			fJRECombo= new ComboDialogField(SWT.READ_ONLY);
			fillInstalledJREs(fJRECombo);
			fJRECombo.setDialogFieldListener(this);

			Combo comboControl= fJRECombo.getComboControl(fGroup);
			comboControl.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false)); // make sure column 2 is grabbing (but no fill)
			comboControl.setVisibleItemCount(30);
			
			DialogField.createEmptySpace(fGroup);
			
			fUseEEJRE= new SelectionButtonDialogField(SWT.RADIO);
			fUseEEJRE.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_JREGroup_specific_EE);
			fUseEEJRE.doFillIntoGrid(fGroup, 1);
						
			fEECombo= new ComboDialogField(SWT.READ_ONLY);
			fillExecutionEnvironments(fEECombo);
			fEECombo.setDialogFieldListener(this);

			Combo eeComboControl= fEECombo.getComboControl(fGroup);
			eeComboControl.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false)); // make sure column 2 is grabbing (but no fill)
			eeComboControl.setVisibleItemCount(30);
			
			DialogField.createEmptySpace(fGroup);
			
			switch (getLastSelectedJREKind()) {
			case DEFAULT_JRE:				
				fUseDefaultJRE.setSelection(true);
				break;
			case PROJECT_JRE:
				fUseProjectJRE.setSelection(true);
				break;
			case EE_JRE:
				fUseEEJRE.setSelection(true);
				break;
			}
			
			fJRECombo.setEnabled(fUseProjectJRE.isSelected());
			fEECombo.setEnabled(fUseEEJRE.isSelected());
			
			fUseDefaultJRE.setDialogFieldListener(this);
			fUseProjectJRE.setDialogFieldListener(this);
			fUseEEJRE.setDialogFieldListener(this);
		}

		private void fillInstalledJREs(ComboDialogField comboField) {
			String selectedItem= getLastSelectedJRE();
			int selectionIndex= -1;
			if (fUseProjectJRE.isSelected()) {
				selectionIndex= comboField.getSelectionIndex();
				if (selectionIndex != -1) {//paranoia
					selectedItem= comboField.getItems()[selectionIndex];
				}
			}
			
			fInstalledJVMs= getWorkspaceJREs();
			Arrays.sort(fInstalledJVMs, new Comparator() {

				public int compare(Object arg0, Object arg1) {
					IVMInstall i0= (IVMInstall)arg0;
					IVMInstall i1= (IVMInstall)arg1;
					if (i1 instanceof IVMInstall2 && i0 instanceof IVMInstall2) {
						String cc0= JavaModelUtil.getCompilerCompliance((IVMInstall2) i0, JavaCore.VERSION_1_4);
						String cc1= JavaModelUtil.getCompilerCompliance((IVMInstall2) i1, JavaCore.VERSION_1_4);
						int result= cc1.compareTo(cc0);
						if (result != 0)
							return result;
					}
					return Policy.getComparator().compare(i0.getName(), i1.getName());
				}
				
			});
			selectionIndex= -1;//find new index
			String[] jreLabels= new String[fInstalledJVMs.length];
			fJRECompliance= new String[fInstalledJVMs.length];
			for (int i= 0; i < fInstalledJVMs.length; i++) {
				jreLabels[i]= fInstalledJVMs[i].getName();
				if (selectedItem != null && jreLabels[i].equals(selectedItem)) {
					selectionIndex= i;
				}
				if (fInstalledJVMs[i] instanceof IVMInstall2) {
					fJRECompliance[i]= JavaModelUtil.getCompilerCompliance((IVMInstall2) fInstalledJVMs[i], JavaCore.VERSION_1_4);
				} else {
					fJRECompliance[i]= JavaCore.VERSION_1_4;
				}
			}
			comboField.setItems(jreLabels);
			if (selectionIndex == -1) {
				comboField.selectItem(getDefaultJVMName());
			} else {
				comboField.selectItem(selectedItem);
			}
		}
		
		private void fillExecutionEnvironments(ComboDialogField comboField) {
			String selectedItem= getLastSelectedEE();
			int selectionIndex= -1;
			if (fUseEEJRE.isSelected()) {
				selectionIndex= comboField.getSelectionIndex();
				if (selectionIndex != -1) {// paranoia
					selectedItem= comboField.getItems()[selectionIndex];
				}
			}
			
			fInstalledEEs= JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			Arrays.sort(fInstalledEEs, new Comparator() {
				public int compare(Object arg0, Object arg1) {
					return Policy.getComparator().compare(((IExecutionEnvironment)arg0).getId(), ((IExecutionEnvironment)arg1).getId());
				}
			});
			selectionIndex= -1;//find new index
			String[] eeLabels= new String[fInstalledEEs.length];
			fEECompliance= new String[fInstalledEEs.length];
			for (int i= 0; i < fInstalledEEs.length; i++) {
				eeLabels[i]= fInstalledEEs[i].getId();
				if (selectedItem != null && eeLabels[i].equals(selectedItem)) {
					selectionIndex= i;
				}
				fEECompliance[i]= JavaModelUtil.getExecutionEnvironmentCompliance(fInstalledEEs[i]);
			}
			comboField.setItems(eeLabels);
			if (selectionIndex == -1) {
				comboField.selectItem(getDefaultEEName());
			} else {
				comboField.selectItem(selectedItem);
			}
		}
		
		private IVMInstall[] getWorkspaceJREs() {
			List standins = new ArrayList();
			IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
			for (int i = 0; i < types.length; i++) {
				IVMInstallType type = types[i];
				IVMInstall[] installs = type.getVMInstalls();
				for (int j = 0; j < installs.length; j++) {
					IVMInstall install = installs[j];
					standins.add(new VMStandin(install));
				}
			}
			return ((IVMInstall[])standins.toArray(new IVMInstall[standins.size()]));	
		}

		private String getDefaultJVMName() {
			IVMInstall install= JavaRuntime.getDefaultVMInstall();
			if (install != null) {
				return install.getName();
			} else {
				return NewWizardMessages.JavaProjectWizardFirstPage_UnknownDefaultJRE_name;
			}
		}
		
		private String getDefaultEEName() {
			IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall();
			
			IExecutionEnvironment[] environments= JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			if (defaultVM != null) {
				for (int i= 0; i < environments.length; i++) {
					IVMInstall eeDefaultVM= environments[i].getDefaultVM();
					if (eeDefaultVM != null && defaultVM.getId().equals(eeDefaultVM.getId()))
						return environments[i].getId();			
				}
			}
			
			String defaultCC;
			if (defaultVM instanceof IVMInstall2) {
				defaultCC= JavaModelUtil.getCompilerCompliance((IVMInstall2)defaultVM, JavaCore.VERSION_1_4);
			} else {
				defaultCC= JavaCore.VERSION_1_4;
			}
			
			for (int i= 0; i < environments.length; i++) {
				String eeCompliance= JavaModelUtil.getExecutionEnvironmentCompliance(environments[i]);
				if (defaultCC.endsWith(eeCompliance))
					return environments[i].getId();
			}
			
			return "J2SE-1.5"; //$NON-NLS-1$
		}

		private String getDefaultJVMLabel() {
			return Messages.format(NewWizardMessages.JavaProjectWizardFirstPage_JREGroup_default_compliance, getDefaultJVMName());
		}

		public void update(Observable o, Object arg) {
			updateEnableState();
		}

		private void updateEnableState() {
			final boolean detect= fDetectGroup.mustDetect();
			fUseDefaultJRE.setEnabled(!detect);
			fUseProjectJRE.setEnabled(!detect);
			fUseEEJRE.setEnabled(!detect);
			fJRECombo.setEnabled(!detect && fUseProjectJRE.isSelected());
			fEECombo.setEnabled(!detect && fUseEEJRE.isSelected());
			fPreferenceLink.setEnabled(!detect);
			fGroup.setEnabled(!detect);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e) {
			String jreID= BuildPathSupport.JRE_PREF_PAGE_ID;
			String complianceId= CompliancePreferencePage.PREF_ID;
			Map data= new HashMap();
			data.put(PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE);
			PreferencesUtil.createPreferenceDialogOn(getShell(), jreID, new String[] { jreID, complianceId  }, data).open();
			
			handlePossibleJVMChange();
			fDetectGroup.handlePossibleJVMChange();
		}
		
		public void handlePossibleJVMChange() {
			fUseDefaultJRE.setLabelText(getDefaultJVMLabel());
			fillInstalledJREs(fJRECombo);
			fillExecutionEnvironments(fEECombo);
		}
		

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			updateEnableState();
			fDetectGroup.handlePossibleJVMChange();
			if (field == fJRECombo) {
				if (fUseProjectJRE.isSelected()) {
				
				
					storeSelectionValue(fJRECombo, LAST_SELECTED_JRE_SETTINGS_KEY);
				}
			} else if (field == fEECombo) {
				if (fUseEEJRE.isSelected()) {
					storeSelectionValue(fEECombo, LAST_SELECTED_EE_SETTINGS_KEY);
				}
			} else if (field == fUseDefaultJRE) {
				if (fUseDefaultJRE.isSelected()) {
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_JRE_KIND, DEFAULT_JRE);
				}
			} else if (field == fUseProjectJRE) {
				if (fUseProjectJRE.isSelected()) {
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_JRE_KIND, PROJECT_JRE);
				}
			} else if (field == fUseEEJRE) {
				if (fUseEEJRE.isSelected()) {
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_JRE_KIND, EE_JRE);
				}
			}
		}

		private void storeSelectionValue(ComboDialogField combo, String preferenceKey) {
			int index= combo.getSelectionIndex();
			if (index == -1)
				return;
			
			String item= combo.getItems()[index];
			JavaPlugin.getDefault().getDialogSettings().put(preferenceKey, item);
		}
		
		private int getLastSelectedJREKind() {
			IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			if (settings.get(LAST_SELECTED_JRE_KIND) == null)
				return DEFAULT_JRE;
			
			return settings.getInt(LAST_SELECTED_JRE_KIND);
		}
		
		private String getLastSelectedEE() {
			IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			return settings.get(LAST_SELECTED_EE_SETTINGS_KEY);
		}
		
		private String getLastSelectedJRE() {
			IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			return settings.get(LAST_SELECTED_JRE_SETTINGS_KEY);
		}
			
		public IVMInstall getSelectedJVM() {
			if (fUseProjectJRE.isSelected()) {
				int index= fJRECombo.getSelectionIndex();
				if (index >= 0 && index < fInstalledJVMs.length) { // paranoia
					return fInstalledJVMs[index];
				}
			} else if (fUseEEJRE.isSelected()) {
				
			}
			return null;
		}
		
		public IPath getJREContainerPath() {
			if (fUseProjectJRE.isSelected()) {
				int index= fJRECombo.getSelectionIndex();
				if (index >= 0 && index < fInstalledJVMs.length) { // paranoia
					return JavaRuntime.newJREContainerPath(fInstalledJVMs[index]);
				}
			} else if (fUseEEJRE.isSelected()) {
				int index= fEECombo.getSelectionIndex();
				if (index >= 0 && index < fInstalledEEs.length) { // paranoia
					return JavaRuntime.newJREContainerPath(fInstalledEEs[index]);
				}
			}
			return null;
		}
		
		public String getSelectedCompilerCompliance() {
			if (fUseProjectJRE.isSelected()) {
				int index= fJRECombo.getSelectionIndex();
				if (index >= 0 && index < fJRECompliance.length) { // paranoia
					return fJRECompliance[index];
				}
			} else if (fUseEEJRE.isSelected()) {
				int index= fEECombo.getSelectionIndex();
				if (index >= 0 && index < fEECompliance.length) { // paranoia
					return fEECompliance[index];
				}
			}
			return null;
		}


	}

	/**
	 * Show a warning when the project location contains files.
	 */
	private final class DetectGroup extends Observable implements Observer, SelectionListener {

		private final Link fHintText;
		private Label fIcon;
		private boolean fDetect;
		
		public DetectGroup(Composite parent) {
			
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			GridLayout layout= new GridLayout(2, false);
			layout.horizontalSpacing= 10;
			composite.setLayout(layout);
			
			fIcon= new Label(composite, SWT.LEFT);
			fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
			GridData gridData= new GridData(SWT.LEFT, SWT.CENTER, false, false);
			fIcon.setLayoutData(gridData);
			
			fHintText= new Link(composite, SWT.WRAP);
			fHintText.setFont(composite.getFont());
			fHintText.addSelectionListener(this);
			gridData= new GridData(GridData.FILL, SWT.FILL, true, true);
			gridData.widthHint= convertWidthInCharsToPixels(50);
			gridData.heightHint= convertHeightInCharsToPixels(3);
			fHintText.setLayoutData(gridData);
			
			handlePossibleJVMChange();
		}
		
		public void handlePossibleJVMChange() {
			
			if (JavaRuntime.getDefaultVMInstall() == null) {
				fHintText.setText(NewWizardMessages.JavaProjectWizardFirstPage_NoJREFound_link);
				fHintText.setVisible(true);
				fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
				fIcon.setVisible(true);
				return;
			}
			
			String selectedCompliance= fJREGroup.getSelectedCompilerCompliance();
			if (selectedCompliance != null) {
				String defaultCompliance= JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE);
				if (selectedCompliance.equals(defaultCompliance)) {
					fHintText.setVisible(false);
					fIcon.setVisible(false);
				} else {
					fHintText.setText(Messages.format(NewWizardMessages.JavaProjectWizardFirstPage_DetectGroup_differendWorkspaceCC_message, new String[] {defaultCompliance, selectedCompliance}));
					fHintText.setVisible(true);
					fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
					fIcon.setVisible(true);
				}
				return;
			}

			selectedCompliance= JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE);
			IVMInstall selectedJVM= fJREGroup.getSelectedJVM();
			if (selectedJVM == null) {
				selectedJVM= JavaRuntime.getDefaultVMInstall();
			}
			String jvmCompliance= JavaCore.VERSION_1_4;
			if (selectedJVM instanceof IVMInstall2) {
				jvmCompliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) selectedJVM, JavaCore.VERSION_1_4);
			}
			if (!selectedCompliance.equals(jvmCompliance) && (JavaModelUtil.is50OrHigher(selectedCompliance) || JavaModelUtil.is50OrHigher(jvmCompliance))) {
				if (selectedCompliance.equals(JavaCore.VERSION_1_5))
					selectedCompliance= "5.0"; //$NON-NLS-1$
				else if (selectedCompliance.equals(JavaCore.VERSION_1_6))
					selectedCompliance= "6.0"; //$NON-NLS-1$
				
				fHintText.setText(Messages.format(NewWizardMessages.JavaProjectWizardFirstPage_DetectGroup_jre_message, new String[] {selectedCompliance, jvmCompliance}));
				fHintText.setVisible(true);
				fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
				fIcon.setVisible(true);
			} else {
				fHintText.setVisible(false);
				fIcon.setVisible(false);
			}
		}
		
		public void update(Observable o, Object arg) {
			if (o instanceof LocationGroup) {
				boolean oldDetectState= fDetect;
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
				
				if (oldDetectState != fDetect) {
					setChanged();
					notifyObservers();
					
					if (fDetect) {
						fHintText.setVisible(true);
						fHintText.setText(NewWizardMessages.JavaProjectWizardFirstPage_DetectGroup_message);
						fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
						fIcon.setVisible(true);
					} else {
						handlePossibleJVMChange();
					}
				}
			}
		}

		public boolean mustDetect() {
			return fDetect;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e) {
			String jreID= BuildPathSupport.JRE_PREF_PAGE_ID;
			String complianceId= CompliancePreferencePage.PREF_ID;
			Map data= new HashMap();
			data.put(PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE);
			String id= "JRE".equals(e.text) ? jreID : complianceId; //$NON-NLS-1$
			PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { jreID, complianceId  }, data).open();
			
			fJREGroup.handlePossibleJVMChange();
			handlePossibleJVMChange();
		}
	}

	/**
	 * Validate this page and show appropriate warnings and error NewWizardMessages.
	 */
	private final class Validator implements Observer {

		public void update(Observable o, Object arg) {

			final IWorkspace workspace= JavaPlugin.getWorkspace();

			final String name= fNameGroup.getName();

			// check whether the project name field is empty
			if (name.length() == 0) { 
				setErrorMessage(null);
				setMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_enterProjectName); 
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
				setErrorMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_projectAlreadyExists); 
				setPageComplete(false);
				return;
			}
			
			IPath projectLocation= ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getProjectName());
			if (projectLocation.toFile().exists()) {
				try {
					//correct casing
					String canonicalPath= projectLocation.toFile().getCanonicalPath();
					projectLocation= new Path(canonicalPath);
				} catch (IOException e) {
					JavaPlugin.log(e);
				}
				
				String existingName= projectLocation.lastSegment();
				if (!existingName.equals(fNameGroup.getName())) {
					setErrorMessage(Messages.format(NewWizardMessages.JavaProjectWizardFirstPage_Message_invalidProjectNameForWorkspaceRoot, existingName));
					setPageComplete(false);
					return;
				}
				
			}

			final String location= fLocationGroup.getLocation().toOSString();

			// check whether location is empty
			if (location.length() == 0) {
				setErrorMessage(null);
				setMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_enterLocation); 
				setPageComplete(false);
				return;
			}

			// check whether the location is a syntactically correct path
			if (!Path.EMPTY.isValidPath(location)) { 
				setErrorMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_invalidDirectory); 
				setPageComplete(false);
				return;
			}

			IPath projectPath= Path.fromOSString(location);
			
			if (fLocationGroup.isInWorkspace())		
				projectPath= projectPath.append(fNameGroup.getName());
			
			if (projectPath.toFile().exists()) {//create from existing source			
				if (Platform.getLocation().isPrefixOf(projectPath)) { //create from existing source in workspace
					if (!Platform.getLocation().equals(projectPath.removeLastSegments(1))) {
						setErrorMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_notOnWorkspaceRoot);
						setPageComplete(false);
						return;
					}
	
					if (!projectPath.toFile().exists()) {
						setErrorMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_notExisingProjectOnWorkspaceRoot);
						setPageComplete(false);
						return;
					}
				}
			} else if (!fLocationGroup.isInWorkspace()) {//create at non existing external location
				if (!canCreate(projectPath.toFile())) {
					setErrorMessage(NewWizardMessages.JavaProjectWizardFirstPage_Message_cannotCreateAtExternalLocation); 
					setPageComplete(false);
					return;
				}
				
				// If we do not place the contents in the workspace validate the
				// location.
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

		private boolean canCreate(File file) {
			while (!file.exists()) {
				file= file.getParentFile();
				if (file == null)
					return false;
			}
			
			return file.canWrite();
		}

	}

	private NameGroup fNameGroup;
	private LocationGroup fLocationGroup;
	private LayoutGroup fLayoutGroup;
	private JREGroup fJREGroup;
	private DetectGroup fDetectGroup;
	private Validator fValidator;

	private String fInitialName;
	
	private static final String PAGE_NAME= NewWizardMessages.JavaProjectWizardFirstPage_page_pageName;
	private WorkingSetGroup fWorkingSetGroup;
	private IStructuredSelection fSelection; 

	/**
	 * Create a new <code>SimpleProjectFirstPage</code>.
	 */
	public JavaProjectWizardFirstPage() {
		super(PAGE_NAME);
		setPageComplete(false);
		setTitle(NewWizardMessages.JavaProjectWizardFirstPage_page_title); 
		setDescription(NewWizardMessages.JavaProjectWizardFirstPage_page_description); 
		fInitialName= ""; //$NON-NLS-1$
		initializeDefaultVM();
	}
	
	private void initializeDefaultVM() {
		JavaRuntime.getDefaultVMInstall();
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
		fJREGroup= new JREGroup(composite);
		fLayoutGroup= new LayoutGroup(composite);
		String[] workingSetTypes= new String[] {JavaWorkingSetUpdater.ID, "org.eclipse.ui.resourceWorkingSetPage"}; //$NON-NLS-1$
		fWorkingSetGroup= new WorkingSetGroup(composite, fSelection == null ? StructuredSelection.EMPTY : fSelection, workingSetTypes);
		fDetectGroup= new DetectGroup(composite);
		
		// establish connections
		fNameGroup.addObserver(fLocationGroup);
		fDetectGroup.addObserver(fLayoutGroup);
		fDetectGroup.addObserver(fJREGroup);
		fLocationGroup.addObserver(fDetectGroup);

		// initialize all elements
		fNameGroup.notifyObservers();
		
		// create and connect validator
		fValidator= new Validator();
		fNameGroup.addObserver(fValidator);
		fLocationGroup.addObserver(fValidator);

		setControl(composite);
		Dialog.applyDialogFont(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE);
	}	

	/**
	 * Returns the current project location path as entered by the user, or its
	 * anticipated initial value. Note that if the default has been returned
	 * the path in a project description used to create a project should not be
	 * set.
	 * <p>
	 * TODO At some point this method has to be converted to return an URI instead
	 * of an path. However, this first requires support from Platform/UI to specify
	 * a project location different than in a local file system. 
	 * </p>
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
		final String location= fLocationGroup.getLocation().toOSString();
		IPath projectPath= Path.fromOSString(location);
		return Platform.getLocation().isPrefixOf(projectPath);
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
	
	/**
	 * Returns the path of the classpath container selected, or <code>null</code> if the default JRE was selected
	 * @return the path to the selected JRE
	 */
	public IPath getJREContainerPath() {
		return fJREGroup.getJREContainerPath();
	}
	
	/**
	 * @return the selected Compiler Compliance, or <code>null</code> iff the default Compiler Compliance should be used
	 */
	public String getCompilerCompliance() {
		return fJREGroup.getSelectedCompilerCompliance();
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
	 * @param layout the layout to initialize
	 * @param margins true if margins should be used
	 * @return the initialized layout
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
	 * @param selection the current workbench selection, not <b>null</b>
	 */
	public void setSelection(IStructuredSelection selection) {
		fSelection= selection;
	}

	/**
	 * @return the selected working sets, not <b>null</b>
	 */
	public IWorkingSet[] getWorkingSets() {
		return fWorkingSetGroup.getSelectedWorkingSets();
	}
}
