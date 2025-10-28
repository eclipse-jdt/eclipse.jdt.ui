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

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_TOSTRING;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
 * Change from
 *
 * <pre>
 * ByteArrayOutputStream ba= new ByteArrayOutputStream();
 *
 * String result= ba.toString();
 * </pre>
 *
 * <pre>
 * ByteArrayOutputStream ba= new ByteArrayOutputStream();
 * try {
 * 	String result= ba.toString(Charset.defaultCharset().displayName());
 * } catch (UnsupportedEncodingException e1) {
 * 	e1.printStackTrace();
 * }
 * </pre>
 *
 * since Java 10
 *
 * <pre>
 * ByteArrayOutputStream ba= new ByteArrayOutputStream();
 *
 * String result= ba.toString(Charset.defaultCharset());
 * </pre>
 *
 */
public class ByteArrayOutputStreamExplicitEncoding extends AbstractExplicitEncoding<MethodInvocation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		if (!JavaModelUtil.is10OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			/**
			 * For Java 9 and older just do nothing
			 */
			return;
		}
		ReferenceHolder<ASTNode, Object> holder= new ReferenceHolder<>();
		HelperVisitor.callMethodInvocationVisitor(ByteArrayOutputStream.class, METHOD_TOSTRING, compilationUnit, holder, nodesprocessed,
				(visited, aholder) -> processFoundNode(fixcore, operations, cb, visited, aholder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore, Set<CompilationUnitRewriteOperation> operations,
			ChangeBehavior cb, MethodInvocation visited,
			ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		if (ASTNodes.usesGivenSignature(visited, ByteArrayOutputStream.class.getCanonicalName(), METHOD_TOSTRING, String.class.getCanonicalName())) {
			if (!(arguments.get(0) instanceof StringLiteral)) {
				return false;
			}
			StringLiteral argstring3= (StringLiteral) arguments.get(0);
			if (!encodings.contains(argstring3.getLiteralValue())) {
				return false;
			}
			Nodedata nd= new Nodedata();
			nd.encoding= encodingmap.get(argstring3.getLiteralValue());
			nd.replace= true;
			nd.visited= argstring3;
			holder.put(visited, nd);
			operations.add(fixcore.rewrite(visited, cb, holder));
			return false;
		}
		if (ASTNodes.usesGivenSignature(visited, ByteArrayOutputStream.class.getCanonicalName(), METHOD_TOSTRING)) {
			Nodedata nd2= new Nodedata();
			nd2.encoding= null;
			nd2.replace= false;
			nd2.visited= visited;
			holder.put(visited, nd2);
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
		ASTNode callToCharsetDefaultCharset= cb.computeCharsetASTNode(cuRewrite, ast, nodedata.encoding, Nodedata.charsetConstants);
		/**
		 * Add Charset.defaultCharset().displayName() as second (last) parameter of "toString()"
		 * call Add Charset.defaultCharset() as second (last) parameter
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
		String insert= ""; //$NON-NLS-1$
		switch (cb) {
			case KEEP_BEHAVIOR:
				insert= "Charset.defaultCharset().displayName()"; //$NON-NLS-1$
				break;
			case ENFORCE_UTF8:
				insert= "StandardCharsets.UTF_8.displayName()"; //$NON-NLS-1$
				break;
		}
		if (afterRefactoring) {
			return "ByteArrayOutputStream ba=new ByteArrayOutputStream();\n" //$NON-NLS-1$
					+ "try {\n" //$NON-NLS-1$
					+ "	String result=ba.toString(" + insert + ");\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ "} catch (UnsupportedEncodingException e1) {\n" //$NON-NLS-1$
					+ "	e1.printStackTrace();\n" //$NON-NLS-1$
					+ "}\n"; //$NON-NLS-1$
		}
		return """
				ByteArrayOutputStream ba=new ByteArrayOutputStream();
				try {
					String result=ba.toString();
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				"""; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "ba.toString()"; //$NON-NLS-1$
	}
}
