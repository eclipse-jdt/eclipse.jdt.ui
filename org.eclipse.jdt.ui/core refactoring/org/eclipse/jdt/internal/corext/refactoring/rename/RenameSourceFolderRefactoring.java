package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;


public class RenameSourceFolderRefactoring	extends Refactoring implements IRenameRefactoring {

	private IPackageFragmentRoot fSourceFolder;
	private String fNewName;
	
	public RenameSourceFolderRefactoring(IPackageFragmentRoot sourceFolder){
		Assert.isNotNull(sourceFolder); 
		fSourceFolder= sourceFolder;
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename Source Folder:" + fSourceFolder + " to:" + fNewName;
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
		return fSourceFolder.getElementName();
	}
			
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		if (! fSourceFolder.exists())
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (fSourceFolder.isArchive())
			return RefactoringStatus.createFatalErrorStatus("");
		
		if (fSourceFolder.isExternal())	
			return RefactoringStatus.createFatalErrorStatus("");
			
		if (fSourceFolder.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("");
			
		if (! fSourceFolder.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus("");	
		
		if (! fSourceFolder.isConsistent())	
			return RefactoringStatus.createFatalErrorStatus("");	
		
		if (fSourceFolder.getUnderlyingResource() instanceof IProject)
			return RefactoringStatus.createFatalErrorStatus("");

		return new RefactoringStatus();
	}

	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		if (! newName.trim().equals(newName))
			return RefactoringStatus.createFatalErrorStatus("Name must not start or end with a blank.");
		
		IContainer c= 	fSourceFolder.getCorrespondingResource().getParent();
		if (! c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus("This is an invalid name for a file or folder.");
		
		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, IResource.FOLDER));
		if (result.hasFatalError())
			return result;		
				
		result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), IResource.FOLDER)));		
		if (result.hasFatalError())
			return result;
			
		IJavaProject project= fSourceFolder.getJavaProject();
		IPath p= project.getProject().getFullPath().append(newName);
		if (project.findPackageFragmentRoot(p) != null)
			return RefactoringStatus.createFatalErrorStatus("The package or folder already exists");
					
		return result;		
	}
	
	private String createNewPath(String newName) throws JavaModelException {
		return fSourceFolder.getCorrespondingResource().getFullPath().removeLastSegments(1).append(newName).toString();
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (isReadOnly())
				return RefactoringStatus.createErrorStatus("Source folder " + fSourceFolder.getElementName() + " is marked as read-only.");
			return new RefactoringStatus();
		} finally{
			pm.done();
		}		
	}
	
	private boolean isReadOnly() throws JavaModelException{
		if (Checks.isClasspathDelete(fSourceFolder))
			return false;
		return fSourceFolder.getCorrespondingResource().isReadOnly();
	}
	
	//-- changes

	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			return new RenameSourceFolderChange(fSourceFolder, fNewName);
		} finally{
			pm.done();
		}	
	}
}

