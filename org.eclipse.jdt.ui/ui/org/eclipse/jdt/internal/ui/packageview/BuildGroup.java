/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.BuildAction;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;

import org.eclipse.jdt.ui.IContextMenuConstants;

public class BuildGroup extends ContextMenuGroup {

	private BuildAction fBuildAction;
	private BuildAction fFullBuildAction;

	public BuildGroup(IViewPart part, boolean listenToSelectionChanges) {
		Shell shell= part.getSite().getShell();
		ISelectionProvider provider= part.getSite().getSelectionProvider();
		
		fBuildAction= new BuildAction(shell,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		fBuildAction.setText(PackagesMessages.getString("BuildGroup.buildProject")); //$NON-NLS-1$
		
		fFullBuildAction= new BuildAction(shell,
			IncrementalProjectBuilder.FULL_BUILD);
		fFullBuildAction.setText(PackagesMessages.getString("BuildGroup.rebuildProject")); //$NON-NLS-1$
		if (listenToSelectionChanges) {
			provider.addSelectionChangedListener(fBuildAction);
			provider.addSelectionChangedListener(fFullBuildAction);
		}
	}

	public void fill(IMenuManager manager, GroupContext context) {
		ISelection selection= context.getSelection();
		if (!(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection ss= (IStructuredSelection)selection;
		
		fFullBuildAction.selectionChanged(ss);
		if (fFullBuildAction.isEnabled())
			manager.appendToGroup(IContextMenuConstants.GROUP_BUILD, fFullBuildAction);
	}
	
	public void fillActionBars(IActionBars bars) {
		bars.setGlobalActionHandler(IWorkbenchActionConstants.BUILD_PROJECT, fBuildAction);
		bars.setGlobalActionHandler(IWorkbenchActionConstants.REBUILD_PROJECT, fFullBuildAction);
	}	
}
