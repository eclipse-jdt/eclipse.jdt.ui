/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;

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

		/* package */ IRenameRefactoring fRefactoring;

		protected AbstractRenameSupport(IRenameRefactoring refactoring) {
			fRefactoring= refactoring;
		}

		public RefactoringStatus lightCheck() throws JavaModelException {
			return lightCheck(fRefactoring);
		}

		public abstract RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException; 
			
		public IRenameRefactoring getRefactoring() {
			return fRefactoring;
		}

		public boolean canRename(Object element) throws JavaModelException{
			// TODO This is a workaround to create the right refactoring when renaming a virtual method.
			// This should be moved to a factory with a call back in 2.2
			if (fRefactoring instanceof RenameMethodRefactoring && element instanceof IMethod) {
				fRefactoring= RenameMethodRefactoring.createInstance((IMethod)element, (RenameMethodRefactoring)fRefactoring);
			}
			boolean canRename= lightCheck().isOK();
			 if (!canRename)	
			 	fRefactoring= null;
			 return canRename;	
		}
		
		public void rename(Shell parent, Object element) throws JavaModelException{
			Assert.isNotNull(fRefactoring);
			RefactoringWizard wizard= createWizard(fRefactoring);
			RefactoringStarter starter= new RefactoringStarter();
			SelectionState state= new SelectionState(element);
			Object newElementToRename;
			if (wizard != null)
				newElementToRename= starter.activate((Refactoring)fRefactoring, wizard, parent, RefactoringMessages.getString("RefactoringSupportFactory.rename"), true); //$NON-NLS-1$
			else	
				newElementToRename= starter.activate(fRefactoring, parent, RefactoringMessages.getString("RefactoringSupportFactory.rename"), getNameEntryMessage(), false, element); //$NON-NLS-1$
			
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
		
		public RefactoringWizard createWizard(IRenameRefactoring ref) {
			return null;
		}
				
		String getNameEntryMessage(){
			return ""; //$NON-NLS-1$
		}
	}
	
	public static class JavaProject extends AbstractRenameSupport {
		public JavaProject(IJavaProject project) {
			super(new RenameJavaProjectRefactoring(project));
		}
		public RenameJavaProjectRefactoring getSpecificRefactoring() {
			return (RenameJavaProjectRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameJavaProjectRefactoring)refactoring).checkActivation(new NullProgressMonitor());
		}
		String getNameEntryMessage(){
			return RefactoringMessages.getString("RefactoringSupportFactory.project_name"); //$NON-NLS-1$
		}
	}

	public static class SourceFolder extends AbstractRenameSupport {
		public SourceFolder(IPackageFragmentRoot root) {
			super(new RenameSourceFolderRefactoring(root));
		}
		public RenameSourceFolderRefactoring getSpecificRefactoring() {
			return (RenameSourceFolderRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameSourceFolderRefactoring)refactoring).checkActivation(new NullProgressMonitor());
		}
		String getNameEntryMessage(){
			return RefactoringMessages.getString("RefactoringSupportFactory.source_folder_name"); //$NON-NLS-1$
		}
	}
	
	public static class PackageFragment extends AbstractRenameSupport {
		public PackageFragment(IPackageFragment element) {
			super(new RenamePackageRefactoring(element));
		}
		public RenamePackageRefactoring getSpecificRefactoring() {
			return (RenamePackageRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenamePackageRefactoring)refactoring).checkActivation(new NullProgressMonitor());
		}
		public RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringSupportFactory.rename_Package"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringSupportFactory.package_name"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
			String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
		}
	}
	
	public static class CompilationUnit extends AbstractRenameSupport {
		public CompilationUnit(ICompilationUnit unit) {
			super(createRefactoring(unit));
		}
		public RenameCompilationUnitRefactoring getSpecificRefactoring() {
			return (RenameCompilationUnitRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameCompilationUnitRefactoring)refactoring).checkPreactivation();
		}
		public RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringSupportFactory.rename_Cu"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringSupportFactory.compilation_unit"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE; 
			String errorPageHelp= IJavaHelpContextIds.RENAME_CU_ERROR_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
		}
		private static IRenameRefactoring createRefactoring(ICompilationUnit element) {
			ICompilationUnit cu= (ICompilationUnit)element;
			if (cu.isWorkingCopy())
				return new RenameCompilationUnitRefactoring((ICompilationUnit)cu.getOriginalElement());
			return new RenameCompilationUnitRefactoring(cu);	
		}
	}
	
	public static class Type extends AbstractRenameSupport {
		public Type(IType element) {
			super(new RenameTypeRefactoring(element));
		}
		public RenameTypeRefactoring getSpecificRefactoring() {
			return (RenameTypeRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameTypeRefactoring)refactoring).checkPreactivation();
		}
		public RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_type_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_type_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE; 
			String errorPageHelp= IJavaHelpContextIds.RENAME_TYPE_ERROR_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_TYPE;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
		}
	}
	
	public static class Method extends AbstractRenameSupport {
		public Method(IMethod element) throws JavaModelException {
			super(RenameMethodRefactoring.createInstance(element));
		}
		public RenameMethodRefactoring getSpecificRefactoring() {
			return (RenameMethodRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameMethodRefactoring)refactoring).checkPreactivation();
		}
		public RefactoringWizard createWizard(IRenameRefactoring refactoring) {
			String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE;
			String errorPageHelp= IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE;
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD;
			return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
		}
	}
	
	public static class Field extends AbstractRenameSupport {
		public Field(IField element) {
			super(new RenameFieldRefactoring(element));
		}
		public RenameFieldRefactoring getSpecificRefactoring() {
			return (RenameFieldRefactoring)fRefactoring;
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameFieldRefactoring)refactoring).checkPreactivation();
		}
		public RefactoringWizard createWizard(IRenameRefactoring refactoring){
			String title= RefactoringMessages.getString("RefactoringGroup.rename_field_title"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("RefactoringGroup.rename_field_message"); //$NON-NLS-1$
			String wizardPageHelp= IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE; 
			String errorPageHelp= IJavaHelpContextIds.RENAME_FIELD_ERROR_WIZARD_PAGE;
			//XXX: missing icon for field
			ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
			RenameFieldWizard w= new RenameFieldWizard(refactoring, title, message, wizardPageHelp, errorPageHelp);
			w.setInputPageImageDescriptor(imageDesc);
			return w;
		}
	}
	
	public static class Resource extends AbstractRenameSupport {
		public Resource(IResource element) {
			super(new RenameResourceRefactoring(element));
		}
		public RefactoringStatus lightCheck(IRenameRefactoring refactoring) throws JavaModelException{
			return ((RenameResourceRefactoring)refactoring).checkActivation(new NullProgressMonitor());
		}
		String getNameEntryMessage(){
			return RefactoringMessages.getString("RefactoringSupportFactory.resource_name"); //$NON-NLS-1$
		}
	}
	
	private static RefactoringWizard createRenameWizard(IRenameRefactoring ref, String title, String message, String wizardPageHelp, String errorPageHelp, ImageDescriptor image){
		RenameRefactoringWizard w= new RenameRefactoringWizard(ref, title, message, wizardPageHelp, errorPageHelp);
		w.setInputPageImageDescriptor(image);
		return w;
	}	
}
