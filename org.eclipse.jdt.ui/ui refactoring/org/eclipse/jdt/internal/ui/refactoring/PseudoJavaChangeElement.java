/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.util.Assert;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.ltk.internal.ui.refactoring.ChangeElement;
import org.eclipse.ltk.internal.ui.refactoring.PseudoLanguageChangeElement;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

/**
 * TODO should remove dependency to JDT/Core 
 *      (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=61312)
 */ 
public class PseudoJavaChangeElement extends PseudoLanguageChangeElement {

	private IJavaElement fJavaElement;

	public PseudoJavaChangeElement(ChangeElement parent, IJavaElement element) {
		super(parent);
		fJavaElement= element;
		Assert.isNotNull(fJavaElement);
	}
	
	/**
	 * Returns the Java element.
	 * 
	 * @return the Java element managed by this node
	 */
	public IJavaElement getJavaElement() {
		return fJavaElement;
	}
	
	public Object getModifiedElement() {
		return fJavaElement;
	}

	public IRegion getTextRange() throws CoreException {
		ISourceRange range= ((ISourceReference)fJavaElement).getSourceRange();
		return new Region(range.getOffset(), range.getLength());
	}	
}