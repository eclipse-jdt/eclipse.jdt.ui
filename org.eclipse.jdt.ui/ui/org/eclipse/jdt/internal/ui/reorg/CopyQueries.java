package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICopyQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IDeepCopyQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

public class CopyQueries implements ICopyQueries {

	private static final String EMPTY= " "; //XXX workaround for bug#16256 //$NON-NLS-1$

	private IDeepCopyQuery fDeepCopyQuery;

	public CopyQueries() {
		//just one instance, so that we get the correct 'yes to all' behavior
		fDeepCopyQuery= new DeepCopyQuery(); 
	}

	private static String removeTrailingJava(String name) {
		Assert.isTrue(name.endsWith(".java")); //$NON-NLS-1$
		return name.substring(0, name.length() - ".java".length()); //$NON-NLS-1$
	}

	public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu) {
		String key= ReorgMessages.getString("CopyQueries.enterNewNameQuestion"); //$NON-NLS-1$
		String message= MessageFormat.format(key, new String[]{removeTrailingJava(cu.getElementName())});
		return createStaticQuery(createCompilationUnitNameValidator(cu), message, removeTrailingJava(cu.getElementName()));
	}


	public INewNameQuery createNewResourceNameQuery(IResource res) {
		String key= ReorgMessages.getString("CopyQueries.enterNewNameQuestion"); //$NON-NLS-1$
		String message= MessageFormat.format(key, new String[]{ res.getName()});
		return createStaticQuery(createResourceNameValidator(res), message, res.getName());
	}


	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack) {
		String key= ReorgMessages.getString("CopyQueries.enterNewNameQuestion"); //$NON-NLS-1$
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
				InputDialog dialog= new InputDialog(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("CopyQueries.nameConflictMessage"), message, initial, validator); //$NON-NLS-1$
				if (dialog.open() == Window.CANCEL)
					throw new OperationCanceledException();
				return dialog.getValue();
			}
		};
	}

	private static IInputValidator createResourceNameValidator(final IResource res){
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText) || res.getParent() == null) //$NON-NLS-1$
					return EMPTY;
				if (res.getParent().findMember(newText) != null)
					return ReorgMessages.getString("CopyQueries.resourceWithThisNameAlreadyExists"); //$NON-NLS-1$
				if (! res.getParent().getFullPath().isValidSegment(newText))
					return ReorgMessages.getString("CopyQueries.invalidNameMessage"); //$NON-NLS-1$
				IStatus status= res.getParent().getWorkspace().validateName(newText, res.getType());
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
					
				if (res.getName().equalsIgnoreCase(newText))
					return ReorgMessages.getString("CopyQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
					
				return null;
			}
		};
		return validator;
	}

	private static IInputValidator createCompilationUnitNameValidator(final ICompilationUnit cu) {
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText)) //$NON-NLS-1$
					return EMPTY;
				String newCuName= newText + ".java"; //$NON-NLS-1$
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
					return ReorgMessages.getString("CopyQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
				
				return null;	
			}
		};
		return validator;
	}


	private static IInputValidator createPackageNameValidator(final IPackageFragment pack) {
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText)) //$NON-NLS-1$
					return EMPTY;
				IStatus status= JavaConventions.validatePackageName(newText);
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
				
				IJavaElement parent= pack.getParent();
				try {
					if (parent instanceof IPackageFragmentRoot){ 
						if (! RenamePackageRefactoring.isPackageNameOkInRoot(newText, (IPackageFragmentRoot)parent))
							return ReorgMessages.getString("CopyQueries.packagewithThatNameexistsMassage");	 //$NON-NLS-1$
					}	
				} catch (JavaModelException e) {
					return EMPTY;
				}
				if (pack.getElementName().equalsIgnoreCase(newText))
					return ReorgMessages.getString("CopyQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
					
				return null;
			}
		};	
		return validator;
	}			

	public IDeepCopyQuery getDeepCopyQuery() {
		return fDeepCopyQuery;
	}
	
	private static class DeepCopyQuery implements IDeepCopyQuery{

		private boolean alwaysDeepCopy= false;
		private boolean neverDeepCopy= false;
		private boolean fCanceled= false;
		
		public boolean performDeepCopy(final IResource source) {
			final Shell parentShell= JavaPlugin.getActiveWorkbenchShell();
			final int[] result = new int[1];
			IPath location = source.getLocation();
		
			if (location == null) {
				//undefined path variable
				return false;
			}
			if (location.toFile().exists() == false) {
				//link target does not exist
				return false;
			}
			if (alwaysDeepCopy) {
				return true;
			}
			if (neverDeepCopy) {
				return false;
			}
			// Dialogs need to be created and opened in the UI thread
			Runnable query = new Runnable() {
				public void run() {
					int resultId[]= {
						IDialogConstants.YES_ID,
						IDialogConstants.YES_TO_ALL_ID,
						IDialogConstants.NO_ID,
						IDialogConstants.NO_TO_ALL_ID,
						IDialogConstants.CANCEL_ID};
 
					String message= MessageFormat.format(	ReorgMessages.getString("CopyQueries.deep_copy"), //$NON-NLS-1$
						new Object[] {source.getFullPath().makeRelative()});
					MessageDialog dialog= new MessageDialog(
						parentShell, 
						ReorgMessages.getString("CopyQueries.Linked_Resource"), //$NON-NLS-1$
						null,
						message,
						MessageDialog.QUESTION,
						new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.YES_TO_ALL_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.NO_TO_ALL_LABEL,
							IDialogConstants.CANCEL_LABEL },
						0);
					dialog.open();
					result[0]= resultId[dialog.getReturnCode()];
				}
			};
			parentShell.getDisplay().syncExec(query);
			if (result[0] == IDialogConstants.YES_TO_ALL_ID) {
				alwaysDeepCopy= true;
				return true;		
			}
			if (result[0] == IDialogConstants.YES_ID) {
				return true;
			}
			if (result[0] == IDialogConstants.NO_TO_ALL_ID) {
				neverDeepCopy= true;
			}
			if (result[0] == IDialogConstants.CANCEL_ID) {
				fCanceled= true;
				throw new OperationCanceledException();
			}
			return false;
		}
	}
}
