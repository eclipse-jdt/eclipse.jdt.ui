package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.cus.RenameCompilationUnitRefactoring;
import org.eclipse.jdt.internal.core.refactoring.fields.RenameFieldRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameParametersRefactoring;
import org.eclipse.jdt.internal.core.refactoring.packageroots.RenameSourceFolderRefactoring;
import org.eclipse.jdt.internal.core.refactoring.packages.RenamePackageRefactoring;
import org.eclipse.jdt.internal.core.refactoring.projects.RenameJavaProjectRefactoring;
import org.eclipse.jdt.internal.core.refactoring.resources.RenameResourceRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jface.resource.ImageDescriptor;

public class RefactoringWizardFactory {

	//no instances
	private RefactoringWizardFactory(){
	}
		
	public static  RefactoringWizard createWizard(Refactoring ref){
		RefactoringWizard result= null;

		if (ref instanceof RenameFieldRefactoring)
			result= createWizard((RenameFieldRefactoring)ref);

		else if (ref instanceof RenameTypeRefactoring)
			result= createWizard((RenameTypeRefactoring)ref);	

		else if (ref instanceof RenameJavaProjectRefactoring)
			result= createWizard((RenameJavaProjectRefactoring)ref);		
	
		else if (ref instanceof RenameSourceFolderRefactoring)
			result= createWizard((RenameSourceFolderRefactoring)ref);		
		
		else if (ref instanceof RenamePackageRefactoring)
			result= createWizard((RenamePackageRefactoring)ref);		

		else if (ref instanceof RenameCompilationUnitRefactoring)
			result= createWizard((RenameCompilationUnitRefactoring)ref);	
			
		else if (ref instanceof RenameResourceRefactoring)	
			result= createWizard((RenameResourceRefactoring)ref);	 
			
		else if (ref instanceof RenameMethodRefactoring)
			result= createWizard((RenameMethodRefactoring)ref);	

		else if (ref instanceof RenameParametersRefactoring)	
			result= new RenameParametersWizard();

		if (result != null)
			result.init(ref);		

		return result;	
	}
	
	//--
	private static ITextBufferChangeCreator getChangeCreator(){
		return  new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
	}
	
	private static RefactoringWizard createWizard(RenameJavaProjectRefactoring ref){
		String title= "Rename Java Project";
		String message= "Enter the new name for this Java project.";
		//FIX ME: wrong help and icon
		String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_NEWJPRJ;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}
	
	private static RefactoringWizard createWizard(RenameSourceFolderRefactoring ref){
		String title= "Rename Source Folder";
		String message= "Enter the new name for this source folder.";
		//FIX ME: wrong help and icon
		String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}
	
	private static RefactoringWizard createWizard(RenamePackageRefactoring ref){
		String title= "Rename Package";
		String message= "Enter the new name for this package. References to all types declared in it will be updated.";
		String wizardPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_PACKAGE_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}
	
	private static RefactoringWizard createWizard(RenameCompilationUnitRefactoring ref){
		String title= "Rename Compilation Unit";
		String message= "Enter the new name for this compilation unit. Refactoring will also rename and update references to the type (if any exists) that has the same name as this compilation unit.";
		String wizardPageHelp= IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_CU_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}
	
	private static RefactoringWizard createWizard(RenameResourceRefactoring ref){
		String title= "Rename Resource";
		String message= "Enter the new name for this resource.";
		//FIX ME: wrong help and icon
		String wizardPageHelp= IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_CU_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}
	
	private static RefactoringWizard createWizard(RenameFieldRefactoring ref){
		String title= RefactoringMessages.getString("RefactoringGroup.rename_field_title"); //$NON-NLS-1$
		String message= RefactoringMessages.getString("RefactoringGroup.rename_field_message"); //$NON-NLS-1$
		String wizardPageHelp= IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_FIELD_ERROR_WIZARD_PAGE;
		//XXX: missing icon for field
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_CU;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}

	private static  RefactoringWizard createWizard(RenameTypeRefactoring ref) {
		String title= RefactoringMessages.getString("RefactoringGroup.rename_type_title"); //$NON-NLS-1$
		String message= RefactoringMessages.getString("RefactoringGroup.rename_type_message"); //$NON-NLS-1$
		String wizardPageHelp= IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_TYPE_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_TYPE;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}

	private static  RefactoringWizard createWizard(RenameMethodRefactoring ref) {
		String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
		String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
		String wizardPageHelp= IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE;
		String errorPageHelp= IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE;
		ImageDescriptor imageDesc= JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD;
		return createRenameWizard(ref, title, message, wizardPageHelp, errorPageHelp, imageDesc);
	}
	
	private static RefactoringWizard createRenameWizard(IRenameRefactoring ref, String title, String message, String wizardPageHelp, String errorPageHelp, ImageDescriptor image){
		RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, wizardPageHelp, errorPageHelp);
		w.setInputPageImageDescriptor(image);
		return w;
	}
}

