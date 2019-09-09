/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * Red Hat Inc. - modified to use TypeParametersFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public class TypeParametersFix extends CompilationUnitRewriteOperationsFix {

	public static final class InsertTypeArgumentsVisitor extends ASTVisitor {

		private final ArrayList<ASTNode> fNodes;

		public InsertTypeArgumentsVisitor(ArrayList<ASTNode> nodes) {
			fNodes= nodes;
		}

		@Override
		public boolean visit(ParameterizedType createdType) {
			if (createdType == null || createdType.typeArguments().size() != 0) {
				return true;
			}

			ITypeBinding binding= createdType.resolveBinding();
			if (binding == null) {
				return true;
			}

			ITypeBinding[] typeArguments= binding.getTypeArguments();
			if (typeArguments.length == 0) {
				return true;
			}

			fNodes.add(createdType);
			return true;
		}
	}

	public static TypeParametersFix createInsertInferredTypeArgumentsFix(CompilationUnit compilationUnit, ParameterizedType node) {
		if (node == null)
			return null;

		final ArrayList<ASTNode> changedNodes= new ArrayList<>();
		node.accept(new InsertTypeArgumentsVisitor(changedNodes));

		if (changedNodes.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation op= new TypeParametersFixCore.InsertTypeArgumentsOperation(new ParameterizedType[] { node });
		return new TypeParametersFix(FixMessages.TypeParametersFix_insert_inferred_type_arguments_name, compilationUnit,
				new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });
	}

	public static TypeParametersFix createRemoveRedundantTypeArgumentsFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.RedundantSpecificationOfTypeArguments) {
			IProblemLocationCore problemLocation= (ProblemLocation)problem;
			ParameterizedType parameterizedType= TypeParametersFixCore.getParameterizedType(compilationUnit, problemLocation);
			if (parameterizedType == null)
				return null;
			TypeParametersFixCore.RemoveTypeArgumentsOperation operation= new TypeParametersFixCore.RemoveTypeArgumentsOperation(parameterizedType);
			return new TypeParametersFix(FixMessages.TypeParametersFix_remove_redundant_type_arguments_name, compilationUnit,
					new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operation });
		}
		return null;
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean insertInferredTypeArguments, boolean removeRedundantTypeArguments) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}

		return createCleanUp(compilationUnit, locations,
				insertInferredTypeArguments,
				removeRedundantTypeArguments);
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, boolean insertInferredTypeArguments, boolean removeRedundantTypeArguments) {

		if (insertInferredTypeArguments) {
			final ArrayList<ASTNode> changedNodes= new ArrayList<>();
			compilationUnit.accept(new InsertTypeArgumentsVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation op=
					new TypeParametersFixCore.InsertTypeArgumentsOperation(changedNodes.toArray(new ParameterizedType[changedNodes.size()]));
			return new TypeParametersFix(FixMessages.TypeParametersFix_insert_inferred_type_arguments_name, compilationUnit,
					new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });

		} else if (removeRedundantTypeArguments) {
			List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> result= new ArrayList<>();
			for (IProblemLocation location : problems) {
				IProblemLocationCore problem= (ProblemLocation)location;
				int id= problem.getProblemId();

				if (id == IProblem.RedundantSpecificationOfTypeArguments) {
					ParameterizedType parameterizedType= TypeParametersFixCore.getParameterizedType(compilationUnit, problem);
					if (parameterizedType == null)
						return null;
					result.add(new TypeParametersFixCore.RemoveTypeArgumentsOperation(parameterizedType));
				}
			}
			if (!result.isEmpty()) {
				return new TypeParametersFix(FixMessages.TypeParametersFix_remove_redundant_type_arguments_name, compilationUnit,
						result.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[result.size()]));
			}
		}
		return null;
	}

	protected TypeParametersFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
