package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.UserInputWizardPage;

class InlineConstantInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "InlineConstantInputPage";//$NON-NLS-1$

	private InlineConstantRefactoring fRefactoring;
	private Group fInlineMode;
	private Button fRemove;

	private final int fOriginalMessageType;
	private final String fOriginalMessage;
	
	public InlineConstantInputPage(String description, int messageType) {
		super(PAGE_NAME, true);
	    fOriginalMessage= description;
	    fOriginalMessageType= messageType;
		setDescription(description);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		fRefactoring= (InlineConstantRefactoring)getRefactoring();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);
		GridData gd= null;

		fInlineMode= new Group(result, SWT.NONE);
		fInlineMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fInlineMode.setLayout(new GridLayout());
		fInlineMode.setText("Inline");
		
		final Button all= new Button(fInlineMode, SWT.RADIO);
		all.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		all.setText("&All references");
		all.setSelection(fRefactoring.getReplaceAllReferences());
		all.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
			    if (! all.getSelection())
			    	return;
				fRemove.setEnabled(true);
				fRefactoring.setReplaceAllReferences(true);
			}
		});

		fRemove= new Button(fInlineMode, SWT.CHECK);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= convertWidthInCharsToPixels(3);
		fRemove.setLayoutData(gd);
		fRemove.setText("&Delete constant declaration");
		fRemove.setEnabled(all.getSelection());
		fRemove.setSelection(fRefactoring.getRemoveDeclaration());
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			    fRefactoring.setRemoveDeclaration(fRemove.getSelection());
			}
		});

		
		final Button onlySelected= new Button(fInlineMode, SWT.RADIO);
		onlySelected.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		onlySelected.setText("&Only the selected reference");
		onlySelected.setSelection(!fRefactoring.getReplaceAllReferences());
		onlySelected.setEnabled(!fRefactoring.isDeclarationSelected());
		onlySelected.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
			    if (! onlySelected.getSelection())
			        return;
				fRemove.setSelection(false);
				fRemove.setEnabled(false);
			    fRefactoring.setRemoveDeclaration(false);
				fRefactoring.setReplaceAllReferences(false);
			}
		});		
	}

    private void updateRemoveDeclarationState() {
        fRefactoring.setRemoveDeclaration(fRemove.getSelection());
    }
	
	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#restoreMessage()
	 */
	protected void restoreMessage() {
		setMessage(fOriginalMessage, fOriginalMessageType);
	}
}
