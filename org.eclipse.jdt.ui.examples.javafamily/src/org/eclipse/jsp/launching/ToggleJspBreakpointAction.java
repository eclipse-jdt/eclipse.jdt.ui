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
package org.eclipse.jsp.launching;

import org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.Action;

import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.IEditorInput;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * ToggleJspBreakpointAction
 */
public class ToggleJspBreakpointAction extends Action {

	private final ITextEditor fEditor;
	private final IVerticalRulerInfo fRulerInfo;

	@Override
	public void run() {
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		IResource resource = getResource();
		int lineNumber = fRulerInfo.getLineOfLastMouseButtonActivity() + 1;
		for (IBreakpoint bp : manager.getBreakpoints()) {
			if (bp instanceof IJavaStratumLineBreakpoint) {
				IJavaStratumLineBreakpoint breakpoint = (IJavaStratumLineBreakpoint)bp;
				if (breakpoint.getMarker().getResource().equals(resource)) {
					try {
						if (breakpoint.getLineNumber() == lineNumber) {
							// remove
							breakpoint.delete();
							return;
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		createBreakpoint();
	}

	protected void createBreakpoint() {
		IResource resource = getResource();
		int lineNumber = fRulerInfo.getLineOfLastMouseButtonActivity() + 1;
		try {
			JDIDebugModel.createStratumBreakpoint(resource, null, resource.getName(), null, null, lineNumber, -1, -1, 0, true, null); //
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public ToggleJspBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		super(LaunchingMessages.ToggleJspBreakpointAction_2);
		fEditor = editor;
		fRulerInfo = rulerInfo;
	}

	protected IResource getResource() {
		IEditorInput input= fEditor.getEditorInput();
		IResource resource= input.getAdapter(IFile.class);
		if (resource == null) {
			resource= input.getAdapter(IResource.class);
		}
		return resource;
	}
}
