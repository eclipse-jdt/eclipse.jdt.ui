package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class RenameSourceFolderChange extends AbstractJavaElementRenameChange {

	public RenameSourceFolderChange(IPackageFragmentRoot sourceFolder, String newName) throws JavaModelException {
		this(sourceFolder.getPath(), sourceFolder.getElementName(), newName);
		Assert.isTrue(!sourceFolder.isReadOnly(), RefactoringCoreMessages.getString("RenameSourceFolderChange.assert.readonly"));  //$NON-NLS-1$
		Assert.isTrue(!sourceFolder.isArchive(), RefactoringCoreMessages.getString("RenameSourceFolderChange.assert.archive"));  //$NON-NLS-1$
	}
	
	private RenameSourceFolderChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
	
	private IPath createNewPath(){
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	/* non java-doc
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected IChange createUndoChange() {
		return new RenameSourceFolderChange(createNewPath(), getNewName(), getOldName());
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameSourceFolderChange.rename", //$NON-NLS-1$
			new String[]{getOldName(), getNewName()});
	}

	/* non java-doc
	 * @see AbstractRenameChange#doRename
	 */	
	protected void doRename(IProgressMonitor pm) throws Exception {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("RenameSourceFolderChange.renaming"), 2); //$NON-NLS-1$
			modifyClassPath(new SubProgressMonitor(pm, 1));
			IPath path= getResource().getFullPath().removeLastSegments(1).append(getNewName());
			getResource().move(path, false, new SubProgressMonitor(pm, 1));
		} finally{
			pm.done();
		}	
	}
	
	private void modifyClassPath(IProgressMonitor pm) throws JavaModelException{
		IClasspathEntry[] oldEntries= getJavaProject().getRawClasspath();
		IClasspathEntry[] newEntries= new IClasspathEntry[oldEntries.length];
		for (int i= 0; i < newEntries.length; i++) {
			if (isOurEntry(oldEntries[i]))
				newEntries[i]= createModifiedEntry();
			else
				newEntries[i]= oldEntries[i];	
		}
		getJavaProject().setRawClasspath(newEntries, pm);
	}
	
	private boolean isOurEntry(IClasspathEntry cpe){
		if (cpe.getEntryKind() != IClasspathEntry.CPE_SOURCE)
			return false;
		if (! cpe.getPath().equals(getResourcePath()))
			return false;
		return true;	
	}
	
	private IClasspathEntry createModifiedEntry(){
		IPath path= getJavaProject().getProject().getFullPath().append(getNewName());
		return JavaCore.newSourceEntry(path);
	}
	
	private IJavaProject getJavaProject(){
		return ((IPackageFragmentRoot)getModifiedLanguageElement()).getJavaProject();
	}

	/* non java-doc
	 * @see IChange#aboutToPerform(ChangeContext, IProgressMonitor)
	 */	
	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= super.aboutToPerform(context, pm);

		if (context.getUnsavedFiles().length == 0)
			return result;
		
		result.merge(checkIfModifiable((IPackageFragmentRoot)getModifiedLanguageElement(), context, pm));
		
		return result;
	}
}

