/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GenerateGroup;
import org.eclipse.jdt.internal.ui.actions.OpenSourceReferenceAction;
import org.eclipse.jdt.internal.ui.actions.ShowInPackageViewAction;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;
import org.eclipse.jdt.internal.ui.viewsupport.SeverityItemMapper;
import org.eclipse.jdt.internal.ui.wizards.NewGroup;

 
public abstract class TypeHierarchyViewer extends TreeViewer {
	
	private OpenSourceReferenceAction fOpen;
	private ShowInPackageViewAction fShowInPackageViewAction;
	private ContextMenuGroup[] fStandardGroups;
	
	private SeverityItemMapper fSeverityItemMapper;
	
	private HashMap fPathToWidget;
	
	public TypeHierarchyViewer(Composite parent, IContentProvider contentProvider, IWorkbenchPart part) {
		super(new Tree(parent, SWT.SINGLE));
		
		fSeverityItemMapper= new SeverityItemMapper();
		
		fPathToWidget= new HashMap();
		
		setContentProvider(contentProvider);
				
		setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
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
		fShowInPackageViewAction= new ShowInPackageViewAction(part.getSite(), this);
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
	
	protected void handleDispose(DisposeEvent event) {
		Menu menu= getTree().getMenu();
		if (menu != null)
			menu.dispose();
		super.handleDispose(event);
	}


	/**
	 * Fills up the context menu with items for the hierarchy viewer
	 * Should be called by the creator of the context menu
	 */	
	public void contributeToContextMenu(IMenuManager menu) {
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpen);
		if (fShowInPackageViewAction.canOperateOn())
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fShowInPackageViewAction);
		ContextMenuGroup.add(menu, fStandardGroups, this);
	}

	/**
	 * Fills up the tool bar with items for the hierarchy viewer
	 * Should be called by the creator of the tool bar
	 */	
	public void contributeToToolBar(ToolBarManager tbm) {
	}
	
	/**
	 * Set the member filter
	 */
	public void setMemberFilter(IMember[] memberFilter) {
		getHierarchyContentProvider().setMemberFilter(memberFilter);
	}	
	
	
	private IResource getMappingResource(IMember member) {
		try {
			int type= member.getElementType();
			if (type == IJavaElement.TYPE || type == IJavaElement.METHOD || type == IJavaElement.INITIALIZER) {
				ICompilationUnit cu= member.getCompilationUnit();
				if (cu != null) {
					return cu.getCorrespondingResource();
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}			
		return null;
	}	
	
	/**
	 * @see TreeViewer#mapElement
	 */
	protected void mapElement(Object element, Widget widget) {
		super.mapElement(element, widget);
		IResource res= getMappingResource((IMember)element);
		if (res != null && widget instanceof Item) {
			fSeverityItemMapper.addToMap(res, (Item)widget);
		}
	}

	/**
	 * @see TreeViewer#unmapElement
	 */	
	protected void unmapElement(Object element) {
		super.unmapElement(element);
		IResource res= getMappingResource((IMember)element);
		if (res != null) {
			fSeverityItemMapper.removeFromMap(res, element);
		}
	}

	/**
	 * @see TreeViewer#unmapAllElements
	 */	
	protected void unmapAllElements() {
		super.unmapAllElements();
		fSeverityItemMapper.clearMap();
	}

	/**
	 * @called when severities changed
	 */	
	public void severitiesChanged(Collection changed) {
		fSeverityItemMapper.severitiesChanged(changed, (ILabelProvider)getLabelProvider());
	}
	
	
	/**
	 * Returns true if the hierarchy contains elements.
	 * With member filtering it is possible that no elements are visible
	 */ 
	public boolean containsElements() {
		return ((IStructuredContentProvider)getContentProvider()).getElements(null).length > 0;
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
	
	/**
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