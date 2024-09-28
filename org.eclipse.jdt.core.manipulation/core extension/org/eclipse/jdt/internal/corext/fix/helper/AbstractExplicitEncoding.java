/*******************************************************************************
 * Copyright (c) 2021 Carsten Hammer.
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

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_DEFAULT_CHARSET;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_DISPLAY_NAME;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.common.ReferenceHolder;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * @param <T> Type found in Visitor
 */
public abstract class AbstractExplicitEncoding<T extends ASTNode> {
	static Map<String, String> encodingmap = Map.of(
		    "UTF-8", "UTF_8", //$NON-NLS-1$ //$NON-NLS-2$
		    "UTF-16", "UTF_16", //$NON-NLS-1$ //$NON-NLS-2$
		    "UTF-16BE", "UTF_16BE", //$NON-NLS-1$ //$NON-NLS-2$
		    "UTF-16LE", "UTF_16LE", //$NON-NLS-1$ //$NON-NLS-2$
		    "ISO-8859-1", "ISO_8859_1", //$NON-NLS-1$ //$NON-NLS-2$
		    "US-ASCII", "US_ASCII" //$NON-NLS-1$ //$NON-NLS-2$
		);
	static Set<String> encodings=encodingmap.keySet();
	public enum ChangeBehavior {KEEP, USE_UTF8, USE_UTF8_AGGREGATE}

	static class Nodedata {
		public boolean replace;
		public ASTNode visited;
		public String encoding;
	}


	protected static final String ENCODING = "encoding"; //$NON-NLS-1$
	protected static final String REPLACE = "replace"; //$NON-NLS-1$

	public abstract void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb);


	public abstract void rewrite(UseExplicitEncodingFixCore useExplicitEncodingFixCore, T visited, CompilationUnitRewrite cuRewrite,
			TextEditGroup group, ChangeBehavior cb, ReferenceHolder<ASTNode, Object> data);

	protected static Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, ChangeBehavior cb, String charset) {
		Expression callToCharsetDefaultCharset=null;
		switch(cb) {
		case KEEP:
			if(charset!=null) {
				callToCharsetDefaultCharset= addCharsetUTF8(cuRewrite, ast,charset);
			} else {
			// needs Java 1.5
				callToCharsetDefaultCharset= addCharsetComputation(cuRewrite, ast);
			}
			break;
		case USE_UTF8_AGGREGATE:
			/**
			 * @TODO not implemented
			 */
		case USE_UTF8:
			// needs Java 1.7
			callToCharsetDefaultCharset= addCharsetUTF8(cuRewrite, ast,charset);
			break;
		}
		return callToCharsetDefaultCharset;
	}

	protected static String computeCharsetforPreview(ChangeBehavior cb) {
		String insert=""; //$NON-NLS-1$
		switch(cb) {
		case KEEP:
			insert="Charset.defaultCharset()"; //$NON-NLS-1$
			break;
		case USE_UTF8_AGGREGATE:
			//				insert="charset_constant"; //$NON-NLS-1$
			//$FALL-THROUGH$
		case USE_UTF8:
			insert="StandardCharsets.UTF_8"; //$NON-NLS-1$
			break;
		}
		return insert;
	}

	/**
	 * Create access to StandardCharsets.UTF_8, needs Java 1.7 or newer
	 *
	 * @param cuRewrite CompilationUnitRewrite
	 * @param ast AST
	 * @param charset Charset as String
	 * @return FieldAccess that returns Charset for UTF_8
	 */
	protected static FieldAccess addCharsetUTF8(CompilationUnitRewrite cuRewrite, AST ast, String charset) {
		/**
		 * Add import java.nio.charset.StandardCharsets - available since Java 1.7
		 */
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		importRewrite.addImport(StandardCharsets.class.getCanonicalName());
		/**
		 * Add field access to StandardCharsets.UTF_8
		 */
		FieldAccess fieldaccess= ast.newFieldAccess();
		fieldaccess.setExpression(ASTNodeFactory.newName(ast, StandardCharsets.class.getSimpleName()));

		fieldaccess.setName(ast.newSimpleName(charset));
		return fieldaccess;
	}
	/**
	 * Create call to Charset.defaultCharset(), needs Java 1.5 or newer
	 *
	 * @param cuRewrite CompilationUnitRewrite
	 * @param ast AST
	 * @return MethodInvocation that returns Charset for platform encoding
	 */
	protected static MethodInvocation addCharsetComputation(final CompilationUnitRewrite cuRewrite, AST ast) {
		/**
		 * Add import java.nio.charset.Charset
		 */
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		importRewrite.addImport(Charset.class.getCanonicalName());
		/**
		 * Add call to Charset.defaultCharset() - this is available since Java 1.5
		 */
		MethodInvocation firstCall= ast.newMethodInvocation();
		firstCall.setExpression(ASTNodeFactory.newName(ast, Charset.class.getSimpleName()));
		firstCall.setName(ast.newSimpleName(METHOD_DEFAULT_CHARSET));
		return firstCall;
	}

	/**
	 * Create call to Charset.defaultCharset().displayName(), needs Java 1.5 or newer
	 *
	 * @param cuRewrite CompilationUnitRewrite
	 * @param ast AST
	 * @param cb ChangeBehavior
	 * @param charset Charset as String
	 * @return MethodInvocation that returns String
	 */
	protected static MethodInvocation addCharsetStringComputation(final CompilationUnitRewrite cuRewrite, AST ast, ChangeBehavior cb, String charset) {
		Expression callToCharsetDefaultCharset= computeCharsetASTNode(cuRewrite, ast, cb, charset);
		/**
		 * Add second call to Charset.defaultCharset().displayName()
		 */
		MethodInvocation secondCall= ast.newMethodInvocation();
		secondCall.setExpression(callToCharsetDefaultCharset);
		secondCall.setName(ast.newSimpleName(METHOD_DISPLAY_NAME));
		return secondCall;
	}


	/**
	 * Adds an import to the class. This method should be used for every class reference added to
	 * the generated code.
	 *
	 * @param typeName a fully qualified name of a type
	 * @param cuRewrite CompilationUnitRewrite
	 * @param ast AST
	 * @return simple name of a class if the import was added and fully qualified name if there was
	 *         a conflict
	 */
	protected static Name addImport(String typeName, final CompilationUnitRewrite cuRewrite, AST ast) {
		String importedName= cuRewrite.getImportRewrite().addImport(typeName);
		return ast.newName(importedName);
	}


	public abstract String getPreview(boolean afterRefactoring, ChangeBehavior cb);
}
