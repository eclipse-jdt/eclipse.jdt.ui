/*******************************************************************************
 * Copyright (c) 2024, 2025 Red Hat Inc. and others.
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
		// ask for fresh AST as we are setting all default options on and we run as last cleanup
		return new CleanUpRequirements(requireAST, requireAST, false, requireAST, requiredOptions);
	}

	private boolean requireAST() {
	    return isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null || !isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS))
			return null;

		ICleanUpFix coreFix= fLiteral != null ? UnusedSuppressWarningsFixCore.createAllFix(fSavedCompilationUnit == null ? compilationUnit : fSavedCompilationUnit,
				fLiteral) : UnusedSuppressWarningsFixCore.createAllFix(compilationUnit);
		return coreFix;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null || !isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS))
			return null;

		ICleanUpFix coreFix= UnusedSuppressWarningsFixCore.createAllFix(compilationUnit, fLiteral);
		return coreFix;
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();

		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS)) {
//			result.putAll(JavaCore.getOptions());
			result.put(JavaCore.COMPILER_PB_DEPRECATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_TERMINAL_DEPRECATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE, JavaCore.ENABLED);
			result.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.ENABLED);
			result.put(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, JavaCore.ENABLED);
			result.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.ERROR); // do not change to WARNING
			// API LEAK
			result.put(JavaCore.COMPILER_PB_API_LEAKS, JavaCore.WARNING);
			// BOXING
			result.put(JavaCore.COMPILER_PB_AUTOBOXING, JavaCore.WARNING);
			// CAST
			result.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.WARNING);
			// PREVIEW
			result.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.WARNING);
			// RAW
			result.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
			// SERIAL
			result.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.WARNING);
			// SYNCHRONIZED
			result.put(JavaCore.COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, JavaCore.WARNING);
			// SUPER
			result.put(JavaCore.COMPILER_PB_OVERRIDING_METHOD_WITHOUT_SUPER_INVOCATION, JavaCore.WARNING);
			// NLS
			result.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
			// HIDING
			result.put(JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING, JavaCore.WARNING);
			// NULL options
			result.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_ANNOTATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED, JavaCore.WARNING);
			// RESTRICTION
			result.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, JavaCore.WARNING);
			// STATIC ACCESS
			result.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
			// UNUSED
			result.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, getPreview());
			result.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_EXCEPTION_PARAMETER, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_LABEL, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_TYPE_ARGUMENTS_FOR_METHOD_INVOCATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_REDUNDANT_SUPERINTERFACE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_TYPE_PARAMETER, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNUSED_LAMBDA_PARAMETER, JavaCore.WARNING);
			// STATIC_METHOD
			result.put(JavaCore.COMPILER_PB_MISSING_STATIC_ON_METHOD, JavaCore.WARNING);
		    result.put(JavaCore.COMPILER_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD, JavaCore.WARNING);
		    // RESOURCE
		    result.put(JavaCore.COMPILER_PB_UNCLOSED_CLOSEABLE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_POTENTIALLY_UNCLOSED_CLOSEABLE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_RECOMMENDED_RESOURCE_MANAGEMENT, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_INCOMPATIBLE_OWNING_CONTRACT, JavaCore.WARNING);
			// INCOMPLETE_SWITCH
			result.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_SWITCH_MISSING_DEFAULT_CASE, JavaCore.WARNING);
			// UNCHECKED
			result.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
			String suppressRawWhenUnchecked = System.getProperty("suppressRawWhenUnchecked"); //$NON-NLS-1$
			if (suppressRawWhenUnchecked != null && "true".equalsIgnoreCase(suppressRawWhenUnchecked)) { //$NON-NLS-1$
				result.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
			}
		    // JAVADOC
			result.put(JavaCore.COMPILER_PB_INVALID_JAVADOC, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS, JavaCore.WARNING);
			// UNLIKELY_ARGUMENT_TYPE
			result.put(JavaCore.COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE, JavaCore.WARNING);
			result.put(JavaCore.COMPILER_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE, JavaCore.WARNING);
		}

		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS)) {
			return new String[] { MultiFixMessages.UnusedSuppressWarningsCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS)) {
			return """
				int x = 3;
				System.out.println(x);

				"""; //$NON-NLS-1$
		}

		return """
			@SuppressWarnings("unused")
			int x = 3;
			System.out.println(x);
			"""; //$NON-NLS-1$
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
