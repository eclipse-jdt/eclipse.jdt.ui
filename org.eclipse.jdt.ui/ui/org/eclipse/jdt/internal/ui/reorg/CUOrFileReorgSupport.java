/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CUOrFileReorgSupport implements ICopySupport, IMoveSupport, INamingPolicy {
	private static final String PREFIX= "reorg_policy.cu.";
	private static final String ERROR_DUPLICATE= "duplicate";
	private static final String ERROR_WRONG_EXTENSION= "wrong_extension";
	private static final String ERROR_INVALID_NAME= "invalid_name";

	public Object copyTo(Object element, Object dest, String newName, IProgressMonitor pm) throws CoreException, JavaModelException {
		IPackageFragment destination= getDestination(dest);
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)element;
			String oldName= cu.getElementName();
			cu.copy(destination, null, newName, true, pm);
			if (newName == null)
				return destination.getCompilationUnit(oldName);
			return destination.getCompilationUnit(newName);
		} else if (element instanceof IFile) {
			IFile file= (IFile)element;
			String oldName= file.getName();
			IResource destResource= destination.getCorrespondingResource();
			IPath path= destResource.getFullPath();
			if (newName == null)
				path= path.append(oldName);
			else 
				path= path.append(newName);
			file.copy(path, true, pm);
			return destResource.getWorkspace().getRoot().getFile(path);		
		}
		return null;
	}
	
	public boolean isCopyable(Object element) {
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)element;
			try {
				IResource res= cu.getUnderlyingResource();
				return res != null && res.equals(cu.getCorrespondingResource());
			} catch (JavaModelException e) {
			}
		}
		return element instanceof IFile;
	}

	public String isValidNewName(Object original, Object destination, String name) {
		IPackageFragment pkg= getDestination(destination);
		if (pkg == null)
			return null;
			
		// the order is important here since getCompilationUnit() throws an exception
		// if the name is invalid.
		if (original instanceof ICompilationUnit) {
			if (!name.endsWith(".java"))
				return JavaPlugin.getResourceString(PREFIX+ERROR_WRONG_EXTENSION);
			if (!JavaConventions.validateCompilationUnitName(name).isOK())
				return JavaPlugin.getResourceString(PREFIX+ERROR_INVALID_NAME);
		}
		try {
			if (pkg.getCompilationUnit(name).exists() || ReorgSupport.getResource(pkg, name) != null)
				return JavaPlugin.getResourceString(PREFIX+ERROR_DUPLICATE);
		} catch (JavaModelException e) {
		}
		return null;
	}

	public Object getElement(Object parent, String name) {
		IPackageFragment pkg= getDestination(parent);
		if (pkg == null)
			return null;
			
		if (name.endsWith(".java"))
			return pkg.getCompilationUnit(name);
		try {
			return ReorgSupport.getResource(pkg, name);
		} catch (JavaModelException e) {
		}
		return null;
	}

	public Object moveTo(Object element, Object dest, String newName, IProgressMonitor pm) throws JavaModelException, CoreException {
		IPackageFragment destination= getDestination(dest);
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)element;
			String oldName= cu.getElementName();
			cu.move(destination, null, newName, true, pm);
			if (newName == null)
				return destination.getCompilationUnit(oldName);
			return destination.getCompilationUnit(newName);
		} else if (element instanceof IFile) {
			IFile file= (IFile)element;
			String oldName= file.getName();
			IResource destResource= destination.getCorrespondingResource();
			IPath path= destResource.getFullPath();
			if (newName == null)
				path= path.append(oldName);
			else 
				path= path.append(newName);
			file.move(path, true, pm);
			return destResource.getWorkspace().getRoot().getFile(path);		
		}
		return null;
	}

	public boolean canReplace(Object original, Object container, String newName) {
		IPackageFragment fragment= (IPackageFragment)container;
		try {
			Object res= ReorgSupport.getResource(fragment, newName);
			if (original.equals(res))
				return false;
		} catch (JavaModelException e) {
		}
		ICompilationUnit cu= fragment.getCompilationUnit(newName);
		if (original.equals(cu))
			return false;
		return true;
	}
	
	public boolean canCopy(List elements, Object dest) {
		return getDestination(dest) != null;
	}
	
	public boolean canBeAncestor(Object ancestor) {
		if (ancestor instanceof IJavaModel)
			return true;
		if (ancestor instanceof IJavaProject)
			return true;
		if (ancestor instanceof IPackageFragmentRoot) {
			return !((IPackageFragmentRoot)ancestor).isReadOnly();
		}
		if (ancestor instanceof IPackageFragment)
			return !((IPackageFragment)ancestor).isReadOnly();
		return false;
	}
	
	public boolean isMovable(Object element) {
		return isCopyable(element);
	}

	public boolean canMove(List elements, Object destination) {
		if (destinationIsParent(elements, getDestination(destination)))
			return false;
		
		return canCopy(elements, destination);
	}
	
	private IPackageFragment getDestination(Object dest) {
		IPackageFragment result= null;
		try {
			result= ReorgSupport.getDestinationAsPackageFragement(dest);
			if (result != null && !result.isReadOnly())
				return result;
		} catch (JavaModelException e) {
		}
		return null;	
	}
	
	private boolean destinationIsParent(List elements, IPackageFragment pkg) {
		return ReorgSupport.destinationIsParent(elements, pkg);
	}
}