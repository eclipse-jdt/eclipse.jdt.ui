package org.eclipse.jdt.internal.ui.packageview;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.Iterator;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ui.actions.BuildAction;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;


public class BuildGroup extends ContextMenuGroup {

	public static final String GROUP_NAME= IContextMenuConstants.GROUP_BUILD;

	private BuildAction fBuildAction;
	private BuildAction fFullBuildAction;

	public void fill(IMenuManager manager, GroupContext context) {
		if (! shouldContribute(context))
			return;
			
		if (fBuildAction == null)
			fBuildAction= new BuildAction(JavaPlugin.getActiveWorkbenchShell(),
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		
		fBuildAction.selectionChanged((IStructuredSelection)context.getSelection());
		manager.appendToGroup(GROUP_NAME, fBuildAction);
		
		if (fFullBuildAction == null) {
			fFullBuildAction= new BuildAction(JavaPlugin.getActiveWorkbenchShell(),
				IncrementalProjectBuilder.FULL_BUILD);
		}
		
		fFullBuildAction.selectionChanged((IStructuredSelection)context.getSelection());
		manager.appendToGroup(GROUP_NAME, fFullBuildAction);
	}
	
	private boolean shouldContribute(GroupContext context) {
		ISelection s= context.getSelection();
		if (! (s instanceof IStructuredSelection))
			return false;
			
		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.isEmpty())
			return false;
			
		Iterator iter= selection.iterator();
		while (iter.hasNext()) {
			IResource res= getResourceFor(iter.next());
			if (!(res instanceof IProject))
				return false;
		}
		return true;
	}
	
	private IResource getResourceFor(Object element) {
		if (element instanceof IJavaElement) {
			try {
				element= ((IJavaElement)element).getCorrespondingResource();
			} catch (JavaModelException e) {
				return null;
			}
		}
		if (!(element instanceof IResource) || ((IResource)element).isPhantom()) {
			return null;
		}
		return (IResource)element;
	}		
}