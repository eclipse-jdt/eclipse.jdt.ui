/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.cus;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.RenameResourceChange;

public class RenameCompilationUnitRefactoring extends CompilationUnitRefactoring implements IRenameRefactoring, IPreactivatedRefactoring{

	private String fNewName;
	private RenameTypeRefactoring fRenameTypeRefactoring;
	private boolean fWillRenameType;
	
	public RenameCompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit cu){
		super(changeCreator, cu);
		computeRenameTypeRefactoring();
	}
		
	private void computeRenameTypeRefactoring(){
		IType type= getCu().getType(getSimpleCUName());
		if (type.exists())
			fRenameTypeRefactoring= new RenameTypeRefactoring(getTextBufferChangeCreator(), type);
		else
			fRenameTypeRefactoring= null;
		fWillRenameType= (fRenameTypeRefactoring != null);	
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
			result.addFatalError("The same name chosen");	
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
		return "Rename \"" + getCu().getElementName() + "\" to \"" + fNewName + "\"";
	}

	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		ICompilationUnit cu= getCu();
		if (! cu.exists())
			result.addFatalError(cu.getElementName() + " does not exist in the model");
		
		if (cu.isReadOnly())
			result.addFatalError(cu.getElementName() + " is read only");	
		
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
			result.addError(getCu().getElementName() + " cannot be parsed correctly. No references will be updated if you proceed");
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
				result1.addError(getCu().getElementName() + " cannot not be parsed correctly.");
			else 
				result1.addError(getCu().getElementName() + " cannot not be parsed correctly. No references will be updated if you proceed");
			result1.merge(result2);			
		}	
		
		if (fWillRenameType)
			return fRenameTypeRefactoring.checkInput(pm);
		else{
			RefactoringStatus result= new RefactoringStatus();
			result.merge(Checks.checkCompilationUnitNewName(getCu(), fNewName));
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
		composite.addChange(new RenameResourceChange(getResource(getCu()), removeFileNameExtension(fNewName)));
		return composite;	
	}
}
