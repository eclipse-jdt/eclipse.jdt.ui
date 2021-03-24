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
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.List;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder.OccurrenceLocation;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.search.SearchMessages;


/**
 * Class used to find the target for a EnumConstructor statement according to the language
 * specification.
 * <p>
 * The target statement is a EnumConstructor statement.
 *
 * @since 3.22
 */
public class EnumConstructorTargetFinder extends ASTVisitor {

	private ASTNode fSelected;

	private CompilationUnit fASTRoot;

	/*
	 * Initializes the finder. Returns <code>TRUE</code> if everything is OK.
	 */
	public boolean initialize(CompilationUnit root, EnumConstantDeclaration node) {
		if (node != null) {
			fASTRoot= root;
			try {
				if (root.getTypeRoot() == null || root.getTypeRoot().getBuffer() == null) {
					return false;
				}
			} catch (JavaModelException e) {
				return false;
			}
			fSelected= node;
			return true;
		}
		return false;
	}

	/*
	 * @return the location of target match or <code>null</code> if no matches are found
	 */
	public OccurrenceLocation getOccurrence() {
		ASTNode targetNode= findEnumConstructor(fASTRoot, (EnumConstantDeclaration) fSelected);
		if (targetNode == null) {
			return null;
		}
		String description= Messages.format(SearchMessages.EnumConstructorTargetFinder_description, ASTNodes.asString(targetNode));
		OccurrenceLocation location= new OccurrenceLocation(targetNode.getStartPosition(), targetNode.getLength(), 0, description);
		return location;
	}

	private ASTNode findEnumConstructor(CompilationUnit cu, EnumConstantDeclaration enumNode) {
		ASTNode enumDeclarationNode= ASTResolving.findParentType(enumNode);
		if (enumDeclarationNode == null) {
			return null;
		}
		if (enumDeclarationNode.getNodeType() != ASTNode.ENUM_DECLARATION) {
			return null;
		}
		EnumDeclaration enumDeclaration= (EnumDeclaration) enumDeclarationNode;
		List<ASTNode> enumNodeArguments= enumNode.arguments();
		List<ASTNode> enumBodyDeclarations= enumDeclaration.bodyDeclarations();

		declarationLoop: for (ASTNode enumBodyDeclarationNode : enumBodyDeclarations) {
			if (enumBodyDeclarationNode.getNodeType() != ASTNode.METHOD_DECLARATION) {
				continue;
			}
			MethodDeclaration enumMethodDeclaration= (MethodDeclaration) enumBodyDeclarationNode;
			if (!enumMethodDeclaration.getName().getIdentifier().equals(enumDeclaration.getName().getIdentifier())) {
				continue;
			}

			List<SingleVariableDeclaration> enumBodyDeclarationNodeParameters= enumMethodDeclaration.parameters();
			if (enumBodyDeclarationNodeParameters.size() != enumNodeArguments.size()) {
				continue;
			}

			for (int i= 0; i < enumBodyDeclarationNodeParameters.size(); i++) {
				ASTNode nodeArgument= enumNodeArguments.get(i);
				SingleVariableDeclaration singleVariableDeclaration= enumBodyDeclarationNodeParameters.get(i);
				if(singleVariableDeclaration.isVarargs()) {
					continue declarationLoop;
				}
				Type parameterType= singleVariableDeclaration.getType();
				if (parameterType == null) {
					continue declarationLoop;
				}
				ITypeBinding nodeTypeParameter= parameterType.resolveBinding();
				if (nodeTypeParameter == null) {
					continue declarationLoop;
				}

				ITypeBinding nodeTypeArgument= null;
				if (nodeArgument.getNodeType() == ASTNode.METHOD_INVOCATION) {
					nodeTypeArgument= ((Expression) nodeArgument).resolveTypeBinding();
				} else if (nodeArgument.getNodeType() == ASTNode.METHOD_INVOCATION) {
					nodeTypeArgument= ((MethodInvocation) nodeArgument).resolveMethodBinding().getReturnType();
				} else if (nodeArgument.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
					nodeTypeArgument= ((ClassInstanceCreation) nodeArgument).resolveTypeBinding();
				} else if (nodeArgument.getParent().getNodeType() == ASTNode.ENUM_CONSTANT_DECLARATION) {
					IMethodBinding resolveConstructorBinding= ((EnumConstantDeclaration) nodeArgument.getParent()).resolveConstructorBinding();
					ITypeBinding[] parameterTypes= resolveConstructorBinding.getParameterTypes();
					if (parameterTypes.length != enumBodyDeclarationNodeParameters.size()) {
						continue declarationLoop;
					}
					nodeTypeArgument= parameterTypes[i];
				}
				if (nodeTypeArgument == null) {
					nodeTypeArgument= ASTResolving.guessBindingForReference(nodeArgument);
				}
				if (nodeTypeArgument == null) {
					continue declarationLoop;
				}

				if (Bindings.equals(nodeTypeArgument, nodeTypeParameter)) {
					continue;
				}
				ITypeBinding unboxedTypeArgument= Bindings.getUnboxedTypeBinding(nodeTypeArgument, cu.getAST());
				ITypeBinding unboxedTypeParameter= Bindings.getUnboxedTypeBinding(nodeTypeParameter, cu.getAST());
				if (Bindings.equals(unboxedTypeArgument, unboxedTypeParameter)) {
					continue;
				}
				continue declarationLoop;
			} // for all enum constructor parameters

			return ((MethodDeclaration) enumBodyDeclarationNode).getName();

		} // for all enum declaration nodes
		return null;
	}
}
