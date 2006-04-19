/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor;

public class SurroundWithTemplateMenuAction implements IWorkbenchWindowPulldownDelegate2 {

	private Menu fMenu;
	private JavaEditor fEditor;
	
	public SurroundWithTemplateMenuAction() {}
	
	public SurroundWithTemplateMenuAction(JavaEditor editor) {
		fEditor= editor;
	}

	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}
	
	public void fillContextMenu(IMenuManager menu) {
		IAction[] actions= getActions(fEditor);
		
		if (actions == null)
			return;
		
		MenuManager subMenu = new MenuManager(ActionMessages.SurroundWithTemplateMenuAction_SurroundWithTemplateSubMenuName, "org.eclipse.jdt.ui.surround.with.template.menu"); //$NON-NLS-1$
		for (int i= 0; i < actions.length; i++) {
			subMenu.add(actions[i]);
		}
		menu.appendToGroup(GenerateActionGroup.GROUP_CODE, subMenu);
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		setMenu(null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(IWorkbenchWindow window) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		// Default do nothing - just a menu
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// Default do nothing
	}
	
	protected void fillMenu(Menu menu) {
		JavaEditor editor;
		
		IEditorPart activeEditor= JavaPlugin.getActivePage().getActiveEditor();
		if (activeEditor instanceof JavaEditor) {
			editor= (JavaEditor)activeEditor;
		} else {
			editor= null;
		}
		
		IAction[] actions= getActions(editor);
		
		if (actions == null || actions.length == 0) {
			Action action= new Action(ActionMessages.SurroundWithTemplateMenuAction_NoneApplicable) {
				public void run() {
					//Do nothing
				}
			};
			action.setEnabled(false);
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(menu, -1);
		} else {
			for (int i= 0; i < actions.length; i++) {
				ActionContributionItem item= new ActionContributionItem(actions[i]);
				item.fill(menu, -1);
			}
		}
	}

	protected void initMenu() {
		fMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				MenuItem[] items = m.getItems();
				for (int i=0; i < items.length; i++) {
					items[i].dispose();
				}
				fillMenu(m);
			}
		});
	}
	
	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}
	
	private IAction[] getActions(JavaEditor editor) {
		if (editor == null)
			return null;
		
		ISelectionProvider selectionProvider= editor.getSelectionProvider();
		if (selectionProvider == null)
			return null;
		
		ISelection selection= selectionProvider.getSelection();
		if (!(selection instanceof ITextSelection))
			return null;
		
		ITextSelection textSelection= (ITextSelection)selection;
		if (textSelection.getLength() == 0)
			return null;
		
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(editor.getEditorInput());
		if (cu == null)
			return null;
		
		QuickTemplateProcessor quickTemplateProcessor= new QuickTemplateProcessor();
		IInvocationContext context= new AssistContext(cu, textSelection.getOffset(), textSelection.getLength());
		
		try {
			IJavaCompletionProposal[] proposals= quickTemplateProcessor.getAssists(context, null);
			if (proposals == null || proposals.length == 0)
				return null;
			
			return getActionsFromProposals(proposals, context.getSelectionOffset(), editor.getViewer());
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	private IAction[] getActionsFromProposals(IJavaCompletionProposal[] proposals, final int offset, final ITextViewer viewer) {
		List result= new ArrayList();
		
		for (int i= 0, j= 1; i < proposals.length; i++) {
			if (proposals[i] instanceof ICompletionProposalExtension2) {
				final IJavaCompletionProposal proposal= proposals[i];
				
				StringBuffer actionName= new StringBuffer();
				if (j<10) {
					actionName.append('&').append(j).append(' ');
				}
				actionName.append(proposals[i].getDisplayString());
				
				Action action= new Action(actionName.toString()) {
					/**
					 * {@inheritDoc} 
					 */
					public void run() {
						applyProposal(proposal, viewer, (char)0, 0, offset);
					}					
				};
				
				result.add(action);
				j++;
			}
		}
		if (result.size() == 0)
			return null;
		
		return (IAction[])result.toArray(new IAction[result.size()]);
	}
	
	private void applyProposal(ICompletionProposal proposal, ITextViewer viewer, char trigger, int stateMask, final int offset) {
		Assert.isTrue(proposal instanceof ICompletionProposalExtension2);
		
		IRewriteTarget target= null;
		IEditingSupportRegistry registry= null;
		IEditingSupport helper= new IEditingSupport() {

			public boolean isOriginator(DocumentEvent event, IRegion focus) {
				return focus.getOffset() <= offset && focus.getOffset() + focus.getLength() >= offset;
			}

			public boolean ownsFocusShell() {
				return false;
			}

		};
		
		try {
			IDocument document= viewer.getDocument();

			if (viewer instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) viewer;
				target= extension.getRewriteTarget();
			}

			if (target != null)
				target.beginCompoundChange();

			if (viewer instanceof IEditingSupportRegistry) {
				registry= (IEditingSupportRegistry) viewer;
				registry.register(helper);
			}

			((ICompletionProposalExtension2)proposal).apply(viewer, trigger, stateMask, offset);
			
			Point selection= proposal.getSelection(document);
			if (selection != null) {
				viewer.setSelectedRange(selection.x, selection.y);
				viewer.revealRange(selection.x, selection.y);
			}
		} finally {
			if (target != null)
				target.endCompoundChange();

			if (registry != null)
				registry.unregister(helper);
		}
	}
}