package org.eclipse.jdt.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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
import org.eclipse.jdt.internal.ui.util.ArrayUtility;
 
/**
 * Standard tree content provider for Java elements.
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
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class JavaElementContentProvider implements ITreeContentProvider, IElementChangedListener {
	
	private TreeViewer fViewer;

	/**
	 * Creates a new content provider for Java elements.
	 */
	public JavaElementContentProvider() {
	}
	
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
		fViewer= (TreeViewer)viewer;
		if (oldInput == null && newInput != null) {
			JavaCore.addElementChangedListener(this); 
		} else if (oldInput != null && newInput == null) {
			JavaCore.removeElementChangedListener(this); 
		}
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
	 * Method declared on ITreeContentProvider.
	 */
	public Object getParent(Object element) {
		if (!exists(element))
			return null;

		return internalGetParent(element);			
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
			JavaPlugin.getDefault().logErrorStatus("Child non present", e.getStatus());
		}
	}
	
	/**
	 * Processes a delta recursively. When more than two children are affected the
	 * tree is fully refreshed starting at this node. The delta is processed in the
	 * current thread but the viewer updates are posted to the UI thread.
	 */
	private void processDelta(IJavaElementDelta delta) throws JavaModelException {
		int kind= delta.getKind();
		int flags= delta.getFlags();
		IJavaElement element= delta.getElement();
		
		// handle open and closing of a solution or project
		if (((flags & IJavaElementDelta.F_CLOSED) != 0) || ((flags & IJavaElementDelta.F_OPENED) != 0)) {			
			postRefresh(element);
			return;
		}

		if (kind == IJavaElementDelta.REMOVED) {
			Object parent= internalGetParent(element);			
			postRemove(element);
			if (parent instanceof IPackageFragment) 
				updatePackageIcon((IPackageFragment)parent);
			// we are filtering out empty subpackages, so we
			// a package becomes empty we remove it from the viewer. 
			if (isPackageFragmentEmpty(element.getParent())) {
				postRefresh(internalGetParent(parent));
			}  
			return;
		}

		if (kind == IJavaElementDelta.ADDED) { 
			Object parent= internalGetParent(element);
			// we are filtering out empty subpackages, so we
			// have to handle additions to them specially. 
			if (parent instanceof IPackageFragment) {
				postRefresh(internalGetParent(parent));
			} else {  
				postAdd(parent, element);
			}						
		}

		// we don't show the contents of a compilation or IClassFile, so don't go any deeper
		if ((element instanceof ICompilationUnit) || (element instanceof IClassFile))
			return;
		
		if (isClassPathChange(delta)) {
			 // throw the towel and do a full refresh of the affected java project. 
			postRefresh(element.getJavaProject());
		}
		
		if (delta.getResourceDeltas() != null) {
			IResourceDelta[] rd= delta.getResourceDeltas();
			IJavaProject project= element.getJavaProject();
			for (int i= 0; i < rd.length; i++) {
				processResourceDelta(rd[i], element, project.getOutputLocation());
			}
		}
		
		IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
		if (affectedChildren.length > 1) {
			// a package fragment might become non empty refresh from the parent
			if (element instanceof IPackageFragment) {
				IJavaElement parent= (IJavaElement)internalGetParent(element);
				postRefresh(parent);
				return;
			}
			// more than one child changed, refresh from here downwards
			postRefresh(fixupProjectPackageFragmentRoot(element));
			return;
		}
		for (int i= 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i]);
		}
	}
	
	/**
	 * Updates the package icon
	 */
	 private void updatePackageIcon(final IJavaElement element) {
	 	postRunnable(new Runnable() {
				public void run() {
					fViewer.update(element, new String[]{IBasicPropertyConstants.P_IMAGE});
				}
		});
	 }

	/**
	 * Process resource deltas
	 */
	private void processResourceDelta(IResourceDelta delta, Object parent, IPath output) {
		int status= delta.getKind();
		IResource resource= delta.getResource();
		// filter out changes affecting the output folder
		if (resource == null || output.isPrefixOf(resource.getFullPath())) 
			return;
			
		// this could be optimized by handling all the added children in the parent
		if ((status & IResourceDelta.REMOVED) != 0) {
			if (parent instanceof IPackageFragment) 
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent));
			else 
				postRemove(resource);
		}
		if ((status & IResourceDelta.ADDED) != 0) {
			if (parent instanceof IPackageFragment) 
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent));
			else
				postAdd(parent, resource);
		}
		int changeFlags= delta.getFlags();
		IResourceDelta[] affectedChildren= delta.getAffectedChildren();
		
		if (affectedChildren.length > 1) {
			// more than one child changed, refresh from here downwards
			postRefresh(resource);
			return;
		}

		for (int i= 0; i < affectedChildren.length; i++)
			processResourceDelta(affectedChildren[i], resource, output);
	}
	
	private void postRefresh(final Object root) {
		postRunnable(new Runnable() {
				public void run() {
					fViewer.refresh(root);
				}
		});
	}
	
	private void postAdd(final Object parent, final Object element) {
		postRunnable(new Runnable() {
				public void run() {
					fViewer.add(parent, element);
				}
		});
	}

	private void postRemove(final Object element) {
		postRunnable(new Runnable() {
				public void run() {
					fViewer.remove(element);
				}
		});
	}

	private void postRunnable(final Runnable r) {
		Control ctrl= fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(r);
		}
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
				return false;
			}
		}
		Object[] children= getChildren(element);
		return (children != null) && children.length > 0;
	}
		
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
				if (list.size() == 0)
					System.out.println("No children found");	
			}
			else if (hasChildren(root)/*root.hasChildren()*/) {
				list.add(root);
			} else {
				System.out.println("Root doesn't have children");
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
			return folder.members();
		} catch(CoreException e) {
			return ArrayUtility.getEmptyArray();
		}
	}
	
	private boolean isClassPathChange(IJavaElementDelta delta) {
		int flags= delta.getFlags();
		return (delta.getKind() == IJavaElementDelta.CHANGED && 
			((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_CLASSPATH_REORDER) != 0));
	}
	
	private Object fixupProjectPackageFragmentRoot(Object element) {
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
	
	private boolean isPackageFragmentEmpty(IJavaElement element) throws JavaModelException {
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment)element;
			if (!(fragment.hasChildren() || fragment.getNonJavaResources().length > 0) && fragment.hasSubpackages()) 
				return true;
		}
		return false;
	}

	private boolean isProjectPackageFragmentRoot(IPackageFragmentRoot root) throws JavaModelException {
		IResource resource= root.getUnderlyingResource();
		return (resource instanceof IProject);
	}
	
	private boolean exists(Object element) {
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
	
	private Object internalGetParent(Object element) {
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