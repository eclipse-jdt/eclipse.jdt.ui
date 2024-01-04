/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.SuperMethodReference;

final class MethodReferenceFinder extends ASTVisitor {

	private final IMethodBinding fTargetMethodBinding;

	private FinalConditionsChecker fFinalConditionsChecker;

	/**
	 * Creates a new MethodReferenceFinder with the specified target method binding and final
	 * conditions checker. This class is used to find method references in a given context, using
	 * the provided target method binding and conditions checker.
	 *
	 * @param targetMethodBinding The IMethodBinding representing the target method to find
	 *            references for. It must not be null and should correspond to the method whose
	 *            references are being searched.
	 * @param finalConditionsChecker The FinalConditionsChecker instance used to check final
	 *            conditions during the reference search. It must not be null and provides necessary
	 *            checks to determine valid references to the target method.
	 */
	public MethodReferenceFinder(IMethodBinding targetMethodBinding, FinalConditionsChecker finalConditionsChecker) {
		fFinalConditionsChecker= finalConditionsChecker;
		fTargetMethodBinding= targetMethodBinding;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Visits an ExpressionMethodReference node and checks if it refers to the selected method. The
	 * final conditions checker is used to perform this check, ensuring that the method reference
	 * does not refer to the same method as the target method being searched for.
	 *
	 * @param node The ExpressionMethodReference node being visited. It represents a method
	 *            reference expression in the abstract syntax tree (AST). It must not be null.
	 * @return {@code true} to continue visiting the children of this node, {@code false} otherwise.
	 */
	@Override
	public boolean visit(ExpressionMethodReference node) {
		// Check if the method reference refers to the selected method
		fFinalConditionsChecker.checkMethodReferenceNotReferingToMethod(node, fTargetMethodBinding);
		return super.visit(node);
	}


	/**
	 * {@inheritDoc}
	 * <p>
	 * Visits an SuperMethodReference node and checks if it refers to the selected method. The final
	 * conditions checker is used to perform this check, ensuring that the method reference does not
	 * refer to the same method as the target method being searched for.
	 *
	 * @param node The SuperMethodReference node being visited. It represents a method reference
	 *            expression in the abstract syntax tree (AST). It must not be null.
	 * @return {@code true} to continue visiting the children of this node, {@code false} otherwise.
	 */
	@Override
	public boolean visit(SuperMethodReference node) {
		// Check if the method reference refers to the selected method
		fFinalConditionsChecker.checkMethodReferenceNotReferingToMethod(node, fTargetMethodBinding);
		return super.visit(node);
	}
}
