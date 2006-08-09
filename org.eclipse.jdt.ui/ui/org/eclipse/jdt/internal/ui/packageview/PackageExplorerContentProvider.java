/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;
 
/**
 * Content provider for the PackageExplorer.
 * 
 * <p>
 * Since 2.1 this content provider can provide the children for flat or hierarchical
 * layout. The hierarchical layout is done by delegating to the <code>PackageFragmentProvider</code>.
 * </p>
 * 
 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider
 * @see org.eclipse.jdt.internal.ui.packageview.PackageFragmentProvider
 */
public class PackageExplorerContentProvider extends StandardJavaElementContentProvider implements ITreeContentProvider, IElementChangedListener {
	
	protected static final int ORIGINAL= 0;
	protected static final int PARENT= 1 << 0;
	protected static final int GRANT_PARENT= 1 << 1;
	protected static final int PROJECT= 1 << 2;
	
	private TreeViewer fViewer;
	private Object fInput;
	private boolean fIsFlatLayout;
	private PackageFragmentProvider fPackageFragmentProvider;
	
	private int fPendingChanges;
	
	/**
	 * Creates a new content provider for Java elements.
	 */
	public PackageExplorerContentProvider(boolean provideMembers) {
		super(provideMembers);
		fPackageFragmentProvider= new PackageFragmentProvider();
	}
		
	/* package */ PackageFragmentProvider getPackageFragmentProvider() {
		return fPackageFragmentProvider;
	}
	
	protected Object getViewerInput() {
		return fInput;
	}
	
	/* (non-Javadoc)
	 * Method declared on IElementChangedListener.
	 */
	public void elementChanged(final ElementChangedEvent event) {
		try {
			// 58952 delete project does not update Package Explorer [package explorer] 
			// if the input to the viewer is deleted then refresh to avoid the display of stale elements
			if (inputDeleted())
				return;
			processDelta(event.getDelta());
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	private boolean inputDeleted() {
		if (fInput == null)
			return false;
		if ((fInput instanceof IJavaElement) && ((IJavaElement) fInput).exists())
			return false;
		if ((fInput instanceof IResource) && ((IResource) fInput).exists())
			return false;
		if (fInput instanceof WorkingSetModel)
			return false;
		postRefresh(fInput, ORIGINAL, fInput);
		return true;
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void dispose() {
		super.dispose();
		JavaCore.removeElementChangedListener(this);
		fPackageFragmentProvider.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider#getPackageFragmentRootContent(org.eclipse.jdt.core.IPackageFragmentRoot)
	 */
	protected Object[] getPackageFragmentRootContent(IPackageFragmentRoot root) throws JavaModelException {
		if (fIsFlatLayout) {
			return super.getPackageFragmentRootContent(root);
		}
		
		// hierarchical package mode
		ArrayList result= new ArrayList();
		fPackageFragmentProvider.getHierarchicalPackageChildren(root, null, result);
		if (!isProjectPackageFragmentRoot(root)) {
			Object[] nonJavaResources= root.getNonJavaResources();
			for (int i= 0; i < nonJavaResources.length; i++) {
				result.add(nonJavaResources[i]);
			}
		}
		return result.toArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider#getPackageContent(org.eclipse.jdt.core.IPackageFragment)
	 */
	protected Object[] getPackageContent(IPackageFragment fragment) throws JavaModelException {
		if (fIsFlatLayout) {
			return super.getPackageContent(fragment);
		}
		
		// hierarchical package mode
		ArrayList result= new ArrayList();
		
		fPackageFragmentProvider.getHierarchicalPackageChildren((IPackageFragmentRoot) fragment.getParent(), fragment, result);
		Object[] nonPackages= super.getPackageContent(fragment);
		if (result.isEmpty())
			return nonPackages;
		for (int i= 0; i < nonPackages.length; i++) {
			result.add(nonPackages[i]);
		}
		return result.toArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider#getFolderContent(org.eclipse.core.resources.IFolder)
	 */
	protected Object[] getFolderContent(IFolder folder) throws CoreException {
		if (fIsFlatLayout) {
			return super.getFolderContent(folder);
		}
		
		// hierarchical package mode
		ArrayList result= new ArrayList();
		
		fPackageFragmentProvider.getHierarchicalPackagesInFolder(folder, result);
		Object[] others= super.getFolderContent(folder);
		if (result.isEmpty())
			return others;
		for (int i= 0; i < others.length; i++) {
			result.add(others[i]);
		}
		return result.toArray();
	}
	
	
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaModel) 
				return concatenate(getJavaProjects((IJavaModel)parentElement), getNonJavaProjects((IJavaModel)parentElement));

			if (parentElement instanceof ClassPathContainer)
				return getContainerPackageFragmentRoots((ClassPathContainer)parentElement);
				
			if (parentElement instanceof IProject) 
				return ((IProject)parentElement).members();
			
			return super.getChildren(parentElement);
		} catch (CoreException e) {
			return NO_CHILDREN;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider#getPackageFragmentRoots(org.eclipse.jdt.core.IJavaProject)
	 */
	protected Object[] getPackageFragmentRoots(IJavaProject project) throws JavaModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;
			
		List result= new ArrayList();

		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			IClasspathEntry classpathEntry= root.getRawClasspathEntry();
			if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
				if (isProjectPackageFragmentRoot(root)) {
					// filter out package fragments that correspond to projects and
					// replace them with the package fragments directly
					Object[] fragments= getPackageFragmentRootContent(root);
					for (int j= 0; j < fragments.length; j++) {
						result.add(fragments[j]);
					}
				} else {
					result.add(root);
				}
			}
		}
		
		// separate loop to make sure all containers are on the classpath
		IClasspathEntry[] rawClasspath= project.getRawClasspath();
		for (int i= 0; i < rawClasspath.length; i++) {
			IClasspathEntry classpathEntry= rawClasspath[i];
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				result.add(new ClassPathContainer(project, classpathEntry));
			}	
		}	
		Object[] resources= project.getNonJavaResources();
		for (int i= 0; i < resources.length; i++) {
			result.add(resources[i]);
		}
		return result.toArray();
	}

	private Object[] getContainerPackageFragmentRoots(ClassPathContainer container) {
		return container.getChildren(container);
	}

	private Object[] getNonJavaProjects(IJavaModel model) throws JavaModelException {
		return model.getNonJavaResources();
	}

	protected Object internalGetParent(Object element) {

		if (!fIsFlatLayout && element instanceof IPackageFragment) {
			return fPackageFragmentProvider.getHierarchicalPackageParent((IPackageFragment) element);
		} else if (element instanceof IPackageFragmentRoot) {
			// since we insert logical package containers we have to fix
			// up the parent for package fragment roots so that they refer
			// to the container and containers refere to the project
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			IJavaProject project= root.getJavaProject();
			try {
				IClasspathEntry[] entries= project.getRawClasspath();
				for (int i= 0; i < entries.length; i++) {
					IClasspathEntry entry= entries[i];
					if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						if (ClassPathContainer.contains(project, entry, root)) 
							return new ClassPathContainer(project, entry);
					}
				}
			} catch (JavaModelException e) {
				// fall through
			}
		} else if (element instanceof ClassPathContainer) {
			return ((ClassPathContainer)element).getJavaProject();
		}
		return super.internalGetParent(element);
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		fPackageFragmentProvider.inputChanged(viewer, oldInput, newInput);
		fViewer= (TreeViewer)viewer;
		if (oldInput == null && newInput != null) {
			JavaCore.addElementChangedListener(this); 
		} else if (oldInput != null && newInput == null) {
			JavaCore.removeElementChangedListener(this); 
		}
		fInput= newInput;
	}

	// ------ delta processing ------

	/**
	 * Processes a delta recursively. When more than two children are affected the
	 * tree is fully refreshed starting at this node. The delta is processed in the
	 * current thread but the viewer updates are posted to the UI thread.
	 */
	private void processDelta(IJavaElementDelta delta) throws JavaModelException {
	
		int kind= delta.getKind();
		int flags= delta.getFlags();
		IJavaElement element= delta.getElement();
		int elementType= element.getElementType();
		
		
		if (elementType != IJavaElement.JAVA_MODEL && elementType != IJavaElement.JAVA_PROJECT) {
			IJavaProject proj= element.getJavaProject();
			if (proj == null || !proj.getProject().isOpen()) // TODO: Not needed if parent already did the 'open' check!
				return;	
		}
		
		if (!fIsFlatLayout && elementType == IJavaElement.PACKAGE_FRAGMENT) {
			fPackageFragmentProvider.processDelta(delta);
			if (processResourceDeltas(delta.getResourceDeltas(), element))
			    return;
			handleAffectedChildren(delta, element);
			return;
		}
		
		if (elementType == IJavaElement.COMPILATION_UNIT) {
			ICompilationUnit cu= (ICompilationUnit) element;
			if (!JavaModelUtil.isPrimary(cu)) {
				return;
			}
						
			if (!getProvideMembers() && cu.isWorkingCopy() && kind == IJavaElementDelta.CHANGED) {
				return;
			}
			
			if ((kind == IJavaElementDelta.CHANGED) && !isStructuralCUChange(flags)) {
				return; // test moved ahead
			}
			
			if (!isOnClassPath(cu)) { // TODO: isOnClassPath expensive! Should be put after all cheap tests
				return;
			}
			
		}
		
		if (elementType == IJavaElement.JAVA_PROJECT) {
			// handle open and closing of a project
			if ((flags & (IJavaElementDelta.F_CLOSED | IJavaElementDelta.F_OPENED)) != 0) {			
				postRefresh(element, ORIGINAL, element);
				return;
			}
			// if the raw class path has changed we refresh the entire project
			if ((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
				postRefresh(element, ORIGINAL, element);
				return;				
			}
		}
	
		if (kind == IJavaElementDelta.REMOVED) {
			Object parent= internalGetParent(element);			
			if (element instanceof IPackageFragment) {
				// refresh package fragment root to allow filtering empty (parent) packages: bug 72923
				if (fViewer.testFindItem(parent) != null)
					postRefresh(parent, PARENT, element);
				return;
			}
			
			postRemove(element);
			if (parent instanceof IPackageFragment) 
				postUpdateIcon((IPackageFragment)parent);
			// we are filtering out empty subpackages, so we
			// a package becomes empty we remove it from the viewer. 
			if (isPackageFragmentEmpty(element.getParent())) {
				if (fViewer.testFindItem(parent) != null)
					postRefresh(internalGetParent(parent), GRANT_PARENT, element);
			}  
			return;
		}
	
		if (kind == IJavaElementDelta.ADDED) { 
			Object parent= internalGetParent(element);
			// we are filtering out empty subpackages, so we
			// have to handle additions to them specially. 
			if (parent instanceof IPackageFragment) {
				Object grandparent= internalGetParent(parent);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an unvisible parent
				if (parent.equals(fInput)) {
					postRefresh(parent, PARENT, element);
				} else {
					// refresh from grandparent if parent isn't visible yet
					if (fViewer.testFindItem(parent) == null)
						postRefresh(grandparent, GRANT_PARENT, element);
					else {
						postRefresh(parent, PARENT, element);
					}	
				}
				return;				
			} else {  
				postAdd(parent, element);
			}
		}
	
		if (elementType == IJavaElement.COMPILATION_UNIT) {
			if (kind == IJavaElementDelta.CHANGED) {
				// isStructuralCUChange already performed above
				postRefresh(element, ORIGINAL, element);
				updateSelection(delta);
			}
			return;
		}
		// no changes possible in class files
		if (elementType == IJavaElement.CLASS_FILE)
			return;
		
		
		if (elementType == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
			// the contents of an external JAR has changed
			if ((flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0) {
				postRefresh(element, ORIGINAL, element);
				return;
			}
			// the source attachment of a JAR has changed
			if ((flags & (IJavaElementDelta.F_SOURCEATTACHED | IJavaElementDelta.F_SOURCEDETACHED)) != 0)
				postUpdateIcon(element);
			
			if (isClassPathChange(delta)) {
				 // throw the towel and do a full refresh of the affected java project. 
				postRefresh(element.getJavaProject(), PROJECT, element);
				return;
			}
		}
		
		if (processResourceDeltas(delta.getResourceDeltas(), element))
			return;
	
		handleAffectedChildren(delta, element);
	}
	
	private static boolean isStructuralCUChange(int flags) {
		// No refresh on working copy creation (F_PRIMARY_WORKING_COPY)
		return ((flags & IJavaElementDelta.F_CHILDREN) != 0) || ((flags & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_FINE_GRAINED)) == IJavaElementDelta.F_CONTENT);
	}
	
	/* package */ void handleAffectedChildren(IJavaElementDelta delta, IJavaElement element) throws JavaModelException {
		IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
		if (affectedChildren.length > 1) {
			// a package fragment might become non empty refresh from the parent
			if (element instanceof IPackageFragment) {
				IJavaElement parent= (IJavaElement)internalGetParent(element);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an unvisible parent
				if (element.equals(fInput)) {
					postRefresh(element, ORIGINAL, element);
				} else {
					postRefresh(parent, PARENT, element);
				}
				return;
			}
			// more than one child changed, refresh from here downwards
			if (element instanceof IPackageFragmentRoot) {
				Object toRefresh= skipProjectPackageFragmentRoot((IPackageFragmentRoot)element);
				postRefresh(toRefresh, ORIGINAL, toRefresh);
			} else {
				postRefresh(element, ORIGINAL, element);
			}
			return;
		}
		processAffectedChildren(affectedChildren);
	}
	
	protected void processAffectedChildren(IJavaElementDelta[] affectedChildren) throws JavaModelException {
		for (int i= 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i]);
		}
	}

	private boolean isOnClassPath(ICompilationUnit element) {
		IJavaProject project= element.getJavaProject();
		if (project == null || !project.exists())
			return false;
		return project.isOnClasspath(element);
	}

	/**
	 * Updates the selection. It finds newly added elements
	 * and selects them.
	 */
	private void updateSelection(IJavaElementDelta delta) {
		final IJavaElement addedElement= findAddedElement(delta);
		if (addedElement != null) {
			final StructuredSelection selection= new StructuredSelection(addedElement);
			postRunnable(new Runnable() {
				public void run() {
					Control ctrl= fViewer.getControl();
					if (ctrl != null && !ctrl.isDisposed()) {
						// 19431
						// if the item is already visible then select it
						if (fViewer.testFindItem(addedElement) != null)
							fViewer.setSelection(selection);
					}
				}
			});	
		}	
	}

	private IJavaElement findAddedElement(IJavaElementDelta delta) {
		if (delta.getKind() == IJavaElementDelta.ADDED)  
			return delta.getElement();
		
		IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
		for (int i= 0; i < affectedChildren.length; i++) 
			return findAddedElement(affectedChildren[i]);
			
		return null;
	}

	/**
	 * Updates the package icon
	 */
	 private void postUpdateIcon(final IJavaElement element) {
	 	postRunnable(new Runnable() {
			public void run() {
				// 1GF87WR: ITPUI:ALL - SWTEx + NPE closing a workbench window.
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) 
					fViewer.update(element, new String[]{IBasicPropertyConstants.P_IMAGE});
			}
		});
	 }

	/**
	 * Process a resource delta.
	 * 
	 * @return true if the parent got refreshed
	 */
	private boolean processResourceDelta(IResourceDelta delta, Object parent) {
		int status= delta.getKind();
		int flags= delta.getFlags();
		
		IResource resource= delta.getResource();
		// filter out changes affecting the output folder
		if (resource == null)
			return false;	
			
		// this could be optimized by handling all the added children in the parent
		if ((status & IResourceDelta.REMOVED) != 0) {
			if (parent instanceof IPackageFragment) {
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent), PARENT, parent);
				return true;
			} else 
				postRemove(resource);
		}
		if ((status & IResourceDelta.ADDED) != 0) {
			if (parent instanceof IPackageFragment) {
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent), PARENT, parent);	
				return true;
			} else
				postAdd(parent, resource);
		}
		// open/close state change of a project
		if ((flags & IResourceDelta.OPEN) != 0) {
			postProjectStateChanged(internalGetParent(parent));
			return true;		
		}
		processResourceDeltas(delta.getAffectedChildren(), resource);
		return false;
	}
	
	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
	}
	/**
	 * Process resource deltas.
	 *
	 * @return true if the parent got refreshed
	 */
	private boolean processResourceDeltas(IResourceDelta[] deltas, Object parent) {
		if (deltas == null)
			return false;
		
		if (deltas.length > 1) {
			// more than one child changed, refresh from here downwards
			postRefresh(parent, ORIGINAL, parent);
			return true;
		}

		for (int i= 0; i < deltas.length; i++) {
			if (processResourceDelta(deltas[i], parent))
				return true;
		}

		return false;
	}

	private void postRefresh(Object root, int relation, Object affectedElement) {
		// JFace doesn't refresh when object isn't part of the viewer
		// Therefore move the refresh start down to the viewer's input
		if (isParent(root, fInput)) 
			root= fInput;
		List toRefresh= new ArrayList(1);
		toRefresh.add(root);
		augmentElementToRefresh(toRefresh, relation, affectedElement);
		postRefresh(toRefresh, true);
	}
	
	protected void augmentElementToRefresh(List toRefresh, int relation, Object affectedElement) {
	}

	boolean isParent(Object root, Object child) {
		Object parent= getParent(child);
		if (parent == null)
			return false;
		if (parent.equals(root))
			return true;
		return isParent(root, parent);
	}

	protected void postRefresh(final List toRefresh, final boolean updateLabels) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					for (Iterator iter= toRefresh.iterator(); iter.hasNext();) {
						fViewer.refresh(iter.next(), updateLabels);
					}
				}
			}
		});
	}

	protected void postAdd(final Object parent, final Object element) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()){
					// TODO workaround for 39754 New projects being added to the TreeViewer twice
					if (fViewer.testFindItem(element) == null) 
						fViewer.add(parent, element);
				}
			}
		});
	}

	protected void postRemove(final Object element) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					fViewer.remove(element);
				}
			}
		});
	}

	protected void postProjectStateChanged(final Object root) {
		postRunnable(new Runnable() {
			public void run() {
				//fPart.projectStateChanged(root); 
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					fViewer.refresh(root, true);
					// trigger a syntetic selection change so that action refresh their
					// enable state.
					fViewer.setSelection(fViewer.getSelection());
				}
			}
		});
	}

	/* package */ void postRunnable(final Runnable r) {
		Control ctrl= fViewer.getControl();
		final Runnable trackedRunnable= new Runnable() {
			public void run() {
				try {
					r.run();
				} finally {
					removePendingChange();
				}
			}
		};
		if (ctrl != null && !ctrl.isDisposed()) {
			addPendingChange();
			try {
				ctrl.getDisplay().asyncExec(trackedRunnable); 
			} catch (RuntimeException e) {
				removePendingChange();
				throw e;
			} catch (Error e) {
				removePendingChange();
				throw e; 
			}
		}
	}

	// ------ Pending change management due to the use of asyncExec in postRunnable.
	
	public synchronized boolean hasPendingChanges() {
		return fPendingChanges > 0;  
	}
	
	private synchronized void addPendingChange() {
		fPendingChanges++;
	}

	synchronized void removePendingChange() {
		fPendingChanges--;
		if (fPendingChanges < 0)
			fPendingChanges= 0;
	}
}
