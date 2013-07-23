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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class LambdaExpressionsFix extends CompilationUnitRewriteOperationsFix {

	private static final class AnonymousClassCreationVisitor extends ASTVisitor {

		private final ArrayList<ClassInstanceCreation> fNodes;

		private AnonymousClassCreationVisitor(ArrayList<ClassInstanceCreation> nodes) {
			fNodes= nodes;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			final AnonymousClassDeclaration anonymTypeDecl= node.getAnonymousClassDeclaration();
			if (anonymTypeDecl == null || anonymTypeDecl.resolveBinding() == null) {
				return true;
			}

			// check for functional interface
			//TODO: need an API from JDT Core for org.eclipse.jdt.internal.compiler.lookup.TypeBinding.isFunctionalInterface()
			//for now do this simple but insufficient check
			List<BodyDeclaration> bodyDeclarations= anonymTypeDecl.bodyDeclarations();
			int size= bodyDeclarations.size();
			if (size != 1) //cannot convert if there is are fields or additional methods from Object class
				return true;

			fNodes.add(node);

			return true;
		}
	}

	private static class CreateLambdaOperation extends CompilationUnitRewriteOperation {

		private final HashSet<ClassInstanceCreation> fExpressions;

		public CreateLambdaOperation(HashSet<ClassInstanceCreation> expressions) {
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
				lambdaExpression.setBody(rewrite.createCopyTarget(methodDeclaration.getBody()));

				rewrite.replace(classInstanceCreation, lambdaExpression, group);

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

		HashSet<ClassInstanceCreation> expressions= new HashSet<ClassInstanceCreation>(changedNodes);
		CreateLambdaOperation op= new CreateLambdaOperation(expressions);
		return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, compilationUnit, new CompilationUnitRewriteOperation[] { op });
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean useLambda) {
		if (!JavaModelUtil.is18OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		if (useLambda) {
			final ArrayList<ClassInstanceCreation> changedNodes= new ArrayList<ClassInstanceCreation>();
			compilationUnit.accept(new AnonymousClassCreationVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			HashSet<ClassInstanceCreation> expressions= new HashSet<ClassInstanceCreation>(changedNodes);
			CompilationUnitRewriteOperation op= new CreateLambdaOperation(expressions);
			return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, compilationUnit, new CompilationUnitRewriteOperation[] { op });
		}
		return null;
	}

	protected LambdaExpressionsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
