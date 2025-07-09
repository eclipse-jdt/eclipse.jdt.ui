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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
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
 * Find: Stream fw=new PrintStream("file.txt", "UTF-8")
 *
 * Rewrite: Stream fw=new PrintStream("file.txt", StandardCharsets.UTF_8)
 *
 * Find: Stream fw=new PrintStream(new File("file.txt"), "UTF-8")
 *
 * Rewrite: Stream fw=new PrintStream(new File("file.txt"), StandardCharsets.UTF_8)
 *
 * Find: Stream fw=new PrintStream(new java.io.OutputStream(), boolean, "UTF-8")
 *
 * Rewrite: Stream fw=new PrintStream(new java.io.OutputStream(), boolean, StandardCharsets.UTF_8)
 *
 */
public class PrintStreamExplicitEncoding extends AbstractExplicitEncoding<ClassInstanceCreation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callClassInstanceCreationVisitor(PrintStream.class, compilationUnit, datah, nodesprocessed, (visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore, Set<CompilationUnitRewriteOperation> operations,
			ChangeBehavior cb, ClassInstanceCreation visited,
			ReferenceHolder<ASTNode, Object> holder) {
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
			case 3:
				if (!(arguments.get(2) instanceof StringLiteral)) {
					return false;
				}
				StringLiteral argstring4= (StringLiteral) arguments.get(2);
				if (!encodings.contains(argstring4.getLiteralValue())) {
					return false;
				}
				holder.put(argstring4, encodingmap.get(argstring4.getLiteralValue()));
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
		 * new FileOutputStream(<filename>)
		 */
		ClassInstanceCreation fosclassInstance= ast.newClassInstanceCreation();
		fosclassInstance.setType(ast.newSimpleType(addImport(FileOutputStream.class.getCanonicalName(), cuRewrite, ast)));
		fosclassInstance.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((ASTNode) visited.arguments().get(0))));
		/**
		 * new OutputStreamWriter(new FileOutputStream(<filename>))
		 */
		ClassInstanceCreation oswclassInstance= ast.newClassInstanceCreation();
		oswclassInstance.setType(ast.newSimpleType(addImport(OutputStreamWriter.class.getCanonicalName(), cuRewrite, ast)));
		oswclassInstance.arguments().add(fosclassInstance);
		oswclassInstance.arguments().add(callToCharsetDefaultCharset);
		/**
		 * new BufferedWriter(new OutputStreamWriter(new FileOutputStream(<filename>)))
		 */
		ClassInstanceCreation bwclassInstance= ast.newClassInstanceCreation();
		bwclassInstance.setType(ast.newSimpleType(addImport(BufferedWriter.class.getCanonicalName(), cuRewrite, ast)));
		bwclassInstance.arguments().add(oswclassInstance);

		ASTNodes.replaceButKeepComment(rewrite, visited, bwclassInstance, group);
	}

	@Override
	public String getPreview(boolean afterRefactoring, ChangeBehavior cb) {
		if (afterRefactoring) {
			return "Stream w=new PrintStream(\"out.txt\"," + cb.computeCharsetforPreview() + ");\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"Stream w=new PrintStream(\"out.txt\",StandardCharsets.UTF_8);\n" + //$NON-NLS-1$
					"Stream w=new PrintStream(new File(\"out.txt\"),StandardCharsets.UTF_8);\n"; //$NON-NLS-1$
		}
		return """
				Stream w=new PrintStream("out.txt");
				Stream w=new PrintStream("out.txt","UTF-8");
				Stream w=new PrintStream(new File("out.txt"),"UTF-8");
				"""; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "new PrintStream(\"out.txt\")"; //$NON-NLS-1$
	}
}
