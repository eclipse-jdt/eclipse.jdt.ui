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
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CreateProfileDialog extends StatusDialog implements IDialogConstants {
	
	Label fNameLabel;
	Text fNameText;
	Label fProfileLabel;
	Combo fProfileCombo;
	StatusInfo fOk, fEmpty, fDuplicate;

	final ProfileManager fProfileManager;
	List fSortedProfiles;
	String [] fSortedNames;
	
	Profile fBaseProfile;
	
	private String fNewKey;

	/**
	 * @param parentShell
	 */
	public CreateProfileDialog(Shell parentShell, ProfileManager profileManager) {
		super(parentShell);
		fProfileManager= profileManager;
		fSortedProfiles= fProfileManager.getSortedProfiles();
		fSortedNames= fProfileManager.getSortedNames();
		fOk= new StatusInfo();
		fDuplicate= new StatusInfo(IStatus.ERROR, "A profile with this name already exists");
		fEmpty= new StatusInfo(IStatus.ERROR, "Enter a new profile name");
		
	}
	
	
	public void create() {
		super.create();
		setTitle("New Profile");
//		setMessage("Create a new code formatting profile");
	}
	
	public Control createDialogArea(Composite parent) {
				
		final int numColumns= 2;
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertHorizontalDLUsToPixels(VERTICAL_MARGIN);
		layout.marginWidth= convertVerticalDLUsToPixels(HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(HORIZONTAL_SPACING);
		layout.numColumns= numColumns;
		Composite area= new Composite(parent, SWT.NULL);
		area.setLayout(layout);
		
		// Create "Profile Name:" label
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns;
		gd.widthHint= convertWidthInCharsToPixels(60);
		fNameLabel = new Label(area, SWT.NONE);
		fNameLabel.setText("Profile name:");
		fNameLabel.setLayoutData(gd);
		
		// Create text field to enter name
		gd = new GridData( GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		fNameText= new Text(area, SWT.SINGLE | SWT.BORDER);
		fNameText.setLayoutData(gd);
		fNameText.addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});
		
		// Create "New Profile..." label
		gd = new GridData();
		gd.horizontalSpan = numColumns;
		fProfileLabel = new Label(area, SWT.WRAP);
		fProfileLabel.setText("Initialize settings using the following profile:");
		fProfileLabel.setLayoutData(gd);
		
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		fProfileCombo = new Combo(area, SWT.DROP_DOWN | SWT.READ_ONLY);
		fProfileCombo.setLayoutData(gd);
		fProfileCombo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		fProfileCombo.setItems(fSortedNames);
		fProfileCombo.setText(fProfileManager.getProfile(ProfileManager.DEFAULT_PROFILE).getName());
		updateStatus(fEmpty);
		
		return area;
	}


	/**
	 * Validate the current settings
	 */
	protected void doValidation() {
		final String name= fNameText.getText();
		
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
	
	
	public String getNewKey() {
		return fNewKey;
	}
		
	protected void okPressed() {
		if (!getStatus().isOK()) return;
		final Map baseSettings= new HashMap(((Profile)fSortedProfiles.get(fProfileCombo.getSelectionIndex())).getSettings());
		final String profileName= fNameText.getText();			
		fProfileManager.createProfile(profileName, baseSettings);
		super.okPressed();
	}
}














