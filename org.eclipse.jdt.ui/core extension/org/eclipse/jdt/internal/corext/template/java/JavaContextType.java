/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de - see bug 25376
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
 * A context type for java code.
 */
public class JavaContextType extends CompilationUnitContextType {

	public static final String NAME= "java"; //$NON-NLS-1$

	protected static class Array extends TemplateVariable {
		public Array() {
			super("array", JavaTemplateMessages.getString("JavaContextType.variable.description.array")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArray();
	    }
	}

	protected static class ArrayType extends TemplateVariable {
	    public ArrayType() {
	     	super("array_type", JavaTemplateMessages.getString("JavaContextType.variable.description.array.type")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayType();
	    }
	}

	protected static class ArrayElement extends TemplateVariable {
	    public ArrayElement() {
	     	super("array_element", JavaTemplateMessages.getString("JavaContextType.variable.description.array.element"));	//$NON-NLS-1$ //$NON-NLS-2$    
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayElement();
	    }	    
	}

	protected static class Index extends TemplateVariable {
	    public Index() {
	     	super("index", JavaTemplateMessages.getString("JavaContextType.variable.description.index")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).getIndex();
	    }	    
	}

	protected static class Collection extends TemplateVariable {
	    public Collection() {
		    super("collection", JavaTemplateMessages.getString("JavaContextType.variable.description.collection")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessCollection();
	    }
	}

	protected static class Iterator extends TemplateVariable {

	    public Iterator() {
		    super("iterator", JavaTemplateMessages.getString("JavaContextType.variable.description.iterator")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	    	JavaContext javaContext= (JavaContext) context;

			if (!context.isReadOnly())
		    	javaContext.addIteratorImport();
	    	
	        return javaContext.getIterator();
	    }	    
	}
	
	protected static class Todo extends TemplateVariable {

		public Todo() {
			super("todo", JavaTemplateMessages.getString("JavaContextType.variable.description.todo")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		public String evaluate(TemplateContext context) {
			JavaContext javaContext= (JavaContext) context;
			ICompilationUnit compilationUnit= javaContext.getCompilationUnit();
			if (compilationUnit == null)
				return "XXX"; //$NON-NLS-1$
			
			IJavaProject javaProject= compilationUnit.getJavaProject();
			String todoTaskTag= StubUtility.getTodoTaskTag(javaProject);
			if (todoTaskTag == null)
				return "XXX"; //$NON-NLS-1$

			return todoTaskTag;
		}
	}	
/*
	protected static class Arguments extends SimpleTemplateVariable {
	    public Arguments() {
	     	super("arguments", TemplateMessages.getString("JavaContextType.variable.description.arguments"), "");   
	    }
	}
*/	


	/**
	 * Creates a java context type.
	 */
	public JavaContextType() {
		super(NAME);
		
		// global
		addVariable(new GlobalVariables.Cursor());
		addVariable(new GlobalVariables.WordSelection());
		addVariable(new GlobalVariables.LineSelection());
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Year());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		
		// compilation unit
		addVariable(new File());
		addVariable(new PrimaryTypeName());
		addVariable(new ReturnType());
		addVariable(new Method());
		addVariable(new Type());
		addVariable(new Package());
		addVariable(new Project());
		addVariable(new Arguments());

		// java
		addVariable(new Array());
		addVariable(new ArrayType());
		addVariable(new ArrayElement());
		addVariable(new Index());
		addVariable(new Iterator());
		addVariable(new Collection());
		addVariable(new Todo());
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
		return new JavaContext(this, document, offset, length, compilationUnit);
	}

}
