/*******************************************************************************
 * Copyright (c) 2000, 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;

import org.eclipse.jdt.internal.ui.util.RowLayouter;

abstract class RenameInputWizardPage extends TextInputWizardPage {

	private String fHelpContextID;
	private QualifiedNameComponent fQualifiedNameComponent;
	
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
		
		WorkbenchHelp.setHelp(getControl(), fHelpContextID);
	}
	
	public void dispose() {
		if (fQualifiedNameComponent != null)
			fQualifiedNameComponent.savePatterns(getRefactoringSettings(), getContainer());
		super.dispose();
	}
	
	private void addOptionalUpdateCommentsAndStringCheckboxes(Composite result, RowLayouter layouter) {
		if (!(getRefactoring() instanceof ITextUpdatingRefactoring))
			return; 		
		ITextUpdatingRefactoring refactoring= (ITextUpdatingRefactoring)getRefactoring();
		if (!refactoring.canEnableTextUpdating())
			return;
		
		addUpdateJavaDocCheckbox(result, layouter, refactoring);
		addUpdateCommentsCheckbox(result, layouter, refactoring);
		addUpdateStringsCheckbox(result, layouter, refactoring);
	}

	private void addOptionalUpdateReferencesCheckbox(Composite result, RowLayouter layouter) {
		if (! (getRefactoring() instanceof IReferenceUpdatingRefactoring))
			return;
		final IReferenceUpdatingRefactoring ref= (IReferenceUpdatingRefactoring)getRefactoring();	
		if (! ref.canEnableUpdateReferences())	
			return;
		String title= RefactoringMessages.getString("RenameInputWizardPage.update_references"); //$NON-NLS-1$
		boolean defaultValue= ref.getUpdateReferences();
		final Button checkBox= createCheckbox(result, title, defaultValue, layouter);
		ref.setUpdateReferences(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ref.setUpdateReferences(checkBox.getSelection());
			}
		});		
	}
		
	private void addUpdateStringsCheckbox(Composite result, RowLayouter layouter, final ITextUpdatingRefactoring refactoring) {
		String title= RefactoringMessages.getString("RenameInputWizardPage.ppdate_string_references"); //$NON-NLS-1$
		boolean defaultValue= refactoring.getUpdateStrings();
		final Button checkBox= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateStrings(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateStrings(checkBox.getSelection());
				updatePreviewReview();
			}
		});		
	}

	private void  addUpdateCommentsCheckbox(Composite result, RowLayouter layouter, final ITextUpdatingRefactoring refactoring) {
		String title= RefactoringMessages.getString("RenameInputWizardPage.update_comment_references"); //$NON-NLS-1$
		boolean defaultValue= refactoring.getUpdateComments();
		final Button checkBox= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateComments(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateComments(checkBox.getSelection());
				updatePreviewReview();
			}
		});		
	}

	private void  addUpdateJavaDocCheckbox(Composite result, RowLayouter layouter, final ITextUpdatingRefactoring refactoring) {
		String title= RefactoringMessages.getString("RenameInputWizardPage.update_javadoc_references"); //$NON-NLS-1$
		boolean defaultValue= refactoring.getUpdateJavaDoc();
		final Button checkBox= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateJavaDoc(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateJavaDoc(checkBox.getSelection());
				updatePreviewReview();
			}
		});		
	}
		
	private void addOptionalUpdateQualifiedNameComponent(Composite parent, RowLayouter layouter, int marginWidth) {
		if (!(getRefactoring() instanceof IQualifiedNameUpdatingRefactoring))
			return;
		final IQualifiedNameUpdatingRefactoring ref= (IQualifiedNameUpdatingRefactoring)getRefactoring();
		if (!ref.canEnableQualifiedNameUpdating())
			return;
		Button checkbox= new Button(parent, SWT.CHECK);
		int indent= marginWidth + checkbox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		checkbox.setText(RefactoringMessages.getString("RenameInputWizardPage.update_qualified_names")); //$NON-NLS-1$
		layouter.perform(checkbox);
		
		fQualifiedNameComponent= new QualifiedNameComponent(parent, SWT.NONE, ref, getRefactoringSettings());
		layouter.perform(fQualifiedNameComponent);
		GridData gd= (GridData)fQualifiedNameComponent.getLayoutData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalIndent= indent;
		fQualifiedNameComponent.setEnabled(false);

		checkbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled= ((Button)e.widget).getSelection();
				fQualifiedNameComponent.setEnabled(enabled);
				ref.setUpdateQualifiedNames(enabled);
				updatePreviewReview();
			}
		});
	}
	
	protected String getLabelText() {
		return RefactoringMessages.getString("RenameInputWizardPage.enter_name"); //$NON-NLS-1$
	}

	private IRenameRefactoring getRenameRefactoring() {
		return (IRenameRefactoring)getRefactoring();
	}
	
	private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		layouter.perform(checkBox);
		return checkBox;		
	}
	
	private void updatePreviewReview() {
		boolean previewReview= false;
		IRefactoring ref= getRefactoring();
		if (ref instanceof ITextUpdatingRefactoring) {
			ITextUpdatingRefactoring tur= (ITextUpdatingRefactoring)ref;
			previewReview= tur.getUpdateComments() || tur.getUpdateJavaDoc() || tur.getUpdateStrings();
		}
		if (ref instanceof IQualifiedNameUpdatingRefactoring) {
			previewReview |= ((IQualifiedNameUpdatingRefactoring)ref).getUpdateQualifiedNames();
		}
		getRefactoringWizard().setPreviewReview(previewReview);
	}
}
