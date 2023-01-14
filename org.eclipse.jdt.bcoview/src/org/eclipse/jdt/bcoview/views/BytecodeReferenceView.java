/*******************************************************************************
 * Copyright (c) 2023 Eugene Kuleshov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eugene Kuleshov - initial API and implementation
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.views;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;
import org.eclipse.jdt.bcoview.preferences.BCOConstants;
import org.eclipse.jdt.bcoview.ui.actions.DefaultToggleAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class BytecodeReferenceView extends ViewPart implements IPartListener2, ISelectionListener {

	private static final String VIEW_ID = "org.eclipse.jdt.bcoview.views.BytecodeOutlineView"; //$NON-NLS-1$

	private Browser browser;

	private DefaultToggleAction linkWithViewAction;

	private boolean linkWithView;

	public BytecodeReferenceView() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		browser = new Browser(parent, SWT.BORDER);
		final IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
		linkWithView = BytecodeOutlinePlugin.getDefault().getPreferenceStore().getBoolean(BCOConstants.LINK_REF_VIEW_TO_EDITOR);
		linkWithViewAction = new DefaultToggleAction(BCOConstants.LINK_REF_VIEW_TO_EDITOR) {
			@Override
			public void run(boolean newState) {
				linkWithView = newState;
				if (linkWithView) {
					ISelectionService selectionService = workbenchWindow.getSelectionService();
					try {
						IViewPart part = workbenchWindow.getActivePage().showView(VIEW_ID);
						ISelection selection = selectionService.getSelection(VIEW_ID);
						selectionChanged(part, selection);
					} catch (PartInitException e) {
						BytecodeOutlinePlugin.log(e, IStatus.ERROR);
					}
				}
			}
		};
		final IActionBars bars = getViewSite().getActionBars();
		final IToolBarManager tmanager = bars.getToolBarManager();
		tmanager.add(linkWithViewAction);
		shouDefaultEmptyPage();
		workbenchWindow.getPartService().addPartListener(this);
	}

	@Override
	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		browser.dispose();
		browser = null;
		linkWithViewAction = null;
		super.dispose();
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		//
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		//
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		//
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		//
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// WORKAROUND  - sometimes Eclipse does not invoke partVisible(),
		// but only partOpened()...
		partVisible(partRef);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getSite().getId())) {
			getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getSite().getId())) {
			IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
			ISelectionService selectionService = workbenchWindow.getSelectionService();
			selectionService.addPostSelectionListener(this);

			// perform initialization with already existing selection (if any)
			ISelection selection = selectionService.getSelection(VIEW_ID);
			if (selection != null) {
				IViewReference viewReference = workbenchWindow.getActivePage().findViewReference(VIEW_ID);
				if (viewReference != null) {
					selectionChanged(viewReference.getView(false), selection);
				}
			}
		}
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		//
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		boolean isBytecodePartSelection = part instanceof IBytecodePart;
		if (!linkWithView || !isBytecodePartSelection) {
			return;
		}
		int line = -1;
		if (selection instanceof ITextSelection) {
			line = ((ITextSelection) selection).getStartLine();
		} else if (selection instanceof IStructuredSelection) {
			IStructuredSelection sselection = (IStructuredSelection) selection;
			int size = sselection.size();
			if (size == 1 && sselection.getFirstElement() instanceof Integer) {
				line = ((Integer) sselection.getFirstElement()).intValue();
			}
		}

		if (line < 0) {
			shouDefaultEmptyPage();
			return;
		}
		int opcode = ((IBytecodePart) part).getBytecodeInstructionAtLine(line);
		StringBuilder helpFor = HelpUtils.getOpcodeHelpFor(opcode);
		if (helpFor.length() > 0) {
			browser.setText(helpFor.toString());
		} else {
			shouDefaultEmptyPage();
		}
	}

	private void shouDefaultEmptyPage() {
		browser.setUrl(HelpUtils.getHelpIndex().toString());
	}

}

