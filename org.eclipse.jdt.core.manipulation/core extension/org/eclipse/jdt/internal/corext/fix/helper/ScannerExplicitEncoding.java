/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.common.HelperVisitor;
import org.eclipse.jdt.internal.common.ReferenceHolder;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 *
 * Java 10
 *
 * Change
 *
 * Find: new java.util.Scanner(new File("filename.txt"),"UTF-8")
 *
 * Rewrite: new java.util.Scanner(new File("filename.txt"),StandardCharsets.UTF_8);
 *
 * Find: new java.util.Scanner("filename.txt", "UTF-8")
 *
 * Rewrite: new java.util.Scanner("filename.txt", StandardCharsets.UTF_8)
 *
 * Find: new java.util.Scanner(java.io.OutputStream, "UTF-8")
 *
 * Rewrite: new java.util.Scanner(java.io.OutputStream, StandardCharsets.UTF_8)
 *
 * Find: new java.util.Scanner(java.io.OutputStream)
 *
 * Rewrite: new java.util.Scanner(java.io.OutputStream, Charset.defaultCharset())
 */
public class ScannerExplicitEncoding extends AbstractExplicitEncoding<ClassInstanceCreation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		if (!JavaModelUtil.is10OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			/**
			 * For Java 9 and older just do nothing
			 */
			return;
		}
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callClassInstanceCreationVisitor(Scanner.class, compilationUnit, datah, nodesprocessed, (visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore, Set<CompilationUnitRewriteOperation> operations,
			ChangeBehavior cb, ClassInstanceCreation visited,
			ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		Nodedata nd= new Nodedata();

		switch (arguments.size()) {
			case 4:
			case 2:
				int encodingIndex= (arguments.size() == 4) ? 3 : 1;
				ASTNode argumentNode= arguments.get(encodingIndex);

				if (argumentNode instanceof StringLiteral) {
					StringLiteral encodingLiteral= (StringLiteral) argumentNode;
					String encodingValue= encodingLiteral.getLiteralValue().toUpperCase();

					if (encodings.contains(encodingValue)) {
						nd.encoding= encodingmap.get(encodingValue);
						nd.replace= true;
						nd.visited= encodingLiteral;
						holder.put(visited, nd);
						operations.add(fixcore.rewrite(visited, cb, holder));
					}
				}
				break;

			case 1:
				nd.encoding= null;
				nd.replace= false;
				nd.visited= visited;
				holder.put(visited, nd);
				operations.add(fixcore.rewrite(visited, cb, holder));
				break;

			default:
				break;
		}

		return false;
	}

	@Override
	public void rewrite(UseExplicitEncodingFixCore upp, final ClassInstanceCreation visited, final CompilationUnitRewrite cuRewrite,
			TextEditGroup group, ChangeBehavior cb, ReferenceHolder<ASTNode, Object> data) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		Nodedata nodedata= (Nodedata) data.get(visited);
		ASTNode callToCharsetDefaultCharset= cb.computeCharsetASTNode(cuRewrite, ast, nodedata.encoding,Nodedata.charsetConstants);
		/**
		 * Add Charset.defaultCharset() as second (last) parameter
		 */
		ListRewrite listRewrite= rewrite.getListRewrite(visited, ClassInstanceCreation.ARGUMENTS_PROPERTY);
		if (nodedata.replace) {
			listRewrite.replace(nodedata.visited, callToCharsetDefaultCharset, group);
		} else {
			listRewrite.insertLast(callToCharsetDefaultCharset, group);
		}
	}

	@Override
	public String getPreview(boolean afterRefactoring, ChangeBehavior cb) {
		if (afterRefactoring) {
			return "new java.util.Scanner(\"asdf\",StandardCharsets.UTF_8);\n"; //$NON-NLS-1$
		}
		return "new java.util.Scanner(\"asdf\", \"UTF-8\");\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "new java.util.Scanner()"; //$NON-NLS-1$
	}
}
