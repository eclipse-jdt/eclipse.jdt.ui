/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;


public class RenameResourceRefactoring extends Refactoring implements IRenameRefactoring {

	private IResource fResource;
	private String fNewName;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameResourceRefactoring(ITextBufferChangeCreator changeCreator, IResource resource){
		Assert.isNotNull(resource); 
		Assert.isNotNull(changeCreator);
		fTextBufferChangeCreator= changeCreator;		
		fResource= resource;
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename resource " +  getCurrentName() + " to:" + fNewName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#setNewName(String)
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fResource.getName();
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return false;
	}

	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean update) {
	}	
	
	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		return false;
	}
	
	//--- preconditions 
	
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		if (! fResource.exists())
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (! fResource.isAccessible())	
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (fResource.isReadOnly())	
			return RefactoringStatus.createFatalErrorStatus("");	
		
		return new RefactoringStatus();
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		IContainer c= fResource.getParent();
		if (c == null)
			return RefactoringStatus.createFatalErrorStatus("Internal Error");
						
		if (c.findMember(fNewName) != null)
			return RefactoringStatus.createFatalErrorStatus("A file or folder with this name already exists.");
			
		if (!c.getFullPath().isValidSegment(fNewName))
			return RefactoringStatus.createFatalErrorStatus("This is an invalid name for a file or folder.");
	
		RefactoringStatus result= new RefactoringStatus();
		result.merge(validateStatus(c.getWorkspace().validateName(fNewName, IResource.FOLDER)));
		if (! result.hasFatalError())
			result.merge(validateStatus(c.getWorkspace().validatePath(createNewPath(), IResource.FOLDER)));		
		return result;		
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (fResource.isReadOnly())
				return RefactoringStatus.createErrorStatus("Resource " + fResource.getName() + " is marked as read-only.");
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	private String createNewPath(){
		return fResource.getFullPath().removeLastSegments(1).append(fNewName).toString();
	}
	
	private static RefactoringStatus validateStatus(IStatus status){
		if (status.isOK())
			return null;
		
		switch (status.getSeverity()){
			case IStatus.INFO:
				return RefactoringStatus.createWarningStatus(status.getMessage());
			case IStatus.WARNING:
				return RefactoringStatus.createErrorStatus(status.getMessage());
			case IStatus.ERROR:
				return RefactoringStatus.createFatalErrorStatus(status.getMessage());
			default:	
				return null;
		}
	}
	
	//--- changes 
	
	/* non java-doc 
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			return new RenameResourceChange(fResource, fNewName);
		} finally{
			pm.done();
		}	
	}


}

