/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Filter for the methods viewer.
 * Changing a filter property does not trigger a refiltering of the viewer
 */
public class MethodsViewerFilter extends ViewerFilter {
	
	public static final int FILTER_PUBLIC= 1;
	public static final int FILTER_PRIVATE= 2;
	public static final int FILTER_PROTECTED= 4;
	public static final int FILTER_DEFAULT= 8;
	public static final int FILTER_STATIC= 16;
	public static final int FILTER_FIELDS= 32;
	
	private int fFilterProperties;
	
	
	/**
	 * Modifies filter and add a property to filter for
	 */
	public final void addFilter(int filter) {
		fFilterProperties |= filter;
	}

	/**
	 * Modifies filter and remove a property to filter for
	 */	
	public final void removeFilter(int filter) {
		fFilterProperties &= (-1 ^ filter);
	}

	/**
	 * Tests if a property is filtered
	 */		
	public final boolean hasFilter(int filter) {
		return (fFilterProperties & filter) != 0;
	}
	

	/**
	 * @see ViewerFilter@isFilterProperty
	 */
	public boolean isFilterProperty(Object element, Object property) {
		return false;
	}

	/**
	 * @see ViewerFilter@select
	 */		
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		try {
			if (element instanceof IField && hasFilter(FILTER_FIELDS)) {
				return false;
			}
			if (element instanceof IMember) {
				IMember member= (IMember)element;
				int flags= member.getFlags();
				if (Flags.isStatic(flags) && 
					(hasFilter(FILTER_STATIC) || "<clinit>".equals(member.getElementName()))) {
					return false;
				}
				if (Flags.isPublic(flags) || member.getDeclaringType().isInterface()) {
					return !hasFilter(FILTER_PUBLIC);
				} else if (Flags.isPrivate(flags)) {
					return !hasFilter(FILTER_PRIVATE);
				} else if (Flags.isProtected(flags)) {
					return !hasFilter(FILTER_PROTECTED);
				} else {  // default(flags)
					return !hasFilter(FILTER_DEFAULT);
				}	
			}			
		} catch (JavaModelException e) {
			// ignore
		}
		return false;
	}		

}