/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.views;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

import org.eclipse.jdt.core.IJavaElement;

public class EditorListener implements ISelectionListener, IFileBufferListener, IPartListener2 {
	volatile protected BytecodeOutlineView view;

	EditorListener(BytecodeOutlineView view) {
		this.view = view;
	}

	/**
	 * clean view reference
	 */
	public void dispose() {
		this.view = null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(selection instanceof ITextSelection)) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (ssel.isEmpty()) {
					return;
				}
				if (ssel.getFirstElement() instanceof IJavaElement) {
					/*
					 * this may be selection in outline view. If so, the editor selection
					 * would be changed but no event would be sent :(
					 * So we just delay the call and wait for new selection in editor
					 */
					Display display = Display.getDefault();
					// fork
					display.asyncExec(() -> {
						if (view != null) {
							view.checkOpenEditors(true);
						}
					});
				}
			}
			return;
		}
		view.handleSelectionChanged(part, selection);
	}

	@Override
	public void dirtyStateChanged(IFileBuffer buffer, final boolean isDirty) {
		if (!view.isLinkedWithEditor()) {
			return;
		}
		if (isSupportedBuffer(buffer)) {
			// first call set only view flag - cause
			view.handleBufferIsDirty(isDirty);

			// second call will really refresh view
			if (!isDirty) {
				// this one will be called in UI thread after some delay, because we need
				// to wait until the bytecode will be written on disk
				final Runnable runnable2 = () -> view.handleBufferIsDirty(isDirty);
				// this one will be called in UI thread ASAP and allow us to leave
				// current (probably non-UI) thread
				Runnable runnable1 = () -> {
					Display display = Display.getCurrent();
					display.timerExec(1000, runnable2);
				};
				Display display = Display.getDefault();
				display.asyncExec(runnable1);
			}
		}
	}

	private static boolean isSupportedBuffer(IFileBuffer buffer) {
		String fileExtension = buffer.getLocation().getFileExtension();
		// TODO export to properties
		return "java".equals(fileExtension);// || "groovy".equals(fileExtension);  //$NON-NLS-1$
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		view.handlePartHidden(partRef.getPart(false));
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		view.handlePartHidden(partRef.getPart(false));
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		view.handlePartVisible(partRef.getPart(false));
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		view.handlePartVisible(partRef.getPart(false));
	}

	@Override
	public void bufferDisposed(IFileBuffer buffer) {
		// is not used here
	}

	@Override
	public void bufferCreated(IFileBuffer buffer) {
		// is not used here
	}

	@Override
	public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
		// is not used here
	}

	@Override
	public void bufferContentReplaced(IFileBuffer buffer) {
		// is not used here
	}

	@Override
	public void stateChanging(IFileBuffer buffer) {
		// is not used here
	}

	@Override
	public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
		// is not used here
	}

	@Override
	public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
		//is not used here
	}

	@Override
	public void underlyingFileDeleted(IFileBuffer buffer) {
		//is not used here
	}

	@Override
	public void stateChangeFailed(IFileBuffer buffer) {
		//is not used here
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// is not used here
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		// is not used here
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// is not used here
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// is not used here
	}

}
