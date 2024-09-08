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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public enum ChangeBehavior {
	KEEP_BEHAVIOR() {
		@Override
		protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset, Map<String, QualifiedName> charsetConstants) {
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
			String insert= "Charset.defaultCharset()"; //$NON-NLS-1$
			return insert;
		}
	},
	ENFORCE_UTF8() {
		@Override
		protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset, Map<String, QualifiedName> charsetConstants) {
			String charset2= charset == null ? "UTF_8" : charset; //$NON-NLS-1$
			Expression callToCharsetDefaultCharset= addCharsetUTF8(cuRewrite, ast, charset2);
			return callToCharsetDefaultCharset;
		}

		@Override
		protected String computeCharsetforPreview() {
			String insert= "StandardCharsets.UTF_8"; //$NON-NLS-1$
			return insert;
		}
	},
	ENFORCE_UTF8_AGGREGATE() {
		@Override
		protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset2, Map<String, QualifiedName> charsetConstants) {
			String charset= charset2 == null ? "UTF_8" : charset2; //$NON-NLS-1$
			// Generate a valid Java identifier for the charset name (e.g., UTF_8)
		    String fieldName = charset.toUpperCase().replace('-', '_');

		    // Check if this charset constant is already stored in the map
		    if (charsetConstants.containsKey(fieldName)) {
		        return charsetConstants.get(fieldName);
		    }

		    // Add import for StandardCharsets
		    ImportRewrite importRewrite = cuRewrite.getImportRewrite();
		    importRewrite.addImport(StandardCharsets.class.getCanonicalName());
		    importRewrite.addImport(Charset.class.getCanonicalName());

		    // Check if the static field already exists in the class
		    TypeDeclaration enclosingType = (TypeDeclaration) cuRewrite.getRoot().types().get(0);
		    FieldDeclaration existingField = findStaticCharsetField(enclosingType, fieldName);

		    QualifiedName fieldReference;
		    if (existingField == null) {
		        // Create a new static field if it doesn't exist
		        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		        fragment.setName(ast.newSimpleName(fieldName));
		        fragment.setInitializer(createCharsetAccessExpression(ast, charset));

		        FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
		        fieldDeclaration.setType(ast.newSimpleType(ast.newName("Charset"))); //$NON-NLS-1$
		        fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		        fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		        fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));

		        // Add the new field to the class
		        cuRewrite.getASTRewrite().getListRewrite(enclosingType, TypeDeclaration.BODY_DECLARATIONS_PROPERTY)
		            .insertFirst(fieldDeclaration, null);

		        // Create a QualifiedName to refer to this new field
		        fieldReference = ast.newQualifiedName(
		        	    ast.newSimpleName(enclosingType.getName().getIdentifier()),
		        	    ast.newSimpleName(fragment.getName().getIdentifier())
		        	);
		    } else {
		        // If the field already exists, find its reference name
		        VariableDeclarationFragment fragment = (VariableDeclarationFragment) existingField.fragments().get(0);
		        fieldReference = ast.newQualifiedName(
		            ast.newSimpleName(enclosingType.getName().getIdentifier()),
		            fragment.getName()
		        );
		    }

		    // Cache the field reference in the map and return it
		    charsetConstants.put(fieldName, fieldReference);
		    return fieldReference;
		}

		@Override
		protected String computeCharsetforPreview() {
			return "CharsetConstant"; //$NON-NLS-1$
		}
	};

	abstract protected Expression computeCharsetASTNode(final CompilationUnitRewrite cuRewrite, AST ast, String charset, Map<String, QualifiedName> charsetConstants);

	abstract protected String computeCharsetforPreview();

	protected FieldDeclaration findStaticCharsetField(TypeDeclaration type, String fieldName) {
	    for (FieldDeclaration field : type.getFields()) {
	        for (Object fragment : field.fragments()) {
	            if (fragment instanceof VariableDeclarationFragment) {
	                VariableDeclarationFragment varFrag = (VariableDeclarationFragment) fragment;
	                if (varFrag.getName().getIdentifier().equals(fieldName)) {
	                    return field;
	                }
	            }
	        }
	    }
	    return null;
	}

	protected Expression createCharsetAccessExpression(AST ast, String charset) {
	    FieldAccess fieldAccess = ast.newFieldAccess();
	    fieldAccess.setExpression(ast.newName(StandardCharsets.class.getSimpleName()));
	    fieldAccess.setName(ast.newSimpleName(charset));
	    return fieldAccess;
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
	protected MethodInvocation addCharsetStringComputation(final CompilationUnitRewrite cuRewrite, AST ast, ChangeBehavior cb, String charset, Map<String, QualifiedName> charsetConstants) {
		Expression callToCharsetDefaultCharset= computeCharsetASTNode(cuRewrite, ast, charset, charsetConstants);
		/**
		 * Add second call to Charset.defaultCharset().displayName()
		 */
		MethodInvocation secondCall= ast.newMethodInvocation();
		secondCall.setExpression(callToCharsetDefaultCharset);
		secondCall.setName(ast.newSimpleName(METHOD_DISPLAY_NAME));
		return secondCall;
	}
}
