package org.eclipse.jdt.internal.core.refactoring.packages;

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
		
		result.merge(checkPackageRoot((IPackageFragmentRoot)getCorrespondingJavaElement(), context, pm));
		
		return result;
	}
	
	/*package*/ static RefactoringStatus checkPackageRoot(IPackageFragmentRoot root, ChangeContext context, IProgressMonitor pm){
		if (root == null)
			return null;
		
		if (! root.exists())
			return null;
		
		if (root.isArchive())
			return null;	
		
		if (root.isExternal())
			return null;
		
		RefactoringStatus result= new RefactoringStatus();
				
		try {
			IJavaElement[] packs= root.getChildren();
			if (packs == null || packs.length == 0)
				return null;
			
			pm.beginTask("", packs.length); //$NON-NLS-1$
			for (int i= 0; i < packs.length; i++) {
				result.merge(checkPackage((IPackageFragment)packs[i], context, new SubProgressMonitor(pm, 1)));
			}	
			pm.done();
		} catch (JavaModelException e) {
			handleJavaModelException(e, result);
		}
		return result;
	}
	
	/*package*/ static RefactoringStatus checkPackage(IPackageFragment pack, ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		ICompilationUnit[] units= pack.getCompilationUnits();
		if (units == null || units.length == 0)
			return null;
		
		RefactoringStatus result= new RefactoringStatus();
		
		pm.beginTask("", units.length); //$NON-NLS-1$
		for (int i= 0; i < units.length; i++) {
			pm.subTask("Checking change for:" + pack.getElementName());
			checkIfResourceIsUnsaved(units[i], result, context);
			pm.worked(1);
		}
		pm.done();		
		return result;
	}
}

