/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.HashSet;import java.util.Iterator;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IMarkerDelta;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IResourceChangeEvent;import org.eclipse.core.resources.IResourceChangeListener;import org.eclipse.core.resources.IResourceDelta;import org.eclipse.core.resources.IResourceDeltaVisitor;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaElement; import org.eclipse.jdt.core.IJavaModelMarker;import org.eclipse.jdt.core.IJavaProject; import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;
public class ErrorTickManager implements IResourceChangeListener {

	protected static final int SEVERITY_WARNING= 0;
	protected static final int SEVERITY_ERROR= 1; 

	private HashSet fChangedElements; 
	private HashSet fListeners;
	private static final boolean[] fgEmpty= new boolean[2];
	
	class PkgFragmentRootErrorVisitor implements IResourceDeltaVisitor {
		private IPath fRoot;
		private boolean fInsideRoot= false;
		PkgFragmentRootErrorVisitor(IPath root) {
			fRoot= root;
		}
		
		public boolean visit(IResourceDelta delta) throws CoreException {
			IPath r= delta.getFullPath();
			
			if (!fInsideRoot) {
				if (fRoot.equals(r)) {
					fInsideRoot= true;
					return true;
				} else {
					if (r.isPrefixOf(fRoot))  
						return true;
				}
				return false;
			}
			checkInvalidate(delta, r);
			return true;
		}
	}
	
	private void checkInvalidate(IResourceDelta delta, IPath r) {
		if (delta.getKind() == IResourceDelta.REMOVED) {
			invalidate(r);
		} else if (delta.getKind() == IResourceDelta.CHANGED && isErrorDelta(delta)) {
			invalidate(r);
		} 
	}


	private boolean isErrorDelta(IResourceDelta delta) {	
		if ((delta.getFlags() & IResourceDelta.MARKERS) == 0)
			return false;
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
		return false;
	}
	
	class ProjectErrorVisitor implements IResourceDeltaVisitor {
		
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource res= delta.getResource();
			if (res instanceof IProject) {
				checkInvalidate(delta, res.getFullPath());
				IProject p= (IProject)res;
				IJavaProject jProject= getJavaProject(p);
				if (jProject != null) {
					try {
						IClasspathEntry[] cps= jProject.getResolvedClasspath(true);
						for (int i= 0; i < cps.length; i++) {
							if (cps[i].getEntryKind() == IClasspathEntry.CPE_SOURCE)
								delta.accept(new PkgFragmentRootErrorVisitor(cps[i].getPath()));
						}
					} catch (JavaModelException e) {
					}
				}
				return false;
			}
			return true;
		}
	}
		

	public ErrorTickManager() {
		fListeners= new HashSet(3);
	}
	

	
	public void resourceChanged(IResourceChangeEvent event) {
		beginChange();
		try {
			IResourceDelta delta= event.getDelta();
			if (delta != null)
				delta.accept(new ProjectErrorVisitor());
		} catch (CoreException e) {
			//XXX: error handling
		}
		endChange();
	}

	private IJavaProject getJavaProject(IProject proj) {
		try {
			if (proj.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp= JavaCore.create(proj);
				if (jp.isOpen()) 
					return jp;
			}
		} catch (CoreException e) {
		}
		return null;
	}			
	
	void invalidate(IPath path) {
		if (path == null)
			return;
		while (path.segmentCount() > 0) {
			if (!fChangedElements.contains(path))
				fChangedElements.add(path);
			path= path.removeLastSegments(1);
		}
	}
	
	/**
	 *@returns 	a boolean array of length 2. The first element says if the element contains
	 * 		warnings, the second element says if the element contains errors.
	 */
	public boolean[] getErrorInfo(IJavaElement element) {
		int depth= IResource.DEPTH_INFINITE;
		if (element instanceof IPackageFragment) {
			depth= IResource.DEPTH_ONE;
		}
		
		try {
			IResource res= element.getCorrespondingResource();
			if (res == null)
				return fgEmpty;
			IMarker[] markers= res.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, depth);
			if (markers == null)
				return fgEmpty;
			boolean[] info= new boolean[2];
			accumulateProblems(markers, info);
			if (res instanceof IProject) {
				markers= res.findMarkers(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, true, IResource.DEPTH_ONE);
				accumulateProblems(markers, info);
			}
			return info;
		} catch (CoreException e) {
		}
		return fgEmpty; 
	}
	
	private void accumulateProblems(IMarker[] markers, boolean[] info) {
		if (markers == null)
			return;
		for (int i= 0; i < markers.length; i++) {
			if (info[0] && info[1])
				return;
			int priority= markers[i].getAttribute(IMarker.SEVERITY, -1);
			if (priority == IMarker.SEVERITY_WARNING)
				info[0]= true;
			if (priority == IMarker.SEVERITY_ERROR)
				info[1]= true;
		}
	}
	
	// change propagation
	public void beginChange() {
		fChangedElements= new HashSet();
	}
	  
	public void endChange() { 
		fireChanges();
		fChangedElements= null;
	}
	
	public void addListener(ISeverityListener l) {
		if (fListeners.size() == 0)
			JavaPlugin.getWorkspace().addResourceChangeListener(this);
		fListeners.add(l);
	}
	
	public void removeListener(ISeverityListener l) {
		fListeners.remove(l);
		if (fListeners.size() == 0)
			JavaPlugin.getWorkspace().removeResourceChangeListener(this);
	}
	
	private void fireChanges() {
		Iterator listeners= fListeners.iterator();
		while (listeners.hasNext()) {
			ISeverityListener l= (ISeverityListener)listeners.next();
			l.severitiesChanged(fChangedElements);
		}
	}

}