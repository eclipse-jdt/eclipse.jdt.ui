package org.eclipse.jdt.internal.ui.javaeditor;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * Event sent out by changes of the compilation unit annotation model.
 */
public class CompilationUnitAnnotationModelEvent  extends AnnotationModelEvent {
	
	private boolean fIncludesMarkerAnnotationChanges;
	private IResource fUnderlyingResource;
	
	
	/**
	 * Constructor for CompilationUnitAnnotationModelEvent.
	 * @param model
	 * @param underlyingResource The annotation model's underlying resource 
	 * @param includesMarkerAnnotationChanges
	 */
	public CompilationUnitAnnotationModelEvent(IAnnotationModel model, IResource underlyingResource, boolean includesMarkerAnnotationChanges) {
		super(model);
		fIncludesMarkerAnnotationChanges= includesMarkerAnnotationChanges;
		fUnderlyingResource= underlyingResource;
	}
	
	/**
	 * Returns whether the change included marker annotations.
	 * 
	 * @return <code>true</code> if the change included marker annotations
	 */
	public boolean includesMarkerAnnotationChanges() {
		return fIncludesMarkerAnnotationChanges;
	}
	
	/**
	 * Returns the annotation model's underlying resource
	 */
	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

}
