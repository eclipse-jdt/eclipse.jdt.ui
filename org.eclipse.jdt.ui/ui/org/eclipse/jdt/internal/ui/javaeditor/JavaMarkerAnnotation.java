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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;

import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.jdt.core.IJavaModelMarker;

import org.eclipse.jdt.internal.core.Util;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;



public class JavaMarkerAnnotation extends MarkerAnnotation implements IProblemAnnotation {		
	
	private static final int UNKNOWN= 0;
	private static final int PROBLEM= 1;
	private static final int TASK= 2;
	
	private IDebugModelPresentation fPresentation;
	private IProblemAnnotation fOverlay;
	private boolean fNotRelevant= false;
	private int fType;
	
	
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
			
			fType= UNKNOWN;
			
		} else {
			
			fType= UNKNOWN;
			try {
				if (marker.isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER))
					fType= PROBLEM;
				else if (marker.isSubtypeOf(IMarker.TASK))
					fType= TASK;
			} catch(CoreException e) {
				JavaPlugin.log(e);
			}
			super.initialize();
		
		}
	}

	/*
	 * @see IProblemAnnotation#getMessage()
	 */
	public String getMessage() {
		if (isProblem() || isTask())
			return getMarker().getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
		return ""; //$NON-NLS-1$
	}

	/*
	 * @see IProblemAnnotation#isError()
	 */
	public boolean isError() {
		if (isProblem()) {
			int markerSeverity= getMarker().getAttribute(IMarker.SEVERITY, -1);
			return (markerSeverity == IMarker.SEVERITY_ERROR);
		}
		return false;
	}

	/*
	 * @see IProblemAnnotation#isWarning()
	 */
	public boolean isWarning() {
		if (isProblem()) {
			int markerSeverity= getMarker().getAttribute(IMarker.SEVERITY, -1);
			return (markerSeverity == IMarker.SEVERITY_WARNING);
		}
		return false;
	}
	
	/*
	 * @see IProblemAnnotation#isTemporary()
	 */
	public boolean isTemporary() {
		return false;
	}
	
	/*
	 * @see IProblemAnnotation#getArguments()
	 */
	public String[] getArguments() {
		if (isProblem())
			return Util.getProblemArgumentsFromMarker(getMarker().getAttribute(IJavaModelMarker.ARGUMENTS, "")); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see IProblemAnnotation#getId()
	 */
	public int getId() {
		if (isProblem())
			return getMarker().getAttribute(IJavaModelMarker.ID, -1);
		return -1;
	}
	
	/*
	 * @see IProblemAnnotation#isProblem()
	 */
	public boolean isProblem() {
		return fType == PROBLEM;
	}
	
	/*
	 * @see IProblemAnnotation#isTask()
	 */
	public boolean isTask() {
		return fType ==TASK;
	}	
	
	/*
	 * @see IProblemAnnotation#isRelevant()
	 */
	public boolean isRelevant() {
		return !fNotRelevant;
	}
	
	public void setOverlay(IProblemAnnotation problemAnnotation) {
		if (fOverlay != null)
			fOverlay.removeOverlaid(this);
			
		fOverlay= problemAnnotation;
		fNotRelevant= (fNotRelevant || fOverlay != null);
		
		if (problemAnnotation != null)
			problemAnnotation.addOverlaid(this);
	}
	
	/*
	 * @see IProblemAnnotation#hasOverlay()
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
	 * @see IProblemAnnotation#addOverlaid(IProblemAnnotation)
	 */
	public void addOverlaid(IProblemAnnotation annotation) {
		// not supported
	}

	/*
	 * @see IProblemAnnotation#removeOverlaid(IProblemAnnotation)
	 */
	public void removeOverlaid(IProblemAnnotation annotation) {
		// not supported
	}
	
	/*
	 * @see IProblemAnnotation#getOverlaidIterator()
	 */
	public Iterator getOverlaidIterator() {
		// not supported
		return null;
	}
}