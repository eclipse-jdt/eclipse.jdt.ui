/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
 * A context type for java code.
 */
public class JavaContextType extends CompilationUnitContextType {

	protected static class Array extends TemplateVariable {
		public Array() {
			super("array", TemplateMessages.getString("JavaContextType.variable.description.array"));
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArray();
	    }
	}

	protected static class ArrayType extends TemplateVariable {
	    public ArrayType() {
	     	super("array_type", TemplateMessages.getString("JavaContextType.variable.description.array.type"));
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayType();
	    }
	}

	protected static class ArrayElement extends TemplateVariable {
	    public ArrayElement() {
	     	super("array_element", TemplateMessages.getString("JavaContextType.variable.description.array.element"));	        
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayElement();
	    }	    
	}

	protected static class Index extends TemplateVariable {
	    public Index() {
	     	super("index", TemplateMessages.getString("JavaContextType.variable.description.index"));
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).getIndex();
	    }	    
	}

	protected static class Collection extends TemplateVariable {
	    public Collection() {
		    super("collection", TemplateMessages.getString("JavaContextType.variable.description.collector"));
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessCollection();
	    }
	}

	protected static class Iterator extends TemplateVariable {
	    public Iterator() {
		    super("collection", TemplateMessages.getString("JavaContextType.variable.description.iterator"));
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).getIterator();
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
		super("java");
		
		// global
		addVariable(new GlobalVariables.Cursor());
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		
		// compilation unit
		addVariable(new File());
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
	}
	
	/*
	 * @see ContextType#createContext()
	 */	
	public TemplateContext createContext() {
		return new JavaContext(this, fString, fPosition, fCompilationUnit);
	}

}
