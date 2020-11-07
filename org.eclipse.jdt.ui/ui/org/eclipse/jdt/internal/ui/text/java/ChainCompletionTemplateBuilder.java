/**
 * Copyright (c) 2010, 2019 Darmstadt University of Technology and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Olav Lenz - externalize Strings.
 */
package org.eclipse.jdt.internal.ui.text.java;

import static java.text.MessageFormat.format;

import org.eclipse.swt.graphics.Image;

import org.eclipse.text.templates.ContextTypeRegistry;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.CompletionContext;

import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.Chain;
import org.eclipse.jdt.internal.ui.text.ChainElement;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;

/**
 * Creates the templates for a given call chain.
 */
public final class ChainCompletionTemplateBuilder {

	private ChainCompletionTemplateBuilder() {
	}

	public static TemplateProposal create(final Chain chain, final JavaContentAssistInvocationContext context) {
		final String title= ChainElement.createChainCode(chain, true, 0);
		final String body= ChainElement.createChainCode(chain, false, chain.getExpectedDimensions());

		final Template template= new Template(title,
				format("{0,choice,1#1 element|1<{0,number,integer} elements}", chain.getElements().size()), //$NON-NLS-1$
				"java", body, false); //$NON-NLS-1$
		return createTemplateProposal(template, context);
	}

	private static TemplateProposal createTemplateProposal(final Template template,
			final JavaContentAssistInvocationContext contentAssistContext) {
		final DocumentTemplateContext templateContext= createJavaContext(contentAssistContext);
		final Region region= new Region(templateContext.getCompletionOffset(), templateContext.getCompletionLength());
		final TemplateProposal proposal= new TemplateProposal(template, templateContext, region,
				getChainCompletionIcon());
		return proposal;
	}

	private static JavaContext createJavaContext(final JavaContentAssistInvocationContext contentAssistContext) {
		final ContextTypeRegistry templateContextRegistry= JavaPlugin.getDefault().getTemplateContextRegistry();
		final TemplateContextType templateContextType= templateContextRegistry.getContextType(JavaContextType.ID_ALL);
		final CompletionContext ctx= contentAssistContext.getCoreContext();
		final JavaContext javaTemplateContext= new JavaContext(templateContextType, contentAssistContext.getDocument(),
				ctx.getTokenStart(), ctx.getToken().length,
				contentAssistContext.getCompilationUnit());
		javaTemplateContext.setForceEvaluation(true);
		return javaTemplateContext;
	}

	private static Image getChainCompletionIcon() {
		return JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_MISC_PUBLIC);
	}
}
