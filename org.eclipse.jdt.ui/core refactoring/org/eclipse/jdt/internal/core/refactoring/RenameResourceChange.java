/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.Change;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.ChangeContext;

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
	 * @param newName does not include an extension
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
			pm.beginTask("rename resource", 1);
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
			
		String oldName= fResourcePath.removeFileExtension().lastSegment();
		IPath newPath= renamedResourcePath(fResourcePath, fNewName);
		return new RenameResourceChange(newPath, oldName);
	}
	
	/**
	 * changes resource names - changes the name, leaves the extension untouched
	 * /s/p/Fred.java renamed to Dude becomes /s/p/Dude.java
	 */
	public static IPath renamedResourcePath(IPath path, String newName){
		String oldExtension= path.getFileExtension();
		String newEnding= oldExtension == null ? "": "." + oldExtension;
		return path.removeFileExtension().removeLastSegments(1).append(newName + newEnding);
	}
	
	public String getName(){
		return "RenameResource: " + fResourcePath.toString() + " to:" + renamedResourcePath(fResourcePath, fNewName);
	}
	
	public IJavaElement getCorrespondingJavaElement(){
		return JavaCore.create(getResource());
	}	
}