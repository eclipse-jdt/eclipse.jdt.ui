/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;


public class RenameResourceRefactoring extends Refactoring implements IRenameRefactoring {

	private IResource fResource;
	private String fNewName;
	
	public RenameResourceRefactoring(IResource resource){
		Assert.isNotNull(resource); 
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
	 * @see IRenameRefactoring#getNewName()
	*/
	public String getNewName(){
		return fNewName;
	}

	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fResource.getName();
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
				
		return new RefactoringStatus();
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		IContainer c= fResource.getParent();
		if (c == null)
			return RefactoringStatus.createFatalErrorStatus("Internal Error");
						
		if (c.findMember(newName) != null)
			return RefactoringStatus.createFatalErrorStatus("A file or folder with this name already exists.");
			
		if (!c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus("This is an invalid name for a file or folder.");
	
		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, fResource.getType()));
		if (! result.hasFatalError())
			result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), fResource.getType())));		
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

	private String createNewPath(String newName){
		return fResource.getFullPath().removeLastSegments(1).append(newName).toString();
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

