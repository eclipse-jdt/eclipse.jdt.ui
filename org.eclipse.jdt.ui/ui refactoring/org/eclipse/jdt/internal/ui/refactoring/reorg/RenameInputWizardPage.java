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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

abstract class RenameInputWizardPage extends TextInputWizardPage {

	private String fHelpContextID;
	private Button fUpdateReferences;
	private Button fUpdateJavaDoc;
	private Button fUpdateComments;
	private Button fUpdateStrings;
	private Button fUpdateQualifiedNames;
	private QualifiedNameComponent fQualifiedNameComponent;
	private static final String UPDATE_REFERENCES= "updateReferences"; //$NON-NLS-1$
	private static final String UPDATE_JAVADOC= "updateJavaDoc"; //$NON-NLS-1$
	private static final String UPDATE_COMMENTS= "updateComments"; //$NON-NLS-1$
	private static final String UPDATE_STRINGS= "updateStrings"; //$NON-NLS-1$
	private static final String UPDATE_QUALIFIED_NAMES= "updateQualifiedNames"; //$NON-NLS-1$
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 * @param initialSetting the initialSetting.
	 */
	public RenameInputWizardPage(String description, String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(description, isLastUserPage, initialValue);
		fHelpContextID= contextHelpId;
	}
	
	/* non java-doc
	 * @see DialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite superComposite= new Composite(parent, SWT.NONE);
		setControl(superComposite);
		initializeDialogUnits(superComposite);
		
		superComposite.setLayout(new GridLayout());
		Composite composite= new Composite(superComposite, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		composite.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		Label label= new Label(composite, SWT.NONE);
		label.setText(getLabelText());
		
		Text text= createTextInputField(composite);
		text.selectAll();
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(25);
		text.setLayoutData(gd);

				
		layouter.perform(label, text, 1);
		
		addOptionalUpdateReferencesCheckbox(composite, layouter);
		addOptionalUpdateCommentsAndStringCheckboxes(composite, layouter);
		addOptionalUpdateQualifiedNameComponent(composite, layouter, layout.marginWidth);
		
		Dialog.applyDialogFont(superComposite);
		WorkbenchHelp.setHelp(getControl(), fHelpContextID);
	}
	
	protected boolean saveSettings() {
		if (getContainer() instanceof Dialog)
			return ((Dialog)getContainer()).getReturnCode() == IDialogConstants.OK_ID;
		return true;
	}
	
	public void dispose() {
		if (saveSettings()) {
			saveBooleanSetting(UPDATE_REFERENCES, fUpdateReferences);
			saveBooleanSetting(UPDATE_JAVADOC, fUpdateJavaDoc);
			saveBooleanSetting(UPDATE_COMMENTS, fUpdateComments);
			saveBooleanSetting(UPDATE_STRINGS, fUpdateStrings);
			saveBooleanSetting(UPDATE_QUALIFIED_NAMES, fUpdateQualifiedNames);
			if (fQualifiedNameComponent != null)
				fQualifiedNameComponent.savePatterns(getRefactoringSettings());
		}
		super.dispose();
	}
	
	private void addOptionalUpdateReferencesCheckbox(Composite result, RowLayouter layouter) {
		final IReferenceUpdating ref= (IReferenceUpdating)getRefactoring().getAdapter(IReferenceUpdating.class);
		if (ref == null || !ref.canEnableUpdateReferences())	
			return;
		String title= RefactoringMessages.getString("RenameInputWizardPage.update_references"); //$NON-NLS-1$
		boolean defaultValue= getBooleanSetting(UPDATE_REFERENCES, ref.getUpdateReferences());
		fUpdateReferences= createCheckbox(result, title, defaultValue, layouter);
		ref.setUpdateReferences(fUpdateReferences.getSelection());
		fUpdateReferences.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ref.setUpdateReferences(fUpdateReferences.getSelection());
			}
		});		
	}
		
	private void addOptionalUpdateCommentsAndStringCheckboxes(Composite result, RowLayouter layouter) {
		ITextUpdating refactoring= (ITextUpdating)getRefactoring().getAdapter(ITextUpdating.class);
		
		if (refactoring == null || !refactoring.canEnableTextUpdating())
			return;
		
		addUpdateJavaDocCheckbox(result, layouter, refactoring);
		addUpdateCommentsCheckbox(result, layouter, refactoring);
		addUpdateStringsCheckbox(result, layouter, refactoring);
	}

	private void  addUpdateJavaDocCheckbox(Composite result, RowLayouter layouter, final ITextUpdating refactoring) {
		String title= RefactoringMessages.getString("RenameInputWizardPage.update_javadoc_references"); //$NON-NLS-1$
		boolean defaultValue= getBooleanSetting(UPDATE_JAVADOC, refactoring.getUpdateJavaDoc());
		fUpdateJavaDoc= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateJavaDoc(fUpdateJavaDoc.getSelection());
		fUpdateJavaDoc.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateJavaDoc(fUpdateJavaDoc.getSelection());
				updateForcePreview();
			}
		});		
	}
	
	private void  addUpdateCommentsCheckbox(Composite result, RowLayouter layouter, final ITextUpdating refactoring) {
		String title= RefactoringMessages.getString("RenameInputWizardPage.update_comment_references"); //$NON-NLS-1$
		boolean defaultValue= getBooleanSetting(UPDATE_COMMENTS, refactoring.getUpdateComments());
		fUpdateComments= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateComments(fUpdateComments.getSelection());
		fUpdateComments.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateComments(fUpdateComments.getSelection());
				updateForcePreview();
			}
		});		
	}

	private void addUpdateStringsCheckbox(Composite result, RowLayouter layouter, final ITextUpdating refactoring) {
		String title= RefactoringMessages.getString("RenameInputWizardPage.ppdate_string_references"); //$NON-NLS-1$
		boolean defaultValue= getBooleanSetting(UPDATE_STRINGS, refactoring.getUpdateStrings());
		fUpdateStrings= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateStrings(fUpdateStrings.getSelection());
		fUpdateStrings.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateStrings(fUpdateStrings.getSelection());
				updateForcePreview();
			}
		});		
	}

	private void addOptionalUpdateQualifiedNameComponent(Composite parent, RowLayouter layouter, int marginWidth) {
		final IQualifiedNameUpdating ref= (IQualifiedNameUpdating)getRefactoring().getAdapter(IQualifiedNameUpdating.class);
		if (ref == null || !ref.canEnableQualifiedNameUpdating())
			return;
		fUpdateQualifiedNames= new Button(parent, SWT.CHECK);
		int indent= marginWidth + fUpdateQualifiedNames.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		fUpdateQualifiedNames.setText(RefactoringMessages.getString("RenameInputWizardPage.update_qualified_names")); //$NON-NLS-1$
		layouter.perform(fUpdateQualifiedNames);
		
		fQualifiedNameComponent= new QualifiedNameComponent(parent, SWT.NONE, ref, getRefactoringSettings());
		layouter.perform(fQualifiedNameComponent);
		GridData gd= (GridData)fQualifiedNameComponent.getLayoutData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalIndent= indent;
		
		boolean defaultSelection= getBooleanSetting(UPDATE_QUALIFIED_NAMES, ref.getUpdateQualifiedNames());
		fUpdateQualifiedNames.setSelection(defaultSelection);
		fQualifiedNameComponent.setEnabled(defaultSelection);

		fUpdateQualifiedNames.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled= ((Button)e.widget).getSelection();
				fQualifiedNameComponent.setEnabled(enabled);
				ref.setUpdateQualifiedNames(enabled);
				updateForcePreview();
			}
		});
	}
	
	protected String getLabelText() {
		return RefactoringMessages.getString("RenameInputWizardPage.enter_name"); //$NON-NLS-1$
	}

	protected boolean getBooleanSetting(String key, boolean defaultValue) {
		String update= getRefactoringSettings().get(key);
		if (update != null)
			return Boolean.valueOf(update).booleanValue();
		else
			return defaultValue;
	}
	
	protected void saveBooleanSetting(String key, Button checkBox) {
		if (checkBox != null)
			getRefactoringSettings().put(key, checkBox.getSelection());
	}

	private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		layouter.perform(checkBox);
		return checkBox;		
	}
	
	private void updateForcePreview() {
		boolean forcePreview= false;
		Refactoring refactoring= getRefactoring();
		ITextUpdating tu= (ITextUpdating)refactoring.getAdapter(ITextUpdating.class);
		IQualifiedNameUpdating qu= (IQualifiedNameUpdating)refactoring.getAdapter(IQualifiedNameUpdating.class);
		if (tu != null) {
			forcePreview= tu.getUpdateComments() || tu.getUpdateJavaDoc() || tu.getUpdateStrings();
		}
		if (qu != null) {
			forcePreview |= qu.getUpdateQualifiedNames();
		}
		getRefactoringWizard().setPreviewReview(forcePreview);
	}
}
