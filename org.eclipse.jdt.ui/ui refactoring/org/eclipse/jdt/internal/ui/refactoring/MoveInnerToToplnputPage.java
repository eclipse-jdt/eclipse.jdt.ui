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

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

class MoveInnerToToplnputPage extends TextInputWizardPage{

	private final boolean fIsInitialInputValid;
	private static final String DESCRIPTION = RefactoringMessages.getString("MoveInnerToToplnputPage.description"); //$NON-NLS-1$
	
	public MoveInnerToToplnputPage(String initialValue) {
		super(DESCRIPTION, true, initialValue);
		fIsInitialInputValid= ! ("".equals(initialValue)); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite newControl= new Composite(parent, SWT.NONE);
		setControl(newControl);
		WorkbenchHelp.setHelp(newControl, IJavaHelpContextIds.MOVE_INNER_TO_TOP_WIZARD_PAGE);
		newControl.setLayout(new GridLayout());
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		newControl.setLayout(layout);
		
		Label label= new Label(newControl, SWT.NONE);
		label.setText(RefactoringMessages.getString("MoveInnerToToplnputPage.enter_name")); //$NON-NLS-1$
		
		Text text= createTextInputField(newControl);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button finalCheckBox= new Button(newControl, SWT.CHECK);
		finalCheckBox.setText(RefactoringMessages.getString("MoveInnerToToplnputPage.instance_final")); //$NON-NLS-1$
		finalCheckBox.setSelection(getMoveRefactoring().isInstanceFieldMarkedFinal());
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		finalCheckBox.setLayoutData(gd);		
		finalCheckBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getMoveRefactoring().setMarkInstanceFieldAsFinal(finalCheckBox.getSelection());
			}
		});
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
	 */
	protected RefactoringStatus validateTextField(String text) {
		getMoveRefactoring().setEnclosingInstanceName(text);
		return getMoveRefactoring().checkEnclosingInstanceName(text);
	}	

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#isInitialInputValid()
	 */
	protected boolean isInitialInputValid() {
		return fIsInitialInputValid;
	}

	private MoveInnerToTopRefactoring getMoveRefactoring() {
		return (MoveInnerToTopRefactoring)getRefactoring();
	}
	
}
