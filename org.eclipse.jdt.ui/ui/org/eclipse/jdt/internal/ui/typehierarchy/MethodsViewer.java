/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ResourceBundle;import org.eclipse.swt.SWT;import org.eclipse.swt.events.ControlAdapter;import org.eclipse.swt.events.ControlEvent;import org.eclipse.swt.events.DisposeEvent;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Menu;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.jface.action.IMenuListener;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.action.Separator;import org.eclipse.jface.action.ToolBarManager;import org.eclipse.jface.viewers.DoubleClickEvent;import org.eclipse.jface.viewers.IDoubleClickListener;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.ui.IMemento;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;import org.eclipse.jdt.internal.ui.actions.GenerateGroup;import org.eclipse.jdt.internal.ui.actions.OpenSourceReferenceAction;import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


/**
 * Method viewer shows a list of methods of a input type. 
 * Offers filter actions.
 * No dependency to the type hierarchy view
 */
public class MethodsViewer extends TableViewer {
	
	private static final String PREFIX_FILTER_NONPUBLIC= "MethodsViewer.hide_nonpublic.";
	private static final String PREFIX_FILTER_FIELDS= "MethodsViewer.hide_fields.";
	private static final String PREFIX_FILTER_STATIC= "MethodsViewer.hide_static.";
	private static final String PREFIX_VISIBILITY_MENU= "MethodsViewer.visibilitymenu.";	
	
	private static final String TAG_HIDEFIELDS= "hidefields";
	private static final String TAG_HIDESTATIC= "hidestatic";
	private static final String TAG_HIDENONPUBLIC= "hidenonpublic";
	private static final String TAG_SHOWINHERITED= "showinherited";		
	
	private MethodsViewerFilterAction fHideNonPublic;
	private MethodsViewerFilterAction fHideFields;
	private MethodsViewerFilterAction fHideStatic;
	
	private MethodsViewerFilter fFilter;
		
	private OpenSourceReferenceAction fOpen;

	private ShowInheritedMembersAction fShowInheritedMembersAction;

	private ContextMenuGroup[] fStandardGroups;	
	
	public MethodsViewer(Composite parent, IWorkbenchPart part) {
		super(new Table(parent, SWT.MULTI));
		
		final Table table= getTable();
		final TableColumn column= new TableColumn(table, SWT.NULL | SWT.MULTI | SWT.FULL_SELECTION);
		table.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				column.setWidth(table.getSize().x-2*table.getBorderWidth());
			}
		});

		MethodsContentProvider contentProvider= new MethodsContentProvider();
		setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		setContentProvider(contentProvider);
		
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
		
		fOpen= new OpenSourceReferenceAction();
		fOpen.setSelectionProvider(this);
		
		addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (fOpen.isEnabled()) {
					fOpen.run();
				}
			}
		});
				
		fFilter= new MethodsViewerFilter();
		
		// fields	
		fHideFields= new MethodsViewerFilterAction(this, fFilter,  bundle, PREFIX_FILTER_FIELDS, MethodsViewerFilter.FILTER_FIELDS, false);
		fHideFields.setImageDescriptors("lcl16", "fields_co.gif");
		
		// static
		fHideStatic= new MethodsViewerFilterAction(this, fFilter, bundle, PREFIX_FILTER_STATIC, MethodsViewerFilter.FILTER_STATIC, false);
		fHideStatic.setImageDescriptors("lcl16", "static_co.gif");
		
		// non-public
		fHideNonPublic= new MethodsViewerFilterAction(this, fFilter, bundle, PREFIX_FILTER_NONPUBLIC, MethodsViewerFilter.FILTER_NONPUBLIC, false);
		fHideNonPublic.setImageDescriptors("lcl16", "public_co.gif");
		
		
		addFilter(fFilter);
		
		fShowInheritedMembersAction= new ShowInheritedMembersAction(this, false);		
		
		fStandardGroups= new ContextMenuGroup[] {
			new JavaSearchGroup(), new GenerateGroup()
		};
		
		setSorter(new ViewerSorter() {
			public boolean isSorterProperty(Object element, Object property) {
				// doesn't matter, only world changed is used.
				return true;
			}
			public int category(Object element) {
				if (element instanceof IMethod) {
					try {
						if (((IMethod)element).isConstructor()) {
							return 1;
						} else {
							return 2;
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e.getStatus());
					}
				}
				return 0;
			}
		});
			
	}
	
	/**
	 * Show inherited methods
	 */
	public void showInheritedMethods(boolean on) {
		MethodsContentProvider cprovider= (MethodsContentProvider) getContentProvider();
		try {
			cprovider.showInheritedMethods(on);
			JavaElementLabelProvider lprovider= (JavaElementLabelProvider) getLabelProvider();
			if (on) {
				lprovider.turnOn(JavaElementLabelProvider.SHOW_CONTAINER);
			} else {
				lprovider.turnOff(JavaElementLabelProvider.SHOW_CONTAINER);
			}
			refresh();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getControl().getShell(), "Error", "");
		}
	}
	
	public boolean isShowInheritedMethods() {
		return ((MethodsContentProvider) getContentProvider()).isShowInheritedMethods();
	}
	
	
	/**
	 * Saves the state of the filter actions
	 */
	public void saveState(IMemento memento) {
		memento.putString(TAG_HIDEFIELDS, String.valueOf(fFilter.hasFilter(MethodsViewerFilter.FILTER_FIELDS)));
		memento.putString(TAG_HIDESTATIC, String.valueOf(fFilter.hasFilter(MethodsViewerFilter.FILTER_STATIC)));
		memento.putString(TAG_HIDENONPUBLIC, String.valueOf(fFilter.hasFilter(MethodsViewerFilter.FILTER_NONPUBLIC)));
		memento.putString(TAG_SHOWINHERITED, String.valueOf(isShowInheritedMethods()));
	}

	/**
	 * Restores the state of the filter actions
	 */	
	public void restoreState(IMemento memento) {
		boolean set= Boolean.valueOf(memento.getString(TAG_HIDEFIELDS)).booleanValue();
		fFilter.setFilter(MethodsViewerFilter.FILTER_FIELDS, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDESTATIC)).booleanValue();
		fFilter.setFilter(MethodsViewerFilter.FILTER_STATIC, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDENONPUBLIC)).booleanValue();
		fFilter.setFilter(MethodsViewerFilter.FILTER_NONPUBLIC, set);		
		
		fHideFields.updateState();
		fHideStatic.updateState();
		fHideNonPublic.updateState();
		
		set= Boolean.valueOf(memento.getString(TAG_SHOWINHERITED)).booleanValue();
		showInheritedMethods(set);
		
		fShowInheritedMembersAction.updateState();
	}
		
	
		
	/**
	 * Attaches a contextmenu listener to the table
	 */
	public void setMenuListener(IMenuListener menuListener) {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(menuListener);
		Menu menu= menuMgr.createContextMenu(getTable());
		getTable().setMenu(menu);		
	}	
		
	protected void handleDispose(DisposeEvent event) {
		Menu menu= getTable().getMenu();
		if (menu != null)
			menu.dispose();
		super.handleDispose(event);
	}
	
	/**
	 * Fills up the context menu with items for the method viewer
	 * Should be called by the creator of the context menu
	 */	
	public void contributeToContextMenu(IMenuManager menu) {
		
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpen);	
		ContextMenuGroup.add(menu, fStandardGroups, this);
	}

	/**
	 * Fills up the tool bar with items for the method viewer
	 * Should be called by the creator of the tool bar
	 */
	public void contributeToToolBar(ToolBarManager tbm) {
		tbm.add(fShowInheritedMembersAction);
		tbm.add(new Separator());
		tbm.add(fHideFields);
		tbm.add(fHideStatic);
		tbm.add(fHideNonPublic);
		//tbm.update(true);
	}

}