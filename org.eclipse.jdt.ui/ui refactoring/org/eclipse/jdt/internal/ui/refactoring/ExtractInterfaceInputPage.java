package org.eclipse.jdt.internal.ui.refactoring;

import java.text.MessageFormat;

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
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class ExtractInterfaceInputPage extends TextInputWizardPage {

	public ExtractInterfaceInputPage() {
		super(true);
	}

	public ExtractInterfaceInputPage(String initialValue) {
		super(true, "");
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
		label.setText("Interface name:");
		
		Text text= createTextInputField(result);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		layouter.perform(label, text, 1);
		
		addReplaceAllCheckbox(result, layouter);
	}

	private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
		String key= "Change references to the class \'{0}\' into references to the interface (where possible)"; 
		String title= MessageFormat.format(key, new String[]{getExtractInterfaceRefactoring().getInputClass().getElementName()});
		boolean defaultValue= getExtractInterfaceRefactoring().isReplaceOccurrences();
		final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
		getExtractInterfaceRefactoring().setReplaceOccurrences(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getExtractInterfaceRefactoring().setReplaceOccurrences(checkBox.getSelection());
			}
		});		
	}

	private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter){
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		layouter.perform(checkBox);
		return checkBox;		
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
	 */
	protected RefactoringStatus validateTextField(String text) {
		getExtractInterfaceRefactoring().setNewInterfaceName(text);
		return getExtractInterfaceRefactoring().checkNewInterfaceName(text);
	}	

	private ExtractInterfaceRefactoring getExtractInterfaceRefactoring(){
		return (ExtractInterfaceRefactoring)getRefactoring();
	}
}
