/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;

/**
 * A tuple used to keep source of an element and its type.
 * @see IJavaElement
 * @see ISourceReference
 */
public class TypedSource {
	
	private final String fSource;
	private final int fType;

	private TypedSource(String source, int type){
		Assert.isNotNull(source);
		Assert.isTrue(canCreateForType(type));
		fSource= source;
		fType= type;				  
	}
	
	public static TypedSource create(ISourceReference ref) throws JavaModelException{
		return create(SourceRangeComputer.computeSource(ref), ((IJavaElement)ref).getElementType());
	}

	public static TypedSource create(String source, int type) {
		if (source == null || ! canCreateForType(type))
			return null;
		return new TypedSource(source, type);
	}

	public String getSource() {
		return fSource;
	}

	public int getType() {
		return fType;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (! (other instanceof TypedSource))
			return false;
		
		TypedSource ts= (TypedSource)other;
		return ts.getSource().equals(getSource()) && ts.getType() == getType();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getSource().hashCode() ^ (97 * getType());
	}

	private static boolean canCreateForType(int type){
		return type == IJavaElement.FIELD 
				|| type == IJavaElement.TYPE
				|| type == IJavaElement.IMPORT_CONTAINER
				|| type == IJavaElement.IMPORT_DECLARATION
				|| type == IJavaElement.INITIALIZER
				|| type == IJavaElement.METHOD
				|| type == IJavaElement.PACKAGE_DECLARATION;
	}
	
	public static TypedSource[] createTypeSources(IJavaElement[] javaElements) throws JavaModelException {
		List result= new ArrayList(javaElements.length);
		for (int i= 0; i < javaElements.length; i++) {
			IJavaElement element= javaElements[i];
			if (element instanceof ISourceReference){
				TypedSource typedSource= TypedSource.create((ISourceReference)element);
				if (typedSource != null)
					result.add(typedSource);
			}
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);
	}
	
	public static void sortByType(TypedSource[] typedSources){
		Arrays.sort(typedSources, createTypeComparator());
	}

	public static Comparator createTypeComparator() {
		return new Comparator(){
			public int compare(Object arg0, Object arg1) {
				return ((TypedSource)arg0).getType() - ((TypedSource)arg1).getType();
			}
		};
	}
}

