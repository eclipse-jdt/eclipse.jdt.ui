/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.cus.RenameCompilationUnitRefactoring;
import org.eclipse.jdt.internal.core.refactoring.packageroots.RenameSourceFolderRefactoring;
import org.eclipse.jdt.internal.core.refactoring.packages.RenamePackageRefactoring;
import org.eclipse.jdt.internal.core.refactoring.projects.RenameJavaProjectRefactoring;
import org.eclipse.jdt.internal.core.refactoring.resources.RenameResourceRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringWizardFactory;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.widgets.Shell;

public class RefactoringSupportFactory {

	private abstract static class RenameSupport implements IRefactoringRenameSupport {
		private Refactoring fRefactoring;

		public boolean canRename(Object element) {
			fRefactoring= createRefactoring(element, new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider()));
			try {
				//FIX ME: must have a better solution to this
				boolean canRename= false;
				if (fRefactoring instanceof IPreactivatedRefactoring)
					canRename= ((IPreactivatedRefactoring)fRefactoring).checkPreactivation().isOK();
				 else
					canRename= fRefactoring.checkActivation(new NullProgressMonitor()).isOK();
				 if (!canRename)	
				 	fRefactoring= null;
				 return canRename;	
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
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

		private final RefactoringWizard createWizard() {
			return RefactoringWizardFactory.createWizard(fRefactoring);
		}
	}
	
	private static RenameSupport createPackageRename(){
		return new RenameSupport(){
			protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenamePackageRefactoring(creator, (IPackageFragment)element);
			}
		};
	}
	
	private static RenameSupport createCompilationUnitRename(){
		return new RenameSupport(){
			protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameCompilationUnitRefactoring(creator, (ICompilationUnit)element);
			}
		};
	}
	
	private static RenameSupport createSourceFolderRename(){
		return new RenameSupport(){
			protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameSourceFolderRefactoring(creator, (IPackageFragmentRoot)element);
			}
		};
	}
	
	private static RenameSupport createJavaProjectRename(){
		return new RenameSupport(){
			protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameJavaProjectRefactoring(creator, (IJavaProject)element);
			}
		};
	}
	
	private static RenameSupport createResourceRename(){
		return new RenameSupport(){
			protected Refactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameResourceRefactoring(creator, (IResource)element);
			}
		};
	}
	
	public static IRefactoringRenameSupport createRenameSupport(Object element) {
			
		if (element instanceof IPackageFragment)
			return createPackageRename();
			
		if (element instanceof ICompilationUnit)
			return createCompilationUnitRename();
		
		if (element instanceof IPackageFragmentRoot)
			return createSourceFolderRename();
			
		if (element instanceof IJavaProject)
			return createJavaProjectRename();
		
		if (element instanceof IResource)
			return createResourceRename();
			
		return null;	
	}
}
