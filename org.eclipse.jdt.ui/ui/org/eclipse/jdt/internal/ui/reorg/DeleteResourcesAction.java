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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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

		try{
					
		if (!confirmDelete(selection))
			return;

		if (hasReadOnlyResources(selection) && !isOkToDeleteReadOnly()) 
			return;

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
		String messagePattern= 	ReorgMessages.getString("DeleteResourcesAction.referenced"); //$NON-NLS-1$
		return new PackageFragmentRootManipulationQuery(getShell(), ReorgMessages.getString("DeleteResourcesAction.Delete"), messagePattern); //$NON-NLS-1$
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
	
	private static boolean confirmDelete(IStructuredSelection selection) throws JavaModelException {
		Assert.isTrue(ClipboardActionUtil.getSelectedProjects(selection).isEmpty());
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= createConfirmationString(selection);
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), title, label);
	}
	
	private static String createConfirmationString(IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1){
			Object firstElement= selection.getFirstElement();
			String pattern= createConfirmationStringForSingleElement(firstElement);
			return  MessageFormat.format(pattern, new String[]{getName(firstElement)});
		} else {
			String pattern= createConfirmationStringForMultipleElements(selection);
			return MessageFormat.format(pattern, new String[]{String.valueOf(selection.size())});
		}
	}
	
	private static String createConfirmationStringForSingleElement(Object firstElement) throws JavaModelException {
		if (isDefaultPackageWithLinkedFiles(firstElement))	
			return ReorgMessages.getString("DeleteResourcesAction.sure_delete_linked_multiple"); //$NON-NLS-1$
	
		if (! isLinkedResource(firstElement))
			return ReorgMessages.getString("DeleteResourcesAction.sure_delete"); //$NON-NLS-1$
		
		if (! isLinkedPackageOrPackageFragmentRoot(firstElement))	
			return ReorgMessages.getString("DeleteResourcesAction.sure_delete_linked_single"); //$NON-NLS-1$

		//XXX workaround for jcore bugs - linked packages or source folders cannot be deleted properly		
		return ReorgMessages.getString("DeleteResourcesAction.sure_delete_linked_single_package_or_pfr"); //$NON-NLS-1$
	}
	
	private static String createConfirmationStringForMultipleElements(IStructuredSelection selection) throws JavaModelException {
		if (! containsLinkedResources(selection))
			return ReorgMessages.getString("DeleteResourcesAction.sure_delete_resources"); //$NON-NLS-1$

		if (! containLinkedPackagesOrPackageFragmentRoots(selection))	
			return ReorgMessages.getString("DeleteResourcesAction.sure_delete_linked_multiple"); //$NON-NLS-1$
		
		//XXX workaround for jcore bugs - linked packages or source folders cannot be deleted properly
		return ReorgMessages.getString("DeleteResourcesAction.sure_delete_linked_multiple_with_packages_or_pfr"); //$NON-NLS-1$
	}
	
	private static boolean containLinkedPackagesOrPackageFragmentRoots(IStructuredSelection selection) {
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (isLinkedPackageOrPackageFragmentRoot(iter.next()))
				return true;
		}
		return false;
	}

	private static boolean containsLinkedResources(IStructuredSelection selection) throws JavaModelException {
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (isLinkedResource(element))
				return true;
			if (isDefaultPackageWithLinkedFiles(element))
				return true;
		}
		return false;
	}

	private static boolean isLinkedPackageOrPackageFragmentRoot(Object object) {
		if ((object instanceof IPackageFragment) || (object instanceof IPackageFragmentRoot))
			return isLinkedResource(ResourceUtil.getResource(object));
		else
			return false;	
	}
	
	private static boolean isLinkedResource(Object element) {
		IResource resource= ResourceUtil.getResource(element);
		return (resource != null && resource.isLinked());
	}
	
	private static boolean isDefaultPackageWithLinkedFiles(Object firstElement) throws JavaModelException {
		if (! isDefaultPackage(firstElement))
			return false;
		IPackageFragment defaultPackage= (IPackageFragment)firstElement;
		ICompilationUnit[] cus= defaultPackage.getCompilationUnits();
		for (int i= 0; i < cus.length; i++) {
			if (isLinkedResource(cus[i]))
				return true;
		}
		return false;
	}

	private static boolean isDefaultPackage(Object firstElement) {
		return (firstElement instanceof IPackageFragment && ((IPackageFragment)firstElement).isDefaultPackage());
	}

	private static String getName(Object element){
		//need to render 1 case differently
		if (isDefaultPackage(element))
			return ReorgMessages.getString("DeleteResourcesAction.default_package"); //$NON-NLS-1$
		else
			return ReorgUtils.getName(element);
	}
}
