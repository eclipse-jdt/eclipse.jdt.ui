/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IAction;

public class ActionUtil {

	private static final String DOTS= "...";
	private static final int DOTS_LENGTH= DOTS.length();

	public static String getText(IAction action) {
		String text= action.getText();
		int last= text.length();
		int dots= text.lastIndexOf(DOTS);
		if (dots != -1 && dots + DOTS_LENGTH == last)
			last= dots;
		StringBuffer result= new StringBuffer();
		for (int i= 0; i < text.length(); i++) {
			char ch= text.charAt(i);
			if (ch == '@')
				break;
			if (ch != '&')
				result.append(ch);
		}
		return result.toString();
	}	
}
