/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.HashSet;
import java.util.Iterator;

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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
public class ErrorTickManager implements IResourceChangeListener {

	public static final int ERRORTICK_WARNING= 1;
	public static final int ERRORTICK_ERROR= 2;
	
	private static final int ERRORTICK_ALL= ERRORTICK_WARNING | ERRORTICK_ERROR; 

	private HashSet fChangedElements; 
	private HashSet fListeners;
	
	
	private class PkgFragmentRootErrorVisitor implements IResourceDeltaVisitor {
		
		private IPath fRoot;
		private boolean fInsideRoot;
		
		public PkgFragmentRootErrorVisitor() {
		}
		
		public void init(IPath path) {
			fRoot= path;
			fInsideRoot= false;
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
			if (proj.isOpen() && proj.hasNature(JavaCore.NATURE_ID)) {
				return JavaCore.create(proj);
			}
			return null;
		}		
		
	}
	
	public ErrorTickManager() {
		fListeners= new HashSet(3);
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
	public void beginChange() {
		fChangedElements= new HashSet();
	}
	  
	public void endChange() { 
		fireChanges();
		fChangedElements= null;
	}	
	
	/**
	 * @returns an set (specified by an int) using the constants
	 * ERRORTICK_ERROR, ERRORTICK_WARNING
	 */
	public int getErrorInfo(IJavaElement element) {
		int info= 0;
		try {
			IResource res= null;
			ISourceRange range= null;
			int depth= IResource.DEPTH_INFINITE;
			
			int type= element.getElementType();
			
			if (type < IJavaElement.TYPE) {
				res= element.getCorrespondingResource();
				if (type == IJavaElement.PACKAGE_FRAGMENT) {
					depth= IResource.DEPTH_ONE;
				} else if (type == IJavaElement.JAVA_PROJECT) {
					IMarker[] bpMarkers= res.findMarkers(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, true, IResource.DEPTH_ONE);
					info= accumulateProblems(bpMarkers, info, null);
				}
			} else if (type == IJavaElement.TYPE || type == IJavaElement.METHOD || type == IJavaElement.INITIALIZER) {
				res= element.getUnderlyingResource();
				range= ((IMember)element).getSourceRange();
			}
			
			if (res != null) {
				IMarker[] markers= res.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, depth);
				if (markers != null) {
					info= accumulateProblems(markers, info, range);
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}
		return info;
	}
	
	private int accumulateProblems(IMarker[] markers, int info, ISourceRange range) {
		if (markers != null) {	
			for (int i= 0; i < markers.length && (info != ERRORTICK_ALL); i++) {
				IMarker curr= markers[i];
				if (range == null || isInRange(curr, range)) {
					int priority= curr.getAttribute(IMarker.SEVERITY, -1);
					if (priority == IMarker.SEVERITY_WARNING) {
						info |= ERRORTICK_WARNING;
					} else if (priority == IMarker.SEVERITY_ERROR) {
						info |= ERRORTICK_ERROR;
					}
				}
			}
		}
		return info;
	}
	
	private boolean isInRange(IMarker marker, ISourceRange range) {
		int pos= marker.getAttribute(IMarker.CHAR_START, -1);
		int offset= range.getOffset();
		return (offset <= pos && offset + range.getLength() > pos);
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