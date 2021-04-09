/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.fix;

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.FIELD_PATH_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.FIELD_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_BOOLEAN;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_DEFAULT_CHARSET;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_DISPLAY_NAME;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_GET_DEFAULT;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_GET_PROPERTY;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_GET_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_LINE_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.METHOD_PARSEBOOLEAN;
import static org.eclipse.jdt.internal.ui.fix.MultiFixMessages.ConstantsCleanUp_description;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

public enum UpdateProperty {
	/**
	 * After change -Dfile.encoding=xxx is no longer taken into account
	 * setting encoding -Dfile.encoding="UTF-8" is not official supported anyway as it is
	 * a "read only" property similar as -Dsun.jnu.encoding="UTF-8" is not supported officially
	 *
	 * Change
	 * <code>System.getProperty("file.encoding");</code>
	 * to
	 * <code>Charset.defaultCharset().displayName();</code> since Java 1.5
	 *
	 */
	FILE_ENCODING("file.encoding", //$NON-NLS-1$
			Charset.class,
			METHOD_DEFAULT_CHARSET,
			METHOD_DISPLAY_NAME,
			null,
			null,
			UpdateProperty::defaultvisitor,
			UpdateProperty::defaultRewrite),
	/**
	 * After change -Dpath.separator=xxx is no longer taken into account
	 *
	 * Change
	 * <code>System.getProperty("path.separator");</code>
	 * to
	 * <code>File.pathSeparator;</code>
	 *
	 */
	PATH_SEPARATOR("path.separator", //$NON-NLS-1$
			null,
			null,
			null,
			File.class,
			FIELD_PATH_SEPARATOR,
			UpdateProperty::defaultvisitor,
			UpdateProperty::pathRewrite),
	/**
	 * After change -Dfile.separator=xxx is no longer taken into account
	 *
	 * Change
	 * <code>System.getProperty("file.separator");</code>
	 * to
	 * <code>FileSystems.getDefault().getSeparator();</code> introduced in Java 1.7
	 * fallback
	 * <code>File.separator;</code>
	 *
	 * see system property {@code java.nio.file.spi.DefaultFileSystemProvider} to set default file system
	 */
	FILE_SEPARATOR("file.separator", //$NON-NLS-1$
			FileSystems.class,
			METHOD_GET_DEFAULT,
			METHOD_GET_SEPARATOR,
			File.class,
			FIELD_SEPARATOR,
			UpdateProperty::defaultvisitor,
			UpdateProperty::defaultRewrite),
	/**
	 * Change
	 * <code>System.getProperty("line.separator");</code>
	 * to
	 * <code>System.lineSeparator();</code>
	 */
	LINE_SEPARATOR("line.separator", //$NON-NLS-1$
			System.class,
			METHOD_LINE_SEPARATOR,
			null,
			null,
			null,
			UpdateProperty::defaultvisitor,
			UpdateProperty::defaultRewrite),
	/**
	 * Change
	 * <code>Boolean.parseBoolean(System.getProperty("arbitrarykey"));</code>
	 * to
	 * <code>Boolean.getBoolean("arbitrarykey");</code>
	 *
	 * Currently <code>Boolean.parseBoolean(System.getProperty("arbitrarykey", "false"))</code> is not supported
	 */
	BOOLEAN_PROPERTY(null,
			null,
			METHOD_BOOLEAN,
			null,
			Boolean.class,
			null,
			UpdateProperty::parseboolean_visitor,
			UpdateProperty::booleanRewrite);

	String key;
	Class<?> cl;
	String simplename;
	String simplename2;
	Class<?> alternativecl;
	String constant;
	IFinder myfinder;
	IRewriter myrewriter;

	UpdateProperty(String key, Class<?> cl, String simplename, String simplename2, Class<?> alternativecl, String constant,
			IFinder myfinder, IRewriter myrewriter) {
		this.key= key;
		this.cl= cl;
		this.simplename= simplename;
		this.simplename2= simplename2;
		this.alternativecl= alternativecl;
		this.constant= constant;
		this.myfinder= myfinder;
		this.myrewriter= myrewriter;
	}

	static void defaultvisitor(final UpdateProperty upp, final CompilationUnit compilationUnit,final Set<CompilationUnitRewriteOperation> operations,final Set<ASTNode> nodesprocessed) {
		compilationUnit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodInvocation visited) {
				if(nodesprocessed.contains(visited)) {
					return false;
				}
				/**
				 * look for
				 * <code>System.getProperty("file.encoding");</code>
				 * <code>System.getProperty("path.separator");</code>
				 * <code>System.getProperty("file.separator");</code>
				 * <code>System.getProperty("line.separator");</code>
				 *
				 */
				if (ASTNodes.usesGivenSignature(visited, System.class.getCanonicalName(), METHOD_GET_PROPERTY, String.class.getCanonicalName())) {
					Expression expression= (Expression) visited.arguments().get(0);
					Object propertykey= expression.resolveConstantExpressionValue();
					if (propertykey instanceof String && upp.key.equals(propertykey)) {
						operations.add(upp.rewrite(visited, null, expression));
						nodesprocessed.add(visited);
						return false;
					}
				}
				return true;
			}
		});
	}

	static void parseboolean_visitor(final UpdateProperty upp, final CompilationUnit compilationUnit, final Set<CompilationUnitRewriteOperation> operations, final Set<ASTNode> nodesprocessed) {
		compilationUnit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodInvocation visited) {
				if(nodesprocessed.contains(visited)) {
					return false;
				}
				/**
				 * look for
				 * <code>Boolean.parseBoolean(System.getProperty("arbitrarykey"));</code>
				 * (has to be done after completing the specific search)
				 */
				if (ASTNodes.usesGivenSignature(visited, System.class.getCanonicalName(), METHOD_GET_PROPERTY, String.class.getCanonicalName())) {
					Expression expression= (Expression) visited.arguments().get(0);
					Object propertykey= expression.resolveConstantExpressionValue();
					if (propertykey instanceof String && visited.getParent() instanceof MethodInvocation) {
						MethodInvocation parent=(MethodInvocation) visited.getParent();
						if (ASTNodes.usesGivenSignature(parent, Boolean.class.getCanonicalName(), METHOD_PARSEBOOLEAN, String.class.getCanonicalName())) {
							operations.add(upp.rewrite(parent, (String) propertykey,expression));
							nodesprocessed.add(visited);
							return false;
						}
					}
				}
				return true;
			}
		});
	}

	interface IRewriter {
		void computeRewriter(final UpdateProperty upp,final MethodInvocation visited, final String propertykey,
				final Expression expression, final CompilationUnitRewrite cuRewrite, final TextEditGroup group) throws CoreException;
	}

	CompilationUnitRewriteOperation rewrite(final MethodInvocation visited, final String propertykey, final Expression expression) {
		return new CompilationUnitRewriteOperation() {
			@Override
			public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
				TextEditGroup group= createTextEditGroup(Messages.format(ConstantsCleanUp_description,UpdateProperty.this.toString()), cuRewrite);
				cuRewrite.getASTRewrite().setTargetSourceRangeComputer(computer);
				myrewriter.computeRewriter(UpdateProperty.this, visited, propertykey, expression, cuRewrite, group);
			}
		};
	}

	private static void booleanRewrite(UpdateProperty upp,final MethodInvocation visited, final String propertykey, Expression expression, final CompilationUnitRewrite cuRewrite,
			TextEditGroup group) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		/**
		 * Add import
		 */
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		importRewrite.addImport(upp.alternativecl.getCanonicalName());
		/**
		 * Add field access
		 */
		MethodInvocation newMethodInvocation= ast.newMethodInvocation();
		newMethodInvocation.setExpression(ASTNodeFactory.newName(ast, upp.alternativecl.getSimpleName()));
		newMethodInvocation.setName(ast.newSimpleName(upp.simplename));
		newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(cuRewrite.getASTRewrite(), ASTNodes.getUnparenthesedExpression(expression)));
		ASTNode replace_with_Call= newMethodInvocation;
		ASTNodes.replaceButKeepComment(rewrite, visited, replace_with_Call, group);
	}

	private static void defaultRewrite(UpdateProperty upp, final MethodInvocation visited, final String propertykey, Expression expression,
			final CompilationUnitRewrite cuRewrite,final TextEditGroup group) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		ASTNode replace_with_Call;
		if (JavaModelUtil.is1d7OrHigher(cuRewrite.getCu().getJavaProject())) {

			/**
			 * Add import
			 */
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			importRewrite.addImport(upp.cl.getCanonicalName());
			/**
			 * Add first method call
			 */
			MethodInvocation firstCall= ast.newMethodInvocation();
			firstCall.setExpression(ASTNodeFactory.newName(ast, upp.cl.getSimpleName()));
			firstCall.setName(ast.newSimpleName(upp.simplename));

			if(upp.simplename2==null) {
				replace_with_Call= firstCall;
			} else {
				/**
				 * Add second method call
				 */
				MethodInvocation secondCall= ast.newMethodInvocation();
				secondCall.setExpression(firstCall);
				secondCall.setName(ast.newSimpleName(upp.simplename2));
				replace_with_Call= secondCall;
			}
		} else {
			if(upp.alternativecl==null) {
				/**
				 * can be null for System.getProperty("file.encoding") on Java < 7
				 */
				return;
			} else {
				/**
				 * fallback to File.pathSeparator for java < 7.0
				 */

				/**
				 * Add import
				 */
				ImportRewrite importRewrite= cuRewrite.getImportRewrite();
				importRewrite.addImport(upp.alternativecl.getCanonicalName());
				/**
				 * Add field access
				 */
				FieldAccess fieldaccess= ast.newFieldAccess();
				fieldaccess.setExpression(ASTNodeFactory.newName(ast, upp.alternativecl.getSimpleName()));
				fieldaccess.setName(ast.newSimpleName(upp.constant));
				replace_with_Call= fieldaccess;
			}
		}
		ASTNodes.replaceAndRemoveNLS(rewrite, visited, replace_with_Call, group, cuRewrite);
	}

	private static void pathRewrite(UpdateProperty upp, final MethodInvocation visited, final String propertykey, Expression expression,
			final CompilationUnitRewrite cuRewrite,final TextEditGroup group) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		ASTNode replace_with_Call;
		/**
		 * Add import
		 */
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		importRewrite.addImport(upp.alternativecl.getCanonicalName());
		/**
		 * Add field access
		 */
		FieldAccess fieldaccess= ast.newFieldAccess();
		fieldaccess.setExpression(ASTNodeFactory.newName(ast, upp.alternativecl.getSimpleName()));
		fieldaccess.setName(ast.newSimpleName(upp.constant));
		replace_with_Call= fieldaccess;
		ASTNodes.replaceAndRemoveNLS(rewrite, visited, replace_with_Call, group, cuRewrite);
	}

	interface IFinder {
		void finder(final UpdateProperty upp, final CompilationUnit compilationUnit,final  Set<CompilationUnitRewriteOperation> operations,final  Set<ASTNode> nodesprocessed);
	}

	/**
	 * Compute set of CompilationUnitRewriteOperation to refactor supported system property to method calls
	 *
	 * @param compilationUnit unit to search for System.getProperty(xxx) calls
	 * @param operations set of all CompilationUnitRewriteOperations created already
	 * @param nodesprocessed list to remember nodes already processed
	 */
	public void findOperations(final CompilationUnit compilationUnit,final Set<CompilationUnitRewriteOperation> operations,final Set<ASTNode> nodesprocessed) {
		myfinder.finder(this, compilationUnit, operations, nodesprocessed);
	}

	final static TargetSourceRangeComputer computer= new TargetSourceRangeComputer() {
		@Override
		public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
			if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
				return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
			}
			return super.computeSourceRange(nodeWithComment);
		}
	};
}
