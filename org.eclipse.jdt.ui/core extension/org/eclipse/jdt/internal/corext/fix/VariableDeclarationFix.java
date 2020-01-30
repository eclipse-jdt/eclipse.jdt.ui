/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Chris West (Faux) <eclipse@goeswhere.com> - [clean up] "Use modifier 'final' where possible" can introduce compile errors - https://bugs.eclipse.org/bugs/show_bug.cgi?id=272532
 *     Red Hat Inc. - modified to use VariableDelcarationFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class VariableDeclarationFix extends CompilationUnitRewriteOperationsFix {

	public static VariableDeclarationFix createChangeModifierToFinalFix(final CompilationUnit compilationUnit, ASTNode[] selectedNodes) {
		HashMap<IBinding, List<SimpleName>> writtenNames= new HashMap<>();
		VariableDeclarationFixCore.WrittenNamesFinder finder= new VariableDeclarationFixCore.WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);
		List<VariableDeclarationFixCore.ModifierChangeOperation> ops= new ArrayList<>();
		VariableDeclarationFixCore.VariableDeclarationFinder visitor= new VariableDeclarationFixCore.VariableDeclarationFinder(true, true, true, ops, writtenNames);
		if (selectedNodes.length == 1) {
			selectedNodes[0].accept(visitor);
		} else {
			for (ASTNode selectedNode : selectedNodes) {
				selectedNode.accept(visitor);
			}
		}
		if (ops.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] result= ops.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[ops.size()]);
		String label;
		if (result.length == 1) {
			label= FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description;
		} else {
			label= FixMessages.VariableDeclarationFix_ChangeMidifiersToFinalWherPossible_description;
		}
		return new VariableDeclarationFix(label, compilationUnit, result);
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean addFinalFields, boolean addFinalParameters, boolean addFinalLocals) {

		if (!addFinalFields && !addFinalParameters && !addFinalLocals)
			return null;

		HashMap<IBinding, List<SimpleName>> writtenNames= new HashMap<>();
		VariableDeclarationFixCore.WrittenNamesFinder finder= new VariableDeclarationFixCore.WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);

		List<VariableDeclarationFixCore.ModifierChangeOperation> operations= new ArrayList<>();
		VariableDeclarationFixCore.VariableDeclarationFinder visitor= new VariableDeclarationFixCore.VariableDeclarationFinder(addFinalFields, addFinalParameters, addFinalLocals, operations, writtenNames);
		compilationUnit.accept(visitor);

		if (operations.isEmpty())
			return null;

		return new VariableDeclarationFix(FixMessages.VariableDeclarationFix_add_final_change_name, compilationUnit, operations.toArray(new CompilationUnitRewriteOperation[operations.size()]));
	}


	protected VariableDeclarationFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
