/*******************************************************************************
 * Copyright (c) 2023 Zsombor Gegesy
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Zsombor Gegesy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyUI;

/**
 * Provides a hyperlink for method calls, method declarations to open the Call Hierarchy view.
 */
public class OpenCallHierarchyHyperlinkDetector extends JavaElementHyperlinkDetector {

	@Override
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		if (CallHierarchy.isPossibleInputElement(element) && (element instanceof IMember imember)) {
			hyperlinksCollector.add(new OpenCallHierarchyHyperlink(wordRegion, openAction.getSite().getWorkbenchWindow(), imember));
		}
	}

	private static class OpenCallHierarchyHyperlink implements IHyperlink {

		private final IRegion region;

		private final IWorkbenchWindow iWorkbenchWindow;

		private final IMember element;

		public OpenCallHierarchyHyperlink(IRegion region, IWorkbenchWindow iWorkbenchWindow, IMember element) {
			this.region= region;
			this.iWorkbenchWindow= iWorkbenchWindow;
			this.element= element;
		}

		@Override
		public IRegion getHyperlinkRegion() {
			return region;
		}

		@Override
		public String getTypeLabel() {
			return null;
		}

		@Override
		public String getHyperlinkText() {
			return JavaEditorMessages.OpenCallHierarchyHyperlinkDetector_hyperlinkText;
		}

		@Override
		public void open() {
			if (CallHierarchy.isPossibleInputElement(element)) {
				IMember[] members= new IMember[] { element };
				CallHierarchyUI.openSelectionDialog(members, iWorkbenchWindow);
			}
		}

	}
}
