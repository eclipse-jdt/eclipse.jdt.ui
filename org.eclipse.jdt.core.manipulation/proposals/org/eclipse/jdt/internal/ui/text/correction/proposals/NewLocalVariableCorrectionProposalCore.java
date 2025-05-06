/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

public class NewLocalVariableCorrectionProposalCore extends LinkedCorrectionProposalCore {
	private ASTNode fSelectedNode;
	private ICompilationUnit fCu;

	public NewLocalVariableCorrectionProposalCore(String label, ICompilationUnit cu, ASTNode selectedNode, int relevance) {
		super(label, cu, null, relevance);
		fSelectedNode= selectedNode;
		fCu= cu;
	}

	public boolean hasProposal() {
		Expression exp= null;
		if (fSelectedNode instanceof ExpressionStatement expStmt) {
			exp= expStmt.getExpression();
		} else if (fSelectedNode instanceof Expression exp1 && exp1.getLocationInParent() == ExpressionStatement.EXPRESSION_PROPERTY) {
			exp= (Expression)fSelectedNode;
		}
		exp= ASTNodes.getUnparenthesedExpression(exp);
		if (exp != null) {
			ITypeBinding typeBinding= exp.resolveTypeBinding();
			if (typeBinding == null) {
				if (exp instanceof CastExpression) {
					return true;
				}
			} else {
				return true;
			}
		}

		return false;
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		ExpressionStatement expStmt= null;
		Expression exp= null;
		if (fSelectedNode instanceof ExpressionStatement) {
			expStmt= (ExpressionStatement)fSelectedNode;
			exp= ASTNodes.getUnparenthesedExpression(expStmt.getExpression());
		} else if (fSelectedNode instanceof Expression && fSelectedNode.getLocationInParent() == ExpressionStatement.EXPRESSION_PROPERTY) {
			exp= ASTNodes.getUnparenthesedExpression((Expression)fSelectedNode);
			expStmt= (ExpressionStatement)fSelectedNode.getParent();
		}
		if (expStmt != null && exp != null) {
			AST ast= fSelectedNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			CompilationUnit cu= (CompilationUnit) fSelectedNode.getRoot();
			ImportRewrite importRewrite= createImportRewrite(cu);
			ITypeBinding typeBinding= exp.resolveTypeBinding();
			Type type= null;
			String typeName= null;
			if (typeBinding == null) {
				if (exp instanceof CastExpression castExp) {
					type= (Type) rewrite.createCopyTarget(castExp.getType());
					typeName= castExp.getType().toString();
				}
			} else {
				type= importRewrite.addImport(typeBinding, ast);
				typeName= typeBinding.getName();
			}
			if (type != null) {
				VariableDeclarationFragment vdf= ast.newVariableDeclarationFragment();
				AbstractTypeDeclaration typeDecl= ASTNodes.getFirstAncestorOrNull(expStmt, AbstractTypeDeclaration.class);
				IType declaringType= getType(fCu, typeDecl.getName().getFullyQualifiedName());
				String names[]= StubUtility.getVariableNameSuggestions(NamingConventions.VK_PARAMETER, declaringType.getJavaProject(), typeName, 0, Arrays.asList(computeReservedIdentifiers(expStmt, cu)), true);
				vdf.setName(ast.newSimpleName(names[0]));
				vdf.setInitializer((Expression) rewrite.createCopyTarget(exp));
				VariableDeclarationStatement vdStmt= ast.newVariableDeclarationStatement(vdf);
				vdStmt.setType(type);
				rewrite.replace(expStmt, vdStmt, null);
				return rewrite;
			}
		}
		return null;
	}

	protected IType getType(ICompilationUnit cu, String name) throws JavaModelException {
		for (IType type : cu.getAllTypes()) {
			if (type.getTypeQualifiedName('.').equals(name) || type.getElementName().equals(name)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Returns the reserved identifiers in the method to avoid.
	 *
	 * @param node the node that must avoid reserved identifiers
	 * @return the reserved identifiers
	 * @throws JavaModelException
	 *             if the method declaration could not be found
	 */
	protected String[] computeReservedIdentifiers(ASTNode node, CompilationUnit cu) throws JavaModelException {
		final List<String> names= new ArrayList<>();
		final MethodDeclaration declaration= ASTNodes.getFirstAncestorOrNull(node, MethodDeclaration.class);
		if (declaration != null) {
			final List<SingleVariableDeclaration> parameters= declaration.parameters();
			VariableDeclaration variable= null;
			for (SingleVariableDeclaration parameter : parameters) {
				variable= parameter;
				names.add(variable.getName().getIdentifier());
			}
			final Block body= declaration.getBody();
			if (body != null) {
				for (IBinding binding : new ScopeAnalyzer(cu).getDeclarationsAfter(body.getStartPosition(), ScopeAnalyzer.VARIABLES))
					names.add(binding.getName());
			}
		}
		final String[] result= new String[names.size()];
		names.toArray(result);
		return result;
	}

}