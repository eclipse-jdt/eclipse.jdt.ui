/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.util.ListenerList;

/**
 * Listens to resource deltas and filters for marker changes of type
 * IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER and IJavaModelMarker.BUILDPATH_PROBLEM_MARKER.
 * Viewers showing error ticks should register as listener to
 * this type.
 */
public class ProblemMarkerManager implements IResourceChangeListener {

	/**
	 * Visitors used to filter the element delta changes
	 */	private static class ProjectErrorVisitor implements IResourceDeltaVisitor {

		private class PkgFragmentRootErrorVisitor implements IResourceDeltaVisitor {
			
			private IPath fRoot;
			private boolean fInsideRoot;
			
			public PkgFragmentRootErrorVisitor() {
			}
			
			public void init(IPath rootPath) {
				fRoot= rootPath;
				fInsideRoot= false;
			}
			
			public boolean visit(IResourceDelta delta) throws CoreException {
				IPath path= delta.getFullPath();
				
				if (!fInsideRoot) {
					if (fRoot.equals(path)) {
						fInsideRoot= true;
						return true;
					}
					return (path.isPrefixOf(fRoot));
				}
				checkInvalidate(delta, path);
				return true;
			}
		}

		private PkgFragmentRootErrorVisitor fPkgFragmentRootErrorVisitor;
		private HashSet fChangedElements; 
		
		public ProjectErrorVisitor(HashSet changedElements) {
			fPkgFragmentRootErrorVisitor= new PkgFragmentRootErrorVisitor();
			fChangedElements= changedElements;
		}
			
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource res= delta.getResource();
			if (res instanceof IProject && delta.getKind() == IResourceDelta.CHANGED) {
				try {
					IJavaProject jProject= getJavaProject((IProject)res);
					if (jProject != null) {
						checkInvalidate(delta, res.getFullPath());
						
						IClasspathEntry[] cps= jProject.getRawClasspath();
						for (int i= 0; i < cps.length; i++) {
							if (cps[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								fPkgFragmentRootErrorVisitor.init(cps[i].getPath());
								delta.accept(fPkgFragmentRootErrorVisitor);
							}
						}
					}
				} catch (CoreException e) {
					JavaPlugin.log(e.getStatus());
				}
				return false;
			}
			return true;
		}
		
		private void checkInvalidate(IResourceDelta delta, IPath path) {
			int kind= delta.getKind();
			if (kind == IResourceDelta.REMOVED  || (kind == IResourceDelta.CHANGED && isErrorDelta(delta))) {
				// invalidate the path and all parent paths
				while (!path.isEmpty() && !path.isRoot()) {
					fChangedElements.add(path);
					path= path.removeLastSegments(1);
				}
			} 
		}	
		
		private boolean isErrorDelta(IResourceDelta delta) {	
			if ((delta.getFlags() & IResourceDelta.MARKERS) != 0) {
				IMarkerDelta[] markerDeltas= delta.getMarkerDeltas();
				for (int i= 0; i < markerDeltas.length; i++) {
					if (markerDeltas[i].isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER) ||
							markerDeltas[i].isSubtypeOf(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER)) {
						int kind= markerDeltas[i].getKind();
						if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED)
							return true;
						int severity= markerDeltas[i].getAttribute(IMarker.SEVERITY, -1);
						int newSeverity= markerDeltas[i].getMarker().getAttribute(IMarker.SEVERITY, -1);
						if (newSeverity != severity)
							return true; 
					}
				}
			}
			return false;
		}			

		private IJavaProject getJavaProject(IProject proj) throws CoreException {
			if (proj.hasNature(JavaCore.NATURE_ID) && proj.isOpen()) {
				return JavaCore.create(proj);
			}
			return null;
		}		
		
	}

	private ListenerList fListeners;
	
	
	public ProblemMarkerManager() {
		fListeners= new ListenerList(5);
	}

	/*
	 * @see IResourceChangeListener#resourceChanged
	 */	
	public void resourceChanged(IResourceChangeEvent event) {
		HashSet changedElements= new HashSet();
		
		try {
			IResourceDelta delta= event.getDelta();
			if (delta != null)
				delta.accept(new ProjectErrorVisitor(changedElements));
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}

		if (changedElements.size() > 0) {
			fireChanges(changedElements);
		}
	}
	
	/**
	 * Adds a listener for problem marker changes.
	 */
	public void addListener(IProblemChangedListener listener) {
		if (fListeners.isEmpty()) { 
			JavaPlugin.getWorkspace().addResourceChangeListener(this);
		}
		fListeners.add(listener);
	}

	/**
	 * Removes a <code>IProblemChangedListener</code>.
	 */	
	public void removeListener(IProblemChangedListener listener) {
		fListeners.remove(listener);
		if (fListeners.isEmpty()) {
			JavaPlugin.getWorkspace().removeResourceChangeListener(this);
		}
	}
	
	private void fireChanges(Set changes) {
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			IProblemChangedListener curr= (IProblemChangedListener) listeners[i];
			curr.problemsChanged(changes);
		}			
	}

}