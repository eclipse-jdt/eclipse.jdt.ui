/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.refactoring;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.util.RowLayouter;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RenameInputWizardPage extends TextInputWizardPage{
	private String fHelpContextID;	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public RenameInputWizardPage(String contextHelpId, boolean isLastUserPage) {
		super(isLastUserPage);		fHelpContextID= contextHelpId;
	}
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 * @param initialSetting the initialSetting.
	 */
	public RenameInputWizardPage(String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(isLastUserPage, initialValue);		fHelpContextID= contextHelpId;
	}
	
	/* non java-doc
	 * @see DialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;		layout.verticalSpacing= 8;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(getLabelText());
		
		Text text= createTextInputField(result);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		layouter.perform(label, text, 1);				if (getRenameRefactoring().canEnableUpdateReferences()){			final Button checkBox= new Button(result, SWT.CHECK);			checkBox.setText("Update references to the renamed element");			checkBox.setSelection(getRenameRefactoring().getUpdateReferences());			checkBox.addSelectionListener(new SelectionAdapter(){				public void widgetSelected(SelectionEvent e) {					getRenameRefactoring().setUpdateReferences(checkBox.getSelection());				}			});			layouter.perform(checkBox);		}					WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, fHelpContextID));	}
	
	protected String getLabelText(){
		return RefactoringMessages.getString("RenameInputWizardPage.enter_name"); //$NON-NLS-1$
	}
	private IRenameRefactoring getRenameRefactoring(){		return (IRenameRefactoring)getRefactoring();	}
}
