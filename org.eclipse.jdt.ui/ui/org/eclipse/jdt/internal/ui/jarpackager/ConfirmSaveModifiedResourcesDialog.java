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
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

/**
 * This dialog displays a list of <code>IFile</code> and asks
 * the user to confirm saving all of them.
 * <p>
 * This concrete dialog class can be instantiated as is.
 * It is not intended to be subclassed.
 * </p>
 */
public class ConfirmSaveModifiedResourcesDialog extends MessageDialog {
	
	// String constants for widgets
	private static String TITLE= JarPackagerMessages.getString("ConfirmSaveModifiedResourcesDialog.title"); //$NON-NLS-1$
	private static String MESSAGE= JarPackagerMessages.getString("ConfirmSaveModifiedResourcesDialog.message"); //$NON-NLS-1$

	private TableViewer fList;
	private IFile[] fUnsavedFiles;
	
	public ConfirmSaveModifiedResourcesDialog(Shell parentShell, IFile[] unsavedFiles) {
		super(
			parentShell,
			TITLE,
			null,
			MESSAGE,
			QUESTION,
			new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
			0);
		fUnsavedFiles= unsavedFiles;
	}

	protected Control createCustomArea(Composite parent) {
		fList= new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fList.setContentProvider(new ListContentProvider());
		AppearanceAwareLabelProvider lprovider= new AppearanceAwareLabelProvider(
			AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS,
			AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS
		);
		lprovider.addLabelDecorator(new ProblemsLabelDecorator());
		fList.setLabelProvider(lprovider);
		fList.setInput(Arrays.asList(fUnsavedFiles));
		Control control= fList.getControl();
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(20);
		data.heightHint= convertHeightInCharsToPixels(5);
		control.setLayoutData(data);
		applyDialogFont(control);
		return control;
	}

	
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.CONFIRM_SAVE_MODIFIED_RESOURCES_DIALOG);
	}


}
