package org.eclipse.jdt.internal.ui.reorg;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

/**
 * Abstraction layer for name collision detection and naming rules (for example valid 
 * package names
 */
public interface INamingPolicy {
	/**
	 * returns whether an object with the given name could be created in the given
	 * container
	 */
	String isValidNewName(Object original, Object container, String name);
	boolean canReplace(Object original, Object container, String name);
	Object getElement(Object parent, String name);
}