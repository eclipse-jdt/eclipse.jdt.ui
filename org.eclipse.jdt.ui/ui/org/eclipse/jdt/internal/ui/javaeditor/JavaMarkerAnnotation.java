/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;


import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.model.IBreakpoint;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IJavaModelMarker;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.core.Util;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;



public class JavaMarkerAnnotation extends MarkerAnnotation implements IJavaAnnotation {
	
	private static Image fgImage;
	private static boolean fgImageInitialized= false;
	
	private IDebugModelPresentation fPresentation;
	private IJavaAnnotation fOverlay;
	private boolean fNotRelevant= false;
	private AnnotationType fType;
	
	
	public JavaMarkerAnnotation(IMarker marker) {
		super(marker);
	}
	
	/*
	 * @see MarkerAnnotation#getUnknownImageName(IMarker)
	 */
	protected String getUnknownImageName(IMarker marker) {
		return JavaPluginImages.IMG_OBJS_GHOST;
	}
	
	/**
	 * Initializes the annotation's icon representation and its drawing layer
	 * based upon the properties of the underlying marker.
	 */
	protected void initialize() {
		
		IMarker marker= getMarker();
		
		if (MarkerUtilities.isMarkerType(marker, IBreakpoint.BREAKPOINT_MARKER)) {
			
			if (fPresentation == null) 
				fPresentation= DebugUITools.newDebugModelPresentation();
				
			setLayer(4);
			setImage(fPresentation.getImage(marker));					
			
			fType= AnnotationType.UNKNOWN;
			
		} else {
			
			fType= AnnotationType.UNKNOWN;
			try {
				
				if (marker.isSubtypeOf(IMarker.PROBLEM)) {
					int severity= marker.getAttribute(IMarker.SEVERITY, -1);
					switch (severity) {
						case IMarker.SEVERITY_ERROR:
							fType= AnnotationType.ERROR;
							break;
						case IMarker.SEVERITY_WARNING:
							fType= AnnotationType.WARNING;
							break;
					}
				} else if (marker.isSubtypeOf(IMarker.TASK))
					fType= AnnotationType.TASK;
				else if (marker.isSubtypeOf(SearchUI.SEARCH_MARKER)) 
					fType= AnnotationType.SEARCH;
				else if (marker.isSubtypeOf(IMarker.BOOKMARK))
					fType= AnnotationType.BOOKMARK;
					
			} catch(CoreException e) {
				JavaPlugin.log(e);
			}
			
			super.initialize();
			
			if (indicateQuixFixableProblems() && JavaCorrectionProcessor.hasCorrections(marker)) {
				if (!fgImageInitialized) {
					fgImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
					fgImageInitialized= true;
				}
				setImage(fgImage);
			}
		}
	}

	private boolean indicateQuixFixableProblems() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CORRECTION_INDICATION);
	}

	/*
	 * @see IJavaAnnotation#getMessage()
	 */
	public String getMessage() {
		IMarker marker= getMarker();
		if (marker == null)
			return ""; //$NON-NLS-1$
		else
			return marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
	}

	/*
	 * @see IJavaAnnotation#isTemporary()
	 */
	public boolean isTemporary() {
		return false;
	}
	
	/*
	 * @see IJavaAnnotation#getArguments()
	 */
	public String[] getArguments() {
		if (isProblem())
			return Util.getProblemArgumentsFromMarker(getMarker().getAttribute(IJavaModelMarker.ARGUMENTS, "")); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see IJavaAnnotation#getId()
	 */
	public int getId() {
		if (isProblem())
			return getMarker().getAttribute(IJavaModelMarker.ID, -1);
		return -1;
	}
	
	/*
	 * @see IJavaAnnotation#isProblem()
	 */
	public boolean isProblem() {
		return fType == AnnotationType.WARNING || fType == AnnotationType.ERROR;
	}
	
	/*
	 * @see IJavaAnnotation#isRelevant()
	 */
	public boolean isRelevant() {
		return !fNotRelevant;
	}

	/**
	 * Overlays this annotation with the given javaAnnotation.
	 * 
	 * @param javaAnnotation annotation that is overlaid by this annotation
	 */
	public void setOverlay(IJavaAnnotation javaAnnotation) {
		if (fOverlay != null)
			fOverlay.removeOverlaid(this);
			
		fOverlay= javaAnnotation;
		fNotRelevant= (fNotRelevant || fOverlay != null);
		
		if (javaAnnotation != null)
			javaAnnotation.addOverlaid(this);
	}
	
	/*
	 * @see IJavaAnnotation#hasOverlay()
	 */
	public boolean hasOverlay() {
		return fOverlay != null;
	}
	
	/*
	 * @see MarkerAnnotation#getImage(Display)
	 */
	public Image getImage(Display display) {
		if (fOverlay != null) {
			Image image= fOverlay.getImage(display);
			if (image != null)
				return image;
		}
		return super.getImage(display);
	}
	
	/*
	 * @see IJavaAnnotation#addOverlaid(IJavaAnnotation)
	 */
	public void addOverlaid(IJavaAnnotation annotation) {
		// not supported
	}

	/*
	 * @see IJavaAnnotation#removeOverlaid(IJavaAnnotation)
	 */
	public void removeOverlaid(IJavaAnnotation annotation) {
		// not supported
	}
	
	/*
	 * @see IJavaAnnotation#getOverlaidIterator()
	 */
	public Iterator getOverlaidIterator() {
		// not supported
		return null;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation#getAnnotationType()
	 */
	public AnnotationType getAnnotationType() {
		return fType;
	}
}