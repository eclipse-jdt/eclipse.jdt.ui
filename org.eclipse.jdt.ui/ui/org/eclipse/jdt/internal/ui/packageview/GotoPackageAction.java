/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.action.Action;

import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

class GotoPackageAction extends Action {
	
	private PackageExplorerPart fPackageExplorer;
	private EmptyInnerPackageFilter fFilter;
	
	GotoPackageAction(PackageExplorerPart part) {
		super(PackagesMessages.getString("GotoPackage.action.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("GotoPackage.action.description")); //$NON-NLS-1$
		fPackageExplorer= part;
		fFilter= new EmptyInnerPackageFilter();
	}
 
	public void run() { 
		try {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			SelectionDialog dialog= createAllPackagesDialog(shell);
			dialog.setTitle(getDialogTitle());
			dialog.setMessage(PackagesMessages.getString("GotoPackage.dialog.message")); //$NON-NLS-1$
			dialog.open();		
			Object[] res= dialog.getResult();
			if (res != null && res.length == 1) 
				gotoPackage((IPackageFragment)res[0]); 
		} catch (JavaModelException e) {
		}
	}
	
	private SelectionDialog createAllPackagesDialog(Shell shell) throws JavaModelException{
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(
			shell, 
			new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_ROOT|JavaElementLabelProvider.SHOW_POST_QUALIFIED)
		);
		dialog.setIgnoreCase(false);
		dialog.setElements(collectPackages()); // XXX inefficient
		return dialog;
	}
	
	private Object[] collectPackages() throws JavaModelException {
		IWorkspaceRoot wsroot= JavaPlugin.getWorkspace().getRoot();
		IJavaModel model= JavaCore.create(wsroot);
		IJavaProject[] projects= model.getJavaProjects();
		Set set= new HashSet(); 
		List allPackages= new ArrayList();
		for (int i= 0; i < projects.length; i++) {
			IPackageFragmentRoot[] roots= projects[i].getPackageFragmentRoots();	
			for (int j= 0; j < roots.length; j++) {
				IPackageFragmentRoot root= roots[j];
				if (root.isArchive() && !showLibraries()) 
					continue;
		 		if (!set.contains(root)) {
					set.add(root);
					IJavaElement[] packages= root.getChildren();
					appendPackages(allPackages, packages);
				}
			}
		}
		return allPackages.toArray();
	}
	
	private void appendPackages(List all, IJavaElement[] packages) {
		for (int i= 0; i < packages.length; i++) {
			IJavaElement element= packages[i];
			if (fFilter.select(null, null, element))
				all.add(element); 
		}
	}
		
	private void gotoPackage(IPackageFragment p) {
		fPackageExplorer.selectReveal(new StructuredSelection(p));
		if (!p.equals(getSelectedElement())) {
			MessageDialog.openInformation(fPackageExplorer.getSite().getShell(), 
				getDialogTitle(), 
				PackagesMessages.getFormattedString("PackageExplorer.element_not_present", p.getElementName())); //$NON-NLS-1$
		}
	}
	
	private boolean showLibraries()  {
		return fPackageExplorer.getLibraryFilter().getShowLibraries();
	}
	
	private Object getSelectedElement() {
		return ((IStructuredSelection)fPackageExplorer.getSite().getSelectionProvider().getSelection()).getFirstElement();
	}	
	
	private String getDialogTitle() {
		return PackagesMessages.getString("GotoPackage.dialog.title"); //$NON-NLS-1$
	}
	
}