/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;

public abstract class MessageInputPage extends UserInputWizardPage{	

	public static final int STYLE_NONE= 0;
	public static final int STYLE_INFORMATION= 1;
	public static final int STYLE_QUESTION= 2;
	public static final int STYLE_ERROR= 3;
	public static final int STYLE_WARNING= 4;

	private final int fStyle;

	public MessageInputPage(String pageName, boolean isLastUserPage, int style) {
		super(pageName, isLastUserPage);
		fStyle= style;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		result.setLayout(layout);
		Label spacer= new Label(result, SWT.NONE);
		GridData gd= new GridData();
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		gd.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		spacer.setLayoutData(gd);
		Composite labelComposite= new Composite(result, SWT.NONE);
		labelComposite.setLayoutData(new GridData());
		GridLayout labelLayout= new GridLayout();
		labelLayout.numColumns= 2;
		labelComposite.setLayout(labelLayout);
		Label imageLabel= new Label(labelComposite, SWT.LEFT);
		imageLabel.setImage(getMessageImage());
		Label textLabel= new Label(labelComposite, SWT.LEFT | SWT.WRAP);
		textLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		textLabel.setText(getMessageString());
		Dialog.applyDialogFont(result);
	}

	protected abstract String getMessageString();

	protected Image getMessageImage() {
		switch(fStyle){
			case STYLE_ERROR: return JFaceResources.getImage(Dialog.DLG_IMG_ERROR);
			case STYLE_WARNING: return JFaceResources.getImage(Dialog.DLG_IMG_WARNING);
			case STYLE_INFORMATION: return JFaceResources.getImage(Dialog.DLG_IMG_INFO);
			case STYLE_QUESTION: return JFaceResources.getImage(Dialog.DLG_IMG_QUESTION);
			default: return null;
		}
	}
}