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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractInterfaceWizard extends RefactoringWizard {
	
	public ExtractInterfaceWizard(ExtractInterfaceRefactoring ref) {
		super(ref, RefactoringMessages.getString("ExtractInterfaceWizard.Extract_Interface")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractInterfaceInputPage());
	}
	
	private static class ExtractInterfaceInputPage extends TextInputWizardPage {

		private Button fReplaceAllCheckbox;
		private Button fDeclarePublicCheckbox;
		private Button fDeclareAbstractCheckbox;
		private CheckboxTableViewer fTableViewer;
		private static final String DESCRIPTION = RefactoringMessages.getString("ExtractInterfaceInputPage.description"); //$NON-NLS-1$
		private static final String SETTING_PUBLIC= 		"Public";//$NON-NLS-1$
		private static final String SETTING_ABSTRACT= 		"Abstract";//$NON-NLS-1$

		public ExtractInterfaceInputPage() {
			super(DESCRIPTION, true);
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
		
			Label label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ExtractInterfaceInputPage.Interface_name")); //$NON-NLS-1$
		
			Text text= createTextInputField(result);
			text.selectAll();
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
			addReplaceAllCheckbox(result);
			addDeclareAsPublicCheckbox(result);
			addDeclareAsAbstractCheckbox(result);

			Label separator= new Label(result, SWT.NONE);
			GridData gd= new GridData();
			gd.horizontalSpan= 2;
			separator.setLayoutData(gd);

			Label tableLabel= new Label(result, SWT.NONE);
			tableLabel.setText(RefactoringMessages.getString("ExtractInterfaceInputPage.Members")); //$NON-NLS-1$
			tableLabel.setEnabled(anyMembersToExtract());
			gd= new GridData();
			gd.horizontalSpan= 2;
			tableLabel.setLayoutData(gd);
		
			Dialog.applyDialogFont(result);
			addMemberListComposite(result);
			initializeCheckboxes();
			updateUIElementEnablement();
		}

		private void addMemberListComposite(Composite result) {
			Composite composite= new Composite(result, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			composite.setLayout(layout);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan= 2;
			composite.setLayoutData(gd);
		
			fTableViewer= CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
			fTableViewer.setLabelProvider(createLabelProvider());
			fTableViewer.setContentProvider(new ArrayContentProvider());
			try {
				fTableViewer.setInput(getExtractInterfaceRefactoring().getExtractableMembers());
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.getString("ExtractInterfaceInputPage.Extract_Interface"), RefactoringMessages.getString("ExtractInterfaceInputPage.Internal_Error")); //$NON-NLS-1$ //$NON-NLS-2$
				fTableViewer.setInput(new IMember[0]);
			}
			fTableViewer.addCheckStateListener(new ICheckStateListener(){
				public void checkStateChanged(CheckStateChangedEvent event) {
					ExtractInterfaceInputPage.this.updateUIElementEnablement();
				}
			}); 
			fTableViewer.getControl().setEnabled(anyMembersToExtract());

			createButtonComposite(composite);
		}

		protected void updateUIElementEnablement() {
			boolean anyMethodsChecked= containsMethods(getCheckedMembers());
			fDeclarePublicCheckbox.setEnabled(anyMethodsChecked);
			fDeclareAbstractCheckbox.setEnabled(anyMethodsChecked);
		}

		private static boolean containsMethods(IMember[] members) {
			for (int i= 0; i < members.length; i++) {
				if (members[i].getElementType() == IJavaElement.METHOD)
					return true;
			}
			return false;
		}

		private ILabelProvider createLabelProvider(){
			AppearanceAwareLabelProvider lprovider= new AppearanceAwareLabelProvider(
				AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE,
				AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
			);
		
			return new DecoratingJavaLabelProvider(lprovider);
		}

		private void createButtonComposite(Composite composite) {
			GridData gd;
			Composite buttonComposite= new Composite(composite, SWT.NONE);
			GridLayout gl= new GridLayout();
			gl.marginHeight= 0;
			gl.marginWidth= 0;
			buttonComposite.setLayout(gl);
			gd= new GridData(GridData.FILL_VERTICAL);
			buttonComposite.setLayoutData(gd);
		
			Button selectAll= new Button(buttonComposite, SWT.PUSH);
			selectAll.setText(RefactoringMessages.getString("ExtractInterfaceInputPage.Select_All")); //$NON-NLS-1$
			selectAll.setEnabled(anyMembersToExtract());
			selectAll.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(selectAll);
			selectAll.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					fTableViewer.setAllChecked(true);
					ExtractInterfaceInputPage.this.updateUIElementEnablement();
				}
			});
		
			Button deSelectAll= new Button(buttonComposite, SWT.PUSH);
			deSelectAll.setText(RefactoringMessages.getString("ExtractInterfaceInputPage.Deselect_All")); //$NON-NLS-1$
			deSelectAll.setEnabled(anyMembersToExtract());
			deSelectAll.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(deSelectAll);
			deSelectAll.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					fTableViewer.setAllChecked(false);
					ExtractInterfaceInputPage.this.updateUIElementEnablement();
				}
			});
		}

		private boolean anyMembersToExtract() {
			try {
				return getExtractInterfaceRefactoring().getExtractableMembers().length > 0;
			} catch (JavaModelException e) {
				return false;
			}
		}

		private void addReplaceAllCheckbox(Composite result) {
			String[] keys= {getExtractInterfaceRefactoring().getInputType().getElementName()};
			String title= RefactoringMessages.getFormattedString("ExtractInterfaceInputPage.change_references", keys);  //$NON-NLS-1$
			boolean defaultValue= getExtractInterfaceRefactoring().isReplaceOccurrences();
			fReplaceAllCheckbox= createCheckbox(result,  title, defaultValue);
			getExtractInterfaceRefactoring().setReplaceOccurrences(fReplaceAllCheckbox.getSelection());
			fReplaceAllCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractInterfaceRefactoring().setReplaceOccurrences(fReplaceAllCheckbox.getSelection());
				}
			});		
		}
		
		private void addDeclareAsPublicCheckbox(Composite result) {
			String[] keys= {"&public"}; //$NON-NLS-1$
			String title= RefactoringMessages.getFormattedString("ExtractInterfaceWizard.12", keys); //$NON-NLS-1$
			boolean defaultValue= getExtractInterfaceRefactoring().getMarkInterfaceMethodsAsPublic();
			fDeclarePublicCheckbox= createCheckbox(result,  title, defaultValue);
			getExtractInterfaceRefactoring().setMarkInterfaceMethodsAsPublic(fDeclarePublicCheckbox.getSelection());
			fDeclarePublicCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractInterfaceRefactoring().setMarkInterfaceMethodsAsPublic(fDeclarePublicCheckbox.getSelection());
				}
			});		
		}

		private void addDeclareAsAbstractCheckbox(Composite result) {
			String[] keys= {"&abstract"}; //$NON-NLS-1$
			String title= RefactoringMessages.getFormattedString("ExtractInterfaceWizard.12", keys); //$NON-NLS-1$
			boolean defaultValue= getExtractInterfaceRefactoring().getMarkInterfaceMethodsAsAbstract();
			fDeclareAbstractCheckbox= createCheckbox(result,  title, defaultValue);
			getExtractInterfaceRefactoring().setMarkInterfaceMethodsAsAbstract(fDeclareAbstractCheckbox.getSelection());
			fDeclareAbstractCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractInterfaceRefactoring().setMarkInterfaceMethodsAsAbstract(fDeclareAbstractCheckbox.getSelection());
				}
			});		
		}
		
		private static Button createCheckbox(Composite parent, String title, boolean value){
			Button checkBox= new Button(parent, SWT.CHECK);
			checkBox.setText(title);
			checkBox.setSelection(value);
			GridData layoutData= new GridData();
			layoutData.horizontalSpan= 2;

			checkBox.setLayoutData(layoutData);
			return checkBox;		
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
		 */
		protected RefactoringStatus validateTextField(String text) {
			getExtractInterfaceRefactoring().setNewInterfaceName(text);
			return getExtractInterfaceRefactoring().checkNewInterfaceName(text);
		}	

		private ExtractInterfaceRefactoring getExtractInterfaceRefactoring(){
			return (ExtractInterfaceRefactoring)getRefactoring();
		}
	
		/*
		 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
		 */
		public IWizardPage getNextPage() {
			try {
				initializeRefactoring();
				storeDialogSettings();
				return super.getNextPage();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return null;
			}
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
		 */
		public boolean performFinish(){
			try {
				initializeRefactoring();
				storeDialogSettings();
				return super.performFinish();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return false;
			}
		}

		private void initializeRefactoring() throws JavaModelException {
			getExtractInterfaceRefactoring().setNewInterfaceName(getText());
			getExtractInterfaceRefactoring().setReplaceOccurrences(fReplaceAllCheckbox.getSelection());
			getExtractInterfaceRefactoring().setExtractedMembers(getCheckedMembers());
			getExtractInterfaceRefactoring().setMarkInterfaceMethodsAsAbstract(fDeclareAbstractCheckbox.getSelection());
			getExtractInterfaceRefactoring().setMarkInterfaceMethodsAsPublic(fDeclarePublicCheckbox.getSelection());
		}
		
		private IMember[] getCheckedMembers() {
			List checked= Arrays.asList(fTableViewer.getCheckedElements());
			return (IMember[]) checked.toArray(new IMember[checked.size()]);
		}

		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
		 */
		public void dispose() {
			fReplaceAllCheckbox= null;
			fTableViewer= null;
			super.dispose();
		}
		
		private void initializeCheckboxes() {
			initializeCheckBox(fDeclarePublicCheckbox, SETTING_PUBLIC, ExtractInterfaceRefactoring.DEFAULT_DECLARE_METHODS_PUBLIC);
			initializeCheckBox(fDeclareAbstractCheckbox, SETTING_ABSTRACT, ExtractInterfaceRefactoring.DEFAULT_DECLARE_METHODS_ABSTRACT);	
		}

		private void initializeCheckBox(Button checkbox, String property, boolean def){
			String s= JavaPlugin.getDefault().getDialogSettings().get(property);
			if (s != null)
				checkbox.setSelection(new Boolean(s).booleanValue());
			else	
				checkbox.setSelection(def);
		}

		private void storeDialogSettings(){
			JavaPlugin.getDefault().getDialogSettings().put(SETTING_PUBLIC, fDeclarePublicCheckbox.getSelection());
			JavaPlugin.getDefault().getDialogSettings().put(SETTING_ABSTRACT, fDeclareAbstractCheckbox.getSelection());
		}

	}
}
