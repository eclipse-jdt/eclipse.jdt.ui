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

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;


/**
 * This class is the resolver for the variable <code>inner_expression</code>. <br/>
 * The value of the resolved variable will be the source code of the node which resolves to the
 * <code>inner_expression</code> The type of the resolved variable will be the fully qualified name
 * or the name of the primitive type of the node which the content assist is invoked on.
 */
public class InnerExpressionResolver extends SimpleTemplateVariableResolver {

	public static final String INNER_EXPRESSION_VAR= "inner_expression"; //$NON-NLS-1$

	public static final String HIDE_FLAG= "novalue"; //$NON-NLS-1$

	public static final String[] FLAGS= { HIDE_FLAG };

	public InnerExpressionResolver() {
		super(INNER_EXPRESSION_VAR, ""); // TODO Add description //$NON-NLS-1$
	}

	@Override
	protected String resolve(TemplateContext context) {
		if (!(context instanceof JavaPostfixContext))
			return ""; //$NON-NLS-1$

		return ((JavaPostfixContext) context).getAffectedStatement();
	}

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		if (context instanceof JavaPostfixContext && variable instanceof JavaVariable) {
			JavaPostfixContext c= (JavaPostfixContext) context;
			JavaVariable jv= (JavaVariable) variable;
			List<String> params= variable.getVariableType().getParams();

			if (!params.contains(HIDE_FLAG)) {
				jv.setValue(resolve(context));
			} else {
				// TODO This is a hacky solution to hide the output of the variable for specific templates (e.g. .field).
				jv.setValues(new String[] { "", resolve(context) }); // We hide the value from the output //$NON-NLS-1$
			}

			jv.setParamType(c.getInnerExpressionTypeSignature());
			jv.setResolved(true);
			jv.setUnambiguous(true);
			return;
		}
		super.resolve(variable, context);
	}
}
