/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Group;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.core.refactoring.code.ExtractMethodRefactoring;import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage2;import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class ExtractMethodInputPage extends TextInputWizardPage2 {

	private ExtractMethodRefactoring fRefactoring;
	private Label fPreview;
	private static final String PREFIX= "ExtractMethod.inputPage.";

	public ExtractMethodInputPage() {
		super(true);
		setDescription(RefactoringResources.getResourceString(PREFIX + "description"));
	}
	
	protected void textModified(String text) {
		super.textModified(text);
		if (text.length() == 0 && fPreview != null)
			fPreview.setText("");
	}
	
	protected RefactoringStatus validateTextField(String text) {
		fRefactoring.setMethodName(text);
		updatePreview();
		return fRefactoring.checkMethodName();
	}
	
	public void createControl(Composite parent) {
		fRefactoring= (ExtractMethodRefactoring)getRefactoring();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(getLabelText());
		
		Text text= createTextInputField(result);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		layouter.perform(label, text, 1);
		
		label= new Label(result, SWT.NONE);
		label.setText("Access Modifiers:");
		
		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 4; layout.marginWidth= 0;
		group.setLayout(layout);
		
		String[] labels= new String[] {"public", "default", "protected", "private" };
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			radio.setText(labels[i]);
			radio.setData(labels[i]);
			if (i == 2)
				radio.setSelection(true);
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					setVisibility((String)event.widget.getData());
				}
			});
		}
		layouter.perform(label, group, 1);
		
		label= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layouter.perform(label);
		
		label= new Label(result, SWT.NONE);
		label.setText("Signature preview:");
		
		fPreview= new Label(result, SWT.NONE);
		fPreview.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		layouter.perform(label, fPreview, 1);
	}	

	private String getLabelText(){
		return RefactoringResources.getResourceString(PREFIX + "newName.message");
	}
	
	private void setVisibility(String s) {
		fRefactoring.setVisibility(s);
		updatePreview();
	}
	
	private void updatePreview() {
		String text= getText();
		if (text.length() != 0)
			text= fRefactoring.getSignature();
		fPreview.setText(text);
	}
}