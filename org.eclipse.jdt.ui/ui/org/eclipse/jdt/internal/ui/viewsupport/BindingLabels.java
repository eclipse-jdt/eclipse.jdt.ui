/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.dom.Bindings;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class BindingLabels {

	public static String getFullyQualified(IBinding binding) {
		IJavaElement javaElement;
		try {
			javaElement= binding.getJavaElement();
		} catch (IllegalArgumentException e) {
			// TODO: see bug 78087
			JavaPlugin.log(e);
			javaElement= null;
		}
		if (javaElement != null)
			return JavaElementLabels.getElementLabel(javaElement, JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES);
		else
			return Bindings.asString(binding); //fallback: better than nothing
	}

}
