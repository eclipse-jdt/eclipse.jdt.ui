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
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IViewPart;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class JavaUIHelp {

	public static void setHelp(StructuredViewer viewer, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(viewer, contextId);
		viewer.getControl().addHelpListener(listener);
	}

	public static void setHelp(JavaEditor editor, StyledText text, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(editor, contextId);
		text.addHelpListener(listener);
	}
	
	public static IContextProvider getHelpContextProvider(IViewPart part, String contextId) {
		ISelection selection= part.getViewSite().getSelectionProvider().getSelection();
		if (!(selection instanceof StructuredSelection)) 
			return null;
		Object[] elements= ((StructuredSelection)selection).toArray();
		if (elements.length == 0)
			return null;
		return new JavaViewerContextProvider(contextId, elements);
	}

	private static class JavaUIHelpListener implements HelpListener {

		private StructuredViewer fViewer;
		private String fContextId;
		private JavaEditor fEditor;

		public JavaUIHelpListener(StructuredViewer viewer, String contextId) {
			fViewer= viewer;
			fContextId= contextId;
		}

		public JavaUIHelpListener(JavaEditor editor, String contextId) {
			fContextId= contextId;
			fEditor= editor;
		}

		/*
		 * @see HelpListener#helpRequested(HelpEvent)
		 * 
		 */
		public void helpRequested(HelpEvent e) {
			try {
				Object[] selected= null;
				if (fViewer != null) {
					ISelection selection= fViewer.getSelection();
					if (selection instanceof IStructuredSelection) {
						selected= ((IStructuredSelection)selection).toArray();
					}
				} else if (fEditor != null) {
					IJavaElement input= SelectionConverter.getInput(fEditor);
					if (ActionUtil.isOnBuildPath(input)) {
						selected= SelectionConverter.codeResolve(fEditor);
					}
				}
				JavadocHelpContext.displayHelp(fContextId, selected);
			} catch (CoreException x) {
				JavaPlugin.log(x);
			}
		}
	}

	private static class JavaViewerContextProvider implements IContextProvider {
		private String fId;
		private Object[] fSelected;
		public JavaViewerContextProvider(String id, Object[] selected) {
			fId= id;
			fSelected= selected;
		}
		public int getContextChangeMask() {
			return SELECTION;
		}
		public IContext getContext(Object target) {
			IContext context= HelpSystem.getContext(fId);
			if (context != null) {
				if (fSelected != null && fSelected.length > 0) {
					try {
						context= new JavadocHelpContext(context, fSelected);
					} catch (CoreException e) {
						JavaPlugin.log(e);
					}
				}
			}
			return context;
		}
		public String getSearchExpression(Object target) {
			return null;
		}
	}
}