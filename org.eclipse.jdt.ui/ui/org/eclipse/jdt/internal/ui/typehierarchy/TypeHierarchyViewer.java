/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GenerateGroup;
import org.eclipse.jdt.internal.ui.actions.OpenSourceReferenceAction;
import org.eclipse.jdt.internal.ui.actions.ShowInPackageViewAction;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;
import org.eclipse.jdt.internal.ui.viewsupport.IProblemChangedListener;
import org.eclipse.jdt.internal.ui.viewsupport.MarkerErrorTickProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.jdt.internal.ui.wizards.NewGroup;
 
public abstract class TypeHierarchyViewer extends ProblemTreeViewer implements IProblemChangedListener {
	
	private OpenSourceReferenceAction fOpen;
	private ShowInPackageViewAction fShowInPackageViewAction;
	private ContextMenuGroup[] fStandardGroups;
			
	public TypeHierarchyViewer(Composite parent, IContentProvider contentProvider, ILabelProvider lprovider, IWorkbenchPart part) {
		super(new Tree(parent, SWT.SINGLE));
				
		setContentProvider(contentProvider);
		setLabelProvider(lprovider);
		setSorter(new ViewerSorter() {
			public boolean isSorterProperty(Object element, Object property) {
				return true;
			}
		
			public int category(Object element) {
				if (element instanceof IType) {
					IType type= (IType)element;
					try {
						return (((IType)element).isInterface()) ? 2 : 1;
					} catch (JavaModelException e) {
					}
				} else if (element instanceof IMember) {
					return 0;
				}
				return 3;
			}
		});
		
		fOpen= new OpenSourceReferenceAction(this);
		addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				fOpen.run();
			}
		});
		fShowInPackageViewAction= new ShowInPackageViewAction();
		fStandardGroups= new ContextMenuGroup[] {
			new JavaSearchGroup(), new NewGroup(), new GenerateGroup()
		};		
	}
	
	/**
	 * Attaches a contextmenu listener to the tree
	 */
	public void initContextMenu(IMenuListener menuListener, String popupId, IWorkbenchPartSite viewSite) {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(menuListener);
		Menu menu= menuMgr.createContextMenu(getTree());
		getTree().setMenu(menu);
		viewSite.registerContextMenu(popupId, menuMgr, this);
	}

	/**
	 * Fills up the context menu with items for the hierarchy viewer
	 * Should be called by the creator of the context menu
	 */	
	public void contributeToContextMenu(IMenuManager menu) {
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpen);
		// XXX need to decide when to contribute the Show in PackagesView action
		// if (fShowInPackageViewAction.canOperateOn())
		menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fShowInPackageViewAction);
		ContextMenuGroup.add(menu, fStandardGroups, this);
	}

	/**
	 * Set the member filter
	 */
	public void setMemberFilter(IMember[] memberFilter) {
		getHierarchyContentProvider().setMemberFilter(memberFilter);
	}

	/**
	 * Returns if method filtering is enabled.
	 */	
	public boolean isMethodFiltering() {
		return getHierarchyContentProvider().getMemberFilter() != null;
	}
		
	/**
	 * Returns true if the hierarchy contains elements. Returns one of them
	 * With member filtering it is possible that no elements are visible
	 */ 
	public Object containsElements() {
		Object[] elements=  ((IStructuredContentProvider)getContentProvider()).getElements(null);
		if (elements.length > 0) {
			return elements[0];
		}
		return null;
	}
		
	/**
	 * Returns true if the hierarchy contains element the element.
	 */ 
	public boolean isElementShown(Object element) {
		return findItem(element) != null;
	}
	
	/**
	 * Updates the content of this viewer: refresh and expanding the tree in the way wanted.
	 */
	public abstract void updateContent();	
	
	/**
	 * Returns the title for the current view
	 */
	public abstract String getTitle();
	
	/*
	 * @see StructuredViewer#setContentProvider
	 * Content provider must be of type TypeHierarchyContentProvider
	 */
	public void setContentProvider(IContentProvider cp) {
		Assert.isTrue(cp instanceof TypeHierarchyContentProvider);
		super.setContentProvider(cp);
	}

	protected TypeHierarchyContentProvider getHierarchyContentProvider() {
		return (TypeHierarchyContentProvider)getContentProvider();
	}
	
}