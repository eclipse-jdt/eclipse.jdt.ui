/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;
/**
 * @since 1.0
 */
public interface IAdornmentProvider {
	
	/**
	 * Computes the adornment flags for the given element.
	 * Flags defined in <code>JavaElementImageDescriptor</code>.
	 */
	int computeAdornmentFlags(Object element, int renderFlags);
	
	
	/**
	 * Called when the parent label provider is disposed
	 */
	void dispose();

}
