/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 * Red Hat Inc. - copied and modified for use jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocationCore;

public class TypeParametersFixCore extends CompilationUnitRewriteOperationsFixCore {

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

	public static class InsertTypeArgumentsOperation extends CompilationUnitRewriteOperation {

		private final ParameterizedType[] fCreatedTypes;

		public InsertTypeArgumentsOperation(ParameterizedType[] parameterizedTypes) {
			fCreatedTypes= parameterizedTypes;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.TypeParametersFix_insert_inferred_type_arguments_description, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			for (ParameterizedType createdType : fCreatedTypes) {
				ITypeBinding[] typeArguments= createdType.resolveBinding().getTypeArguments();
				ContextSensitiveImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(cuRewrite.getRoot(), createdType.getStartPosition(), importRewrite);

				ListRewrite argumentsRewrite= rewrite.getListRewrite(createdType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
				for (ITypeBinding typeArgument : typeArguments) {
					Type argumentNode= importRewrite.addImport(typeArgument, ast, importContext, TypeLocation.TYPE_ARGUMENT);
					argumentsRewrite.insertLast(argumentNode, group);
				}
			}
		}
	}

	public static class RemoveTypeArgumentsOperation extends CompilationUnitRewriteOperation {

		private final ParameterizedType fParameterizedType;

		public RemoveTypeArgumentsOperation(ParameterizedType parameterizedType) {
			fParameterizedType= parameterizedType;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.TypeParametersFix_remove_redundant_type_arguments_description, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			rewrite.setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());

			ListRewrite listRewrite= rewrite.getListRewrite(fParameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);

			List<Type> typeArguments= fParameterizedType.typeArguments();
			for (Type typeArgument : typeArguments) {
				listRewrite.remove(typeArgument, group);
			}
		}
	}

	public static TypeParametersFixCore createInsertInferredTypeArgumentsFix(CompilationUnit compilationUnit, ParameterizedType node) {
		if (node == null)
			return null;

		final ArrayList<ASTNode> changedNodes= new ArrayList<>();
		node.accept(new InsertTypeArgumentsVisitor(changedNodes));

		if (changedNodes.isEmpty())
			return null;

		CompilationUnitRewriteOperation op= new InsertTypeArgumentsOperation(new ParameterizedType[] { node });
		return new TypeParametersFixCore(FixMessages.TypeParametersFix_insert_inferred_type_arguments_name, compilationUnit, new CompilationUnitRewriteOperation[] { op });
	}

	public static TypeParametersFixCore createRemoveRedundantTypeArgumentsFix(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		int id= problem.getProblemId();
		if (id == IProblem.RedundantSpecificationOfTypeArguments) {
			ParameterizedType parameterizedType= getParameterizedType(compilationUnit, problem);
			if (parameterizedType == null)
				return null;
			RemoveTypeArgumentsOperation operation= new RemoveTypeArgumentsOperation(parameterizedType);
			return new TypeParametersFixCore(FixMessages.TypeParametersFix_remove_redundant_type_arguments_name, compilationUnit, new CompilationUnitRewriteOperation[] { operation });
		}
		return null;
	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit, boolean insertInferredTypeArguments, boolean removeRedundantTypeArguments) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocationCore[] locations= new IProblemLocationCore[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocationCore(problems[i]);
		}

		return createCleanUp(compilationUnit, locations,
				insertInferredTypeArguments,
				removeRedundantTypeArguments);
	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit, IProblemLocationCore[] problems, boolean insertInferredTypeArguments, boolean removeRedundantTypeArguments) {

		if (insertInferredTypeArguments) {
			final ArrayList<ASTNode> changedNodes= new ArrayList<>();
			compilationUnit.accept(new InsertTypeArgumentsVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			CompilationUnitRewriteOperation op= new InsertTypeArgumentsOperation(changedNodes.toArray(new ParameterizedType[changedNodes.size()]));
			return new TypeParametersFixCore(FixMessages.TypeParametersFix_insert_inferred_type_arguments_name, compilationUnit, new CompilationUnitRewriteOperation[] { op });

		} else if (removeRedundantTypeArguments) {
			List<CompilationUnitRewriteOperation> result= new ArrayList<>();
			for (IProblemLocationCore problem : problems) {
				int id= problem.getProblemId();

				if (id == IProblem.RedundantSpecificationOfTypeArguments) {
					ParameterizedType parameterizedType= getParameterizedType(compilationUnit, problem);
					if (parameterizedType == null)
						return null;
					result.add(new RemoveTypeArgumentsOperation(parameterizedType));
				}
			}
			if (!result.isEmpty()) {
				return new TypeParametersFixCore(FixMessages.TypeParametersFix_remove_redundant_type_arguments_name, compilationUnit, result.toArray(new CompilationUnitRewriteOperation[result.size()]));
			}
		}
		return null;
	}

	public static ParameterizedType getParameterizedType(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;

		while (!(selectedNode instanceof ParameterizedType) && !(selectedNode instanceof Statement)) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode instanceof ParameterizedType) {
			return (ParameterizedType) selectedNode;
		}
		return null;
	}

	protected TypeParametersFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
