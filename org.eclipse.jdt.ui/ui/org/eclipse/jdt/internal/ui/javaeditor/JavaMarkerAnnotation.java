package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class JavaMarkerAnnotation extends MarkerAnnotation {		
	
	private IDebugModelPresentation fPresentation;
	
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
			
			if (MarkerUtilities.isMarkerType(getMarker(), IBreakpoint.BREAKPOINT_MARKER)) {
				if (fPresentation == null) 
					fPresentation= DebugUITools.newDebugModelPresentation();
				setLayer(2);
				setImage(fPresentation.getImage(getMarker()));
				return;						
			}
			
		} catch (CoreException e) {
		}
		
		super.initialize();
	}
}