/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.util.Assert;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.core.refactoring.cus.RenameCompilationUnitRefactoring;import org.eclipse.jdt.internal.core.refactoring.packages.RenamePackageRefactoring;import org.eclipse.jdt.internal.core.refactoring.projects.RenameProjectRefactoring;
import org.eclipse.jdt.internal.core.refactoring.packageroots.RenameSourceFolderRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;


public class RefactoringSupportFactory {

	private abstract static class RenameSupport implements IRefactoringRenameSupport {
		private Refactoring fRefactoring;
		private static IProgressMonitor fgNullProgressMonitor= new NullProgressMonitor();

		public boolean canRename(Object element) {
			fRefactoring= createRefactoring(element, new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider()));
			try {
				//FIX ME: must have a better solution to this
				if (fRefactoring instanceof IPreactivatedRefactoring){
					if (((IPreactivatedRefactoring)fRefactoring).checkPreactivation().isOK())
						return true;
				} else { 
				 	if (fRefactoring.checkActivation(fgNullProgressMonitor).isOK())
						return true;
				}	
				fRefactoring= null;
				return false;	
			} catch (JavaModelException e) {
				fRefactoring= null;
				return false;
			}	
		}
		
		public void rename(Object element) {
			Assert.isNotNull(fRefactoring);
			Shell parent= JavaPlugin.getActiveWorkbenchShell();
			RefactoringWizard wizard= createWizard();
			wizard.init(fRefactoring);
			RefactoringWizardDialog dialog= new RefactoringWizardDialog(parent, wizard);
			dialog.open();
			fRefactoring= null;
		}
		
		protected abstract Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator);
		
		protected abstract RefactoringWizard createWizard();
	}
	
	private static class RenamePackage extends RenameSupport {
		protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
			return new RenamePackageRefactoring(creator, (IPackageFragment)element);
		}
		
		protected RefactoringWizard createWizard() {
			String title= ReorgMessages.getString("refactoringSupportFactory.renamePkg.title"); //$NON-NLS-1$
			String message= ReorgMessages.getString("refactoringSupportFactory.renamePkg.message"); //$NON-NLS-1$
			RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE, IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE); 
			w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE);
			return w;
		}
	}

	private static class RenameCUnit extends RenameSupport {
		protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
			return new RenameCompilationUnitRefactoring(creator, (ICompilationUnit)element);
		}
		
		protected RefactoringWizard createWizard() {
			String title= ReorgMessages.getString("refactoringSupportFactory.renameCU.title"); //$NON-NLS-1$
			String message= ReorgMessages.getString("refactoringSupportFactory.renameCU.message"); //$NON-NLS-1$
			RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE, IJavaHelpContextIds.RENAME_CU_ERROR_WIZARD_PAGE); 
			w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
			return w;
		}
	}
	
	private static class RenamePackageFragmentRoot extends RenameSupport {
		protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
			return new RenameSourceFolderRefactoring(creator, (IPackageFragmentRoot)element);
		}
		
		protected RefactoringWizard createWizard() {
			String title= "Rename Source Folder";
			String message= "Enter the new name for this source folder.";
			//FIX ME: wrong help
			RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE, IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE); 
			//FIX ME: wrong icon
			w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE);
			return w;
		}
	}
	
	private static class RenameProject extends RenameSupport {
		protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
			return new RenameProjectRefactoring(creator, (IJavaProject)element);
		}
		
		protected RefactoringWizard createWizard() {
			String title= "Rename Java Project";
			String message= "Enter the new name for this Java project.";
			//FIX ME: wrong help
			RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE, IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE); 
			//FIX ME: wrong icon
			w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE);
			return w;
		}
	}

	public static IRefactoringRenameSupport createRenameSupport(Object element) {
			
		if (element instanceof IPackageFragment)
			return new RenamePackage();
		
		if (element instanceof ICompilationUnit)
			return new RenameCUnit();
		
		if (element instanceof IPackageFragmentRoot)
			return new RenamePackageFragmentRoot();
		
		if (element instanceof IJavaProject)
			return new RenameProject();
				
		return null;	
	}
}
