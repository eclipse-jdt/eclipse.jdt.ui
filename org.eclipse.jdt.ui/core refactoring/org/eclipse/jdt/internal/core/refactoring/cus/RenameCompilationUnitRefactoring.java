/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.cus;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;

public class RenameCompilationUnitRefactoring extends CompilationUnitRefactoring implements IRenameRefactoring, IPreactivatedRefactoring{

	private String fNewName;
	private RenameTypeRefactoring fRenameTypeRefactoring;
	private boolean fWillRenameType;
	
	public RenameCompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit cu){
		super(changeCreator, cu);
		computeRenameTypeRefactoring();
	}
		
	/* non javadoc
	 * see Refactoring#setUnsavedFileList
	 */
	public void setUnsavedFiles(IFile[] files){
		super.setUnsavedFiles(files);
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUnsavedFiles(files);
	}
		
	/* non java-doc
	 * @see IRenameRefactoring#setNewName(String)
	 * @param newName 'java' must be included
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
		if (fWillRenameType)
			fRenameTypeRefactoring.setNewName(removeFileNameExtension(newName));
	}

	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkCompilationUnitName(fNewName));
		if (fWillRenameType)
			result.merge(fRenameTypeRefactoring.checkNewName());
		if (Checks.isAlreadyNamed(getCompilationUnit(), fNewName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameCompilationUnitRefactoring.same_name"));	 //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return getCompilationUnit().getElementName();
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.name",  //$NON-NLS-1$
															new String[]{getCompilationUnit().getElementName(), fNewName});
	}

	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		if (fRenameTypeRefactoring == null)
			return false;
		return fRenameTypeRefactoring.canEnableUpdateReferences();
	}

	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean update) {
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUpdateReferences(update);
	}

	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		if (fRenameTypeRefactoring == null)
			return false;

		return fRenameTypeRefactoring.getUpdateReferences();		
	}

	//--- preconditions
	
	/* non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		ICompilationUnit cu= getCompilationUnit();
		if (! cu.exists())
			return RefactoringStatus.createFatalErrorStatus(""); //$NON-NLS-1$
		
		if (cu.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(""); //$NON-NLS-1$
		
		if (mustCancelRenamingType())
			fWillRenameType= false;
		
		if (fWillRenameType)
			return fRenameTypeRefactoring.checkPreactivation();
		else	
			return new RefactoringStatus();;
	}
	
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		if (mustCancelRenamingType()){
			Assert.isTrue(! fWillRenameType);
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", getCompilationUnit().getElementName())); //$NON-NLS-1$
		}	
		 
		// we purposely do not check activation of the renameTypeRefactoring here. 
		return new RefactoringStatus();
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fWillRenameType && (!getCompilationUnit().isStructureKnown())){
				RefactoringStatus result1= new RefactoringStatus();
				
				RefactoringStatus result2= new RefactoringStatus();
				result2.merge(Checks.checkCompilationUnitNewName(getCompilationUnit(), fNewName));
				if (result2.hasFatalError())
					result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed_1", getCompilationUnit().getElementName())); //$NON-NLS-1$
				else 
					result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", getCompilationUnit().getElementName())); //$NON-NLS-1$
				result1.merge(result2);			
			}	
		
			if (fWillRenameType)
				return fRenameTypeRefactoring.checkInput(pm);
			else
				return Checks.checkCompilationUnitNewName(getCompilationUnit(), removeFileNameExtension(fNewName));
		} finally{
			pm.done();
		}		
	}
	
	private void computeRenameTypeRefactoring(){
		if (getSimpleCUName().indexOf(".") != -1){ //$NON-NLS-1$
			fRenameTypeRefactoring= null;
			fWillRenameType= false;
			return;
		}
		IType type= getCompilationUnit().getType(getSimpleCUName());
		if (type.exists())
			fRenameTypeRefactoring= new RenameTypeRefactoring(getTextBufferChangeCreator(), type);
		else
			fRenameTypeRefactoring= null;
		fWillRenameType= (fRenameTypeRefactoring != null);	
	}
	
	private boolean mustCancelRenamingType() throws JavaModelException {
		return (fRenameTypeRefactoring != null) && (! getCompilationUnit().isStructureKnown());
	}
	
	//--- changes
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		//renaming the file is taken care of in renameTypeRefactoring
		if (fWillRenameType)
			return fRenameTypeRefactoring.createChange(pm);
	
		return new RenameCompilationUnitChange(getCompilationUnit(), fNewName);
	}
}
