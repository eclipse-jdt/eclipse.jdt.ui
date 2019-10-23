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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CodeStyleFixCore.AddThisQualifierOperation;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFixCore.ToStaticAccessOperation;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.CleanUpFixWrapper;
import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * A fix which fixes code style issues.
 */
public class CodeStyleFix extends CompilationUnitRewriteOperationsFix {


	public static CompilationUnitRewriteOperationsFix[] createNonStaticAccessFixes(CompilationUnit compilationUnit, IProblemLocation problem) {
		IProblemLocationCore problemLocation= (ProblemLocation)problem;
		if (!CodeStyleFixCore.isNonStaticAccess(problemLocation))
			return null;

		ToStaticAccessOperation operations[]= CodeStyleFixCore.createToStaticAccessOperations(compilationUnit, new HashMap<ASTNode, Block>(), problemLocation, false);
		if (operations == null)
			return null;

		String label1= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, operations[0].getAccessorName());
		CompilationUnitRewriteOperationsFix fix1= new CompilationUnitRewriteOperationsFix(label1, compilationUnit,
				new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {operations[0]});

		if (operations.length > 1) {
			String label2= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStaticUsingInstanceType_description, operations[1].getAccessorName());
			CompilationUnitRewriteOperationsFix fix2= new CompilationUnitRewriteOperationsFix(label2, compilationUnit,
					new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {operations[1]});
			return new CompilationUnitRewriteOperationsFix[] {fix1, fix2};
		}
		return new CompilationUnitRewriteOperationsFix[] {fix1};
	}

	public static CompilationUnitRewriteOperationsFix createAddFieldQualifierFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (IProblem.UnqualifiedFieldAccess != problem.getProblemId())
			return null;

		IProblemLocationCore problemLocation= (ProblemLocation)problem;
		AddThisQualifierOperation operation= CodeStyleFixCore.getUnqualifiedFieldAccessResolveOperation(compilationUnit, problemLocation);
		if (operation == null)
			return null;

		String groupName= operation.getDescription();
		return new CodeStyleFix(groupName, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {operation});
	}

	public static CompilationUnitRewriteOperationsFix createIndirectAccessToStaticFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		IProblemLocationCore problemLocation= (ProblemLocation)problem;
		if (!CodeStyleFixCore.isIndirectStaticAccess(problemLocation))
			return null;

		ToStaticAccessOperation operations[]= CodeStyleFixCore.createToStaticAccessOperations(compilationUnit, new HashMap<ASTNode, Block>(), problemLocation, false);
		if (operations == null)
			return null;

		String label= Messages.format(FixMessages.CodeStyleFix_ChangeStaticAccess_description, operations[0].getAccessorName());
		return new CodeStyleFix(label, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {operations[0]});
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic,
			boolean qualifyStaticFieldAccess,
			boolean changeIndirectStaticAccessToDirect,
			boolean qualifyMethodAccess,
			boolean qualifyStaticMethodAccess,
			boolean removeFieldQualifier,
			boolean removeMethodQualifier) {

		ICleanUpFixCore fix= CodeStyleFixCore.createCleanUp(compilationUnit, addThisQualifier, changeNonStaticAccessToStatic, qualifyStaticFieldAccess,
				changeIndirectStaticAccessToDirect, qualifyMethodAccess, qualifyStaticMethodAccess, removeFieldQualifier, removeMethodQualifier);
		return fix == null ? null : new CleanUpFixWrapper(fix);
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic,
			boolean changeIndirectStaticAccessToDirect) {

		IProblemLocationCore[] problemLocationArray= null;
		if (problems != null) {
			List<IProblemLocationCore> problemList= new ArrayList<>();
			for (IProblemLocation location : problems) {
				IProblemLocationCore problem= (ProblemLocation)location;
				problemList.add(problem);
			}
			problemLocationArray= problemList.toArray(new IProblemLocationCore[0]);
		}

		ICleanUpFixCore fix= CodeStyleFixCore.createCleanUp(compilationUnit, problemLocationArray, addThisQualifier, changeNonStaticAccessToStatic, changeIndirectStaticAccessToDirect);
		return fix == null ? null : new CleanUpFixWrapper(fix);
	}

	private CodeStyleFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
	}

}
