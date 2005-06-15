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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.structure.UseSuperTypeRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;

public class UseSupertypeWizard extends RefactoringWizard{

	/* package */ static final String DIALOG_SETTING_SECTION= "UseSupertypeWizard"; //$NON-NLS-1$
	
	public UseSupertypeWizard(UseSuperTypeRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.UseSupertypeWizard_Use_Super_Type_Where_Possible); 
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new UseSupertypeInputPage());
	}
	
	private static class UseSupertypeInputPage extends UserInputWizardPage{

		private static final String REWRITE_INSTANCEOF= "rewriteInstanceOf";  //$NON-NLS-1$
		public static final String PAGE_NAME= "UseSupertypeInputPage";//$NON-NLS-1$
		private TableViewer fTableViewer; 
		private final Map fFileCount;  //IType -> Integer
		private final static String MESSAGE= RefactoringMessages.UseSupertypeInputPage_Select_supertype; 
		private JavaElementLabelProvider fTableLabelProvider;
		private IDialogSettings fSettings;
		
		public UseSupertypeInputPage() {
			super(PAGE_NAME);
			fFileCount= new HashMap(2);
			setMessage(MESSAGE);
		}

		private void loadSettings() {
			fSettings= getDialogSettings().getSection(UseSupertypeWizard.DIALOG_SETTING_SECTION);
			if (fSettings == null) {
				fSettings= getDialogSettings().addNewSection(UseSupertypeWizard.DIALOG_SETTING_SECTION);
				fSettings.put(REWRITE_INSTANCEOF, false);
			}
			((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().setInstanceOf(fSettings.getBoolean(REWRITE_INSTANCEOF));
		}	

		public void createControl(Composite parent) {
			loadSettings();
			Composite composite= new Composite(parent, SWT.NONE);
			setControl(composite);
			composite.setLayout(new GridLayout());

			Label label= new Label(composite, SWT.NONE);
			label.setText(Messages.format(
					RefactoringMessages.UseSupertypeInputPage_Select_supertype_to_use, //$NON-NLS-1$
					JavaElementLabels.getElementLabel(((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().getSubType(), JavaElementLabels.T_FULLY_QUALIFIED)));
			label.setLayoutData(new GridData());
		
			addTableComponent(composite);

			final Button checkbox= new Button(composite, SWT.CHECK);
			checkbox.setText(RefactoringMessages.UseSupertypeInputPage_Use_in_instanceof); 
			checkbox.setLayoutData(new GridData());
			checkbox.setSelection(((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().isInstanceOf());
			checkbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().setInstanceOf(checkbox.getSelection());
					fSettings.put(REWRITE_INSTANCEOF, checkbox.getSelection());
					setMessage(MESSAGE);
					setPageComplete(true);
					fFileCount.clear();
					fTableViewer.refresh();
				}
			});

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
						setMessage(RefactoringMessages.UseSupertypeInputPage_No_updates, IMessageProvider.INFORMATION); 
						setPageComplete(false);
					} else {
						setMessage(MESSAGE);
						setPageComplete(true);
					}
					fTableViewer.refresh();
				}
			});
			fTableViewer.setInput(((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().getSuperTypes());
			fTableViewer.getTable().setSelection(0);
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
			final int count= ((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().getChanges();
			fFileCount.put(selectedType, new Integer(count));
			if (count == 0) {
				setMessage(RefactoringMessages.UseSupertypeInputPage_No_updates, IMessageProvider.INFORMATION); 
				setPageComplete(false);
			}
			fTableViewer.refresh();
			if (noSupertypeCanBeUsed()){
				setMessage(RefactoringMessages.UseSupertypeWizard_10, IMessageProvider.INFORMATION); 
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
			final int count= ((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().getChanges();
			if (count == 0) {
				updateUpdateLabels();
				return false;
			}
			return superFinish;
		}

		private void initializeRefactoring() {
			StructuredSelection ss= (StructuredSelection)fTableViewer.getSelection();
			((UseSuperTypeRefactoring)getRefactoring()).getUseSuperTypeProcessor().setSuperType((IType)ss.getFirstElement());
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
					return Messages.format(RefactoringMessages.UseSupertypeInputPage_no_possible_updates, keys); 
				} else if (count == 1){
					String [] keys= {superText};
					return Messages.format(RefactoringMessages.UseSupertypeInputPage_updates_possible_in_file, keys); 
				}	else {
					String[] keys= {superText, String.valueOf(count)};
					return Messages.format(RefactoringMessages.UseSupertypeInputPage_updates_possible_in_files, keys); 
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
