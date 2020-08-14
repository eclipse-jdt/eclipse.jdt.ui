/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * A dialog to configure the source attachment of a library (class folder, archives
 * and variable entries).
 *
 */
public class SourceAttachmentDialog extends StatusDialog {

	private SourceAttachmentBlock fSourceAttachmentBlock;

	/**
	 * Creates an instance of the SourceAttachmentDialog. After
	 * <code>open</code>, the edited paths can be accessed from
	 * the classpath entry returned by <code>getResult</code>
	 * @param parent Parent shell for the dialog
	 * @param entry The entry to edit.
	 */
	public SourceAttachmentDialog(Shell parent, IClasspathEntry entry) {
		this(parent, entry, false);
	}

	/**
	 * Creates an instance of the SourceAttachmentDialog. After
	 * <code>open</code>, the edited paths can be accessed from
	 * the classpath entry returned by <code>getResult</code>
	 * @param parent Parent shell for the dialog
	 * @param entry The entry to edit.
	 * @param canEditEncoding whether the source attachment encoding can be edited
	 */
	public SourceAttachmentDialog(Shell parent, IClasspathEntry entry, boolean canEditEncoding) {
		super(parent);

		IStatusChangeListener listener= this::updateStatus;
		fSourceAttachmentBlock= new SourceAttachmentBlock(listener, entry, canEditEncoding);

		setTitle(NewWizardMessages.SourceAttachmentDialog_title);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @since 3.4
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.SOURCE_ATTACHMENT_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		Control inner= createSourceAttachmentControls(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Creates the controls for the source attachment configuration.
	 *
	 * @param composite the parent composite
	 * @return the control
	 */
	protected Control createSourceAttachmentControls(Composite composite) {
		return fSourceAttachmentBlock.createControl(composite);
	}

	/**
	 * Returns the configured class path entry.
	 *
	 * @return the configured class path entry
	 */
	public IClasspathEntry getResult() {
		return fSourceAttachmentBlock.getNewEntry();
	}
}
