package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class RenameFieldInputWizardPage extends RenameInputWizardPage {

	private Button fRenameGetter;
	private Button fRenameSetter;
	
	public RenameFieldInputWizardPage(String contextHelpId, String initialValue) {
		super(contextHelpId, true, initialValue);
	}

	/* non java-doc
	 * @see DialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite parentComposite= (Composite)getControl();
				
		Composite composite= new Composite(parentComposite, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fRenameGetter= new Button(composite, SWT.CHECK);
		fRenameGetter.setEnabled(isGetterRenamingEnabled());
		fRenameGetter.setSelection(getRenameFieldRefactoring().getRenameGetter());
		fRenameGetter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRenameGetter.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getRenameFieldRefactoring().setRenameGetter(fRenameGetter.getSelection());
			}
		});
		
		fRenameSetter= new Button(composite, SWT.CHECK);
		fRenameSetter.setEnabled(isSetterRenamingEnabled());
		fRenameSetter.setSelection(getRenameFieldRefactoring().getRenameSetter());
		fRenameSetter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRenameSetter.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getRenameFieldRefactoring().setRenameSetter(fRenameSetter.getSelection());
			}
		});
		
		updateGetterSetterLabels();
	}
	
	protected void updateGetterSetterLabels(){
		fRenameGetter.setText(getRenameGetterLabel());
		fRenameSetter.setText(getRenameSetterLabel());
	}
	
	private String getRenameGetterLabel(){
		String defaultLabel= RefactoringMessages.getString("RenameFiledInputWizardPage.rename_getter"); //$NON-NLS-1$
		if (! isGetterRenamingEnabled())
			return defaultLabel;
		try {
			IMethod	getter= getRenameFieldRefactoring().getGetter();
			if (! getter.exists())
				return defaultLabel;
			String getterSig= 	JavaElementUtil.createMethodSignature(getter);
			return RefactoringMessages.getFormattedString("RenameFiledInputWizardPage.rename_getter_to", new String[]{getterSig, createNewGetterName()}); //$NON-NLS-1$
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return defaultLabel;			
		}
	}
	
	private String createNewGetterName() throws JavaModelException {
		return getRenameFieldRefactoring().getNewGetterName();
	}
	
	private String getRenameSetterLabel(){
		String defaultLabel= RefactoringMessages.getString("RenameFiledInputWizardPage.rename_setter"); //$NON-NLS-1$
		if (! isSetterRenamingEnabled())
			return defaultLabel;
		try {
			IMethod	setter= getRenameFieldRefactoring().getSetter();
			if (! setter.exists())
				return defaultLabel;
			String setterSig= 	JavaElementUtil.createMethodSignature(setter);
			return RefactoringMessages.getFormattedString("RenameFiledInputWizardPage.rename_setter_to", new String[]{setterSig, createNewSetterName()});//$NON-NLS-1$
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return defaultLabel;			
		}
	}
	
	private String createNewSetterName(){
		return getRenameFieldRefactoring().getNewSetterName();
	}
	
	private boolean isGetterRenamingEnabled(){
		try {
			return getRenameFieldRefactoring().canEnableGetterRenaming();
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}
	
	private boolean isSetterRenamingEnabled(){
		try {
			return getRenameFieldRefactoring().canEnableSetterRenaming();
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}
	
	private RenameFieldRefactoring getRenameFieldRefactoring(){
		return (RenameFieldRefactoring)getRefactoring();
	}
}
