/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.reorg;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.util.Assert;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.Refactoring;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.core.refactoring.cus.RenameCompilationUnitRefactoring;import org.eclipse.jdt.core.refactoring.packages.RenamePackageRefactoring;import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;

public class RefactoringSupportFactory {

	private abstract static class RenameSupport implements IRefactoringRenameSupport {
		private Refactoring fRefactoring;
		
		public boolean canRename(Object element) {
			fRefactoring= createRefactoring(element, new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider()));
			try {
				RefactoringStatus status= fRefactoring.checkActivation(new NullProgressMonitor());
				if (status.hasFatalError()) {
					fRefactoring= null;
					return false;
				}
				return true;
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
			return new RenameRefactoringWizard("Rename Package"); 
		}
	}

	private static class RenameCUnit extends RenameSupport {
		protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
			return new RenameCompilationUnitRefactoring(creator, (ICompilationUnit)element);
		}
		
		protected RefactoringWizard createWizard() {
			return new RenameRefactoringWizard("Rename Compilation Unit"); 
		}
	}

	public static IRefactoringRenameSupport createRenameSupport(Object element) {
		if(! JavaPlugin.getDefault().getPreferenceStore().getBoolean(
				IPreferencesConstants.LINK_RENAME_IN_PACKAGES_TO_REFACTORING))
			return null;
			
		if (element instanceof IPackageFragment)
			return new RenamePackage();
		
		if (element instanceof ICompilationUnit)
			return new RenameCUnit();
				
		return null;	
	}
}
