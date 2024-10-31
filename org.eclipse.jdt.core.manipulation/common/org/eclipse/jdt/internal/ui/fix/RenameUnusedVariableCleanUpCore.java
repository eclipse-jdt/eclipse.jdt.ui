/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified from UnusedCodeCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Pattern;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.RenameUnusedVariableFixCore;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Create fixes which can remove unused code
 * see org.eclipse.jdt.internal.corext.fix.UnusedCodeFix
 */
public class RenameUnusedVariableCleanUpCore extends AbstractMultiFix {

	public RenameUnusedVariableCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public RenameUnusedVariableCleanUpCore() {
		super();
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= requireAST();
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	private boolean requireAST() {
		return isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);
	}

	@Override
	public ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		return RenameUnusedVariableFixCore.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES));
	}

	@Override
	public ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return RenameUnusedVariableFixCore.createCleanUp(compilationUnit, problems,
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES));
	}

	public Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();

		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES)) {
			result.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_LAMBDA_PARAMETER, JavaCore.WARNING);
		}

		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedVariable_description);
		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		buf.append("    public void bar() {\n"); //$NON-NLS-1$
		if (!isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES)) {
			String code= """
					record R(int i, long l) {}

					R r = new R(1, 1);
					switch (r) {
					   	case R(_, long l) -> {}
					   	case R r2 -> {}
					}
					"""; //$NON-NLS-1$
			buf.append(code);
		} else {
			String code= """
					record R(int i, long l) {}

					R r = new R(1, 1);
					switch (r) {
					   	case R(_, _) -> {}
					   	case R _ -> {}
					}
					"""; //$NON-NLS-1$
			buf.append(code);
		}
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES)) {
			buf.append("\n"); //$NON-NLS-1$
		}

		return buf.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		if (UnusedCodeFixCore.isUnusedMember(problem) || UnusedCodeFixCore.isUnusedLambdaParameter(problem))
			return isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		return false;
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES)) {
			for (IProblem problem : problems) {
				int id= problem.getID();
				if (id == IProblem.LocalVariableIsNeverUsed) {
					ProblemLocation location= new ProblemLocation(problem);
					SimpleName name= UnusedCodeFixCore.getUnusedName(compilationUnit, location);
					if (JavaModelUtil.is22OrHigher(compilationUnit.getJavaElement().getJavaProject()) &&
							name.getParent() instanceof SingleVariableDeclaration nameParent &&
							nameParent.getParent() instanceof Pattern) {
						result++;
					}
				} else if (id == IProblem.LambdaParameterIsNeverUsed) {
					++result;
				}
			}
		}
		return result;
	}
}
