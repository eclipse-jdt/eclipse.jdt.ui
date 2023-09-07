package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class SuppressWarningsSubProcessorCore {

	static final String ADD_SUPPRESSWARNINGS_ID= "org.eclipse.jdt.ui.correction.addSuppressWarnings"; //$NON-NLS-1$

	public static final boolean hasSuppressWarningsProposal(IJavaProject javaProject, int problemId) {
		if (CorrectionEngine.getWarningToken(problemId) != null && JavaModelUtil.is50OrHigher(javaProject)) {
			String optionId= JavaCore.getOptionForConfigurableSeverity(problemId);
			if (optionId != null) {
				String optionValue= javaProject.getOption(optionId, true);
				return JavaCore.INFO.equals(optionValue)
						|| JavaCore.WARNING.equals(optionValue)
						|| (JavaCore.ERROR.equals(optionValue) && JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, true)));
			}
		}
		return false;
	}

}
