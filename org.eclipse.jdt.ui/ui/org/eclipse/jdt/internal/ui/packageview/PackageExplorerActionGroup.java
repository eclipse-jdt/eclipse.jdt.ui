/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.resources.IContainer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.NewWizardMenu;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.actions.RefreshAction;
import org.eclipse.ui.views.framelist.BackAction;
import org.eclipse.ui.views.framelist.ForwardAction;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.framelist.UpAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.ui.actions.BuildActionGroup;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.ImportActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.MemberFilterActionGroup;
import org.eclipse.jdt.ui.actions.NavigateActionGroup;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jdt.ui.actions.ShowActionGroup;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;

import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;

public class PackageExplorerActionGroup extends CompositeActionGroup {

	private PackageExplorerPart fPart;

	private GoIntoAction fZoomInAction;
 	private BackAction fBackAction;
	private ForwardAction fForwardAction;
	private UpAction fUpAction;
	private GotoTypeAction fGotoTypeAction;
	private GotoPackageAction fGotoPackageAction;
	
	private NavigateActionGroup fNavigateActionGroup;
	private BuildActionGroup fBuildActionGroup;
	private CCPActionGroup fCCPActionGroup;
	private WorkingSetFilterActionGroup fWorkingSetFilterActionGroup;
	
	private MemberFilterActionGroup fMemberFilterActionGroup;

 	private ShowLibrariesAction fShowLibrariesAction;
  	private FilterSelectionAction fFilterAction;
	
	public PackageExplorerActionGroup(PackageExplorerPart part) {
		super();
		fPart= part;
		Shell shell= fPart.getSite().getShell();
		setGroups(new ActionGroup[] {
			fNavigateActionGroup= new NavigateActionGroup(fPart), 
			new ShowActionGroup(fPart), 
			fCCPActionGroup= new CCPActionGroup(fPart),
			new RefactorActionGroup(fPart),
			new ImportActionGroup(fPart),
			new GenerateActionGroup(fPart), 
			fBuildActionGroup= new BuildActionGroup(fPart),
			new JavaSearchActionGroup(fPart, fPart.getViewer()),
			fWorkingSetFilterActionGroup= new WorkingSetFilterActionGroup(part.getViewer(), JavaUI.ID_PACKAGES, shell, createTitleUpdater())});
		
		PackagesFrameSource frameSource= new PackagesFrameSource(fPart);
		FrameList frameList= new FrameList(frameSource);
		frameSource.connectTo(frameList);
			
		fZoomInAction= new GoIntoAction(frameList);
		fBackAction= new BackAction(frameList);
		fForwardAction= new ForwardAction(frameList);
		fUpAction= new UpAction(frameList);

		fGotoTypeAction= new GotoTypeAction(fPart);
		fGotoPackageAction= new GotoPackageAction(fPart);
		
		fMemberFilterActionGroup= new MemberFilterActionGroup(fPart.getViewer(), "PackageView");  //$NON-NLS-1$
		
		fShowLibrariesAction = new ShowLibrariesAction(fPart, PackagesMessages.getString("PackageExplorer.referencedLibs")); //$NON-NLS-1$				
		fFilterAction = new FilterSelectionAction(shell, fPart, PackagesMessages.getString("PackageExplorer.filters")); //$NON-NLS-1$		
	}

	//---- Persistent state -----------------------------------------------------------------------

	/* package */ void restoreState(IMemento memento) {
		fMemberFilterActionGroup.restoreState(memento);
		fWorkingSetFilterActionGroup.restoreState(memento);
	}
	
	/* package */ void saveState(IMemento memento) {
		fMemberFilterActionGroup.saveState(memento);
		fWorkingSetFilterActionGroup.saveState(memento);
	}

	//---- Action Bars ----------------------------------------------------------------------------

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		setGlobalActionHandlers(actionBars);
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());
	}

	private void setGlobalActionHandlers(IActionBars actionBars) {
		// Navigate Go Into and Go To actions.
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_INTO, fZoomInAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.BACK, fBackAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.FORWARD, fForwardAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.UP, fUpAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.GOTO_TYPE, fGotoTypeAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.GOTO_PACKAGE, fGotoPackageAction);
	}

	/* package */ void fillToolBar(IToolBarManager toolBar) {
		toolBar.removeAll();
		
		toolBar.add(fBackAction);
		toolBar.add(fForwardAction);
		toolBar.add(fUpAction);
		
		if (JavaBasePreferencePage.showCompilationUnitChildren()) {
			toolBar.add(new Separator());
			fMemberFilterActionGroup.contributeToToolBar(toolBar);
		}
	}
	
	/* package */ void fillViewMenu(IMenuManager menu) {
		menu.add(fFilterAction);		
		menu.add(fShowLibrariesAction);  
		
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
	}

	/* package */ void handleSelectionChanged(SelectionChangedEvent event) {
		fZoomInAction.update();
	}

	//---- Context menu -------------------------------------------------------------------------

	public void fillContextMenu(IMenuManager menu) {		
		IStructuredSelection selection= (IStructuredSelection)getContext().getSelection();
		int size= selection.size();
		Object element= selection.getFirstElement();
		IJavaElement jElement= element instanceof IJavaElement ? (IJavaElement)element : null;
		
		if (size == 1 && isNewTarget(jElement)) {
			IMenuManager newMenu= new MenuManager(PackagesMessages.getString("PackageExplorer.new")); //$NON-NLS-1$
			menu.appendToGroup(IContextMenuConstants.GROUP_NEW, newMenu);
			new NewWizardMenu(newMenu, fPart.getSite().getWorkbenchWindow(), false);
		}
				
		addGotoMenu(menu, element, size);
		
		addOpenNewWindowAction(menu, element);
		
		super.fillContextMenu(menu);
	}
	
	 private void addGotoMenu(IMenuManager menu, Object element, int size) {
		
		if (size == 1 && fPart.getViewer().isExpandable(element) && isGoIntoTarget(element)) 
			menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fZoomInAction);
		
		/*	
		MenuManager gotoMenu= new MenuManager(PackagesMessages.getString("PackageExplorer.gotoTitle")); //$NON-NLS-1$
		menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, gotoMenu);
		gotoMenu.add(fBackAction);
		gotoMenu.add(fForwardAction);
		gotoMenu.add(fUpAction);
		gotoMenu.add(fGotoTypeAction);
		gotoMenu.add(fGotoPackageAction);
		*/
	}
	
	private boolean isNewTarget(IJavaElement element) {
		if (element == null)
			return false;
		int type= element.getElementType();
		return type == IJavaElement.JAVA_PROJECT ||
			type == IJavaElement.PACKAGE_FRAGMENT_ROOT || 
			type == IJavaElement.PACKAGE_FRAGMENT;
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
			try {
				element= ((IJavaElement)element).getCorrespondingResource();
			} catch(JavaModelException e) {
			}
		}
		if (!(element instanceof IContainer))
			return;
		menu.appendToGroup(
			IContextMenuConstants.GROUP_OPEN, 
			new OpenInNewWindowAction(fPart.getSite().getWorkbenchWindow(), (IContainer)element));
	}

	//---- Key board handling ------------------------------------------------------------

	/* package*/ void handleDoubleClick(DoubleClickEvent event) {
		OpenAction openAction= fNavigateActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}
		TreeViewer viewer= fPart.getViewer();
		Object element= ((IStructuredSelection)event.getSelection()).getFirstElement();
		if (viewer.isExpandable(element)) {
			if (JavaBasePreferencePage.doubleClickGoesInto()) {
				// don't zoom into compilation units and class files
				if (element instanceof IOpenable && 
					!(element instanceof ICompilationUnit) && 
					!(element instanceof IClassFile)) {
					fZoomInAction.run();
				}
			} else {
				viewer.setExpandedState(element, !viewer.getExpandedState(element));
			}
		}
	}
	
	/* package */ void handleKeyEvent(KeyEvent event) {
		if (event.stateMask != 0) 
			return;		
		
		if (event.keyCode == SWT.F5) {
			RefreshAction action= fBuildActionGroup.getRefreshAction();
			if (action.isEnabled())
				action.run();
		} else if (event.character == SWT.DEL) {
			IAction delete= fCCPActionGroup.getDeleteAction();
			if (delete.isEnabled())
				delete.run();
		}
	}
	
	private IPropertyChangeListener createTitleUpdater() {
		return new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property= event.getProperty();
				if (IWorkbenchPage.CHANGE_WORKING_SET_REPLACE.equals(property)
				|| IWorkingSet.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
					Object workingSet= (IWorkingSet)event.getNewValue();
					String workingSetName= null;
					if (workingSet instanceof IWorkingSet)
						workingSetName= ((IWorkingSet)workingSet).getName();
					fPart.setWorkingSetName(workingSetName);
					fPart.updateTitle();
				}
			}
		};
	}
}

