/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.part.FileEditorInput;
 
/**
 * A base content provider for Java elements. It provides access to the
 * Java element hierarchy without listening to changes in the Java model.
 * <p>
 * The following Java element hierarchy is surfaced by this content provider:
 * <p>
 * <pre>
Java model (<code>IJavaModel</code>)
   Java project (<code>IJavaProject</code>)
      package fragment root (<code>IPackageFragmentRoot</code>)
         package fragment (<code>IPackageFragment</code>)
            compilation unit (<code>ICompilationUnit</code>)
            binary class file (<code>IClassFile</code>)
 * </pre>
 * </p> 			
 * <p>
 * Note that when the entire Java project is declared to be package fragment root,
 * the corresponding package fragment root element that normally appears between the
 * Java project and the package fragments is automatically filtered out.
 * </p>
 * This content provider can optionally return working copy elements.
 * @see JavaUI#IWorkingCopyProvider
 * 
 * @since 2.0
 */
public class StandardJavaElementContentProvider implements ITreeContentProvider, IWorkingCopyProvider {

	protected static final Object[] NO_CHILDREN= new Object[0];

	protected boolean fProvideMembers= false;
	protected boolean fProvideWorkingCopy= false;
	
	/**
	 * Creates a new conent provider. It does not
	 * provide the members of a compilation unit and
	 * it does not provide working copies.
	 */	
	public StandardJavaElementContentProvider() {
	}
	
	/**
	 * Creates a new label provider.
	 *
	 * @param providerMembers does this content provider provide the members of a compilation unit
	 * @param provideWorkingCopies does this content provider provide working copy elements.
	 */
	public StandardJavaElementContentProvider(boolean provideMembers, boolean provideWorkingCopy) {
		fProvideMembers= provideMembers;
		fProvideWorkingCopy= provideWorkingCopy;
	}
	
	/**
	 * Returns whether the members are provided when asking
	 * for a CU's or ClassFile's children.
	 */
	public boolean getProvideMembers() {
		return fProvideMembers;
	}

	/**
	 * Sets whether the members are provided from
	 * a working copy of a compilation unit
	 */
	public void setProvideWorkingCopy(boolean b) {
		fProvideWorkingCopy= b;
	}

	/**
	 * Returns whether the members are provided 
	 * from a working copy a compilation unit.
	 */
	public boolean getProvideWorkingCopy() {
		return fProvideWorkingCopy;
	}

	/* (non-Javadoc)
	 * @see IReconciled#isReconciled()
	 */
	public boolean providesWorkingCopies() {
		return fProvideWorkingCopy;
	}


	/**
	 * Returns whether the members are provided when asking
	 * for a CU's or ClassFile's children.
	 */
	public void setProvideMembers(boolean b) {
		fProvideMembers= b;
	}
	
	/* (non-Javadoc)
	 * Method declared on IStructuredContentProvider.
	 */
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
	
	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * Method declared on ITreeContentProvider.
	 */
	public Object[] getChildren(Object element) {
		if (!exists(element))
			return NO_CHILDREN;
			
		try {
			if (element instanceof IJavaModel) 
				return getJavaProjects((IJavaModel)element);
			
			if (element instanceof IJavaProject) 
				return getPackageFragmentRoots((IJavaProject)element);
			
			if (element instanceof IPackageFragmentRoot) 
				return getPackageFragments((IPackageFragmentRoot)element);
			
			if (element instanceof IPackageFragment) 
				return getPackageContents((IPackageFragment)element);
				
			if (element instanceof IFolder)
				return getResources((IFolder)element);
			
			if (fProvideMembers &&
				element instanceof ISourceReference && element instanceof IParent) {
				if (element instanceof ICompilationUnit && fProvideWorkingCopy) {
					IWorkingCopy wc= getWorkingCopy((ICompilationUnit)element);
					if (wc != null)
						return ((IParent)wc).getChildren();
				}
				return ((IParent)element).getChildren();
			}
		} catch (JavaModelException e) {
			return NO_CHILDREN;
		}		
		return NO_CHILDREN;	
	}

	/* (non-Javadoc)
	 *
	 * @see ITreeContentProvider
	 */
	public boolean hasChildren(Object element) {
		if (fProvideMembers) {
			// assume CUs and class files are never empty
			if (element instanceof ICompilationUnit ||
				element instanceof IClassFile) {
				return true;
			}
		} else {
			// don't allow to drill down into a compilation unit or class file
			if (element instanceof ICompilationUnit ||
				element instanceof IClassFile ||
				element instanceof IFile)
			return false;
		}
			
		if (element instanceof IJavaProject) {
			IJavaProject jp= (IJavaProject)element;
			if (!jp.getProject().isOpen()) {
				return false;
			}	
		}
		
		if (element instanceof IParent) {
			try {
				// when we have Java children return true, else we fetch all the children
				if (((IParent)element).hasChildren())
					return true;
			} catch(JavaModelException e) {
				return true;
			}
		}
		Object[] children= getChildren(element);
		return (children != null) && children.length > 0;
	}
	 
	/* (non-Javadoc)
	 * Method declared on ITreeContentProvider.
	 */
	public Object getParent(Object element) {
		if (!exists(element))
			return null;
		return internalGetParent(element);			
	}
	
	private Object[] getPackageFragments(IPackageFragmentRoot root) throws JavaModelException {
		IJavaElement[] fragments= root.getChildren();
		Object[] nonJavaResources= root.getNonJavaResources();
		if (nonJavaResources == null)
			return fragments;
		return concatenate(fragments, nonJavaResources);
	}
	
	private Object[] getPackageFragmentRoots(IJavaProject project) throws JavaModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;
			
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		List list= new ArrayList(roots.length);
		// filter out package fragments that correspond to projects and
		// replace them with the package fragments directly
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)roots[i];
			if (isProjectPackageFragmentRoot(root)) {
				Object[] children= root.getChildren();
				for (int k= 0; k < children.length; k++) 
					list.add(children[k]);
			}
			else if (hasChildren(root)) {
				list.add(root);
			} 
		}
		return concatenate(list.toArray(), project.getNonJavaResources());
	}

	private Object[] getJavaProjects(IJavaModel jm) throws JavaModelException {
		return jm.getJavaProjects();
	}
	
	private Object[] getPackageContents(IPackageFragment fragment) throws JavaModelException {
		if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
			return concatenate(fragment.getCompilationUnits(), fragment.getNonJavaResources());
		}
		return concatenate(fragment.getClassFiles(), fragment.getNonJavaResources());
	}
	
	IWorkingCopy getWorkingCopy(ICompilationUnit cu) throws JavaModelException {
		IFile file= null;
		try {
			file= (IFile)cu.getUnderlyingResource();
		} catch (JavaModelException e) {
		}
		if (file != null) {
			IWorkingCopyManager wm= JavaPlugin.getDefault().getWorkingCopyManager();
			FileEditorInput ei= new FileEditorInput(file);
			IWorkingCopy wc= wm.getWorkingCopy(ei);
			return wc;
		}
		return null;
	}
	
	private Object[] getResources(IFolder folder) {
		try {
			// filter out folders that are package fragment roots
			Object[] members= folder.members();
			List nonJavaResources= new ArrayList();
			for (int i= 0; i < members.length; i++) {
				Object o= members[i];
				if (!(o instanceof IFolder && JavaCore.create((IFolder)o) != null)) {
					nonJavaResources.add(o);
				}	
			}
			return nonJavaResources.toArray();
		} catch(CoreException e) {
			return NO_CHILDREN;
		}
	}
	
	protected boolean isClassPathChange(IJavaElementDelta delta) {
		int flags= delta.getFlags();
		return (delta.getKind() == IJavaElementDelta.CHANGED && 
			((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_CLASSPATH_REORDER) != 0));
	}
	
	
	/**
	 * Returns parent of the presented hierarchy. Skips over 
	 * project package fragment root nodes.
	 */
	protected Object skipProjectPackageFragmentRoot(IPackageFragmentRoot root) {
		try {
			if (isProjectPackageFragmentRoot(root))
				return root.getParent(); 
			return root;
		} catch(JavaModelException e) {
			return root;
		}
	}
	
	protected boolean isPackageFragmentEmpty(IJavaElement element) throws JavaModelException {
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment)element;
			if (!(fragment.hasChildren() || fragment.getNonJavaResources().length > 0) && fragment.hasSubpackages()) 
				return true;
		}
		return false;
	}

	protected boolean isProjectPackageFragmentRoot(IPackageFragmentRoot root) throws JavaModelException {
		IResource resource= root.getUnderlyingResource();
		return (resource instanceof IProject);
	}
	
	protected boolean exists(Object element) {
		if (element == null) {
			return false;
		}
		if (element instanceof IResource) {
			return ((IResource)element).exists();
		}
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).exists();
		}
		return true;
	}
	
	protected Object internalGetParent(Object element) {
		if (element instanceof IJavaProject) {
			return ((IJavaProject)element).getJavaModel();
		}
		// try to map resources to the containing package fragment
		if (element instanceof IResource) {
			IResource parent= ((IResource)element).getParent();
			Object jParent= JavaCore.create(parent);
			if (jParent != null) 
				return jParent;
			return parent;
		}

		// for package fragments that are contained in a project package fragment
		// we have to skip the package fragment root as the parent.
		if (element instanceof IPackageFragment) {
			IPackageFragmentRoot parent= (IPackageFragmentRoot)((IPackageFragment)element).getParent();
			return skipProjectPackageFragmentRoot(parent);
		}
		if (element instanceof IJavaElement) {
			IJavaElement candidate= ((IJavaElement)element).getParent();
			// If the parent is a CU we might have shown working copy elements below CU level. If so
			// return the original element instead of the working copy.
			if (candidate != null && candidate.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit unit= (ICompilationUnit)candidate;
				if (unit.isWorkingCopy())
					candidate= unit.getOriginalElement();
			}
			return candidate;
		}
		return null;
	}
	
	protected static Object[] concatenate(Object[] a1, Object[] a2) {
		int a1Len= a1.length;
		int a2Len= a2.length;
		Object[] res= new Object[a1Len + a2Len];
		System.arraycopy(a1, 0, res, 0, a1Len);
		System.arraycopy(a2, 0, res, a1Len, a2Len); 
		return res;
	}


}