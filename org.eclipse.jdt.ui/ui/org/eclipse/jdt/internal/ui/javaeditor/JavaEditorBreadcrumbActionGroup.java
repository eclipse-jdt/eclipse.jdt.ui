/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.commands.operations.IUndoContext;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.operations.UndoRedoActionGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.BuildActionGroup;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.ProjectActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.NewWizardsActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup;
import org.eclipse.jdt.internal.ui.workingsets.ConfigureWorkingSetAssignementAction;


/**
 * Actions to show in the context menu for elements in
 * the {@link JavaEditorBreadcrumb}.
 * 
 * @since 3.4
 */
final class JavaEditorBreadcrumbActionGroup extends CompositeActionGroup	 {
	
	static final class WorkingSetActionGroup extends ActionGroup {

		private final ConfigureWorkingSetAssignementAction fAssignWorkingSetAction;
		private final ISelectionProvider fSelectionProvider;

		public WorkingSetActionGroup(IWorkbenchSite site, ISelectionProvider selectionProvider) {
			fSelectionProvider= selectionProvider;
			fAssignWorkingSetAction= new ConfigureWorkingSetAssignementAction(site);
			fAssignWorkingSetAction.setSpecialSelectionProvider(selectionProvider);
			selectionProvider.addSelectionChangedListener(fAssignWorkingSetAction);
		}

		public void dispose() {
			super.dispose();
			fSelectionProvider.removeSelectionChangedListener(fAssignWorkingSetAction);
		}

		public void fillContextMenu(IMenuManager menu) {
			if (fAssignWorkingSetAction.isEnabled())
				menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fAssignWorkingSetAction);
		}
	}
	
	
	public JavaEditorBreadcrumbActionGroup(IWorkbenchPartSite site, ISelectionProvider selectionProvider) {
		super(new ActionGroup[] {
				new UndoRedoActionGroup(site, (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(IUndoContext.class), true),
				new NewWizardsActionGroup(site),
				new JavaSearchActionGroup(site, selectionProvider),
				new OpenViewActionGroup(site, selectionProvider),
				new CCPActionGroup(site, selectionProvider),
				new GenerateBuildPathActionGroup(site, selectionProvider),
				new GenerateActionGroup(site, selectionProvider),
				new RefactorActionGroup(site, selectionProvider),
				new BuildActionGroup(site, selectionProvider),
				new ProjectActionGroup(site, selectionProvider),
				new WorkingSetActionGroup(site, selectionProvider)
		});
	}
}
