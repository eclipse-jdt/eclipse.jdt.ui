package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.RenameJavaProjectChange;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.JavaPlugin;


public class RenameJavaProjectRefactoring extends Refactoring implements IRenameRefactoring {

	private IJavaProject fProject;
	private String fNewName;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameJavaProjectRefactoring(ITextBufferChangeCreator changeCreator, IJavaProject project){
		Assert.isNotNull(project); 
		Assert.isNotNull(changeCreator);
		fTextBufferChangeCreator= changeCreator;		
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
		return "Rename Java project " + getCurrentName() + " to:" + fNewName;
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
			return new RenameJavaProjectChange(fProject, fNewName);
		} finally{
			pm.done();
		}	
	}
}