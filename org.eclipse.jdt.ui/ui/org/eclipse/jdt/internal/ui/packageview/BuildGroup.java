/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.HashSet;import java.util.Iterator;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IStorage;import org.eclipse.core.resources.IncrementalProjectBuilder;import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;import org.eclipse.jdt.internal.ui.actions.GroupContext;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.ui.actions.BuildAction;


public class BuildGroup extends ContextMenuGroup {

	private BuildAction fBuildAction;
	private BuildAction fFullBuildAction;

	public void fill(IMenuManager manager, GroupContext context) {
		if (!shouldContribute(context))
			return;
			
		if (fBuildAction == null) {
			fBuildAction= new BuildAction(JavaPlugin.getActiveWorkbenchShell(),
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
			fBuildAction.setText(PackagesMessages.getString("BuildGroup.buildProject")); //$NON-NLS-1$
		}
		
		
		fBuildAction.selectionChanged(convertSelectionToProjects((IStructuredSelection)context.getSelection()));
		manager.appendToGroup(IContextMenuConstants.GROUP_BUILD, fBuildAction);
		
		if (fFullBuildAction == null) {
			fFullBuildAction= new BuildAction(JavaPlugin.getActiveWorkbenchShell(),
				IncrementalProjectBuilder.FULL_BUILD);
			fFullBuildAction.setText(PackagesMessages.getString("BuildGroup.rebuildProject")); //$NON-NLS-1$
		}
		
		fFullBuildAction.selectionChanged(convertSelectionToProjects((IStructuredSelection)context.getSelection()));
		manager.appendToGroup(IContextMenuConstants.GROUP_BUILD, fFullBuildAction);
	}
	
	private boolean shouldContribute(GroupContext context) {
		ISelection s= context.getSelection();
		if (! (s instanceof IStructuredSelection))
			return false;
			
		IStructuredSelection selection= convertSelectionToProjects((IStructuredSelection)s);
		return !(selection.isEmpty());
	}
	
	private IStructuredSelection convertSelectionToProjects(IStructuredSelection selection) {
		HashSet result= new HashSet();
		Iterator elements= selection.iterator();
		while (elements.hasNext()) {
			IProject p= getProjectFor(elements.next());
			if (p != null)
				result.add(p);
		}
		Object[] projects= new Object[result.size()];
		projects= result.toArray(projects);
		return new StructuredSelection(projects);
	}
	
	private IProject getProjectFor(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).getJavaProject().getProject();
		} else if (element instanceof IResource) {
			return ((IResource)element).getProject();
		}
		return null;
	}
}
