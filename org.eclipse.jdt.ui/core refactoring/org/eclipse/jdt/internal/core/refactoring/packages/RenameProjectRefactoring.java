package org.eclipse.jdt.internal.core.refactoring.packages;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class RenameProjectRefactoring extends Refactoring implements IRenameRefactoring {

	private IJavaProject fProject;
	private String fNewName;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameProjectRefactoring(ITextBufferChangeCreator changeCreator, IJavaProject project){
		Assert.isNotNull(project, "source folder"); 
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;		
		fProject= project;
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
		if (! fProject.exists())
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (fProject.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (! fProject.isConsistent())
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (! fProject.isStructureKnown())
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
		return fProject.getElementName();
	}

	/**
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		IStatus status= JavaPlugin.getWorkspace().validateName(fNewName, IResource.PROJECT);
		if (!status.isOK()){
			if (status.isMultiStatus())
				return RefactoringStatus.createFatalErrorStatus("It is an invalid name for a project.");
			return 
				RefactoringStatus.createFatalErrorStatus(status.getMessage());
		}
		
		if (projectNameAlreadyExists())
			return RefactoringStatus.createFatalErrorStatus("A project with that name already exists.");
		
		return new RefactoringStatus();
	}
	
	private boolean projectNameAlreadyExists(){
		return fProject.getJavaModel().getJavaProject(fNewName).exists();
	}

	/**
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			CompositeChange builder= new CompositeChange();
			builder.addChange(new RenameProjectChange(fProject, fNewName));
			return builder;
		} finally{
			pm.done();
		}	
	}

	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename project " + getCurrentName() + " to:" + fNewName;
	}

}

