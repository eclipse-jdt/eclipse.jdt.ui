/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.*;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.text.template.contentassist.*;
import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariableGuess;

/**
 * A context type for java code.
 */
public class JavaContextType extends CompilationUnitContextType {

	public static final String NAME= "java"; //$NON-NLS-1$

	protected static class Array extends TemplateVariableResolver {
		public Array() {
			super("array", JavaTemplateMessages.getString("JavaContextType.variable.description.array")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		protected String[] resolveAll(TemplateContext context) {
	        return ((JavaContext) context).getArrays();
	    }
		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
		 */
		public void resolve(TemplateVariable variable, TemplateContext context) {
			if (variable instanceof MultiVariable) {
				JavaContext jc= (JavaContext) context;
				MultiVariable mv= (MultiVariable) variable;
				String[] bindings= resolveAll(context);
				if (bindings.length > 0) {
					mv.setValues(bindings);
					MultiVariableGuess guess= jc.getMultiVariableGuess();
					if (guess == null) {
						guess= new MultiVariableGuess(mv);
						jc.setMultiVariableGuess(guess);
					}
				}
				if (bindings.length > 1)
					variable.setUnambiguous(false);
				else
					variable.setUnambiguous(isUnambiguous(context));
			} else
				super.resolve(variable, context);
		}
	}

	protected static class ArrayType extends TemplateVariableResolver {
	    public ArrayType() {
	     	super("array_type", JavaTemplateMessages.getString("JavaContextType.variable.description.array.type")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    protected String[] resolveAll(TemplateContext context) {
	        
	    	String[][] arrayTypes= ((JavaContext) context).getArrayTypes();
	    	String[] types= new String[arrayTypes.length];
	    	for (int i= 0; i < types.length; i++) {
				types[i]= arrayTypes[i][0];
			}
	    	return types;
	    }
	    
		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
		 */
		public void resolve(TemplateVariable variable, TemplateContext context) {
			if (variable instanceof MultiVariable) {
				MultiVariable mv= (MultiVariable) variable;
				String[] arrays= ((JavaContext) context).getArrays();
				String[][] types= ((JavaContext) context).getArrayTypes();
				
				for (int i= 0; i < arrays.length; i++) {
					mv.setValues(arrays[i], types[i]);
				}

				if (arrays.length > 1 || types.length == 1 && types[0].length > 1)
					variable.setUnambiguous(false);
				else
					variable.setUnambiguous(isUnambiguous(context));
				
			} else {
				super.resolve(variable, context);
			}
		}
	}

	protected static class ArrayElement extends TemplateVariableResolver {
	    public ArrayElement() {
	     	super("array_element", JavaTemplateMessages.getString("JavaContextType.variable.description.array.element"));	//$NON-NLS-1$ //$NON-NLS-2$    
	    }
	    protected String[] resolveAll(TemplateContext context) {
	        String[][] groupedElements= ((JavaContext) context).getArrayElements();

	        String[] elements= new String[groupedElements.length];
	    	for (int i= 0; i < elements.length; i++) {
				elements[i]= groupedElements[i][0];
			}
	    	return elements;
	    }	    

		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
		 */
		public void resolve(TemplateVariable variable, TemplateContext context) {
			if (variable instanceof MultiVariable) {
				MultiVariable mv= (MultiVariable) variable;
				String[] arrays= ((JavaContext) context).getArrays();
				String[][] elems= ((JavaContext) context).getArrayElements();
				
				for (int i= 0; i < arrays.length; i++) {
					mv.setValues(arrays[i], elems[i]);
				}

				if (arrays.length > 1 || elems.length == 1 && elems[0].length > 1)
					variable.setUnambiguous(false);
				else
					variable.setUnambiguous(isUnambiguous(context));
				
			} else
				super.resolve(variable, context);
		}
	}

	protected static class Index extends TemplateVariableResolver {
	    public Index() {
	     	super("index", JavaTemplateMessages.getString("JavaContextType.variable.description.index")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    protected String resolve(TemplateContext context) {
	        return ((JavaContext) context).getIndex();
	    }	    
	}

	protected static class Collection extends TemplateVariableResolver {
	    public Collection() {
		    super("collection", JavaTemplateMessages.getString("JavaContextType.variable.description.collection")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    
		protected String[] resolveAll(TemplateContext context) {
	    	String[] collections= ((JavaContext) context).getCollections();
	    	if (collections.length > 0)
	    		return collections;
	    	return super.resolveAll(context);
		}
	}

	protected static class Iterable extends TemplateVariableResolver {
	    public Iterable() {
		    super("iterable", JavaTemplateMessages.getString("JavaContextType.variable.description.iterable")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    
		protected String[] resolveAll(TemplateContext context) {
	    	String[] iterables= ((JavaContext) context).getIterables();
	    	if (iterables.length > 0)
	    		return iterables;
	    	return super.resolveAll(context);
		}
		
		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
		 */
		public void resolve(TemplateVariable variable, TemplateContext context) {
			if (variable instanceof MultiVariable) {
				JavaContext jc= (JavaContext) context;
				MultiVariable mv= (MultiVariable) variable;
				String[] bindings= resolveAll(context);
				if (bindings.length > 0) {
					mv.setValues(bindings);
					MultiVariableGuess guess= jc.getMultiVariableGuess();
					if (guess == null) {
						guess= new MultiVariableGuess(mv);
						jc.setMultiVariableGuess(guess);
					}
				}
				if (bindings.length > 1)
					variable.setUnambiguous(false);
				else
					variable.setUnambiguous(isUnambiguous(context));
			} else
				super.resolve(variable, context);
		}
	}
	
	protected static class IterableType extends TemplateVariableResolver {
	    public IterableType() {
	     	super("iterable_type", JavaTemplateMessages.getString("JavaContextType.variable.description.iterable.type")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    
	    protected String[] resolveAll(TemplateContext context) {
	    	String[][] iterableTypes= ((JavaContext) context).getIterableTypes();
	    	String[] types= new String[iterableTypes.length];
	    	for (int i= 0; i < iterableTypes.length; i++) {
				types[i]= iterableTypes[i][0];
			}
	    	if (types.length > 0)
	    		return types;
	    	return super.resolveAll(context);
	    }
	    
		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
		 */
		public void resolve(TemplateVariable variable, TemplateContext context) {
			if (variable instanceof MultiVariable) {
				MultiVariable mv= (MultiVariable) variable;
				String[] iterables= ((JavaContext) context).getIterables();
				String[][] types= ((JavaContext) context).getIterableTypes();
				
				for (int i= 0; i < iterables.length; i++) {
					mv.setValues(iterables[i], types[i]);
				}

				if (iterables.length > 1 || types.length == 1 && types[0].length > 1)
					variable.setUnambiguous(false);
				else
					variable.setUnambiguous(isUnambiguous(context));
				
			} else {
				super.resolve(variable, context);
			}
		}
	}

	protected static class IterableElement extends TemplateVariableResolver {
	    public IterableElement() {
	     	super("iterable_element", JavaTemplateMessages.getString("JavaContextType.variable.description.iterable.element"));	//$NON-NLS-1$ //$NON-NLS-2$    
	    }
	    
	    protected String[] resolveAll(TemplateContext context) {
	    	List result= new ArrayList();
	        String[][] groupedElements= ((JavaContext) context).getIterableElements();
	        for (int i= 0; i < groupedElements.length; i++) {
				String[] names= groupedElements[i];
				for (int j= 0; j < names.length; j++) {
					result.add(names[j]);
				}
			}
	        
			return (String[]) result.toArray(new String[result.size()]);
	    }	    

		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
		 */
		public void resolve(TemplateVariable variable, TemplateContext context) {
			if (variable instanceof MultiVariable) {
				MultiVariable mv= (MultiVariable) variable;
				String[] iterables= ((JavaContext) context).getIterables();
				String[][] elems= ((JavaContext) context).getIterableElements();
				
				for (int i= 0; i < iterables.length; i++) {
					mv.setValues(iterables[i], elems[i]);
				}

				if (iterables.length > 1 || elems.length == 1 && elems[0].length > 1)
					variable.setUnambiguous(false);
				else
					variable.setUnambiguous(isUnambiguous(context));
				
			} else
				super.resolve(variable, context);
		}
	}

	protected static class Iterator extends TemplateVariableResolver {

	    public Iterator() {
		    super("iterator", JavaTemplateMessages.getString("JavaContextType.variable.description.iterator")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    protected String resolve(TemplateContext context) {
	    	JavaContext javaContext= (JavaContext) context;

			if (!context.isReadOnly())
		    	javaContext.addIteratorImport();
	    	
	        return javaContext.getIterator();
	    }	    
	}
	
	protected static class Todo extends TemplateVariableResolver {

		public Todo() {
			super("todo", JavaTemplateMessages.getString("JavaContextType.variable.description.todo")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		protected String resolve(TemplateContext context) {
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
	protected static class Arguments extends SimpleVariableResolver {
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
		addResolver(new GlobalTemplateVariables.Cursor());
		addResolver(new GlobalTemplateVariables.WordSelection());
		addResolver(new GlobalTemplateVariables.LineSelection());
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());
		
		// compilation unit
		addResolver(new File());
		addResolver(new PrimaryTypeName());
		addResolver(new ReturnType());
		addResolver(new Method());
		addResolver(new Type());
		addResolver(new Package());
		addResolver(new Project());
		addResolver(new Arguments());

		// java
		addResolver(new Array());
		addResolver(new ArrayType());
		addResolver(new ArrayElement());
		addResolver(new Index());
		addResolver(new Iterator());
		addResolver(new Collection());
		addResolver(new Iterable());
		addResolver(new IterableType());
		addResolver(new IterableElement());
		addResolver(new Todo());
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
		return new JavaContext(this, document, offset, length, compilationUnit);
	}

}
