/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;import java.util.HashSet;import java.util.List;import java.util.Set;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaModel;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.dialogs.SelectionDialog;

public class GotoPackageAction extends Action {
	
	private PackageExplorerPart fPackageExplorer;
	private EmptyInnerPackageFilter fFilter;
	
	public GotoPackageAction(PackageExplorerPart part) {
		super(PackagesMessages.getString("GotoPackage.action.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("GotoPackage.action.description")); //$NON-NLS-1$
		fPackageExplorer= part;
		fFilter= new EmptyInnerPackageFilter();
	}
 
	public void run() { 
		try {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			SelectionDialog dialog= createAllPackagesDialog(shell);
			dialog.setTitle(PackagesMessages.getString("GotoPackage.dialog.title")); //$NON-NLS-1$
			dialog.setMessage(PackagesMessages.getString("GotoPackage.dialog.message")); //$NON-NLS-1$
			dialog.open();		
			Object[] res= dialog.getResult();
			if (res != null && res.length == 1) 
				gotoPackage((IPackageFragment)res[0]); 
		} catch (JavaModelException e) {
		}
	}
	
	SelectionDialog createAllPackagesDialog(Shell shell) throws JavaModelException{
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
		int flags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_CONTAINER;
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setIgnoreCase(false);
		dialog.setElements(allPackages.toArray()); // XXX inefficient
		return dialog;
	}
	
	void appendPackages(List all, IJavaElement[] packages) {
		for (int i= 0; i < packages.length; i++) {
			IJavaElement element= packages[i];
			if (fFilter.select(null, null, element))
				all.add(element); 
		}
	}
		
	void gotoPackage(IPackageFragment p) {
		fPackageExplorer.selectReveal(new StructuredSelection(p));
		return;
	}
	
	boolean showLibraries()  {
		return fPackageExplorer.getLibraryFilter().getShowLibraries();
	}
}