/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
 * A context type for javadoc.
 */
public class JavaDocContextType extends ContextType {
	
	/** the document string */
	private String fString;

	/** the completion position within the document string */
	private int fPosition;

	/** the associated compilation unit, may be <code>null</code> */
	private ICompilationUnit fCompilationUnit;

	/**
	 * A variable returning the name of the compilation unit.
	 */
	protected static class File extends TemplateVariable {
		public File() {
			super("file", TemplateMessages.getString("JavaDocContextType.variable.description.file"));
		}
		public String evaluate(TemplateContext context) {
			ICompilationUnit unit= ((JavaContext) context).getUnit();
			
			return (unit == null) ? null : unit.getElementName();
		}
	}

	/**
	 * Creates a java context type.
	 */
	public JavaDocContextType() {
		super("javadoc");
		
		// global
		addVariable(new GlobalVariables.Cursor());
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		
		// javadoc
		addVariable(new File());
	}
	
	/**
	 * Sets parameters for creating a context. Must be called before createContext().
	 */
	public void setContextParameters(String string, int position, ICompilationUnit compilationUnit) {
		fString= string;
		fPosition= position;
		fCompilationUnit= compilationUnit;
	}
	
	/*
	 * @see ContextType#createContext()
	 */	
	public TemplateContext createContext() {
		return new JavaDocContext(this, fString, fPosition, fCompilationUnit);
	}

}
