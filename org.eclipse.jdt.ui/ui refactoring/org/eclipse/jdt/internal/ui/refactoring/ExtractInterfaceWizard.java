/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;

public class ExtractInterfaceWizard extends RefactoringWizard {
	
	public ExtractInterfaceWizard(ExtractInterfaceRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_UESR_INTERFACE); 
		setDefaultPageTitle(RefactoringMessages.getString("ExtractInterfaceWizard.Extract_Interface")); //$NON-NLS-1$
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
		private Button fGenerateAnnotationsCheckbox;
		private Button fGenerateCommentsCheckbox;
		private CheckboxTableViewer fTableViewer;
		private static final String DESCRIPTION = RefactoringMessages.getString("ExtractInterfaceInputPage.description"); //$NON-NLS-1$
		private static final String SETTING_PUBLIC= 		"Public";//$NON-NLS-1$
		private static final String SETTING_ABSTRACT= 		"Abstract";//$NON-NLS-1$
		private static final String SETTING_REPLACE= "Replace"; //$NON-NLS-1$
		private static final String SETTING_ANNOTATIONS= "Annotations"; //$NON-NLS-1$
		private static final String SETTING_COMMENTS= "Comments"; //$NON-NLS-1$

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
			addGenerateCommentsCheckbox(result);
			addGenerateAnnotationsCheckbox(result);
			initializeCheckboxes();
			updateUIElementEnablement();
		}

		private void addGenerateAnnotationsCheckbox(Composite result) {
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			String title= RefactoringMessages.getString("ExtractInterfaceWizard.generate.annotations"); //$NON-NLS-1$
			fGenerateAnnotationsCheckbox= createCheckbox(result,  title, false);
			processor.setAnnotations(fGenerateAnnotationsCheckbox.getSelection());
			fGenerateAnnotationsCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					processor.setAnnotations(fGenerateAnnotationsCheckbox.getSelection());
				}
			});		
		}

		private void addGenerateCommentsCheckbox(Composite result) {
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			String title= RefactoringMessages.getString("ExtractInterfaceWizard.generate.comments"); //$NON-NLS-1$
			fGenerateCommentsCheckbox= createCheckbox(result,  title, false);
			processor.setComments(fGenerateCommentsCheckbox.getSelection());
			fGenerateCommentsCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					processor.setComments(fGenerateCommentsCheckbox.getSelection());
				}
			});		
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
				fTableViewer.setInput(getExtractInterfaceRefactoring().getExtractInterfaceProcessor().getExtractableMembers());
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
			final IJavaProject project= getExtractInterfaceRefactoring().getExtractInterfaceProcessor().getType().getJavaProject();
			boolean annotations= project.getOption(JavaCore.COMPILER_COMPLIANCE, true).equals(JavaCore.VERSION_1_5) && project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true).equals(JavaCore.VERSION_1_5);
			boolean anyMethodsChecked= containsMethods(getCheckedMembers());
			fDeclarePublicCheckbox.setEnabled(anyMethodsChecked);
			fDeclareAbstractCheckbox.setEnabled(anyMethodsChecked);
			fGenerateCommentsCheckbox.setEnabled(anyMethodsChecked);
			fGenerateAnnotationsCheckbox.setEnabled(anyMethodsChecked && annotations);
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
				return getExtractInterfaceRefactoring().getExtractInterfaceProcessor().getExtractableMembers().length > 0;
			} catch (JavaModelException e) {
				return false;
			}
		}

		private void addReplaceAllCheckbox(Composite result) {
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			String[] keys= {processor.getType().getElementName()};
			String title= RefactoringMessages.getFormattedString("ExtractInterfaceInputPage.change_references", keys);  //$NON-NLS-1$
			boolean defaultValue= processor.isReplace();
			fReplaceAllCheckbox= createCheckbox(result,  title, defaultValue);
			processor.setReplace(fReplaceAllCheckbox.getSelection());
			fReplaceAllCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					processor.setReplace(fReplaceAllCheckbox.getSelection());
				}
			});		
		}
		
		private void addDeclareAsPublicCheckbox(Composite result) {
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			String[] keys= {"&public"}; //$NON-NLS-1$
			String title= RefactoringMessages.getFormattedString("ExtractInterfaceWizard.12", keys); //$NON-NLS-1$
			boolean defaultValue= processor.getPublic();
			fDeclarePublicCheckbox= createCheckbox(result,  title, defaultValue);
			processor.setPublic(fDeclarePublicCheckbox.getSelection());
			fDeclarePublicCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					processor.setPublic(fDeclarePublicCheckbox.getSelection());
				}
			});		
		}

		private void addDeclareAsAbstractCheckbox(Composite result) {
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			String[] keys= {"&abstract"}; //$NON-NLS-1$
			String title= RefactoringMessages.getFormattedString("ExtractInterfaceWizard.12", keys); //$NON-NLS-1$
			boolean defaultValue= processor.getAbstract();
			fDeclareAbstractCheckbox= createCheckbox(result,  title, defaultValue);
			processor.setAbstract(fDeclareAbstractCheckbox.getSelection());
			fDeclareAbstractCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					processor.setAbstract(fDeclareAbstractCheckbox.getSelection());
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
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			processor.setInterfaceName(text);
			return processor.checkInterfaceName(text);
		}

		private ExtractInterfaceRefactoring getExtractInterfaceRefactoring() {
			return (ExtractInterfaceRefactoring) getRefactoring();
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
			final ExtractInterfaceProcessor processor= getExtractInterfaceRefactoring().getExtractInterfaceProcessor();
			processor.setInterfaceName(getText());
			processor.setReplace(fReplaceAllCheckbox.getSelection());
			processor.setExtractedMembers(getCheckedMembers());
			processor.setAbstract(fDeclareAbstractCheckbox.getSelection());
			processor.setPublic(fDeclarePublicCheckbox.getSelection());
			processor.setAnnotations(fGenerateAnnotationsCheckbox.getSelection());
			processor.setComments(fGenerateCommentsCheckbox.getSelection());
		}
		
		private IMember[] getCheckedMembers() {
			List checked= Arrays.asList(fTableViewer.getCheckedElements());
			return (IMember[]) checked.toArray(new IMember[checked.size()]);
		}

		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
		 */
		public void dispose() {
			fGenerateAnnotationsCheckbox= null;
			fGenerateCommentsCheckbox= null;
			fReplaceAllCheckbox= null;
			fTableViewer= null;
			super.dispose();
		}
		
		private void initializeCheckboxes() {
			initializeCheckBox(fDeclarePublicCheckbox, SETTING_PUBLIC, true);
			initializeCheckBox(fDeclareAbstractCheckbox, SETTING_ABSTRACT, true);	
			initializeCheckBox(fReplaceAllCheckbox, SETTING_REPLACE, true);				
			initializeCheckBox(fGenerateAnnotationsCheckbox, SETTING_ANNOTATIONS, false);				
			initializeCheckBox(fGenerateCommentsCheckbox, SETTING_COMMENTS, true);				
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
			JavaPlugin.getDefault().getDialogSettings().put(SETTING_ANNOTATIONS, fGenerateAnnotationsCheckbox.getSelection());
			JavaPlugin.getDefault().getDialogSettings().put(SETTING_ABSTRACT, fDeclareAbstractCheckbox.getSelection());
			JavaPlugin.getDefault().getDialogSettings().put(SETTING_REPLACE, fReplaceAllCheckbox.getSelection());
			JavaPlugin.getDefault().getDialogSettings().put(SETTING_COMMENTS, fGenerateCommentsCheckbox.getSelection());
		}
	}
}