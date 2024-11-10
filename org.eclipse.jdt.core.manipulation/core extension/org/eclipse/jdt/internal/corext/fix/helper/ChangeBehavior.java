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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public enum ChangeBehavior {
	KEEP_BEHAVIOR() {
		@Override
		protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset) {
			Expression callToCharsetDefaultCharset= null;

			if (charset != null) {
				callToCharsetDefaultCharset= addCharsetUTF8(cuRewrite, ast, charset);
			} else {
				// needs Java 1.5
				callToCharsetDefaultCharset= addCharsetComputation(cuRewrite, ast);
			}

			return callToCharsetDefaultCharset;
		}

		@Override
		protected String computeCharsetforPreview() {
			String insert= ""; //$NON-NLS-1$
			insert= "Charset.defaultCharset()"; //$NON-NLS-1$
			return insert;
		}
	},
	ENFORCE_UTF8() {
		@Override
		protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset) {
			Expression callToCharsetDefaultCharset= null;
			callToCharsetDefaultCharset= addCharsetUTF8(cuRewrite, ast, charset);
			return callToCharsetDefaultCharset;
		}

		@Override
		protected String computeCharsetforPreview() {
			String insert= ""; //$NON-NLS-1$
			insert= "StandardCharsets.UTF_8"; //$NON-NLS-1$
			return insert;
		}
	},
	ENFORCE_UTF8_AGGREGATE() {
		@Override
		protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset) {
			Expression callToCharsetDefaultCharset= null;
			/**
			 * @TODO not implemented
			 */
			return callToCharsetDefaultCharset;
		}

		@Override
		protected String computeCharsetforPreview() {
			String insert= ""; //$NON-NLS-1$
			//				insert="charset_constant"; //$NON-NLS-1$
			return insert;
		}
	};


	abstract protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset);

	abstract protected String computeCharsetforPreview();

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
	protected MethodInvocation addCharsetStringComputation(final CompilationUnitRewrite cuRewrite, AST ast, ChangeBehavior cb, String charset) {
		Expression callToCharsetDefaultCharset= computeCharsetASTNode(cuRewrite, ast, charset);
		/**
		 * Add second call to Charset.defaultCharset().displayName()
		 */
		MethodInvocation secondCall= ast.newMethodInvocation();
		secondCall.setExpression(callToCharsetDefaultCharset);
		secondCall.setName(ast.newSimpleName(METHOD_DISPLAY_NAME));
		return secondCall;
	}
}
