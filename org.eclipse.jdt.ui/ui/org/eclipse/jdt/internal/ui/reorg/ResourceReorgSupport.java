/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ResourceReorgSupport  implements ICopySupport, IMoveSupport, INamingPolicy {
	private static final String PREFIX= "reorg_policy.file.";
	private static final String ERROR_DUPLICATE= "duplicate";
	private static final String ERROR_EXCEPTION= "exception";
	private static final String ERROR_INVALID_NAME= "invalid_name";

	public boolean isCopyable(Object element) {
		if (element instanceof IFolder)
			return true;
		
		if (element instanceof IFile) {
			Object parent= ReorgSupportFactory.getJavaParent(element);
			if (parent instanceof IJavaElement) {
				return !((IJavaElement)parent).isReadOnly();
			}
			return parent != null;
		};
		return false;
	}

	public Object copyTo(Object element, Object dest, String newName, IProgressMonitor pm) throws CoreException, JavaModelException {
		IResource destResource= getDestination(dest);
		IResource res= (IResource)element;
		String oldName= res.getName();
		IPath path= destResource.getFullPath();
		if (newName == null)
			path= path.append(oldName);
		else 
			path= path.append(newName);
		res.copy(path, true, pm);
		return destResource.getWorkspace().getRoot().getFile(path);
	}

	public String isValidNewName(Object original, Object destination, String name) {
		IContainer c= getDestination(destination);
		if (c == null)
			return JavaPlugin.getResourceString(PREFIX+ERROR_EXCEPTION);
						
		if (c.findMember(name) != null)
			return JavaPlugin.getResourceString(PREFIX+ERROR_DUPLICATE);
			
		IPath p= c.getFullPath();
		if (!p.isValidSegment(name))
			return JavaPlugin.getResourceString(PREFIX+ERROR_INVALID_NAME);
		return null;
	}
	
	public Object getElement(Object parent, String name) {
		IContainer c= getDestination(parent);
		if (c == null)
			return null;						
		return c.findMember(name);
	}
	
	public String getElementName(Object element) {
		if (element instanceof IResource) {
			return ((IResource)element).getName();
		} 
		return null;
	}

	public Object moveTo(Object element, Object dest, String newName, IProgressMonitor pm) throws JavaModelException, CoreException {
		IResource destResource= getDestination(dest);
		IResource res= (IResource)element;
		String oldName= res.getName();
		IPath path= destResource.getFullPath();
		if (newName == null)
			path= path.append(oldName);
		else 
			path= path.append(newName);
		res.move(path, true, pm);
		return destResource.getWorkspace().getRoot().getFile(path);
	}

	public boolean canReplace(Object o, Object destination, String newName) {
		// the resource copy/move doesn't overwrite existing resources.
		return false;
	}
	
	public boolean canCopy(List elements, Object dest) {
		return getDestination(dest) != null;
	}
	
	public boolean canBeAncestor(Object element) {
		if (element instanceof IJavaModel)
			return true;
		if (element instanceof IJavaProject)
			return true;
		if (element instanceof IPackageFragment)
			return !((IPackageFragment)element).isReadOnly();
		if (element instanceof IPackageFragmentRoot)
			return !((IPackageFragmentRoot)element).isReadOnly();
		return element instanceof IContainer;
	}
	
	public boolean isMovable(Object element) {
		return isCopyable(element);
	}

	public boolean canMove(List elements, Object dest) {
		if (destinationIsParent(elements, getDestination(dest)))
			return false;
			
		return canCopy(elements, dest);
	}
	
	private IContainer getDestination(Object dest) {
		IPackageFragment pkg= null;
		try {
			if (dest instanceof IJavaElement) {
				dest= ((IJavaElement)dest).getCorrespondingResource();
			}
		} catch (JavaModelException e) {
		}
					
		if (dest instanceof IContainer)
			return (IContainer)dest;
			
		return null;		
	}
	
	private boolean destinationIsParent(List elements, IContainer dest) {
		if (dest == null)
			return false;
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			IResource resource= (IResource)iter.next();
			if (dest.equals(resource.getParent()))
				return true;
		}
		return false;
	}
}