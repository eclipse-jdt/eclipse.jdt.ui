/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.ChangeParametersControl;
import org.eclipse.jdt.internal.ui.refactoring.ParameterChangeListener;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class ExtractMethodInputPage extends TextInputWizardPage {

	private static final String THROW_RUNTIME_EXCEPTIONS= "ThrowRuntimeExceptions"; //$NON-NLS-1$

	private ExtractMethodRefactoring fRefactoring;
	private Label fPreview;
	private IDialogSettings fSettings;
	private static final String MESSAGE = RefactoringMessages.getString("ExtractMethodInputPage.description");//$NON-NLS-1$

	public ExtractMethodInputPage() {
		super(MESSAGE, true);
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
		loadSettings();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		initializeDialogUnits(result);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(getLabelText());
		
		Text text= createTextInputField(result);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		layouter.perform(label, text, 1);
		
		label= new Label(result, SWT.NONE);
		label.setText(RefactoringMessages.getString("ExtractMethodInputPage.access_Modifiers")); //$NON-NLS-1$
		
		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 4; layout.marginWidth= 0;
		group.setLayout(layout);
		
		String[] labels= new String[] {
			RefactoringMessages.getString("ExtractMethodInputPage.public"),  //$NON-NLS-1$
			RefactoringMessages.getString("ExtractMethodInputPage.protected"), //$NON-NLS-1$
			RefactoringMessages.getString("ExtractMethodInputPage.default"), //$NON-NLS-1$
			RefactoringMessages.getString("ExtractMethodInputPage.private") //$NON-NLS-1$
		};
		String[] data= new String[] {"public", "protected", "", "private" }; //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		String visibility= fRefactoring.getVisibility();
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			radio.setText(labels[i]);
			radio.setData(data[i]);
			if (data[i].equals(visibility))
				radio.setSelection(true);
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					setVisibility((String)event.widget.getData());
				}
			});
		}
		layouter.perform(label, group, 1);
		
		ChangeParametersControl cp= new ChangeParametersControl(result, SWT.NULL, "Parameters", new ParameterChangeListener() {
			public void parameterChanged(ParameterInfo parameter) {
				updatePreview(getText());
			}
			public void parameterReordered() {
				updatePreview(getText());
			}
		}, false);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= 2;
		cp.setLayoutData(gd);
		cp.setInput(fRefactoring.getParameterInfos());
		
		Button checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.getString("ExtractMethodInputPage.throwRuntimeExceptions")); //$NON-NLS-1$
		checkBox.setSelection(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setRethrowRuntimeException(((Button)e.widget).getSelection());
			}
		});
		layouter.perform(checkBox);
		
		label= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layouter.perform(label);
		
		label= new Label(result, SWT.NONE);
		gd= new GridData();
		gd.verticalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);
		label.setText(RefactoringMessages.getString("ExtractMethodInputPage.signature_preview")); //$NON-NLS-1$
		
		fPreview= new Label(result, SWT.WRAP);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(50);
		fPreview.setLayoutData(gd);
		
		layouter.perform(label, fPreview, 1);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.EXTRACT_METHOD_WIZARD_PAGE);		
	}	

	private String getLabelText(){
		return RefactoringMessages.getString("ExtractMethodInputPage.label_text"); //$NON-NLS-1$
	}
	
	private void setVisibility(String s) {
		fRefactoring.setVisibility(s);
		updatePreview(getText());
	}
	
	private void setRethrowRuntimeException(boolean value) {
		fSettings.put(THROW_RUNTIME_EXCEPTIONS, value);
		fRefactoring.setThrowRuntimeExceptions(value);
		updatePreview(getText());
	}
	
	private void updatePreview(String text) {
		if (fPreview == null)
			return;
			
		if (text.length() == 0)
			text= "someMethodName";			 //$NON-NLS-1$
			
		fPreview.setText(fRefactoring.getSignature(text));
	}
	
	private void loadSettings() {
		fSettings= getDialogSettings().getSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
			fSettings.put(THROW_RUNTIME_EXCEPTIONS, false);
		}
		fRefactoring.setThrowRuntimeExceptions(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
	}	
}