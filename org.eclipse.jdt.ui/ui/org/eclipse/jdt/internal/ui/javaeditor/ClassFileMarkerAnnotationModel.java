package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;


/**
 *
 */
public class ClassFileMarkerAnnotationModel extends AbstractMarkerAnnotationModel implements IResourceChangeListener, IResourceDeltaVisitor {

	protected IClassFile fClassFile;
	protected IWorkspace fWorkspace;
	protected IResource fMarkerResource;
	protected boolean fChangesApplied;
	

	public ClassFileMarkerAnnotationModel(IResource markerResource, IClassFile classFile) {
		super();
		fMarkerResource= markerResource;
		fClassFile= classFile;
		fWorkspace= classFile.getJavaModel().getWorkspace();
	}
	
	/**
	 * @see AbstractMarkerAnnotationModel#isAcceptable
	 */
	protected boolean isAcceptable(IMarker marker) {
		try {
			return JavaCore.getJavaCore().isReferencedBy(fClassFile, marker);
		} catch (CoreException x) {
			handleCoreException(x, JavaEditorMessages.getString("ClassFileMarkerAnnotationModel.error.isAcceptable")); //$NON-NLS-1$
			return false;
		}
	}
	
	protected boolean isAffected(IMarkerDelta markerDelta) {
		try {
			return JavaCore.getJavaCore().isReferencedBy(fClassFile, markerDelta);
		} catch (CoreException x) {
			handleCoreException(x, JavaEditorMessages.getString("ClassFileMarkerAnnotationModel.error.isAffected")); //$NON-NLS-1$
			return false;
		}
	}
	
	/**
	 * @see AbstractMarkerAnnotationModel#createMarkerAnnotation(IMarker)
	 */
	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
		return new JavaMarkerAnnotation(marker);
	}
	
	/**
	 * @see AbstractMarkerAnnotationModel#listenToMarkerChanges(boolean)
	 */
	protected void listenToMarkerChanges(boolean listen) {
		if (listen)
			fWorkspace.addResourceChangeListener(this);
		else
			fWorkspace.removeResourceChangeListener(this);
	}
	
	/**
	 * @see AbstractMarkerAnnotationModel#deleteMarkers(IMarker[])
	 */
	protected void deleteMarkers(IMarker[] markers) throws CoreException {
		// empty as class files are read only
	}

	/**
	 * @see AbstractMarkerAnnotationModel#retrieveMarkers()
	 */
	protected IMarker[] retrieveMarkers() throws CoreException {
		return fMarkerResource.findMarkers(IMarker.MARKER, true, IResource.DEPTH_INFINITE);
	}
			
	/**
	 * @see IResourceDeltaVisitor#visit
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		
		if (delta != null) {
			
			if (fMarkerResource != null && !fMarkerResource.equals(delta.getResource()))
				return true;
			
			IMarkerDelta[] markerDeltas= delta.getMarkerDeltas();
			for (int i= 0; i < markerDeltas.length; i++) {
				if (isAffected(markerDeltas[i])) {
					IMarker marker= markerDeltas[i].getMarker();
					switch (markerDeltas[i].getKind()) {
						case IResourceDelta.ADDED :
							addMarkerAnnotation(marker);
							fChangesApplied= true;
							break;
						case IResourceDelta.REMOVED :
							removeMarkerAnnotation(marker);
							fChangesApplied= true;
							break;
						case IResourceDelta.CHANGED:
							modifyMarkerAnnotation(marker);
							fChangesApplied= true;
							break;
					}
				}
			}
			
			return (fMarkerResource == null);
		}
		
		return false;
	}
	
	/**
	 * @see IResourceChangeListener#resourceChanged
	 */
	public void resourceChanged(IResourceChangeEvent e) {
		IResourceDelta delta= e.getDelta();
		try {
			
			if (delta != null) {
				fChangesApplied= false;
				delta.accept(this);
				if (fChangesApplied)
					fireModelChanged();
			}
			
		} catch (CoreException x) {
			handleCoreException(x, JavaEditorMessages.getString("ClassFileMarkerAnnotationModel.error.resourceChanged")); //$NON-NLS-1$
		}
	}
}

