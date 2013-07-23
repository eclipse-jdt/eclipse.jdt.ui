/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.LambdaExpressionsFix;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class LambdaExpressionsCleanUp extends AbstractCleanUp {

	public LambdaExpressionsCleanUp(Map<String, String> options) {
		super(options);
	}

	public LambdaExpressionsCleanUp() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	private boolean requireAST() {
		return isEnabled(CleanUpConstants.USE_LAMBDA);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null)
			return null;

		if (!isEnabled(CleanUpConstants.USE_LAMBDA))
			return null;

		return LambdaExpressionsFix.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.USE_LAMBDA));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<String>();
		if (isEnabled(CleanUpConstants.USE_LAMBDA))
			result.add(MultiFixMessages.LambdaExpressionsCleanUp_use_lambda_where_possible);

		return result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPreview() {
		StringBuffer buf= new StringBuffer();

		if (isEnabled(CleanUpConstants.USE_LAMBDA)) {
			buf.append("Runnable r = () -> {\n"); //$NON-NLS-1$
			buf.append("    //do something\n"); //$NON-NLS-1$
			buf.append("};\n"); //$NON-NLS-1$
		} else {
			buf.append("Runnable r = new Runnable() {\n"); //$NON-NLS-1$
			buf.append("    @Override\n"); //$NON-NLS-1$
			buf.append("    public void run() {\n"); //$NON-NLS-1$
			buf.append("        //do something\n"); //$NON-NLS-1$
			buf.append("    }\n"); //$NON-NLS-1$
			buf.append("};\n"); //$NON-NLS-1$
		}
		return buf.toString();
	}

}
