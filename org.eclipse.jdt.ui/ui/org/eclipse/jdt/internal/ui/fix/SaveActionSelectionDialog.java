/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeFormatingTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeStyleTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MemberAccessesTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MissingCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.UnnecessaryCodeTabPage;

public class SaveActionSelectionDialog extends CleanUpSelectionDialog {
	
	private static final String PREFERENCE_KEY= "clean_up_save_particpant_modify_dialog"; //$NON-NLS-1$
	private static final int APPLY_BUTTON_ID= IDialogConstants.CLIENT_ID;
	
	private Map fOrginalValues;
	Button fApplyButton;
	
	public SaveActionSelectionDialog(Shell parentShell, Map settings) {
		super(parentShell, settings, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_CleanUpSaveParticipantConfiguration_Title);
		
		fOrginalValues= new HashMap(settings);
	}
	
	protected CleanUpTabPage[] createTabPages(Map workingValues) {
		CleanUpTabPage[] result= new CleanUpTabPage[5];
		
		result[0]= new CodeStyleTabPage(this, workingValues, true, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_CodeStyle_TabPage);
		result[1]= new MemberAccessesTabPage(this, workingValues, true, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_MemberAccesses_TabPage);
		result[2]= new UnnecessaryCodeTabPage(this, workingValues, true, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_UnnecessaryCode_TabPage);
		result[3]= new MissingCodeTabPage(this, workingValues, true, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_MissingCode_TabPage);
		result[4]= new CodeFormatingTabPage(this, workingValues, true, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_CodeOrganizing_TabPage);
		
		return result;
	}
	
	protected void okPressed() {
		applyPressed();
		super.okPressed();
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == APPLY_BUTTON_ID) {
			applyPressed();
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	private void applyPressed() {
		fOrginalValues= new HashMap(getWorkingValues());
		updateStatus(StatusInfo.OK_STATUS);
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		fApplyButton= createButton(parent, APPLY_BUTTON_ID, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_Apply_Button, false);
		fApplyButton.setEnabled(false);
		
		GridLayout layout= (GridLayout)parent.getLayout();
		layout.numColumns++;
		layout.makeColumnsEqualWidth= false;
		Label label= new Label(parent, SWT.NONE);
		GridData data= new GridData();
		data.widthHint= layout.horizontalSpacing;
		label.setLayoutData(data);
		super.createButtonsForButtonBar(parent);
	}
	
	protected void updateButtonsEnableState(IStatus status) {
		super.updateButtonsEnableState(status);
		if (fApplyButton != null && !fApplyButton.isDisposed()) {
			fApplyButton.setEnabled(hasChanges() && !status.matches(IStatus.ERROR));
		}
	}
	
	private boolean hasChanges() {
		for (Iterator iterator= getWorkingValues().keySet().iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			if (!getWorkingValues().get(key).equals(fOrginalValues.get(key)))
				return true;
		}
		return false;
	}

	protected String getEmptySelectionMessage() {
		return SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_SelectAnAction_Error;
	}
	
	protected String getSelectionCountMessage(int selectionCount, int size) {
		return Messages.format(SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_XofYSelected_Label, new Object[] {new Integer(selectionCount), new Integer(size)});
	}

	protected String getPreferenceKeyPrefix() {
		return PREFERENCE_KEY;
	}
}
