/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameCompilationUnitRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameFieldRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameJavaProjectRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameResourceRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameSourceFolderRefactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameTypeRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;


public class RefactoringSupportFactory {

	private abstract static class RenameSupport implements IRefactoringRenameSupport {
		private IRenameRefactoring fRefactoring;

		public boolean canRename(Object element) {
			try {
				fRefactoring= createRefactoring(element, new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider()));
				//FIX ME: must have a better solution to this
				boolean canRename= false;
				if (fRefactoring instanceof IPreactivatedRefactoring)
					canRename= ((IPreactivatedRefactoring)fRefactoring).checkPreactivation().isOK();
				 else
					canRename= ((Refactoring)fRefactoring).checkActivation(new NullProgressMonitor()).isOK();
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
			new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), createWizard(fRefactoring)).open();
			fRefactoring= null;
		}
		
		abstract IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) throws JavaModelException;

		abstract RefactoringWizard createWizard(IRenameRefactoring ref);
	}
	
	private static RefactoringWizard createRenameWizard(IRenameRefactoring ref, String title, String message, String wizardPageHelp, String errorPageHelp, ImageDescriptor image){
		RenameRefactoringWizard w= new RenameRefactoringWizard(ref, title, message, wizardPageHelp, errorPageHelp);
		w.setInputPageImageDescriptor(image);
		return w;
	}
	
	private static RenameSupport createPackageRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenamePackageRefactoring(creator, (IPackageFragment)element);
			}
			
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
				String title= "Rename Package";
				String message= "Enter the new name for this package. References to all types declared in it will be updated.";
				String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
				String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}
		};
	}
	
	private static RenameSupport createCompilationUnitRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameCompilationUnitRefactoring(creator, (ICompilationUnit)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
				String title= "Rename Compilation Unit";
				String message= "Enter the new name for this compilation unit. Refactoring will also rename and update references to the type (if any exists) that has the same name as this compilation unit.";
				String wizardPageHelp= IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE; 
				String errorPageHelp= IJavaHelpContextIds.RENAME_CU_ERROR_WIZARD_PAGE;
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}
		};
	}
	
	private static RenameSupport createSourceFolderRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameSourceFolderRefactoring(creator, (IPackageFragmentRoot)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
				String title= "Rename Source Folder";
				String message= "Enter the new name for this source folder.";
				//FIX ME: wrong help and icon
				String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
				String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}
		};
	}
	
	private static RenameSupport createJavaProjectRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameJavaProjectRefactoring(creator, (IJavaProject)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
				String title= "Rename Java Project";
				String message= "Enter the new name for this Java project.";
				//FIX ME: wrong help and icon
				String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
				String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_NEWJPRJ;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}
		};
	}
	
	private static RenameSupport createResourceRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameResourceRefactoring(creator, (IResource)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
					String title= "Rename Resource";
					String message= "Enter the new name for this resource.";
					//FIX ME: wrong help and icon
					String wizardPageHelp= IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE; 
					String errorPageHelp= IJavaHelpContextIds.RENAME_CU_ERROR_WIZARD_PAGE;
					ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
					return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}
		};
	}
	
	private static RenameSupport createTypeRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameTypeRefactoring(creator, (IType)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_type_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_type_message"); //$NON-NLS-1$
				String wizardPageHelp= IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE; 
				String errorPageHelp= IJavaHelpContextIds.RENAME_TYPE_ERROR_WIZARD_PAGE;
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_TYPE;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}
		};
	}
	
	private static RenameSupport createMethodRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) throws JavaModelException{
				return RenameMethodRefactoring.createInstance(creator, (IMethod)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring) {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
				String wizardPageHelp= IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE;
				String errorPageHelp= IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE;
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
			}	
		};
	}
	
	private static RenameSupport createFieldRename(){
		return new RenameSupport(){
			IRenameRefactoring createRefactoring(Object element, ITextBufferChangeCreator creator) {
				return new RenameFieldRefactoring(creator, (IField)element);
			}
			RefactoringWizard createWizard(IRenameRefactoring refactoring){
				String title= RefactoringMessages.getString("RefactoringGroup.rename_field_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_field_message"); //$NON-NLS-1$
				String wizardPageHelp= IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE; 
				String errorPageHelp= IJavaHelpContextIds.RENAME_FIELD_ERROR_WIZARD_PAGE;
				//XXX: missing icon for field
				ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
				return createRenameWizard(refactoring, title, message, wizardPageHelp, errorPageHelp, imageDesc);
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
		
		if (element instanceof IType)
			return createTypeRename();
			
		if (element instanceof IMethod)
			return createMethodRename();

		if (element instanceof IField)
			return createFieldRename();
			
		return null;	
	}
}
