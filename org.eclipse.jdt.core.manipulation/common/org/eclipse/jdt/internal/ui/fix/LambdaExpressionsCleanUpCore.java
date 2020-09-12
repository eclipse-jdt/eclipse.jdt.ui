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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.LambdaExpressionsFixCore;

public class LambdaExpressionsCleanUpCore extends AbstractCleanUpCore {

	public LambdaExpressionsCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public LambdaExpressionsCleanUpCore() {
		super();
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		boolean convertFunctionalInterfaces= isEnabled(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		if (!convertFunctionalInterfaces)
			return false;

		return isEnabled(CleanUpConstants.USE_LAMBDA)
				|| isEnabled(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
	}

	@Override
	public ICleanUpFixCore createFixCore(CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null)
			return null;

		boolean convertFunctionalInterfaces= isEnabled(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		if (!convertFunctionalInterfaces)
			return null;

		return LambdaExpressionsFixCore.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.USE_LAMBDA),
				isEnabled(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION));
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES)) {
			if (isEnabled(CleanUpConstants.USE_LAMBDA)) {
				result.add(MultiFixMessages.LambdaExpressionsCleanUp_use_lambda_where_possible);
			}
			if (isEnabled(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION)) {
				result.add(MultiFixMessages.LambdaExpressionsCleanUp_use_anonymous);
			}
		}

		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		boolean convert= isEnabled(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		boolean useLambda= isEnabled(CleanUpConstants.USE_LAMBDA);
		boolean useAnonymous= isEnabled(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		boolean firstLambda= convert && useLambda;
		boolean secondLambda= !convert || !useAnonymous;

		if (firstLambda) {
			buf.append("IntConsumer c = i -> {\n"); //$NON-NLS-1$
			buf.append("    System.out.println(i);\n"); //$NON-NLS-1$
			buf.append("};\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
		} else {
			buf.append("IntConsumer c = new IntConsumer() {\n"); //$NON-NLS-1$
			buf.append("    @Override public void accept(int value) {\n"); //$NON-NLS-1$
			buf.append("        System.out.println(i);\n"); //$NON-NLS-1$
			buf.append("    }\n"); //$NON-NLS-1$
			buf.append("};\n"); //$NON-NLS-1$
		}

		if (secondLambda) {
			buf.append("Runnable r = () -> { /* do something */ };\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
		} else {
			buf.append("Runnable r = new Runnable() {\n"); //$NON-NLS-1$
			buf.append("    @Override public void run() {\n"); //$NON-NLS-1$
			buf.append("        //do something\n"); //$NON-NLS-1$
			buf.append("    }\n"); //$NON-NLS-1$
			buf.append("};\n"); //$NON-NLS-1$
		}
		return buf.toString();
	}

}
