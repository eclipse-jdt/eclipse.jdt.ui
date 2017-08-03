/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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
import org.eclipse.jface.util.Util;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

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
	 * @param value The value to edit.
	 */
	public ModuleAddExportsDialog(Shell parent, IJavaElement[] sourceJavaElements, ModuleAddExport value) {
		super(parent);

		IStatusChangeListener listener= new IStatusChangeListener() {
			@Override
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};
		fAddExportsBlock= new ModuleAddExportsBlock(listener, sourceJavaElements, value);

		setTitle(NewWizardMessages.AddExportsDialog_title);
		if (sourceJavaElements == null)
			updateStatus(new Status(IStatus.WARNING, JavaPlugin.getPluginId(),
					NewWizardMessages.AddExportsDialog_notPersisted_warning));
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
		updateButtonsEnableState(newSilentError()); // silently disable OK button until user input is given 
	}

	public static Status newSilentError() {
		return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), Util.ZERO_LENGTH_STRING);
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
	 *
	 * @return the configured export value, or {@code null} if no export was configured.
	 */
	public ModuleAddExport getExport() {
		return fAddExportsBlock.getExport();
	}
}
