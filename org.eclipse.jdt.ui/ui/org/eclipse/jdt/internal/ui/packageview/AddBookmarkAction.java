/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.Iterator;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRunnable;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.ui.PlatformUI;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.internal.IHelpContextIds;import org.eclipse.ui.internal.WorkbenchMessages;import org.eclipse.ui.internal.WorkbenchPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

/*
 * XXX This class is a copy of org.eclipse.ui.actions.AddBookmarkAction
 */
public class AddBookmarkAction extends JavaUIAction {

	/**
	 * The id of this action.
	 */
	public static final String ID = PlatformUI.PLUGIN_ID + ".AddBookmarkAction";
	//$NON-NLS-1$

	private ISelectionProvider fSelectionProvider;

	/**
	 * Creates a new bookmark action.
	 *
	 * @param shell the shell for any dialogs
	 */
	public AddBookmarkAction(ISelectionProvider provider) {
		super(WorkbenchMessages.getString("AddBookmarkLabel")); //$NON-NLS-1$
		setId(ID);
		setToolTipText(WorkbenchMessages.getString("AddBookmarkToolTip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(
			this,
			new Object[] { IHelpContextIds.ADD_BOOKMARK_ACTION });
		fSelectionProvider= provider;
	}

	/* (non-Javadoc)
	 * Method declared on IAction.
	 */
	public void run() {
		createMarker(IMarker.BOOKMARK);
	}
	
	public boolean canOperateOnSelection() {
		IStructuredSelection selection = getStructuredSelection();
		for (Iterator enum = selection.iterator(); enum.hasNext();) {
			Object o = enum.next();
			if (o instanceof IAdaptable) {
				Object resource= ((IAdaptable)o).getAdapter(IResource.class);
				if (!(resource instanceof IFile))
					return false;
			} else {
				return false;
			}
		}
		return true;
	}

	private void createMarker(String markerType) {
		IStructuredSelection selection = getStructuredSelection();
		for (Iterator enum = selection.iterator(); enum.hasNext();) {
			Object o = enum.next();
			if (o instanceof IAdaptable) {
				Object resource= ((IAdaptable)o).getAdapter(IResource.class);
				if (resource instanceof IFile)
					createMarker((IFile)resource, markerType);
			}
		}
	}

	private void createMarker(final IFile file, final String markerType) {
		try {
			file.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IMarker marker = file.createMarker(markerType);
					marker.setAttribute(IMarker.MESSAGE, file.getName());
				}
			}, null);
		} catch (CoreException e) {
			WorkbenchPlugin.log(null, e.getStatus()); // We don't care
		}
	}

	private IStructuredSelection getStructuredSelection() {
		ISelection selection= fSelectionProvider.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection)selection;
		}
		return new StructuredSelection();
	}
}
