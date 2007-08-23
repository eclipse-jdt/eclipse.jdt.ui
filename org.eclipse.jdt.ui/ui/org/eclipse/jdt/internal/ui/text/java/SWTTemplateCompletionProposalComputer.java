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

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.SWTContextType;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;

/**
 * @since 3.4
 */
public class SWTTemplateCompletionProposalComputer extends TemplateCompletionProposalComputer {

	private final TemplateEngine fSWTTemplateEngine;

	public SWTTemplateCompletionProposalComputer() {
		ContextTypeRegistry templateContextRegistry= JavaPlugin.getDefault().getTemplateContextRegistry();

		SWTContextType contextType= (SWTContextType) templateContextRegistry.getContextType(SWTContextType.NAME);
		if (contextType == null) {
			contextType= new SWTContextType();
			templateContextRegistry.addContextType(contextType);

			TemplateContextType otherContextType= templateContextRegistry.getContextType(JavaContextType.NAME);
			if (otherContextType == null) {
				otherContextType= new JavaContextType();
				templateContextRegistry.addContextType(otherContextType);
			}

			contextType.inheritResolvers(otherContextType);
		}

		fSWTTemplateEngine= new TemplateEngine(contextType) {
			
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine#createTemplateProposal(org.eclipse.jface.text.templates.Template, org.eclipse.jface.text.IRegion, org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext)
			 */
			protected TemplateProposal createTemplateProposal(Template template, IRegion region, CompilationUnitContext context) {
				TemplateProposal result= new TemplateProposal(template, context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SWT_TEMPLATE));
				return result;
			}		
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.TemplateCompletionProposalComputer#computeCompletionEngine(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected TemplateEngine computeCompletionEngine(JavaContentAssistInvocationContext context) {

		ICompilationUnit unit= context.getCompilationUnit();
		if (unit == null)
			return null;

		try {
			IType type= unit.getJavaProject().findType("org.eclipse.swt.SWT"); //$NON-NLS-1$
			if (type == null)
				return null;
		} catch (JavaModelException e) {
			return null;
		}

		return fSWTTemplateEngine;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
	 */
	public void sessionEnded() {
		fSWTTemplateEngine.reset();
	}

}
