/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

class ChangeSignatureInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ChangeSignatureInputPage"; //$NON-NLS-1$
	private Label fSignaturePreview;
	
	public ChangeSignatureInputPage() {
		super(PAGE_NAME, true);
		setMessage(RefactoringMessages.getString("ChangeSignatureInputPage.new_order")); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout((new GridLayout()));
		
        try {
            int[] availableVisibilities= getChangeMethodSignatureRefactoring().getAvailableVisibilities();
            int currectVisibility= getChangeMethodSignatureRefactoring().getVisibility();
            IVisibilityChangeListener visibilityChangeListener= new IVisibilityChangeListener(){
            	public void visibilityChanged(int newVisibility) {
            		getChangeMethodSignatureRefactoring().setVisibility(newVisibility);
            		update(true);
                }
            };
            Composite visibilityComposite= VisibilityControlUtil.createVisibilityControl(composite, visibilityChangeListener, availableVisibilities, currectVisibility);
            if (visibilityComposite != null)
	       		visibilityComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            if ( getChangeMethodSignatureRefactoring().canChangeReturnType())
            	createReturnTypeControl(composite);
            createParameterTableComposite(composite);
            
            Label label= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
            label.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
            
            fSignaturePreview= new Label(composite, SWT.WRAP);
            GridData gl= new GridData(GridData.FILL_BOTH);
            gl.widthHint= convertWidthInCharsToPixels(50);
            fSignaturePreview.setLayoutData(gl);
            update(false);
            
            setControl(composite);
        } catch (JavaModelException e) {
        	ExceptionHandler.handle(e, RefactoringMessages.getString("ChangeSignatureInputPage.Change_Signature"), RefactoringMessages.getString("ChangeSignatureInputPage.Internal_Error")); //$NON-NLS-1$ //$NON-NLS-2$
        }
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MODIFY_PARAMETERS_WIZARD_PAGE);
	}

	private void createReturnTypeControl(Composite parent) throws JavaModelException {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
			GridLayout layout= new GridLayout();
			layout.numColumns= 2; layout.marginWidth= 0;
			composite.setLayout(layout);
			
			Label label= new Label(composite, SWT.NONE);
			label.setText(RefactoringMessages.getString("ChangeSignatureInputPage.return_type")); //$NON-NLS-1$
			label.setLayoutData((new GridData()));
			
			final Text text= new Text(composite, SWT.BORDER);
			text.setText(getChangeMethodSignatureRefactoring().getReturnTypeString());
			text.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
			
			text.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					getChangeMethodSignatureRefactoring().setNewReturnTypeName(text.getText());
					update(true);
				}
			});
	}
	
	private void createParameterTableComposite(Composite composite) {
		String labelText= RefactoringMessages.getString("ChangeSignatureInputPage.parameters"); //$NON-NLS-1$
		ChangeParametersControl cp= new ChangeParametersControl(composite, SWT.NONE, labelText, new IParameterListChangeListener() {
			public void parameterChanged(ParameterInfo parameter) {
				update(true);
			}
			public void parameterListChanged() {
				update(true);
			}
			public void parameterAdded(ParameterInfo parameter) {
				getChangeMethodSignatureRefactoring().setupNewParameterInfo(parameter);
				update(true);
			}
		}, true, true, true);
		cp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		cp.setInput(getChangeMethodSignatureRefactoring().getParameterInfos());
	}
	
	private ChangeSignatureRefactoring getChangeMethodSignatureRefactoring(){
		return	(ChangeSignatureRefactoring)getRefactoring();
	}

	private void update(boolean displayErrorMessage){
		updateStatus(displayErrorMessage);
		updateSignaturePreview();
	}

	private void updateStatus(boolean displayErrorMessage) {
		try{
			if (getChangeMethodSignatureRefactoring().isSignatureSameAsInitial()){
			    setErrorMessage(null);
			    setPageComplete(false);
			    return;
			}
		    RefactoringStatus nameCheck= getChangeMethodSignatureRefactoring().checkSignature();
			if (nameCheck.hasFatalError()){
				if (displayErrorMessage)
					setErrorMessage(nameCheck.getFirstMessage(RefactoringStatus.FATAL));
				setPageComplete(false);
			} else {
				setErrorMessage(null);	
				setPageComplete(true);
			}	
		} catch (JavaModelException e){
			setErrorMessage(RefactoringMessages.getString("ChangeSignatureInputPage.Internal_Error")); //$NON-NLS-1$
			setPageComplete(false);
			JavaPlugin.log(e);
		};
	}

	private void updateSignaturePreview() {
		try{
			fSignaturePreview.setText(RefactoringMessages.getString("ChangeSignatureInputPage.method_Signature_Preview") + getChangeMethodSignatureRefactoring().getMethodSignaturePreview()); //$NON-NLS-1$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("ChangeSignatureRefactoring.modify_Parameters"), RefactoringMessages.getString("ChangeSignatureInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
		}	
	}	
}	