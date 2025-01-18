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

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.common.HelperVisitor;
import org.eclipse.jdt.internal.common.ReferenceHolder;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * Change
 *
 * Find: Reader is=new FileReader("file.txt")
 *
 * Rewrite: Reader is=new InputStreamReader(new
 * FileInputStream("file.txt"),Charset.defaultCharset());
 *
 * Charset.defaultCharset() is available since Java 1.5
 *
 */
public class FileReaderExplicitEncoding extends AbstractExplicitEncoding<ClassInstanceCreation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callClassInstanceCreationVisitor(FileReader.class, compilationUnit, datah, nodesprocessed, (visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore,
			Set<CompilationUnitRewriteOperation> operations, ChangeBehavior cb,
			ClassInstanceCreation visited, ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		switch (arguments.size()) {
			case 1:
				break;
			case 2:
				if (!(arguments.get(1) instanceof StringLiteral)) {
					return false;
				}
				StringLiteral argstring3= (StringLiteral) arguments.get(1);
				if (!encodings.contains(argstring3.getLiteralValue())) {
					return false;
				}
				holder.put(argstring3, encodingmap.get(argstring3.getLiteralValue()));
				break;
			default:
				return false;
		}
		operations.add(fixcore.rewrite(visited, cb, holder));
		return false;
	}

	@Override
	public void rewrite(UseExplicitEncodingFixCore upp, final ClassInstanceCreation visited, final CompilationUnitRewrite cuRewrite,
			TextEditGroup group, ChangeBehavior cb, ReferenceHolder<ASTNode, Object> data) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		ASTNode callToCharsetDefaultCharset= cb.computeCharsetASTNode(cuRewrite, ast, (String) data.get(visited),Nodedata.charsetConstants);
		/**
		 * new FileInputStream(<filename>)
		 */
		ClassInstanceCreation fisclassInstance= ast.newClassInstanceCreation();
		fisclassInstance.setType(ast.newSimpleType(addImport(FileInputStream.class.getCanonicalName(), cuRewrite, ast)));
		fisclassInstance.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((ASTNode) visited.arguments().get(0))));
		/**
		 * new InputStreamReader(new FileInputStream(<filename>))
		 */
		ClassInstanceCreation isrclassInstance= ast.newClassInstanceCreation();
		isrclassInstance.setType(ast.newSimpleType(addImport(InputStreamReader.class.getCanonicalName(), cuRewrite, ast)));
		isrclassInstance.arguments().add(fisclassInstance);
		isrclassInstance.arguments().add(callToCharsetDefaultCharset);

		ASTNodes.replaceButKeepComment(rewrite, visited, isrclassInstance, group);
	}

	@Override
	public String getPreview(boolean afterRefactoring, ChangeBehavior cb) {
		if (afterRefactoring) {
			return "Reader r=new InputStreamReader(new FileInputStream(inputfile)," + cb.computeCharsetforPreview() + ");\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "Reader r=new FileReader(inputfile);\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "new FileReader(inputfile)"; //$NON-NLS-1$
	}
}
