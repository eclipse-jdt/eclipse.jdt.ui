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
 *     Microsoft Corporation - extracted from org.eclipse.jdt.internal.corext.template.java.JavaContext
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TemplateUtils {

	private TemplateUtils() {

	}

	/**
	 * Evaluates a 'java' template in the context of a compilation unit
	 *
	 * @param template the template to be evaluated
	 * @param compilationUnit the compilation unit in which to evaluate the template
	 * @param position the position inside the compilation unit for which to evaluate the template
	 * @return the evaluated template
	 * @throws CoreException in case the template is of an unknown context type
	 * @throws BadLocationException in case the position is invalid in the compilation unit
	 * @throws TemplateException in case the evaluation fails
	 */
	public static String evaluateTemplate(Template template, ICompilationUnit compilationUnit, int position) throws CoreException, BadLocationException, TemplateException {

		TemplateContextType contextType= JavaPlugin.getDefault().getTemplateContextRegistry().getContextType(template.getContextTypeId());
		if (!(contextType instanceof CompilationUnitContextType))
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaTemplateMessages.JavaContext_error_message, null));

		IDocument document= new Document();
		if (compilationUnit != null && compilationUnit.exists())
			document.set(compilationUnit.getSource());

		CompilationUnitContext context= (CompilationUnitContext) ((CompilationUnitContextType) contextType).createContext(document, position, 0, compilationUnit);
		context.setForceEvaluation(true);

		TemplateBuffer buffer= context.evaluate(template);
		if (buffer == null)
			return null;
		return buffer.getString();
	}
}
