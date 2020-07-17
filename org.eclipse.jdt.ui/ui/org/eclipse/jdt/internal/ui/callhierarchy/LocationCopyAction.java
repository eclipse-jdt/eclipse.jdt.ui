/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;


import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;

/**
 * Copies the selection from the location viewer.
 */
class LocationCopyAction extends Action {
	private final Clipboard fClipboard;
	private final IViewSite fViewSite;
	private final LocationViewer fLocationViewer;

	LocationCopyAction(IViewSite viewSite, Clipboard clipboard, LocationViewer locationViewer) {
		fClipboard= clipboard;
		fViewSite= viewSite;
		fLocationViewer= locationViewer;

		setText(CallHierarchyMessages.LocationCopyAction_copy);
		setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
		setEnabled(!fLocationViewer.getSelection().isEmpty());

		locationViewer.addSelectionChangedListener(event -> setEnabled(! event.getSelection().isEmpty()));
	}

	@Override
	public void run() {
		IStructuredSelection selection= (IStructuredSelection) fLocationViewer.getSelection();
		StringBuilder buf= new StringBuilder();
		for (Object name : selection) {
			CallLocation location= (CallLocation) name;
			buf.append(location.getLineNumber()).append('\t').append(location.getCallText());
			buf.append('\n');
		}
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		try {
			fClipboard.setContents(
					new String[]{ CopyCallHierarchyAction.convertLineTerminators(buf.toString()) },
					new Transfer[]{ plainTextTransfer });
		} catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(fViewSite.getShell(), CallHierarchyMessages.CopyCallHierarchyAction_problem, CallHierarchyMessages.CopyCallHierarchyAction_clipboard_busy))
				run();
		}
	}
}