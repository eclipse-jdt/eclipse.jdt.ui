/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;


/**
 * @author tip
 */
public class ChangeTypeWizard extends RefactoringWizard {

	private ChangeTypeRefactoring fCT;

	public ChangeTypeWizard(ChangeTypeRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.ChangeTypeWizard_title);
		fCT= ref;
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	@Override
	protected void addUserInputPages(){
		addPage(new ChangeTypeInputPage());
	}

	// For debugging
	static String print(Collection<ITypeBinding> types){
		if (types.isEmpty())
			return "{ }"; //$NON-NLS-1$
		StringBuilder result = new StringBuilder("{ "); //$NON-NLS-1$
		for (Iterator<ITypeBinding> it=types.iterator(); it.hasNext(); ){
			ITypeBinding type= it.next();
			result.append(type.getQualifiedName());
			if (it.hasNext()){
				result.append(", ");  //$NON-NLS-1$
			} else {
				result.append(" }"); //$NON-NLS-1$
			}
		}
		return result.toString();
	}


	/**
	 * A JavaElementLabelProvider that supports graying out of invalid types.
	 */
	private static class ChangeTypeLabelProvider extends BindingLabelProvider
										  implements IColorProvider {

		private Color fGrayColor;
		/** color -> gray */
		private HashMap<Image, Image> fGrayImages;

		public ChangeTypeLabelProvider(){
			fGrayColor= Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			fGrayImages= new HashMap<>();
		}

		private Collection<ITypeBinding> fInvalidTypes;

		public void grayOut(Collection<ITypeBinding> invalidTypes){
			fInvalidTypes= invalidTypes;
			/*
			 * Invalidate all labels. Invalidating only invalid types doesn't
			 * work since there can be multiple nodes in the tree that
			 * correspond to the same invalid IType. The TreeViewer only updates
			 * the label of one of these ITypes and leaves the others in their
			 * old state.
			 */
			fireLabelProviderChanged(new LabelProviderChangedEvent(this));
		}

		@Override
		public Color getForeground(Object element) {
			if (isInvalid(element))
				return fGrayColor;
			else
				return null;
		}

		private boolean isInvalid(Object element) {
			if (fInvalidTypes == null)
				return false; // initially, everything is enabled
			else
				return fInvalidTypes.contains(element);
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		@Override
		public Image getImage(Object element) {
			Image image= super.getImage(element);
			if (isInvalid(element) && image != null) {
				Image grayImage= fGrayImages.get(image);
				if (grayImage == null) {
					grayImage= new Image(Display.getCurrent(), image, SWT.IMAGE_GRAY);
					fGrayImages.put(image, grayImage);
				}
				return grayImage;
			} else {
				return image;
			}
		}

		@Override
		public void dispose() {
			for (Image image : fGrayImages.values()) {
				image.dispose();
			}
			fGrayImages.clear();
			super.dispose();
		}
	}

	private class ChangeTypeInputPage extends UserInputWizardPage{

		public static final String PAGE_NAME= "ChangeTypeInputPage";//$NON-NLS-1$
		private final  String MESSAGE= RefactoringMessages.ChangeTypeInputPage_Select_Type;
		private ChangeTypeLabelProvider fLabelProvider;
		private TreeViewer fTreeViewer;
		private boolean fTreeUpdated= false;

		public ChangeTypeInputPage() {
			super(PAGE_NAME);
			setMessage(MESSAGE);
		}

		private class ValidTypesTask implements Runnable {
			private Collection<ITypeBinding> fInvalidTypes;
			private Collection<ITypeBinding> fValidTypes;
			@Override
			public void run() {
				IRunnableWithProgress runnable= pm -> {
					pm.beginTask(RefactoringMessages.ChangeTypeWizard_analyzing, 1000);
					ChangeTypeRefactoring ct= (ChangeTypeRefactoring)ChangeTypeWizard.this.getRefactoring();
					fInvalidTypes = new HashSet<>();
					fInvalidTypes.addAll(fCT.getAllSuperTypes(ct.getOriginalType()));
					fValidTypes= ct.computeValidTypes(new SubProgressMonitor(pm, 950));
					fInvalidTypes.add(ct.getOriginalType());
					fInvalidTypes.removeAll(fValidTypes);
					pm.worked(50);
					pm.done();
				};
				boolean internalError= false;
				try {
					getWizard().getContainer().run(true, true, runnable);
				} catch (InvocationTargetException e) {
					internalError= true;
					JavaPlugin.log(e);
					ChangeTypeInputPage.this.setErrorMessage(RefactoringMessages.ChangeTypeWizard_internalError);
				} catch (InterruptedException e) {
					ChangeTypeInputPage.this.setMessage(RefactoringMessages.ChangeTypeWizard_computationInterrupted);
				}

				fLabelProvider.grayOut(fInvalidTypes);

				if (internalError) {
					setPageComplete(false);
				} else if (fValidTypes == null || fValidTypes.isEmpty()){
					ChangeTypeInputPage.this.setErrorMessage(RefactoringMessages.ChangeTypeWizard_declCannotBeChanged);
					setPageComplete(false);
				} else {
					TreeItem selection= getInitialSelection(fValidTypes);
					fTreeViewer.getTree().setSelection(new TreeItem[]{ selection });
					setPageComplete(true);
					ChangeTypeInputPage.this.setMessage(""); //$NON-NLS-1$
				}
			}
		}

		private TreeItem getInitialSelection(Collection<ITypeBinding> types) {

			// first, find a most general valid type (there may be more than one)
			ITypeBinding type= types.iterator().next();
			for (ITypeBinding other : types) {
				if (getGeneralizeTypeRefactoring().isSubTypeOf(type, other)){
					type= other;
				}
			}

			// now find a corresponding TreeItem (there may be more than one)
			return findItem(fTreeViewer.getTree().getItems(), type);
		}

		private TreeItem findItem(TreeItem[] items, ITypeBinding type){
			for (TreeItem item : items) {
				if (item.getData().equals(type)) {
					return item;
				}
			}
			for (TreeItem ti : items) {
				TreeItem item= findItem(ti.getItems(), type);
				if (item != null) return item;
			}
			return null;
		}


		@Override
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			setControl(composite);
			composite.setLayout(new GridLayout());
			composite.setLayoutData(new GridData());

			Label label= new Label(composite, SWT.NONE);
			label.setText(Messages.format(
					RefactoringMessages.ChangeTypeWizard_pleaseChooseType,
					((ChangeTypeRefactoring) getRefactoring()).getTarget()));
			label.setLayoutData(new GridData());

			addTreeComponent(composite);
			Dialog.applyDialogFont(composite);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.CHANGE_TYPE_WIZARD_PAGE);
		}

		/**
		 * Tree-viewer that shows the allowable types in a tree view.
		 * @param parent the parent
		 */
		private void addTreeComponent(Composite parent) {
			fTreeViewer= new TreeViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.grabExcessHorizontalSpace= true;
			gd.grabExcessVerticalSpace= true;
			Tree tree= fTreeViewer.getTree();
			Dialog.applyDialogFont(tree);
			gd.heightHint= tree.getItemHeight() * 12;
			tree.setLayoutData(gd);

			fTreeViewer.setContentProvider(new ChangeTypeContentProvider(((ChangeTypeRefactoring)getRefactoring())));
			fLabelProvider= new ChangeTypeLabelProvider();
			fTreeViewer.setLabelProvider(fLabelProvider);
			ISelectionChangedListener listener= event -> {
				IStructuredSelection selection= (IStructuredSelection)event.getSelection();
				typeSelected((ITypeBinding)selection.getFirstElement());
			};
			fTreeViewer.addSelectionChangedListener(listener);
			fTreeViewer.setInput(new ChangeTypeContentProvider.RootType(getGeneralizeTypeRefactoring().getOriginalType()));
			fTreeViewer.expandToLevel(10);
		}

		private void typeSelected(ITypeBinding type) {
			boolean isValid= getGeneralizeTypeRefactoring().getValidTypes().contains(type);
			ChangeTypeInputPage.this.setPageComplete(isValid);
			if (isValid) {
				ChangeTypeInputPage.this.setMessage(""); //$NON-NLS-1$
			} else {
				if (getGeneralizeTypeRefactoring().getOriginalType().equals(type)) {
					ChangeTypeInputPage.this.setMessage(Messages.format(
						RefactoringMessages.ChangeTypeWizard_with_itself, BasicElementLabels.getJavaElementName(type.getName())));

				} else {
					ChangeTypeInputPage.this.setMessage(Messages.format(
						RefactoringMessages.ChangeTypeWizard_grayed_types,
						new Object[] {BasicElementLabels.getJavaElementName(type.getName()), BasicElementLabels.getJavaElementName(getGeneralizeTypeRefactoring().getOriginalType().getName())}));
				}
			}
		}

		private ChangeTypeRefactoring getGeneralizeTypeRefactoring(){
			return (ChangeTypeRefactoring)getRefactoring();
		}
		/*
		 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
		 */
		@Override
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private ITypeBinding getSelectedType() {
			IStructuredSelection ss= (IStructuredSelection)fTreeViewer.getSelection();
			return (ITypeBinding)ss.getFirstElement();
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
		 */
		@Override
		public boolean performFinish(){
			initializeRefactoring();
			return super.performFinish();
		}

		private void initializeRefactoring() {
			getGeneralizeTypeRefactoring().setSelectedType(getSelectedType());
		}

		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
		 */
		@Override
		public void dispose() {
			fTreeViewer= null;
			super.dispose();
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible && fTreeViewer != null)
				fTreeViewer.getTree().setFocus();
				if (!fTreeUpdated){
					fTreeViewer.getTree().getDisplay().asyncExec(new ValidTypesTask());
					fTreeUpdated= true;
				}
		}
	}
}
