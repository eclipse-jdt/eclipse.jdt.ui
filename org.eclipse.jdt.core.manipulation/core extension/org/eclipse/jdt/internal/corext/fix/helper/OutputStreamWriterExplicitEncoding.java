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

import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

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
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * Change
 *
 * Find: OutputStreamWriter os=new OutputStreamWriter(InputStream in, String cs)
 *
 * Rewrite: OutputStreamWriter os=new OutputStreamWriter(InputStream in, CharSet cs)
 *
 *
 * Find: OutputStreamWriter os=new OutputStreamWriter(InputStream in)
 *
 * Rewrite: OutputStreamWriter os=new OutputStreamWriter(InputStream in, Charset.defaultCharset())
 *
 *
 * Find: OutputStreamWriter os=new OutputStreamWriter(InputStream in)
 *
 * Rewrite: OutputStreamWriter os=new OutputStreamWriter(InputStream in, StandardCharsets.UTF_8)
 *
 * Charset.defaultCharset() is available since Java 1.5
 *
 */
public class OutputStreamWriterExplicitEncoding extends AbstractExplicitEncoding<ClassInstanceCreation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callClassInstanceCreationVisitor(OutputStreamWriter.class, compilationUnit, datah, nodesprocessed,
				(visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore,
			Set<CompilationUnitRewriteOperation> operations, ChangeBehavior cb,
			ClassInstanceCreation visited, ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		switch (arguments.size()) {
			case 2:
				if (!(arguments.get(1) instanceof StringLiteral)) {
					return false;
				}
				StringLiteral argstring3= (StringLiteral) arguments.get(1);
				if (!encodings.contains(argstring3.getLiteralValue())) {
					return false;
				}
				Nodedata nd= new Nodedata();
				nd.encoding= encodingmap.get(argstring3.getLiteralValue());
				nd.replace= true;
				nd.visited= argstring3;
				holder.put(visited, nd);
				operations.add(fixcore.rewrite(visited, cb, holder));
				break;
			case 1:
				Nodedata nd2= new Nodedata();
				nd2.encoding= null;
				nd2.replace= false;
				nd2.visited= visited;
				holder.put(visited, nd2);
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
			try {
				ASTNodes.replaceAndRemoveNLS(rewrite, nodedata.visited, callToCharsetDefaultCharset, group, cuRewrite);
			} catch (CoreException e) {
				JavaManipulationPlugin.log(e); // should never happen
			}
		} else {
			listRewrite.insertLast(callToCharsetDefaultCharset, group);
		}
		removeUnsupportedEncodingException(visited, group, rewrite, importRewriter);
	}

	@Override
	public String getPreview(boolean afterRefactoring, ChangeBehavior cb) {
		if (afterRefactoring) {
			return "Writer w = new OutputStreamWriter(out, " + cb.computeCharsetforPreview() //$NON-NLS-1$
					+ ");\nOutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(\"\"), StandardCharsets.UTF_8);\n"; //$NON-NLS-1$
		}
		return "Writer w = new OutputStreamWriter(out);\nOutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(\"\"), \"UTF-8\");\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "new OutputStreamWriter(out)"; //$NON-NLS-1$
	}
}
