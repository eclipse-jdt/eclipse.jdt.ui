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

import java.util.Formatter;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.common.HelperVisitor;
import org.eclipse.jdt.internal.common.ReferenceHolder;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 *
 * Find: new java.util.Formatter(new File(), String cs) throws UnsupportedEncodingException
 *
 * Rewrite: new java.util.Formatter(new File(), Charset cs)
 *
 * Find: new java.util.Formatter(new File(), String cs,new java.util.Locale())
 *
 * Rewrite: new java.util.Formatter(new File(), Charset cs,new java.util.Locale())
 *
 * Find: new java.util.Formatter(new java.io.OutputStream(), String cs)
 *
 * Rewrite: new java.util.Formatter(new java.io.OutputStream(), Charset cs)
 *
 * Find: new java.util.Formatter(new java.io.OutputStream(), String cs,new java.util.Locale())
 *
 * Rewrite: new java.util.Formatter(new java.io.OutputStream(), Charset cs,new java.util.Locale())
 *
 * Find: new java.util.Formatter(new String(), String cs)
 *
 * Rewrite: new java.util.Formatter(new String(), Charset cs)
 *
 * Find: new java.util.Formatter(new String(), String cs,new java.util.Locale())
 *
 * Rewrite: new java.util.Formatter(new String(), Charset cs,new java.util.Locale())
 *
 * Find: new java.util.Formatter(new File())
 *
 * Rewrite: new java.util.Formatter(new File(), Charset.defaultCharset()) depends on
 * https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/lang/System.html#file.encoding
 *
 */
public class FormatterExplicitEncoding extends AbstractExplicitEncoding<ClassInstanceCreation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callClassInstanceCreationVisitor(Formatter.class, compilationUnit, datah, nodesprocessed, (visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore,
			Set<CompilationUnitRewriteOperation> operations,
			ChangeBehavior cb,
			ClassInstanceCreation visited,
			ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		Nodedata nd= new Nodedata();

		switch (arguments.size()) {
			case 2:
			case 3:
				if (arguments.get(1) instanceof StringLiteral) {
					StringLiteral argString= (StringLiteral) arguments.get(1);
					String encodingKey= argString.getLiteralValue().toUpperCase();

					if (encodings.contains(encodingKey)) {
						nd.encoding= encodingmap.get(encodingKey);
						nd.replace= true;
						nd.visited= argString;
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
		ImportRewrite importRewriter= cuRewrite.getImportRewrite();
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
		removeUnsupportedEncodingException(visited, group, rewrite, importRewriter);
	}

	@Override
	public String getPreview(boolean afterRefactoring, ChangeBehavior cb) {
		if (afterRefactoring) {
			return "Formatter r=new java.util.Formatter(out, " + cb.computeCharsetforPreview() + ");\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "Formatter r=new java.util.Formatter(out);\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "new java.util.Formatter(out)"; //$NON-NLS-1$
	}
}
