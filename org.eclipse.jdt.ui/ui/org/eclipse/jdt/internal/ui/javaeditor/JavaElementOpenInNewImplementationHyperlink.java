/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Java element open in new tab implementation hyperlink.
 */
public class JavaElementOpenInNewImplementationHyperlink implements IHyperlink {

	private final IRegion fRegion;
	private final IJavaElement fElement;

	/**
	 * Creates a new Java element open new tab hyperlink for types and methods.
	 *
	 * @param region the region of the link
	 * @param javaElement the element (type or method) to open
	 *            element
	 * @param editor the editor
	 */
	public JavaElementOpenInNewImplementationHyperlink(IRegion region, IJavaElement javaElement, IEditorPart editor) {
		Assert.isNotNull(region);
		Assert.isNotNull(javaElement);
		Assert.isTrue(javaElement instanceof IMethod || javaElement instanceof IType);

		fRegion= region;
		fElement= javaElement;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	@Override
	public String getHyperlinkText() {
		return JavaEditorMessages.JavaElementOpenHyperlink_hyperlinkText;
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	/**
	 * Opens the given implementation hyperlink for types and methods in a new cloned tab.
	 */
	@Override
	public void open() {
		openInNewTab( fElement);
	}

	/**
	 * Opens the method or type in a new cloned tab
	 *
	 * @param javaElement the method or type
	 */
	public static void openInNewTab(final IJavaElement javaElement) {
		try {
			IEditorPart existingEditr = EditorUtility.isOpenInEditor(javaElement);
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (existingEditr == null || window == null) {
				return;
			}
			IWorkbenchPage page= window.getActivePage();
			if (page == null) {
				return;
			}
			IEditorPart target = page.openEditor(
					existingEditr.getEditorInput(),
					existingEditr.getSite().getId(),
					true,
					IWorkbenchPage.MATCH_NONE);

			JavaUI.revealInEditor(target, javaElement);
		} catch (PartInitException e) {
			JavaPlugin.log(e);
		}
	}
}
