/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
 
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
class PackageExplorerContentProvider extends StandardJavaElementContentProvider implements ITreeContentProvider, IElementChangedListener {
	
	protected TreeViewer fViewer;
	protected Object fInput;

	private boolean fIsFlatLayout;
	private PackageFragmentProvider fPackageFragmentProvider= new PackageFragmentProvider();


	/**
	 * Creates a new content provider for Java elements.
	 */
	public PackageExplorerContentProvider() {
	}

	/**
	 * Creates a new content provider for Java elements.
	 */
	public PackageExplorerContentProvider(boolean provideMembers, boolean provideWorkingCopy) {
		super(provideMembers, provideWorkingCopy);	
	}
	
	/* (non-Javadoc)
	 * Method declared on IElementChangedListener.
	 */
	public void elementChanged(final ElementChangedEvent event) {
		try {
			processDelta(event.getDelta());
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void dispose() {
		super.dispose();
		JavaCore.removeElementChangedListener(this);
		fPackageFragmentProvider.dispose();
	}

	// ------ Code which delegates to PackageFragmentProvider ------

	private boolean needsToDelegate(Object element) {
		int type= -1;
		if (element instanceof IJavaElement)
			type= ((IJavaElement)element).getElementType();
		return (!fIsFlatLayout && (type == IJavaElement.PACKAGE_FRAGMENT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT || type == IJavaElement.JAVA_PROJECT));
	}		

	public Object[] getChildren(Object parentElement) {
		if (needsToDelegate(parentElement)) {
			Object[] packageFragments= fPackageFragmentProvider.getChildren(parentElement);
			return getWithParentsResources(packageFragments, parentElement);
		} else
			return super.getChildren(parentElement);
	}

	public Object getParent(Object child) {
		if (needsToDelegate(child)) {
			return fPackageFragmentProvider.getParent(child);
		} else
			return super.getParent(child);
	}

	/**
	 * Returns the given objects with the resources of the parent.
	 */
	private Object[] getWithParentsResources(Object[] existingObject, Object parent) {
		Object[] objects= super.getChildren(parent);
		List list= new ArrayList();
		// Add everything that is not a PackageFragment
		for (int i= 0; i < objects.length; i++) {
			Object object= objects[i];
			if (!(object instanceof IPackageFragment)) {
				list.add(object);
			}
		}
		if (existingObject != null)
			list.addAll(Arrays.asList(existingObject));
		return list.toArray();
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
	public void processDelta(IJavaElementDelta delta) throws JavaModelException {

		int kind= delta.getKind();
		int flags= delta.getFlags();
		IJavaElement element= delta.getElement();
		
		if(element.getElementType()!= IJavaElement.JAVA_MODEL && element.getElementType()!= IJavaElement.JAVA_PROJECT){
			IJavaProject proj= element.getJavaProject();
			if (proj == null || !proj.getProject().isOpen())
				return;	
		}
		
		if (!fIsFlatLayout && element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			fPackageFragmentProvider.processDelta(delta);
			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();			
			if(affectedChildren.length > 1)	
				postRefresh(element);
			else
				processAffectedChildren(affectedChildren);
			return;
		}
			

		if (!getProvideWorkingCopy() && isWorkingCopy(element))
			return; 

		if (element != null && element.getElementType() == IJavaElement.COMPILATION_UNIT && !isOnClassPath((ICompilationUnit)element))
			return;
			 
		// handle open and closing of a solution or project
		if (((flags & IJavaElementDelta.F_CLOSED) != 0) || ((flags & IJavaElementDelta.F_OPENED) != 0)) {			
			postRefresh(element);
			return;
		}

		if (kind == IJavaElementDelta.REMOVED) {
			// when a working copy is removed all we have to do
			// is to refresh the compilation unit
			if (isWorkingCopy(element)) {
				refreshWorkingCopy((IWorkingCopy)element);
				return;
			}
			Object parent= internalGetParent(element);			
			postRemove(element);
			if (parent instanceof IPackageFragment) 
				postUpdateIcon((IPackageFragment)parent);
			// we are filtering out empty subpackages, so we
			// a package becomes empty we remove it from the viewer. 
			if (isPackageFragmentEmpty(element.getParent())) {
				if (fViewer.testFindItem(parent) != null)
					postRefresh(internalGetParent(parent));
			}  
			return;
		}

		if (kind == IJavaElementDelta.ADDED) { 
			// when a working copy is added all we have to do
			// is to refresh the compilation unit
			if (isWorkingCopy(element)) {
				refreshWorkingCopy((IWorkingCopy)element);
				return;
			}
			Object parent= internalGetParent(element);
			// we are filtering out empty subpackages, so we
			// have to handle additions to them specially. 
			if (parent instanceof IPackageFragment) {
				Object grandparent= internalGetParent(parent);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an unvisible parent
				if (parent.equals(fInput)) {
					postRefresh(parent);
				} else {
					// refresh from grandparent if parent isn't visible yet
					if (fViewer.testFindItem(parent) == null)
						postRefresh(grandparent);
					else {
						postRefresh(parent);
					}	
				}
				return;				
			} else {  
				postAdd(parent, element);
			}
		}

		if (element instanceof ICompilationUnit) {
			if (getProvideWorkingCopy()) {
				IJavaElement original= ((IWorkingCopy)element).getOriginalElement();
				if (original != null)
					element= original;
			}
			if (kind == IJavaElementDelta.CHANGED) {
				postRefresh(element);
				updateSelection(delta);
				return;
			}
		}
		// we don't show the contents of a compilation or IClassFile, so don't go any deeper
		if ((element instanceof ICompilationUnit) || (element instanceof IClassFile))
			return;
			
		// the contents of an external JAR has changed
		if (element instanceof IPackageFragmentRoot && ((flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0)) {
			postRefresh(element);
			return;
		}
		// the source attachment of a JAR has changed
		if (element instanceof IPackageFragmentRoot && (((flags & IJavaElementDelta.F_SOURCEATTACHED) != 0 || ((flags & IJavaElementDelta.F_SOURCEDETACHED)) != 0)))
			postUpdateIcon(element);
			
		if (isClassPathChange(delta)) {
			 // throw the towel and do a full refresh of the affected java project. 
			postRefresh(element.getJavaProject());
			return;
		}
		
		if (processResourceDeltas(delta.getResourceDeltas(), element))
			return;

		handleAffectedChildren(delta, element);
	}
		
	private void handleAffectedChildren(IJavaElementDelta delta, IJavaElement element) throws JavaModelException {
		
		IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
		if (affectedChildren.length > 1) {
			// a package fragment might become non empty refresh from the parent
			if (element instanceof IPackageFragment) {
				IJavaElement parent= (IJavaElement)internalGetParent(element);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an unvisible parent
				if (element.equals(fInput)) {
					postRefresh(element);
				} else {
					postRefresh(parent);
				}
				return;
			}
			// more than one child changed, refresh from here downwards
			if (element instanceof IPackageFragmentRoot)
				postRefresh(skipProjectPackageFragmentRoot((IPackageFragmentRoot)element));
			else
				postRefresh(element);
			return;
		}
		processAffectedChildren(affectedChildren);
	}
	
	protected void processAffectedChildren(IJavaElementDelta[] affectedChildren) throws JavaModelException {
		for (int i= 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i]);
		}
	}

	private boolean isOnClassPath(ICompilationUnit element) throws JavaModelException {
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
	 * Refreshes the Compilation unit corresponding to the workging copy
	 * @param iWorkingCopy
	 */
	private void refreshWorkingCopy(IWorkingCopy workingCopy) {
		IJavaElement original= workingCopy.getOriginalElement();
		if (original != null)
			postRefresh(original);
	}

	private boolean isWorkingCopy(IJavaElement element) {
		return (element instanceof IWorkingCopy) && ((IWorkingCopy)element).isWorkingCopy();
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
1	 * Process a resource delta.
	 * 
	 * @return true if the parent got refreshed
	 */
	private boolean processResourceDelta(IResourceDelta delta, Object parent) {
		int status= delta.getKind();
		IResource resource= delta.getResource();
		// filter out changes affecting the output folder
		if (resource == null)
			return false;	
			
		// this could be optimized by handling all the added children in the parent
		if ((status & IResourceDelta.REMOVED) != 0) {
			if (parent instanceof IPackageFragment) {
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent));
				return true;
			} else 
				postRemove(resource);
		}
		if ((status & IResourceDelta.ADDED) != 0) {
			if (parent instanceof IPackageFragment) {
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent));	
				return true;
			} else
				postAdd(parent, resource);
		}
		
		processResourceDeltas(delta.getAffectedChildren(), resource);
		return false;
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
			postRefresh(parent);
			return true;
		}

		for (int i= 0; i < deltas.length; i++) {
			if (processResourceDelta(deltas[i], parent))
				return true;
		}

		return false;
	}
	
	private void postRefresh(final Object root) {
		postRunnable(new Runnable() {
			public void run() {
				// 1GF87WR: ITPUI:ALL - SWTEx + NPE closing a workbench window.
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()){
					fViewer.refresh(root);
				}
			}
		});
	}
	
	private void postAdd(final Object parent, final Object element) {
		postRunnable(new Runnable() {
			public void run() {
				// 1GF87WR: ITPUI:ALL - SWTEx + NPE closing a workbench window.
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()){
					fViewer.add(parent, element);
				}
			}
		});
	}

	private void postRemove(final Object element) {
		postRunnable(new Runnable() {
			public void run() {
				// 1GF87WR: ITPUI:ALL - SWTEx + NPE closing a workbench window.
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					fViewer.remove(element);
				}
			}
		});
	}

	private void postRunnable(final Runnable r) {
		Control ctrl= fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(r); 
		}
	}

	void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
	}
}