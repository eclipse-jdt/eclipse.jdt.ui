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

public class InlineConstantInputPage extends UserInputWizardPage {

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
		
		Button radio= new Button(fInlineMode, SWT.RADIO);
		radio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radio.setText("All references");
		radio.setSelection(fRefactoring.getReplaceAllReferences());
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRemove.setEnabled(true);
				if (((Button)event.widget).getSelection())
					fRefactoring.setReplaceAllReferences(true);
			}
		});

		fRemove= new Button(fInlineMode, SWT.CHECK);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= convertWidthInCharsToPixels(3);
		fRemove.setLayoutData(gd);
		fRemove.setText("Delete constant declaration");
		fRemove.setEnabled(radio.getSelection());
		fRemove.setSelection(fRefactoring.getRemoveDeclaration());
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setRemoveDeclaration(((Button)e.widget).getSelection());
			}
		});

		
		radio= new Button(fInlineMode, SWT.RADIO);
		radio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radio.setText("Only the selected reference");
		radio.setSelection(!fRefactoring.getReplaceAllReferences());
		radio.setEnabled(!fRefactoring.isDeclarationSelected());
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRemove.setSelection(false);
				fRemove.setEnabled(false);
				if (((Button)event.widget).getSelection())
					fRefactoring.setReplaceAllReferences(true);
			}
		});		
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#restoreMessage()
	 */
	protected void restoreMessage() {
		setMessage(fOriginalMessage, fOriginalMessageType);
	}
}
