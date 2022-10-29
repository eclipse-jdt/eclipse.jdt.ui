/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Frits Jalvingh <jal@etc.to> - Contribution for Bug 459831 - [launching] Support attaching external annotations to a JRE container
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * A dialog to configure the external annotations attachment of any classpath entry.
 *
 */
public class ExternalAnnotationsAttachmentDialog extends StatusDialog {

	/**
	 * Shows the UI for configuring an external annotations attachment. <code>null</code> is
	 * returned when the user cancels the dialog. The dialog does not apply any changes.
	 *
	 * @param shell The parent shell for the dialog
	 * @param initialEntry The entry to edit.
	 * @param javaProject The enclosing java project, or {@code null} in which case a container
	 * 	relative annotation path cannot be validated and has no content assist.
	 * @return Returns the selected path, possibly different from the initialEntry,
	 * or <code>null</code> if the dialog has been cancelled.
	 */
	public static IPath configureExternalAnnotationsAttachment(Shell shell, IPath initialEntry, IJavaProject javaProject) {
		ExternalAnnotationsAttachmentDialog dialog= new ExternalAnnotationsAttachmentDialog(shell, initialEntry, javaProject);
		if (dialog.open() == Window.OK) {
			return dialog.getResult();
		}
		return null;
	}

	private ExternalAnnotationsAttachmentBlock fAnnotationsAttachmentBlock;

	/**
	 * Creates an instance. After <code>open</code>, the edited path can be accessed from the
	 * classpath entry returned by <code>getResult</code>
	 *
	 * @param parent Parent shell for the dialog
	 * @param entry The entry to edit.
	 * @param javaProject The enclosing java project, or {@code null} in which case a container
	 * 	relative annotation path cannot be validated and has no content assist.
	 */
	protected ExternalAnnotationsAttachmentDialog(Shell parent, IPath entry, IJavaProject javaProject) {
		super(parent);

		IStatusChangeListener listener= this::updateStatus;
		fAnnotationsAttachmentBlock= new ExternalAnnotationsAttachmentBlock(listener, entry, javaProject);

		setTitle(NewWizardMessages.ExternalAnnotationsDialog_title);
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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.EXTERNAL_ANNOTATIONS_ATTACHMENT_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		Control inner= createAnnotationAttachmentControls(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Creates the controls for the annotations attachment configuration.
	 *
	 * @param composite the parent composite
	 * @return the control
	 */
	protected Control createAnnotationAttachmentControls(Composite composite) {
		return fAnnotationsAttachmentBlock.createControl(composite);
	}

	/**
	 * Returns the configured class path entry.
	 *
	 * @return the configured class path entry, or Path.EMPTY if no path was selected.
	 */
	protected IPath getResult() {
		return fAnnotationsAttachmentBlock.getAnnotationsPath();
	}
}
