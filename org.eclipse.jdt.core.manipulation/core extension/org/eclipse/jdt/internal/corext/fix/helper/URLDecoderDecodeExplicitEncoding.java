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

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_DECODE;

import java.net.URLDecoder;
import java.util.List;
import java.util.Set;

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
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Java 10
 *
 * Find: java.net.URLDecoder.decode("asdf","UTF-8")
 *
 * Rewrite: java.net.URLDecoder.decode("asdf",StandardCharsets.UTF_8)
 *
 * Find: java.net.URLDecoder.decode("asdf") Without the parameter the default is the file.encoding
 * system property so Charset.defaultCharset() URLDecoder.decode("asdf") is (nearly) the same as
 * URLDecoder.decode("asdf",Charset.defaultCharset()) But it is not really better (other than that
 * you can see that it is depending on the default charset)
 *
 * KEEP
 *
 * Rewrite: java.net.URLDecoder.decode("asdf",Charset.defaultCharset())
 *
 * USE_UTF8
 *
 * Rewrite: java.net.URLDecoder.decode("asdf",StandardCharsets.UTF_8) This changes how the code
 * works but it might be the better choice if you want to get rid of depending on environment
 * settings
 */
public class URLDecoderDecodeExplicitEncoding extends AbstractExplicitEncoding<MethodInvocation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		if (!JavaModelUtil.is10OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			/**
			 * For Java 9 and older just do nothing
			 */
			return;
		}
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callMethodInvocationVisitor(URLDecoder.class, METHOD_DECODE, compilationUnit, datah, nodesprocessed,
				(visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore,
			Set<CompilationUnitRewriteOperation> operations, ChangeBehavior cb,
			MethodInvocation visited, ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		if (ASTNodes.usesGivenSignature(visited, URLDecoder.class.getCanonicalName(), METHOD_DECODE, String.class.getCanonicalName(), String.class.getCanonicalName())) {
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
		if (ASTNodes.usesGivenSignature(visited, URLDecoder.class.getCanonicalName(), METHOD_DECODE, String.class.getCanonicalName())) {
			Nodedata nd= new Nodedata();
			switch (cb) {
				case KEEP_BEHAVIOR:
					nd.encoding= null;
					break;
				case ENFORCE_UTF8:
					nd.encoding= "UTF_8"; //$NON-NLS-1$
					break;
			}
			nd.replace= false;
			nd.visited= visited;
			holder.put(visited, nd);
			operations.add(fixcore.rewrite(visited, cb, holder));
			return false;
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
		 * Add Charset.defaultCharset() or StandardCharsets.UTF_8 as second (last) parameter
		 */
		ListRewrite listRewrite= rewrite.getListRewrite(visited, MethodInvocation.ARGUMENTS_PROPERTY);
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
			return "java.net.URLDecoder.decode(\"asdf\", StandardCharsets.UTF_8);\n"; //$NON-NLS-1$
		}
		return "java.net.URLDecoder.decode(\"asdf\", \"UTF-8\");\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "URLDecoder.decode()"; //$NON-NLS-1$
	}
}
