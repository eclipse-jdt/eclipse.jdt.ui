/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.Utilities;

public class ReorgSupport {

	private static final String DEFAULT_PACKAGE= "";
	
	/**
	 * Returns the actual destination for the given <code>dest</code> if the
	 * elements to be dropped are files or compilation units.
	 */
	public static IPackageFragment getDestinationAsPackageFragement(Object dest) throws JavaModelException {
		if (dest instanceof IPackageFragment)
			return (IPackageFragment)dest;
		
		if (dest instanceof IJavaProject) {
			dest= getDestinationAsPackageFragmentRoot((IJavaProject)dest);
		}
			
		if (dest instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)dest;
			return root.getPackageFragment(DEFAULT_PACKAGE);
		}
		
		return null;
	}	
	
	/**
	 * Checks if <code>dest</code> isn't the parent of one of the elements given by the 
	 * list <code>elements</code>.
	 */
	public static boolean destinationIsParent(List elements, IJavaElement dest) {
		if (dest == null)
			return false;
		IResource parent= null;
		try {
			parent= dest.getCorrespondingResource();
		} catch(JavaModelException e) {
			return false;
		}
			
		if (parent == null)
			return false;
			
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			IResource resource= Utilities.convertToResource(iter.next());
			if (resource == null)
				return false;
			if (parent.equals(resource.getParent()))
				return true;	
		}
		return false;
	}
	
	/**
	 * Checks if <code>parent</code> isn't the parent of one of the elements given by the 
	 * list <code>elements</code>.
	 */
	public static boolean destinationIsParent(List elements, IPackageFragmentRoot parent) {
		if (parent == null)
			return false;
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			IPackageFragment pkg= (IPackageFragment)iter.next();
			if (parent.equals(pkg.getParent()))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns the package fragment root to be used as a destination for the
	 * given project. If the project has more than one package fragment root
	 * that isn't an archive <code>null</code> is returned.
	 */
	public static IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		IPackageFragmentRoot result= null;
		for (int i= 0; i < roots.length; i++) {
			if (! roots[i].isArchive()) {
				if (result != null)
					return null;
				result= roots[i];
			}
		}
		return result;
	}
	
}