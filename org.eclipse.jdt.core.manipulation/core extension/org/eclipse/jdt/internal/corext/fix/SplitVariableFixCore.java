/*******************************************************************************
 * Copyright (c) 2023 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.DimensionRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public final class SplitVariableFixCore extends CompilationUnitRewriteOperationsFixCore {

	public SplitVariableFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
	}

	public static SplitVariableFixCore createSplitVariableFix(CompilationUnit compilationUnit, ASTNode node) {
		VariableDeclarationFragment fragment;
		if (node instanceof VariableDeclarationFragment) {
			fragment= (VariableDeclarationFragment) node;
		} else if (node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY) {
			fragment= (VariableDeclarationFragment) node.getParent();
		} else {
			return null;
		}
		if (fragment.getInitializer() == null) {
			return null;
		}

		Statement statement;
		ASTNode fragParent= fragment.getParent();
		boolean isVarType= false;
		if (fragParent instanceof VariableDeclarationStatement) {
			statement= (VariableDeclarationStatement) fragParent;
			Type type= ((VariableDeclarationStatement) fragParent).getType();
			isVarType= (type == null) ? false : type.isVar();
		} else if (fragParent instanceof VariableDeclarationExpression) {
			if (fragParent.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
				return null;
			}
			statement= (Statement) fragParent.getParent();
			Type type= ((VariableDeclarationExpression) fragParent).getType();
			isVarType= (type == null) ? false : type.isVar();
		} else {
			return null;
		}
		if (!(statement instanceof ForStatement) &&
				!(statement instanceof VariableDeclarationStatement)) {
			return null;
		}
		// statement is ForStatement or VariableDeclarationStatement
		ASTNode statementParent= statement.getParent();
		StructuralPropertyDescriptor property= statement.getLocationInParent();
		if (!property.isChildListProperty()) {
			return null;
		}
		return new SplitVariableFixCore(CorrectionMessages.QuickAssistProcessor_splitdeclaration_description, compilationUnit,
				new CompilationUnitRewriteOperation[] { new SplitVariableProposalOperation(statement, fragment, fragParent, isVarType, statementParent, property) });
	}

	private static class SplitVariableProposalOperation extends CompilationUnitRewriteOperation {

		private Statement statement;

		private VariableDeclarationFragment fragment;

		private ASTNode fragParent;

		private boolean isVarType;

		private ASTNode statementParent;

		private StructuralPropertyDescriptor property;

		public SplitVariableProposalOperation(Statement statement, VariableDeclarationFragment fragment, ASTNode fragParent,
				boolean isVarType, ASTNode statementParent, StructuralPropertyDescriptor property) {
			this.statement= statement;
			this.fragment= fragment;
			this.fragParent= fragParent;
			this.isVarType= isVarType;
			this.statementParent= statementParent;
			this.property= property;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			final CompilationUnit cup= (CompilationUnit) fragment.getRoot();
			final AST ast= cuRewrite.getAST();

			// for multiple declarations; all must be moved outside, leave none behind
			if (statement instanceof ForStatement) {
				IBuffer buffer= cuRewrite.getCu().getBuffer();
				ForStatement forStatement= (ForStatement) statement;
				VariableDeclarationExpression oldVarDecl= (VariableDeclarationExpression) fragParent;
				Type type= oldVarDecl.getType();
				ITypeBinding tBinding= type.resolveBinding();
				List<VariableDeclarationFragment> oldFragments= oldVarDecl.fragments();
				ListRewrite forListRewrite= rewrite.getListRewrite(forStatement, ForStatement.INITIALIZERS_PROPERTY);
				// create the new initializers
				for (VariableDeclarationFragment oldFragment : oldFragments) {
					int extendedStartPositionFragment= cup.getExtendedStartPosition(oldFragment);
					int extendedLengthFragment= cup.getExtendedLength(oldFragment);
					StringBuilder codeFragment= new StringBuilder(
							buffer.getText(extendedStartPositionFragment, extendedLengthFragment));
					if (oldFragment.getInitializer() == null) {
						ITypeBinding typeBinding= type.resolveBinding();
						if ("Z".equals(typeBinding.getBinaryName())) { //$NON-NLS-1$
							codeFragment.append(" = false"); //$NON-NLS-1$
						} else if (type.isPrimitiveType()) {
							codeFragment.append(" = 0"); //$NON-NLS-1$
						} else {
							codeFragment.append(" = null"); //$NON-NLS-1$
						}
					}
					Assignment newAssignmentFragment= (Assignment) rewrite.createStringPlaceholder(codeFragment.toString(),
							ASTNode.ASSIGNMENT);
					forListRewrite.insertLast(newAssignmentFragment, null);
				}

				// create the new declarations
				Type nType= null;
				if (isVarType) {
					ImportRewrite importRewrite= cuRewrite.getImportRewrite();
					ImportRewriteContext icontext= new ContextSensitiveImportRewriteContext(cup, importRewrite);
					nType= importRewrite.addImport(tBinding, ast, icontext, TypeLocation.LOCAL_VARIABLE);
					String codeDeclaration= tBinding.getName();
					String commentToken= ""; //$NON-NLS-1$
					int extendedStatementStart= cup.getExtendedStartPosition(oldVarDecl);
					if (oldVarDecl.getStartPosition() > extendedStatementStart) {
						commentToken= buffer.getText(extendedStatementStart,
								oldVarDecl.getStartPosition() - extendedStatementStart);
					}
					codeDeclaration= commentToken + codeDeclaration;
					nType= (Type) rewrite.createStringPlaceholder(codeDeclaration.trim(), type.getNodeType());
				} else {
					int extendedStartPositionDeclaration= cup.getExtendedStartPosition(oldVarDecl);
					int firstFragmentStart= ((ASTNode) oldVarDecl.fragments().get(0)).getStartPosition();
					String codeDeclaration= buffer.getText(extendedStartPositionDeclaration,
							firstFragmentStart - extendedStartPositionDeclaration);
					nType= (Type) rewrite.createStringPlaceholder(codeDeclaration.trim(), type.getNodeType());
				}

				VariableDeclarationFragment newFrag= ast.newVariableDeclarationFragment();
				VariableDeclarationStatement newVarDec= ast.newVariableDeclarationStatement(newFrag);
				newVarDec.setType(nType);
				newFrag.setName(ast.newSimpleName(oldFragments.get(0).getName().getIdentifier()));
				newFrag.extraDimensions()
						.addAll(DimensionRewrite.copyDimensions(oldFragments.get(0).extraDimensions(), rewrite));

				for (int i= 1; i < oldFragments.size(); i++) {
					VariableDeclarationFragment oldFragment= oldFragments.get(i);
					newFrag= ast.newVariableDeclarationFragment();
					newFrag.setName(ast.newSimpleName(oldFragment.getName().getIdentifier()));
					newFrag.extraDimensions()
							.addAll(DimensionRewrite.copyDimensions(oldFragment.extraDimensions(), rewrite));
					newVarDec.fragments().add(newFrag);
				}
				newVarDec.modifiers().addAll(ASTNodeFactory.newModifiers(ast, oldVarDecl.getModifiers()));

				ListRewrite listRewriter= rewrite.getListRewrite(statementParent, (ChildListPropertyDescriptor) property);
				listRewriter.insertBefore(newVarDec, statement, null);

				rewrite.remove(oldVarDecl, null);
			} else {
				List<? extends ASTNode> list= ASTNodes.getChildListProperty(statementParent,
						(ChildListPropertyDescriptor) property);
				int insertIndex= list.indexOf(statement);
				ITypeBinding binding= fragment.getInitializer().resolveTypeBinding();
				Expression placeholder= (Expression) rewrite.createMoveTarget(fragment.getInitializer());
				if (placeholder instanceof ArrayInitializer && binding != null && binding.isArray()) {
					ArrayCreation creation= ast.newArrayCreation();
					creation.setInitializer((ArrayInitializer) placeholder);
					final ITypeBinding componentType= binding.getElementType();
					Type type= null;
					if (componentType.isPrimitive()) {
						type= ast.newPrimitiveType(PrimitiveType.toCode(componentType.getName()));
					} else {
						type= ast.newSimpleType(ast.newSimpleName(componentType.getName()));
					}
					creation.setType(ast.newArrayType(type, binding.getDimensions()));
					placeholder= creation;
				}

				Assignment assignment= ast.newAssignment();
				assignment.setRightHandSide(placeholder);
				assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().getIdentifier()));

				// statement is VariableDeclarationStatement
				Statement newStatement= ast.newExpressionStatement(assignment);
				insertIndex+= 1; // add after declaration

				if (isVarType) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) statement;
					Type type= varDecl.getType();
					ITypeBinding tBinding= type.resolveBinding();
					ImportRewrite importRewrite= cuRewrite.getImportRewrite();
					ImportRewriteContext icontext= new ContextSensitiveImportRewriteContext(cup, importRewrite);
					Type nType= importRewrite.addImport(tBinding, ast, icontext, TypeLocation.LOCAL_VARIABLE);
					rewrite.set(varDecl, VariableDeclarationStatement.TYPE_PROPERTY, nType, null);
					DimensionRewrite.removeAllChildren(fragment, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY,
							rewrite, null);
				}

				ListRewrite listRewriter= rewrite.getListRewrite(statementParent, (ChildListPropertyDescriptor) property);
				listRewriter.insertAt(newStatement, insertIndex, null);
			}
		}
	}

}
