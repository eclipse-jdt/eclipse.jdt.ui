package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICopyQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CopyQueries implements ICopyQueries {

	private static final String EMPTY= " "; //XXX workaround for bug#16256

	public CopyQueries() {
	}

	private static String removeTrailingJava(String name) {
		Assert.isTrue(name.endsWith(".java"));
		return name.substring(0, name.length() - ".java".length());
	}

	public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu) {
		String key= "Enter a new name for ''{0}''";
		String message= MessageFormat.format(key, new String[]{cu.getElementName()});
		return createStaticQuery(createCompilationUnitNameValidator(cu), message, removeTrailingJava(cu.getElementName()));
	}


	public INewNameQuery createNewResourceNameQuery(IResource res) {
		String key= "Enter a new name for ''{0}''";
		String message= MessageFormat.format(key, new String[]{ res.getName()});
		return createStaticQuery(createResourceNameValidator(res), message, res.getName());
	}


	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack) {
		String key= "Enter a new name for ''{0}''";
		String message= MessageFormat.format(key, new String[]{pack.getElementName()});
		return createStaticQuery(createPackageNameValidator(pack), message, pack.getElementName());
	}


	public INewNameQuery createNullQuery(){
		return createStaticQuery(null);
	}


	public INewNameQuery createStaticQuery(final String newName){
		return new INewNameQuery(){
			public String getNewName() {
				return newName;
			}
		};
	}

	private static INewNameQuery createStaticQuery(final IInputValidator validator, final String message, final String initial){
		return new INewNameQuery(){
			public String getNewName() {
				InputDialog dialog= new InputDialog(JavaPlugin.getActiveWorkbenchShell(), "Name Conflict", message, initial, validator);
				if (dialog.open() == Window.CANCEL)
					throw new OperationCanceledException();
				return dialog.getValue();
			}
		};
	}

	private static IInputValidator createResourceNameValidator(final IResource res){
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText) || res.getParent() == null)
					return EMPTY;
				if (res.getParent().findMember(newText) != null)
					return "Resource with this name already exists";
				if (! res.getParent().getFullPath().isValidSegment(newText))
					return "Invalid name";
				IStatus status= res.getParent().getWorkspace().validateName(newText, res.getType());
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
					
				if (res.getName().equalsIgnoreCase(newText))
					return "Resource exists with different case";
					
				return null;
			}
		};
		return validator;
	}

	private static IInputValidator createCompilationUnitNameValidator(final ICompilationUnit cu) {
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText))
					return EMPTY;
				String newCuName= newText + ".java";
				IStatus status= JavaConventions.validateCompilationUnitName(newCuName);	
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
				RefactoringStatus refStatus;
				try {
					refStatus= Checks.checkCompilationUnitNewName(cu, newText);
				} catch (JavaModelException e) {
					return EMPTY;
				}
				if (refStatus.hasFatalError())
					return refStatus.getFirstMessage(RefactoringStatus.FATAL);

				if (cu.getElementName().equalsIgnoreCase(newCuName))
					return "Resource exists with different case";
				
				return null;	
			}
		};
		return validator;
	}


	private static IInputValidator createPackageNameValidator(final IPackageFragment pack) {
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText))
					return EMPTY;
				IStatus status= JavaConventions.validatePackageName(newText);
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
				
				IJavaElement parent= pack.getParent();
				try {
					if (parent instanceof IPackageFragmentRoot){ 
						if (! RenamePackageRefactoring.isPackageNameOkInRoot(newText, (IPackageFragmentRoot)parent))
							return "Package with that name exists";	
					}	
				} catch (JavaModelException e) {
					return EMPTY;
				}
				if (pack.getElementName().equalsIgnoreCase(newText))
					return "Resource exists with different case";
					
				return null;
			}
		};	
		return validator;
	}			
}
