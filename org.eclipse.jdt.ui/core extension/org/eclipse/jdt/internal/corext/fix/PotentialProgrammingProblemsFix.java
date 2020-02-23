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
 *     Red Hat Inc. - modified to use PotentialProgrammingProblemsFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionHashOperationCore;


public class PotentialProgrammingProblemsFix extends CompilationUnitRewriteOperationsFix {

	private static PotentialProgrammingProblemsFixCore.ISerialVersionFixContext fCurrentContext;

	public static IProposableFix[] createMissingSerialVersionFixes(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() != IProblem.MissingSerialVersion)
			return null;

		final ICompilationUnit unit= (ICompilationUnit)compilationUnit.getJavaElement();
		if (unit == null)
			return null;

		final SimpleName simpleName= PotentialProgrammingProblemsFixCore.getSelectedName(compilationUnit, (ProblemLocationCore)problem);
		if (simpleName == null)
			return null;

		ASTNode declaringNode= PotentialProgrammingProblemsFixCore.getDeclarationNode(simpleName);
		if (declaringNode == null)
			return null;

		SerialVersionDefaultOperationCore defop= new SerialVersionDefaultOperationCore(unit, new ASTNode[] {declaringNode});
		IProposableFix fix1= new PotentialProgrammingProblemsFix(FixMessages.Java50Fix_SerialVersion_default_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {defop});

		SerialVersionHashOperationCore hashop= new SerialVersionHashOperationCore(unit, new ASTNode[] {declaringNode});
		IProposableFix fix2= new PotentialProgrammingProblemsFix(FixMessages.Java50Fix_SerialVersion_hash_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {hashop});

		return new IProposableFix[] {fix1, fix2};
	}

	public static RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor,
			boolean calculatedId,
			boolean defaultId,
			boolean randomId) throws CoreException {

		return PotentialProgrammingProblemsFixCore.checkPreConditions(project, compilationUnits, monitor, calculatedId, defaultId, randomId);
    }

	public static RefactoringStatus checkPostConditions(IProgressMonitor monitor) {
		return PotentialProgrammingProblemsFixCore.checkPostConditions(monitor);
   }

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean addSerialVersionIds) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		return createCleanUp(compilationUnit, locations, addSerialVersionIds);
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, boolean addSerialVersionIds) {
		if (addSerialVersionIds) {

			final ICompilationUnit unit= (ICompilationUnit)compilationUnit.getJavaElement();
			if (unit == null)
				return null;

			List<ASTNode> declarationNodes= new ArrayList<>();
			for (IProblemLocation problem : problems) {
				if (problem.getProblemId() == IProblem.MissingSerialVersion) {
					final SimpleName simpleName= PotentialProgrammingProblemsFixCore.getSelectedName(compilationUnit, (ProblemLocationCore)problem);
					if (simpleName != null) {
						ASTNode declarationNode= PotentialProgrammingProblemsFixCore.getDeclarationNode(simpleName);
						if (declarationNode != null) {
							declarationNodes.add(declarationNode);
						}
					}
				}
			}
			if (declarationNodes.isEmpty())
				return null;

			for (ASTNode declarationNode : declarationNodes) {
	            ITypeBinding binding= PotentialProgrammingProblemsFixCore.getTypeBinding(declarationNode);
	            if (fCurrentContext.getSerialVersionId(binding) != null) {
	            	PotentialProgrammingProblemsFixCore.SerialVersionHashBatchOperation op= new PotentialProgrammingProblemsFixCore.SerialVersionHashBatchOperation(unit, declarationNodes.toArray(new ASTNode[declarationNodes.size()]), fCurrentContext);
	    			return new PotentialProgrammingProblemsFix(FixMessages.PotentialProgrammingProblemsFix_add_id_change_name, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {op});
	            }
            }
		}
		return null;
	}

	protected PotentialProgrammingProblemsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}