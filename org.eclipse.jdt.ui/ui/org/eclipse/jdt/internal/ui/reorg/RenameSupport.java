/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IContainer;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.ISourceManipulation;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

public class RenameSupport implements IRenameSupport {


	public Object rename(Object element, String newName, IProgressMonitor pm) throws CoreException, JavaModelException {
		if (element instanceof ISourceManipulation) {
			ISourceManipulation m= (ISourceManipulation)element;
			m.rename(newName, true, pm);
			JdtHackFinder.fixme("rename doesn't return renamed element");
			return null;
		} 
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			element= root.getCorrespondingResource();
		}
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			boolean isFile= res instanceof IFile;
			IWorkspace ws= res.getWorkspace();
			IPath path= res.getFullPath();
			path= path.removeLastSegments(1);
			path= path.append(newName);
			res.move(path, true, pm);
			if (isFile)
				res= ws.getRoot().getFile(path);
			else 
				res= ws.getRoot().getFolder(path);
			return JavaCore.create(res);
		}
		
		if (element instanceof IJavaProject) {
			IJavaProject p= (IJavaProject)element;
			IProject project= p.getProject();
			IContainer parent= project.getParent();
			IPath newPath= parent.getFullPath().append(newName);
			p.getProject().move(newPath, true, pm);
			return parent.findMember(newName);
		}
		return null;
	}

	/**
	 * returns a user-readable error message.
	 * returns null if ok
	 */
	public boolean canRename(Object element) {
		if (element instanceof IPackageFragment) {
			IPackageFragment pkg= (IPackageFragment)element;
			if (pkg.isDefaultPackage())
				return false;
		}
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			if (root.isReadOnly()) 
				return false;
			try {
				IResource res= root.getUnderlyingResource();
				if (res instanceof IProject)
					return false;
			} catch (JavaModelException e) {
				return false;
			}
			return true;
		}
		if (element instanceof IJavaElement) {
			IJavaElement jElement= (IJavaElement)element;
			if (jElement.isReadOnly())
				return false;
			IJavaElement parent= jElement.getParent();
			if (parent == null || parent.isReadOnly())
				return false;
			return element instanceof IPackageFragmentRoot || 
				element instanceof IPackageFragment ||
				element instanceof ICompilationUnit ||
				element instanceof IJavaProject;
		}
		
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
	
}