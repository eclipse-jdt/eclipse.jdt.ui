/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstraction layer for copying elements. 
 */

public interface ICopySupport {
	/**
	 * returns whether this element can ever be copied by this policy. Used for 
	 * enabling/disabling
	 */
	boolean isCopyable(Object element);
	/**
	 * Can elements be copied to the destination element? Since there are different
	 * policies for different element types, we don't have to know what is copied.
	 */
	boolean canCopy(List element, Object destination);
	/** 
	 * Return true if the element can appear above a valid destination for the copy
	 * used to filter the java element tree when selecting the destination
	 */
	boolean canBeAncestor(Object ancestor);
	/**
	 * actually does the copy operation
	 */
	Object copyTo(Object source, Object destination, String newName, IProgressMonitor pm) throws JavaModelException, CoreException;
	
	String getElementName(Object element);
}