/*******************************************************************************
 * Copyright (c) 2019 Nicolaj Hoess.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nicolaj Hoess - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;

/**
 * This class is responsible for resolving a given type or the type of another variable to its
 * actual type.
 *
 * TODO Maybe <code>ActualType</code> is not the best term in this context.
 */
public class ActualTypeResolver extends TypeResolver {

	private static final String EMPTY= ""; //$NON-NLS-1$

	private static final String ARRAY_BRACKETS= "[]"; //$NON-NLS-1$

	private static final String GENERIC_CLASS_SERPATOR= ","; //$NON-NLS-1$

	private static final String GENERIC_CLASS_OPEN_DIAMOND= "<"; //$NON-NLS-1$

	private static final String GENERIC_CLASS_CLOSE_DIAMOND= ">"; //$NON-NLS-1$

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		List<String> params= variable.getVariableType().getParams();
		if (params.size() > 0 && context instanceof JavaPostfixContext) {
			String param= params.get(0);
			JavaPostfixContext jc= (JavaPostfixContext) context;
			TemplateVariable ref= jc.getTemplateVariable(param);
			MultiVariable mv= (MultiVariable) variable;

			if (ref instanceof JavaVariable) {
				// Reference is another variable
				JavaVariable refVar= (JavaVariable) ref;
				jc.addDependency(refVar, mv);

				param= refVar.getParamType();
				if (param != null && !param.isEmpty()) {
					param = param.replace("? extends ", EMPTY); //$NON-NLS-1$

					// Handle arrays
					if (param.endsWith(ARRAY_BRACKETS)) {
						param = param.substring(0, param.length() - 2);
					}

					// Handle generic types
					if (param.contains(GENERIC_CLASS_OPEN_DIAMOND) && param.endsWith(GENERIC_CLASS_CLOSE_DIAMOND)) {
						// Extract content inside outermost <>
						String inner = param.substring(param.indexOf(GENERIC_CLASS_OPEN_DIAMOND) + 1,
								param.lastIndexOf(GENERIC_CLASS_CLOSE_DIAMOND));
						
						if (inner.contains(GENERIC_CLASS_SERPATOR)) {
							inner = inner.substring(0, inner.indexOf(GENERIC_CLASS_SERPATOR)).trim();
						}
						param = inner;
					}

					// Remove any trailing '>' that might have been left over
					if (param.endsWith(GENERIC_CLASS_CLOSE_DIAMOND)) {
						param = param.substring(0, param.length() - 1);
					}

					String reference = jc.addImportGenericClass(param);
					mv.setValue(reference);
					mv.setUnambiguous(true);
					mv.setResolved(true);
					return;
				}
			}
		}
		super.resolve(variable, context);
	}
}
