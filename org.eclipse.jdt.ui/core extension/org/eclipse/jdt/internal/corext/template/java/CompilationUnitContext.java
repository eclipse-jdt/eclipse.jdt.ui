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
import org.eclipse.jface.text.templates.DocumentTemplateContext;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariableGuess;


/**
 * A compilation unit context.
 */
public abstract class CompilationUnitContext extends DocumentTemplateContext {

	/** The compilation unit, may be <code>null</code>. */
	private final ICompilationUnit fCompilationUnit;
	/** A flag to force evaluation in head-less mode. */
	protected boolean fForceEvaluation;
	/** A global state for proposals that change if a master proposal changes. */
	protected MultiVariableGuess fMultiVariableGuess;

	/**
	 * Creates a compilation unit context.
	 * 
	 * @param type   the context type
	 * @param document the document
	 * @param completionOffset the completion position within the document
	 * @param completionLength the completion length within the document
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 */
	protected CompilationUnitContext(ContextType type, IDocument document, int completionOffset,
		int completionLength, ICompilationUnit compilationUnit)
	{
		super(type, document, completionOffset, completionLength);
		fCompilationUnit= compilationUnit;
	}
	
	/**
	 * Returns the compilation unit if one is associated with this context, <code>null</code> otherwise.
	 */
	public final ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the enclosing element of a particular element type, <code>null</code>
	 * if no enclosing element of that type exists.
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
	 * Forces evaluation.
	 */
	public void setForceEvaluation(boolean evaluate) {
		fForceEvaluation= evaluate;	
	}
	
	/**
	 * Returns the multivariable guess - state
	 * @return
	 */
	public MultiVariableGuess getMultiVariableGuess() {
		return fMultiVariableGuess;
	}

	/**
	 * @param multiVariableGuess The multiVariableGuess to set.
	 */
	public void setMultiVariableGuess(MultiVariableGuess multiVariableGuess) {
		fMultiVariableGuess= multiVariableGuess;
	}
}
