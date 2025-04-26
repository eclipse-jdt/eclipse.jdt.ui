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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.common.ReferenceHolder;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseExplicitEncodingFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * @param <T> Type found in Visitor
 */
public abstract class AbstractExplicitEncoding<T extends ASTNode> {
	private static final String JAVA_IO_UNSUPPORTED_ENCODING_EXCEPTION= "java.io.UnsupportedEncodingException"; //$NON-NLS-1$

	private static final String UNSUPPORTED_ENCODING_EXCEPTION= "UnsupportedEncodingException"; //$NON-NLS-1$

	static Map<String, String> encodingmap= Map.of(
			"UTF-8", "UTF_8", //$NON-NLS-1$ //$NON-NLS-2$
			"UTF-16", "UTF_16", //$NON-NLS-1$ //$NON-NLS-2$
			"UTF-16BE", "UTF_16BE", //$NON-NLS-1$ //$NON-NLS-2$
			"UTF-16LE", "UTF_16LE", //$NON-NLS-1$ //$NON-NLS-2$
			"ISO-8859-1", "ISO_8859_1", //$NON-NLS-1$ //$NON-NLS-2$
			"US-ASCII", "US_ASCII" //$NON-NLS-1$ //$NON-NLS-2$
	);

	static Set<String> encodings= encodingmap.keySet();

	static class Nodedata {
		public boolean replace;

		public ASTNode visited;

		public String encoding;

		public static Map<String, QualifiedName> charsetConstants= new HashMap<>();
	}

	protected static final String ENCODING= "encoding"; //$NON-NLS-1$

	protected static final String REPLACE= "replace"; //$NON-NLS-1$

	public abstract void find(UseExplicitEncodingFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, ChangeBehavior cb);

	public abstract void rewrite(UseExplicitEncodingFixCore useExplicitEncodingFixCore, T visited, CompilationUnitRewrite cuRewrite,
			TextEditGroup group, ChangeBehavior cb, ReferenceHolder<ASTNode, Object> data);

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

	protected static String findVariableValue(SimpleName variable, ASTNode context) {
		ASTNode current= context.getParent();
		while (current != null && !(current instanceof MethodDeclaration) && !(current instanceof TypeDeclaration)) {
			current= current.getParent();
		}

		if (current instanceof MethodDeclaration) {
			MethodDeclaration method= (MethodDeclaration) current;
			List<?> statements= method.getBody().statements();

			for (Object stmt : statements) {
				if (stmt instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDeclStmt= (VariableDeclarationStatement) stmt;
					for (Object frag : varDeclStmt.fragments()) {
						VariableDeclarationFragment fragment= (VariableDeclarationFragment) frag;
						if (fragment.getName().getIdentifier().equals(variable.getIdentifier())) {
							Expression initializer= fragment.getInitializer();
							if (initializer instanceof StringLiteral) {
								return ((StringLiteral) initializer).getLiteralValue().toUpperCase();
							}
						}
					}
				}
			}
		} else if (current instanceof TypeDeclaration) {
			TypeDeclaration type= (TypeDeclaration) current;
			FieldDeclaration[] fields= type.getFields();

			for (FieldDeclaration field : fields) {
				for (Object frag : field.fragments()) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment) frag;
					if (fragment.getName().getIdentifier().equals(variable.getIdentifier())) {
						Expression initializer= fragment.getInitializer();
						if (initializer instanceof StringLiteral) {
							return ((StringLiteral) initializer).getLiteralValue().toUpperCase();
						}
					}
				}
			}
		}
		return null;
	}

	public abstract String getPreview(boolean afterRefactoring, ChangeBehavior cb);

	protected void removeUnsupportedEncodingException(final ASTNode visited, TextEditGroup group, ASTRewrite rewrite, ImportRewrite importRewriter) {
		ASTNode parent= visited.getParent();
		while (parent != null && !(parent instanceof MethodDeclaration) && !(parent instanceof TryStatement)) {
			parent= parent.getParent();
		}

		if (parent instanceof MethodDeclaration) {
			MethodDeclaration method= (MethodDeclaration) parent;
			ListRewrite throwsRewrite= rewrite.getListRewrite(method, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
			List<Type> thrownExceptions= method.thrownExceptionTypes();
			for (Type exceptionType : thrownExceptions) {
				if (exceptionType.toString().equals(UNSUPPORTED_ENCODING_EXCEPTION)) {
					throwsRewrite.remove(exceptionType, group);
					importRewriter.removeImport(JAVA_IO_UNSUPPORTED_ENCODING_EXCEPTION);
				}
			}
		} else if (parent instanceof TryStatement) {
			TryStatement tryStatement= (TryStatement) parent;

			List<CatchClause> catchClauses= tryStatement.catchClauses();
			for (CatchClause catchClause : catchClauses) {
				SingleVariableDeclaration exception= catchClause.getException();
				Type exceptionType= exception.getType();

				if (exceptionType instanceof UnionType) {
					UnionType unionType= (UnionType) exceptionType;
					ListRewrite unionRewrite= rewrite.getListRewrite(unionType, UnionType.TYPES_PROPERTY);

					List<Type> types= unionType.types();
					types.stream()
							.filter(type -> type.toString().equals(UNSUPPORTED_ENCODING_EXCEPTION))
							.forEach(type -> unionRewrite.remove(type, group));

					if (types.size() == 1) {
						rewrite.replace(unionType, types.get(0), group);
					} else if (types.isEmpty()) {
						rewrite.remove(catchClause, group);
					}
				} else if (exceptionType.toString().equals(UNSUPPORTED_ENCODING_EXCEPTION)) {
					rewrite.remove(catchClause, group);
					importRewriter.removeImport(JAVA_IO_UNSUPPORTED_ENCODING_EXCEPTION);
				}
			}

			if (tryStatement.catchClauses().isEmpty() && tryStatement.getFinally() == null) {
				Block tryBlock= tryStatement.getBody();

				if (tryStatement.resources().isEmpty() && tryBlock.statements().isEmpty()) {
					rewrite.remove(tryStatement, group);
				} else if (tryStatement.resources().isEmpty()) {
					rewrite.replace(tryStatement, tryBlock, group);
				}
			}
		}
	}

}
