/*******************************************************************************
 * Copyright (c) 2022 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.corrections.proposals;

/**
 * Code action kind constants used with {@link ASTRewriteCorrectionProposal}.
 * Most of kinds are aligned with Language Server Protocol, but not limited to
 * it.
 */
public final class CodeActionKind {
    /**
     * Base kind for refactoring rewrite actions: 'refactor.rewrite'
     *
     * Example rewrite actions:
     *
     * - Convert JavaScript function to class - Add or remove parameter -
     * Encapsulate field - Make method static - Move method to base class - ...
     */
    public static final String RefactorRewrite = "refactor.rewrite";
}
