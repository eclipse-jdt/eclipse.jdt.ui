/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jdt.internal.corext.fix.Java50Fix;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can transform pre Java50 code to Java50 code
 * @see org.eclipse.jdt.internal.corext.fix.Java50Fix
 *
 */
public class Java50CleanUp extends AbstractMultiFix {

	public Java50CleanUp(Map options) {
		super(options);
	}

	public Java50CleanUp() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public CleanUpRequirements getRequirements() {
		boolean requireAST= requireAST();
		Map requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	private boolean requireAST() {
		boolean addAnotations= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS);

		return addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE) ||
		       addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED) ||
		       isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES);
	}

	/**
	 * {@inheritDoc}
	 */
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		boolean addAnotations= isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		return Java50Fix.createCleanUp(compilationUnit,
				addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE),
				addAnotations && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED),
				isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES));
	}

	/**
	 * {@inheritDoc}
	 */
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;

		return Java50Fix.createCleanUp(compilationUnit, problems,
				isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE),
				isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED),
				isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES));
	}

	private Map getRequiredOptions() {
		Map result= new Hashtable();
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE))
			result.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);

		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED))
			result.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.WARNING);

		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES))
			result.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getStepDescriptions() {
		List result= new ArrayList();
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE))
			result.add(MultiFixMessages.Java50MultiFix_AddMissingOverride_description);
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED))
			result.add(MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description);
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES))
			result.add(MultiFixMessages.Java50CleanUp_AddTypeParameters_description);
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();

		buf.append("class E {\n"); //$NON-NLS-1$
		buf.append("    /**\n"); //$NON-NLS-1$
		buf.append("     * @deprecated\n"); //$NON-NLS-1$
		buf.append("     */\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED)) {
			buf.append("    @Deprecated\n"); //$NON-NLS-1$
		}
		buf.append("    public void foo() {}\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("class ESub extends E {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
			buf.append("    @Override\n"); //$NON-NLS-1$
		}
		buf.append("    public void foo() {}\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$

		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() == IProblem.MissingOverrideAnnotation)
			return isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);

		if (Java50Fix.isMissingDeprecationProblem(problem))
			return isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		if (Java50Fix.isRawTypeReference(problem))
			return isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES);

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE)) {
			result+= getNumberOfProblems(problems, IProblem.MissingOverrideAnnotation);
		}
		if (isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS) && isEnabled(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.FieldMissingDeprecatedAnnotation || id == IProblem.MethodMissingDeprecatedAnnotation || id == IProblem.TypeMissingDeprecatedAnnotation)
					result++;
			}
		}
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.UnsafeTypeConversion || id == IProblem.RawTypeReference || id == IProblem.UnsafeRawMethodInvocation)
					result++;
			}
		}
		return result;
	}

}
