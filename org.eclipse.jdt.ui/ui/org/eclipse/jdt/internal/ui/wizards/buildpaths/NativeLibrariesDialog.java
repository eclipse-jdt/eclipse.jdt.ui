/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.internal.ui.preferences.NativeLibrariesConfigurationBlock;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;


public class NativeLibrariesDialog extends StatusDialog {

	private final NativeLibrariesConfigurationBlock fConfigurationBlock;

	public NativeLibrariesDialog(Shell parent, String nativeLibPath, IClasspathEntry parentEntry) {
		super(parent);
		setTitle(NewWizardMessages.NativeLibrariesDialog_title);

		IStatusChangeListener listener= this::updateStatus;

		fConfigurationBlock= new NativeLibrariesConfigurationBlock(listener, parent, nativeLibPath, parentEntry);
		setHelpAvailable(false);
	}

	@Override
	protected boolean isResizable() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		Control inner= fConfigurationBlock.createContents(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		return composite;
	}

	public String getNativeLibraryPath() {
		return fConfigurationBlock.getNativeLibraryPath();
	}

}
