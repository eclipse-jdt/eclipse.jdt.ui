/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

import org.eclipse.jdt.internal.ui.util.ArrayUtility;
 
/**
 * Abstract content provider for Java elements.
 * Use this class when you want to present the Java elements in a viewer.
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
 */
public abstract class AbstractJavaElementContentProvider implements IStructuredContentProvider, IElementChangedListener {
	
	protected StructuredViewer fViewer;
	protected Object fInput;
	
	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void dispose() {
		JavaCore.removeElementChangedListener(this);
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer= (StructuredViewer)viewer;
		if (oldInput == null && newInput != null) {
			JavaCore.addElementChangedListener(this); 
		} else if (oldInput != null && newInput == null) {
			JavaCore.removeElementChangedListener(this); 
		}
		fInput= newInput;
	}

	/* (non-Javadoc)
	 * Method declared on IStructuredContentProvider.
	 */
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
	
	/* (non-Javadoc)
	 * Method declared on ITreeContentProvider.
	 */
	public Object[] getChildren(Object element) {
		if (!exists(element))
			return ArrayUtility.getEmptyArray();
			
		try {
			if (element instanceof IJavaModel) 
				return getJavaProjects((IJavaModel)element);
			
			if (element instanceof IJavaProject) 
				return getNonProjectPackageFragmentRoots((IJavaProject)element);
			
			if (element instanceof IPackageFragmentRoot) 
				return getPackageFragments((IPackageFragmentRoot)element);
			
			if (element instanceof IPackageFragment) 
				return getPackageContents((IPackageFragment)element);
				
			if (element instanceof IFolder)
				return getResources((IFolder)element);
			
		} catch (JavaModelException e) {
			return ArrayUtility.getEmptyArray();
		}		
		return ArrayUtility.getEmptyArray();	
	}

	/* (non-Javadoc)
	 *
	 * @see ITreeContentProvider
	 */
	public boolean hasChildren(Object element) {
		// don't allow to drill down into a compilation unit or class file
		if (element instanceof ICompilationUnit ||
				element instanceof IClassFile ||
				element instanceof IFile)
			return false;
			
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
				//ErrorDialog.openError(Utilities.getFocusShell(), "Children non present", null, e.getStatus());
				return true;
			}
		}
		Object[] children= getChildren(element);
		return (children != null) && children.length > 0;
	}

	/* (non-Javadoc)
	 * Method declared on IElementChangedListener.
	 */
	public void elementChanged(final ElementChangedEvent event) {
		handleElementDelta(event.getDelta());
	}
	
	private void handleElementDelta(IJavaElementDelta delta) {
		try {
			processDelta(delta);
		} catch(JavaModelException e) {
			JavaPlugin.getDefault().logErrorStatus(JavaUIMessages.getString("JavaElementContentProvider.errorMessage"), e.getStatus()); //$NON-NLS-1$
		}
	}

	/**
	 * Processes the Java element delta recursively.
	 */
	protected abstract void processDelta(IJavaElementDelta delta) throws JavaModelException;
	
	private Object[] getPackageFragments(IPackageFragmentRoot root) throws JavaModelException {
		IJavaElement[] fragments= root.getChildren();
		// workaround for 1GE2T86: ITPJUI:WIN2000 - Null pointer exception in packages view
		// getNonJavaResources sometimes returns null!
		Object[] nonJavaResources= root.getNonJavaResources();
		if (nonJavaResources == null)
			return fragments;
		return ArrayUtility.merge(fragments, nonJavaResources);
	}
	
	private Object[] getNonProjectPackageFragmentRoots(IJavaProject project) throws JavaModelException {
		// return an empty enumeration when the project is closed
		if (!project.getProject().isOpen())
			return ArrayUtility.getEmptyArray();
			
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		List list= new ArrayList(roots.length);
		// filter out package fragments that correspond to projects and
		// replace them with the package fragments directly
		boolean projectIsRoot= false;
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)roots[i];
			if (isProjectPackageFragmentRoot(root)) {
				projectIsRoot= true;
				Object[] children= getPackageFragments(root);
				for (int k= 0; k < children.length; k++) {
					list.add(children[k]);
				}
				if (list.size() == 0){
					//System.out.println("No children found");	
				}	
			}
			else if (hasChildren(root)/*root.hasChildren()*/) {
				list.add(root);
			} else {
				//System.out.println("Root doesn't have children");
			}
		}
		if (projectIsRoot)
			return list.toArray();
		return ArrayUtility.merge(list.toArray(), project.getNonJavaResources());
	}

	private Object[] getJavaProjects(IJavaModel jm) throws JavaModelException {
		return jm.getJavaProjects();
	}
	
	private Object[] getPackageContents(IPackageFragment fragment) throws JavaModelException {
		if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
			return ArrayUtility.merge(fragment.getCompilationUnits(), fragment.getNonJavaResources());
		}
		return ArrayUtility.merge(fragment.getClassFiles(), fragment.getNonJavaResources());
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
			return ArrayUtility.getEmptyArray();
		}
	}
	
	protected boolean isClassPathChange(IJavaElementDelta delta) {
		int flags= delta.getFlags();
		return (delta.getKind() == IJavaElementDelta.CHANGED && 
			((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_CLASSPATH_REORDER) != 0));
	}
	
	protected Object fixupProjectPackageFragmentRoot(Object element) {
		try {
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				if (isProjectPackageFragmentRoot(root))
					return root.getParent(); 
			}
			return element;
		} catch(JavaModelException e) {
			return element;
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
			Object packageFragment= JavaCore.create(parent);
			if (packageFragment != null) 
				return packageFragment;
			return parent;
		}

		// for package fragments that are contained in a project package fragment
		// we have to skip the package fragment root as the parent.
		if (element instanceof IPackageFragment) {
			IPackageFragmentRoot parent= (IPackageFragmentRoot)((IPackageFragment)element).getParent();
			return fixupProjectPackageFragmentRoot(parent);
		}
		if (element instanceof IJavaElement)
			return ((IJavaElement)element).getParent();
		return null;
	}
}