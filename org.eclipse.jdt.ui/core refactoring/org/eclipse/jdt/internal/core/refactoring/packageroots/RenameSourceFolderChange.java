package org.eclipse.jdt.internal.core.refactoring.packageroots;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.AbstractRenameChange;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

public class RenameSourceFolderChange extends AbstractRenameChange {

	protected RenameSourceFolderChange(IPackageFragmentRoot sourceFolder, String newName) throws JavaModelException {
		this(sourceFolder.getCorrespondingResource().getFullPath(), sourceFolder.getElementName(), newName);
		Assert.isTrue(!sourceFolder.isReadOnly(), "should not be read-only"); 
		Assert.isTrue(!sourceFolder.isArchive(), "should not be an archive"); 
	}
	
	private RenameSourceFolderChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
	
	private IPath createNewPath(){
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	/**
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected IChange createUndoChange() {
		return new RenameSourceFolderChange(createNewPath(), getNewName(), getOldName());
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Rename Source Folder " + getOldName() + " to:" + getNewName();
	}
	
	protected void doRename(IProgressMonitor pm) throws Exception {
		IPackageFragmentRoot root= (IPackageFragmentRoot)getCorrespondingJavaElement();
		IResource res= (IResource)root.getCorrespondingResource();
		IPath path= res.getFullPath().removeLastSegments(1).append(getNewName());
		res.move(path, true, pm);
		JavaCore.create(res.getWorkspace().getRoot().getFolder(path));
	}
	
	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= super.aboutToPerform(context, pm);

		if (context.getUnsavedFiles().length == 0)
			return result;
		
		result.merge(checkIfUnsaved((IPackageFragmentRoot)getCorrespondingJavaElement(), context, pm));
		
		return result;
	}
}

