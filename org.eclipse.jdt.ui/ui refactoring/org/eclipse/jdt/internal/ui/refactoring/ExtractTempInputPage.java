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

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class ExtractTempInputPage extends TextInputWizardPage {

	public ExtractTempInputPage() {
		super(true);
		setMessage("Enter a name for the introduced local variable.");
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
		label.setText("Enter a name for the local variable:");
		
		Text text= createTextInputField(result);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		layouter.perform(label, text, 1);
		
		addReplaceAllCheckbox(result, layouter);
		addDeclareFinalCheckbox(result, layouter);
		//WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, fHelpContextID));		
	}
	
	private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
		String title= "Replace all occurrences of the selected expression with references to the local variable";
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
		String title= "Declare the local variable as 'final'";
		boolean defaultValue= getExtractTempRefactoring().declareFinal();
		final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
		getExtractTempRefactoring().setDeclareFinal(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getExtractTempRefactoring().setDeclareFinal(checkBox.getSelection());
			}
		});		
	}
	
	
	//overridden
	protected RefactoringStatus validateTextField(String text) {
		getExtractTempRefactoring().setTempName(text);
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

}
