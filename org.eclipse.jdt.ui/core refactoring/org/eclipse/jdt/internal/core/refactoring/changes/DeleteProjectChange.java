package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

class DeleteProjectChange extends DeleteChange {
	
	private IProject fProject;
	private boolean fDeleteContents;
	
	DeleteProjectChange(IProject project, boolean deleteProjectContents){
		fProject= project;
		fDeleteContents= deleteProjectContents;
	}
		
	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Delete project";
	}

	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return JavaCore.create(fProject);
	}
	
	/**
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws CoreException{
		fProject.delete(fDeleteContents, true, pm);
	}
}

