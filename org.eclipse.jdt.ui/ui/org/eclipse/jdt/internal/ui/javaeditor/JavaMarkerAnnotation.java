package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.debug.core.IDebugConstants;

import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class JavaMarkerAnnotation extends MarkerAnnotation {		
	
	private JDIModelPresentation fPresentation;
	
	public JavaMarkerAnnotation(IMarker marker) {
		super(marker);
	}
	
	/**
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
		
		try {
			String type= getMarker().getType();
			
			if (MarkerUtilities.isMarkerType(getMarker(), IDebugConstants.BREAKPOINT_MARKER)) {
				if (fPresentation == null) 
					fPresentation= new JDIModelPresentation();
				setLayer(2);
				setImage(fPresentation.getImage(getMarker()));
				return;						
			} else if (MarkerUtilities.isMarkerType(getMarker(), SearchUI.SEARCH_MARKER)) {
				setLayer(2);
				setImage(SearchUI.getSearchMarkerImage());
				return;
			}
			
		} catch (CoreException e) {
		}
		
		super.initialize();
	}
}