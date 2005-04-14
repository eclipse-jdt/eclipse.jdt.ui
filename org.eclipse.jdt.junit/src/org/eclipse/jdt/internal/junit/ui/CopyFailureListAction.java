/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Copies the names of the methods that failed to the clipboard.
 */
public class CopyFailureListAction extends Action {
	private FailureTab fView;
	
	private final Clipboard fClipboard;
		
	/**
	 * Constructor for CopyFailureListAction.
	 */
	public CopyFailureListAction(TestRunnerViewPart runner, FailureTab view, Clipboard clipboard) {
		super(JUnitMessages.CopyFailureList_action_label);  
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.COPYFAILURELIST_ACTION);
		fView= view;
		fClipboard= clipboard;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
					
		try{
			fClipboard.setContents(
				new String[] { fView.getAllFailedTestNames() }, 
				new Transfer[]{ plainTextTransfer });
		}  catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) 
				throw e;
			if (MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), JUnitMessages.CopyFailureList_problem, JUnitMessages.CopyFailureList_clipboard_busy))  
				run();
		}
	}

}
