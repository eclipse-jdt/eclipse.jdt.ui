package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.DeleteResourceAction;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

public class DeleteResourcesAction extends SelectionDispatchAction {

	protected DeleteResourcesAction(IWorkbenchSite site) {
		super(site);
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		if (ClipboardActionUtil.hasOnlyProjects(selection)){
			deleteProjects(selection);
			return;
		}	

		DeleteRefactoring refactoring= new DeleteRefactoring(selection.toList(), createRootManipulationQuery());
		
		if (!confirmDelete(selection))
			return;

		if (hasReadOnlyResources(selection) && !isOkToDeleteReadOnly()) 
			return;

		try{
			
			if (! confirmDeleteSourceFolderAsSubresource(selection))	
				return;
			MultiStatus status= ClipboardActionUtil.perform(refactoring);
			if (!status.isOK()) {
				JavaPlugin.log(status);
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("DeleteResourceAction.delete"), ReorgMessages.getString("DeleteResourceAction.exception"), status); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (CoreException e){
			ExceptionHandler.handle(e, ReorgMessages.getString("DeleteResourceAction.delete"), ReorgMessages.getString("DeleteResourceAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}	
	}
	
	private IPackageFragmentRootManipulationQuery createRootManipulationQuery(){
		String messagePattern= 	"Package fragment root ''{0}'' is referenced by the following projects. " +								"Do you still want to delete it?";
		return new PackageFragmentRootManipulationQuery(getShell(), "Delete", messagePattern);
	}

	private static void deleteProjects(IStructuredSelection selection){
		DeleteResourceAction action= new DeleteResourceAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(selection);
		action.run();
	}
	
	private static boolean isOkToDeleteReadOnly(){
			String msg= ReorgMessages.getString("deleteAction.confirmReadOnly"); //$NON-NLS-1$
			String title= ReorgMessages.getString("deleteAction.checkDeletion"); //$NON-NLS-1$
			return MessageDialog.openQuestion(
					JavaPlugin.getActiveWorkbenchShell(),
					title,
					msg);
	}
	
	private static boolean hasReadOnlyResources(IStructuredSelection selection){
		for (Iterator iter= selection.iterator(); iter.hasNext();){	
			if (ReorgUtils.shouldConfirmReadOnly(iter.next()))
				return true;
		}
		return false;
	}
	
	private static boolean confirmDeleteSourceFolderAsSubresource(IStructuredSelection selection) throws CoreException {
		if (! containsSourceFolderAsSubresource(selection))
			return true;
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("DeleteResourcesAction.deleteAction.confirm.message"); //$NON-NLS-1$
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), title, label);		
	}
	
	private static boolean containsSourceFolderAsSubresource(IStructuredSelection selection) throws CoreException{
		for (Iterator iter= selection.iterator(); iter.hasNext();){	
			Object each= iter.next();
			if (each instanceof IFolder && containsSourceFolder((IFolder)each))
				return true;
		}
		return false;
	}
	
	private static boolean containsSourceFolder(IFolder folder) throws CoreException{
		IResource[] subFolders= folder.members();
		for (int i = 0; i < subFolders.length; i++) {
			if (! (subFolders[i] instanceof IFolder))
				continue;
			IJavaElement element= JavaCore.create((IFolder)folder);
			if (element instanceof IPackageFragmentRoot)	
				return true;
			if (element instanceof IPackageFragment)	
				continue;
			if (containsSourceFolder((IFolder)subFolders[i]))
				return true;
		}
		return false;
	}
	
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		if (selection.isEmpty())
			setEnabled(false);
		else	
			setEnabled(ClipboardActionUtil.canActivate(new DeleteRefactoring(selection.toList(), null)));
	}
	
	private static boolean confirmDelete(IStructuredSelection selection) {
		Assert.isTrue(ClipboardActionUtil.getSelectedProjects(selection).isEmpty());
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= createConfirmationString(selection);
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), title, label);
	}
	
	private static String createConfirmationString(IStructuredSelection selection) {
		if (selection.size() == 1){
			Object firstElement= selection.getFirstElement();
			String pattern= createConfirmationStringForSingleElement(firstElement);
			return  MessageFormat.format(pattern, new String[]{getName(firstElement)});
		} else {
			String pattern= createConfirmationStringForMultipleElements(selection);
			return MessageFormat.format(pattern, new String[]{String.valueOf(selection.size())});
		}
	}
	
	private static String createConfirmationStringForSingleElement(Object firstElement) {
		if (isLinkedResource(firstElement))
			return "Are you sure you want to delete linked resource ''{0}''?\n" +
							"Only the workspace link will be deleted. Link target will remain unchanged.";
		else
			return "Are you sure you want to delete ''{0}''?";
	}
	
	private static String createConfirmationStringForMultipleElements(IStructuredSelection selection) {
		if (containsLinkedResources(selection))
			return "Are you sure you want to delete these {0} resources?\n\n" +
							"Selection contains linked resources\n" +
							"Only the workspace links will be deleted. Link targets will remain unchanged.";
		else	
			return "Are you sure you want to delete these {0} resources?";
	}
	
	private static boolean containsLinkedResources(IStructuredSelection selection) {
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (isLinkedResource(iter.next()))
				return true;
		}
		return false;
	}
	
	private static boolean isLinkedResource(Object element) {
		IResource resource= ResourceUtil.getResource(element);
		return (resource != null && resource.isLinked());
	}
	
	private static String getName(Object element){
		//need to render 1 case differently
		if (element instanceof IPackageFragment && ((IPackageFragment)element).isDefaultPackage())
			return "(default package)";
		else
			return ReorgUtils.getName(element);
	}
}
