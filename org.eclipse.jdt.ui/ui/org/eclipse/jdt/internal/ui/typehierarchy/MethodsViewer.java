/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GenerateGroup;
import org.eclipse.jdt.internal.ui.actions.OpenJavaElementAction;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.internal.ui.viewsupport.ErrorTickImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementSorter;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;

/**
 * Method viewer shows a list of methods of a input type. 
 * Offers filter actions. 
 * No dependency to the type hierarchy view
 */
public class MethodsViewer extends ProblemTableViewer {
	
	private static final String TAG_HIDEFIELDS= "hidefields"; //$NON-NLS-1$
	private static final String TAG_HIDESTATIC= "hidestatic"; //$NON-NLS-1$
	private static final String TAG_HIDENONPUBLIC= "hidenonpublic"; //$NON-NLS-1$
	private static final String TAG_SHOWINHERITED= "showinherited";		 //$NON-NLS-1$
	private static final String TAG_VERTICAL_SCROLL= "mv_vertical_scroll";		 //$NON-NLS-1$
	
	private static final int LABEL_BASEFLAGS= JavaElementLabels.M_PARAMETER_TYPES;
	
	private MethodsViewerFilterAction[] fFilterActions;
	private MethodsViewerFilter fFilter;
	
	private JavaUILabelProvider fLabelProvider;
		
	private OpenJavaElementAction fOpen;

	private ShowInheritedMembersAction fShowInheritedMembersAction;

	private ContextMenuGroup[] fStandardGroups;	
	
	public MethodsViewer(Composite parent, IWorkbenchPart part) {
		super(new Table(parent, SWT.MULTI));
		
		fLabelProvider= new JavaUILabelProvider(new ErrorTickImageProvider());
		
		MethodsContentProvider contentProvider= new MethodsContentProvider();

		setLabelProvider(fLabelProvider);
		setContentProvider(contentProvider);
				
		fOpen= new OpenJavaElementAction(this);
		
		addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				fOpen.run();
			}
		});
				
		fFilter= new MethodsViewerFilter();
		
		// fields
		String title= TypeHierarchyMessages.getString("MethodsViewer.hide_fields.label"); //$NON-NLS-1$
		String helpContext= IJavaHelpContextIds.FILTER_FIELDS_ACTION;
		MethodsViewerFilterAction hideFields= new MethodsViewerFilterAction(this, title, MethodsViewerFilter.FILTER_FIELDS, helpContext, false);
		hideFields.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.description")); //$NON-NLS-1$
		hideFields.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.tooltip.checked")); //$NON-NLS-1$
		hideFields.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideFields, "fields_co.gif"); //$NON-NLS-1$
		
		// static
		title= TypeHierarchyMessages.getString("MethodsViewer.hide_static.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_STATIC_ACTION;
		MethodsViewerFilterAction hideStatic= new MethodsViewerFilterAction(this, title, MethodsViewerFilter.FILTER_STATIC, helpContext, false);
		hideStatic.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_static.description")); //$NON-NLS-1$
		hideStatic.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_static.tooltip.checked")); //$NON-NLS-1$
		hideStatic.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_static.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideStatic, "static_co.gif"); //$NON-NLS-1$
		
		// non-public
		title= TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_PUBLIC_ACTION;
		MethodsViewerFilterAction hideNonPublic= new MethodsViewerFilterAction(this, title, MethodsViewerFilter.FILTER_NONPUBLIC, helpContext, false);
		hideNonPublic.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.description")); //$NON-NLS-1$
		hideNonPublic.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.tooltip.checked")); //$NON-NLS-1$
		hideNonPublic.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideNonPublic, "public_co.gif"); //$NON-NLS-1$
		
		// order corresponds to order in toolbar
		fFilterActions= new MethodsViewerFilterAction[] { hideFields, hideStatic, hideNonPublic };
		
		addFilter(fFilter);
		
		fShowInheritedMembersAction= new ShowInheritedMembersAction(this, false);
		showInheritedMethods(false);
		
		fStandardGroups= new ContextMenuGroup[] {
			new JavaSearchGroup(), new GenerateGroup()
		};
		
		setSorter(new JavaElementSorter());
	}
	
	/**
	 * Show inherited methods
	 */
	public void showInheritedMethods(boolean on) {
		MethodsContentProvider cprovider= (MethodsContentProvider) getContentProvider();
		try {
			getTable().setRedraw(false);
			cprovider.showInheritedMethods(on);
			fShowInheritedMembersAction.setChecked(on);
			if (on) {
				fLabelProvider.setTextFlags(LABEL_BASEFLAGS | JavaElementLabels.ALL_POST_QUALIFIED);
			} else {
				fLabelProvider.setTextFlags(LABEL_BASEFLAGS);
			}
			refresh();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getControl().getShell(), TypeHierarchyMessages.getString("MethodsViewer.toggle.error.title"), TypeHierarchyMessages.getString("MethodsViewer.toggle.error.message")); //$NON-NLS-2$ //$NON-NLS-1$
		} finally {
			getTable().setRedraw(true);
		}
	}
		
	/*
	 * @see Viewer#inputChanged(Object, Object)
	 */
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
	}
	
	/**
	 * Returns <code>true</code> if inherited methods are shown.
	 */	
	public boolean isShowInheritedMethods() {
		return ((MethodsContentProvider) getContentProvider()).isShowInheritedMethods();
	}

	/**
	 * Filters the method list
	 */	
	public void setMemberFilter(int filterProperty, boolean set) {
		if (set) {
			fFilter.addFilter(filterProperty);
		} else {
			fFilter.removeFilter(filterProperty);
		}
		for (int i= 0; i < fFilterActions.length; i++) {
			if (fFilterActions[i].getFilterProperty() == filterProperty) {
				fFilterActions[i].setChecked(set);
			}
		}
		refresh();
	}

	/**
	 * Returns <code>true</code> if the given filter is set.
	 */	
	public boolean hasMemberFilter(int filterProperty) {
		return fFilter.hasFilter(filterProperty);
	}
	
	/**
	 * Saves the state of the filter actions
	 */
	public void saveState(IMemento memento) {
		memento.putString(TAG_HIDEFIELDS, String.valueOf(hasMemberFilter(MethodsViewerFilter.FILTER_FIELDS)));
		memento.putString(TAG_HIDESTATIC, String.valueOf(hasMemberFilter(MethodsViewerFilter.FILTER_STATIC)));
		memento.putString(TAG_HIDENONPUBLIC, String.valueOf(hasMemberFilter(MethodsViewerFilter.FILTER_NONPUBLIC)));
		memento.putString(TAG_SHOWINHERITED, String.valueOf(isShowInheritedMethods()));

		ScrollBar bar= getTable().getVerticalBar();
		int position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_VERTICAL_SCROLL, String.valueOf(position));
	}

	/**
	 * Restores the state of the filter actions
	 */	
	public void restoreState(IMemento memento) {
		boolean set= Boolean.valueOf(memento.getString(TAG_HIDEFIELDS)).booleanValue();
		setMemberFilter(MethodsViewerFilter.FILTER_FIELDS, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDESTATIC)).booleanValue();
		setMemberFilter(MethodsViewerFilter.FILTER_STATIC, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDENONPUBLIC)).booleanValue();
		setMemberFilter(MethodsViewerFilter.FILTER_NONPUBLIC, set);		
		
		set= Boolean.valueOf(memento.getString(TAG_SHOWINHERITED)).booleanValue();
		showInheritedMethods(set);
		
		ScrollBar bar= getTable().getVerticalBar();
		if (bar != null) {
			Integer vScroll= memento.getInteger(TAG_VERTICAL_SCROLL);
			if (vScroll != null) {
				bar.setSelection(vScroll.intValue());
			}
		}
	}
	
	/**
	 * Attaches a contextmenu listener to the table
	 */
	public void initContextMenu(IMenuListener menuListener, String popupId, IWorkbenchPartSite viewSite) {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(menuListener);
		Menu menu= menuMgr.createContextMenu(getTable());
		getTable().setMenu(menu);
		viewSite.registerContextMenu(popupId, menuMgr, this);
	}	
		
	
	/**
	 * Fills up the context menu with items for the method viewer
	 * Should be called by the creator of the context menu
	 */	
	public void contributeToContextMenu(IMenuManager menu) {
		if (fOpen.canActionBeAdded()) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpen);
		}
		ContextMenuGroup.add(menu, fStandardGroups, this);
	}

	/**
	 * Fills up the tool bar with items for the method viewer
	 * Should be called by the creator of the tool bar
	 */
	public void contributeToToolBar(ToolBarManager tbm) {
		tbm.add(fShowInheritedMembersAction);
		tbm.add(new Separator());
		tbm.add(fFilterActions[0]); // fields
		tbm.add(fFilterActions[1]); // static
		tbm.add(fFilterActions[2]); // public
	}

	/*
	 * @see StructuredViewer#handleInvalidSelection(ISelection, ISelection)
	 */
	protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
		// on change of input, try to keep selected methods stable by selecting a method with the same
		// signature: See #5466
		List oldSelections= SelectionUtil.toList(invalidSelection);
		List newSelections= SelectionUtil.toList(newSelection);
		if (!oldSelections.isEmpty()) {
			ArrayList newSelectionElements= new ArrayList(newSelections);
			try {
				Object[] currElements= getFilteredChildren(getInput());
				for (int i= 0; i < oldSelections.size(); i++) {
					Object curr= oldSelections.get(i);
					if (curr instanceof IMethod && !newSelections.contains(curr)) {
						IMethod method= (IMethod) curr;
						if (method.exists()) {
							IMethod similar= findSimilarMethod(method, currElements);
							if (similar != null) {
								newSelectionElements.add(similar);
							}
						}
					}
				}
				newSelection= new StructuredSelection(newSelectionElements);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		setSelection(newSelection);
		updateSelection(newSelection);
	}
	
	private IMethod findSimilarMethod(IMethod meth, Object[] elements) throws JavaModelException {
		String name= meth.getElementName();
		String[] paramTypes= meth.getParameterTypes();
		boolean isConstructor= meth.isConstructor();
		
		for (int i= 0; i < elements.length; i++) {
			Object curr= elements[i];
			if (curr instanceof IMethod && JavaModelUtil.isSameMethodSignature(name, paramTypes, isConstructor, (IMethod) curr)) {
				return (IMethod) curr;
			}
		}
		return null;
	}

}