/*******************************************************************************
 * Copyright (c) 2017, 2020 GK Software AG, and others.
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

import java.util.Collection;
import java.util.Set;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExpose;

/**
 * A dialog to configure add-exports of a library.
 *
 */
public class ModuleAddExportsDialog extends StatusDialog {

	private ModuleAddExportsBlock fAddExportsBlock;

	/**
	 * Creates an instance. After <code>open</code>, the edited export can be accessed using {@code #getResult()}
	 * or {@code #getExport()}.
	 *
	 * @param parent Parent shell for the dialog
	 * @param sourceJavaElements java elements representing the source modules from where packages should be exported
	 * @param possibleTargetModules modules to be offered in content assist, or {@code null}
	 * @param value The value to edit.
	 * @param alreadyExportedPackages The packages for which add-exports already done
	 */
	public ModuleAddExportsDialog(Shell parent, IJavaElement[] sourceJavaElements, Collection<String> possibleTargetModules, ModuleAddExpose value, Set<String> alreadyExportedPackages) {
		super(parent);

		IStatusChangeListener listener= this::updateStatus;
		fAddExportsBlock= new ModuleAddExportsBlock(listener, sourceJavaElements, possibleTargetModules, value, alreadyExportedPackages);

		setTitle(NewWizardMessages.AddExportsDialog_title);
		if (sourceJavaElements == null)
			updateStatus(new Status(IStatus.WARNING, JavaPlugin.getPluginId(),
					NewWizardMessages.AddModuleDetailsDialog_notPersisted_warning));
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.EXTERNAL_ANNOTATIONS_ATTACHMENT_DIALOG); // FIXME
	}

	@Override
	public void create() {
		super.create();
		updateButtonsEnableState(ModuleDialog.newSilentError()); // silently disable OK button until user input is given
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		Control inner= createAddExportsControls(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Creates the controls for the add-exports configuration.
	 *
	 * @param composite the parent composite
	 * @return the control
	 */
	protected Control createAddExportsControls(Composite composite) {
		return fAddExportsBlock.createControl(composite);
	}

	/**
	 * Returns the configured export value.
	 *
	 * @return the configured export value, or an empty string if no export was configured.
	 */
	public String getResult() {
		return fAddExportsBlock.getValue();
	}

	/**
	 * Returns the configured export value.
	 * @param parentAttribute the "module" attribute to which this export is associated
	 *
	 * @return the configured export value, or {@code null} if no export was configured.
	 */
	public ModuleAddExpose getExport(CPListElementAttribute parentAttribute) {
		return fAddExportsBlock.getExport(parentAttribute);
	}
}
