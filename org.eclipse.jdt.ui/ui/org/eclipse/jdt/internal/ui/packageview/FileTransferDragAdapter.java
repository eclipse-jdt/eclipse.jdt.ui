/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.util.Resources;

/**
 * Drag support class to allow dragging of files and folder from
 * the packages view to another application.
 */
class FileTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {
	
	private ISelectionProvider fProvider;
	
	FileTransferDragAdapter(ISelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}

	public Transfer getTransfer() {
		return FileTransfer.getInstance();
	}
	
	public void dragStart(DragSourceEvent event) {
		event.doit= isDragable(fProvider.getSelection());
	}
	
	private boolean isDragable(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator iter= ((IStructuredSelection)selection).iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof IPackageFragment) {
					return false;
				} else if (element instanceof IJavaElement) {
					IPackageFragmentRoot root= (IPackageFragmentRoot)((IJavaElement)element).getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null && root.isArchive())
						return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public void dragSetData(DragSourceEvent event){
		List elements= getResources();
		if (elements == null || elements.size() == 0) {
			event.data= null;
			return;
		}
		
		event.data= getResourceLocations(elements);
	}

	private static String[] getResourceLocations(List resources) {
		return Resources.getLocationOSStrings((IResource[]) resources.toArray(new IResource[resources.size()]));
	}
	
	public void dragFinished(DragSourceEvent event) {
		if (!event.doit)
			return;
		
		if (event.detail == DND.DROP_MOVE){
			handleDropMove(event);
		}	
		else if (event.detail == DND.DROP_NONE || event.detail == DND.DROP_TARGET_MOVE) {
			handleRefresh(event);
		}
	}
	
	private void handleDropMove(DragSourceEvent event) {
		final List elements= getResources();
		if (elements == null || elements.size() == 0)
			return;
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				try {
					monitor.beginTask(PackagesMessages.getString("DragAdapter.deleting"), elements.size()); //$NON-NLS-1$
					MultiStatus status= createMultiStatus();
					Iterator iter= elements.iterator();
					while(iter.hasNext()) {
						IResource resource= (IResource)iter.next();
						try {
							monitor.subTask(resource.getFullPath().toOSString());
							resource.delete(true, null);
							
						} catch (CoreException e) {
							status.add(e.getStatus());
						} finally {
							monitor.worked(1);
						}
					}
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} finally {
					monitor.done();
				}
			}
		};
		runOperation(op, true, false);
	}
	
	private  void handleRefresh(DragSourceEvent event) {
		final Set roots= collectRoots(getResources());
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				try {
					monitor.beginTask(PackagesMessages.getString("DragAdapter.refreshing"), roots.size()); //$NON-NLS-1$
					MultiStatus status= createMultiStatus();
					Iterator iter= roots.iterator();
					while (iter.hasNext()) {
						IResource r= (IResource)iter.next();
						try {
							r.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
						} catch (CoreException e) {
							status.add(e.getStatus());
						}	
					}
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} finally {
					monitor.done();
				}
			}
		};
		
		runOperation(op, true, false);
	}

	protected Set collectRoots(final List elements) {
		final Set roots= new HashSet(10);
		
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			IResource resource= (IResource)iter.next();
			IResource parent= resource.getParent();
			if (parent == null) {
				roots.add(resource);
			} else {
				roots.add(parent);
			}
		}
		return roots;
	}
	
	private List getResources() {
		ISelection s= fProvider.getSelection();
		if (!(s instanceof IStructuredSelection)) 
			return null;
		
		List result= new ArrayList(10);
		Iterator iter= ((IStructuredSelection)s).iterator();
		while (iter.hasNext()) {
			Object o= iter.next();
			IResource r= null;
			if (o instanceof IResource) {
				r= (IResource)o;
			} else if (o instanceof IAdaptable) {
				r= (IResource)((IAdaptable)o).getAdapter(IResource.class);
			}
			if (r != null)
				result.add(r);
		}
		return result;
	}
	
	private MultiStatus createMultiStatus() {
		return new MultiStatus(JavaPlugin.getPluginId(), 
			IStatus.OK, PackagesMessages.getString("DragAdapter.problem"), null); //$NON-NLS-1$
	}
	
	private void runOperation(IRunnableWithProgress op, boolean fork, boolean cancelable) {
		try {
			Shell parent= JavaPlugin.getActiveWorkbenchShell();
			new ProgressMonitorDialog(parent).run(fork, cancelable, op);
		} catch (InvocationTargetException e) {
			String message= PackagesMessages.getString("DragAdapter.problem"); //$NON-NLS-1$
			String title= PackagesMessages.getString("DragAdapter.problemTitle"); //$NON-NLS-1$
			ExceptionHandler.handle(e, title, message);
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		}
	}
}