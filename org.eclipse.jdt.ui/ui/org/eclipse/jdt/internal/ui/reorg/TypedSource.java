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
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;

/**
 * A tuple used to keep source of  an element and its type.
 * @see IJavaElement
 * @see ISourceReference
 */
class TypedSource {
	
	private final String fSource;
	private final int fType;

	public TypedSource(String source, int type){
		Assert.isNotNull(source);
		Assert.isTrue(type == IJavaElement.FIELD 
						  || type == IJavaElement.TYPE
						  || type == IJavaElement.IMPORT_CONTAINER
						  || type == IJavaElement.IMPORT_DECLARATION
						  || type == IJavaElement.INITIALIZER
						  || type == IJavaElement.METHOD
						  || type == IJavaElement.PACKAGE_DECLARATION);
		fSource= source;
		fType= type;				  
	}
	
	public TypedSource(ISourceReference ref) throws JavaModelException{
		this(SourceRangeComputer.computeSource(ref), ((IJavaElement)ref).getElementType());
		Assert.isTrue(((IJavaElement)ref).getElementType() != IJavaElement.CLASS_FILE);
		Assert.isTrue(((IJavaElement)ref).getElementType() != IJavaElement.COMPILATION_UNIT);
	}

	public String getSource() {
		return fSource;
	}

	public int getType() {
		return fType;
	}
}

