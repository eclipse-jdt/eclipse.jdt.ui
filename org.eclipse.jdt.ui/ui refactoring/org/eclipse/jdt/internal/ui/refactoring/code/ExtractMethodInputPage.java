/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.core.refactoring.code.ExtractMethodRefactoring;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class ExtractMethodInputPage extends TextInputWizardPage {

	private ExtractMethodRefactoring fRefactoring;
	private Label fPreview;
	private static final String PREFIX= "ExtractMethod.inputPage.";

	public ExtractMethodInputPage() {
		super(true);
		setDescription(RefactoringResources.getResourceString(PREFIX + "description"));
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
	}
	
	protected void textModified(String text) {
		super.textModified(text);
		updatePreview(text);
	}
	
	protected RefactoringStatus validateTextField(String text) {
		fRefactoring.setMethodName(text);
		updatePreview(text);
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
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.EXTRACT_METHOD_WIZARD_PAGE));		
	}	

	private String getLabelText(){
		return RefactoringResources.getResourceString(PREFIX + "newName.message");
	}
	
	private void setVisibility(String s) {
		fRefactoring.setVisibility(s);
		updatePreview(getText());
	}
	
	private void updatePreview(String text) {
		if (fPreview == null)
			return;
			
		if (text.length() == 0)
			text= "someMethodName";			
			
		fPreview.setText(fRefactoring.getSignature(text));
	}
}