/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ResourceBundle;import org.eclipse.swt.SWT;import org.eclipse.swt.events.ControlAdapter;import org.eclipse.swt.events.ControlEvent;import org.eclipse.swt.events.DisposeEvent;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Menu;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.jface.action.IAction;import org.eclipse.jface.action.IMenuListener;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.action.Separator;import org.eclipse.jface.action.ToolBarManager;import org.eclipse.jface.viewers.DoubleClickEvent;import org.eclipse.jface.viewers.IDoubleClickListener;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;import org.eclipse.jdt.internal.ui.actions.GenerateGroup;import org.eclipse.jdt.internal.ui.actions.OpenSourceReferenceAction;import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


/**
 * Method viewer shows a list of methods of a input type. 
 * Offers filter actions.
 * No dependency to the type hierarchy view
 */
public class MethodsViewer extends TableViewer {
	
	private static final String PREFIX_FILTER_PRIVATE= "MethodsViewer.show_private.";
	private static final String PREFIX_FILTER_PROTECTED= "MethodsViewer.show_protected.";
	private static final String PREFIX_FILTER_PUBLIC= "MethodsViewer.show_public.";
	private static final String PREFIX_FILTER_DEFAULT= "MethodsViewer.show_default.";
	private static final String PREFIX_FILTER_FIELDS= "MethodsViewer.show_fields.";
	private static final String PREFIX_FILTER_STATIC= "MethodsViewer.show_static.";
	private static final String PREFIX_VISIBILITY_MENU= "MethodsViewer.visibilitymenu.";	
	
	
	private MethodsViewerFilterAction fShowPrivate;
	private MethodsViewerFilterAction fShowProtected;
	private MethodsViewerFilterAction fShowPublic;
	private MethodsViewerFilterAction fShowDefault;
	private MethodsViewerFilterAction fShowFields;
	private MethodsViewerFilterAction fShowStatic;
		
	private OpenSourceReferenceAction fOpen;

	private IAction fShowInheritedMembersAction;

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
				
		MethodsViewerFilter filter= new MethodsViewerFilter();
		
		// fields	
		fShowFields= new MethodsViewerFilterAction(this, filter,  bundle, PREFIX_FILTER_FIELDS, MethodsViewerFilter.FILTER_FIELDS, false);
		//fShowFields.setImageDescriptor(JavaPluginImages.DESC_LCL_SHOW_FIELDS);
		fShowFields.setImageDescriptors("lcl16", "fields_co.gif");
		
		// static
		fShowStatic= new MethodsViewerFilterAction(this, filter, bundle, PREFIX_FILTER_STATIC, MethodsViewerFilter.FILTER_STATIC, true);
		//fShowStatic.setImageDescriptor(JavaPluginImages.DESC_LCL_SHOW_STATIC);
		fShowStatic.setImageDescriptors("lcl16", "static_co.gif");
		
		// private
		fShowPrivate= new MethodsViewerFilterAction(this, filter, bundle, PREFIX_FILTER_PRIVATE, MethodsViewerFilter.FILTER_PRIVATE, true);
		fShowPrivate.setImageDescriptor(JavaPluginImages.DESC_MISC_PRIVATE);
		
		// protected
		fShowProtected= new MethodsViewerFilterAction(this, filter, bundle, PREFIX_FILTER_PROTECTED, MethodsViewerFilter.FILTER_PROTECTED, true);
		fShowProtected.setImageDescriptor(JavaPluginImages.DESC_MISC_PROTECTED);

		// default
		fShowDefault= new MethodsViewerFilterAction(this, filter, bundle, PREFIX_FILTER_DEFAULT, MethodsViewerFilter.FILTER_DEFAULT, true);
		fShowDefault.setImageDescriptor(JavaPluginImages.DESC_MISC_DEFAULT);

		// public
		fShowPublic= new MethodsViewerFilterAction(this, filter, bundle, PREFIX_FILTER_PUBLIC, MethodsViewerFilter.FILTER_PUBLIC, true);
		fShowPublic.setImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC);
		
		addFilter(filter);
		
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
		
		// Viewer setup stuff.
		String label= JavaPlugin.getResourceString(PREFIX_VISIBILITY_MENU + "label");
		MenuManager submenu= new MenuManager(label);	
		submenu.add(fShowFields);
		submenu.add(fShowStatic);
		submenu.add(new Separator());	
		submenu.add(fShowPublic);
		submenu.add(fShowProtected);
		submenu.add(fShowPrivate);
		submenu.add(fShowDefault);
		menu.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, submenu);
		
		menu.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fShowInheritedMembersAction);
	}

	/**
	 * Fills up the tool bar with items for the method viewer
	 * Should be called by the creator of the tool bar
	 */
	public void contributeToToolBar(ToolBarManager tbm) {
		tbm.add(fShowInheritedMembersAction);
		tbm.add(new Separator());
		tbm.add(fShowFields);
		tbm.add(fShowStatic);
		tbm.add(new Separator());
		tbm.add(fShowPublic);
		tbm.add(fShowProtected);
		tbm.add(fShowPrivate);
		tbm.add(fShowDefault);
		//tbm.update(true);
	}

}