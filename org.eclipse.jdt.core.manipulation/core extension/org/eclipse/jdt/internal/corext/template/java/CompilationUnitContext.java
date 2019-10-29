/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;


/**
 * A compilation unit context.
 */
public class CompilationUnitContext extends DocumentTemplateContext {

	/** The compilation unit, may be <code>null</code>. */
	private final ICompilationUnit fCompilationUnit;
	/** A flag to force evaluation in head-less mode. */
	protected boolean fForceEvaluation;
	/** <code>true</code> if the context has a managed (i.e. added to the document) position, <code>false</code> otherwise. */
	private final boolean fIsManaged;

	/**
	 * Creates a compilation unit context.
	 *
	 * @param type   the context type
	 * @param document the document
	 * @param completionOffset the completion position within the document
	 * @param completionLength the completion length within the document
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 */
	public CompilationUnitContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength, ICompilationUnit compilationUnit) {
		super(type, document, completionOffset, completionLength);
		fCompilationUnit= compilationUnit;
		fIsManaged= false;
	}

	/**
	 * Creates a compilation unit context.
	 *
	 * @param type   the context type
	 * @param document the document
	 * @param completionPosition the position defining the completion offset and length
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 * @since 3.2
	 */
	public CompilationUnitContext(TemplateContextType type, IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		super(type, document, completionPosition);
		fCompilationUnit= compilationUnit;
		fIsManaged= true;
	}

	/**
	 * Returns the compilation unit if one is associated with this context,
	 * <code>null</code> otherwise.
	 *
	 * @return the compilation unit of this context or <code>null</code>
	 */
	public final ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the enclosing element of a particular element type,
	 * <code>null</code> if no enclosing element of that type exists.
	 *
	 * @param elementType the element type
	 * @return the enclosing element of the given type or <code>null</code>
	 */
	public IJavaElement findEnclosingElement(int elementType) {
		if (fCompilationUnit == null)
			return null;

		try {
			IJavaElement element= fCompilationUnit.getElementAt(getStart());
			if (element == null) {
				element= fCompilationUnit;
			}

			return element.getAncestor(elementType);

		} catch (JavaModelException e) {
			return null;
		}
	}

	/**
	 * Sets whether evaluation is forced or not.
	 *
	 * @param evaluate <code>true</code> in order to force evaluation,
	 *            <code>false</code> otherwise
	 */
	public void setForceEvaluation(boolean evaluate) {
		fForceEvaluation= evaluate;
	}

	/**
	 * Gets whether evaluation is forced or not.
	 * @return whether evaluation is forced or not.
	 */
	public boolean isForceEvaluation() {
		return fForceEvaluation;
	}

	/**
	 * Gets if the context has a managed position.
	 * @return if the context has a managed position.
	 */
	public boolean isManaged() {
		return fIsManaged;
	}

	public IJavaProject getJavaProject() {
		ICompilationUnit compilationUnit= getCompilationUnit();
		IJavaProject project= compilationUnit == null ? null : compilationUnit.getJavaProject();
		return project;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter == IProject.class) {
			return adapter.cast(getJavaProject().getProject());
		}
		return super.getAdapter(adapter);
	}
}
