package org.eclipse.jdt.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */


import org.eclipse.ui.IViewPart;

import org.eclipse.jdt.core.IType;

/**
 * The standard type hierarchy view presents a type hierarchy for a given input class
 * or interface. Visually, this view consists of a pair of viewers, one showing the type
 * hierarchy, the other showing the members of the type selected in the first.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see JavaUI#ID_TYPE_HIERARCHY
 */
public interface ITypeHierarchyViewPart extends IViewPart {

	/**
	 * Sets the input element of this type hierarchy view.
	 *
	 * @param type the input element of this type hierarchy view, or <code>null</code>
	 *  to clear any input element
	 */
	public void setInput(IType type);

	/**
	 * Returns the input element of this type hierarchy view.
	 *
	 * @return the input element, or <code>null</code> if no input element is set
	 * @see #setInput
	 */
	public IType getInput(); 
}