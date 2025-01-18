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

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_STORE_TO_XML;

import java.util.List;
import java.util.Properties;
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
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Find: Properties.storeToXML(java.io.OutputStream,"comment","UTF-8") throws
 * UnsupportedEncodingException By default the UTF-8 character encoding is used so
 * Properties.storeToXML(java.io.OutputStream,"comment") is the same as
 * Properties.storeToXML(java.io.OutputStream,"comment", StandardCharsets.UTF_8)
 *
 * Rewrite: Properties.storeToXML(java.io.OutputStream,"comment", StandardCharsets.UTF_8)
 *
 */
public class PropertiesStoreToXMLExplicitEncoding extends AbstractExplicitEncoding<MethodInvocation> {

	@Override
	public void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb) {
		if (!JavaModelUtil.is10OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			/**
			 * For Java 9 and older just do nothing
			 */
			return;
		}
		ReferenceHolder<ASTNode, Object> datah= new ReferenceHolder<>();
		HelperVisitor.callMethodInvocationVisitor(Properties.class, METHOD_STORE_TO_XML, compilationUnit, datah, nodesprocessed,
				(visited, holder) -> processFoundNode(fixcore, operations, cb, visited, holder));
	}

	private static boolean processFoundNode(UseExplicitEncodingFixCore fixcore,
			Set<CompilationUnitRewriteOperation> operations, ChangeBehavior cb,
			MethodInvocation visited, ReferenceHolder<ASTNode, Object> holder) {
		List<ASTNode> arguments= visited.arguments();
		switch (arguments.size()) {
			case 3:
				if (!(arguments.get(2) instanceof StringLiteral)) {
					return false;
				}
				StringLiteral argstring3= (StringLiteral) arguments.get(2);
				if (!encodings.contains(argstring3.getLiteralValue().toUpperCase())) {
					return false;
				}
				Nodedata nd= new Nodedata();
				nd.encoding= encodingmap.get(argstring3.getLiteralValue().toUpperCase());
				nd.replace= true;
				nd.visited= argstring3;
				holder.put(visited, nd);
				operations.add(fixcore.rewrite(visited, cb, holder));
				break;
			case 2:
				Nodedata nd2= new Nodedata();
				nd2.encoding= "UTF_8"; //$NON-NLS-1$
				nd2.replace= false;
				nd2.visited= visited;
				holder.put(visited, nd2);
				operations.add(fixcore.rewrite(visited, cb, holder));
				break;
			default:
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
		 * Add StandardCharsets.UTF_8 as third (last) parameter
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
			return "Properties p=new Properties();\n" + //$NON-NLS-1$
					"p.storeToXML(java.io.OutputStream,String,StandardCharsets.UTF_8);\n"; //$NON-NLS-1$
		}
		return "Properties p=new Properties();\n" + //$NON-NLS-1$
				"p.storeToXML(java.io.OutputStream,String,\"UTF-8\");\n"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "Properties.storeToXML()"; //$NON-NLS-1$
	}
}
