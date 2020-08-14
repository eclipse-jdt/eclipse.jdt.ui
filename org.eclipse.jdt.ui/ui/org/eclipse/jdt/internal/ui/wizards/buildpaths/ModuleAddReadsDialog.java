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
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddReads;

/**
 * A dialog to configure add-reads of a library.
 *
 */
public class ModuleAddReadsDialog extends StatusDialog {

	private ModuleAddReadsBlock fAddReadsBlock;

	/**
	 * Creates an instance. After <code>open</code>, the edited export can be accessed using {@code #getResult()}
	 * or {@code #getReads()}.
	 *
	 * @param parent Parent shell for the dialog
	 * @param sourceJavaElements java elements representing the source modules for which more reads should be added
	 * @param value The value to edit.
	 */
	public ModuleAddReadsDialog(Shell parent, IJavaElement[] sourceJavaElements, ModuleAddReads value) {
		super(parent);

		IStatusChangeListener listener= this::updateStatus;
		fAddReadsBlock= new ModuleAddReadsBlock(listener, sourceJavaElements, value);

		setTitle(NewWizardMessages.AddReadsDialog_title);
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

		Control inner= createAddReadsControls(composite);
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
	protected Control createAddReadsControls(Composite composite) {
		return fAddReadsBlock.createControl(composite);
	}

	/**
	 * Returns the configured export value.
	 *
	 * @return the configured export value, or an empty string if no export was configured.
	 */
	public String getResult() {
		return fAddReadsBlock.getValue();
	}

	/**
	 * Returns the configured export value.
	 * @param parentAttribute the "module" attribute to which this export is associated
	 *
	 * @return the configured export value, or {@code null} if no export was configured.
	 */
	public ModuleAddReads getReads(CPListElementAttribute parentAttribute) {
		return fAddReadsBlock.getReads(parentAttribute);
	}
}
