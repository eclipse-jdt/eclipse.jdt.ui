/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * @since 3.4
 */
public class SWTContextType extends CompilationUnitContextType {

	/**
	 * The id under which this context type is registered
	 */
	public static final String NAME= "swt"; //$NON-NLS-1$

	public SWTContextType() {
		super(NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
		return new SWTContext(this, document, offset, length, compilationUnit);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, Position offset, ICompilationUnit compilationUnit) {
		return new SWTContext(this, document, offset, compilationUnit);
	}

	/**
	 * Inherit all resolvers from <code>otherContext</code>.
	 * 
	 * @param otherContext the context from which to retrieve the resolvers from
	 */
	public void inheritResolvers(TemplateContextType otherContext) {
		for (Iterator iterator= otherContext.resolvers(); iterator.hasNext();) {
			TemplateVariableResolver resolver= (TemplateVariableResolver) iterator.next();
			addResolver(resolver);
		}
	}
}
