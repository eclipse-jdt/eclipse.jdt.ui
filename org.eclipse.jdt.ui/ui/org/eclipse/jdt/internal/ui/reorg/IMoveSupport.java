/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstraction layer for moving elements
 */
public interface IMoveSupport {
	
	/**
	 * returns whether this element can ever be copied by this policy. Used for 
	 * enabling/disabling
	 */
	boolean isMovable(Object element);
	/** 
	 * Return true if the element can appear above a valid destination for the move
	 * used to filter the java element tree when selecting the destination
	 */
	boolean canBeAncestor(Object ancestor);
	/**
	 * actually does the move operation
	 */
	Object moveTo(Object source, Object destination, String newName, IProgressMonitor pm) throws JavaModelException, CoreException;
	
	/**
	 * Checks if we can move the given elements to the given destination.
	 */
	boolean canMove(List elements, Object destination); 
	
	String getElementName(Object element);
}