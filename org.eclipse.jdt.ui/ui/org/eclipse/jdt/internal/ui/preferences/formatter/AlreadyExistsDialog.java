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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
 * The dialog to rename an imported profile. 
 */
public class AlreadyExistsDialog extends StatusDialog {
	
	private Composite fComposite;
	protected Text fNameText;
	private Button fRenameRadio, fOverwriteRadio;
	
	private final int numColumns= 2;
	
	private final StatusInfo fOk;
	private final StatusInfo fEmpty;
	private final StatusInfo fDuplicate;
	
	private final CustomProfile fProfile;
	private final ProfileManager fProfileManager;
	
	/**
	 * Create a new <code>AlreadyExistsDialog</code>.
	 */
	public AlreadyExistsDialog(Shell parentShell, CustomProfile profile, ProfileManager profileManager) {
		super(parentShell);
		fProfile= profile;
		fProfileManager= profileManager;
		fOk= new StatusInfo();
		fDuplicate= new StatusInfo(IStatus.ERROR, "A profile with this name already exists");
		fEmpty= new StatusInfo(IStatus.ERROR, "Profile name is empty");
	}
	
	
	public void create() {
		super.create();
		setTitle("Profile already exists");
	}
	
	public Control createDialogArea(Composite parent) {
				
		initializeComposite(parent);
		
		createLabel("A profile with the name '"	+ fProfile.getName() + "' already exists in this workspace. What would you like to do?");

		fRenameRadio= createRadioButton("&Rename the imported profile:");
		fNameText= createTextField();

		fOverwriteRadio= createRadioButton("&Overwrite the existing profile");

		fRenameRadio.setSelection(true);
		
		fNameText.setText(fProfile.getName());
		fNameText.setSelection(0, fProfile.getName().length());
		fNameText.setFocus();
		
		fNameText.addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});
		
		fRenameRadio.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fNameText.setEnabled(true);
				fNameText.setFocus();
				fNameText.setSelection(0, fNameText.getText().length());
				doValidation();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		fOverwriteRadio.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fNameText.setEnabled(false);
				doValidation();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		updateStatus(fDuplicate);
		
		return fComposite;
	}
	
	private void initializeComposite(Composite parent) {
		fComposite= new Composite(parent, SWT.NULL);
		
		final GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns= numColumns;
		
		fComposite.setLayout(layout);
	}
	
	private Label createLabel(String text) {
		final GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		gd.widthHint= convertWidthInCharsToPixels(60);
		final Label label= new Label(fComposite, SWT.WRAP);
		label.setText(text);
		label.setLayoutData(gd);
		return label;
	}
	
	private Button createRadioButton(String text) {
		final GridData gd = new GridData();
		gd.horizontalSpan = numColumns;
		gd.widthHint= convertWidthInCharsToPixels(60);
		final Button radio= new Button(fComposite, SWT.RADIO);
		radio.setLayoutData(gd);
		radio.setText(text);
		return radio;
	}
	
	private Text createTextField() {
		final GridData gd = new GridData( GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		final Text text= new Text(fComposite, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(gd);
		return text;
	}


	/**
	 * Validate the current settings
	 */
	protected void doValidation() {

		if (fOverwriteRadio.getSelection()) {
			updateStatus(fOk);
			return;
		}

		final String name= fNameText.getText().trim();
		
		if (name.length() == 0) {
			updateStatus(fEmpty);
			return;
		}
		
		if (fProfileManager.containsName(name)) {
			updateStatus(fDuplicate);
			return;
		}
		
		updateStatus(fOk);
	}
	
	
	protected void okPressed() {
		if (!getStatus().isOK()) 
			return;
		if (fRenameRadio.getSelection())
			fProfile.setName(fNameText.getText().trim());
		super.okPressed();
	}
}














