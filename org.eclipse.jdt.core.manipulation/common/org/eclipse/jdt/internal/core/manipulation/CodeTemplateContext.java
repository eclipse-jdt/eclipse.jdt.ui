/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

public class CodeTemplateContext extends TemplateContext {

	private String fLineDelimiter;
	private IJavaProject fProject;

	public CodeTemplateContext(String contextTypeName, IJavaProject project, String lineDelim) {
		super(JavaManipulation.getCodeTemplateContextRegistry().getContextType(contextTypeName));
		fLineDelimiter= lineDelim;
		fProject= project;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter == IProject.class) {
			return adapter.cast(getJavaProject().getProject());
		}
		return super.getAdapter(adapter);
	}

	public IJavaProject getJavaProject() {
		return fProject;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.TemplateContext#evaluate(org.eclipse.jdt.internal.corext.template.Template)
	 */
	@Override
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
		// test that all variables are defined
		Iterator<TemplateVariableResolver> iterator= getContextType().resolvers();
		while (iterator.hasNext()) {
			TemplateVariableResolver var= iterator.next();
			if (var instanceof CodeTemplateContextType.CodeTemplateVariableResolver) {
				Assert.isNotNull(getVariable(var.getType()), "Variable " + var.getType() + "not defined"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (!canEvaluate(template))
			return null;

		String pattern= changeLineDelimiter(template.getPattern(), fLineDelimiter);

		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(pattern);
		getContextType().resolve(buffer, this);
		return buffer;
	}

	private static String changeLineDelimiter(String code, String lineDelim) {
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(code);
			int nLines= tracker.getNumberOfLines();
			if (nLines == 1) {
				return code;
			}

			StringBuilder buf= new StringBuilder();
			for (int i= 0; i < nLines; i++) {
				if (i != 0) {
					buf.append(lineDelim);
				}
				IRegion region = tracker.getLineInformation(i);
				String line= code.substring(region.getOffset(), region.getOffset() + region.getLength());
				buf.append(line);
			}
			return buf.toString();
		} catch (BadLocationException e) {
			// can not happen
			return code;
		}
	}

	@Override
	public boolean canEvaluate(Template template) {
		return true;
	}

	public void setCompilationUnitVariables(ICompilationUnit cu) {
		setVariable(CodeTemplateContextType.FILENAME, cu.getElementName());
		setVariable(CodeTemplateContextType.PACKAGENAME, cu.getParent().getElementName());
		setVariable(CodeTemplateContextType.PROJECTNAME, cu.getJavaProject().getElementName());
	}

}
