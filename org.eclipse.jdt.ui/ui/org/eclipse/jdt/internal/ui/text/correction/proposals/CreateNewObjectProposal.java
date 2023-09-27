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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * <pre>
 * - propose create new local instance
 * </pre>
 */
public class CreateNewObjectProposal extends LinkedCorrectionProposal {

	private ITypeBinding fTypeNode;

	private IVariableBinding fVariableBinding;

	private Statement fSelectedNode;

	private VariableDeclarationFragment fVariableDeclarationFragment;

	private AST fAST;

	private CompilationUnit fCu;


	public CreateNewObjectProposal(ICompilationUnit cu, Statement selectedNode, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fSelectedNode= Objects.requireNonNull(selectedNode);
		fTypeNode= Objects.requireNonNull(typeNode);
		fAST= fSelectedNode.getAST();
		fCu= (CompilationUnit) fSelectedNode.getRoot();
	}

	public CreateNewObjectProposal(ICompilationUnit cu, VariableDeclarationFragment variableDeclarationFragment, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fVariableDeclarationFragment= Objects.requireNonNull(variableDeclarationFragment);
		fTypeNode= Objects.requireNonNull(typeNode);
		fAST= fVariableDeclarationFragment.getAST();
		fCu= (CompilationUnit) fVariableDeclarationFragment.getRoot();
	}

	public CreateNewObjectProposal(ICompilationUnit cu, VariableDeclarationFragment variableDeclarationFragment, IVariableBinding variableBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fVariableDeclarationFragment= Objects.requireNonNull(variableDeclarationFragment);
		fVariableBinding= Objects.requireNonNull(variableBinding);
		fTypeNode= variableBinding.getDeclaringClass();
		fAST= fVariableDeclarationFragment.getAST();
		fCu= (CompilationUnit) fVariableDeclarationFragment.getRoot();
	}

	/**
	 * @return TRUE if typeNode is a class
	 */
	public boolean hasProposal() {
		return fTypeNode.isClass();
	}

	@Override
	public Image getImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(
				new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
	}

	@Override
	public String getName() {
		return Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createnew_instance_of_object, fTypeNode.getName());
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		ASTRewrite rewrite= ASTRewrite.create(fAST);
		ImportRewrite importRewrite= createImportRewrite(fCu);

		IMethodBinding constructorMethod= getConstructorMethod();
		List<Expression> typeArguments= getConstructorArguments(rewrite, constructorMethod.getParameterTypes());

		Type newSimpleType= importRewrite.addImport(fTypeNode, fAST);

		if (fVariableBinding != null && fVariableDeclarationFragment != null) {
			BodyDeclaration findParentBodyDeclaration= ASTResolving.findParentBodyDeclaration(fVariableDeclarationFragment);
			String newVariableName= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL,
					getCompilationUnit().getJavaProject(), newSimpleType, null,
					Arrays.asList(ASTResolving.getUsedVariableNames(findParentBodyDeclaration)))[0];

			VariableDeclarationFragment newVariableDeclarationFragment= fAST.newVariableDeclarationFragment();
			newVariableDeclarationFragment.setName(fAST.newSimpleName(newVariableName));
			VariableDeclarationStatement newVariableDeclarationStatement= fAST.newVariableDeclarationStatement(newVariableDeclarationFragment);
			newVariableDeclarationStatement.setType(newSimpleType);
			ClassInstanceCreation classInstanceCreation= fAST.newClassInstanceCreation();
			classInstanceCreation.setType(importRewrite.addImport(fTypeNode, fAST));
			classInstanceCreation.arguments().addAll(typeArguments);
			newVariableDeclarationFragment.setInitializer(classInstanceCreation);

			ASTNode parentBlock= ASTNodes.getParent(fVariableDeclarationFragment, ASTNode.BLOCK);
			ListRewrite listRewrite= rewrite.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
			listRewrite.insertBefore(newVariableDeclarationStatement, fVariableDeclarationFragment.getParent(), null);

			QualifiedName qualifiedName= (QualifiedName) fVariableDeclarationFragment.getInitializer();
			QualifiedName newQualifiedName= fAST.newQualifiedName(fAST.newName(newVariableName), (SimpleName) rewrite.createCopyTarget(qualifiedName.getName()));
			rewrite.replace(fVariableDeclarationFragment.getInitializer(), newQualifiedName, null);

			addLinkedRanges(rewrite, classInstanceCreation);
		}

		else if (fSelectedNode instanceof ExpressionStatement) {
			ExpressionStatement selectedNode= (ExpressionStatement) fSelectedNode;
			String newVariableName= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL,
					getCompilationUnit().getJavaProject(), newSimpleType, null,
					Arrays.asList(ASTResolving.getUsedVariableNames(ASTResolving.findParentBodyDeclaration(selectedNode))))[0];

			VariableDeclarationFragment newVariableDeclarationFragment= fAST.newVariableDeclarationFragment();
			newVariableDeclarationFragment.setName(fAST.newSimpleName(newVariableName));
			VariableDeclarationStatement newVariableDeclarationStatement= fAST.newVariableDeclarationStatement(newVariableDeclarationFragment);
			newVariableDeclarationStatement.setType(newSimpleType);
			ClassInstanceCreation classInstanceCreation= fAST.newClassInstanceCreation();
			classInstanceCreation.setType(importRewrite.addImport(fTypeNode, fAST));
			classInstanceCreation.arguments().addAll(typeArguments);
			newVariableDeclarationFragment.setInitializer(classInstanceCreation);

			ASTNode parentBlock= ASTNodes.getParent(selectedNode, ASTNode.BLOCK);
			ListRewrite listRewrite= rewrite.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
			listRewrite.insertBefore(newVariableDeclarationStatement, selectedNode, null);

			MethodInvocation oldMethodInvocation= (MethodInvocation) selectedNode.getExpression();
			String methodName= oldMethodInvocation.getName().getIdentifier();

			MethodInvocation newMethodInvocation= fAST.newMethodInvocation();
			newMethodInvocation.setName(fAST.newSimpleName(methodName));
			newMethodInvocation.setExpression(fAST.newName(newVariableName));
			List<Expression> arguments= oldMethodInvocation.arguments();
			for (Expression argument : arguments) {
				newMethodInvocation.arguments().add(rewrite.createCopyTarget(argument));
			}
			rewrite.replace(selectedNode.getExpression(), newMethodInvocation, null);

			addLinkedRanges(rewrite, classInstanceCreation);
		} else if (fVariableDeclarationFragment != null) {
			VariableDeclarationFragment newVariableDeclarationFragment= fAST.newVariableDeclarationFragment();
			newVariableDeclarationFragment.setName(fAST.newSimpleName(fVariableDeclarationFragment.getName().getIdentifier()));

			ClassInstanceCreation classInstanceCreation= fAST.newClassInstanceCreation();
			classInstanceCreation.setType(importRewrite.addImport(fTypeNode, fAST));
			classInstanceCreation.arguments().addAll(typeArguments);
			newVariableDeclarationFragment.setInitializer(classInstanceCreation);

			rewrite.replace(fVariableDeclarationFragment, newVariableDeclarationFragment, null);
		}
		return rewrite;
	}

	private IMethodBinding getConstructorMethod() {
		for (IMethodBinding iMethodBinding : fTypeNode.getDeclaredMethods()) {
			if (iMethodBinding.isDefaultConstructor() || iMethodBinding.isConstructor()) {
				return iMethodBinding;
			}
		}
		return null;
	}

	private static List<Expression> getConstructorArguments(ASTRewrite rewrite, ITypeBinding[] typeBindings) {
		List<Expression> constructorArgs= new ArrayList<>();
		AST ast= rewrite.getAST();
		for (ITypeBinding typeBinding : typeBindings) {
			constructorArgs.add(ASTNodeFactory.newDefaultExpression(ast, typeBinding));
		}
		return constructorArgs;
	}

	private void addLinkedRanges(ASTRewrite rewrite, ClassInstanceCreation newStub) {
		List<Expression> parameters= newStub.arguments();
		int index= 0;
		for (Expression curr : parameters) {
			addLinkedPosition(rewrite.track(curr), false, "arg_" + (index++)); //$NON-NLS-1$
		}
	}
}
