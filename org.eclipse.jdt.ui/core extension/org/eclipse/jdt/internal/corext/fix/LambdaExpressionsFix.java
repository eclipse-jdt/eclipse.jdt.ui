/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
 *     Jerome Cambon <jerome.cambon@oracle.com> - [1.8][clean up][quick assist] Convert lambda to anonymous must qualify references to 'this'/'super' - https://bugs.eclipse.org/430573
 *     Stephan Herrmann - Contribution for Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *     Red Hat Inc. - modified to use LambdaExpressionsFixCore static classes
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LambdaExpression;

import org.eclipse.jdt.internal.corext.fix.LambdaExpressionsFixCore.FunctionalAnonymousClassesFinder;
import org.eclipse.jdt.internal.corext.fix.LambdaExpressionsFixCore.LambdaExpressionsFinder;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class LambdaExpressionsFix extends CompilationUnitRewriteOperationsFix {

	private static boolean fConversionRemovesAnnotations;

	public static LambdaExpressionsFix createConvertToLambdaFix(ClassInstanceCreation cic) {
		CompilationUnit root= (CompilationUnit) cic.getRoot();
		if (!JavaModelUtil.is1d8OrHigher(root.getJavaElement().getJavaProject()))
			return null;

		if (!LambdaExpressionsFixCore.isFunctionalAnonymous(cic))
			return null;

		LambdaExpressionsFixCore.CreateLambdaOperation op= new LambdaExpressionsFixCore.CreateLambdaOperation(Collections.singletonList(cic));
		String message;
		if (fConversionRemovesAnnotations) {
			message= FixMessages.LambdaExpressionsFix_convert_to_lambda_expression_removes_annotations;
		} else {
			message= FixMessages.LambdaExpressionsFix_convert_to_lambda_expression;
		}
		return new LambdaExpressionsFix(message, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });
	}

	public static IProposableFix createConvertToAnonymousClassCreationsFix(LambdaExpression lambda) {
		// offer the quick assist at pre 1.8 levels as well to get rid of the compilation error (TODO: offer this as a quick fix in that case)

		if (lambda.resolveTypeBinding() == null || lambda.resolveTypeBinding().getFunctionalInterfaceMethod() == null)
			return null;

		LambdaExpressionsFixCore.CreateAnonymousClassCreationOperation op= new LambdaExpressionsFixCore.CreateAnonymousClassCreationOperation(Collections.singletonList(lambda));
		CompilationUnit root= (CompilationUnit) lambda.getRoot();
		return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean useLambda, boolean useAnonymous) {
		if (!JavaModelUtil.is1d8OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		if (useLambda) {
			ArrayList<ClassInstanceCreation> convertibleNodes= FunctionalAnonymousClassesFinder.perform(compilationUnit);
			if (convertibleNodes.isEmpty())
				return null;

			Collections.reverse(convertibleNodes); // process nested anonymous classes first
			CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation op= new LambdaExpressionsFixCore.CreateLambdaOperation(convertibleNodes);
			return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });

		} else if (useAnonymous) {
			ArrayList<LambdaExpression> convertibleNodes= LambdaExpressionsFinder.perform(compilationUnit);
			if (convertibleNodes.isEmpty())
				return null;

			Collections.reverse(convertibleNodes); // process nested lambdas first
			CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation op= new LambdaExpressionsFixCore.CreateAnonymousClassCreationOperation(convertibleNodes);
			return new LambdaExpressionsFix(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });

		}
		return null;
	}

	protected LambdaExpressionsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
