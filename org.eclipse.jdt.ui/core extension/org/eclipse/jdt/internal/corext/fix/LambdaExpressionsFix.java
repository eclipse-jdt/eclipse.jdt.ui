/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class LambdaExpressionsFix extends CompilationUnitRewriteOperationsFix {

	private static final class AnonymousClassCreationVisitor extends ASTVisitor {

		private final ArrayList<ClassInstanceCreation> fNodes;

		private AnonymousClassCreationVisitor(ArrayList<ClassInstanceCreation> nodes) {
			fNodes= nodes;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			ITypeBinding typeBinding= node.resolveTypeBinding();
			if (typeBinding == null)
				return true;
			ITypeBinding[] interfaces= typeBinding.getInterfaces();
			if (interfaces.length != 1)
				return true;
			if (!interfaces[0].isFunctionalInterface())
				return true;

			final AnonymousClassDeclaration anonymTypeDecl= node.getAnonymousClassDeclaration();
			if (anonymTypeDecl == null || anonymTypeDecl.resolveBinding() == null) {
				return true;
			}
			List<BodyDeclaration> bodyDeclarations= anonymTypeDecl.bodyDeclarations();
			int size= bodyDeclarations.size();
			if (size != 1) //cannot convert if there is are fields or additional methods from Object class
				return true;

			fNodes.add(node);

			return true;
		}
	}

	private static final class LambdaExpressionVisitor extends ASTVisitor {

		private final ArrayList<LambdaExpression> fNodes;

		private LambdaExpressionVisitor(ArrayList<LambdaExpression> nodes) {
			fNodes= nodes;
		}

		@Override
		public boolean visit(LambdaExpression node) {
			ITypeBinding typeBinding= node.resolveTypeBinding();
			if (typeBinding != null) {
				fNodes.add(node);
			}
			return true;
		}
	}

	private static class CreateLambdaOperation extends CompilationUnitRewriteOperation {

		private final ArrayList<ClassInstanceCreation> fExpressions;

		public CreateLambdaOperation(ArrayList<ClassInstanceCreation> expressions) {
			fExpressions= expressions;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			for (Iterator<ClassInstanceCreation> iterator= fExpressions.iterator(); iterator.hasNext();) {
				ClassInstanceCreation classInstanceCreation= iterator.next();

				AST ast= classInstanceCreation.getAST();

				AnonymousClassDeclaration anonymTypeDecl= classInstanceCreation.getAnonymousClassDeclaration();
				List<BodyDeclaration> bodyDeclarations= anonymTypeDecl.bodyDeclarations();

				Object object= bodyDeclarations.get(0);
				if (!(object instanceof MethodDeclaration))
					continue;
				MethodDeclaration methodDeclaration= (MethodDeclaration) object;

				LambdaExpression lambdaExpression= ast.newLambdaExpression();
				lambdaExpression.setParentheses(true); // TODO: minor: no parentheses for single inferred-type parameter?
				List<SingleVariableDeclaration> parameters= lambdaExpression.parameters(); // TODO: minor: do we want to create VaribaleDeclarationFragments or inferred-type parameter - never?
				List<SingleVariableDeclaration> methodParameters= methodDeclaration.parameters();

				for (Iterator<SingleVariableDeclaration> iterator1= methodParameters.iterator(); iterator1.hasNext();) {
					SingleVariableDeclaration singleVariableDeclaration= iterator1.next();
					parameters.add((SingleVariableDeclaration) rewrite.createCopyTarget(singleVariableDeclaration));
				}
				Block body= methodDeclaration.getBody();
				List<Statement> statements= body.statements();
				ASTNode lambdaBody;
				if (statements.size() == 1) {
					//lambda should use short form with just an expression body if possible
					Statement statement= statements.get(0);
					if (statement instanceof ExpressionStatement) {
						lambdaBody= ((ExpressionStatement) statement).getExpression();
					} else if (statement instanceof ReturnStatement) {
						lambdaBody= ((ReturnStatement) statement).getExpression();
					} else {
						lambdaBody= body;
					}
				} else {
					lambdaBody= body;
				}
				lambdaExpression.setBody(rewrite.createCopyTarget(lambdaBody));
				rewrite.replace(classInstanceCreation, lambdaExpression, group);

			}
		}
	}

	private static class CreateAnonymousClassCreationOperation extends CompilationUnitRewriteOperation {

		private final ArrayList<LambdaExpression> fExpressions;

		public CreateAnonymousClassCreationOperation(ArrayList<LambdaExpression> changedNodes) {
			fExpressions= changedNodes;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			for (Iterator<LambdaExpression> iterator= fExpressions.iterator(); iterator.hasNext();) {
				LambdaExpression lambdaExpression= iterator.next();

				AST ast= lambdaExpression.getAST();

				ITypeBinding lambdaTypeBinding= lambdaExpression.resolveTypeBinding();
				IMethodBinding methodBinding= lambdaTypeBinding.getDeclaredMethods()[0];

				final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cuRewrite.getCu().getJavaProject());
				ImportRewrite importRewrite= cuRewrite.getImportRewrite();
				ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(cuRewrite.getRoot(), importRewrite);
				MethodDeclaration methodDeclaration= StubUtility2.createImplementationStub(cuRewrite.getCu(), rewrite, importRewrite, importContext,
						methodBinding, lambdaTypeBinding.getName(), settings, false);

				Block block;
				ASTNode lambdaBody= lambdaExpression.getBody();
				if (lambdaBody instanceof Block) {
					block= (Block) rewrite.createCopyTarget(lambdaBody);
				} else {
					block= ast.newBlock();
					List<Statement> statements= block.statements();
					ITypeBinding returnType= methodBinding.getReturnType();
					Expression copyTarget= (Expression) rewrite.createCopyTarget(lambdaBody);
					if (Bindings.isVoidType(returnType)) {
						ExpressionStatement newExpressionStatement= ast.newExpressionStatement(copyTarget);
						statements.add(newExpressionStatement);
					} else {
						ReturnStatement returnStatement= ast.newReturnStatement();
						returnStatement.setExpression(copyTarget);
						statements.add(returnStatement);
					}
				}
				methodDeclaration.setBody(block);

				AnonymousClassDeclaration anonymousClassDeclaration= ast.newAnonymousClassDeclaration();
				List<BodyDeclaration> bodyDeclarations= anonymousClassDeclaration.bodyDeclarations();
				bodyDeclarations.add(methodDeclaration);

				ClassInstanceCreation classInstanceCreation= ast.newClassInstanceCreation();
				classInstanceCreation.setType(ast.newSimpleType(ast.newName(lambdaTypeBinding.getName())));
				classInstanceCreation.setAnonymousClassDeclaration(anonymousClassDeclaration);

				rewrite.replace(lambdaExpression, classInstanceCreation, group);
			}
		}
	}

	public static LambdaExpressionsFix createConvertToLambdaFix(CompilationUnit compilationUnit, ASTNode[] nodes) {
		if (!JavaModelUtil.is18OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		final ArrayList<ClassInstanceCreation> changedNodes= new ArrayList<ClassInstanceCreation>();
		for (int i= 0; i < nodes.length; i++) {
			nodes[i].accept(new AnonymousClassCreationVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;

		CreateLambdaOperation op= new CreateLambdaOperation(changedNodes);
		return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, compilationUnit, new CompilationUnitRewriteOperation[] { op });
	}

	public static IProposableFix createConvertToAnonymousClassCreationsFix(CompilationUnit compilationUnit, ASTNode[] nodes) {
		// offer the quick assist at pre 1.8 levels as well to get rid of the compilation error (TODO: offer this as a quick fix in that case)

		final ArrayList<LambdaExpression> changedNodes= new ArrayList<LambdaExpression>();
		for (int i= 0; i < nodes.length; i++) {
			nodes[i].accept(new LambdaExpressionVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;

		CreateAnonymousClassCreationOperation op= new CreateAnonymousClassCreationOperation(changedNodes);
		return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, compilationUnit, new CompilationUnitRewriteOperation[] { op });
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean useLambda) {
		if (!JavaModelUtil.is18OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		if (useLambda) {
			final ArrayList<ClassInstanceCreation> changedNodes= new ArrayList<ClassInstanceCreation>();
			compilationUnit.accept(new AnonymousClassCreationVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			CompilationUnitRewriteOperation op= new CreateLambdaOperation(changedNodes);
			return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, compilationUnit, new CompilationUnitRewriteOperation[] { op });
		}
		return null;
	}

	protected LambdaExpressionsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
