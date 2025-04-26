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

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_NEW_READER;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
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
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Java 10
 *
 * Change
 *
 * Find: Reader r=Channels.newReader(ch,"UTF-8")
 *
 * Rewrite: Reader r=Channels.newReader(ch,StandardCharsets.UTF_8)
 *
 * Find: Reader r5 = Channels.newReader(ch, "ISO-8859-1", 0, 1024)
 *
 * Rewrite: Reader r5 = Channels.newReader(ch, StandardCharsets.ISO_8859_1, 0, 1024)
 *
 *
 *
 */
public class ChannelsNewReaderExplicitEncoding extends AbstractExplicitEncoding<MethodInvocation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		if (!JavaModelUtil.is10OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			/**
			 * For Java 9 and older just do nothing
			 */
			return;
		}
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callMethodInvocationVisitor(Channels.class, METHOD_NEW_READER, compilationUnit, datah, nodesprocessed,
				(visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore,
			Set<CompilationUnitRewriteOperation> operations, ChangeBehavior cb,
			MethodInvocation visited, ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		if (ASTNodes.usesGivenSignature(visited, Channels.class.getCanonicalName(), METHOD_NEW_READER,
				ReadableByteChannel.class.getCanonicalName(), String.class.getCanonicalName())) {

			ASTNode encodingArg= arguments.get(1);

			String encodingValue= null;
			if (encodingArg instanceof StringLiteral) {
				encodingValue= ((StringLiteral) encodingArg).getLiteralValue().toUpperCase();
			} else if (encodingArg instanceof SimpleName) {
				encodingValue= findVariableValue((SimpleName) encodingArg, visited);
			}

			if (encodingValue != null && encodings.contains(encodingValue)) {
				Nodedata nd= new Nodedata();
				nd.encoding= encodingmap.get(encodingValue);
				nd.replace= true;
				nd.visited= encodingArg;
				holder.put(visited, nd);
				operations.add(fixcore.rewrite(visited, cb, holder));
				return false;
			}
		}
		return false;
	}

	@Override
	public void rewrite(UseExplicitEncodingFixCore upp, final MethodInvocation visited, final CompilationUnitRewrite cuRewrite,
			TextEditGroup group, ChangeBehavior cb, ReferenceHolder<ASTNode, Object> data) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		ImportRewrite importRewriter= cuRewrite.getImportRewrite();
		Nodedata nodedata= (Nodedata) data.get(visited);
		ASTNode callToCharsetDefaultCharset= cb.computeCharsetASTNode(cuRewrite, ast, nodedata.encoding,Nodedata.charsetConstants);
		/**
		 * Add Charset.defaultCharset() as second (last) parameter
		 */
		ListRewrite listRewrite= rewrite.getListRewrite(visited, MethodInvocation.ARGUMENTS_PROPERTY);
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
			return "Reader r=Channels.newReader(ch,StandardCharsets.UTF_8);\n"; //$NON-NLS-1$
		}
		return "Reader r=Channels.newReader(ch,\"UTF-8\");\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "Channels.newReader(ch,StandardCharsets.UTF_8)"; //$NON-NLS-1$
	}
}
