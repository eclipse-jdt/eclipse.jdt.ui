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
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.*;

import org.eclipse.jdt.core.ICompilationUnit;


/**
 * A context type for javadoc.
 */
public class JavaDocContextType extends CompilationUnitContextType {

	public static final String NAME= "javadoc"; //$NON-NLS-1$

	/**
	 * Creates a java context type.
	 */
	public JavaDocContextType() {
		super(NAME);
		
		// global
		addVariable(new GlobalVariables.Cursor());
		addVariable(new GlobalVariables.LineSelection());
		addVariable(new GlobalVariables.WordSelection());
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Year());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		
		// compilation unit
		addVariable(new File());
		addVariable(new PrimaryTypeName());
		addVariable(new Method());
		addVariable(new ReturnType());
		addVariable(new Arguments());
		addVariable(new Type());
		addVariable(new Package());
		addVariable(new Project());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
		return new JavaDocContext(this, document, offset, length, compilationUnit);
	}	
	
}
