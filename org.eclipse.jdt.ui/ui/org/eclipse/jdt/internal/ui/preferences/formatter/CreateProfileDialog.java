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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

/**
 * The dialog to create a new profile. 
 */
public class CreateProfileDialog extends StatusDialog {
	
	private Text fNameText;
	private Combo fProfileCombo;
	private Button fEditCheckbox;
	
	private final StatusInfo fOk, fEmpty, fDuplicate;

	private final ProfileManager fProfileManager;
	private final List fSortedProfiles;
	private final String [] fSortedNames;
	
	private CustomProfile fCreatedProfile;
	protected boolean fOpenEditDialog;
	
	/**
	 * Create a new CreateProfileDialog.
	 */
	public CreateProfileDialog(Shell parentShell, ProfileManager profileManager) {
		super(parentShell);
		fProfileManager= profileManager;
		fSortedProfiles= fProfileManager.getSortedProfiles();
		fSortedNames= fProfileManager.getSortedNames();
		fOk= new StatusInfo();
		fDuplicate= new StatusInfo(IStatus.ERROR, "A profile with this name already exists");
		fEmpty= new StatusInfo(IStatus.ERROR, "Profile name is empty");
	}
	
	
	public void create() {
		super.create();
		setTitle("New Code Formatter Profile");
	}
	
	public Control createDialogArea(Composite parent) {
				
		final int numColumns= 2;
		
		GridData gd;
		
		final GridLayout layout= new GridLayout(numColumns, false);
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		
		// Create "Profile name:" label
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = numColumns;
		gd.widthHint= convertWidthInCharsToPixels(60);
		final Label nameLabel = new Label(composite, SWT.WRAP);
		nameLabel.setText("Profile name:");
		nameLabel.setLayoutData(gd);
		
		// Create text field to enter name
		gd = new GridData( GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		fNameText= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fNameText.setLayoutData(gd);
		fNameText.addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});
		
		// Create "Initialize settings ..." label
		gd = new GridData();
		gd.horizontalSpan = numColumns;
		Label profileLabel = new Label(composite, SWT.WRAP);
		profileLabel.setText("Initialize settings with the following profile:");
		profileLabel.setLayoutData(gd);
		
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		fProfileCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fProfileCombo.setLayoutData(gd);
		
		
		// "Open the edit dialog now"
		gd= new GridData();
		gd.horizontalSpan= numColumns;
		fEditCheckbox= new Button(composite, SWT.CHECK);
		fEditCheckbox.setText("Open the edit dialog now");
		fEditCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fOpenEditDialog= ((Button)e.widget).getSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		fEditCheckbox.setSelection(true);
		fOpenEditDialog= true;
		
		fProfileCombo.setItems(fSortedNames);
		fProfileCombo.setText(fProfileManager.getProfile(ProfileManager.DEFAULT_PROFILE).getName());
		updateStatus(fEmpty);
		
		applyDialogFont(composite);
		
		return composite;
	}


	/**
	 * Validate the current settings
	 */
	protected void doValidation() {
		final String name= fNameText.getText().trim();
		
		if (fProfileManager.containsName(name)) {
			updateStatus(fDuplicate);
			return;
		}
		if (name.length() == 0) {
			updateStatus(fEmpty);
			return;
		}
		updateStatus(fOk);
	}
	
	
	protected void okPressed() {
		if (!getStatus().isOK()) 
			return;
		final Map baseSettings= new HashMap(((Profile)fSortedProfiles.get(fProfileCombo.getSelectionIndex())).getSettings());
		final String profileName= fNameText.getText();
		fCreatedProfile= new CustomProfile(profileName, baseSettings);
		fProfileManager.addProfile(fCreatedProfile);
		super.okPressed();
	}
	
	public final CustomProfile getCreatedProfile() {
		return fCreatedProfile;
	}
	
	public final boolean openEditDialog() {
		return fOpenEditDialog;
	}
}














