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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.IType;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.structure.UseSupertypeWherePossibleRefactoring;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class UseSupertypeWizard extends RefactoringWizard{

	public UseSupertypeWizard(UseSupertypeWherePossibleRefactoring ref) {
		super(ref, RefactoringMessages.getString("UseSupertypeWizard.Use_Super_Type_Where_Possible")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new UseSupertypeInputPage());
	}
	
	private static class UseSupertypeInputPage extends UserInputWizardPage{

		public static final String PAGE_NAME= "UseSupertypeInputPage";//$NON-NLS-1$
		private TableViewer fTableViewer; 
		private final Map fFileCount;  //IType -> Integer
		private final static String MESSAGE= RefactoringMessages.getString("UseSupertypeInputPage.Select_supertype"); //$NON-NLS-1$
		private JavaElementLabelProvider fTableLabelProvider;

		public UseSupertypeInputPage() {
			super(PAGE_NAME, true);
			fFileCount= new HashMap(2);
			setMessage(MESSAGE);
		}

		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			setControl(composite);
			composite.setLayout(new GridLayout());
			
			//XXX 39862 use supertype: cannot control whether instanceof references are updated 

//			final Button checkbox= new Button(composite, SWT.CHECK);
//			checkbox.setText(RefactoringMessages.getString("UseSupertypeInputPage.Use_in_instanceof")); //$NON-NLS-1$
//			checkbox.setLayoutData(new GridData());
//			checkbox.setSelection(getUseSupertypeRefactoring().getUseSupertypeInInstanceOf());
//			checkbox.addSelectionListener(new SelectionAdapter(){
//				public void widgetSelected(SelectionEvent e) {
//					getUseSupertypeRefactoring().setUseSupertypeInInstanceOf(checkbox.getSelection());
//					setMessage(MESSAGE);
//					setPageComplete(true);
//					fFileCount.clear();
//					fTableViewer.refresh();
//				}
//			});
		
			Label label= new Label(composite, SWT.NONE);
			label.setText(RefactoringMessages.getString("UseSupertypeInputPage.Select_supertype_to_use")); //$NON-NLS-1$
			label.setLayoutData(new GridData());
		
			addTableComponent(composite);
			Dialog.applyDialogFont(composite);
		}

		private void addTableComponent(Composite composite) {
			fTableViewer= new TableViewer(composite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
			fTableLabelProvider= new UseSupertypeLabelProvider(fFileCount);
			fTableViewer.setLabelProvider(fTableLabelProvider);
			fTableViewer.setContentProvider(new ArrayContentProvider());
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection ss= (IStructuredSelection)event.getSelection();
					if (new Integer(0).equals(fFileCount.get(ss.getFirstElement()))){
						setMessage(RefactoringMessages.getString("UseSupertypeInputPage.No_updates"), IMessageProvider.INFORMATION); //$NON-NLS-1$
						setPageComplete(false);
					} else {
						setMessage(MESSAGE);
						setPageComplete(true);
					}
					fTableViewer.refresh();
				}
			});
			fTableViewer.setInput(getUseSupertypeRefactoring().getSuperTypes());
			fTableViewer.getTable().setSelection(0);
		}
	
		private UseSupertypeWherePossibleRefactoring getUseSupertypeRefactoring(){
			return (UseSupertypeWherePossibleRefactoring)getRefactoring();
		}
		/*
		 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
		 */
		public IWizardPage getNextPage() {
			initializeRefactoring();
			IWizardPage nextPage= super.getNextPage();
			updateUpdateLabels();
			return nextPage;
		}

		private void updateUpdateLabels() {
			IType selectedType= getSelectedSupertype();
			Change change= getRefactoringWizard().getChange();
			if (change instanceof CompositeChange){
				fFileCount.put(selectedType, new Integer(((CompositeChange)change).getChildren().length));
			}
			fTableViewer.refresh();
			if (noSupertypeCanBeUsed()){
				setMessage(RefactoringMessages.getString("UseSupertypeWizard.10"), IMessageProvider.INFORMATION); //$NON-NLS-1$
				setPageComplete(false);	
			}
		}

		private boolean noSupertypeCanBeUsed() {
			return fTableViewer.getTable().getItemCount() == countFilesWithValue(0);
		}

		private int countFilesWithValue(int i) {
			int count= 0;
			for (Iterator iter= fFileCount.keySet().iterator(); iter.hasNext();) {
				if (((Integer)fFileCount.get(iter.next())).intValue() == i)
					count++;
			}
			return count;
		}

		private IType getSelectedSupertype() {
			IStructuredSelection ss= (IStructuredSelection)fTableViewer.getSelection();
			return (IType)ss.getFirstElement();
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
		 */
		public boolean performFinish(){
			initializeRefactoring();
			boolean superFinish= super.performFinish();
			if (! superFinish)
				return false;
			Change c= getRefactoringWizard().getChange();
			if (c instanceof CompositeChange && ((CompositeChange)c).getChildren().length == 0) {
				updateUpdateLabels();
				return false;
			}
			return superFinish;
		}

		private void initializeRefactoring() {
			StructuredSelection ss= (StructuredSelection)fTableViewer.getSelection();
			getUseSupertypeRefactoring().setSuperTypeToUse((IType)ss.getFirstElement());
		}
	
		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
		 */
		public void dispose() {
			fTableViewer= null;
			fFileCount.clear();
			fTableLabelProvider= null;
			super.dispose();
		}
	
		private static class UseSupertypeLabelProvider extends JavaElementLabelProvider{
			private final Map fFileCount;
			private UseSupertypeLabelProvider(Map fileCount){
				fFileCount= fileCount;
			}
			public String getText(Object element) {
				String superText= super.getText(element);
				if  (! fFileCount.containsKey(element))
					return superText;
				int count= ((Integer)fFileCount.get(element)).intValue();
				if (count == 0){
					String[] keys= {superText};
					return RefactoringMessages.getFormattedString("UseSupertypeInputPage.no_possible_updates", keys); //$NON-NLS-1$
				} else if (count == 1){
					String [] keys= {superText};
					return RefactoringMessages.getFormattedString("UseSupertypeInputPage.updates_possible_in_file", keys); //$NON-NLS-1$
				}	else {
					String[] keys= {superText, String.valueOf(count)};
					return RefactoringMessages.getFormattedString("UseSupertypeInputPage.updates_possible_in_files", keys); //$NON-NLS-1$
				}	
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible && fTableViewer != null)
				fTableViewer.getTable().setFocus();
		}

	}
}
