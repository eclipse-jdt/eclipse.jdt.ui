/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

public class DeleteProjectChange extends AbstractDeleteChange {
	
	private IProject fProject;
	private boolean fDeleteContents;
	
	public DeleteProjectChange(IProject project, boolean deleteProjectContents){
		fProject= project;
		fDeleteContents= deleteProjectContents;
	}
		
	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Delete project";
	}

	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return fProject;
	}
	
	/* non java-doc
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws CoreException{
		fProject.delete(fDeleteContents, false, pm);
	}
}

