/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Creates the appropriate reorg support objects for a given element. The I*Support
 * objects allow us to treat various IJavaElements + resources polymorphically.
 * Moreover, they encapsulate the various policies ("can I copy X to Y?") and thus
 * keep them out of the actions.
 */
public class ReorgSupportFactory {
	private static Object fgPackageReorgSupport= new PackageReorgSupport();
	private static Object fgCUReorgSupport= new CUOrFileReorgSupport();
	private static Object fgResourceReorgSupport= new ResourceReorgSupport();
	
	private static IDeleteSupport fgDeleteSupport= new DeleteSupport();
	private static IRenameSupport fgRenameSupport= new RenameSupport();
	private static INamingPolicy fgPackageRootNamingPolicy= new PackageFragmentRootNamingPolicy();
	
	public static ICopySupport createCopySupport(List elements) {
		if (hasParentCollision(elements))
			return NoReorgSupport.getInstance();
		if (elements.size() < 1) 
			return NoReorgSupport.getInstance();
			
		Object first= elements.get(0);
		if (first instanceof IPackageFragment) 
			return (ICopySupport)getReorgSupport((IPackageFragment)first, elements);
		if (first instanceof ICompilationUnit)
			return (ICopySupport)getReorgSupport((ICompilationUnit)first, elements);
		if (first instanceof IResource) {
			return (ICopySupport)getReorgSupport((IResource)first, elements);
		}

		return NoReorgSupport.getInstance();
	}
	
	private static IPath getPath(Object element) {
		String name= getName(element);
		Object parent= getJavaParent(element);
		if (name == null)
			return new Path("");

		if (parent == null) {
			return new Path(name);
		} else {
			return getPath(parent).append(name);
		}
	}
	
	public static Object getJavaParent(Object element) {
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			IResource parent= (IResource)res.getParent();
			Object javaParent= JavaCore.create(parent);
			if (javaParent != null)
				return javaParent;
			return parent;
		} else if (element instanceof IJavaElement) {
			IJavaElement jElement= (IJavaElement)element;
			return jElement.getParent();
		}
		return null;
	}
	
	private static String getName(Object element) {
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			return res.getName();
		} else if (element instanceof IJavaElement) {
			IJavaElement res= (IJavaElement)element;
			return res.getElementName();
		}
		return null;
	}
	
	private static boolean hasParentCollision(List elements) {
		int size= elements.size();
		List paths= new ArrayList(elements.size());
		for (int i= 0; i < size; i++) {
			paths.add(getPath(elements.get(i)));
		}
		for (int i= 0; i < size; i++) {
			for (int j= 0; j < size; j++) {
				if (i != j) {
					IPath left= (IPath)paths.get(i);
					IPath right= (IPath)paths.get(j);
					if (left.isPrefixOf(right))
						return true;
				}
			}
		}
		return false;
	}
	
	private static Object getReorgSupport(IPackageFragment pkg, List elements) {
		for (int i= 0; i < elements.size(); i++) {
			if (!(elements.get(i) instanceof IPackageFragment))
				return NoReorgSupport.getInstance();
		}
		return fgPackageReorgSupport;
	}

	private static Object getReorgSupport(ICompilationUnit cu, List elements) {
		for (int i= 0; i < elements.size(); i++) {
			Object o= elements.get(i);
			if (!(o instanceof ICompilationUnit || o instanceof IFile))
				return NoReorgSupport.getInstance();
		}
		return fgCUReorgSupport;
	}

	private static Object getReorgSupport(IResource res, List elements) {
		for (int i= 0; i < elements.size(); i++) {
			Object o= elements.get(i);
			if (!(o instanceof IResource))
				return NoReorgSupport.getInstance();
		}
		return fgResourceReorgSupport;
	}

	public static IMoveSupport createMoveSupport(List elements) {
		if (hasParentCollision(elements))
			return NoReorgSupport.getInstance();
		if (elements.size() < 1) 
			return NoReorgSupport.getInstance();
			
		Object first= elements.get(0);
		if (first instanceof IPackageFragment) 
			return (IMoveSupport)getReorgSupport((IPackageFragment)first, elements);
		if (first instanceof ICompilationUnit)
			return (IMoveSupport)getReorgSupport((ICompilationUnit)first, elements);
		if (first instanceof IResource) {
			return (IMoveSupport)getReorgSupport((IResource)first, elements);
		}

		return NoReorgSupport.getInstance();
	}
	
	public static IDeleteSupport createDeleteSupport(List elements) {
		if (hasParentCollision(elements))
			return NoReorgSupport.getInstance();
		return fgDeleteSupport;
	}
	
	public static INamingPolicy createNamingPolicy(Object element) {
		if (element instanceof IPackageFragmentRoot)
			return fgPackageRootNamingPolicy;
		if (element instanceof IPackageFragment) 
			return (INamingPolicy)fgPackageReorgSupport;
		if (element instanceof ICompilationUnit)
			return (INamingPolicy)fgCUReorgSupport;
		if (element instanceof IResource) {
			return (INamingPolicy)fgResourceReorgSupport;
		}
		return NoReorgSupport.getInstance();
	}
	
	public static IRenameSupport createRenameSupport(Object element) {
		return fgRenameSupport;
	}
	
	public static final IPackageFragmentRoot getPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		IPackageFragmentRoot[] roots= p.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (isProjectPackageFragementRoot(roots[i]))
				return roots[i];
		}
		return null;
	}
	
	public static final boolean isProjectPackageFragementRoot(IPackageFragmentRoot root) throws JavaModelException {
		return root.getUnderlyingResource() instanceof IProject;
	}
	
	public static final boolean isPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		return getPackageFragmentRoot(p) != null;
	}
	
	

}