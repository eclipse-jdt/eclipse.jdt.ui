/*******************************************************************************
 * Copyright (c) 2019, 2020 Nicolaj Hoess and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nicolaj Hoess - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.Assert;

import org.eclipse.text.templates.ContextTypeRegistry;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.template.java.JavaPostfixContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.CompletionTimeoutProgressMonitor;
import org.eclipse.jdt.internal.ui.text.template.contentassist.PostfixTemplateEngine;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;

/**
 * Computer that computes the template proposals for the Java postfix type.
 */
public class PostfixCompletionProposalComputer extends AbstractTemplateCompletionProposalComputer {

	private final PostfixTemplateEngine postfixCompletionTemplateEngine;

	public PostfixCompletionProposalComputer() {
		ContextTypeRegistry templateContextRegistry= JavaPlugin.getDefault().getTemplateContextRegistry();
		postfixCompletionTemplateEngine= createTemplateEngine(templateContextRegistry, JavaPostfixContextType.ID_ALL);
	}

	private static PostfixTemplateEngine createTemplateEngine(ContextTypeRegistry templateContextRegistry, String contextTypeId) {
		TemplateContextType contextType= templateContextRegistry.getContextType(contextTypeId);
		Assert.isNotNull(contextType);
		return new PostfixTemplateEngine(contextType);
	}

	@Override
	protected TemplateEngine computeCompletionEngine(JavaContentAssistInvocationContext context) {
		ICompilationUnit unit= context.getCompilationUnit();
		if (unit == null) {
			return null;
		}

		IJavaProject javaProject= unit.getJavaProject();
		if (javaProject == null) {
			return null;
		}

		ITextSelection textSelection= context.getTextSelection();
		if (textSelection != null && textSelection.getLength() > 0) {
			// If there is an active selection we must not contribute to the CA
			return null;
		}

		CompletionContext coreContext= context.getCoreContext();
		if (coreContext != null) {
			int tokenLocation= coreContext.getTokenLocation();
			int tokenStart= coreContext.getTokenStart();
			int tokenKind= coreContext.getTokenKind();
			if ((tokenLocation == 0 && tokenStart > -1)
					|| ((tokenLocation & CompletionContext.TL_MEMBER_START) != 0 && tokenKind == CompletionContext.TOKEN_KIND_NAME && tokenStart > -1)
					|| (tokenLocation == 0 && isAfterTrigger(context.getDocument(), context.getInvocationOffset()))) {

				analyzeCoreContext(context, coreContext);
				return postfixCompletionTemplateEngine;
			}
		}
		return null;
	}

	private void analyzeCoreContext(JavaContentAssistInvocationContext context,
			CompletionContext coreContext) {
		// If the coreContext is not extended atm for some reason we have to extend it ourself in order get to the needed information
		if (coreContext.isExtended()) {
			updateTemplateEngine(coreContext);
		} else {
			final ICompilationUnit cu= context.getCompilationUnit();
			final CompletionProposalCollector collector= new CompletionProposalCollector(cu) {
				@Override
				public void acceptContext(final CompletionContext c) {
					super.acceptContext(c);
					updateTemplateEngine(c);
				}
			};
			collector.setInvocationContext(context);
			collector.setRequireExtendedContext(true);
			try {
				cu.codeComplete(context.getInvocationOffset(), collector, new CompletionTimeoutProgressMonitor());
			} catch (JavaModelException e) {
				// continue
			}
		}
	}

	private void updateTemplateEngine(CompletionContext context) {
		IJavaElement enclosingElement= context.getEnclosingElement();
		if (enclosingElement == null) {
			return;
		}

		int tokenLength= context.getToken() != null ? context.getToken().length : 0;
		int invOffset= context.getOffset() - tokenLength - 1;

		ICompilationUnit cu= (ICompilationUnit) enclosingElement.getAncestor(IJavaElement.COMPILATION_UNIT);
		CompilationUnit cuRoot= SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_NO, null);
		if (cuRoot == null) {
			cuRoot= (CompilationUnit) createPartialParser(cu, invOffset).createAST(null);
		}

		if (enclosingElement instanceof IMember) {
			ISourceRange sr;
			try {
				sr= ((IMember) enclosingElement).getSourceRange();
				if (sr == null) {
					return;
				}
			} catch (JavaModelException e) {
				return;
			}

			ASTNode completionNode= NodeFinder.perform(cuRoot, sr);
			if (completionNode == null) {
				return;
			}

			ASTNode[] bestNode= new ASTNode[] { completionNode };
			completionNode.accept(new ASTVisitor() {
				@Override
				public boolean visit(StringLiteral node) {
					int start= node.getStartPosition();
					if (invOffset > start && start >= bestNode[0].getStartPosition()) {
						bestNode[0]= node;
					}
					return true;
				}

				@Override
				public boolean visit(ExpressionStatement node) {
					int start= node.getStartPosition();
					if (invOffset > start && start >= bestNode[0].getStartPosition()) {
						bestNode[0]= node;
					}
					return true;
				}

				@Override
				public boolean visit(SimpleName node) {
					int start= node.getStartPosition();
					if (invOffset > start && start >= bestNode[0].getStartPosition()) {
						bestNode[0]= node;
					}
					return true;
				}

				@Override
				public boolean visit(QualifiedName node) {
					int start= node.getStartPosition();
					if (invOffset > start && start >= bestNode[0].getStartPosition()) {
						bestNode[0]= node;
					}
					return true;
				}

				@Override
				public boolean visit(BooleanLiteral node) {
					int start= node.getStartPosition();
					if (invOffset > start && start >= bestNode[0].getStartPosition()) {
						bestNode[0]= node;
					}
					return true;
				}

				@Override
				public boolean visit(MethodInvocation node) {
					return visit((Expression)node);
				}

				@Override
				public boolean visit(SuperMethodInvocation node) {
					return visit((Expression)node);
				}

				@Override
				public boolean visit(ClassInstanceCreation node) {
					return visit((Expression)node);
				}

				/**
				 * Does NOT override {@link ASTVisitor}
				 * Handle {@link MethodInvocation}, {@link SuperMethodInvocation}
				 * and {@link ClassInstanceCreation}
				 */
				public boolean visit(Expression node) {
					/*
					 * Do not consider a method invocation node as the best node
					 * if it is RECOVERED. A recovered node may in fact be open
					 * 'System.out.println(...' and the best node may be within
					 * the invocation.
					 *
					 * See PostFixCompletionTest#testConcatenatedShorthandIfStatement()
					 */
					if ((node.getFlags() & ASTNode.RECOVERED) == 0) {
						int start= node.getStartPosition();
						int end= start + node.getLength() - 1;
						if (invOffset > start && invOffset == end + 1) {
							bestNode[0]= node;
							return false;
						}
					}
					return true;
				}
			});

			completionNode= bestNode[0];
			Statement s= completionNode instanceof Statement stmt ? stmt : ASTNodes.getFirstAncestorOrNull(completionNode, Statement.class);
			if (s != null && s.getStartPosition() < invOffset && s.getStartPosition() + s.getLength() >= invOffset) {
				ASTNode completionNodeParent= findBestMatchingParentNode(completionNode);
				postfixCompletionTemplateEngine.setASTNodes(completionNode, completionNodeParent);
				postfixCompletionTemplateEngine.setContext(context);
			} else {
				postfixCompletionTemplateEngine.reset();
			}
		}
	}

	/**
	 * This method determines the best matching parent {@link ASTNode} of the given {@link ASTNode}.
	 * Consider the following example for the definition of <i>best matching parent</i>:<br/>
	 * <code>("two" + 2).var$</code> has <code>"two"</code> as completion {@link ASTNode}. The
	 * parent node is <code>"two" + 2</code> which will result in a syntactically incorrect result,
	 * if the template is applied, because the parentheses aren't taken into account.
	 *
	 * @param node The current {@link ASTNode}
	 * @return {@link ASTNode} which either is the parent of the given node or another predecessor
	 *         {@link ASTNode} in the abstract syntax tree.
	 */
	private ASTNode findBestMatchingParentNode(ASTNode node) {
		ASTNode result= node.getParent();
		if (result instanceof InfixExpression) {
			ASTNode completionNodeGrandParent= result.getParent();
			int safeGuard= 0;
			while (completionNodeGrandParent instanceof ParenthesizedExpression && safeGuard++ < 64) {
				result= completionNodeGrandParent;
				completionNodeGrandParent= result.getParent();
			}
		}
		if (node instanceof SimpleName && result instanceof SimpleType) {
			ASTNode completionNodeGrandParent= result.getParent();
			if (completionNodeGrandParent instanceof ClassInstanceCreation) {
				result= completionNodeGrandParent;
			}
		}
		return result;
	}

	/**
	 * Returns true if the given offset is directly after an assist trigger character.
	 *
	 * @param document the actual document of type {@link IDocument}
	 * @param offset the current location in the document
	 * @return <code>true</code> if the given offset is directly after an assist trigger character,
	 *         <code>false</code> otherwise. If the given offset is out of the given document
	 *         <code>false</code> is returned.
	 */
	private boolean isAfterTrigger(IDocument document, int offset) {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		try {
			return triggers.contains(document.get(offset - 1, 1));
		} catch (BadLocationException e) {
			return false;
		}
	}

	private static ASTParser createPartialParser(ICompilationUnit cu, int position) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(cu.getJavaProject());
		parser.setSource(cu);
		parser.setFocalPosition(position);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		return parser;
	}
}
