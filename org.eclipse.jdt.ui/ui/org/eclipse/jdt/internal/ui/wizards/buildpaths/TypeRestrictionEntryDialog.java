/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class TypeRestrictionEntryDialog extends StatusDialog {
	
	private StringDialogField fPatternDialog;
	private StatusInfo fPatternStatus;
	
	private String fPattern;
	private ComboDialogField fRuleKindCombo;
	private int[] fRuleKinds;
		
	public TypeRestrictionEntryDialog(Shell parent, IAccessRule ruleToEdit, CPListElement entryToEdit) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		String title, message;
		if (ruleToEdit == null) {
			title= NewWizardMessages.getString("TypeRestrictionEntryDialog.add.title"); //$NON-NLS-1$
		} else {
			title= NewWizardMessages.getString("TypeRestrictionEntryDialog.edit.title"); //$NON-NLS-1$
		}
		message= NewWizardMessages.getFormattedString("TypeRestrictionEntryDialog.pattern.label", entryToEdit.getPath().makeRelative().toString());  //$NON-NLS-1$
		setTitle(title);
		
		fPatternStatus= new StatusInfo();
		
		TypeRulesAdapter adapter= new TypeRulesAdapter();
		fPatternDialog= new StringDialogField();
		fPatternDialog.setLabelText(message);
		fPatternDialog.setDialogFieldListener(adapter);
		
		fRuleKindCombo= new ComboDialogField(SWT.READ_ONLY);
		fRuleKindCombo.setLabelText(NewWizardMessages.getString("TypeRestrictionEntryDialog.kind.label")); //$NON-NLS-1$
		fRuleKindCombo.setDialogFieldListener(adapter);
		String[] items= {
				NewWizardMessages.getString("TypeRestrictionEntryDialog.kind.non_accessible"), //$NON-NLS-1$
				NewWizardMessages.getString("TypeRestrictionEntryDialog.kind.discourraged"), //$NON-NLS-1$
				NewWizardMessages.getString("TypeRestrictionEntryDialog.kind.accessible") //$NON-NLS-1$
		};
		fRuleKinds= new int[] {
				IAccessRule.K_NON_ACCESSIBLE,
				IAccessRule.K_DISCOURAGED,
				IAccessRule.K_ACCESSIBLE
		};
		fRuleKindCombo.setItems(items);
		
		
		if (ruleToEdit == null) {
			fPatternDialog.setText(""); //$NON-NLS-1$
			fRuleKindCombo.selectItem(0);
		} else {
			fPatternDialog.setText(ruleToEdit.getPattern().toString());
			for (int i= 0; i < fRuleKinds.length; i++) {
				if (fRuleKinds[i] == ruleToEdit.getKind()) {
					fRuleKindCombo.selectItem(i);
					break;
				}
			}
		}
	}
	
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
				
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		
		Label description= new Label(inner, SWT.WRAP);
		description.setText(NewWizardMessages.getString("TypeRestrictionEntryDialog.description")); //$NON-NLS-1$

		GridData gd= new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		gd.widthHint= convertWidthInCharsToPixels(50);
		description.setLayoutData(gd);
		
		fRuleKindCombo.doFillIntoGrid(inner, 2);
		fPatternDialog.doFillIntoGrid(inner, 2);
				
		fPatternDialog.postSetFocusOnDialogField(parent.getDisplay());
		applyDialogFont(composite);		
		return composite;
	}

		
	// -------- TypeRulesAdapter --------

	private class TypeRulesAdapter implements IDialogFieldListener {
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}
	}
	

	protected void doStatusLineUpdate() {
		checkIfPatternValid();
		updateStatus(fPatternStatus);
	}		
	
	protected void checkIfPatternValid() {
		String pattern= fPatternDialog.getText().trim();
		if (pattern.length() == 0) {
			fPatternStatus.setError(NewWizardMessages.getString("TypeRestrictionEntryDialog.error.empty")); //$NON-NLS-1$
			return;
		}
		IPath path= new Path(pattern);
		if (path.isAbsolute() || path.getDevice() != null) {
			fPatternStatus.setError(NewWizardMessages.getString("TypeRestrictionEntryDialog.error.notrelative")); //$NON-NLS-1$
			return;
		}
		
		fPattern= pattern; 
		fPatternStatus.setOK();
	}
	
	public IAccessRule getRule() {
		IPath filePattern= new Path(fPattern);
		int kind= fRuleKinds[fRuleKindCombo.getSelectionIndex()];
		return JavaCore.newAccessRule(filePattern, kind);
	}
	
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.ACCESS_RULES_DIALOG);
	}
}
