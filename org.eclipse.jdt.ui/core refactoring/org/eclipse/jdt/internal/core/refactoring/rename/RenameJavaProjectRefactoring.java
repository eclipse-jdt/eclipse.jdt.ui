package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.AddToClasspathChange;
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteFromClasspathChange;
import org.eclipse.jdt.internal.core.refactoring.changes.RenameJavaProjectChange;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;


public class RenameJavaProjectRefactoring extends Refactoring implements IRenameRefactoring {

	private IJavaProject fProject;
	private String fNewName;
	
	public RenameJavaProjectRefactoring(IJavaProject project){
		Assert.isNotNull(project); 
		fProject= project;
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
		return fProject.getElementName();
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename Java project '" + getCurrentName() + "' to:'" + fNewName + "'";
	}
		
	//-- preconditions
	
	/* non java-doc
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
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		IStatus status= JavaPlugin.getWorkspace().validateName(fNewName, IResource.PROJECT);
		if (!status.isOK()){
			if (status.isMultiStatus())
				return RefactoringStatus.createFatalErrorStatus("It is an invalid name for a project.");
			return RefactoringStatus.createFatalErrorStatus(status.getMessage());
		}
		
		if (projectNameAlreadyExists())
			return RefactoringStatus.createFatalErrorStatus("A project with that name already exists.");
		
		return new RefactoringStatus();
	}
	
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (isReadOnly())
				return RefactoringStatus.createErrorStatus("Project " + fProject.getElementName() + " is marked as read-only.");
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	private boolean isReadOnly() throws JavaModelException{
		return fProject.getCorrespondingResource().isReadOnly();
	}
	
	private boolean projectNameAlreadyExists(){
		return fProject.getJavaModel().getJavaProject(fNewName).exists();
	}

	//--- changes 
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			CompositeChange composite= new CompositeChange("Renaming a Java Project");
			addNewProjectToClasspaths(composite);
			composite.addChange(new RenameJavaProjectChange(fProject, fNewName));
			removeOldProjectFromClasspaths(composite);
			return composite;
		} finally{
			pm.done();
		}	
	}

	private void addNewProjectToClasspaths(CompositeChange composite) {
		IProject[] referencing=getReferencingProjects();
		IPath newProjectPath= createNewProjectPath();
		for (int i= 0; i < referencing.length; i++) {
			IProject project= referencing[i];
			IJavaProject jp= JavaCore.create(project);
			if (jp != null)
				composite.addChange(new AddToClasspathChange(jp, newProjectPath));
		}
	}
	
	private void removeOldProjectFromClasspaths(CompositeChange composite) {
		IProject[] referencing=getReferencingProjects();
		IPath oldProjectPath= fProject.getProject().getFullPath();
		for (int i= 0; i < referencing.length; i++) {
			IProject project= referencing[i];
			IJavaProject jp= JavaCore.create(project);
			if (jp != null)
				composite.addChange(new DeleteFromClasspathChange(oldProjectPath, jp));
		}
	}
	
	private IProject[] getReferencingProjects() {
		return  fProject.getProject().getReferencingProjects();
	}
	
	private IPath createNewProjectPath(){
		return fProject.getProject().getFullPath().removeLastSegments(1).append(fNewName);
	}
}