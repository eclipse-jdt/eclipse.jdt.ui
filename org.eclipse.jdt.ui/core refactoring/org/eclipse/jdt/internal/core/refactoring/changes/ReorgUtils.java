package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

class ReorgUtils {
	
	private static final String DEFAULT_PACKAGE= ""; //$NON-NLS-1$
	
	//no instances
	private ReorgUtils(){
	}
	
	static Object getJavaParent(Object element) {
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
	
	static boolean hasParentCollision(List elements) {
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
	
	private static IPath getPath(Object element) {
		String name= getName(element);
		Object parent= getJavaParent(element);
		if (name == null)
			return new Path(""); //$NON-NLS-1$

		if (parent == null) {
			return new Path(name);
		} else {
			return getPath(parent).append(name);
		}
	}
	
	static String getName(Object element) {
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			return res.getName();
		} else if (element instanceof IJavaElement) {
			IJavaElement res= (IJavaElement)element;
			return res.getElementName();
		}
		return null;
	}
	
	static final IPackageFragmentRoot getPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		IPackageFragmentRoot[] roots= p.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (isProjectPackageFragementRoot(roots[i]))
				return roots[i];
		}
		return null;
	}
	
	static final boolean isProjectPackageFragementRoot(IPackageFragmentRoot root) throws JavaModelException {
		return root.getUnderlyingResource() instanceof IProject;
	}
	
	static final boolean isPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		return getPackageFragmentRoot(p) != null;
	}
	
	/**
	 * Returns the package fragment root to be used as a destination for the
	 * given project. If the project has more than one package fragment root
	 * that isn't an archive <code>null</code> is returned.
	 */
	static IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaProject project) throws JavaModelException {
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
	
	/**
	 * Returns the actual destination for the given <code>dest</code> if the
	 * elements to be dropped are files or compilation units.
	 */
	static IPackageFragment getDestinationAsPackageFragement(Object dest) throws JavaModelException {
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

	//-----------	
	static IPath getResourcePath(IResource resource){
		return resource.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}

	static IFile getFile(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	}
	
	static IFolder getFolder(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
	}
	
	static IProject getProject(IPath path){
		return (IProject)ResourcesPlugin.getWorkspace().getRoot().findMember(path);
	}
	
	static Object getResource(IPackageFragment fragment, String name) throws JavaModelException {
		Object[] children= fragment.getNonJavaResources();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IResource) {
				IResource child= (IResource)children[i];
				if (child.getName().equals(name))
					return children[i];
			} else if (children[i] instanceof IStorage) {
				IStorage child= (IStorage)children[i];
				if (child.getName().equals(name))
					return children[i];
			}
		}
		return null;
	}
	
	/**
	 * Checks if <code>dest</code> isn't the parent of one of the elements given by the 
	 * list <code>elements</code>.
	 */
	static boolean destinationIsParent(List elements, IJavaElement dest) {
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
			IResource resource= convertToResource(iter.next());
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
	static boolean destinationIsParent(List elements, IPackageFragmentRoot parent) {
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
	 * Tries to convert the given object into an <code>IResource</code>. If the 
	 * object can't be converted into an <code>IResource</code> <code>null</code> 
	 * is returned.
	 */
	private static IResource convertToResource(Object o) {
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IAdaptable)
			return (IResource)((IAdaptable)o).getAdapter(IResource.class);
			
		return null;	
	}	
}

