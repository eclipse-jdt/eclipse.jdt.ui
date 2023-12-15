/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.Progress;

/**
 * Action to remove package fragment roots from the classpath of its parent
 * project. Currently, the action is applicable to selections containing
 * non-external archives (JAR or zip).
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RemoveFromClasspathAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>RemoveFromClasspathAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type
	 * <code> org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public RemoveFromClasspathAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.RemoveFromClasspathAction_Remove);
		setToolTipText(ActionMessages.RemoveFromClasspathAction_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.REMOVE_FROM_CLASSPATH_ACTION);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private static boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		for (Object name : selection) {
			if (! canRemove(name))
				return false;
		}
		return true;
	}

	@Override
	public void run(final IStructuredSelection selection) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, new WorkbenchRunnableAdapter(pm -> {
				try{
					IPackageFragmentRoot[] roots= getRootsToRemove(selection);
					pm.beginTask(ActionMessages.RemoveFromClasspathAction_Removing, roots.length);
					for (IPackageFragmentRoot root : roots) {
						int jCoreFlags= IPackageFragmentRoot.NO_RESOURCE_MODIFICATION | IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH;
						root.delete(IResource.NONE, jCoreFlags, Progress.subMonitor(pm, 1));
					}
				} finally {
					pm.done();
				}
			}));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(),
					ActionMessages.RemoveFromClasspathAction_exception_dialog_title,
					ActionMessages.RemoveFromClasspathAction_Problems_occurred);
		} catch (InterruptedException e) {
			// canceled
		}
	}

	private static IPackageFragmentRoot[] getRootsToRemove(IStructuredSelection selection){
		List<Object> result= new ArrayList<>(selection.size());
		for (Object element : selection) {
			if (canRemove(element))
				result.add(element);
		}
		return result.toArray(new IPackageFragmentRoot[result.size()]);
	}

	private static boolean canRemove(Object element){
		if (! (element instanceof IPackageFragmentRoot))
			return false;
		IPackageFragmentRoot root= (IPackageFragmentRoot)element;
		try {
			IClasspathEntry cpe= root.getRawClasspathEntry();
			if (cpe == null || cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
				return false; // don't want to remove the container if only a child is selected
			return true;
		} catch (JavaModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		}
		return false;
	}
}

