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

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateTranslator;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
  */
public class CodeTemplateContext extends TemplateContext {
	
	private String fLineDelimiter;
	private int fInitialIndentLevel;
	private IJavaProject fProject;

	public CodeTemplateContext(String contextTypeName, IJavaProject project, String lineDelim, int initialIndentLevel) {
		super(ContextTypeRegistry.getInstance().getContextType(contextTypeName));
		fLineDelimiter= lineDelim;
		fInitialIndentLevel= initialIndentLevel;
		fProject= project;
	}

	public IJavaProject getJavaProject() {
		return fProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.TemplateContext#evaluate(org.eclipse.jdt.internal.corext.template.Template)
	 */
	public TemplateBuffer evaluate(Template template) throws CoreException {
		// test that all variables are defined
		Iterator iterator= getContextType().variableIterator();
		while (iterator.hasNext()) {
			TemplateVariable var= (TemplateVariable) iterator.next();
			if (var instanceof CodeTemplateContextType.CodeTemplateVariable) {
				Assert.isNotNull(getVariable(var.getName()), "Variable " + var.getName() + "not defined"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (!canEvaluate(template))
			return null;
		
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template.getPattern());
		if (buffer == null)
			return null;
		getContextType().edit(buffer, this);
		return buffer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.TemplateContext#canEvaluate(org.eclipse.jdt.internal.corext.template.Template)
	 */
	public boolean canEvaluate(Template template) {
		return true;
	}
	
	public void setCompilationUnitVariables(ICompilationUnit cu) {
		setVariable(CodeTemplateContextType.FILENAME, cu.getElementName());
		setVariable(CodeTemplateContextType.PACKAGENAME, cu.getParent().getElementName());
		setVariable(CodeTemplateContextType.PROJECTNAME, cu.getJavaProject().getElementName());
	}

}
