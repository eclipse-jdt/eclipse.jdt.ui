/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeInLambdaCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;


public abstract class ReturnTypeBaseSubProcessor<T> {

	/*
	 * These constants must remain for any interested subclass
	 * that may wish to know precisely which unique proposal is
	 * being created and converted to type T, so that the subclass may
	 * categorize it according to its own logic or wrap it in its own
	 * wrapper class.
	 */
	protected static final int MethodWithConstructorName1= 100;
	protected static final int VoidMethodReturns1= 210;
	protected static final int VoidMethodReturns2= 220;
	protected static final int MissingReturnTypeProposal1= 310;
	protected static final int MissingReturnTypeProposal2= 320;
	protected static final int MissingReturnStatementProposal1= 410;
	protected static final int MissingReturnStatementProposal2= 420;
	protected static final int MissingReturnStatementProposal3= 430;
	protected static final int ReplaceReturnWithYieldStatementProposal1= 510;

	protected ReturnTypeBaseSubProcessor() {
	}

	private static class ReturnStatementCollector extends ASTVisitor {
		private ArrayList<ReturnStatement> fResult= new ArrayList<>();

		public ITypeBinding getTypeBinding(AST ast) {
			boolean couldBeObject= false;
			for (ReturnStatement node : fResult) {
				Expression expr= node.getExpression();
				if (expr != null) {
					ITypeBinding binding= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
					if (binding != null) {
						return binding;
					} else {
						couldBeObject= true;
					}
				} else {
					return ast.resolveWellKnownType("void"); //$NON-NLS-1$
				}
			}
			if (couldBeObject) {
				return ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			return ast.resolveWellKnownType("void"); //$NON-NLS-1$
		}

		@Override
		public boolean visit(ReturnStatement node) {
			fResult.add(node);
			return false;
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			return false;
		}

	}

	public void collectMethodWithConstrNameProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof MethodDeclaration declaration) {
			ASTRewrite rewrite= ASTRewrite.create(declaration.getAST());
			rewrite.set(declaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);

			String label= CorrectionMessages.ReturnTypeSubProcessor_constrnamemethod_description;
			ASTRewriteCorrectionProposalCore p= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.CHANGE_TO_CONSTRUCTOR);
			T proposal= rewriteCorrectionProposalToT(p, MethodWithConstructorName1);
			if (proposal != null)
				proposals.add(proposal);
		}
	}


	public void collectVoidMethodReturnsProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}

		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration methodDeclaration && selectedNode.getNodeType() == ASTNode.RETURN_STATEMENT) {
			ReturnStatement returnStatement= (ReturnStatement) selectedNode;
			Expression expr= returnStatement.getExpression();
			if (expr != null) {
				AST ast= astRoot.getAST();

				ITypeBinding binding= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
				if (binding == null) {
					binding= ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				if (binding.isWildcardType()) {
					binding= ASTResolving.normalizeWildcardType(binding, true, ast);
				}

				ASTRewrite rewrite= ASTRewrite.create(ast);

				String label= Messages.format(CorrectionMessages.ReturnTypeSubProcessor_voidmethodreturns_description, BindingLabelProviderCore.getBindingLabel(binding, BindingLabelProviderCore.DEFAULT_TEXTFLAGS));
				LinkedCorrectionProposalCore proposal = new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.VOID_METHOD_RETURNS);
				ImportRewrite imports= proposal.createImportRewrite(astRoot);
				ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(methodDeclaration, imports);
				Type newReturnType= imports.addImport(binding, ast, importRewriteContext, TypeLocation.RETURN_TYPE);

				if (methodDeclaration.isConstructor()) {
					rewrite.set(methodDeclaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
					rewrite.set(methodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
				} else {
					rewrite.replace(methodDeclaration.getReturnType2(), newReturnType, null);
				}
				String key= "return_type"; //$NON-NLS-1$
				proposal.addLinkedPosition(rewrite.track(newReturnType), true, key);
				for (ITypeBinding b : ASTResolving.getRelaxingTypes(ast, binding)) {
					proposal.addLinkedPositionProposal(key, b);
				}

				Javadoc javadoc= methodDeclaration.getJavadoc();
				if (javadoc != null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_RETURN);
					TextElement commentStart= ast.newTextElement();
					newTag.fragments().add(commentStart);

					JavadocTagsSubProcessorCore.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
					proposal.addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$

				}
				T wrapped= linkedCorrectionProposal1ToT(proposal, VoidMethodReturns1);
				if (wrapped != null)
					proposals.add(wrapped);
			}
			ASTRewrite rewrite= ASTRewrite.create(decl.getAST());
			rewrite.remove(returnStatement.getExpression(), null);

			String label= CorrectionMessages.ReturnTypeSubProcessor_removereturn_description;
			ASTRewriteCorrectionProposalCore core= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.CHANGE_TO_RETURN);
			T proposal= rewriteCorrectionProposalToT(core, VoidMethodReturns2);
			if (proposal != null)
				proposals.add(proposal);
		}
	}


	public void collectMissingReturnTypeProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration methodDeclaration) {
			ReturnStatementCollector eval= new ReturnStatementCollector();
			decl.accept(eval);

			AST ast= astRoot.getAST();

			ITypeBinding typeBinding= eval.getTypeBinding(decl.getAST());
			typeBinding= Bindings.normalizeTypeBinding(typeBinding);
			if (typeBinding == null) {
				typeBinding= ast.resolveWellKnownType("void"); //$NON-NLS-1$
			}
			if (typeBinding.isWildcardType()) {
				typeBinding= ASTResolving.normalizeWildcardType(typeBinding, true, ast);
			}

			ASTRewrite rewrite= ASTRewrite.create(ast);

			String label= Messages.format(CorrectionMessages.ReturnTypeSubProcessor_missingreturntype_description, BindingLabelProviderCore.getBindingLabel(typeBinding, BindingLabelProviderCore.DEFAULT_TEXTFLAGS));
			LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.MISSING_RETURN_TYPE);

			ImportRewrite imports= proposal.createImportRewrite(astRoot);
			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);
			Type type= imports.addImport(typeBinding, ast, importRewriteContext, TypeLocation.RETURN_TYPE);

			rewrite.set(methodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
			rewrite.set(methodDeclaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);

			Javadoc javadoc= methodDeclaration.getJavadoc();
			if (javadoc != null && typeBinding != null) {
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_RETURN);
				TextElement commentStart= ast.newTextElement();
				newTag.fragments().add(commentStart);

				JavadocTagsSubProcessorCore.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
				proposal.addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$
			}

			String key= "return_type"; //$NON-NLS-1$
			proposal.addLinkedPosition(rewrite.track(type), true, key);
			if (typeBinding != null) {
				for (ITypeBinding binding : ASTResolving.getRelaxingTypes(ast, typeBinding)) {
					proposal.addLinkedPositionProposal(key, binding);
				}
			}
			T wrapped= linkedCorrectionProposal1ToT(proposal, MissingReturnTypeProposal1);
			if (wrapped != null)
				proposals.add(wrapped);

			// change to constructor
			ASTNode parentType= ASTResolving.findParentType(decl);
			if (parentType instanceof AbstractTypeDeclaration parentTypeDecl) {
				boolean isInterface= parentType instanceof TypeDeclaration && ((TypeDeclaration) parentType).isInterface();
				if (!isInterface) {
					String constructorName= parentTypeDecl.getName().getIdentifier();
					ASTNode nameNode= methodDeclaration.getName();
					label= Messages.format(CorrectionMessages.ReturnTypeSubProcessor_wrongconstructorname_description, BasicElementLabels.getJavaElementName(constructorName));
					ReplaceCorrectionProposalCore core= new ReplaceCorrectionProposalCore(label, cu, nameNode.getStartPosition(), nameNode.getLength(), constructorName, IProposalRelevance.CHANGE_TO_CONSTRUCTOR);
					T prop= replaceCorrectionProposalToT(core, MissingReturnTypeProposal2);
					if (prop != null)
						proposals.add(prop);
				}
			}
		}
	}


	public void collectMissingReturnStatementProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		ReturnStatement existingStatement= (selectedNode instanceof ReturnStatement) ? (ReturnStatement) selectedNode : null;
		// Lambda Expression can be in a MethodDeclaration or a Field Declaration
		if (selectedNode instanceof LambdaExpression lambda) {
			MissingReturnTypeInLambdaCorrectionProposalCore core= new MissingReturnTypeInLambdaCorrectionProposalCore(cu, lambda, existingStatement,
					IProposalRelevance.MISSING_RETURN_TYPE);
			T prop= missingReturnTypeInLambdaProposalToT(core, MissingReturnStatementProposal1);
			if (prop != null)
				proposals.add(prop);
		} else {
			BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (decl instanceof MethodDeclaration methodDecl) {
				Block block= methodDecl.getBody();
				if (block == null) {
					return;
				}
				MissingReturnTypeCorrectionProposalCore core= new MissingReturnTypeCorrectionProposalCore(cu, methodDecl, existingStatement, IProposalRelevance.MISSING_RETURN_TYPE);
				T p= missingReturnTypeProposalToT(core, MissingReturnStatementProposal2);
				proposals.add(p);

				Type returnType= methodDecl.getReturnType2();
				if (returnType != null && !"void".equals(ASTNodes.asString(returnType))) { //$NON-NLS-1$
					AST ast= methodDecl.getAST();
					ASTRewrite rewrite= ASTRewrite.create(ast);
					rewrite.replace(returnType, ast.newPrimitiveType(PrimitiveType.VOID), null);
					Javadoc javadoc= methodDecl.getJavadoc();
					if (javadoc != null) {
						TagElement tagElement= JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_RETURN, null);
						if (tagElement != null) {
							rewrite.remove(tagElement, null);
						}
					}

					String label= CorrectionMessages.ReturnTypeSubProcessor_changetovoid_description;
					ASTRewriteCorrectionProposalCore core2= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.CHANGE_RETURN_TYPE_TO_VOID);
					T proposal= rewriteCorrectionProposalToT(core2, MissingReturnStatementProposal3);
					if (proposal != null)
						proposals.add(proposal);
				}
			}
		}
	}
	public void collectReplaceReturnWithYieldStatementProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (!(selectedNode instanceof ReturnStatement returnStatement)) {
			return;
		}
		Expression expression= returnStatement.getExpression();
		if (expression == null) {
			return;
		}
		ASTNode parent= returnStatement.getParent();
		List<Statement> stmts= null;
		if(parent instanceof Block block) {
			stmts= block.statements();
		} else if (parent instanceof SwitchExpression switchExp) {
			stmts= switchExp.statements();
		}
		if (stmts !=  null) {
			int index= stmts.indexOf(returnStatement);
			if (index < 0) {
				return;
			}

			AST ast= astRoot.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);

			YieldStatement yieldStatement= ast.newYieldStatement();
			yieldStatement.setExpression((Expression) rewrite.createMoveTarget(expression));
			rewrite.replace(returnStatement, yieldStatement, null);

			String label= CorrectionMessages.ReturnTypeSubProcessor_changeReturnToYield_description;
			ASTRewriteCorrectionProposalCore core= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.REMOVE_ABSTRACT_MODIFIER);
			T proposal= rewriteCorrectionProposalToT(core, ReplaceReturnWithYieldStatementProposal1);
			if (proposal != null)
				proposals.add(proposal);
		}
	}

	public void collectMethodReturnsVoidProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) throws JavaModelException {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (!(selectedNode instanceof ReturnStatement returnStatement)) {
			return;
		}
		Expression expression= returnStatement.getExpression();
		if (expression == null) {
			return;
		}
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration methDecl) {
			Type retType= methDecl.getReturnType2();
			if (retType == null || retType.resolveBinding() == null) {
				return;
			}
			TypeMismatchBaseSubProcessor<T> sub= getTypeMismatchSubProcessor();
			if (sub != null)
				sub.collectChangeSenderTypeProposals(context, expression, retType.resolveBinding(), false, IProposalRelevance.METHOD_RETURNS_VOID, proposals);
		}
	}

	protected abstract TypeMismatchBaseSubProcessor<T> getTypeMismatchSubProcessor();
	protected abstract T linkedCorrectionProposal1ToT(LinkedCorrectionProposalCore core, int uid);
	protected abstract T rewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core, int uid);
	protected abstract T replaceCorrectionProposalToT(ReplaceCorrectionProposalCore core, int uid);
	protected abstract T missingReturnTypeProposalToT(MissingReturnTypeCorrectionProposalCore core, int uid);
	protected abstract T missingReturnTypeInLambdaProposalToT(MissingReturnTypeInLambdaCorrectionProposalCore core, int uid);
}
