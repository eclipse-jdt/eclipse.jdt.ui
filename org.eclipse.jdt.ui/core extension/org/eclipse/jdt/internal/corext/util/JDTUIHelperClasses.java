/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.DimensionRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.ReplaceRewrite;
import org.eclipse.jdt.internal.corext.dom.StatementRewrite;
import org.eclipse.jdt.internal.corext.dom.TypeAnnotationRewrite;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.dom.VariableDeclarationRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

/**
 * The org.eclipse.jdt.ui bundle contains a few internal helper classes that simplify
 * common tasks when dealing with JDT Core or UI APIs. With bug 508777, many of these
 * classes have been moved to the org.eclipse.jdt.core.manipulation bundle. We've kept
 * the original package/class names to reduce binary compatibility problems for bundles
 * that illegally accessed these classes.
 * <p>
 * Some classes had to be split in two. They are listed with fully-qualified names here.
 * Back-links in Javadoc from classes in org.eclipse.jdt.core.manipulation to this
 * class are not possible, so we use line comments there: // @see JDTUIHelperClasses
 * </p>
 * <p>
 * New helpers preferably go into the o.e.jdt.core.manipulation bundle.
 * </p>
 *
 * Here's a list of the most important helper classes:
 *
 * <h2>Java Model</h2>
 * <p>
 * APIs in {@link org.eclipse.jdt.core}.
 * </p>
 *
 * <p>
 * Static helper methods for analysis in {@link org.eclipse.jdt.internal.corext.util} and elsewhere:
 * </p>
 * <ul>
 * <li>{@link JavaModelUtil}</li>
 * <li>{@link JavaElementUtil}</li>
 * <li>{@link JdtFlags}</li>
 * <li>{@link JavaConventionsUtil}</li>
 * <li>{@link MethodOverrideTester}</li>
 * <li>{@link SuperTypeHierarchyCache}</li>
 * </ul>
 *
 * <p>
 * Static helper methods for stubs creation:
 * </p>
 * <ul>
 * <li>{@link StubUtility}</li>
 * </ul>
 *
 *
 * <h2>DOM AST</h2>
 * <p>
 * APIs in {@link org.eclipse.jdt.core.dom} and {@link org.eclipse.jdt.core.dom.rewrite}.<br>
 * Core API classes that are easy to miss: {@link NodeFinder}, {@link ASTVisitor}, {@link ASTMatcher}.
 * </p>
 *
 * <p>
 * Static helper methods for analysis:
 * </p>
 * <ul>
 * <li>{@link ASTNodes}</li>
 * <li>{@link ASTNodeSearchUtil}</li>
 * <li>{@link org.eclipse.jdt.internal.ui.text.correction.ASTResolving}</li>
 * <li>{@link org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving}</li>
 * <li>{@link Bindings}</li>
 * <li>{@link TypeRules}</li>
 * </ul>
 *
 * <p>
 * Static helper methods for node/stubs creation:
 * </p>
 * <ul>
 * <li>{@link ASTNodeFactory}</li>
 * <li>{@link StubUtility2}</li>
 * </ul>
 *
 * <p>
 * Helper classes in {@link org.eclipse.jdt.internal.corext.dom}, e.g.:
 * </p>
 * <ul>
 * <li>{@link GenericVisitor}</li>
 * <li>{@link HierarchicalASTVisitor}</li>
 * <li>{@link NecessaryParenthesesChecker}</li>
 * </ul>
 *
 * <p>
 * Helper classes for {@link ASTRewrite}:
 * </p>
 * <ul>
 * <li>{@link CompilationUnitRewrite}</li>
 * <li>{@link BodyDeclarationRewrite}</li>
 * <li>{@link DimensionRewrite}</li>
 * <li>{@link ModifierRewrite}</li>
 * <li>{@link ReplaceRewrite}</li>
 * <li>{@link StatementRewrite}</li>
 * <li>{@link TypeAnnotationRewrite}</li>
 * <li>{@link VariableDeclarationRewrite}</li>
 * </ul>
 *
 * <p>
 * Label and text manipulation helpers:
 * </p>
 * <ul>
 * <li>{@link org.eclipse.jdt.internal.corext.util.Strings}</li>
 * <li>{@link org.eclipse.jdt.internal.core.manipulation.util.Strings}</li>
 * <li>{@link org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels}</li>
 * <li>{@link org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels}</li>
 * </ul>
 *
 * @noreference This class is not intended to be referenced by clients
 */
public final class JDTUIHelperClasses {
	private JDTUIHelperClasses() {
		// no instances
	}
}
