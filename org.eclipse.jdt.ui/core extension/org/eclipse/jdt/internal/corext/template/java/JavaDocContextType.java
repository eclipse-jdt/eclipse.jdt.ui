/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;


/**
 * A context type for javadoc.
 */
public class JavaDocContextType extends CompilationUnitContextType {

	/**
	 * Creates a java context type.
	 */
	public JavaDocContextType() {
		super("javadoc"); //$NON-NLS-1$
		
		// global
		addVariable(new GlobalVariables.Cursor());
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Year());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		
		// compilation unit
		addVariable(new File());
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
