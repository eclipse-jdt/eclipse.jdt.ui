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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.reorg.IRefactoringRenameSupport;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameJavaProjectRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameResourceRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameSourceFolderRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

public class RefactoringSupport {
	/* package */ abstract static class AbstractRenameSupport implements IRefactoringRenameSupport {
		
		private static class SelectionState {
			private Display fDisplay;
			private Object fElement;
			private List fParts;
			private List fSelections;
			public SelectionState(Object element) {
				fElement= element;
				fParts= new ArrayList();
				fSelections= new ArrayList();
				init();
			}
			private void init() {
				IWorkbenchWindow dw = JavaPlugin.getActiveWorkbenchWindow();
				if (dw ==  null)
					return;
				fDisplay= dw.getShell().getDisplay();
				IWorkbenchPage page = dw.getActivePage();
				if (page == null)
					return;
				IViewReference vrefs[]= page.getViewReferences();
				for(int i= 0; i < vrefs.length; i++) {
					consider(vrefs[i].getPart(false));
				}
				IEditorReference refs[]= page.getEditorReferences();
				for(int i= 0; i < refs.length; i++) {
					consider(refs[i].getPart(false));
				}
			}
			private void consider(IWorkbenchPart part) {
				if (part == null)
					return;
				ISetSelectionTarget target= null;
				if (!(part instanceof ISetSelectionTarget)) {
					target= (ISetSelectionTarget)part.getAdapter(ISetSelectionTarget.class);
					if (target == null)
						return;
				} else {
					target= (ISetSelectionTarget)part;
				}
				ISelection s= part.getSite().getSelectionProvider().getSelection();
				if (!(s instanceof IStructuredSelection))
					return;
				IStructuredSelection selection= (IStructuredSelection)s;
				if (!selection.toList().contains(fElement))
					return;
				fParts.add(part);
				fSelections.add(selection);
			}
			public void restore(Object newElement) {
				if (fDisplay == null)
					return;
				for (int i= 0; i < fParts.size(); i++) {
					final IStructuredSelection selection= (IStructuredSelection)fSelections.get(i);
					final ISetSelectionTarget target= (ISetSelectionTarget)fParts.get(i);
					List l= selection.toList();
					int index= l.indexOf(fElement);
					if (index != -1) { 
						l.set(index, newElement);
						fDisplay.asyncExec(new Runnable() {
							public void run() {
								target.selectReveal(selection);
							}
						});
					}
				}
			}
		}

		private IRenameRefactoring fRefactoring;
		
		protected AbstractRenameSupport(IRenameRefactoring refactoring) {
			fRefactoring= refactoring;//can be null
		}

		public final RefactoringStatus lightCheck() throws JavaModelException {
			if (fRefactoring == null)
				return RefactoringStatus.createFatalErrorStatus("This refactoring is not enabled.");
			return new RefactoringStatus();
		}

		public final IRenameRefactoring getRefactoring() {
			return fRefactoring;
		}
		
		public final boolean canRename(Object element) throws JavaModelException{
			// TODO This is a workaround to create the right refactoring when renaming a virtual method.
			// This should be moved to a factory with a call back in 2.2
			if (fRefactoring instanceof RenameMethodRefactoring && element instanceof IMethod) {
				fRefactoring= RenameMethodRefactoring.create((IMethod)element, (RenameMethodRefactoring)fRefactoring);
			}
			return fRefactoring != null;	
		}
		
		public final void rename(Shell parent, Object element) throws JavaModelException{
			Assert.isNotNull(fRefactoring);
			RefactoringWizard wizard= createWizard(fRefactoring);
			Assert.isNotNull(wizard);
			RefactoringStarter starter= new RefactoringStarter();
			SelectionState state= new SelectionState(element);
			Object newElementToRename= starter.activate((Refactoring)fRefactoring, wizard, parent, RefactoringMessages.getString("RefactoringSupportFactory.rename"), true); //$NON-NLS-1$
			
			if (newElementToRename == null)	{
				state.restore(fRefactoring.getNewElement());
				fRefactoring= null;
			} else {
				if (canRename(newElementToRename)) {
					rename(parent, newElementToRename);
				} else {
					MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.getString("RefactoringSupportFactory.error.title"), RefactoringMessages.getString("RefactoringSupportFactory.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}	
		}
		
		abstract RefactoringWizard createWizard(IRenameRefactoring ref);
	}
	
	public static class JavaProject extends AbstractRenameSupport {
		public JavaProject(IJavaProject project) throws JavaModelException {
			super(RenameJavaProjectRefactoring.create(project));
		}
		public RenameJavaProjectRefactoring getSpecificRefactoring() {
			return (RenameJavaProjectRefactoring)getRefactoring();
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_java_project_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_java_project_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_JAVA_PROJECT_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
	}

	public static class SourceFolder extends AbstractRenameSupport {
		public SourceFolder(IPackageFragmentRoot root) throws JavaModelException {
			super(RenameSourceFolderRefactoring.create(root));
		}
		public RenameSourceFolderRefactoring getSpecificRefactoring() {
			return (RenameSourceFolderRefactoring)getRefactoring();
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_source_folder_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_source_folder_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_SOURCE_FOLDER_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
	}
	
	public static class PackageFragment extends AbstractRenameSupport {
		public PackageFragment(IPackageFragment element) throws JavaModelException {
			super(RenamePackageRefactoring.create(element));
		}
		public RenamePackageRefactoring getSpecificRefactoring() {
			return (RenamePackageRefactoring)getRefactoring();
		}
		public RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringSupportFactory.rename_Package"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringSupportFactory.package_name"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
	}
	
	public static class CompilationUnit extends AbstractRenameSupport {
		public CompilationUnit(ICompilationUnit unit) throws JavaModelException {
			super(createRefactoring(unit));
		}
		public RenameCompilationUnitRefactoring getSpecificRefactoring() {
			return (RenameCompilationUnitRefactoring)getRefactoring();
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringSupportFactory.rename_Cu"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringSupportFactory.compilation_unit"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE; 
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
		private static IRenameRefactoring createRefactoring(ICompilationUnit element) throws JavaModelException {
			ICompilationUnit cu= (ICompilationUnit)element;
			if (cu.isWorkingCopy())
				return RenameCompilationUnitRefactoring.create((ICompilationUnit)cu.getOriginalElement());
			return RenameCompilationUnitRefactoring.create(cu);	
		}
	}
	
	public static class Type extends AbstractRenameSupport {
		public Type(IType element) throws JavaModelException {
			super(RenameTypeRefactoring.create(element));
		}
		public RenameTypeRefactoring getSpecificRefactoring() {
			return (RenameTypeRefactoring)getRefactoring();
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_type_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_type_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE; 
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_TYPE;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
	}
	
	public static class Method extends AbstractRenameSupport {
		public Method(IMethod element) throws JavaModelException {
			super(RenameMethodRefactoring.create(element));
		}
		public RenameMethodRefactoring getSpecificRefactoring() {
			return (RenameMethodRefactoring)getRefactoring();
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
	}
	
	public static class Field extends AbstractRenameSupport {
		public Field(IField element) throws JavaModelException {
			super(RenameFieldRefactoring.create(element));
		}
		public RenameFieldRefactoring getSpecificRefactoring() {
			return (RenameFieldRefactoring)getRefactoring();
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring){
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_FIELD;
			RenameFieldWizard w= new RenameFieldWizard(refactoring);
			w.setInputPageImageDescriptor(imageDesc);
			return w;
		}
	}
	
	public static class Resource extends AbstractRenameSupport {
		public Resource(IResource element) throws JavaModelException {
			super(RenameResourceRefactoring.create(element));
		}
		RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_resource_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_resource_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_RESOURCE_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, imageDesc);
		}
	}
	
	private RefactoringSupport(){}
	private static RefactoringWizard createRenameWizard(IRenameRefactoring ref, String title, String message, String wizardPageHelp, ImageDescriptor image){
		RenameRefactoringWizard w= new RenameRefactoringWizard(ref, title, message, wizardPageHelp);
		w.setInputPageImageDescriptor(image);
		return w;
	}	
}
