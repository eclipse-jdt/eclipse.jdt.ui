package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class MoveResourceChange extends ResourceReorgChange {
	
	MoveResourceChange(IResource res, IContainer dest, String newName){
		super(res, dest, newName);
	}
	
	/**
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected void doPerform(IPath path, IProgressMonitor pm) throws JavaModelException{
		try{
			getResource().move(path, true, pm);
		}catch(CoreException e){
			throw new JavaModelException(e);
		}	
	}
	
	/**
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return null;
	}

	public boolean isUndoable(){
		return false;
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Move resource";
	}
}

