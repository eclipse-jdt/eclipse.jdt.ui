package org.eclipse.jdt.internal.core.refactoring.packages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.AbstractRenameChange;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

public class RenameProjectChange extends AbstractRenameChange {

	protected RenameProjectChange(IJavaProject project, String newName) throws JavaModelException {
		this(project.getCorrespondingResource().getFullPath(), project.getElementName(), newName);
		Assert.isTrue(!project.isReadOnly(), "should not be read-only"); 
	}
	
	protected RenameProjectChange(IPath resourcePath, String oldName, String newName) {
		super(resourcePath, oldName, newName);
	}

	/**
	 * @see AbstractRenameChange#doRename(IProgressMonitor)
	 */
	protected void doRename(IProgressMonitor pm) throws Exception {
		IJavaProject p= (IJavaProject)getCorrespondingJavaElement();
		IProject project= p.getProject();
		IContainer parent= project.getParent();
		IPath newPath= parent.getFullPath().append(getNewName());
		p.getProject().move(newPath, true, pm);
	}
	
	private IPath createNewPath(){
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	/**
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected IChange createUndoChange() throws JavaModelException {
		return new RenameProjectChange(createNewPath(), getNewName(), getOldName());
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Rename Java Project " + getOldName() + " to:" + getNewName();
	}

	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		RefactoringStatus result= super.aboutToPerform(context, pm);

		if (context.getUnsavedFiles().length == 0)
			return result;
			
		IJavaProject project= (IJavaProject)getCorrespondingJavaElement();
		if (project.exists()) {
			try {
				IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
				if (roots.length == 0)
					return result;
					
				pm.beginTask("", roots.length); //$NON-NLS-1$
				for (int i= 0; i < roots.length; i++) {
					result.merge(RenameSourceFolderChange.checkPackageRoot(roots[i], context, new SubProgressMonitor(pm, 1)));
				}	
				pm.done();
			} catch (JavaModelException e) {
				handleJavaModelException(e, result);
			}
		}
		return result;
	}
}

