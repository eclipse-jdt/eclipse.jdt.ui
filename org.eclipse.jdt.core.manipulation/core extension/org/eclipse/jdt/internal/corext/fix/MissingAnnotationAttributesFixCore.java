/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class MissingAnnotationAttributesFixCore extends CompilationUnitRewriteOperationsFixCore {

	public MissingAnnotationAttributesFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	/**
	 * Returns a fix that adds the missing required attributes to the annotation at the problem
	 * location, or null if the given problem location isn't an annotation.
	 *
	 * @param cu the AST of the compilation unit where an attribute is missing required attributes
	 * @param problemLocation the error marker that marks the annotation that's missing attributes
	 * @return a fix that adds the missing required attributes to the annotation at the problem
	 *         location, or null if the given problem location isn't an annotation
	 */
	public static MissingAnnotationAttributesFixCore addMissingAnnotationAttributesProposal(CompilationUnit cu, IProblemLocation problemLocation) {
		ASTNode selectedNode= problemLocation.getCoveringNode(cu);
		if (!(selectedNode instanceof Annotation)) {
			return null;
		}
		Annotation annotation= (Annotation) selectedNode;
		MissingAnnotationAttributesProposalOperation rewriteProposal= new MissingAnnotationAttributesProposalOperation(annotation);

		return new MissingAnnotationAttributesFixCore(CorrectionMessages.MissingAnnotationAttributesProposal_add_missing_attributes_label, cu, rewriteProposal);
	}

}
