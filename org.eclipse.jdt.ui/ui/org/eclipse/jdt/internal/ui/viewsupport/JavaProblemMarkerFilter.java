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

public class JavaProblemMarkerFilter implements IResourceChangeListener {
	private HashSet fChangedElements; 
	private ArrayList fListeners;
	
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
		
	private class ProjectErrorVisitor implements IResourceDeltaVisitor {
		
		private PkgFragmentRootErrorVisitor fPkgFragmentRootErrorVisitor;
		
		public ProjectErrorVisitor() {
			fPkgFragmentRootErrorVisitor= new PkgFragmentRootErrorVisitor();
		}
			
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource res= delta.getResource();
			if (res instanceof IProject) {
				checkInvalidate(delta, res.getFullPath());
				try {
					IJavaProject jProject= getJavaProject((IProject)res);
					if (jProject != null) {
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

		private IJavaProject getJavaProject(IProject proj) throws CoreException {
			if (proj.hasNature(JavaCore.NATURE_ID) && proj.isOpen()) {
				return JavaCore.create(proj);
			}
			return null;
		}		
		
	}
	
	public JavaProblemMarkerFilter() {
		fListeners= new ArrayList(3);
	}


	private void checkInvalidate(IResourceDelta delta, IPath r) {
		if (delta.getKind() == IResourceDelta.REMOVED) {
			invalidate(r);
		} else if (delta.getKind() == IResourceDelta.CHANGED && isErrorDelta(delta)) {
			invalidate(r);
		} 
	}
	
	private void invalidate(IPath path) {
		if (path != null) {
			while (path.segmentCount() > 0) {
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
	
	/**
	 * @see IResourceChangeListener#resourceChanged
	 */	
	public void resourceChanged(IResourceChangeEvent event) {
		beginChange();
		try {
			IResourceDelta delta= event.getDelta();
			if (delta != null)
				delta.accept(new ProjectErrorVisitor());
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}
		endChange();
	}
	
	// change propagation
	private void beginChange() {
		fChangedElements= new HashSet();
	}
	  
	private void endChange() { 
		fireChanges(fChangedElements);
		fChangedElements= null;
	}	

	
	public void addListener(IJavaProblemListener listener) {
		if (fListeners.isEmpty()) { 
			JavaPlugin.getWorkspace().addResourceChangeListener(this);
		}
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}
	
	public void removeListener(IJavaProblemListener listener) {
		fListeners.remove(listener);
		if (fListeners.isEmpty()) {
			JavaPlugin.getWorkspace().removeResourceChangeListener(this);
		}
	}
	
	private void fireChanges(Set changes) {
		for (int i= 0; i < fListeners.size(); i++) {
			IJavaProblemListener curr= (IJavaProblemListener)fListeners.get(i);
			curr.severitiesChanged(changes);
		}			
	}

}