package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Iterator;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.jdt.core.IJavaModelMarker;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.internal.core.Util;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class JavaMarkerAnnotation extends MarkerAnnotation implements IProblemAnnotation {		
	
	private IDebugModelPresentation fPresentation;
	private boolean fIsProblemMarker;
	private IProblemAnnotation fOverlay;
	private boolean fNotRelevant= false;
	
	
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
			
			fIsProblemMarker= false;
			
		} else {
			
			try {
				fIsProblemMarker= marker.isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
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
		if (fIsProblemMarker)
			return getMarker().getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
		return ""; //$NON-NLS-1$
	}

	/*
	 * @see IProblemAnnotation#isError()
	 */
	public boolean isError() {
		if (fIsProblemMarker) {
			int markerSeverity= getMarker().getAttribute(IMarker.SEVERITY, -1);
			return (markerSeverity == IMarker.SEVERITY_ERROR);
		}
		return false;
	}

	/*
	 * @see IProblemAnnotation#isWarning()
	 */
	public boolean isWarning() {
		if (fIsProblemMarker) {
			int markerSeverity= getMarker().getAttribute(IMarker.SEVERITY, -1);
			return (markerSeverity == IMarker.SEVERITY_WARNING);
		}
		return false;
	}
	
	/*
	 * @see IProblemAnnotation#isTemporaryProblem()
	 */
	public boolean isTemporaryProblem() {
		return false;
	}
	
	/*
	 * @see IProblemAnnotation#getArguments()
	 */
	public String[] getArguments() {
		if (fIsProblemMarker)
			return Util.getProblemArgumentsFromMarker(getMarker().getAttribute(IJavaModelMarker.ARGUMENTS, "")); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see IProblemAnnotation#getId()
	 */
	public int getId() {
		if (fIsProblemMarker)
			return getMarker().getAttribute(IJavaModelMarker.ID, -1);
		return 0;
	}
	
	/*
	 * @see IProblemAnnotation#isProblem()
	 */
	public boolean isProblem() {
		return fIsProblemMarker;
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