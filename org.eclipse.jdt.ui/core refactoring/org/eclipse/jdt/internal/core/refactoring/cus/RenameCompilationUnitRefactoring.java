/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.cus;

import java.util.List;import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.cus.RenameCompilationUnitChange;import org.eclipse.jdt.internal.core.refactoring.packages.RenamePackageChange;import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.RenameResourceChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

public class RenameCompilationUnitRefactoring extends CompilationUnitRefactoring implements IRenameRefactoring, IPreactivatedRefactoring{

	private String fNewName;
	private RenameTypeRefactoring fRenameTypeRefactoring;
	private boolean fWillRenameType;
	
	public RenameCompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit cu){
		super(changeCreator, cu);
		computeRenameTypeRefactoring();
	}
		
	private void computeRenameTypeRefactoring(){
		//fix for:1GF5ZBA: ITPJUI:WINNT - assertion failed after rightclick on a compilation unit with strange name
		if (getSimpleCUName().indexOf(".") != -1){ //$NON-NLS-1$
			fRenameTypeRefactoring= null;
			fWillRenameType= false;
			return;
		}
		IType type= getCu().getType(getSimpleCUName());
		if (type.exists())
			fRenameTypeRefactoring= new RenameTypeRefactoring(getTextBufferChangeCreator(), type);
		else
			fRenameTypeRefactoring= null;
		fWillRenameType= (fRenameTypeRefactoring != null);	
	}

	/* non javadoc
	 * see Refactoring#setUnsavedFileList
	 */
	public void setUnsavedFileList(List list){
		super.setUnsavedFileList(list);
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUnsavedFileList(list);
	}
		
	/**
	 * @see IRenameRefactoring#setNewName(String)
	 * @param newName 'java' must be included
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
		if (fWillRenameType)
			fRenameTypeRefactoring.setNewName(removeFileNameExtension(newName));
	}

	/**
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkCompilationUnitName(fNewName));
		if (fWillRenameType)
			result.merge(fRenameTypeRefactoring.checkNewName());
		if (Checks.isAlreadyNamed(getCu(), fNewName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameCompilationUnitRefactoring.same_name"));	 //$NON-NLS-1$
		return result;
	}
	
	/**
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return getCu().getElementName();
	}
	
	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.name",  //$NON-NLS-1$
															new String[]{getCu().getElementName(), fNewName});
	}

	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		ICompilationUnit cu= getCu();
		if (! cu.exists())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_in_model", cu.getElementName())); //$NON-NLS-1$
		
		if (cu.isReadOnly())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.read_only", cu.getElementName()));	 //$NON-NLS-1$
		
		if (mustCancelRenamingType())
			fWillRenameType= false;
		
		if (fWillRenameType)
			result.merge(fRenameTypeRefactoring.checkPreactivation());
			
		return result;
	}
	
	/**
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		
		if (mustCancelRenamingType()){
			Assert.isTrue(! fWillRenameType);
			result.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", getCu().getElementName())); //$NON-NLS-1$
		}	
		 
		// we purposely do not check activation of the renameTypeRefactoring here. 
		return result;
	}
	
	private boolean mustCancelRenamingType() throws JavaModelException {
		return (fRenameTypeRefactoring != null) && (! getCu().isStructureKnown());
	}

	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		
		if (fWillRenameType && (!getCu().isStructureKnown())){
			RefactoringStatus result1= new RefactoringStatus();
			
			RefactoringStatus result2= new RefactoringStatus();
			result2.merge(Checks.checkCompilationUnitNewName(getCu(), fNewName));
			if (result2.hasFatalError())
				result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed_1", getCu().getElementName())); //$NON-NLS-1$
			else 
				result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", getCu().getElementName())); //$NON-NLS-1$
			result1.merge(result2);			
		}	
		
		if (fWillRenameType)
			return fRenameTypeRefactoring.checkInput(pm);
		else{
			RefactoringStatus result= new RefactoringStatus();
			result.merge(Checks.checkCompilationUnitNewName(getCu(), removeFileNameExtension(fNewName)));
			return result;
		}
	}
	
	/**
	 * 
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		//renaming the file is taken care of in renameTypeRefactoring
		if (fWillRenameType)
			return fRenameTypeRefactoring.createChange(pm);
	
		CompositeChange composite= new CompositeChange();
		composite.addChange(new RenameCompilationUnitChange(getCu(), fNewName));
		return composite;	
	}
}
