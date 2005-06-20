/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.views;

import org.eclipse.jface.viewers.LabelProvider;


public class JEViewLabelProvider extends LabelProvider /*implements IColorProvider, IFontProvider*/ {
		
	@Override
	public String getText(Object element) {
		if (element instanceof JEAttribute)
			return ((JEAttribute) element).getLabel();
		return super.getText(element);
	}

}
