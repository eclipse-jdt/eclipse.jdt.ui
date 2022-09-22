/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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

@SuppressWarnings("deprecation")
public class ASTHelper {

	public static final int JLS2 = AST.JLS2;
	public static final int JLS3 = AST.JLS3;
	public static final int JLS4 = AST.JLS4;
	public static final int JLS8 = AST.JLS8;
	public static final int JLS9 = AST.JLS9;
	public static final int JLS10 = AST.JLS10;
	public static final int JLS11 = AST.JLS11;
	public static final int JLS12 = AST.JLS12;
	public static final int JLS13 = AST.JLS13;
	public static final int JLS14 = AST.JLS14;
	public static final int JLS15 = AST.JLS15;
	public static final int JLS16 = AST.JLS16;
	public static final int JLS17 = AST.JLS17;
	public static final int JLS18 = AST.JLS18;
	public static final int JLS19 = AST.JLS19;

	private static boolean isNodeTypeSupportedInAST(AST ast, int nodeType) {
		switch (nodeType) {
			case ASTNode.SWITCH_CASE:
			case ASTNode.SWITCH_EXPRESSION:
			case ASTNode.YIELD_STATEMENT:
				return ast.apiLevel() >= JLS14;
			case ASTNode.TEXT_BLOCK:
				return ast.apiLevel() >= JLS15;
			case ASTNode.RECORD_DECLARATION:
			case ASTNode.INSTANCEOF_EXPRESSION:
				return ast.apiLevel() >= JLS16;
			case ASTNode.TAG_PROPERTY:
				return ast.apiLevel() >= AST.JLS18;
			case ASTNode.TYPE_PATTERN:
			case ASTNode.RECORD_PATTERN:
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
				return ast.apiLevel() >= JLS17;
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

	public static boolean isInstanceofExpressionPatternSupported(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.INSTANCEOF_EXPRESSION);
	}

	public static boolean isPatternSupported(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.TYPE_PATTERN);
	}

	public static boolean isRecordPatternSupported(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.RECORD_PATTERN);
	}

	public static boolean isJavaDocCodeSnippetSupported(AST ast) {
		return isNodeTypeSupportedInAST(ast, ASTNode.TAG_PROPERTY);
	}
}
