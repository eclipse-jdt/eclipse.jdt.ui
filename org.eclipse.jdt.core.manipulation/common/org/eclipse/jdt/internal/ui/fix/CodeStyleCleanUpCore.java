/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - create core version based on CodeStyleCleanUp
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
import org.eclipse.jdt.internal.corext.fix.CodeStyleFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Creates fixes which can resolve code style issues
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFixCore
 */
public class CodeStyleCleanUpCore extends AbstractMultiFix {

	public CodeStyleCleanUpCore() {
	}

	public CodeStyleCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= requireAST();
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	private boolean requireAST() {
		boolean nonStaticFields= isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		boolean nonStaticMethods= isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		boolean qualifyStatic= isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);

		return nonStaticFields && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS) ||
		       qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS) ||
		       qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD) ||
		       qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS) ||
		       nonStaticMethods && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS) ||
		       qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD) ||
		       nonStaticFields && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY) ||
		       nonStaticMethods && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);
	}

	@Override
	public org.eclipse.jdt.ui.cleanup.ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;

		boolean nonStaticFields= isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		boolean nonStaticMethods= isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		boolean qualifyStatic= isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);

		return CodeStyleFixCore.createCleanUp(compilationUnit,
				nonStaticFields && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS),
				qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS),
				qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD),
				qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS),
				nonStaticMethods && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS),
				qualifyStatic && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD),
				nonStaticFields && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY),
				nonStaticMethods && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY)
				);
	}


	@Override
	public ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return CodeStyleFixCore.createCleanUp(compilationUnit, problems,
				isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS),
				isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS),
				isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS));
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS))
			result.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS))
			result.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS))
			result.add(MultiFixMessages.CodeStyleMultiFix_AddThisQualifier_description);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS))
			result.add(MultiFixMessages.CodeStyleCleanUp_QualifyNonStaticMethod_description);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY))
			result.add(MultiFixMessages.CodeStyleCleanUp_removeFieldThis_description);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY))
			result.add(MultiFixMessages.CodeStyleCleanUp_removeMethodThis_description);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD))
			result.add(MultiFixMessages.CodeStyleMultiFix_QualifyAccessToStaticField);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD))
			result.add(MultiFixMessages.CodeStyleCleanUp_QualifyStaticMethod_description);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS))
			result.add(MultiFixMessages.CodeStyleMultiFix_ChangeNonStaticAccess_description);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS))
			result.add(MultiFixMessages.CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect);
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		buf.append("private int value;\n"); //$NON-NLS-1$
		buf.append("public int get() {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS)) {
			buf.append("    return this.value + this.value;\n"); //$NON-NLS-1$
		} else if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY)) {
			buf.append("    return value + value;\n"); //$NON-NLS-1$
		} else {
			buf.append("    return this.value + value;\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("public int getZero() {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS)) {
			buf.append("    return this.get() - this.get();\n"); //$NON-NLS-1$
		} else if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY)) {
			buf.append("    return get() - get();\n"); //$NON-NLS-1$
		} else {
			buf.append("    return this.get() - get();\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("class E {\n"); //$NON-NLS-1$
		buf.append("    public static int NUMBER;\n"); //$NON-NLS-1$
		buf.append("    public static void set(int i) {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD)) {
			buf.append("        E.NUMBER= i;\n"); //$NON-NLS-1$
		} else {
			buf.append("        NUMBER= i;\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("    public void reset() {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD)) {
			buf.append("        E.set(0);\n"); //$NON-NLS-1$
		} else {
			buf.append("        set(0);\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("class ESub extends E {\n"); //$NON-NLS-1$
		buf.append("    public void reset() {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS)) {
			buf.append("        E.NUMBER= 0;\n"); //$NON-NLS-1$
		} else {
			buf.append("        ESub.NUMBER= 0;\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("public void dec() {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS)) {
			buf.append("    E.NUMBER--;\n"); //$NON-NLS-1$
		} else {
			buf.append("    (new E()).NUMBER--;\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$

		return buf.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		if (IProblem.UnqualifiedFieldAccess == problem.getProblemId())
			return isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		if (CodeStyleFixCore.isIndirectStaticAccess(problem))
			return isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		if (CodeStyleFixCore.isNonStaticAccess(problem))
			return isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		return false;
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS))
			result+= getNumberOfProblems(problems, IProblem.UnqualifiedFieldAccess);
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS)) {
			for (IProblem problem : problems) {
				int id= problem.getID();
				if (id == IProblem.IndirectAccessToStaticField || id == IProblem.IndirectAccessToStaticMethod)
					result++;
			}
		}
		if (isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS) && isEnabled(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS)) {
			for (IProblem problem : problems) {
				int id= problem.getID();
				if (id == IProblem.NonStaticAccessToStaticField || id == IProblem.NonStaticAccessToStaticMethod)
					result++;
			}
		}
		return result;
	}

}
