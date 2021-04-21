/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * <pre>
 * - propose local instance
 * - propose field instance
 * </pre>
 */
public class CreateObjectReferenceProposal extends LinkedCorrectionProposal {

	private ITypeBinding fTypeNode;

	private ASTNode fSelectedNode;

	private String varName= null;

	private String varClass= null;

	public CreateObjectReferenceProposal(ICompilationUnit cu, ASTNode selectedNode, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fSelectedNode= Objects.requireNonNull(selectedNode);
		fTypeNode= Objects.requireNonNull(typeNode);
	}

	public boolean hasProposal() {
		AST ast= fSelectedNode.getAST();

		ImportRewrite importRewrite= createImportRewrite((CompilationUnit) fSelectedNode.getRoot());
		Type newSimpleType= importRewrite.addImport(fTypeNode, ast);
		IVariableBinding variabelReference= findReference(fSelectedNode, newSimpleType);

		if (variabelReference != null) {
			varName= variabelReference.getName();
			varClass= variabelReference.getType().getName();
			return true;
		}
		return false;
	}

	@Override
	public Image getImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(
				new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
	}

	@Override
	public String getName() {
		return Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createnew_reference_to_instance, new Object[] { varName, varClass });
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fSelectedNode.getAST();
		ImportRewrite importRewrite= createImportRewrite((CompilationUnit) fSelectedNode.getRoot());
		Type newSimpleType= importRewrite.addImport(fTypeNode, ast);
		IVariableBinding findInstance= findReference(fSelectedNode, newSimpleType);
		if (findInstance == null) {
			return null;
		}
		ASTRewrite rewrite= ASTRewrite.create(ast);
		if (fSelectedNode instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement= (ExpressionStatement)fSelectedNode;
			MethodInvocation oldMethodInvocation= (MethodInvocation) expressionStatement.getExpression();
			String methodName= oldMethodInvocation.getName().getIdentifier();
			MethodInvocation newMethodInvocation= ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(methodName));
			newMethodInvocation.setExpression(ast.newName(findInstance.getName()));

			List<Expression> arguments= oldMethodInvocation.arguments();
			for (Expression argument : arguments) {
				newMethodInvocation.arguments().add(rewrite.createCopyTarget(argument));
			}
			rewrite.replace(expressionStatement.getExpression(), newMethodInvocation, null);
			return rewrite;
		} else if (fSelectedNode instanceof QualifiedName) {
			QualifiedName qualifiedName= (QualifiedName)fSelectedNode;
			FieldAccess newFieldAccess= ast.newFieldAccess();
			newFieldAccess.setExpression(ast.newName(findInstance.getName()));
			newFieldAccess.setName(ast.newSimpleName(qualifiedName.getName().getFullyQualifiedName()));
			rewrite.replace(qualifiedName, newFieldAccess, null);
			return rewrite;
		}
		return null;
	}

	/*
	 * search local/class/superclass
	 */
	private IVariableBinding findReference(ASTNode node, Type type) {
		BodyDeclaration parent= ASTNodes.getParent(node, BodyDeclaration.class);
		// search local block
		Set<SimpleName> localVariableIdentifiers= ASTNodes.getLocalVariableIdentifiers(parent, true);
		for (SimpleName name : localVariableIdentifiers) {
			Type type2= ASTNodes.getType((VariableDeclaration) name.getParent());
			String qualifiedTypeName= ASTNodes.getQualifiedTypeName(type);
			String qualifiedTypeName2= ASTNodes.getQualifiedTypeName(type2);
			if (qualifiedTypeName.equals(qualifiedTypeName2) && node.getStartPosition() > name.getStartPosition()) {
				if (node instanceof QualifiedName) {
					// don't refer to the same declaration we are in and don't use a local identifier
					// initialized to null to avoid a possible runtime error
					if (node.getParent().getStartPosition() > name.getStartPosition()) {
						if (name.getParent() instanceof VariableDeclaration) {
							VariableDeclaration decl= (VariableDeclaration)name.getParent();
							Expression initializer= decl.getInitializer();
							if (initializer == null || initializer.getNodeType() != ASTNode.NULL_LITERAL) {
								return ((VariableDeclaration) name.getParent()).resolveBinding();
							}
						}
					}
				} else {
					return ((VariableDeclaration) name.getParent()).resolveBinding();
				}
			}
		}
		// search class hierarchy
		List<IVariableBinding> visibleLocalVariablesInScope= getVisibleLocalVariablesInScope(fSelectedNode);
		for (IVariableBinding binding : visibleLocalVariablesInScope) {
			// if we live in an static body, we must access a static reference
			if (Modifier.isStatic(parent.getModifiers()) && !Modifier.isStatic(binding.getModifiers())) {
				continue;
			}
			if (Bindings.equals(binding.getType(), fTypeNode)) {
				return binding;
			}
		}
		return null;
	}

	private static List<IVariableBinding> getVisibleLocalVariablesInScope(ASTNode node) {
		List<IVariableBinding> variableNames= new ArrayList<>();
		CompilationUnit root= (CompilationUnit) node.getRoot();
		IBinding[] bindings= new ScopeAnalyzer(root).getDeclarationsInScope(node.getStartPosition(), ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY);
		for (IBinding binding : bindings) {
			variableNames.add((IVariableBinding) binding);
		}
		return variableNames;
	}
}
