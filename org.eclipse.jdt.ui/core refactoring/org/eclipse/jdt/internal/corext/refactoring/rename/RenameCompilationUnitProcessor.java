/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;


public class RenameCompilationUnitProcessor extends JavaRenameProcessor implements IReferenceUpdating, ITextUpdating, IQualifiedNameUpdating {

	private RenameTypeProcessor fRenameTypeProcessor;
	private boolean fWillRenameType;
	private ICompilationUnit fCu;
	
	private static final String IDENTIFIER= "org.eclipse.jdt.ui.renameCompilationUnitProcessor"; //$NON-NLS-1$
	
	//---- IRefactoringProcessor --------------------------------
	
	public RenameCompilationUnitProcessor(ICompilationUnit unit) throws CoreException {
		fCu= unit;
		if (fCu != null) {
			computeRenameTypeRefactoring();
			setNewElementName(fCu.getElementName());
		}
	}

	public void initialize(Object[] elements) throws CoreException {
		Assert.isTrue(elements != null && elements.length == 1);
		Object element= elements[0];
		if (element instanceof IFile) {
			IJavaElement jElement= JavaCore.create((IFile)element);
			if (jElement != null && jElement.exists() && jElement.getElementType() == IJavaElement.COMPILATION_UNIT)
				fCu= (ICompilationUnit)jElement;
		} else if (element instanceof ICompilationUnit) {
			fCu= (ICompilationUnit)element;
		}
		if (fCu != null) {
			computeRenameTypeRefactoring();
			setNewElementName(fCu.getElementName());
		}
	}
	
	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() {
		if (fCu == null)
			return false;
		if (! fCu.exists())
			return false;
		if (!JavaModelUtil.isPrimary(fCu))
			return false; //needs to be fed with a real cu
		if (fCu.isReadOnly())
			return false;
		return true;
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString(
			"RenameCompilationUnitRefactoring.name",  //$NON-NLS-1$
			new String[]{fCu.getElementName(), getNewElementName()});
	}

	protected IProject[] getAffectedProjects() throws CoreException {
		return JavaProcessors.computeScope(fCu);
	}

	public Object[] getElements() {
		return new Object[] {fCu};
	}

	public RefactoringParticipant[] getSecondaryParticipants() throws CoreException {
		String newTypeName= removeFileNameExtension(getNewElementName());
		RenameArguments arguments= new RenameArguments(newTypeName, getUpdateReferences());
		return createSecondaryParticipants(computeDerivedElements(), arguments, computeResourceModifications());
	}
	
	private Object[] computeDerivedElements() {
		if (fRenameTypeProcessor == null)
			return new Object[0];
		return fRenameTypeProcessor.getElements();
	}
	
	private ResourceModifications computeResourceModifications() {
		IResource resource= fCu.getResource();
		if (resource == null)
			return null;
		ResourceModifications result= new ResourceModifications();
		result.setRename(resource, getArguments());
		return result;
	}
	
	//---- IRenameProcessor -------------------------------------
	
	public String getCurrentElementName() {
		return getSimpleCUName();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		String typeName= removeFileNameExtension(newName);
		RefactoringStatus result= Checks.checkCompilationUnitName(newName);
		if (fWillRenameType)
			result.merge(fRenameTypeProcessor.checkNewElementName(typeName));
		if (Checks.isAlreadyNamed(fCu, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameCompilationUnitRefactoring.same_name"));	 //$NON-NLS-1$
		return result;
	}
	
	public void setNewElementName(String newName) {
		super.setNewElementName(newName);
		if (fWillRenameType)
			fRenameTypeProcessor.setNewElementName(removeFileNameExtension(newName));
	}
	
	public Object getNewElement() {
		IJavaElement parent= fCu.getParent();
		if (parent.getElementType() != IJavaElement.PACKAGE_FRAGMENT)
			return fCu; //??
		IPackageFragment pack= (IPackageFragment)parent;
		if (JavaConventions.validateCompilationUnitName(getNewElementName()).getSeverity() == IStatus.ERROR)
			return fCu; //??
		return pack.getCompilationUnit(getNewElementName());
	}
	
	//---- ITextUpdating ---------------------------------------------
	
	public boolean canEnableTextUpdating() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.canEnableUpdateReferences();
	}

	public boolean getUpdateTextualMatches() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateTextualMatches();
	}

	public void setUpdateTextualMatches(boolean update) {
		if (fRenameTypeProcessor != null)
			fRenameTypeProcessor.setUpdateTextualMatches(update);
	}
	
	//---- IReferenceUpdating -----------------------------------

	public boolean canEnableUpdateReferences() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.canEnableUpdateReferences();
	}

	public void setUpdateReferences(boolean update) {
		if (fRenameTypeProcessor != null)
			fRenameTypeProcessor.setUpdateReferences(update);
	}

	public boolean getUpdateReferences(){
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateReferences();		
	}
	
	//---- IQualifiedNameUpdating -------------------------------

	public boolean canEnableQualifiedNameUpdating() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.canEnableQualifiedNameUpdating();
	}
	
	public boolean getUpdateQualifiedNames() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateQualifiedNames();
	}
	
	public void setUpdateQualifiedNames(boolean update) {
		if (fRenameTypeProcessor == null)
			return;
		fRenameTypeProcessor.setUpdateQualifiedNames(update);
	}
	
	public String getFilePatterns() {
		if (fRenameTypeProcessor == null)
			return null;
		return fRenameTypeProcessor.getFilePatterns();
	}
	
	public void setFilePatterns(String patterns) {
		if (fRenameTypeProcessor == null)
			return;
		fRenameTypeProcessor.setFilePatterns(patterns);
	}
	
	//--- preconditions ----------------------------------
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		if (fRenameTypeProcessor != null && ! fCu.isStructureKnown()){
			fRenameTypeProcessor= null;
			fWillRenameType= false;
			return new RefactoringStatus();
		}
		
		//for a test case what it's needed, see bug 24248 
		//(the type might be gone from the editor by now)
		if (fWillRenameType && fRenameTypeProcessor != null && ! fRenameTypeProcessor.getType().exists()){
			fRenameTypeProcessor= null;
			fWillRenameType= false;
			return new RefactoringStatus();
		}
		 
		// we purposely do not check activation of the renameTypeRefactoring here. 
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			if (fWillRenameType && (!fCu.isStructureKnown())){
				RefactoringStatus result1= new RefactoringStatus();
				
				RefactoringStatus result2= new RefactoringStatus();
				result2.merge(Checks.checkCompilationUnitNewName(fCu, getNewElementName()));
				if (result2.hasFatalError())
					result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed_1", fCu.getElementName())); //$NON-NLS-1$
				else 
					result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", fCu.getElementName())); //$NON-NLS-1$
				result1.merge(result2);			
			}	
		
			if (fWillRenameType)
				return fRenameTypeProcessor.checkFinalConditions(pm, context);
			else
				return Checks.checkCompilationUnitNewName(fCu, getNewElementName());
		} finally{
			pm.done();
		}		
	}
	
	private void computeRenameTypeRefactoring() throws CoreException{
		if (getSimpleCUName().indexOf(".") != -1) { //$NON-NLS-1$
			fRenameTypeProcessor= null;
			fWillRenameType= false;
			return;
		}
		IType type= getTypeWithTheSameName();
		if (type != null) {
			fRenameTypeProcessor= new RenameTypeProcessor(type);
		} else {
			fRenameTypeProcessor= null;
		}
		fWillRenameType= fRenameTypeProcessor != null && fCu.isStructureKnown();
	}

	private IType getTypeWithTheSameName() {
		try {
			IType[] topLevelTypes= fCu.getTypes();
			String name= getSimpleCUName();
			for (int i = 0; i < topLevelTypes.length; i++) {
				if (name.equals(topLevelTypes[i].getElementName()))
					return topLevelTypes[i];
			}
			return null; 
		} catch (CoreException e) {
			return null;
		}
	}
	
	private String getSimpleCUName() {
		return removeFileNameExtension(fCu.getElementName());
	}
	
	/**
	 * Removes the extension (whatever comes after the last '.') from the given file name.
	 */
	private static String removeFileNameExtension(String fileName) {
		if (fileName.lastIndexOf(".") == -1) //$NON-NLS-1$
			return fileName;
		return fileName.substring(0, fileName.lastIndexOf(".")); //$NON-NLS-1$
	}
	
	//--- changes
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		//renaming the file is taken care of in renameTypeRefactoring
		if (fWillRenameType)
			return fRenameTypeProcessor.createChange(pm);
		
		IResource resource= ResourceUtil.getResource(fCu);
		if (resource != null && resource.isLinked())
			return new ValidationStateChange( 
			   new RenameResourceChange(resource, getNewElementName()));
		
		return new ValidationStateChange( 
			new RenameCompilationUnitChange(fCu, getNewElementName()));
	}
}
