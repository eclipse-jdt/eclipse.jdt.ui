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
import org.eclipse.jface.text.templates.ContextType;
import org.eclipse.jface.text.templates.GlobalVariables;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;


/**
 * Compilation unit context type.
 */
public abstract class CompilationUnitContextType extends ContextType {
	
	protected static class ReturnType extends TemplateVariableResolver {
	 	public ReturnType() {
	 	 	super("return_type", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.return.type")); //$NON-NLS-1$ //$NON-NLS-2$
	 	}
	 	protected String resolve(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;

			try {
				return Signature.toString(((IMethod) element).getReturnType());
			} catch (JavaModelException e) {
				return null;
			}
		}
	}

	protected static class File extends TemplateVariableResolver {
		public File() {
			super("file", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.file")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		protected String resolve(TemplateContext context) {
			ICompilationUnit unit= ((CompilationUnitContext) context).getCompilationUnit();
			
			return (unit == null) ? null : unit.getElementName();
		}
	}
	
	protected static class PrimaryTypeName extends TemplateVariableResolver {
		public PrimaryTypeName() {
			super("primary_type_name", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.primary.type.name")); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		protected String resolve(TemplateContext context) {
			ICompilationUnit unit= ((CompilationUnitContext) context).getCompilationUnit();
			if (unit == null) 
				return null;
			String elementName= unit.getElementName();
			return elementName.substring(0, elementName.lastIndexOf('.'));
		}
	}

	protected static class EnclosingJavaElement extends TemplateVariableResolver {
		protected final int fElementType;
		
		public EnclosingJavaElement(String name, String description, int elementType) {
			super(name, description);
			fElementType= elementType;
		}
		protected String resolve(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(fElementType);
			return (element == null) ? null : element.getElementName();			
		}
	}
	
	protected static class Method extends EnclosingJavaElement {
		public Method() {
			super("enclosing_method", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.method"), IJavaElement.METHOD); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected static class Type extends EnclosingJavaElement {
		public Type() {
			super("enclosing_type", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.type"), IJavaElement.TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
/*
	protected static class SuperClass extends EnclosingJavaElement {
		public Type() {
			super("super_class", TemplateMessages.getString("JavaContextType.variable.description.type"), IJavaElement.TYPE);
		}
	}
*/
	protected static class Package extends EnclosingJavaElement {
		public Package() {
			super("enclosing_package", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.package"), IJavaElement.PACKAGE_FRAGMENT); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}	

	protected static class Project extends EnclosingJavaElement {
		public Project() {
			super("enclosing_project", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.project"), IJavaElement.JAVA_PROJECT); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}	
/*
	protected static class Project2 extends TemplateVariableResolver {
		public Project2() {
			super("project", TemplateMessages.getString("JavaContextType.variable.description.project"));
		}
		public String evaluate(TemplateContext context) {
			ICompilationUnit unit= ((JavaContext) context).getUnit();
			return (unit == null) ? null : unit.getJavaProject().getElementName();
		}
	}	
*/
	protected static class Arguments extends TemplateVariableResolver {
		public Arguments() {
			super("enclosing_method_arguments", JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.method.arguments")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		protected String resolve(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;
				
			IMethod method= (IMethod) element;
			
			try {
				String[] arguments= method.getParameterNames();
				StringBuffer buffer= new StringBuffer();
				
				for (int i= 0; i < arguments.length; i++) {
					if (i > 0)
						buffer.append(", "); //$NON-NLS-1$
					buffer.append(arguments[i]);				
				}
				
				return buffer.toString();

			} catch (JavaModelException e) {
				return null;
			}
		}
	}

/*	
	protected static class Line extends TemplateVariableResolver {
		public Line() {
			super("line", TemplateMessages.getString("CompilationUnitContextType.variable.description.line"));
		}
		public String evaluate(TemplateContext context) {
			return ((JavaTemplateContext) context).guessLineNumber();
		}
	}
*/	

	/*
	 * @see ContextType#ContextType(String)
	 */
	public CompilationUnitContextType(String name) {
		super(name);	
	}

	public abstract CompilationUnitContext createContext(IDocument document, int completionPosition, int i, ICompilationUnit compilationUnit);


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#validateVariables(org.eclipse.jdt.internal.corext.template.TemplateVariable[])
	 */
	protected String validateVariables(TemplateVariable[] variables) {
		// check for multiple cursor variables		
		for (int i= 0; i < variables.length; i++) {
			TemplateVariable position= variables[i];
			if (position.getType().equals(GlobalVariables.Cursor.NAME)) {
				if (position.getOffsets().length > 1) {
					return JavaTemplateMessages.getString("ContextType.error.multiple.cursor.variables"); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

}
