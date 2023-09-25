/*******************************************************************************
 * Copyright (c) 2012, 2018 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * Fix for field related null-issues:
 * <ol>
 * <li>{@link IProblem#NullableFieldReference}</li>
 * <li>{@link IProblem#RequiredNonNullButProvidedSpecdNullable} <em>if relating to a field</em></li>
 * <li>{@link IProblem#RequiredNonNullButProvidedUnknown} <em>if relating to a field</em></li>
 * </ol>
 * Extract the field reference to a fresh local variable.
 * Add a null check for that local variable and move
 * the dereference into the then-block of this null-check:
 * <pre>
 * {@code @Nullable Exception e;}
 * void test() {
 *     e.printStackTrace();
 * }</pre>
 * will be converted to:
 * <pre>
 * {@code @Nullable Exception e;}
 * void test() {
 *     final Exception e2 = e;
 *     if (e2 != null) {
 *         e2.printStackTrace();
 *     } else {
 *         // TODO handle null value
 *     }
 * }</pre>
 * <p>
 * The <code>final</code> keyword is added to remind the user that writing
 * to the local variable has no effect on the original field.</p>
 * <p>Rrespects scoping if the problem occurs inside the initialization
 * of a local variable (by moving statements into the new then block).</p>
 *
 * @since 3.9
 */
public class ExtractToNullCheckedLocalProposal extends LinkedCorrectionProposal {

	public ExtractToNullCheckedLocalProposal(ICompilationUnit cu, CompilationUnit compilationUnit, SimpleName fieldReference, ASTNode enclosingMethod) {
		super(FixMessages.ExtractToNullCheckedLocalProposal_extractToCheckedLocal_proposalName, cu, null, 100, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		setDelegate(new ExtractToNullCheckedLocalProposalCore(cu, compilationUnit, fieldReference, enclosingMethod));
	}
}