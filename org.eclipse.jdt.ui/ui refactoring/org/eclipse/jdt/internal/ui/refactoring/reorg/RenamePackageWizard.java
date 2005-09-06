/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.RowLayouter;


public class RenamePackageWizard extends RenameRefactoringWizard {
	
	public RenamePackageWizard(Refactoring refactoring) {
		super(refactoring, 
			RefactoringMessages.RenamePackageWizard_defaultPageTitle, 
			RefactoringMessages.RenamePackageWizard_inputPage_description, 
			JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE,
			IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE);
	}
	
	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenamePackageInputWizardPage(message, IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}	
		};
	}

	private static class RenamePackageInputWizardPage extends RenameInputWizardPage {
		
		private Button fRenameSubpackages;
		private static final String RENAME_SUBPACKAGES= "renameSubpackages"; //$NON-NLS-1$
		
		public RenamePackageInputWizardPage(String message, String contextHelpId, String initialValue) {
			super(message, contextHelpId, true, initialValue);
		}
	
		/* non java-doc
		 * @see DialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			super.createControl(parent);
////if (true) return;
//			Composite parentComposite= (Composite)getControl();
//			
//			Composite composite= new Composite(parentComposite, SWT.NONE);
//			composite.setLayout(new GridLayout());
//			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
//		
//			Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
//			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//	
//			fRenameSubpackages= new Button(composite, SWT.CHECK);
//			fRenameSubpackages.setText("Rename Subpackages");
//			boolean subpackagesEnablement= false;
//			try {
//				subpackagesEnablement= getRenamePackageProcessor().canEnableRenameSubpackages();
//			} catch (JavaModelException e) {
//				JavaPlugin.log(e);
//			}
//			fRenameSubpackages.setEnabled(subpackagesEnablement);
//			
//			boolean subpackagesSelection= subpackagesEnablement && getBooleanSetting(RENAME_SUBPACKAGES, getRenamePackageProcessor().getRenameSubpackages());
//			fRenameSubpackages.setSelection(subpackagesSelection);
//			getRenamePackageProcessor().setRenameSubpackages(subpackagesSelection);
//			fRenameSubpackages.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			fRenameSubpackages.addSelectionListener(new SelectionAdapter(){
//				public void widgetSelected(SelectionEvent e) {
//					getRenamePackageProcessor().setRenameSubpackages(fRenameSubpackages.getSelection());
//				}
//			});
//			
//			Dialog.applyDialogFont(composite);
		}
		
		protected void hook2(Composite composite, RowLayouter layouter) {
			fRenameSubpackages= new Button(composite, SWT.CHECK);
			fRenameSubpackages.setText(RefactoringMessages.RenamePackageWizard_rename_subpackages);
			boolean subpackagesEnablement= false;
			try {
				subpackagesEnablement= getRenamePackageProcessor().canEnableRenameSubpackages();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			fRenameSubpackages.setEnabled(subpackagesEnablement);
			
			boolean subpackagesSelection= subpackagesEnablement && getBooleanSetting(RENAME_SUBPACKAGES, getRenamePackageProcessor().getRenameSubpackages());
			fRenameSubpackages.setSelection(subpackagesSelection);
			getRenamePackageProcessor().setRenameSubpackages(subpackagesSelection);
			fRenameSubpackages.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fRenameSubpackages.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getRenamePackageProcessor().setRenameSubpackages(fRenameSubpackages.getSelection());
				}
			});
			layouter.perform(fRenameSubpackages);
			
			Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			layouter.perform(separator);
		}
		
//		protected void addOptionalUpdateReferencesCheckbox(Composite parent, RowLayouter layouter) {
//			Composite composite= new Composite(parent, SWT.NONE);
//			GridData gd= new GridData(GridData.FILL_HORIZONTAL);
//			gd.horizontalSpan= 2;
//			composite.setLayoutData(gd);
//			
//			GridLayout layout= new GridLayout();
//			layout.numColumns= 2;
//			layout.verticalSpacing= 8;
//			layout.horizontalSpacing= layout.marginWidth = layout.marginHeight= 0;
//			composite.setLayout(layout);
//			
//			final IReferenceUpdating ref= (IReferenceUpdating)getRefactoring().getAdapter(IReferenceUpdating.class);
//			String title= RefactoringMessages.RenameInputWizardPage_update_references; 
//			boolean defaultValue= true; //bug 77901
//			fUpdateReferences= new Button(composite, SWT.CHECK);
//			fUpdateReferences.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			fUpdateReferences.setText(title);
//			fUpdateReferences.setSelection(defaultValue);
//			
//			ref.setUpdateReferences(fUpdateReferences.getSelection());
//			fUpdateReferences.addSelectionListener(new SelectionAdapter(){
//				public void widgetSelected(SelectionEvent e) {
//					ref.setUpdateReferences(fUpdateReferences.getSelection());
//				}
//			});		
//			
//			fRenameSubpackages= new Button(composite, SWT.CHECK);
//			fRenameSubpackages.setText("Rename Subpackages");
//			boolean subpackagesEnablement= false;
//			try {
//				subpackagesEnablement= getRenamePackageProcessor().canEnableRenameSubpackages();
//			} catch (JavaModelException e) {
//				JavaPlugin.log(e);
//			}
//			fRenameSubpackages.setEnabled(subpackagesEnablement);
//			
//			boolean subpackagesSelection= subpackagesEnablement && getBooleanSetting(RENAME_SUBPACKAGES, getRenamePackageProcessor().getRenameSubpackages());
//			fRenameSubpackages.setSelection(subpackagesSelection);
//			getRenamePackageProcessor().setRenameSubpackages(subpackagesSelection);
//			fRenameSubpackages.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			fRenameSubpackages.addSelectionListener(new SelectionAdapter(){
//				public void widgetSelected(SelectionEvent e) {
//					getRenamePackageProcessor().setRenameSubpackages(fRenameSubpackages.getSelection());
//				}
//			});
//		}
		
		public void dispose() {
		if (false)
			if (saveSettings() && fRenameSubpackages.isEnabled())
				saveBooleanSetting(RENAME_SUBPACKAGES, fRenameSubpackages);
			super.dispose();
		}
		
		private RenamePackageProcessor getRenamePackageProcessor() {
			return (RenamePackageProcessor) ((RenameRefactoring) getRefactoring()).getProcessor();
		}
	}
}
