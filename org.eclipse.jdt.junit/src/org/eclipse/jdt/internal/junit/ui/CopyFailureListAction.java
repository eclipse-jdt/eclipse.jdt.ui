/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.junit.model.TestElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Copies the names of the methods that failed and their traces to the clipboard.
 */
public class CopyFailureListAction extends Action {

	private final Clipboard fClipboard;
	private final TestRunnerViewPart fRunner;

	public CopyFailureListAction(TestRunnerViewPart runner, Clipboard clipboard) {
		super(JUnitMessages.CopyFailureList_action_label);
		fRunner= runner;
		fClipboard= clipboard;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.COPYFAILURELIST_ACTION);
	}

	/*
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		TextTransfer plainTextTransfer = TextTransfer.getInstance();

		try {
			fClipboard.setContents(
					new String[] { getAllFailureTraces() },
					new Transfer[] { plainTextTransfer });
		} catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), JUnitMessages.CopyFailureList_problem, JUnitMessages.CopyFailureList_clipboard_busy))
				run();
		}
	}

	public String getAllFailureTraces() {
		StringBuilder buf= new StringBuilder();
		TestElement[] failures= fRunner.getAllFailures();

		String lineDelim= System.lineSeparator();
		for (TestElement failure : failures) {
			buf.append(failure.getTestName()).append(lineDelim);
			String failureTrace= failure.getTrace();
			if (failureTrace != null) {
				failureTrace = failureTrace.replaceAll("\\r\\n|\\r|\\n", lineDelim); //$NON-NLS-1$
				buf.append(failureTrace);
			}
		}
		return buf.toString();
	}

}
