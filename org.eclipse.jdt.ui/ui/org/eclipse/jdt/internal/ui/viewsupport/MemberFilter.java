/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Filter for the methods viewer.
 * Changing a filter property does not trigger a refiltering of the viewer
 */
public class MemberFilter extends ViewerFilter {

	public static final int FILTER_NONPUBLIC= 1;
	public static final int SHOW_STATIC= 2;
	public static final int SHOW_FIELDS= 4;
	
	private int fFilterProperties;

	/**
	 * Modifies filter and add a property
	 */
	public final void addProperty(int property) {
		fFilterProperties |= property;
	}
	/**
	 * Modifies filter and remove a property
	 */	
	public final void removeProperty(int property) {
		fFilterProperties &= (-1 ^ property);
	}
	/**
	 * Tests if a property is filtered
	 */		
	public final boolean hasProperty(int property) {
		return (fFilterProperties & property) != 0;
	}
	
	/*
	 * @see ViewerFilter@isFilterProperty
	 */
	public boolean isFilterProperty(Object element, Object property) {
		return false;
	}
	
	/*
	 * @see ViewerFilter@select
	 */		
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		try {
			if (element instanceof IMember) {
				
				IMember member= (IMember)element;
				if (member.getElementName().startsWith("<")) { // filter out <clinit> //$NON-NLS-1$
					return false;
				}
				int flags= member.getFlags();
				if (!Flags.isPublic(flags) && !isMemberInInterface(member) && !isTopLevelType(member)) {
					if (hasProperty(FILTER_NONPUBLIC)) {
						return false;
					}
				}
				boolean isField= member.getElementType() == IJavaElement.FIELD;
				boolean isStatic= Flags.isStatic(flags) || isFieldInInterface(member);
				
				if (isField || isStatic) {
					return (isField && hasProperty(SHOW_FIELDS)) || (isStatic && hasProperty(SHOW_STATIC));
				}
			}			
		} catch (JavaModelException e) {
			// ignore
		}
		return true;
	}
	
	private boolean isMemberInInterface(IMember member) throws JavaModelException {
		IType parent= member.getDeclaringType();
		return parent != null && parent.isInterface();
	}
	
	private boolean isFieldInInterface(IMember member) throws JavaModelException {
		return (member.getElementType() == IJavaElement.FIELD) && member.getDeclaringType().isInterface();
	}	
	
	private boolean isTopLevelType(IMember member) throws JavaModelException {
		IType parent= member.getDeclaringType();
		return parent == null;
	}		
}