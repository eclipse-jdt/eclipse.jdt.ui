/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.refactoring;import org.eclipse.jdt.internal.core.refactoring.tagging.IReferenceUpdatingRefactoring;import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;import org.eclipse.jdt.internal.core.refactoring.tagging.ITextUpdatingRefactoring;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.RowLayouter;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;

public class RenameInputWizardPage extends TextInputWizardPage{
	private String fHelpContextID;		private static final String SETTING_UPDATE_REFS= 				"RenameInputWizardPage.UpdateRefs";//$NON-NLS-1$	private static final String SETTING_UPDATE_JAVADOC= 		"RenameInputWizardPage.UpdateJavaDoc";//$NON-NLS-1$	private static final String SETTING_UPDATE_COMMENTS= 	"RenameInputWizardPage.UpdateComments";//$NON-NLS-1$	private static final String SETTING_UPDATE_STRINGS= 		"RenameInputWizardPage.UpdateStrings";//$NON-NLS-1$	
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
				
		layouter.perform(label, text, 1);				addOptionalUpdateReferencesCheckbox(result, layouter);		addOptionalUpdateCommentsAndStringCheckboxes(result, layouter);				WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, fHelpContextID));	}		private void addOptionalUpdateCommentsAndStringCheckboxes(Composite result, RowLayouter layouter) {		if (!(getRefactoring() instanceof ITextUpdatingRefactoring))			return; 				ITextUpdatingRefactoring refactoring= (ITextUpdatingRefactoring)getRefactoring();		if (!refactoring.canEnableTextUpdating())			return;				addUpdateJavaDocCheckbox(result, layouter, refactoring);		addUpdateCommentsCheckbox(result, layouter, refactoring);		addUpdateStringsCheckbox(result, layouter, refactoring);	}

	private void addOptionalUpdateReferencesCheckbox(Composite result, RowLayouter layouter) {		if (! (getRefactoring() instanceof IReferenceUpdatingRefactoring))			return;		final IReferenceUpdatingRefactoring ref= (IReferenceUpdatingRefactoring)getRefactoring();			if (! ref.canEnableUpdateReferences())				return;
		String title= "Update references to the renamed element";		boolean defaultValue= ref.getUpdateReferences();		final String propertyName= SETTING_UPDATE_REFS;		final Button checkBox= createCheckbox(result, propertyName, 	title, defaultValue, layouter);		ref.setUpdateReferences(checkBox.getSelection());		checkBox.addSelectionListener(new SelectionAdapter(){			public void widgetSelected(SelectionEvent e) {				ref.setUpdateReferences(checkBox.getSelection());				storeCheckboxState(checkBox, propertyName);			}		});		
	}			private void addUpdateStringsCheckbox(Composite result, RowLayouter layouter, final ITextUpdatingRefactoring refactoring) {		String title= "Update references in string literals";		boolean defaultValue= refactoring.getUpdateStrings();		final String propertyName= SETTING_UPDATE_STRINGS;		final Button checkBox= createCheckbox(result, propertyName, 	title, defaultValue, layouter);		refactoring.setUpdateStrings(checkBox.getSelection());		checkBox.addSelectionListener(new SelectionAdapter(){			public void widgetSelected(SelectionEvent e) {				refactoring.setUpdateStrings(checkBox.getSelection());				storeCheckboxState(checkBox, propertyName);			}		});			}	private void  addUpdateCommentsCheckbox(Composite result, RowLayouter layouter, final ITextUpdatingRefactoring refactoring) {		String title= "Update references in regular comments";		boolean defaultValue= refactoring.getUpdateComments();		final String propertyName= SETTING_UPDATE_COMMENTS;		final Button checkBox= createCheckbox(result, propertyName, 	title, defaultValue, layouter);		refactoring.setUpdateComments(checkBox.getSelection());		checkBox.addSelectionListener(new SelectionAdapter(){			public void widgetSelected(SelectionEvent e) {				refactoring.setUpdateComments(checkBox.getSelection());				storeCheckboxState(checkBox, propertyName);			}		});			}	private void  addUpdateJavaDocCheckbox(Composite result, RowLayouter layouter, final ITextUpdatingRefactoring refactoring) {		String title= "Update references in JavaDoc comments";		boolean defaultValue= refactoring.getUpdateJavaDoc();		final String propertyName= SETTING_UPDATE_JAVADOC;		final Button checkBox= createCheckbox(result, propertyName, 	title, defaultValue, layouter);		refactoring.setUpdateJavaDoc(checkBox.getSelection());		checkBox.addSelectionListener(new SelectionAdapter(){			public void widgetSelected(SelectionEvent e) {				refactoring.setUpdateJavaDoc(checkBox.getSelection());				storeCheckboxState(checkBox, propertyName);			}		});			}		
	protected String getLabelText(){
		return RefactoringMessages.getString("RenameInputWizardPage.enter_name"); //$NON-NLS-1$
	}
	private IRenameRefactoring getRenameRefactoring(){		return (IRenameRefactoring)getRefactoring();	}		private static Button createCheckbox(Composite parent, String propertyName, String title, boolean defaultValue, RowLayouter layouter){		Button checkBox= new Button(parent, SWT.CHECK);		checkBox.setText(title);		checkBox.setSelection(loadCheckboxState(propertyName, defaultValue));		storeCheckboxState(checkBox, propertyName);		layouter.perform(checkBox);		return checkBox;	}		private static boolean loadCheckboxState(String property, boolean defaultValue){		String res= JavaPlugin.getDefault().getDialogSettings().get(property);		if (res == null)			return defaultValue;		return Boolean.valueOf(res).booleanValue();		}		private static void storeCheckboxState(Button checkBox, String property){		JavaPlugin.getDefault().getDialogSettings().put(property, checkBox.getSelection());		}	
}
