/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.internal.corext.template.TemplateContext;

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
	
	/*
	 * @see ContextType#createContext()
	 */	
	public TemplateContext createContext() {
		return new JavaDocContext(this, fDocument, fOffset, fLength, fCompilationUnit);
	}

}
