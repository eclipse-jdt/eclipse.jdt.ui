package org.eclipse.jdt.internal.core.refactoring.packageroots;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.packageroots.*;


public class RenameSourceFolderRefactoring	extends Refactoring implements IRenameRefactoring {

	private IPackageFragmentRoot fSourceFolder;
	private String fNewName;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameSourceFolderRefactoring(ITextBufferChangeCreator changeCreator, IPackageFragmentRoot sourceFolder){
		Assert.isNotNull(sourceFolder, "source folder"); //$NON-NLS-1$
		Assert.isNotNull(changeCreator, "change creator"); //$NON-NLS-1$
		fTextBufferChangeCreator= changeCreator;		
		fSourceFolder= sourceFolder;
	}
	
	/**
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
		return fSourceFolder.getElementName();
	}

	/**
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		IJavaProject project= fSourceFolder.getJavaProject();
		IPath p= project.getProject().getFullPath().append(fNewName);
		try {
			if (project.findPackageFragmentRoot(p) != null)
				return RefactoringStatus.createFatalErrorStatus("The package or folder already exists");
		} catch (JavaModelException e) {
			return RefactoringStatus.createFatalErrorStatus("Exception occurred");
		}
		return new RefactoringStatus();
	}

	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename Source Folder:" + fSourceFolder + " to:" + fNewName;
	}
	
	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			RefactoringStatus result= new RefactoringStatus();
			if (isReadOnly())
				result.addError("Source folder " + fSourceFolder.getElementName() + " is marked as read-only.");
			return result;	
		} finally{
			pm.done();
		}		
	}

	/**
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
	
	private boolean isReadOnly() throws JavaModelException{
		if (Checks.isClasspathDelete(fSourceFolder))
			return false;
		return fSourceFolder.getCorrespondingResource().isReadOnly();
	}
}

