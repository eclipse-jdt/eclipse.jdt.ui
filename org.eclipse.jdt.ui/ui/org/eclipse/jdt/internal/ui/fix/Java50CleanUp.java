/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.Java50FixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;


/**
 * Create fixes which can transform pre Java50 code to Java50 code
 * @see org.eclipse.jdt.internal.corext.fix.Java50FixCore
 */
public class Java50CleanUp extends AbstractMultiFix {

	public Java50CleanUp(Map<String, String> options) {
		super(options);
	}

	public Java50CleanUp() {
		super();
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= requireAST();
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	private boolean requireAST() {
		boolean addAnotations= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS);

		return addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE) ||
		       addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED) ||
		       isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		boolean addAnotations= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		boolean addOverride= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		ICleanUpFix cleanUpFixCore= Java50FixCore.createCleanUp(compilationUnit,
				addAnotations && addOverride,
				addAnotations && addOverride && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION),
				addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED),
				isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES));
		return cleanUpFixCore;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;

		ProblemLocation[] coreProblems= new ProblemLocation[problems.length];
		for (int i= 0; i < coreProblems.length; i++) {
			coreProblems[i]= new ProblemLocation(problems[i].getOffset(), problems[i].getLength(), problems[i].getProblemId(), problems[i].getProblemArguments(), problems[i].isError(),
					problems[i].getMarkerType());
		}

		boolean addAnnotations= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		boolean addOverride= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		ICleanUpFix cleanUpFixCore= Java50FixCore.createCleanUp(compilationUnit, coreProblems,
				addAnnotations && addOverride,
				addAnnotations && addOverride && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION),
				addAnnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED),
				isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES));
		return cleanUpFixCore;
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
			result.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
			if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION)) {
				result.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION, JavaCore.ENABLED);
			}
		}

		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED))
			result.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.WARNING);

		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES))
			result.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);

		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
			result.add(MultiFixMessages.Java50MultiFix_AddMissingOverride_description);
			if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION)) {
				result.add(MultiFixMessages.Java50MultiFix_AddMissingOverride_description2);
			}
		}
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED))
			result.add(MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description);
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES))
			result.add(MultiFixMessages.Java50CleanUp_AddTypeParameters_description);
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		buf.append("class E {\n"); //$NON-NLS-1$
		buf.append("    /**\n"); //$NON-NLS-1$
		buf.append("     * @deprecated\n"); //$NON-NLS-1$
		buf.append("     */\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED)) {
			buf.append("    @Deprecated\n"); //$NON-NLS-1$
			buf.append("    public void foo() {}\n"); //$NON-NLS-1$
			buf.append("}\n"); //$NON-NLS-1$
		} else {
			buf.append("    public void foo() {}\n"); //$NON-NLS-1$
			buf.append("}\n\n"); //$NON-NLS-1$
		}
		buf.append("class ESub extends E implements Runnable {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
			buf.append("    @Override\n"); //$NON-NLS-1$
		}
		buf.append("    public void foo() {}\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS)
				&& isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)
				&& isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION)) {
			buf.append("    @Override\n"); //$NON-NLS-1$
			buf.append("    public void run() {}\n"); //$NON-NLS-1$
			buf.append("}\n"); //$NON-NLS-1$
		} else {
			buf.append("    public void run() {}\n"); //$NON-NLS-1$
			buf.append("}\n\n"); //$NON-NLS-1$
		}
		if (!isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) || !isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
			buf.append("\n"); //$NON-NLS-1$
		}

		return buf.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();

		if (Java50FixCore.isMissingOverrideAnnotationProblem(id)) {
			if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
				return ! Java50FixCore.isMissingOverrideAnnotationInterfaceProblem(id) || isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);
			}

		} else if (Java50FixCore.isMissingDeprecationProblem(id)) {
			return isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		} else if (Java50FixCore.isRawTypeReferenceProblem(id)) {
			return isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES);
		}

		return false;
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;

		boolean addAnnotations= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		boolean addMissingOverride= addAnnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		boolean addMissingOverrideInterfaceMethods= addMissingOverride && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);
		boolean addMissingDeprecated= addAnnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);
		boolean useTypeArgs= isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES);

		for (IProblem problem : compilationUnit.getProblems()) {
			int id= problem.getID();
			if (addMissingOverride && Java50FixCore.isMissingOverrideAnnotationProblem(id))
				if (! Java50FixCore.isMissingOverrideAnnotationInterfaceProblem(id) || addMissingOverrideInterfaceMethods)
					result++;
			if (addMissingDeprecated && Java50FixCore.isMissingDeprecationProblem(id))
				result++;
			if (useTypeArgs && Java50FixCore.isRawTypeReferenceProblem(id))
				result++;
		}
		return result;
	}

}
