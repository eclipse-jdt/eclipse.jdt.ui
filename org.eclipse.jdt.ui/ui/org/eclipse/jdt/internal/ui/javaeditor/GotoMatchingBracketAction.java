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

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.Assert;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;


public class GotoMatchingBracketAction extends Action {

	public final static String GOTO_MATCHING_BRACKET= "GotoMatchingBracket"; //$NON-NLS-1$
	
	private final JavaEditor fEditor;
	
	public GotoMatchingBracketAction(JavaEditor editor) {
		super(JavaEditorMessages.getString("GotoMatchingBracket.label")); //$NON-NLS-1$
		Assert.isNotNull(editor);
		fEditor= editor;
		setEnabled(true);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GOTO_MATCHING_BRACKET_ACTION);
	}
	
	public void run() {
		fEditor.gotoMatchingBracket();
	}
}
