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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.UserInputWizardPage;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;


/**
 * @author tip
 */
public class ChangeTypeWizard extends RefactoringWizard {


	public ChangeTypeWizard(ChangeTypeRefactoring ref) {
		super(ref, RefactoringMessages.getString("ChangeTypeWizard.title")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ChangeTypeInputPage());
	}
	
	// For debugging
	static String print(Collection/*<IType>*/ types){
		if (types.isEmpty())
			return "{ }"; //$NON-NLS-1$
		String result = "{ "; //$NON-NLS-1$
		for (Iterator it=types.iterator(); it.hasNext(); ){
			IType type= (IType)it.next();
			result += type.getFullyQualifiedName();
			if (it.hasNext()){
				result += ", ";  //$NON-NLS-1$
			} else {
				result += " }"; //$NON-NLS-1$
			}
		}
		return result;
	}
	
	
	/**
	 * A JavaElementLabelProvider that supports graying out of invalid types.
	 */
	private class ChangeTypeLabelProvider extends JavaElementLabelProvider 
										  implements IColorProvider {
		
		public ChangeTypeLabelProvider(){
			fGrayColor= Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		}
		
		private Collection/*<IType>*/ fInvalidTypes;
		
		public void grayOut(Collection/*<IType>*/ invalidTypes){
			fInvalidTypes= invalidTypes; 
			fireLabelProviderChanged(new LabelProviderChangedEvent(this, fInvalidTypes.toArray()));
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {
			if (fInvalidTypes == null){ // initially, everything is enabled
				return null;
			} else {
				if (fInvalidTypes.contains(element))
					return fGrayColor;
				else
					return null;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		public Color getBackground(Object element) {
			return fCurrentBackgroundColor;
		}
		
		private Color fGrayColor;
		private Color fCurrentBackgroundColor;
	}
	
	private class ChangeTypeInputPage extends UserInputWizardPage{

		public static final String PAGE_NAME= "ChangeTypeInputPage";//$NON-NLS-1$
		private final  String MESSAGE= RefactoringMessages.getString("ChangeTypeInputPage.Select_Type"); //$NON-NLS-1$
		private ChangeTypeLabelProvider fLabelProvider;
		private TreeViewer fTreeViewer;

		public ChangeTypeInputPage() {
			super(PAGE_NAME, true);
			setMessage(MESSAGE);
		}
		
		private class ValidTypesTask implements Runnable {
			private Collection/*<IType>*/ fInvalidTypes;
			private Collection/*<IType>*/ fValidTypes;
			public void run() {
				IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) {
						pm.beginTask(RefactoringMessages.getString("ChangeTypeWizard.analyzing"), 1000); //$NON-NLS-1$
						ChangeTypeRefactoring ct= (ChangeTypeRefactoring)ChangeTypeWizard.this.getRefactoring();
						fInvalidTypes = new HashSet();
						fInvalidTypes.addAll(Arrays.asList(ct.getTypeHierarchy().getAllSupertypes(ct.getOriginalType())));
						fValidTypes= ct.computeValidTypes(new SubProgressMonitor(pm, 950));
						fInvalidTypes.add(ct.getOriginalType());
						fInvalidTypes.removeAll(fValidTypes);
						pm.worked(50);
						pm.done();
					}
				};
				try {
					getWizard().getContainer().run(true, true, runnable);
				} catch (InvocationTargetException e) {
					ChangeTypeInputPage.this.setErrorMessage("Internal error during computation of valid types"); //$NON-NLS-1$
				} catch (InterruptedException e) {
					ChangeTypeInputPage.this.setMessage("Computation of valid types was interrupted"); //$NON-NLS-1$
	
				}
													
// BUG: The following call does not gray out all appropriate nodes if there
// are multiple nodes in the tree that correspond to the same IType that needs
// to be grayed out
//				fLabelProvider.grayOut(fInvalidTypes);
				
				// WORKAROUND: traverse the tree explicitly
				Tree tree= fTreeViewer.getTree();
				fTreeViewer.expandToLevel(10);
				grayOutInvalidTypes(tree.getItems());
				
				if (fValidTypes.size() == 0){
					ChangeTypeInputPage.this.setErrorMessage(RefactoringMessages.getString("ChangeTypeWizard.declCannotBeChanged")); //$NON-NLS-1$
				} else {
					ChangeTypeInputPage.this.setMessage(RefactoringMessages.getString("ChangeTypeWizard.pleaseChooseType")); //$NON-NLS-1$
					TreeItem selection= getInitialSelection(fValidTypes);
					fTreeViewer.getTree().setSelection(new TreeItem[]{ selection });
					setPageComplete(true);
				}
			}			
		}
		
		private TreeItem getInitialSelection(Collection/*<IType>*/ types) {
			
			// first, find a most general valid type (there may be more than one)
			IType type= (IType)types.iterator().next();
			for (Iterator it= types.iterator(); it.hasNext(); ){
				IType other= (IType)it.next();
				if (getGeneralizeTypeRefactoring().isSubTypeOf(type, other)){
					type= other;
				}
			}
			
			// now find a corresponding TreeItem (there may be more than one)		
			return findItem(fTreeViewer.getTree().getItems(), type);
		}
		
		private TreeItem findItem(TreeItem[] items, IType type){
			for (int i=0; i < items.length; i++){
				if (items[i].getData().equals(type)) return items[i];
			}
			for (int i=0; i < items.length; i++){
				TreeItem item= findItem(items[i].getItems(), type);
				if (item != null) return item;
			}
			return null;
		}
		
		
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			setControl(composite);
			composite.setLayout(new GridLayout());
			composite.setLayoutData(new GridData());
			addTreeComponent(composite);			
			Dialog.applyDialogFont(composite);
		}
		
		/**
		 * Tree-viewer that shows the allowable types in a tree view.
		 */
		private void addTreeComponent(Composite parent) {
			fTreeViewer= new TreeViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.grabExcessHorizontalSpace= true;
			gd.grabExcessVerticalSpace= true;
			GC gc= new GC(parent); gc.setFont(gc.getFont()); 
			gd.heightHint= Dialog.convertHeightInCharsToPixels(gc.getFontMetrics(), 6); // 6 characters tall
			fTreeViewer.getTree().setLayoutData(gd);
			
			fTreeViewer.setContentProvider(new ChangeTypeContentProvider(((ChangeTypeRefactoring)getRefactoring())));
			fLabelProvider= new ChangeTypeLabelProvider(); 
			fTreeViewer.setLabelProvider(fLabelProvider);
			ISelectionChangedListener listener= new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection= (IStructuredSelection)event.getSelection();
					IType type= (IType)selection.getFirstElement();
					ChangeTypeInputPage.this.setPageComplete(getGeneralizeTypeRefactoring().getValidTypes().contains(type));
				}
			};
			fTreeViewer.addSelectionChangedListener(listener);
			fTreeViewer.setInput(new ChangeTypeContentProvider.RootType(getGeneralizeTypeRefactoring().getOriginalType()));
			fTreeViewer.expandToLevel(10);
		}

		// NEEDED for WORKAROUND
		private void grayOutInvalidTypes(TreeItem[] items) {
			for (int i=0; i < items.length; i++){
				TreeItem item= items[i];
				String itemName= ((IType)item.getData()).getFullyQualifiedName();
				ChangeTypeRefactoring ct= (ChangeTypeRefactoring) getRefactoring();
				Collection validTypeNames= ct.getValidTypeNames();
				if (!validTypeNames.contains(itemName)){
					//item.setGrayed(true); --- useless here; only works for CheckboxTrees
					item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				}
				grayOutInvalidTypes(items[i].getItems());
			}
		}
		
		private ChangeTypeRefactoring getGeneralizeTypeRefactoring(){
			return (ChangeTypeRefactoring)getRefactoring();
		}
		/*
		 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
		 */
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private IType getSelectedType() {
			IStructuredSelection ss= (IStructuredSelection)fTreeViewer.getSelection();
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
			IChange c= getRefactoringWizard().getChange();
			if (c instanceof CompositeChange && ((CompositeChange)c).getChildren().length == 0) {
				return false;
			}
			return superFinish;
		}

		private void initializeRefactoring() {
			getGeneralizeTypeRefactoring().setSelectedType(getSelectedType());
		}
	
		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
		 */
		public void dispose() {
			fTreeViewer= null;
			super.dispose();
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible && fTreeViewer != null)
				fTreeViewer.getTree().setFocus();
				if (!fTreeUpdated){
					fTreeViewer.getTree().getDisplay().asyncExec(new ValidTypesTask());
					fTreeUpdated= true;
				}
		}

		private boolean fTreeUpdated= false;
	}

	
	
}
