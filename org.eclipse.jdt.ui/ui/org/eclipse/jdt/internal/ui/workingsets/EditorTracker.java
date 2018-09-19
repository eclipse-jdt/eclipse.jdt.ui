/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;


public abstract class EditorTracker implements IWindowListener, IPageListener, IPartListener {

	//--- Window listener

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}
	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}
	@Override
	public void windowClosed(IWorkbenchWindow window) {
		window.removePageListener(this);
	}
	@Override
	public void windowOpened(IWorkbenchWindow window) {
		window.addPageListener(this);
	}

	//---- IPageListener

	@Override
	public void pageActivated(IWorkbenchPage page) {
	}
	@Override
	public void pageClosed(IWorkbenchPage page) {
		page.removePartListener(this);
	}
	@Override
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(this);
	}

	//---- Part Listener

	@Override
	public void partActivated(IWorkbenchPart part) {
	}
	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
	}
	@Override
	public void partClosed(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorClosed((IEditorPart)part);
		}
	}
	@Override
	public void partDeactivated(IWorkbenchPart part) {
	}
	@Override
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorOpened((IEditorPart)part);
		}
	}

	public abstract void editorOpened(IEditorPart part);

	public abstract void editorClosed(IEditorPart part);

}
