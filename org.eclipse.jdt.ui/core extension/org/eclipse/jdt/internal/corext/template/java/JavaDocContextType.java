/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 488432
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;

import org.eclipse.jdt.core.ICompilationUnit;


/**
 * The context type for templates inside Javadoc.
 */
public class JavaDocContextType extends CompilationUnitContextType {


	/**
	 * The id under which this context type is registered
	 */
	public static final String ID= "javadoc"; //$NON-NLS-1$

	/**
	 * Creates a java context type.
	 */
	public JavaDocContextType() {

		// global
		addResolver(new GlobalTemplateVariables.Cursor());
		addResolver(new GlobalTemplateVariables.Selection(GlobalTemplateVariables.LineSelection.NAME, JavaTemplateMessages.CompilationUnitContextType_variable_description_line_selection));
		addResolver(new GlobalTemplateVariables.Selection(GlobalTemplateVariables.WordSelection.NAME, JavaTemplateMessages.JavaDocContextType_variable_description_word_selection));
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());

		// compilation unit
		addResolver(new File());
		addResolver(new PrimaryTypeName());
		addResolver(new Method());
		addResolver(new ReturnType());
		addResolver(new Arguments());
		addResolver(new Type());
		addResolver(new Package());
		addResolver(new Project());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	@Override
	public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
		return new JavaDocContext(this, document, offset, length, compilationUnit);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position, org.eclipse.jdt.core.ICompilationUnit)
	 */
	@Override
	public CompilationUnitContext createContext(IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		return new JavaDocContext(this, document, completionPosition, compilationUnit);
	}
}
