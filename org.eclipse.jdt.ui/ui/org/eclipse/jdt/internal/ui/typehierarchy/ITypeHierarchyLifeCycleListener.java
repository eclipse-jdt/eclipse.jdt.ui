/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

/**
 * Used by the TypeHierarchyLifeCycle to inform listeners about a change in the
 * type hierarchy
 */
public interface ITypeHierarchyLifeCycleListener {
	
	/**
	 * The type hierarchy changed.
	 */
	void typeHierarchyChanged(TypeHierarchyLifeCycle typeHierarchyProvider);

}