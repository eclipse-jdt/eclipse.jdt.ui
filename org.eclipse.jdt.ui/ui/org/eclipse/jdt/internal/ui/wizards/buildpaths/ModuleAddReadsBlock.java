/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.BidiUtils;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddReads;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;


/**
 * UI to define one additional exports (add-reads).
 */
public class ModuleAddReadsBlock {

	private final IStatusChangeListener fContext;

	private StringDialogField fSourceModule;
	private StringDialogField fTargetModule;

	private IStatus fSourceModuleStatus;
	private IStatus fTargetModuleStatus;

	private Control fSWTWidget;

	private final ModuleAddReads fInitialValue;

	private IJavaElement[] fSourceJavaElements;

	/**
	 * @param context listeners for status updates
	 * @param sourceJavaElements java element representing the source modules from where packages should be exported
	 * @param initialValue The value to edit
	 */
	public ModuleAddReadsBlock(IStatusChangeListener context, IJavaElement[] sourceJavaElements, ModuleAddReads initialValue) {
		fContext= context;
		fInitialValue= initialValue;
		fSourceJavaElements= sourceJavaElements;

		fSourceModuleStatus= new StatusInfo();

		IDialogFieldListener adapter= this::addReadsDialogFieldChanged;

		// create the dialog fields (no widgets yet)
		fSourceModule= new StringDialogField();
		fSourceModule.setDialogFieldListener(adapter);
		fSourceModule.setLabelText(NewWizardMessages.AddReadsBlock_sourceModule_label);

		fTargetModule= new StringDialogField();
		fTargetModule.setDialogFieldListener(adapter);
		fTargetModule.setLabelText(NewWizardMessages.AddReadsBlock_targetModule_label);

		setDefaults();
	}

	private void setDefaults() {
		if (fInitialValue != null) {
			fSourceModule.setText(fInitialValue.fSourceModule);
			if (!fInitialValue.fSourceModule.isEmpty() && (fSourceJavaElements == null || fSourceJavaElements.length <= 1)) {
				fSourceModule.setEnabled(false);
			}
			fTargetModule.setText(fInitialValue.fTargetModule);
			fTargetModule.setEnabled(true);
		}
	}

	private Set<String> moduleNames() {
		Set<String> moduleNames= new HashSet<>();
		if (fSourceJavaElements != null) {
			for (IJavaElement element : fSourceJavaElements) {
				if (element instanceof IPackageFragmentRoot) {
					IModuleDescription module= ((IPackageFragmentRoot) element).getModuleDescription();
					if (module != null) {
						moduleNames.add(module.getElementName());
					}
				}
			}
		}
		return moduleNames;
	}

	private String getSourceModuleText() {
		return fSourceModule.getText().trim();
	}

	private String getTargetModulesText() {
		return fTargetModule.getText().trim();
	}

	/**
	 * Gets the add-reads value entered by the user
	 * @return the add-reads value, or an empty string if any of the fields was left empty.
	 */
	public String getValue() {
		String sourceModule= getSourceModuleText();
		String targetModules= getTargetModulesText();
		if (sourceModule.isEmpty() || targetModules.isEmpty())
			return ""; //$NON-NLS-1$
		return sourceModule+'='+targetModules;
	}

	public ModuleAddReads getReads(CPListElementAttribute parentAttribute) {
		String sourceModule= getSourceModuleText();
		String targetModules= getTargetModulesText();
		if (sourceModule.isEmpty() || targetModules.isEmpty())
			return null;
		return new ModuleAddReads(sourceModule, targetModules, parentAttribute);
	}

	/**
	 * Creates the control
	 * @param parent the parent
	 * @return the created control
	 */
	public Control createControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		fSWTWidget= parent;

		Composite composite= new Composite(parent, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);


		int widthHint= converter.convertWidthInCharsToPixels(60);

		GridData gd= new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2, 1);
		gd.widthHint= converter.convertWidthInCharsToPixels(50);

		Label message= new Label(composite, SWT.LEFT + SWT.WRAP);
		message.setLayoutData(gd);
		message.setText(NewWizardMessages.AddReadsBlock_message);

		DialogField.createEmptySpace(composite, 2);

		fSourceModule.doFillIntoGrid(composite, 2);
		Text sourceModuleField= fSourceModule.getTextControl(null);
		LayoutUtil.setWidthHint(sourceModuleField, widthHint);
		LayoutUtil.setHorizontalGrabbing(sourceModuleField);
		BidiUtils.applyBidiProcessing(sourceModuleField, StructuredTextTypeHandlerFactory.JAVA);
		if (fSourceJavaElements != null) {
			ModuleDialog.configureModuleContentAssist(fSourceModule.getTextControl(composite), moduleNames());
		}

		DialogField.createEmptySpace(composite, 2);

		fTargetModule.doFillIntoGrid(composite, 2);
		Text targetModulesField= fTargetModule.getTextControl(null);
		LayoutUtil.setWidthHint(targetModulesField, widthHint);
		LayoutUtil.setHorizontalGrabbing(targetModulesField);
		BidiUtils.applyBidiProcessing(targetModulesField, StructuredTextTypeHandlerFactory.JAVA);
		// TODO: content assist from all known modules?

		DialogField.createEmptySpace(composite, 2);

		Dialog.applyDialogFont(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.EXTERNAL_ANNOTATIONS_ATTACHMENT_DIALOG); // FIXME
		return composite;
	}

	// ---------- IDialogFieldListener --------

	private void addReadsDialogFieldChanged(DialogField field) {
		if (fSWTWidget != null) {
			if (field == fSourceModule && fSourceModule.isEnabled()) {
				updateSourceModuleStatus();
			} else if (field == fTargetModule && fTargetModule.isEnabled()) {
				updateTargetModuleStatus();
			}
			doStatusLineUpdate();
		}
	}

	private void updateSourceModuleStatus() {
		fSourceModuleStatus= computeSourceModuleStatus(getSourceModuleText());
		if (fSourceModuleStatus.isOK()) {
			if (getTargetModulesText().isEmpty() && fTargetModuleStatus == null) {
				fTargetModuleStatus= ModuleDialog.newSilentError();
			}
		}
	}

	private void updateTargetModuleStatus() {
		fTargetModuleStatus= computeTargetModuleStatus(getTargetModulesText());
	}

	private IStatus computeSourceModuleStatus(String value) {
		StatusInfo status= new StatusInfo();
		if (value.isEmpty()) {
			status.setError(NewWizardMessages.ModuleAddExportsBlock_sourceModuleEmpty_error);
			return status;
		}
		if (moduleNames().contains(value)) {
			return status;
		}
		status.setError(Messages.format(NewWizardMessages.ModuleAddExportsBlock_wrongSourceModule_error, value));
		return status;
	}

	private IStatus computeTargetModuleStatus(String value) {
		StatusInfo status= new StatusInfo();
		if (value.isEmpty()) {
			status.setError(NewWizardMessages.ModuleAddExportsBlock_targetModuleEmpty_error);
			return status;
		}
		String source= "9", compliance= "9"; //$NON-NLS-1$ //$NON-NLS-2$
		if (fSourceJavaElements != null && fSourceJavaElements.length > 0) {
			IJavaProject project= fSourceJavaElements[0].getJavaProject();
			source= project.getOption(JavaCore.COMPILER_SOURCE, true);
			compliance= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		}
		if (JavaConventions.validateModuleName(value, source, compliance).getSeverity() == IStatus.ERROR) {
			status.setError(Messages.format(NewWizardMessages.ModuleAddExportsBlock_illegalTargetModule_error, value));
		}
		return status;

	}

	private void doStatusLineUpdate() {
		IStatus status= null;
		if (!fSourceModuleStatus.isOK()) {
			status= fSourceModuleStatus; 	// priority
		} else {
			status= fTargetModuleStatus;
		}
		if (status == null) {
			status= Status.OK_STATUS;
		}
		fContext.statusChanged(status);
	}
}
