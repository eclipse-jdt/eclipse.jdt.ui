/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.LineChangeHover;

/**
 * JavaChangeHover
 * @since 3.0
 */
public class JavaChangeHover extends LineChangeHover  {
	
	/*
	 * @see org.eclipse.ui.internal.editors.text.LineChangeHover#formatSource(java.lang.String)
	 */
	protected String formatSource(String content) {
		return content;
	}
	
	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension#getInformationControlCreator()
	 */
	public IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new CustomSourceInformationControl(parent);
			}
		};
	}
}
