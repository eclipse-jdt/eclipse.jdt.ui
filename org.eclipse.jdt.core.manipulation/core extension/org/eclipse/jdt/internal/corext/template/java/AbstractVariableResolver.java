/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.Variable;


/**
 * Resolves template variables to variables which are assignment-compatible with the variable
 * instance class parameters.
 *
 * @since 3.4
 */
public abstract class AbstractVariableResolver extends TemplateVariableResolver {

	protected final String fDefaultType;
	private Variable[] fVariables;

	/**
	 * Create a variable resolver resolving to
	 * <code>defaultType</code> if no types specified
	 * as parameter
	 * @param defaultType the default type to resolve to
	 */
	protected AbstractVariableResolver(String defaultType) {
		fDefaultType= defaultType;
	}

	/**
	 * Returns a set of variables of <code>type</code> visible in
	 * the given <code>context</code>.
	 *
	 * @param type the type name to search variables for
	 * @param context context within to search for variables
	 * @return the visible variables of <code>type</code> in <code>context</code>, empty array if no visible variables
	 */
	protected abstract Variable[] getVisibleVariables(String type, IJavaContext context);

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {

		if (variable instanceof JavaVariable) {
			IJavaContext jc= (IJavaContext) context;
			JavaVariable jv= (JavaVariable) variable;

			List<String> params= variable.getVariableType().getParams();
			if (params.size() == 0) {
				fVariables= getVisibleVariables(fDefaultType, jc);
				jv.setParamType(fDefaultType);
			} else if (params.size() == 1) {
				String type= params.get(0);
				fVariables= getVisibleVariables(type, jc);
				jv.setParamType(type);
			} else {
				ArrayList<Variable> variables= new ArrayList<>();
				for (String string : params) {
					variables.addAll(Arrays.asList(getVisibleVariables(string, jc)));
				}
				fVariables= variables.toArray(new Variable[variables.size()]);

				//set to default type, a template which references to the type
				//of _the_ parameter will not correctly work anyway
				jv.setParamType(fDefaultType);
			}

			if (fVariables.length > 0) {
				jv.setChoices(fVariables);
				jc.markAsUsed(jv.getDefaultValue());
			} else {
				super.resolve(variable, context);
				return;
			}
			if (fVariables.length > 1)
				variable.setUnambiguous(false);
			else
				variable.setUnambiguous(isUnambiguous(context));
		} else
			super.resolve(variable, context);
	}

	@Override
	protected String[] resolveAll(TemplateContext context) {

		String[] names= new String[fVariables.length];
	    for (int i= 0; i < fVariables.length; i++)
			names[i]= fVariables[i].getName();

	    if (names.length > 0)
	    	((IJavaContext)context).markAsUsed(names[0]);

		return names;
	}

}
