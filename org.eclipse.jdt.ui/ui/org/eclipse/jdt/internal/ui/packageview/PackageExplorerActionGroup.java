/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.views.framelist.BackAction;
import org.eclipse.ui.views.framelist.ForwardAction;
import org.eclipse.ui.views.framelist.FrameAction;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.framelist.UpAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.NewWizardsActionGroup;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.BuildActionGroup;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.CustomFiltersActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.ImportActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.NavigateActionGroup;
import org.eclipse.jdt.ui.actions.ProjectActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;

class PackageExplorerActionGroup extends CompositeActionGroup {

	private PackageExplorerPart fPart;

	private GoIntoAction fZoomInAction;
 	private BackAction fBackAction;
	private ForwardAction fForwardAction;
	private UpAction fUpAction;
	private GotoTypeAction fGotoTypeAction;
	private GotoPackageAction fGotoPackageAction;
	private GotoResourceAction fGotoResourceAction;
	private CollapseAllAction fCollapseAllAction;
	
	
	private ToggleLinkingAction fToggleLinkingAction;

	private RefactorActionGroup fRefactorActionGroup;
	private NavigateActionGroup fNavigateActionGroup;
	private WorkingSetFilterActionGroup fWorkingSetFilterActionGroup;
	
	private CustomFiltersActionGroup fCustomFiltersActionGroup;

	private IAction fGotoRequiredProjectAction;	
 	
	public PackageExplorerActionGroup(PackageExplorerPart part) {
		super();
		fPart= part;
		TreeViewer viewer= part.getViewer();
		
		IPropertyChangeListener workingSetListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doWorkingSetChanged(event);
			}
		};
		
		IWorkbenchPartSite site = fPart.getSite();
		Shell shell= site.getShell();
		setGroups(new ActionGroup[] {
			new NewWizardsActionGroup(site),
			fNavigateActionGroup= new NavigateActionGroup(fPart), 
			new CCPActionGroup(fPart),
			new GenerateActionGroup(fPart), 
			fRefactorActionGroup= new RefactorActionGroup(fPart),
			new ImportActionGroup(fPart),
			new BuildActionGroup(fPart),
			new JavaSearchActionGroup(fPart),
			new ProjectActionGroup(fPart), 
			fWorkingSetFilterActionGroup= new WorkingSetFilterActionGroup(JavaUI.ID_PACKAGES, shell, workingSetListener),

			fCustomFiltersActionGroup= new CustomFiltersActionGroup(fPart, viewer),
			new LayoutActionGroup(part)});
		

		viewer.addFilter(fWorkingSetFilterActionGroup.getWorkingSetFilter());
		
		PackagesFrameSource frameSource= new PackagesFrameSource(fPart);
		FrameList frameList= new FrameList(frameSource);
		frameSource.connectTo(frameList);
			
		fZoomInAction= new GoIntoAction(frameList);
		fBackAction= new BackAction(frameList);
		fForwardAction= new ForwardAction(frameList);
		fUpAction= new UpAction(frameList);
		
		fGotoTypeAction= new GotoTypeAction(fPart);
		fGotoPackageAction= new GotoPackageAction(fPart);
		fGotoResourceAction= new GotoResourceAction(fPart);
		fCollapseAllAction= new CollapseAllAction(fPart);	
		fToggleLinkingAction = new ToggleLinkingAction(fPart); 
		fGotoRequiredProjectAction= new GotoRequiredProjectAction(fPart);
	}

	public void dispose() {
		super.dispose();
	}
	

	//---- Persistent state -----------------------------------------------------------------------

	/* package */ void restoreFilterAndSorterState(IMemento memento) {
		fWorkingSetFilterActionGroup.restoreState(memento);
		fCustomFiltersActionGroup.restoreState(memento);
	}
	
	/* package */ void saveFilterAndSorterState(IMemento memento) {
		fWorkingSetFilterActionGroup.saveState(memento);
		fCustomFiltersActionGroup.saveState(memento);
	}

	//---- Action Bars ----------------------------------------------------------------------------

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		setGlobalActionHandlers(actionBars);
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());		
	}

	/* package  */ void updateActionBars(IActionBars actionBars) {
		actionBars.getToolBarManager().removeAll();
		actionBars.getMenuManager().removeAll();
		fillActionBars(actionBars);
		actionBars.updateActionBars();
		fZoomInAction.setEnabled(true);
	}

	private void setGlobalActionHandlers(IActionBars actionBars) {
		// Navigate Go Into and Go To actions.
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_INTO, fZoomInAction);
		actionBars.setGlobalActionHandler(ActionFactory.BACK.getId(), fBackAction);
		actionBars.setGlobalActionHandler(ActionFactory.FORWARD.getId(), fForwardAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.UP, fUpAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_TO_RESOURCE, fGotoResourceAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.GOTO_TYPE, fGotoTypeAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.GOTO_PACKAGE, fGotoPackageAction);
		
		fRefactorActionGroup.retargetFileMenuActions(actionBars);
	}

	/* package */ void fillToolBar(IToolBarManager toolBar) {
		toolBar.add(fBackAction);
		toolBar.add(fForwardAction);
		toolBar.add(fUpAction); 
		
		toolBar.add(new Separator());
		toolBar.add(fCollapseAllAction);
		toolBar.add(fToggleLinkingAction);

	}
	
	/* package */ void fillViewMenu(IMenuManager menu) {
		menu.add(fToggleLinkingAction);

		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
	}

	/* package */ void handleSelectionChanged(SelectionChangedEvent event) {
	}

	//---- Context menu -------------------------------------------------------------------------

	public void fillContextMenu(IMenuManager menu) {		
		IStructuredSelection selection= (IStructuredSelection)getContext().getSelection();
		int size= selection.size();
		Object element= selection.getFirstElement();
		
		if (element instanceof ClassPathContainer.RequiredProjectWrapper) 
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fGotoRequiredProjectAction);
		
		addGotoMenu(menu, element, size);
		
		addOpenNewWindowAction(menu, element);
		
		super.fillContextMenu(menu);
	}
	
	 private void addGotoMenu(IMenuManager menu, Object element, int size) {
		boolean enabled= size == 1 && fPart.getViewer().isExpandable(element) && (isGoIntoTarget(element) || element instanceof IContainer);
		fZoomInAction.setEnabled(enabled);
		if (enabled)
			menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fZoomInAction);
	}
	
	private boolean isGoIntoTarget(Object element) {
		if (element == null)
			return false;
		if (element instanceof IJavaElement) {
			int type= ((IJavaElement)element).getElementType();
			return type == IJavaElement.JAVA_PROJECT || 
				type == IJavaElement.PACKAGE_FRAGMENT_ROOT || 
				type == IJavaElement.PACKAGE_FRAGMENT;
		}
		return false;
	}

	private void addOpenNewWindowAction(IMenuManager menu, Object element) {
		if (element instanceof IJavaElement) {
			element= ((IJavaElement)element).getResource();
			
		}
		// fix for 64890 Package explorer out of sync when open/closing projects [package explorer] 64890  
		if (element instanceof IProject && !((IProject)element).isOpen()) 
			return;
		
		if (!(element instanceof IContainer))
			return;
		menu.appendToGroup(
			IContextMenuConstants.GROUP_OPEN, 
			new OpenInNewWindowAction(fPart.getSite().getWorkbenchWindow(), (IContainer)element));
	}

	//---- Key board and mouse handling ------------------------------------------------------------

	/* package*/ void handleDoubleClick(DoubleClickEvent event) {
		TreeViewer viewer= fPart.getViewer();
		Object element= ((IStructuredSelection)event.getSelection()).getFirstElement();
		if (viewer.isExpandable(element)) {
			if (doubleClickGoesInto()) {
				// don't zoom into compilation units and class files
				if (element instanceof ICompilationUnit || element instanceof IClassFile)
					return;
				if (element instanceof IOpenable || element instanceof IContainer) {
					fZoomInAction.run();
				}
			} else {
				IAction openAction= fNavigateActionGroup.getOpenAction();
				if (openAction != null && openAction.isEnabled() && OpenStrategy.getOpenMethod() == OpenStrategy.DOUBLE_CLICK)
					return;
				viewer.setExpandedState(element, !viewer.getExpandedState(element));
			}
		}
	}
	
	/* package */ void handleOpen(OpenEvent event) {
		IAction openAction= fNavigateActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}
	}
	
	/* package */ void handleKeyEvent(KeyEvent event) {
		if (event.stateMask != 0) 
			return;		
		
		if (event.keyCode == SWT.BS) {
			if (fUpAction != null && fUpAction.isEnabled()) {
				fUpAction.run();
				event.doit= false;
			}
		}
	}
	
	private void doWorkingSetChanged(PropertyChangeEvent event) {
		IWorkingSet workingSet= (IWorkingSet) event.getNewValue();
		
		String workingSetName= null;
		if (workingSet != null)
			workingSetName= workingSet.getName();
		fPart.setWorkingSetName(workingSetName);
		fPart.updateTitle();

		String property= event.getProperty();
		if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
			TreeViewer viewer= fPart.getViewer();
			viewer.getControl().setRedraw(false);
			viewer.refresh();
			viewer.getControl().setRedraw(true);
		}
		
	}

	private boolean doubleClickGoesInto() {
		return PreferenceConstants.DOUBLE_CLICK_GOES_INTO.equals(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.DOUBLE_CLICK));
	}

	public FrameAction getUpAction() {
		return fUpAction;
	}

	public FrameAction getBackAction() {
		return fBackAction;
	}
	public FrameAction getForwardAction() {
		return fForwardAction;
	}

	public WorkingSetFilterActionGroup getWorkingSetActionGroup() {
	    return fWorkingSetFilterActionGroup;
	}
	
	public CustomFiltersActionGroup getCustomFilterActionGroup() {
	    return fCustomFiltersActionGroup;
	}
}
