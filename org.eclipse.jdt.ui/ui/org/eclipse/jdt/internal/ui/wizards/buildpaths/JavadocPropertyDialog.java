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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.preferences.JavadocConfigurationBlock;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;


public class JavadocPropertyDialog extends StatusDialog {

	private JavadocConfigurationBlock fJavadocConfigurationBlock;
	
	public JavadocPropertyDialog(Shell parent, CPListElement element) {
		super(parent);
		
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};		
		
		setTitle(NewWizardMessages.getFormattedString("LibrariesWorkbookPage.JavadocPropertyDialog.title", element.getPath().toString())); //$NON-NLS-1$
		URL initialLocation= (URL) element.getAttribute(CPListElement.JAVADOC);
		fJavadocConfigurationBlock= new JavadocConfigurationBlock(parent, listener, initialLocation, false);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		Control inner= fJavadocConfigurationBlock.createContents(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);		
		return composite;
	}
	
	public URL getJavaDocLocation() {
		return fJavadocConfigurationBlock.getJavadocLocation();
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.JAVADOC_PROPERTY_DIALOG);
	}
}