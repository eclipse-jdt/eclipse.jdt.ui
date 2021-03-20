/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487901, 488432
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;


/**
 * Compilation unit context type.
 */
public abstract class CompilationUnitContextType extends TemplateContextType {

	protected static class ReturnType extends TemplateVariableResolver {
	 	public ReturnType() {
	 	 	super("return_type", JavaTemplateMessages.CompilationUnitContextType_variable_description_return_type);  //$NON-NLS-1$
	 	}
	 	@Override
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
			super("file", JavaTemplateMessages.CompilationUnitContextType_variable_description_file);  //$NON-NLS-1$
		}
		@Override
		protected String resolve(TemplateContext context) {
			ICompilationUnit unit= ((CompilationUnitContext) context).getCompilationUnit();

			return (unit == null) ? null : unit.getElementName();
		}

		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#isUnambiguous(org.eclipse.jface.text.templates.TemplateContext)
		 */
		@Override
		protected boolean isUnambiguous(TemplateContext context) {
			return resolve(context) != null;
		}
	}

	protected static class PrimaryTypeName extends TemplateVariableResolver {
		public PrimaryTypeName() {
			super("primary_type_name", JavaTemplateMessages.CompilationUnitContextType_variable_description_primary_type_name);  //$NON-NLS-1$

		}
		@Override
		protected String resolve(TemplateContext context) {
			ICompilationUnit unit= ((CompilationUnitContext) context).getCompilationUnit();
			if (unit == null)
				return null;
			return JavaCore.removeJavaLikeExtension(unit.getElementName());
		}

		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#isUnambiguous(org.eclipse.jface.text.templates.TemplateContext)
		 */
		@Override
		protected boolean isUnambiguous(TemplateContext context) {
			return resolve(context) != null;
		}
	}

	protected static class EnclosingJavaElement extends TemplateVariableResolver {
		protected final int fElementType;

		public EnclosingJavaElement(String name, String description, int elementType) {
			super(name, description);
			fElementType= elementType;
		}
		@Override
		protected String resolve(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(fElementType);
			if (element instanceof IType)
				return JavaElementLabelsCore.getElementLabel(element, JavaElementLabelsCore.T_CONTAINER_QUALIFIED);
			return (element == null) ? null : element.getElementName();
		}

		/*
		 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#isUnambiguous(org.eclipse.jface.text.templates.TemplateContext)
		 */
		@Override
		protected boolean isUnambiguous(TemplateContext context) {
			return resolve(context) != null;
		}
	}

	protected static class Method extends EnclosingJavaElement {
		public Method() {
			super("enclosing_method", JavaTemplateMessages.CompilationUnitContextType_variable_description_enclosing_method, IJavaElement.METHOD);  //$NON-NLS-1$
		}
	}

	protected static class Type extends EnclosingJavaElement {
		public Type() {
			super("enclosing_type", JavaTemplateMessages.CompilationUnitContextType_variable_description_enclosing_type, IJavaElement.TYPE);  //$NON-NLS-1$
		}
	}

	protected static class Package extends EnclosingJavaElement {
		public Package() {
			super("enclosing_package", JavaTemplateMessages.CompilationUnitContextType_variable_description_enclosing_package, IJavaElement.PACKAGE_FRAGMENT);  //$NON-NLS-1$
		}
	}

	protected static class Project extends EnclosingJavaElement {
		public Project() {
			super("enclosing_project", JavaTemplateMessages.CompilationUnitContextType_variable_description_enclosing_project, IJavaElement.JAVA_PROJECT);  //$NON-NLS-1$
		}
	}

	protected static class Arguments extends TemplateVariableResolver {
		public Arguments() {
			super("enclosing_method_arguments", JavaTemplateMessages.CompilationUnitContextType_variable_description_enclosing_method_arguments);  //$NON-NLS-1$
		}
		@Override
		protected String resolve(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;

			IMethod method= (IMethod) element;

			try {
				String[] arguments= method.getParameterNames();
				return String.join(", ", arguments); //$NON-NLS-1$

			} catch (JavaModelException e) {
				return null;
			}
		}
	}

	/*
	 * @see ContextType#ContextType(String)
	 */
	public CompilationUnitContextType(String id) {
		super(id);
	}

	public CompilationUnitContextType() {
	}

	public abstract DocumentTemplateContext createContext(IDocument document, int completionPosition, int length, ICompilationUnit compilationUnit);
	public abstract DocumentTemplateContext createContext(IDocument document, Position completionPosition, ICompilationUnit compilationUnit);

	@Override
	protected void validateVariables(TemplateVariable[] variables) throws TemplateException {
		// check for multiple cursor variables
		for (TemplateVariable var : variables) {
			if (GlobalTemplateVariables.Cursor.NAME.equals(var.getType())) {
				if (var.getOffsets().length > 1) {
					throw new TemplateException(JavaTemplateMessages.ContextType_error_multiple_cursor_variables);
				}
			}
		}
	}

}
