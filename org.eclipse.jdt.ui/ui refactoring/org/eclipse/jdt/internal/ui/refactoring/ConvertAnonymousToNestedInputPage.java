/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class ConvertAnonymousToNestedInputPage extends UserInputWizardPage {

	private static final String DESCRIPTION = "Select the name and modifiers for the new nested class";
	public static final String PAGE_NAME= "ConvertAnonymousToNestedInputPage";//$NON-NLS-1$
    private Button fDeclareStaticCheckbox;
    private Button fDeclareFinalCheckbox;

	public ConvertAnonymousToNestedInputPage() {
		super(PAGE_NAME, true);
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		result.setLayout(layout);
		
		addVisibilityControl(result);
		addFieldNameField(result);
		addDeclareStaticCheckbox(result);
		addDeclareFinalCheckbox(result);
		
		setPageComplete(false);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.CONVERT_ANONYMOUS_TO_NESTED_WIZARD_PAGE);		
	}

    private void addFieldNameField(Composite result) {
        Label nameLabel= new Label(result, SWT.NONE);
        nameLabel.setText("&Class name:");
        nameLabel.setLayoutData(new GridData());
        
        final Text nameField= new Text(result, SWT.BORDER | SWT.SINGLE);
        nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameField.addModifyListener(new ModifyListener(){
        	public void modifyText(ModifyEvent e) {
        		ConvertAnonymousToNestedInputPage.this.getConvertRefactoring().setClassName(nameField.getText());
        		ConvertAnonymousToNestedInputPage.this.updateStatus();
            }
        });
    }
	
	private void updateStatus() {
    	setPageComplete(getConvertRefactoring().validateInput());
	}
	
	private void addVisibilityControl(Composite result) {
        int[] availableVisibilities= getConvertRefactoring().getAvailableVisibilities();
        int currectVisibility= getConvertRefactoring().getVisibility();
        IVisibilityChangeListener visibilityChangeListener= new IVisibilityChangeListener(){
        	public void visibilityChanged(int newVisibility) {
        		getConvertRefactoring().setVisibility(newVisibility);
            }
        };
        Composite visibilityComposite= VisibilityControlUtil.createVisibilityControl(result, visibilityChangeListener, availableVisibilities, currectVisibility);
        GridData gd= new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan= 2;
        visibilityComposite.setLayoutData(gd);
    }
	
	 public void addDeclareStaticCheckbox(Composite result) {
        GridData gd;
        fDeclareStaticCheckbox= new Button(result, SWT.CHECK);
        fDeclareStaticCheckbox.setEnabled(getConvertRefactoring().canEnableSettingStatic());
        fDeclareStaticCheckbox.setSelection(getConvertRefactoring().getDeclareStatic());
        fDeclareStaticCheckbox.setText("D&eclare class as 'static'");
        gd= new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan= 2;
        fDeclareStaticCheckbox.setLayoutData(gd);
        fDeclareStaticCheckbox.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e) {
        		getConvertRefactoring().setDeclareStatic(fDeclareStaticCheckbox.getSelection());
            }
        });
    }

	 public void addDeclareFinalCheckbox(Composite result) {
        GridData gd;
        fDeclareFinalCheckbox= new Button(result, SWT.CHECK);
        fDeclareFinalCheckbox.setEnabled(getConvertRefactoring().canEnableSettingFinal());
        fDeclareFinalCheckbox.setSelection(getConvertRefactoring().getDeclareFinal());
        fDeclareFinalCheckbox.setText("Decla&re class as 'final'");
        gd= new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan= 2;
        fDeclareFinalCheckbox.setLayoutData(gd);
        fDeclareFinalCheckbox.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e) {
        		getConvertRefactoring().setDeclareFinal(fDeclareFinalCheckbox.getSelection());
            }
        });
    }
	
	private ConvertAnonymousToNestedRefactoring getConvertRefactoring(){
		return (ConvertAnonymousToNestedRefactoring)getRefactoring();
	}
}
