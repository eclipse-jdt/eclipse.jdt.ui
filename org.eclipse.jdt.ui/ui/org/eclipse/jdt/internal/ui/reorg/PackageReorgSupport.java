/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class PackageReorgSupport implements ICopySupport, IMoveSupport, INamingPolicy {

	private static final String PREFIX= "reorg_policy.package.";
	private static final String ERROR_DUPLICATE= "duplicate";
	private static final String ERROR_INVALID_NAME= "invalid_name";
	private static final String ERROR_EXCEPTION= "exception";
	
	public Object copyTo(Object element, Object dest, String newName, IProgressMonitor pm) throws JavaModelException {
		IPackageFragment pkg= (IPackageFragment)element;
		String oldName= pkg.getElementName();
		IPackageFragmentRoot destination= getDestination(dest);
		pkg.copy(destination, null, newName, true, pm);
		if (newName == null)
			return destination.getPackageFragment(oldName);
		return destination.getPackageFragment(newName);
	}

	public boolean canCopy(List elements, Object dest) {
		return getDestination(dest) != null;
	}

	public boolean isCopyable(Object element) {
		IPackageFragment pkg= (IPackageFragment)element;
		if (pkg.isDefaultPackage())
			return false;
		try {
			IResource res= pkg.getUnderlyingResource();
			return  res != null && res.equals(pkg.getCorrespondingResource());
		} catch (JavaModelException e) {
		}
		return false;
	}

	public boolean isCopyCompatible(List elements) {
		for (int i= 0; i < elements.size(); i++) {
			if (!(elements.get(i) instanceof IPackageFragment))
				return false;
		}
		return true;
	}

	public boolean canBeAncestor(Object ancestor) {
		if (ancestor instanceof IJavaModel)
			return true;
		if (ancestor instanceof IJavaProject)
			return true;
		if (ancestor instanceof IPackageFragmentRoot) {
			return !((IPackageFragmentRoot)ancestor).isReadOnly();
		}
		return false;
	}

	public String isValidNewName(Object original, Object destination, String name) {
		IPackageFragmentRoot root= getDestination(destination);
		if (root == null)
			return null;
			
		// the order is important here since getPackageFragment() throws an exception
		// if the name is invalid.
		if (!JavaConventions.validatePackageName(name).isOK())
			return JavaPlugin.getResourceString(PREFIX+ERROR_INVALID_NAME);
		IPackageFragment pkg= root.getPackageFragment(name);
		try {
			if (pkg.exists() && pkg.hasChildren())
				return JavaPlugin.getResourceString(PREFIX+ERROR_DUPLICATE);
		} catch (JavaModelException e) {
			return JavaPlugin.getResourceString(PREFIX+ERROR_EXCEPTION);
		}
		return null;
	}

	public Object getElement(Object parent, String name) {
		IPackageFragmentRoot root= getDestination(parent);
		if (root == null)
			return null;
			
		// the order is important here since getPackageFragment() throws an exception
		// if the name is invalid.
		if (!JavaConventions.validatePackageName(name).isOK())
			return null;
		return root.getPackageFragment(name);
	}
	public String getElementName(Object element) {
		if (element instanceof IPackageFragment) {
			return ((IPackageFragment)element).getElementName();
		} 
		return null;
	}

	public boolean canReplace(Object original, Object container, String newName) {
		IPackageFragmentRoot root= (IPackageFragmentRoot)container;
		if (original.equals(root.getPackageFragment(newName)))
			return false;
		return true;
	}
	
	public Object moveTo(Object element, Object dest, String newName, IProgressMonitor pm) throws JavaModelException {
		IPackageFragment pkg= (IPackageFragment)element;
		String oldName= pkg.getElementName();
		IPackageFragmentRoot destination= getDestination(dest);
		pkg.move(destination, null, newName, true, pm);
		if (newName == null)
			return destination.getPackageFragment(oldName);
		return destination.getPackageFragment(newName);
	}

	public boolean canMove(List elements, Object dest) {
		if (destinationIsParent(elements, getDestination(dest)))
			return false;
		
		return canCopy(elements, dest);	
	}
	
	public boolean isMovable(Object p0) {
		return isCopyable(p0);
	}

	public boolean isMoveCompatible(List p0) {
		return isCopyCompatible(p0);
	}
	
	private IPackageFragmentRoot getDestination(Object dest) {
		IPackageFragmentRoot result= null;
		if (dest instanceof IPackageFragmentRoot) {
			result= (IPackageFragmentRoot)dest;
		}
		
		if (dest instanceof IJavaProject) {
			try {
				result= ReorgSupport.getDestinationAsPackageFragmentRoot((IJavaProject)dest);
			} catch (JavaModelException e) {
			}
		}	
		
		if (result != null && !result.isReadOnly())
			return result;
			
		return null;
	}
	
	private boolean destinationIsParent(List elements, IPackageFragmentRoot root) {
		return ReorgSupport.destinationIsParent(elements, root);
	}

}