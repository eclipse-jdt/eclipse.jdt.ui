package org.eclipse.jdt.internal.ui.javaeditor;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * Event sent out by changes of the compilation unit annotation model.
 */
public class CompilationUnitAnnotationModelEvent  extends AnnotationModelEvent {
	
	private boolean fIncludesMarkerAnnotationChanges= false;
	
	
	/**
	 * Constructor for CompilationUnitAnnotationModelEvent.
	 * @param model
	 * @param includesMarkerAnnotationChanges
	 */
	public CompilationUnitAnnotationModelEvent(IAnnotationModel model, boolean includesMarkerAnnotationChanges) {
		super(model);
		fIncludesMarkerAnnotationChanges= includesMarkerAnnotationChanges;
	}
	
	/**
	 * Returns whether the change included marker annotations.
	 * 
	 * @return <code>true</code> if the change included marker annotations
	 */
	public boolean includesMarkerAnnotationChanges() {
		return fIncludesMarkerAnnotationChanges;
	}
}
