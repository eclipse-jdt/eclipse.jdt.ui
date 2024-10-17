/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.UnusedSuppressWarningsFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Create fix to remove unnecessary SuppressWarnings
 * @see org.eclipse.jdt.internal.corext.fix.UnusedSuppressWarningsFixCore
 */
public class UnusedSuppressWarningsCleanUp extends AbstractMultiFix {

	public UnusedSuppressWarningsCleanUp(Map<String, String> options) {
		super(options);
	}

	public UnusedSuppressWarningsCleanUp() {
		super();
	}

	private StringLiteral fLiteral;
	private CompilationUnit fSavedCompilationUnit= null;

	public void setLiteral(StringLiteral literal) {
		fLiteral= literal;
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= requireAST();
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	private boolean requireAST() {
	    return isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;

		ICleanUpFix coreFix= UnusedSuppressWarningsFixCore.createAllFix(fSavedCompilationUnit == null ? compilationUnit : fSavedCompilationUnit,
				fLiteral);
		return coreFix;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;

		ICleanUpFix coreFix= UnusedSuppressWarningsFixCore.createAllFix(compilationUnit, fLiteral);
		return coreFix;
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();

		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS)) {
			result.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.ENABLED);
			result.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		}

		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		return new String[0];
	}

	@Override
	public String getPreview() {
		// not used as traditional cleanup
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() == IProblem.UnusedWarningToken)
			return isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS);

		return false;
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		try {
			ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
			if (!cu.isStructureKnown())
				return 0; //[clean up] 'Remove unnecessary $NLS-TAGS$' removes necessary ones in case of syntax errors: https://bugs.eclipse.org/bugs/show_bug.cgi?id=285814 :
		} catch (JavaModelException e) {
			return 0;
		}

		fSavedCompilationUnit= compilationUnit;
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS))
			result+= getNumberOfProblems(problems, compilationUnit);

		return result;
	}

	private int getNumberOfProblems(IProblem[] problems, CompilationUnit compilationUnit) {
		int result= 0;
		if (fLiteral == null) {
			return 1;
		}
		for (IProblem problem : problems) {
			IProblemLocation location= new ProblemLocation(problem);
			if (location.getProblemId() == IProblem.UnusedWarningToken) {
				ASTNode node= location.getCoveringNode(compilationUnit);
				if (node instanceof StringLiteral literal) {
					if (literal.getLiteralValue().equals(fLiteral.getLiteralValue())) {
						result++;
					}
				}
			}
		}
		return result;
	}

}
