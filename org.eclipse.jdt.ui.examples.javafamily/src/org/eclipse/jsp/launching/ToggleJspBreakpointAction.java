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
package org.eclipse.jsp.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * ToggleJspBreakpointAction
 */
public class ToggleJspBreakpointAction extends Action implements IAction {
	
	private ITextEditor fEditor;
	private IVerticalRulerInfo fRulerInfo;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] breakpoints = manager.getBreakpoints();
		IResource resource = getResource();
		int lineNumber = fRulerInfo.getLineOfLastMouseButtonActivity() + 1;
		for (int i = 0; i < breakpoints.length; i++) {
			IBreakpoint bp = breakpoints[i];
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
			JDIDebugModel.createStratumBreakpoint(resource, null, resource.getName(), null, null, lineNumber, -1, -1, 0, true, null); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param editor
	 * @param rulerInfo
	 */
	public ToggleJspBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		super(LaunchingMessages.getString("ToggleJspBreakpointAction.2")); //$NON-NLS-1$
		fEditor = editor;
		fRulerInfo = rulerInfo; 
	}

	protected IResource getResource() {
		IEditorInput input= fEditor.getEditorInput();
		IResource resource= (IResource) input.getAdapter(IFile.class);
		if (resource == null) {
			resource= (IResource) input.getAdapter(IResource.class);
		}
		return resource;
	}
}
