package org.eclipse.jdt.internal.ui.typehierarchy;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


/**
 * Used by the TypeHierarchyLifeCycle to inform listeners about a change in the
 * type hierarchy
 */
public interface ITypeHierarchyLifeCycleListener {
	
	void typeHierarchyChanged(TypeHierarchyLifeCycle typeHierarchyProvider);

}