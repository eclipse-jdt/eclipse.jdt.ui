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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * @author Thomas Mäder
 *
 */
public class JavaSearchResultPage extends AbstractTextSearchViewPage {
	private NewSearchViewActionGroup fActionGroup;
	private JavaSearchContentProvider fContentProvider;
	private SortAction fCurrentSortAction;
	private SortAction fSortByNameAction;
	private SortAction fSortByParentName;
	private SortAction fSortByPathAction;
	
	public JavaSearchResultPage() {
		fSortByNameAction= new SortAction(SearchMessages.getString("JavaSearchResultPage.sortByName"), this, JavaSearchResultLabelProvider.SHOW_ELEMENT_CONTAINER); //$NON-NLS-1$
		fSortByPathAction= new SortAction(SearchMessages.getString("JavaSearchResultPage.sortByPath"), this, JavaSearchResultLabelProvider.SHOW_PATH); //$NON-NLS-1$
		fSortByParentName= new SortAction(SearchMessages.getString("JavaSearchResultPage.sortByParentName"), this, JavaSearchResultLabelProvider.SHOW_CONTAINER_ELEMENT); //$NON-NLS-1$
		fCurrentSortAction= fSortByNameAction;
	}

	public void setViewPart(ISearchResultViewPart part) {
		// TODO Auto-generated method stub
		super.setViewPart(part);
		fActionGroup= new NewSearchViewActionGroup(part);
	}
	
	public void showMatch(Object element, int offset, int length) throws PartInitException {
		IEditorPart editor= null;
		if (element instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) element;
			try {
				editor= EditorUtility.openInEditor(javaElement, false);
			} catch (PartInitException e1) {
				return;
			} catch (JavaModelException e1) {
				return;
			}
		} else if (element instanceof IFile) {
			editor= IDE.openEditor(JavaPlugin.getActivePage(), (IFile) element, false);
		}
		if (!(editor instanceof ITextEditor))
			return;
		ITextEditor textEditor= (ITextEditor) editor;
		textEditor.selectAndReveal(offset, length);
	}
	
	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		addSortActions(mgr);
		fActionGroup.setContext(new ActionContext(getSite().getSelectionProvider().getSelection()));
		fActionGroup.fillContextMenu(mgr);
	}
	
	private void addSortActions(IMenuManager mgr) {
		if (!isFlatMode())
			return;
		MenuManager sortMenu= new MenuManager(SearchMessages.getString("JavaSearchResultPage.sortBylabel")); //$NON-NLS-1$
		sortMenu.add(fSortByNameAction);
		sortMenu.add(fSortByPathAction);
		sortMenu.add(fSortByParentName);
		
		fSortByNameAction.setChecked(fCurrentSortAction == fSortByNameAction);
		fSortByPathAction.setChecked(fCurrentSortAction == fSortByPathAction);
		fSortByParentName.setChecked(fCurrentSortAction == fSortByParentName);
		
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
	}

	public void dispose() {
		fActionGroup.dispose();
		super.dispose();
	}
	
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null)
			fContentProvider.elementsChanged(objects);
	}

	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	protected void configureViewer(StructuredViewer viewer) {
		viewer.setLabelProvider(new DelegatingLabelProvider(this, new JavaSearchResultLabelProvider()));
		if (viewer instanceof TreeViewer) {
			fContentProvider= new JavaSearchTreeContentProvider((TreeViewer) viewer);
			viewer.setContentProvider(fContentProvider);
		} else {
			fContentProvider=new JavaSearchTableContentProvider((TableViewer) viewer);
			viewer.setContentProvider(fContentProvider);
			setSortOrder(fCurrentSortAction);
		}
	}

	public void setSortOrder(SortAction action) {
		fCurrentSortAction= action;
		StructuredViewer viewer= getViewer();
		DelegatingLabelProvider lpWrapper= (DelegatingLabelProvider) viewer.getLabelProvider();
		((JavaSearchResultLabelProvider)lpWrapper.getLabelProvider()).setOrder(action.getSortOrder());
		if (action.getSortOrder() == JavaSearchResultLabelProvider.SHOW_ELEMENT_CONTAINER) {
			viewer.setSorter(new NameSorter());
		} else if (action.getSortOrder() == JavaSearchResultLabelProvider.SHOW_PATH) {
			viewer.setSorter(new PathSorter());
		} else
			viewer.setSorter(new ParentSorter());
	}

}
