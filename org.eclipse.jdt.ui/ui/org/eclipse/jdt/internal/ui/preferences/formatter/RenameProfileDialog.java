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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;

/**
 * The dialog to create a new profile. 
 */
public class RenameProfileDialog extends StatusDialog {
	
	private Label fNameLabel;
	private Text fNameText;
	
	private final StatusInfo fOk;
	private final StatusInfo fEmpty;
	private final StatusInfo fDuplicate;

	private final CustomProfile fProfile;
	
	/**
	 * Create a new CreateProfileDialog.
	 */
	public RenameProfileDialog(Shell parentShell, CustomProfile profile) {
		super(parentShell);
		fProfile= profile;
		fOk= new StatusInfo();
		fDuplicate= new StatusInfo(IStatus.ERROR, "A profile with this name already exists");
		fEmpty= new StatusInfo(IStatus.ERROR, "Profile name is empty");
	}
	
	
	public void create() {
		super.create();
		setTitle("Rename Profile");
	}
	
	public Control createDialogArea(Composite parent) {
				
		final int numColumns= 2;
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns= numColumns;
		
		final Composite area= new Composite(parent, SWT.NULL);
		area.setLayout(layout);
		
		// Create "Please enter a new name:" label
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns;
		gd.widthHint= convertWidthInCharsToPixels(60);
		fNameLabel = new Label(area, SWT.NONE);
		fNameLabel.setText("Please &enter a new name:");
		fNameLabel.setLayoutData(gd);
		
		// Create text field to enter name
		gd = new GridData( GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		fNameText= new Text(area, SWT.SINGLE | SWT.BORDER);
		fNameText.setText(fProfile.getName());
		fNameText.setSelection(0, fProfile.getName().length());
		fNameText.setLayoutData(gd);
		fNameText.addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});

		updateStatus(fOk);
		
		return area;
	}


	/**
	 * Validate the current settings
	 */
	protected void doValidation() {
		final String name= fNameText.getText().trim();
		
		if (name.length() == 0) {
			updateStatus(fEmpty);
			return;
		}
		
		if (name.equals(fProfile.getName())) {
			updateStatus(fOk);
			return;
		}

		final ProfileManager profileManager= fProfile.getManager();
		
		if (profileManager.containsName(name)) {
			updateStatus(fDuplicate);
			return;
		}
		
		updateStatus(fOk);
	}
	
	
	protected void okPressed() {
		if (!getStatus().isOK()) 
			return;
		fProfile.setName(fNameText.getText());
		super.okPressed();
	}
}














