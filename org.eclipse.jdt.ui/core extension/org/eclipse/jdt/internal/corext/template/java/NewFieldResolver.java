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

import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.text.edits.TextEdit;

public class NewFieldResolver extends TemplateVariableResolver {

	private static final String ASSIGNMENT_OPERATOR= "="; //$NON-NLS-1$

	private static final int IS_PUBLIC_ARG= 1;

	private static final boolean IS_PUBLIC_DEFAULT_VALUE= false;

	private static final int IS_FORCE_STATIC_ARG= 2;

	private static final boolean IS_FORCE_STATIC_DEFAULT_VALUE= false;

	private static final int IS_FINAL_FIELD_ARG= 3;

	private static final boolean IS_FINAL_FIELD_DEFAULT_VALUE= false;

	private static final int INIT_VALUE_ARG= 4;

	private static final boolean INIT_VALUE_DEFAULT_VALUE= false;

	private final String defaultType;

	public NewFieldResolver() {
		this("java.lang.Object"); //$NON-NLS-1$
	}

	NewFieldResolver(String defaultType) {
		this.defaultType= defaultType;
	}

	private int findAbsolutePositionOfFieldNameBeginning(IDocument doc, TextEdit te, String name) {
		try {
			String temp= doc.get(te.getOffset(), te.getLength());
			if (temp.contains(ASSIGNMENT_OPERATOR)) {
				temp= temp.substring(0, temp.indexOf(ASSIGNMENT_OPERATOR));
			}
			int nameOcc= temp.lastIndexOf(name);
			if (nameOcc != -1) {
				return te.getOffset() + nameOcc;
			}
			// Variable name not included
			// Let's see if we can determine the position with some basic logic
			// This case should not happen but maybe it's not bad to have a fallback
			int offset= temp.length();
			boolean semicolonFound= false;
			while (offset >= 0 && semicolonFound == false) {
				if (temp.charAt(offset) == ' ' && semicolonFound)
					return te.getOffset() + offset;
				if (temp.charAt(offset) == ';')
					semicolonFound= true;
			}
		} catch (BadLocationException e) {
			// continue
		}
		return te.getOffset();
	}

	/*
	 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
	 */
	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		List<String> params= variable.getVariableType().getParams();
		String param;
		if (params.size() == 0)
			param= defaultType;
		else
			param= params.get(0);

		JavaPostfixContext jc= (JavaPostfixContext) context;
		TemplateVariable ref= jc.getTemplateVariable(param);
		MultiVariable mv= (MultiVariable) variable;
		String type= param;
		if (ref instanceof MultiVariable) {
			// Reference is another variable
			JavaVariable refVar= (JavaVariable) ref;
			jc.addDependency(refVar, mv);
			type= refVar.getParamType();
		}

		boolean publicField= getParamValue(params, IS_PUBLIC_ARG, IS_PUBLIC_DEFAULT_VALUE);
		boolean forceStatic= getParamValue(params, IS_FORCE_STATIC_ARG, IS_FORCE_STATIC_DEFAULT_VALUE);
		boolean finalField= getParamValue(params, IS_FINAL_FIELD_ARG, IS_FINAL_FIELD_DEFAULT_VALUE);
		boolean initValue= getParamValue(params, INIT_VALUE_ARG, INIT_VALUE_DEFAULT_VALUE);
		String newType= type;
		String[] names= jc.suggestFieldName(newType, finalField, forceStatic);
		mv.setChoices(names);

		TextEdit te= jc.addField(newType, names[0], publicField, forceStatic, finalField, (initValue && ref instanceof JavaVariable) ? getValueFromVariable((JavaVariable) ref) : null);
		if (te != null) {
			jc.markAsUsed(names[0]);
			// We can apply it to the context
			jc.applyTextEdit(te);
			mv.setResolved(true);
			jc.registerOutOfRangeOffset(mv, findAbsolutePositionOfFieldNameBeginning(jc.getDocument(), te, names[0]) - jc.getAffectedSourceRegion().getOffset());
		} else {
			// Field was not created so doing nothing probably is the only option
		}
	}

	private boolean getParamValue(List<String> params, int paramPos, boolean defaultValue) {
		if (params.size() >= paramPos + 1) {
			String val= params.get(paramPos);
			return Boolean.parseBoolean(val);
		}
		return defaultValue;
	}

	private String getValueFromVariable(JavaVariable var) {
		for (String s : var.getValues()) {
			if (s != null && s.trim().length() > 0) {
				return s;
			}
		}
		return null;
	}
}
