/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
 * A context type for java code.
 */
public class JavaContextType extends CompilationUnitContextType {

	protected static class Array extends TemplateVariable {
		public Array() {
			super(JavaTemplateMessages.getString("JavaContextType.variable.name.array"), JavaTemplateMessages.getString("JavaContextType.variable.description.array")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArray();
	    }
	}

	protected static class ArrayType extends TemplateVariable {
	    public ArrayType() {
	     	super(JavaTemplateMessages.getString("JavaContextType.variable.name.array.type"), JavaTemplateMessages.getString("JavaContextType.variable.description.array.type")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayType();
	    }
	}

	protected static class ArrayElement extends TemplateVariable {
	    public ArrayElement() {
	     	super(JavaTemplateMessages.getString("JavaContextType.variable.name.array.element"), JavaTemplateMessages.getString("JavaContextType.variable.description.array.element"));	//$NON-NLS-1$ //$NON-NLS-2$    
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayElement();
	    }	    
	}

	protected static class Index extends TemplateVariable {
	    public Index() {
	     	super(JavaTemplateMessages.getString("JavaContextType.variable.name.index"), JavaTemplateMessages.getString("JavaContextType.variable.description.index")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).getIndex();
	    }	    
	}

	protected static class Collection extends TemplateVariable {
	    public Collection() {
		    super(JavaTemplateMessages.getString("JavaContextType.variable.name.collection"), JavaTemplateMessages.getString("JavaContextType.variable.description.collection")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessCollection();
	    }
	}

	protected static class Iterator extends TemplateVariable {

	    public Iterator() {
		    super(JavaTemplateMessages.getString("JavaContextType.variable.name.iterator"), JavaTemplateMessages.getString("JavaContextType.variable.description.iterator")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	    	JavaContext javaContext= (JavaContext) context;

			if (!context.isReadOnly())
		    	javaContext.addIteratorImport();
	    	
	        return javaContext.getIterator();
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
		super("java"); //$NON-NLS-1$
		
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
		return new JavaContext(this, fDocument, fPosition, fCompilationUnit);
	}

}
