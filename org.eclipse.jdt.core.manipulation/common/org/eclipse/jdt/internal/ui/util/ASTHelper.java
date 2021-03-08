/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Modifier;

public class ASTHelper {

	@SuppressWarnings("deprecation")
	private static boolean isNodeTypeSupportedInAST(AST ast, int nodeType) {
		switch (nodeType) {
			case ASTNode.SWITCH_CASE:
			case ASTNode.SWITCH_EXPRESSION:
			case ASTNode.YIELD_STATEMENT:
				return ast.apiLevel() >= AST.JLS14;
			case ASTNode.TEXT_BLOCK:
				return ast.apiLevel() >= AST.JLS15;
			case ASTNode.RECORD_DECLARATION:
			case ASTNode.INSTANCEOF_EXPRESSION:
				return ast.isPreviewEnabled();
			default:
				break;
		}
		return true;
	}

	private static boolean isModifierSupportedInAST(AST ast, int modifier) {
		switch (modifier) {
			case Modifier.SEALED:
			case Modifier.NON_SEALED:
				return ast.isPreviewEnabled();
			default:
				break;
		}
		return true;
	}

	public static boolean isYieldNodeSupportedInAST(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.YIELD_STATEMENT);
	}

	public static boolean isSwitchExpressionNodeSupportedInAST(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.SWITCH_EXPRESSION);
	}

	public static boolean isTextBlockNodeSupportedInAST(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.TEXT_BLOCK);
	}

	public static boolean isSwitchCaseExpressionsSupportedInAST(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.SWITCH_CASE);
	}

	public static boolean isRecordDeclarationNodeSupportedInAST(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.RECORD_DECLARATION);
	}

	public static boolean isSealedTypeSupportedInAST(AST ast) {
		return isModifierSupportedInAST(ast, Modifier.SEALED);
	}

}
