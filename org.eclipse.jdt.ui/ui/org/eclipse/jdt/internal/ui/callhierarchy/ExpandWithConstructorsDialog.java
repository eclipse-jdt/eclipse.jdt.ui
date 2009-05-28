/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/**
 * Configuration dialog for default "Expand with Constructors" behavior.
 * 
 * @since 3.5
 */
class ExpandWithConstructorsDialog extends StatusDialog {

	private static final String LINE_DELIMITER_REGEX= "\\r\\n?|\\n"; //$NON-NLS-1$

	private Button fAnonymousButton;
	private StyledText fDefaultTypesText;
	
	protected ExpandWithConstructorsDialog(Shell parentShell) {
		super(parentShell);
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.StatusDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CallHierarchyMessages.ExpandWithConstructorsDialog_title);
		setHelpAvailable(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.CALL_HIERARCHY_EXPAND_WITH_CONSTRUCTORS_DIALOG);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		((GridData)composite.getLayoutData()).widthHint= convertWidthInCharsToPixels(60);
		
		Label descriptionLabel= new Label(composite, SWT.WRAP);
		descriptionLabel.setText(CallHierarchyMessages.ExpandWithConstructorsDialog_explanation_label);
		descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		
		Label typesLabel= new Label(composite, SWT.WRAP);
		typesLabel.setText(CallHierarchyMessages.ExpandWithConstructorsDialog_typeNames_label);
		typesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		fDefaultTypesText= new StyledText(composite, SWT.BORDER | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint= convertHeightInCharsToPixels(10);
		fDefaultTypesText.setLayoutData(gd);
		
		String defaultTypesPref= PreferenceConstants.getPreferenceStore().getString(CallHierarchyContentProvider.PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS);
		String defaultTypesText= defaultTypesPref.replace(';', '\n');
		fDefaultTypesText.setText(defaultTypesText);
		fDefaultTypesText.setSelection(fDefaultTypesText.getCharCount());
		
		fDefaultTypesText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});
		
		
		fAnonymousButton= new Button(composite, SWT.CHECK);
		fAnonymousButton.setText(CallHierarchyMessages.ExpandWithConstructorsDialog_anonymousTypes_label);
		boolean anonymousPref= PreferenceConstants.getPreferenceStore().getBoolean(CallHierarchyContentProvider.PREF_ANONYMOUS_EXPAND_WITH_CONSTRUCTORS);
		fAnonymousButton.setSelection(anonymousPref);
		fAnonymousButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		return composite;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		PreferenceConstants.getPreferenceStore().setValue(CallHierarchyContentProvider.PREF_ANONYMOUS_EXPAND_WITH_CONSTRUCTORS, fAnonymousButton.getSelection());
		
		String defaultTypes= fDefaultTypesText.getText().trim();
		String defaultTypesPref= defaultTypes.replaceAll(LINE_DELIMITER_REGEX, ";"); //$NON-NLS-1$
		PreferenceConstants.getPreferenceStore().setValue(CallHierarchyContentProvider.PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS, defaultTypesPref);
		
		super.okPressed();
	}

	private void validateInput() {
		StatusInfo status= new StatusInfo();
		
		String[] defaultTypes= fDefaultTypesText.getText().split(LINE_DELIMITER_REGEX);
		for (int i= 0; i < defaultTypes.length; i++) {
			String type= defaultTypes[i];
			if (type.length() == 0)
				continue;
			
			IStatus typeNameStatus= JavaConventions.validateJavaTypeName(type, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3);
			if (typeNameStatus.getSeverity() == IStatus.ERROR) {
				status.setError(Messages.format(CallHierarchyMessages.ExpandWithConstructorsDialog_not_a_valid_type_name, type));
				break;
			}
		}
		
		updateStatus(status);
	}
}
