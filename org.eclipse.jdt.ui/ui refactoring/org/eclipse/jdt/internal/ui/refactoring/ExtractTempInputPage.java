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

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class ExtractTempInputPage extends TextInputWizardPage {

	private Label fLabel;
	private final boolean fInitialValid;
	
	public ExtractTempInputPage(String initialValue) {
		super(true, initialValue);
		fInitialValid= ! ("".equals(initialValue));
		setMessage(RefactoringMessages.getString("ExtractTempInputPage.enter_name")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(RefactoringMessages.getString("ExtractTempInputPage.variable_name")); //$NON-NLS-1$
		
		Text text= createTextInputField(result);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		layouter.perform(label, text, 1);
		
		addReplaceAllCheckbox(result, layouter);
		addDeclareFinalCheckbox(result, layouter);
		addSeparator(result, layouter);
		addLabel(result, layouter);
		
		validateTextField(text.getText());
		
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.EXTRACT_TEMP_WIZARD_PAGE);		
	}
	
	private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
		String title= RefactoringMessages.getString("ExtractTempInputPage.replace_all"); //$NON-NLS-1$
		boolean defaultValue= getExtractTempRefactoring().replaceAllOccurrences();
		final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
		getExtractTempRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getExtractTempRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
			}
		});		
	}
	
	private void addDeclareFinalCheckbox(Composite result, RowLayouter layouter) {
		String title= RefactoringMessages.getString("ExtractTempInputPage.declare_final"); //$NON-NLS-1$
		boolean defaultValue= getExtractTempRefactoring().declareFinal();
		final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
		getExtractTempRefactoring().setDeclareFinal(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getExtractTempRefactoring().setDeclareFinal(checkBox.getSelection());
			}
		});		
	}
	
	private void addLabel(Composite result, RowLayouter layouter) {
		fLabel= new Label(result, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(50);
		fLabel.setLayoutData(gd);
		updatePreviewLabel();
		layouter.perform(fLabel);
	}

	private void addSeparator(Composite result, RowLayouter layouter) {
		Label separator= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		layouter.perform(separator);
	}
	
	private void updatePreviewLabel(){
		try {
			if (fLabel != null)
				fLabel.setText(RefactoringMessages.getString("ExtractTempInputPage.signature_preview") + getExtractTempRefactoring().getTempSignaturePreview()); //$NON-NLS-1$
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("ExtractTempInputPage.extract_local"), RefactoringMessages.getString("ExtractTempInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#textModified(java.lang.String)
	 */
	protected void textModified(String text) {
		getExtractTempRefactoring().setTempName(text);
		updatePreviewLabel();
		super.textModified(text);
	}
	
	
	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
	 */
	protected RefactoringStatus validateTextField(String text) {
		return getExtractTempRefactoring().checkTempName(text);
	}	
	
	private ExtractTempRefactoring getExtractTempRefactoring(){
		return (ExtractTempRefactoring)getRefactoring();
	}
	
	private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter){
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		layouter.perform(checkBox);
		return checkBox;		
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#isInitialInputValid()
	 */
	protected boolean isInitialInputValid() {
		return fInitialValid;
	}

}
