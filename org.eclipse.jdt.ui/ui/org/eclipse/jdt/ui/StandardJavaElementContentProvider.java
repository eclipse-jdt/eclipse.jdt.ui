/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;


import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
 
/**
 * A base content provider for Java elements. It provides access to the
 * Java element hierarchy without listening to changes in the Java model.
 * If updating the presentation on Java model change is required than 
 * clients have to subclass, listen to Java model changes and have to update
 * the UI using corresponding methods provided by the JFace viewers or their 
 * own UI presentation.
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
 * 
 * @since 2.0
 */
public class StandardJavaElementContentProvider implements ITreeContentProvider, IWorkingCopyProvider {

	protected static final Object[] NO_CHILDREN= new Object[0];
	protected boolean fProvideMembers;
	
	/**
	 * Creates a new content provider. The content provider does not
	 * provide members of compilation units or class files.
	 */	
	public StandardJavaElementContentProvider() {
		this(false);
	}
	
	/**
	 *@deprecated Use {@link #StandardJavaElementContentProvider(boolean)} instead.
	 * Since 3.0 compilation unit children are always provided as working copies. The Java Model
	 * does not support the 'original' mode anymore.
	 */
	public StandardJavaElementContentProvider(boolean provideMembers, boolean provideWorkingCopy) {
		this(provideMembers);
	}
	
	
	/**
	 * Creates a new <code>StandardJavaElementContentProvider</code>.
	 *
	 * @param provideMembers if <code>true</code> members below compilation units 
	 * and class files are provided. 
	 */
	public StandardJavaElementContentProvider(boolean provideMembers) {
		fProvideMembers= provideMembers;
	}
	
	/**
	 * Returns whether members are provided when asking
	 * for a compilation units or class file for its children.
	 * 
	 * @return <code>true</code> if the content provider provides members; 
	 * otherwise <code>false</code> is returned
	 */
	public boolean getProvideMembers() {
		return fProvideMembers;
	}

	/**
	 * Sets whether the content provider is supposed to return members
	 * when asking a compilation unit or class file for its children.
	 * 
	 * @param b if <code>true</code> then members are provided. 
	 * If <code>false</code> compilation units and class files are the
	 * leaves provided by this content provider.
	 */
	public void setProvideMembers(boolean b) {
		//hello
		fProvideMembers= b;
	}
	
	/**
	 * @deprecated Since 3.0 compilation unit children are always provided as working copies. The Java Model
	 * does not support the 'original' mode anymore. Therefore, this returned state of this method is now equals
	 * to the result of {@link #getProvideMembers()}.
	 */
	public boolean getProvideWorkingCopy() {
		return fProvideMembers;
	}

	/**
	 * @deprecated Since 3.0 compilation unit children are always provided from the working copy. The Java Model
	 * offers a unified world and does not support the 'original' mode anymore. 
	 */
	public void setProvideWorkingCopy(boolean b) {
	}

	/* (non-Javadoc)
	 * @see IWorkingCopyProvider#providesWorkingCopies()
	 */
	public boolean providesWorkingCopies() {
		return getProvideWorkingCopy();
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
			
			if (getProvideMembers() && element instanceof ISourceReference && element instanceof IParent) {
				return ((IParent)element).getChildren();
			}
		} catch (JavaModelException e) {
			return NO_CHILDREN;
		}		
		return NO_CHILDREN;	
	}

	/* (non-Javadoc)
	 * @see ITreeContentProvider
	 */
	public boolean hasChildren(Object element) {
		if (getProvideMembers()) {
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
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object[] getPackageFragmentRoots(IJavaProject project) throws JavaModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;
			
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		List list= new ArrayList(roots.length);
		// filter out package fragments that correspond to projects and
		// replace them with the package fragments directly
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
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

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object[] getJavaProjects(IJavaModel jm) throws JavaModelException {
		return jm.getJavaProjects();
	}
	
	private Object[] getPackageContents(IPackageFragment fragment) throws JavaModelException {
		if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
			return concatenate(fragment.getCompilationUnits(), fragment.getNonJavaResources());
		}
		return concatenate(fragment.getClassFiles(), fragment.getNonJavaResources());
	}
		
	private Object[] getResources(IFolder folder) {
		try {
			// filter out folders that are package fragment roots
			Object[] members= folder.members();
			List nonJavaResources= new ArrayList();
			for (int i= 0; i < members.length; i++) {
				Object o= members[i];
				// A folder can also be a package fragement root in the following case
				// Project
				//  + src <- source folder
				//    + excluded <- excluded from class path
				//      + included  <- a new source folder.
				// Included is a member of excluded, but since it is rendered as a source
				// folder we have to exclude it as a normal child.
				if (o instanceof IFolder) {
					IJavaElement element= JavaCore.create((IFolder)o);
					if (element instanceof IPackageFragmentRoot && element.exists()) {
						continue;
					}
				}
				nonJavaResources.add(o);
			}
			return nonJavaResources.toArray();
		} catch(CoreException e) {
			return NO_CHILDREN;
		}
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean isClassPathChange(IJavaElementDelta delta) {
		
		// need to test the flags only for package fragment roots
		if (delta.getElement().getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			return false;
		
		int flags= delta.getFlags();
		return (delta.getKind() == IJavaElementDelta.CHANGED && 
			((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REORDER) != 0));
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object skipProjectPackageFragmentRoot(IPackageFragmentRoot root) {
		if (isProjectPackageFragmentRoot(root))
			return root.getParent(); 
		return root;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean isPackageFragmentEmpty(IJavaElement element) throws JavaModelException {
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment)element;
			if (fragment.exists() && !(fragment.hasChildren() || fragment.getNonJavaResources().length > 0) && fragment.hasSubpackages()) 
				return true;
		}
		return false;
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean isProjectPackageFragmentRoot(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		return (resource instanceof IProject);
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
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
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object internalGetParent(Object element) {
		if (element instanceof IJavaProject) {
			return ((IJavaProject)element).getJavaModel();
		}
		// try to map resources to the containing package fragment
		if (element instanceof IResource) {
			IResource parent= ((IResource)element).getParent();
			IJavaElement jParent= JavaCore.create(parent);
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=31374
			if (jParent != null && jParent.exists()) 
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
				candidate= JavaModelUtil.toOriginal((ICompilationUnit) candidate);
			}
			return candidate;
		}
		return null;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected static Object[] concatenate(Object[] a1, Object[] a2) {
		int a1Len= a1.length;
		int a2Len= a2.length;
		Object[] res= new Object[a1Len + a2Len];
		System.arraycopy(a1, 0, res, 0, a1Len);
		System.arraycopy(a2, 0, res, a1Len, a2Len); 
		return res;
	}


}
