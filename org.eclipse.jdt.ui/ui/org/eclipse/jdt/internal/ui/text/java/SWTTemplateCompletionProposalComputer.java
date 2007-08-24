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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.java.SWTContextType;

import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;

/**
 * @since 3.4
 */
public class SWTTemplateCompletionProposalComputer extends AbstractTemplateCompletionProposalComputer {

	private final TemplateEngine fSWTTemplateEngine;

	public SWTTemplateCompletionProposalComputer() {
		ContextTypeRegistry templateContextRegistry= JavaPlugin.getDefault().getTemplateContextRegistry();
		TemplateContextType contextType= templateContextRegistry.getContextType(SWTContextType.NAME);
		Assert.isNotNull(contextType);
		fSWTTemplateEngine= new TemplateEngine(contextType);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.TemplateCompletionProposalComputer#computeCompletionEngine(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected TemplateEngine computeCompletionEngine(JavaContentAssistInvocationContext context) {
		ICompilationUnit unit= context.getCompilationUnit();
		if (unit == null)
			return null;
		
		try {
			String partition= TextUtilities.getContentType(context.getDocument(), IJavaPartitions.JAVA_PARTITIONING, context.getInvocationOffset(), true);
			if (!partition.equals(IDocument.DEFAULT_CONTENT_TYPE))
				return null;
		} catch (BadLocationException e1) {
			return null;
		}

		try {
			IType type= unit.getJavaProject().findType("org.eclipse.swt.SWT"); //$NON-NLS-1$
			if (type == null)
				return null;
		} catch (JavaModelException e) {
			return null;
		}

		return fSWTTemplateEngine;
	}

}
