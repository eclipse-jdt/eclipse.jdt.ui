package org.eclipse.jdt.internal.core.refactoring.projects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

public class RenameResourceRefactoring extends Refactoring implements IRenameRefactoring {

	private IResource fResource;
	private String fNewName;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameResourceRefactoring(ITextBufferChangeCreator changeCreator, IResource resource){
		Assert.isNotNull(resource, "resource"); 
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;		
		fResource= resource;
	}
	
	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	/**
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

	/**
	 * @see IRenameRefactoring#setNewName(String)
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/**
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fResource.getName();
	}

	/**
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
	
	private String createNewPath(){
		return fResource.getFullPath().removeLastSegments(1).append(fNewName).toString();
	}
	
	private static RefactoringStatus validateStatus(IStatus status){
		RefactoringStatus result= new RefactoringStatus();
		if (! status.isOK()){
			switch (status.getSeverity()){
				case IStatus.INFO:
					result.addWarning(status.getMessage());
					break;
				case IStatus.WARNING:
					result.addError(status.getMessage());
					break;
				case IStatus.ERROR:
					return RefactoringStatus.createFatalErrorStatus(status.getMessage());
			}
		}	
		return result;
	}
	
	/**
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

	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename resource " +  getCurrentName() + " to:" + fNewName;
	}
}

