/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;


/**
 * Represents a change that renames a given resource
 */
public class RenameResourceChange extends Change {
	
	/*
	 * we cannot use handles because they became invalid when you rename the resource.
	 * paths do not.
	 */
	private IPath fResourcePath;
	private String fNewName;
	
	/**
	 * @param newName includes the extension
	 */
	public RenameResourceChange(IResource resource, String newName){
		this(resource.getFullPath(), newName);
	}
	
	private RenameResourceChange(IPath resourcePath, String newName){
		fResourcePath= resourcePath;
		fNewName= newName;
	}
	
	private IResource getResource(){
		return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
	}
	
	/**
	 * to avoid the exception senders should check if a resource with the new name already exists
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		try {
			pm.beginTask(RefactoringCoreMessages.getString("RenameResourceChange.rename_resource"), 1); //$NON-NLS-1$
			if (!isActive()){
				pm.worked(1);
				return;
			} 
			getResource().move(renamedResourcePath(fResourcePath, fNewName), false, pm);
		} catch(Exception e){
			handleException(context, e);
			setActive(false);
		} finally {
			pm.done();
		}
	}
	
	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();
			
		String oldName= fResourcePath.lastSegment();
		IPath newPath= renamedResourcePath(fResourcePath, fNewName);
		return new RenameResourceChange(newPath, oldName);
	}
	
	/**
	 * changes resource names 
	 * /s/p/A.java renamed to B.java becomes /s/p/B.java
	 */
	public static IPath renamedResourcePath(IPath path, String newName){
		return path.removeLastSegments(1).append(newName);
	}
	
	public String getName(){
		return RefactoringCoreMessages.getFormattedString("RenameResourceChange.name", new String[]{fResourcePath.toString(), fNewName});//$NON-NLS-1$
	}
	
	public Object getModifiedLanguageElement(){
		return getResource();
	}	
}