/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.CompletionContext;

import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContextType;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;


/**
 * Computer computing template proposals for Java and Javadoc context type.
 *
 * @since 3.2
 */
public class TemplateCompletionProposalComputer extends AbstractTemplateCompletionProposalComputer {

	private final TemplateEngine fJavaTemplateEngine;
	private final TemplateEngine fJavaStatementsTemplateEngine;
	private final TemplateEngine fJavaMembersTemplateEngine;
	private final TemplateEngine fJavaModuleTemplateEngine;
	private final TemplateEngine fJavaEmptyTemplateEngine;

	private final TemplateEngine fJavadocTemplateEngine;

	public TemplateCompletionProposalComputer() {
		ContextTypeRegistry templateContextRegistry= JavaPlugin.getDefault().getTemplateContextRegistry();
		fJavaTemplateEngine= createTemplateEngine(templateContextRegistry, JavaContextType.ID_ALL);
		fJavaMembersTemplateEngine= createTemplateEngine(templateContextRegistry, JavaContextType.ID_MEMBERS);
		fJavaStatementsTemplateEngine= createTemplateEngine(templateContextRegistry, JavaContextType.ID_STATEMENTS);
		fJavadocTemplateEngine= createTemplateEngine(templateContextRegistry, JavaDocContextType.ID);
		fJavaModuleTemplateEngine= createTemplateEngine(templateContextRegistry, JavaContextType.ID_MODULE);
		fJavaEmptyTemplateEngine= createTemplateEngine(templateContextRegistry, JavaContextType.ID_EMPTY);
	}

	private static TemplateEngine createTemplateEngine(ContextTypeRegistry templateContextRegistry, String contextTypeId) {
		TemplateContextType contextType= templateContextRegistry.getContextType(contextTypeId);
		Assert.isNotNull(contextType);
		return new TemplateEngine(contextType);
	}

	@Override
	protected TemplateEngine computeCompletionEngine(JavaContentAssistInvocationContext context) {
		try {
			String partition= TextUtilities.getContentType(context.getDocument(), IJavaPartitions.JAVA_PARTITIONING, context.getInvocationOffset(), true);
			if (IJavaPartitions.JAVA_DOC.equals(partition))
				return fJavadocTemplateEngine;
			else {
				CompletionContext coreContext= context.getCoreContext();
				if (coreContext != null) {
					int tokenLocation= coreContext.getTokenLocation();
					if ((tokenLocation & CompletionContext.TL_MEMBER_START) != 0) {
						return fJavaMembersTemplateEngine;
					}
					if ((tokenLocation & CompletionContext.TL_STATEMENT_START) != 0) {
						return fJavaStatementsTemplateEngine;
					}
				}
				if (JavaModelUtil.MODULE_INFO_JAVA.equals(context.getCompilationUnit().getElementName())) {
					return fJavaModuleTemplateEngine;
				} else if (context.getDocument().get().trim().length() == 0) {
					return fJavaEmptyTemplateEngine;
				} else {
					return fJavaTemplateEngine;
				}
			}
		} catch (BadLocationException x) {
			return null;
		}
	}

}
