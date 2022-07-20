/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Karsten Thoms - Bug#451777
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CompletableFuture;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
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
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;

/**
 * The first page of the New Java Project wizard. This page is typically used in combination with
 * {@link NewJavaProjectWizardPageTwo}. Clients can extend this page to modify the UI: Add, remove
 * or reorder sections.
 *
 * <p>
 * Clients may instantiate or subclass.
 * </p>
 *
 * @since 3.4
 */
public class NewJavaProjectWizardPageOne extends WizardPage {

	private static String EMPTY_STR= ""; //$NON-NLS-1$

	/**
	 * Request a project name. Fires an event whenever the text field is
	 * changed, regardless of its content.
	 */
	private final class NameGroup extends Observable implements IDialogFieldListener {

		protected final StringDialogField fNameField;

		public NameGroup() {
			// text field for project name
			fNameField= new StringDialogField();
			fNameField.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_NameGroup_label_text);
			fNameField.setDialogFieldListener(this);
		}

		public Control createControl(Composite composite) {
			Composite nameComposite= new Composite(composite, SWT.NONE);
			nameComposite.setFont(composite.getFont());
			nameComposite.setLayout(new GridLayout(2, false));

			fNameField.doFillIntoGrid(nameComposite, 2);
			LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));

			return nameComposite;
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

		@Override
		public void dialogFieldChanged(DialogField field) {
			fireEvent();
			fModuleGroup.updateModuleNameHintTextVisibility();
		}
	}

	/**
	 * Request a location. Fires an event whenever the checkbox or the location
	 * field is changed, regardless of whether the change originates from the
	 * user or has been invoked programmatically.
	 */
	private final class LocationGroup extends Observable implements Observer, IStringButtonAdapter, IDialogFieldListener {

		protected final SelectionButtonDialogField fUseDefaults;
		protected final StringButtonDialogField fLocation;

		private String fPreviousExternalLocation;

		private final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$

		public LocationGroup() {
			fUseDefaults= new SelectionButtonDialogField(SWT.CHECK);
			fUseDefaults.setDialogFieldListener(this);
			fUseDefaults.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LocationGroup_location_desc);

			fLocation= new StringButtonDialogField(this);
			fLocation.setDialogFieldListener(this);
			fLocation.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LocationGroup_locationLabel_desc);
			fLocation.setButtonLabel(NewWizardMessages.NewJavaProjectWizardPageOne_LocationGroup_browseButton_desc);

			fUseDefaults.setSelection(true);

			fPreviousExternalLocation= ""; //$NON-NLS-1$
		}

		public Control createControl(Composite composite) {
			final int numColumns= 4;

			final Composite locationComposite= new Composite(composite, SWT.NONE);
			locationComposite.setLayout(new GridLayout(numColumns, false));

			fUseDefaults.doFillIntoGrid(locationComposite, numColumns);
			fLocation.doFillIntoGrid(locationComposite, numColumns);
			LayoutUtil.setHorizontalGrabbing(fLocation.getTextControl(null));
			BidiUtils.applyBidiProcessing(fLocation.getTextControl(null), StructuredTextTypeHandlerFactory.FILE);

			return locationComposite;
		}

		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		protected String getDefaultPath(String name) {
			final IPath path= Platform.getLocation().append(name);
			return path.toOSString();
		}

		@Override
		public void update(Observable o, Object arg) {
			if (isUseDefaultSelected()) {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		public IPath getLocation() {
			if (isUseDefaultSelected()) {
				return Platform.getLocation();
			}
			return Path.fromOSString(fLocation.getText().trim());
		}

		public boolean isUseDefaultSelected() {
			return fUseDefaults.isSelected();
		}

		public void setLocation(IPath path) {
			fUseDefaults.setSelection(path == null);
			if (path != null) {
				fLocation.setText(path.toOSString());
			} else {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		@Override
		public void changeControlPressed(DialogField field) {
			final DirectoryDialog dialog= new DirectoryDialog(getShell(), SWT.SHEET);
			dialog.setText(NewWizardMessages.NewJavaProjectWizardPageOne_dialog_title);
			dialog.setMessage(NewWizardMessages.NewJavaProjectWizardPageOne_directory_message);
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
				String oldDirectory= new Path(fLocation.getText().trim()).lastSegment();
				fLocation.setText(selectedDirectory);
				String lastSegment= new Path(selectedDirectory).lastSegment();
				if (lastSegment != null && (fNameGroup.getName().length() == 0 || fNameGroup.getName().equals(oldDirectory))) {
					fNameGroup.setName(lastSegment);
				}
				JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
			}
		}

		@Override
		public void dialogFieldChanged(DialogField field) {
			if (field == fUseDefaults) {
				final boolean checked= fUseDefaults.isSelected();
				if (checked) {
					fPreviousExternalLocation= fLocation.getText();
					fLocation.setText(getDefaultPath(fNameGroup.getName()));
					fLocation.setEnabled(false);
				} else {
					fLocation.setText(fPreviousExternalLocation);
					fLocation.setEnabled(true);
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
		private Group fGroup;
		private Link fPreferenceLink;

		public LayoutGroup() {
			fStdRadio= new SelectionButtonDialogField(SWT.RADIO);
			fStdRadio.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_option_oneFolder);

			fSrcBinRadio= new SelectionButtonDialogField(SWT.RADIO);
			fSrcBinRadio.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_option_separateFolders);

			boolean useSrcBin= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ);
			fSrcBinRadio.setSelection(useSrcBin);
			fStdRadio.setSelection(!useSrcBin);
		}


		public Control createContent(Composite composite) {
			fGroup= new Group(composite, SWT.NONE);
			fGroup.setFont(composite.getFont());
			fGroup.setLayout(initGridLayout(new GridLayout(3, false), true));
			fGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_title);

			fStdRadio.doFillIntoGrid(fGroup, 3);
			LayoutUtil.setHorizontalGrabbing(fStdRadio.getSelectionButton(null));

			fSrcBinRadio.doFillIntoGrid(fGroup, 2);

			fPreferenceLink= new Link(fGroup, SWT.NONE);
			fPreferenceLink.setText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_link_description);
			fPreferenceLink.setLayoutData(new GridData(GridData.END, GridData.END, false, false));
			fPreferenceLink.addSelectionListener(this);

			updateEnableState();
			return fGroup;
		}


		@Override
		public void update(Observable o, Object arg) {
			updateEnableState();
		}

		private void updateEnableState() {
			if (fDetectGroup == null)
				return;

			final boolean detect= fDetectGroup.mustDetect();
			fStdRadio.setEnabled(!detect);
			fSrcBinRadio.setEnabled(!detect);
			if (fPreferenceLink != null) {
				fPreferenceLink.setEnabled(!detect);
			}
			if (fGroup != null) {
				fGroup.setEnabled(!detect);
			}
		}

		/**
		 * Return <code>true</code> if the user specified to create 'source' and 'bin' folders.
		 *
		 * @return returns <code>true</code> if the user specified to create 'source' and 'bin' folders.
		 */
		public boolean isSrcBin() {
			return fSrcBinRadio.isSelected();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			String id= NewJavaProjectPreferencePage.ID;
			PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, null).open();
			fDetectGroup.handlePossibleJVMChange();
			fJREGroup.handlePossibleJVMChange();
		}
	}

	private final class JREGroup extends Observable implements Observer, SelectionListener, IDialogFieldListener {

		private final String LAST_SELECTED_EE_SETTINGS_KEY= JavaUI.ID_PLUGIN + ".last.selected.execution.enviroment"; //$NON-NLS-1$
		private final String LAST_SELECTED_JRE_SETTINGS_KEY= JavaUI.ID_PLUGIN + ".last.selected.project.jre"; //$NON-NLS-1$
//		private static final String LAST_SELECTED_JRE_KIND= JavaUI.ID_PLUGIN + ".last.selected.jre.kind"; // used before EE became default
		private final String LAST_SELECTED_JRE_KIND2= JavaUI.ID_PLUGIN + ".last.selected.jre.kind2"; //$NON-NLS-1$

		private static final int DEFAULT_JRE= 0;
		private static final int PROJECT_JRE= 1;
		private static final int EE_JRE= 2;

		private final SelectionButtonDialogField fUseDefaultJRE, fUseProjectJRE, fUseEEJRE;
		private final ComboDialogField fJRECombo;
		private final ComboDialogField fEECombo;
		private Group fGroup;
		private Link fPreferenceLink;
		private IVMInstall[] fInstalledJVMs;
		private String[] fJRECompliance;
		private IExecutionEnvironment[] fInstalledEEs;
		private String[] fEECompliance;
		private String fDefaultJVMLabel;

		public JREGroup() {
			fUseDefaultJRE= new SelectionButtonDialogField(SWT.RADIO);
			fUseDefaultJRE.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_JREGroup_unspecified_compliance);

			fUseProjectJRE= new SelectionButtonDialogField(SWT.RADIO);
			fUseProjectJRE.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_JREGroup_specific_compliance);

			fJRECombo= new ComboDialogField(SWT.READ_ONLY);
			fJRECombo.setDialogFieldListener(this);

			fUseEEJRE= new SelectionButtonDialogField(SWT.RADIO);
			fUseEEJRE.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_JREGroup_specific_EE);

			fEECombo= new ComboDialogField(SWT.READ_ONLY);
			fEECombo.setDialogFieldListener(this);

			fUseDefaultJRE.setDialogFieldListener(this);
			fUseProjectJRE.setDialogFieldListener(this);
			fUseEEJRE.setDialogFieldListener(this);

			CompletableFuture.supplyAsync(this::getLastSelectedJREKind)
				.thenAcceptAsync(kind -> {
					switch (kind) {
						case DEFAULT_JRE:
							fUseDefaultJRE.setSelection(true);
							break;
						case PROJECT_JRE:
							fUseProjectJRE.setSelection(true);
							break;
						case EE_JRE:
							fUseEEJRE.setSelection(true);
							break;
						default:
							break;
					}
				}, Display.getDefault()::asyncExec);

			CompletableFuture.runAsync(this::initializeJvmFields)
				.thenAcceptAsync(VOID -> {
					if (fGroup.isDisposed()) {
						return;
					}
					fUseDefaultJRE.setLabelText(fDefaultJVMLabel);
					fillInstalledJREs(fJRECombo);
					fillExecutionEnvironments(fEECombo);
					updateEnableState();
					fGroup.requestLayout();
					setChanged();
					notifyObservers();
				}, Display.getDefault()::asyncExec);
		}


		private void initializeJvmFields () {
			fDefaultJVMLabel= getDefaultJVMLabel();

			fInstalledJVMs= getWorkspaceJREs();
			Arrays.sort(fInstalledJVMs, (i0, i1) -> {
				if (i1 instanceof IVMInstall2 && i0 instanceof IVMInstall2) {
					String cc0= JavaModelUtil.getCompilerCompliance((IVMInstall2) i0, JavaCore.VERSION_1_4);
					String cc1= JavaModelUtil.getCompilerCompliance((IVMInstall2) i1, JavaCore.VERSION_1_4);
					int result= JavaCore.compareJavaVersions(cc1, cc0);
					if (result != 0)
						return result;
				}
				return Policy.getComparator().compare(i0.getName(), i1.getName());
			});

			fInstalledEEs= JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		}

		public Control createControl(Composite composite) {
			fGroup= new Group(composite, SWT.NONE);
			fGroup.setFont(composite.getFont());
			fGroup.setLayout(initGridLayout(new GridLayout(2, false), true));
			fGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_JREGroup_title);

			fUseEEJRE.doFillIntoGrid(fGroup, 1);
			Combo eeComboControl= fEECombo.getComboControl(fGroup);
			eeComboControl.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

			fUseProjectJRE.doFillIntoGrid(fGroup, 1);
			Combo comboControl= fJRECombo.getComboControl(fGroup);
			comboControl.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

			fUseDefaultJRE.doFillIntoGrid(fGroup, 1);

			fPreferenceLink= new Link(fGroup, SWT.NONE);
			fPreferenceLink.setFont(fGroup.getFont());
			fPreferenceLink.setText(NewWizardMessages.NewJavaProjectWizardPageOne_JREGroup_link_description);
			fPreferenceLink.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
			fPreferenceLink.addSelectionListener(this);

			updateEnableState();
			return fGroup;
		}


		private void fillInstalledJREs(ComboDialogField comboField) {
			if (fGroup.isDisposed()) {
				return;
			}
			String selectedItem= getLastSelectedJRE();
			int selectionIndex= -1;
			if (fUseProjectJRE.isSelected()) {
				selectionIndex= comboField.getSelectionIndex();
				if (selectionIndex != -1) {//paranoia
					selectedItem= comboField.getItems()[selectionIndex];
				}
			}

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
			List<VMStandin> standins = new ArrayList<>();
			for (IVMInstallType type : JavaRuntime.getVMInstallTypes()) {
				for (IVMInstall install : type.getVMInstalls()) {
					standins.add(new VMStandin(install));
				}
			}
			return standins.toArray(new IVMInstall[standins.size()]);
		}

		private String getDefaultJVMName() {
			IVMInstall install= JavaRuntime.getDefaultVMInstall();
			if (install != null) {
				return install.getName();
			} else {
				return NewWizardMessages.NewJavaProjectWizardPageOne_UnknownDefaultJRE_name;
			}
		}

		private String getDefaultEEName() {
			IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall();

			IExecutionEnvironment[] environments= JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			if (defaultVM != null) {
				for (IExecutionEnvironment environment : environments) {
					IVMInstall eeDefaultVM= environment.getDefaultVM();
					if (eeDefaultVM != null && defaultVM.getId().equals(eeDefaultVM.getId())) {
						return environment.getId();
					}
				}
			}

			String defaultCC=JavaModelUtil.VERSION_LATEST;
			if (defaultVM instanceof IVMInstall2)
				defaultCC= JavaModelUtil.getCompilerCompliance((IVMInstall2)defaultVM, defaultCC);

			for (IExecutionEnvironment environment : environments) {
				String eeCompliance= JavaModelUtil.getExecutionEnvironmentCompliance(environment);
				if (defaultCC.equals(eeCompliance)) {
					return environment.getId();
				}
			}

			return "JavaSE-11"; //$NON-NLS-1$
		}

		private String getDefaultJVMLabel() {
			return Messages.format(NewWizardMessages.NewJavaProjectWizardPageOne_JREGroup_default_compliance, getDefaultJVMName());
		}

		@Override
		public void update(Observable o, Object arg) {
			updateEnableState();
		}

		private void updateEnableState() {
			if (fGroup.isDisposed()) {
				return;
			}
			final boolean detect= fDetectGroup != null ? fDetectGroup.mustDetect() : false;
			fUseDefaultJRE.setEnabled(!detect);
			fUseProjectJRE.setEnabled(!detect);
			fUseEEJRE.setEnabled(!detect);
			fJRECombo.setEnabled(!detect && fUseProjectJRE.isSelected() && fJRECombo.getItems().length > 0);
			fEECombo.setEnabled(!detect && fUseEEJRE.isSelected() && fEECombo.getItems().length > 0);
			if (fPreferenceLink != null) {
				fPreferenceLink.setEnabled(!detect);
			}
			if (fGroup != null) {
				fGroup.setEnabled(!detect);
			}
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			String jreID= BuildPathSupport.JRE_PREF_PAGE_ID;
			String eeID= BuildPathSupport.EE_PREF_PAGE_ID;
			String complianceId= CompliancePreferencePage.PREF_ID;
			Map<String, Boolean> data= new HashMap<>();
			data.put(PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE);
			PreferencesUtil.createPreferenceDialogOn(getShell(), jreID, new String[] { jreID, complianceId , eeID }, data).open();

			handlePossibleJVMChange();
			fDetectGroup.handlePossibleJVMChange();
		}

		public void handlePossibleJVMChange() {
			fUseDefaultJRE.setLabelText(getDefaultJVMLabel());
			fillInstalledJREs(fJRECombo);
			fillExecutionEnvironments(fEECombo);
		}

		@Override
		public void dialogFieldChanged(DialogField field) {
			if (fGroup.isDisposed()) {
				return;
			}
			updateEnableState();
			fDetectGroup.handlePossibleJVMChange();
			fModuleGroup.handlePossibleJVMChange();
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
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_JRE_KIND2, DEFAULT_JRE);
					fUseProjectJRE.setSelection(false);
					fUseEEJRE.setSelection(false);
				}
			} else if (field == fUseProjectJRE) {
				if (fUseProjectJRE.isSelected()) {
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_JRE_KIND2, PROJECT_JRE);
					fUseDefaultJRE.setSelection(false);
					fUseEEJRE.setSelection(false);
				}
			} else if (field == fUseEEJRE) {
				if (fUseEEJRE.isSelected()) {
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_JRE_KIND2, EE_JRE);
					fUseDefaultJRE.setSelection(false);
					fUseProjectJRE.setSelection(false);
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
			if (settings.get(LAST_SELECTED_JRE_KIND2) == null)
				return EE_JRE;

			return settings.getInt(LAST_SELECTED_JRE_KIND2);
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

	private final static class WorkingSetGroup {

		private WorkingSetConfigurationBlock fWorkingSetBlock;

		public WorkingSetGroup() {
			String[] workingSetIds= new String[] { IWorkingSetIDs.JAVA, IWorkingSetIDs.RESOURCE };
			fWorkingSetBlock= new WorkingSetConfigurationBlock(workingSetIds, JavaPlugin.getDefault().getDialogSettings());
			//fWorkingSetBlock.setDialogMessage(NewWizardMessages.NewJavaProjectWizardPageOne_WorkingSetSelection_message);
		}

		public Control createControl(Composite composite) {
			Group workingSetGroup= new Group(composite, SWT.NONE);
			workingSetGroup.setFont(composite.getFont());
			workingSetGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_WorkingSets_group);
			workingSetGroup.setLayout(new GridLayout(1, false));

			fWorkingSetBlock.createContent(workingSetGroup);

			return workingSetGroup;
		}


		public void setWorkingSets(IWorkingSet[] workingSets) {
			fWorkingSetBlock.setWorkingSets(workingSets);
		}

		public IWorkingSet[] getSelectedWorkingSets() {
			return fWorkingSetBlock.getSelectedWorkingSets();
		}
	}

	@SuppressWarnings("deprecation")
	private final class ModuleGroup extends Observable implements IDialogFieldListener{

		private final String LAST_SELECTED_CREATE_MODULEINFO_SETTINGS_KEY= JavaUI.ID_PLUGIN + ".last.selected.create.moduleinfo"; //$NON-NLS-1$
		private final String LAST_SELECTED_CREATE_MODULEINFO_COMMENT_SETTINGS_KEY= JavaUI.ID_PLUGIN + ".last.selected.create.moduleinfo.comments"; //$NON-NLS-1$
		private final SelectionButtonDialogField fCreateModuleInfo;
		private final SelectionButtonDialogField fAddCommentButton;
		private final StringDialogField fModuleNameDialogField;
		private final DialogField fModuleNameHintText;
		private boolean savePreference;

		public ModuleGroup() {
			fCreateModuleInfo= new SelectionButtonDialogField(SWT.CHECK);
			fCreateModuleInfo.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_Create_ModuleInfoFile_name);
			fCreateModuleInfo.setDialogFieldListener(this);
			fCreateModuleInfo.setEnabled(false);
			fAddCommentButton= new SelectionButtonDialogField(SWT.CHECK);
			fAddCommentButton.setLabelText(NewWizardMessages.NewTypeWizardPage_addcomment_label);
			fAddCommentButton.setDialogFieldListener(this);
			fAddCommentButton.setEnabled(false);
			fModuleNameDialogField= new StringDialogField();
			fModuleNameDialogField.setDialogFieldListener(this);
			fModuleNameDialogField.setLabelText(NewWizardMessages.NewModuleInfoWizardPage_module_label);
			fModuleNameDialogField.setEnabled(false);
			fModuleNameHintText= new DialogField();
			fModuleNameHintText.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_Module_group_empty_name);
			savePreference= false;
		}

		public String getCompliance() {
			String compilerCompliance= fJREGroup.getSelectedCompilerCompliance();
			if (compilerCompliance == null) {
				compilerCompliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) JavaRuntime.getDefaultVMInstall(), JavaCore.VERSION_1_4);
			}
			return compilerCompliance;
		}

		public Control createControl(Composite composite) {
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			Group moduleGroup= new Group(composite, SWT.NONE);
			moduleGroup.setFont(composite.getFont());
			moduleGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_Module_group);
			moduleGroup.setLayout(layout);

			fCreateModuleInfo.doFillIntoGrid(moduleGroup, 2);
			fModuleNameDialogField.doFillIntoGrid(moduleGroup, 2);
			fAddCommentButton.doFillIntoGrid(moduleGroup, 2);
			fModuleNameHintText.doFillIntoGrid(moduleGroup, 2);
			fModuleNameHintText.getLabelControl(moduleGroup).setVisible(false);
			LayoutUtil.setHorizontalGrabbing(fModuleNameDialogField.getTextControl(null));

			return moduleGroup;
		}

		public void handlePossibleJVMChange() {
			boolean enable= false;
			boolean oldValEnabled= fCreateModuleInfo.isEnabled();
			String compliance= getCompliance();
			if (compliance != null && JavaModelUtil.is9OrHigher(compliance)) {
				enable= true;
			}
			fCreateModuleInfo.setEnabled(enable);
			fModuleNameDialogField.setEnabled(enable);
			fAddCommentButton.setEnabled(enable);
			fModuleNameHintText.getLabelControl(null).setVisible(false);
			savePreference= false;
			if (!enable) {
				fCreateModuleInfo.setSelection(false);
			} else if(oldValEnabled != enable) {
				String setting=JavaPlugin.getDefault().getDialogSettings().get(LAST_SELECTED_CREATE_MODULEINFO_SETTINGS_KEY);
				boolean val= (setting == null) ? true : Boolean.parseBoolean(setting);
				fCreateModuleInfo.setSelection(val);
			}
			updateModuleFields();
			savePreference= true;
			setChanged();
			notifyObservers();
		}

		public boolean getCreateModuleInfoFile() {
			return fCreateModuleInfo.isSelected();
		}

		public String getModuleName() {
			return fModuleNameDialogField.getText().trim();
		}

		public boolean getGenerateCommentsInModuleInfoFile() {
			return fAddCommentButton.isSelected();
		}

		public String getModuleNameOrProjectName() {
			String moduleName = getModuleName();
			if (moduleName == null || moduleName.length() == 0) {
				moduleName= fNameGroup.getName();
			}
			return  moduleName;
		}

		public void updateModuleNameHintTextVisibility() {
			boolean visible= false;
			String moduleName= getModuleNameOrProjectName();
			if (moduleName != null && moduleName.length() != 0 && fCreateModuleInfo.isEnabled() && fCreateModuleInfo.isSelected() &&fModuleNameDialogField.isEnabled()) {
				visible= true;
				fModuleNameHintText.setLabelText(Messages.format(NewWizardMessages.NewJavaProjectWizardPageOne_Module_group_empty_name, moduleName));
			}
			try {
				fModuleNameHintText.getLabelControl(null).setVisible(visible);
			} catch (AssertionFailedException e) {
				//do nothing , the label has not yet been created.
			}
			setChanged();
			notifyObservers();
		}

		private void updateModuleFields() {
			boolean enable= false;
			boolean commentOldVal = fAddCommentButton.isEnabled() && fAddCommentButton.isSelected();
			boolean moduleNameDlgFieldOldVal= fModuleNameDialogField.isEnabled();
			if (fCreateModuleInfo.isEnabled()) {
				if (fCreateModuleInfo.isSelected()) {
					enable= true;
				}
			}
			fModuleNameDialogField.setEnabled(enable);
			fAddCommentButton.setEnabled(enable);
			if (moduleNameDlgFieldOldVal != enable) {
				fModuleNameDialogField.setText(EMPTY_STR);
			}
			if (commentOldVal != enable) {
				if (enable) {
					String setting=JavaPlugin.getDefault().getDialogSettings().get(LAST_SELECTED_CREATE_MODULEINFO_COMMENT_SETTINGS_KEY);
					boolean val= (setting == null) ? true : Boolean.parseBoolean(setting);
					fAddCommentButton.setSelection(val);
				} else {
					fAddCommentButton.setSelection(enable);
				}
			}
			updateModuleNameHintTextVisibility();
		}

		@Override
		public void dialogFieldChanged(DialogField field) {
			if (field == fCreateModuleInfo) {
				updateModuleFields();
				if (savePreference) {
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_CREATE_MODULEINFO_SETTINGS_KEY, fCreateModuleInfo.isSelected());
					JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_CREATE_MODULEINFO_COMMENT_SETTINGS_KEY, fCreateModuleInfo.isSelected() && fAddCommentButton.isEnabled() && fAddCommentButton.isSelected());
				}
			} else if (field == fAddCommentButton && savePreference){
				JavaPlugin.getDefault().getDialogSettings().put(LAST_SELECTED_CREATE_MODULEINFO_COMMENT_SETTINGS_KEY, fCreateModuleInfo.isSelected() && fAddCommentButton.isEnabled() && fAddCommentButton.isSelected());
			} else if (field == fModuleNameDialogField) {
				updateModuleFields();
			}
		}

		private IStatus validateModuleName(String name) {
			String compliance= getCompliance();
			return JavaConventions.validateModuleName(name, compliance, compliance);
		}

		public IStatus getModuleStatus() {
			StatusInfo status= new StatusInfo();
			if (fCreateModuleInfo.isEnabled() && fCreateModuleInfo.isSelected()) {
				String moduleName= getModuleNameOrProjectName();
				if (moduleName != null && moduleName.length() > 0) {
					IStatus val= validateModuleName(moduleName);
					if (val.getSeverity() == IStatus.ERROR) {
						status.setError(Messages.format(NewWizardMessages.NewModuleInfoWizardPage_error_InvalidModuleName, val.getMessage()));
					} else if (val.getSeverity() == IStatus.WARNING) {
						status.setWarning(Messages.format(NewWizardMessages.NewModuleInfoWizardPage_warning_DiscouragedModuleName, val.getMessage()));
					}
				} else {
					status.setError(NewWizardMessages.NewModuleInfoWizardPage_error_EnterName);
				}
			}
			return status;
		}
	}

	/**
	 * Show a warning when the project location contains files.
	 */
	private final class DetectGroup extends Observable implements Observer, SelectionListener {

		private Link fHintText;
		private Label fIcon;
		private boolean fDetect;

		public DetectGroup() {
			fDetect= false;
		}

		public Control createControl(Composite parent) {

			Composite composite= new Composite(parent, SWT.NONE);
			GridData gridData= new GridData(SWT.FILL, SWT.TOP, true, false);
			gridData.exclude= true;
			composite.setLayoutData(gridData);
			GridLayout layout= new GridLayout(2, false);
			layout.horizontalSpacing= 10;
			composite.setLayout(layout);

			fIcon= new Label(composite, SWT.LEAD);
			fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
			gridData= new GridData(SWT.LEAD, SWT.TOP, false, false);
			fIcon.setLayoutData(gridData);

			fHintText= new Link(composite, SWT.WRAP);
			fHintText.setFont(composite.getFont());
			fHintText.addSelectionListener(this);
			gridData= new GridData(GridData.FILL, SWT.FILL, true, false);
			gridData.widthHint= convertWidthInCharsToPixels(50);
			gridData.heightHint= convertHeightInCharsToPixels(3);
			fHintText.setLayoutData(gridData);

			handlePossibleJVMChange();
			return composite;
		}

		public void handlePossibleJVMChange() {

			if (JavaRuntime.getDefaultVMInstall() == null) {
				fHintText.setText(NewWizardMessages.NewJavaProjectWizardPageOne_NoJREFound_link);
				fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
				setInfoControlVisible(true);
				return;
			}

			String selectedCompliance= fJREGroup.getSelectedCompilerCompliance();
			if (selectedCompliance != null) {
				String defaultCompliance= JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE);
				if (selectedCompliance.equals(defaultCompliance)) {
					setInfoControlVisible(false);
				} else {
					fHintText.setText(Messages.format(NewWizardMessages.NewJavaProjectWizardPageOne_DetectGroup_differendWorkspaceCC_message, new String[] {  BasicElementLabels.getVersionName(defaultCompliance), BasicElementLabels.getVersionName(selectedCompliance)}));
					fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
					setInfoControlVisible(true);
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
				fHintText.setText(Messages.format(NewWizardMessages.NewJavaProjectWizardPageOne_DetectGroup_jre_message, new String[] {BasicElementLabels.getVersionName(selectedCompliance), BasicElementLabels.getVersionName(jvmCompliance)}));
				fIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
				setInfoControlVisible(true);
			} else {
				setInfoControlVisible(false);
			}

		}

		private void setInfoControlVisible(boolean visible) {
			Composite composite= fHintText.getParent();
			composite.setVisible(visible);
			((GridData) composite.getLayoutData()).exclude= !visible;
			updateSize();
		}

		private boolean computeDetectState() {
			if (fLocationGroup.isUseDefaultSelected()) {
				String name= fNameGroup.getName();
				if (name.length() == 0 || JavaPlugin.getWorkspace().getRoot().findMember(name) != null) {
					return false;
				} else {
					final File directory= fLocationGroup.getLocation().append(name).toFile();
					return directory.isDirectory();
				}
			} else {
				final File directory= fLocationGroup.getLocation().toFile();
				return directory.isDirectory();
			}
		}

		@Override
		public void update(Observable o, Object arg) {
			if (o instanceof LocationGroup) {
				boolean oldDetectState= fDetect;
				fDetect= computeDetectState();

				if (oldDetectState != fDetect) {
					setChanged();
					notifyObservers();

					if (fDetect) {
						fHintText.setVisible(true);
						fHintText.setText(NewWizardMessages.NewJavaProjectWizardPageOne_DetectGroup_message);
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

		@Override
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			String jreID= BuildPathSupport.JRE_PREF_PAGE_ID;
			String eeID= BuildPathSupport.EE_PREF_PAGE_ID;
			String complianceId= CompliancePreferencePage.PREF_ID;
			Map<String, Boolean> data= new HashMap<>();
			data.put(PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE);
			String id= "JRE".equals(e.text) ? jreID : complianceId; //$NON-NLS-1$
			PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { jreID, complianceId, eeID  }, data).open();

			fJREGroup.handlePossibleJVMChange();
			handlePossibleJVMChange();
		}
	}

	/**
	 * Validate this page and show appropriate warnings and error NewWizardMessages.
	 */
	private final class Validator implements Observer {

		@Override
		public void update(Observable o, Object arg) {

			final IWorkspace workspace= JavaPlugin.getWorkspace();

			final String name= fNameGroup.getName();

			// check whether the project name field is empty
			if (name.length() == 0) {
				setErrorMessage(null);
				setMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_enterProjectName);
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
			final IProject handle= workspace.getRoot().getProject(name);
			if (handle.exists()) {
				setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_projectAlreadyExists);
				setPageComplete(false);
				return;
			}

			IPath projectLocation= ResourcesPlugin.getWorkspace().getRoot().getLocation().append(name);
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
					setErrorMessage(Messages.format(NewWizardMessages.NewJavaProjectWizardPageOne_Message_invalidProjectNameForWorkspaceRoot, BasicElementLabels.getResourceName(existingName)));
					setPageComplete(false);
					return;
				}

			}

			final String location= fLocationGroup.getLocation().toOSString();

			// check whether location is empty
			if (location.length() == 0) {
				setErrorMessage(null);
				setMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_enterLocation);
				setPageComplete(false);
				return;
			}

			// check whether the location is a syntactically correct path
			if (!Path.EMPTY.isValidPath(location)) {
				setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_invalidDirectory);
				setPageComplete(false);
				return;
			}

			IPath projectPath= null;
			if (!fLocationGroup.isUseDefaultSelected()) {
				projectPath= Path.fromOSString(location);
				if (!projectPath.toFile().exists()) {
					// check non-existing external location
					if (!canCreate(projectPath.toFile())) {
						setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_cannotCreateAtExternalLocation);
						setPageComplete(false);
						return;
					}
				}
			}

			// validate the location
			final IStatus locationStatus= workspace.validateProjectLocation(handle, projectPath);
			if (!locationStatus.isOK()) {
				setErrorMessage(locationStatus.getMessage());
				setPageComplete(false);
				return;
			}

			if (fJREGroup.fUseEEJRE.isSelected() && fJREGroup.fEECombo.getItems().length == 0) {
				setPageComplete(false);
				return;
			}
			if (fJREGroup.fUseProjectJRE.isSelected() && fJREGroup.fJRECombo.getItems().length == 0) {
				setPageComplete(false);
				return;
			}
			final IStatus moduleNameStatus= fModuleGroup.getModuleStatus();
			if (!moduleNameStatus.isOK()) {
				if (moduleNameStatus.getSeverity() == IStatus.WARNING
						|| moduleNameStatus.getSeverity() == IStatus.INFO) {
					setMessage(moduleNameStatus.getMessage());
					setErrorMessage(null);
					setPageComplete(true);
				} else {
					setErrorMessage(moduleNameStatus.getMessage());
					setPageComplete(false);
				}
				return;
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

	private static final String PAGE_NAME= "NewJavaProjectWizardPageOne"; //$NON-NLS-1$

	private final NameGroup fNameGroup;
	private final LocationGroup fLocationGroup;
	private final LayoutGroup fLayoutGroup;
	private final JREGroup fJREGroup;
	private final DetectGroup fDetectGroup;
	private final Validator fValidator;
	private final WorkingSetGroup fWorkingSetGroup;
	private final ModuleGroup fModuleGroup;
	private Runnable sizeUpdater;

	/**
	 * Creates a new {@link NewJavaProjectWizardPageOne}.
	 */
	@SuppressWarnings("deprecation")
	public NewJavaProjectWizardPageOne() {
		super(PAGE_NAME);
		setPageComplete(false);
		setTitle(NewWizardMessages.NewJavaProjectWizardPageOne_page_title);
		setDescription(NewWizardMessages.NewJavaProjectWizardPageOne_page_description);

		fNameGroup= new NameGroup();
		fLocationGroup= new LocationGroup();
		fJREGroup= new JREGroup();
		fLayoutGroup= new LayoutGroup();
		fWorkingSetGroup= new WorkingSetGroup();
		fDetectGroup= new DetectGroup();
		fModuleGroup= new ModuleGroup();

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
		fJREGroup.addObserver(fValidator);
		fModuleGroup.addObserver(fValidator);

		// initialize defaults
		setProjectName(""); //$NON-NLS-1$
		setProjectLocationURI(null);
		setWorkingSets(new IWorkingSet[0]);

		initializeDefaultVM();
	}

	/**
	 * The wizard owning this page can call this method to initialize the fields from the
	 * current selection and active part.
	 *
	 * @param selection used to initialize the fields
	 * @param activePart the (typically active) part to initialize the fields or <code>null</code>
	 */
	public void init(IStructuredSelection selection, IWorkbenchPart activePart) {
		setWorkingSets(getSelectedWorkingSet(selection, activePart));
	}

	private void initializeDefaultVM() {
		JavaRuntime.getDefaultVMInstall();
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final ScrolledComposite scroll= new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite composite= new Composite(scroll, SWT.NULL);
		composite.setFont(parent.getFont());
		final GridLayout compositeLayout= initGridLayout(new GridLayout(1, false), true);
		composite.setLayout(compositeLayout);
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		// create UI elements
		Control nameControl= createNameControl(composite);
		nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control locationControl= createLocationControl(composite);
		locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control jreControl= createJRESelectionControl(composite);
		jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control layoutControl= createProjectLayoutControl(composite);
		layoutControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control workingSetControl= createWorkingSetControl(composite);
		workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control moduleControl= createModuleControl(composite);
		moduleControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createInfoControl(composite);

		scroll.setContent(composite);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setShowFocusedControl(true);
		sizeUpdater= new Runnable() {

			@Override
			public void run() {

				// show/hide horizontal scrollbar
				scroll.setMinWidth(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);

				// show/hide vertical scrollbar
				final int availableWidthBefore = scroll.getClientArea().width;
				scroll.setMinHeight(computeHeight(availableWidthBefore));

				// showing/hiding vertical scrollbar can change available width and therefore the height
				final int availableWidthAfter = scroll.getClientArea().width;
				if (availableWidthBefore != availableWidthAfter) {
					scroll.setMinHeight(computeHeight(availableWidthAfter));
				}

			}

			private int computeHeight(int width) {
				final int availableWidth= width - 2 * compositeLayout.marginWidth;
				int height= 0;
				for (Control child : composite.getChildren()) {
					if (child.getLayoutData() instanceof GridData && ((GridData) child.getLayoutData()).exclude) {
						continue;
					}
					if (height > 0) {
						height+= compositeLayout.verticalSpacing;
					}
					height+= child.computeSize(availableWidth, SWT.DEFAULT).y;
				}
				height+= 2 * compositeLayout.marginHeight;
				return height;
			}

		};
		scroll.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				updateSize();
			}
		});

		setControl(scroll);
	}

	private void updateSize() {
		if (sizeUpdater != null) {
			sizeUpdater.run();
		}
	}

	@Override
	protected void setControl(Control newControl) {
		Dialog.applyDialogFont(newControl);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(newControl, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE);

		super.setControl(newControl);
	}


	/**
	 * Creates the controls for the name field.
	 *
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createNameControl(Composite composite) {
		return fNameGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the location field.
	 *
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createLocationControl(Composite composite) {
		return fLocationGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the JRE selection
	 *
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createJRESelectionControl(Composite composite) {
		return fJREGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the project layout selection.
	 *
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createProjectLayoutControl(Composite composite) {
		return fLayoutGroup.createContent(composite);
	}

	/**
	 * Creates the controls for the working set selection.
	 *
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createWorkingSetControl(Composite composite) {
		return fWorkingSetGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the info section.
	 *
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createInfoControl(Composite composite) {
		return fDetectGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the module section.
	 *
	 * @param composite the parent composite
	 * @return the created control
	 * @since 3.23
	 */
	protected Control createModuleControl(Composite composite) {
		return fModuleGroup.createControl(composite);
	}

	/**
	 * Gets a project name for the new project.
	 *
	 * @return the new project resource handle
	 */
	public String getProjectName() {
		return fNameGroup.getName();
	}

	/**
	 * Sets the name of the new project
	 *
	 * @param name the new name
	 */
	public void setProjectName(String name) {
		if (name == null)
			throw new IllegalArgumentException();

		fNameGroup.setName(name);
	}

	/**
	 * Returns the current project location path as entered by the user, or <code>null</code>
	 * if the project should be created in the workspace.

	 * @return the project location path or its anticipated initial value.
	 */
	public URI getProjectLocationURI() {
		if (fLocationGroup.isUseDefaultSelected()) {
			return null;
		}
		return URIUtil.toURI(fLocationGroup.getLocation());
	}

	/**
	 * Sets the project location of the new project or <code>null</code> if the project
	 * should be created in the workspace
	 *
	 * @param uri the new project location
	 */
	public void setProjectLocationURI(URI uri) {
		IPath path= uri != null ? URIUtil.toPath(uri) : null;
		fLocationGroup.setLocation(path);
	}

	/**
	 * Returns the compiler compliance to be used for the project, or <code>null</code> to use the workspace
	 * compiler compliance.
	 *
	 * @return compiler compliance to be used for the project or <code>null</code>
	 */
	public String getCompilerCompliance() {
		return fJREGroup.getSelectedCompilerCompliance();
	}

	/**
	 * Returns if the module-info creation dialog needs to be shown or not.
	 *
	 * @return 'create module-info.java file' has been checked or not.
	 * @since 3.23
	 */
	public boolean getCreateModuleInfoFile() {
		return fModuleGroup.getCreateModuleInfoFile();
	}

	/**
	 * Returns the module name given.
	 *
	 * @return 'Module name:' text field value, trimming the String. If empty , returns
	 * the project name, trimming the String.
	 * @since 3.27
	 */
	public String getModuleName() {
		String moduleName = fModuleGroup.getModuleName();
		if (moduleName == null || moduleName.length() == 0) {
			moduleName= fNameGroup.getName().trim();
		}
		return  moduleName;
	}

	/**
	 * Returns if the module-info file should have comments.
	 *
	 * @return 'Generate comments' has been checked or not.
	 * @since 3.27
	 */
	public boolean getGenerateCommentsInModuleInfoFile() {
		return fModuleGroup.getGenerateCommentsInModuleInfoFile();
	}

	/**
	 * Returns the default class path entries to be added on new projects. By default this is the JRE
	 * container as selected by the user.
	 *
	 * @return returns the default class path entries
	 */
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IPath newPath= fJREGroup.getJREContainerPath();
		if (newPath != null) {
			return updateWithModuleAttribute(new IClasspathEntry[] { JavaCore.newContainerEntry(newPath) }, newPath);
		}
		return updateWithModuleAttribute(PreferenceConstants.getDefaultJRELibrary(), new Path("org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$
	}

	/**
	 * Updates the JRE container with module info
	 *
	 * @param cpEntries array containing jre container without module attribute
	 * @param newPath JRE path
	 * @return array containing JRE container with module attribute if modular
	 */
	private IClasspathEntry[] updateWithModuleAttribute(IClasspathEntry[] cpEntries, IPath newPath) {
		try {
			if (cpEntries.length == 1) {
				IVMInstall vmInstall= JavaRuntime.getVMInstall(newPath);
				if (vmInstall != null) {
					boolean modularJava= JavaRuntime.isModularJava(vmInstall);
					if (modularJava) {
						IClasspathEntry jre= cpEntries[0];
						IClasspathAttribute[] newAttributes= addModuleAttributeIfNeeded(jre.getExtraAttributes());
						if (newAttributes != null) {
							IClasspathEntry jreModular= JavaCore.newContainerEntry(jre.getPath(), jre.getAccessRules(),
									newAttributes, jre.isExported());
							if (jreModular != null) {
								return new IClasspathEntry[] { jreModular };
							}
						}
					}
				}
			}
		} catch (Exception e) {
			JavaPlugin.log(e);
		}
		return cpEntries;
	}

	private IClasspathAttribute[] addModuleAttributeIfNeeded(IClasspathAttribute[] extraAttributes) {
		String TRUE = "true"; //$NON-NLS-1$
		for (int j= 0; j < extraAttributes.length; j++) {
			IClasspathAttribute classpathAttribute= extraAttributes[j];
			if (IClasspathAttribute.MODULE.equals(classpathAttribute.getName())) {
				if (TRUE.equals(classpathAttribute.getValue())) {
					return null; // no change required
				}
				extraAttributes[j]= JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, TRUE);
				return extraAttributes;
			}
		}
		extraAttributes= Arrays.copyOf(extraAttributes, extraAttributes.length+1);
		extraAttributes[extraAttributes.length-1]= JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, TRUE);
		return extraAttributes;
	}

	/**
	 * Returns the source class path entries to be added on new projects.
	 * The underlying resources may not exist. All entries that are returned must be of kind
	 * {@link IClasspathEntry#CPE_SOURCE}.
	 *
	 * @return returns the source class path entries for the new project
	 */
	public IClasspathEntry[] getSourceClasspathEntries() {
		IPath sourceFolderPath= new Path(getProjectName()).makeAbsolute();

		if (fLayoutGroup.isSrcBin()) {
			IPath srcPath= new Path(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_SRCNAME));
			if (srcPath.segmentCount() > 0) {
				sourceFolderPath= sourceFolderPath.append(srcPath);
			}
		}
		return new IClasspathEntry[] {  JavaCore.newSourceEntry(sourceFolderPath) };
	}

	/**
	 * Returns the source class path entries to be added on new projects.
	 * The underlying resource may not exist.
	 *
	 * @return returns the default class path entries
	 */
	public IPath getOutputLocation() {
		IPath outputLocationPath= new Path(getProjectName()).makeAbsolute();
		if (fLayoutGroup.isSrcBin()) {
			IPath binPath= new Path(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
			if (binPath.segmentCount() > 0) {
				outputLocationPath= outputLocationPath.append(binPath);
			}
		}
		return outputLocationPath;
	}

	/**
	 * Returns the working sets to which the new project should be added.
	 *
	 * @return the selected working sets to which the new project should be added
	 */
	public IWorkingSet[] getWorkingSets() {
		return fWorkingSetGroup.getSelectedWorkingSets();
	}

	/**
	 * Sets the working sets to which the new project should be added.
	 *
	 * @param workingSets the initial selected working sets
	 */
	public void setWorkingSets(IWorkingSet[] workingSets) {
		if (workingSets == null) {
			throw new IllegalArgumentException();
		}
		fWorkingSetGroup.setWorkingSets(workingSets);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fNameGroup.postSetFocus();
			updateSize();
		}
	}

	private GridLayout initGridLayout(GridLayout layout, boolean margins) {
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

	private static final IWorkingSet[] EMPTY_WORKING_SET_ARRAY = new IWorkingSet[0];

	private IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection, IWorkbenchPart activePart) {
		IWorkingSet[] selected= getSelectedWorkingSet(selection);
		if (selected != null && selected.length > 0) {
			for (IWorkingSet s : selected) {
				if (!isValidWorkingSet(s)) {
					return EMPTY_WORKING_SET_ARRAY;
				}
			}
			return selected;
		}

		if (!(activePart instanceof PackageExplorerPart))
			return EMPTY_WORKING_SET_ARRAY;

		PackageExplorerPart explorerPart= (PackageExplorerPart) activePart;
		if (explorerPart.getRootMode() == PackageExplorerPart.PROJECTS_AS_ROOTS) {
			//Get active filter
			IWorkingSet filterWorkingSet= explorerPart.getFilterWorkingSet();
			if (filterWorkingSet == null)
				return EMPTY_WORKING_SET_ARRAY;

			if (!isValidWorkingSet(filterWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			return new IWorkingSet[] {filterWorkingSet};
		} else {
			//If we have been gone into a working set return the working set
			Object input= explorerPart.getViewPartInput();
			if (!(input instanceof IWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			IWorkingSet workingSet= (IWorkingSet)input;
			if (!isValidWorkingSet(workingSet))
				return EMPTY_WORKING_SET_ARRAY;

			return new IWorkingSet[] {workingSet};
		}
	}

	private IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof ITreeSelection))
			return EMPTY_WORKING_SET_ARRAY;

		ITreeSelection treeSelection= (ITreeSelection) selection;
		if (treeSelection.isEmpty())
			return EMPTY_WORKING_SET_ARRAY;

		List<?> elements= treeSelection.toList();
		if (elements.size() == 1) {
			Object element= elements.get(0);
			TreePath[] paths= treeSelection.getPathsFor(element);
			if (paths.length != 1)
				return EMPTY_WORKING_SET_ARRAY;

			TreePath path= paths[0];
			if (path.getSegmentCount() == 0)
				return EMPTY_WORKING_SET_ARRAY;

			Object candidate= path.getSegment(0);
			if (!(candidate instanceof IWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			IWorkingSet workingSetCandidate= (IWorkingSet) candidate;
			if (isValidWorkingSet(workingSetCandidate))
				return new IWorkingSet[] { workingSetCandidate };

			return EMPTY_WORKING_SET_ARRAY;
		}

		ArrayList<IWorkingSet> result= new ArrayList<>();
		for (Object element : elements) {
			if (element instanceof IWorkingSet && isValidWorkingSet((IWorkingSet) element)) {
				result.add((IWorkingSet) element);
			}
		}
		return result.toArray(new IWorkingSet[result.size()]);
	}


	private static boolean isValidWorkingSet(IWorkingSet workingSet) {
		String id= workingSet.getId();
		if (!IWorkingSetIDs.JAVA.equals(id) && !IWorkingSetIDs.RESOURCE.equals(id))
			return false;

		if (workingSet.isAggregateWorkingSet())
			return false;

		return true;
	}
}
