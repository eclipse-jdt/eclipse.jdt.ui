/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.ICompilationUnit;


/**
 * The context for templates inside SWT code.
 * 
 * @since 3.4
 */
public class SWTContext extends JavaContext {

	/*
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public SWTContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength, ICompilationUnit compilationUnit) {
		super(type, document, completionOffset, completionLength, compilationUnit);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position, org.eclipse.jdt.core.ICompilationUnit)
	 */
	public SWTContext(TemplateContextType type, IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		super(type, document, completionPosition, compilationUnit);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.JavaContext#canEvaluate(org.eclipse.jface.text.templates.Template)
	 */
	public boolean canEvaluate(Template template) {
		if (fForceEvaluation)
			return true;

		String key= getKey();
		return template.matches(key, getContextType().getId()) && (key.length() == 0 || template.getName().toLowerCase().startsWith(key.toLowerCase()));
	}
}