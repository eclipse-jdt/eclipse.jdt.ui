/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.actions.NewWizardAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

public class NewGroup extends ContextMenuGroup {
		
	private AbstractOpenWizardAction fNewProjectAction;
	private AbstractOpenWizardAction fNewPackageAction;
	private AbstractOpenWizardAction fNewClassAction;
	private AbstractOpenWizardAction fNewInterfaceAction;
	private AbstractOpenWizardAction fNewPackageRootAction;
	private AbstractOpenWizardAction fNewSnippetAction;
	
	private NewWizardAction fNewWizardAction;
	
	private boolean fActionsCreated;
	
	
	public static final String GROUP_NAME= IContextMenuConstants.GROUP_NEW;
	
	public NewGroup() {
	}
	
	private void createActions() {
		
		if (fActionsCreated)
			return;
	
		String label= NewWizardMessages.getString("NewGroup.wizards_add_package.label"); //$NON-NLS-1$
		Class[] acceptedTypes= new Class[] { IJavaProject.class, IPackageFragmentRoot.class };			
		fNewPackageAction= new OpenPackageWizardAction(label, acceptedTypes);
		fNewPackageAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPACKAGE);
		fNewPackageAction.setToolTipText(NewWizardMessages.getString("NewGroup.wizards_add_package.tooltip")); //$NON-NLS-1$
		fNewPackageAction.setDescription(NewWizardMessages.getString("NewGroup.wizards_add_package.description"));			 //$NON-NLS-1$
		
		label= NewWizardMessages.getString("NewGroup.wizards_add_class.label"); //$NON-NLS-1$
		acceptedTypes= new Class[] { IJavaProject.class, IPackageFragmentRoot.class,
			IPackageFragment.class, ICompilationUnit.class, IClassFile.class };		
		
		fNewClassAction= new OpenClassWizardAction(label, acceptedTypes);
		fNewClassAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWCLASS);
		fNewClassAction.setToolTipText(NewWizardMessages.getString("NewGroup.wizards_add_class.tooltip")); //$NON-NLS-1$
		fNewClassAction.setDescription(NewWizardMessages.getString("NewGroup.wizards_add_class.description"));			 //$NON-NLS-1$
			
		label= NewWizardMessages.getString("NewGroup.wizards_add_interface.label"); //$NON-NLS-1$
		fNewInterfaceAction= new OpenInterfaceWizardAction(label, acceptedTypes);
		fNewInterfaceAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWINTERFACE);	
		fNewInterfaceAction.setToolTipText(NewWizardMessages.getString("NewGroup.wizards_add_interface.tooltip")); //$NON-NLS-1$
		fNewInterfaceAction.setDescription(NewWizardMessages.getString("NewGroup.wizards_add_interface.description")); //$NON-NLS-1$

		acceptedTypes= new Class[] { IJavaProject.class };
		label= NewWizardMessages.getString("NewGroup.wizards_add_snippet.label"); //$NON-NLS-1$
		fNewSnippetAction= new OpenSnippetWizardAction(label, acceptedTypes);
		fNewSnippetAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWSNIPPET);	
		fNewSnippetAction.setToolTipText(NewWizardMessages.getString("NewGroup.wizards_add_snippet.tooltip")); //$NON-NLS-1$
		fNewSnippetAction.setDescription(NewWizardMessages.getString("NewGroup.wizards_add_snippet.description")); //$NON-NLS-1$

		label= NewWizardMessages.getString("NewGroup.wizards_add_packageroot.label"); //$NON-NLS-1$
		acceptedTypes= new Class[] { IJavaProject.class };
		fNewPackageRootAction= 
			new AbstractOpenWizardAction(label, acceptedTypes, false) {
				protected Wizard createWizard() { return new NewSourceFolderCreationWizard(); }
				protected boolean shouldAcceptElement(Object obj) { return !isOnBuildPath(obj); }
			};
		fNewPackageRootAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPACKROOT);	
		fNewPackageRootAction.setToolTipText(NewWizardMessages.getString("NewGroup.wizards_add_packageroot.tooltip")); //$NON-NLS-1$
		fNewPackageRootAction.setDescription(NewWizardMessages.getString("NewGroup.wizards_add_packageroot.description")); //$NON-NLS-1$
			
		// find out if the new project wizard is registered in the registry
		label= NewWizardMessages.getString("NewGroup.wizards_add_project.label"); //$NON-NLS-1$
		acceptedTypes= new Class[] { IJavaModel.class };
		fNewProjectAction= new OpenProjectWizardAction(label, acceptedTypes);
		fNewProjectAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPROJECT);
		fNewProjectAction.setToolTipText(NewWizardMessages.getString("NewGroup.wizards_add_project.tooltip")); //$NON-NLS-1$
		fNewProjectAction.setDescription(NewWizardMessages.getString("NewGroup.wizards_add_project.description")); //$NON-NLS-1$
				
		fNewProjectAction= null;

		
		fNewWizardAction= new NewWizardAction();			
		fActionsCreated= true;	
	}
	
	static boolean isOnBuildPath(Object obj) {
		try {
			if (obj instanceof IJavaElement) {
				IJavaElement elem= (IJavaElement)obj;
				return JavaModelUtil.isOnBuildPath(elem.getJavaProject(), elem);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
		return false;
	}
	
	static boolean isInArchive(Object obj) {
		if (obj instanceof IJavaElement) {
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot((IJavaElement)obj);
			return (root != null) && root.isArchive();
		}
		return false;
	}	
			
	/**
	 * fill the context menu with new-wizard actions
	 */
	public void fill(IMenuManager menu, GroupContext context) {
		createActions();
		
		int i= 0;
		MenuManager manager= new MenuManager(NewWizardMessages.getString("NewGroup.wizards_add")); //$NON-NLS-1$
		if (fNewProjectAction.canActionBeAdded()) {
			manager.add(fNewProjectAction);
			i++;
		}		
		if (fNewPackageRootAction.canActionBeAdded()) {
			manager.add(fNewPackageRootAction);
			i++;
		}
		if (fNewPackageAction.canActionBeAdded()) {
			manager.add(fNewPackageAction);
			i++;
		}	
		if (fNewClassAction.canActionBeAdded()) {
			manager.add(fNewClassAction);
			i++;
		}
		if (fNewInterfaceAction.canActionBeAdded()) {
			manager.add(fNewInterfaceAction);
			i++;
		}
		if (fNewSnippetAction.canActionBeAdded()) {
			manager.add(fNewSnippetAction);
			i++;
		}		

		boolean added= i != 0;
		if (added) {
			manager.add(new Separator());
			manager.add(fNewWizardAction);
		}
		else
			manager.add(fNewWizardAction);
		menu.appendToGroup(GROUP_NAME, manager);
	}
	
	public String getGroupName() {
		return GROUP_NAME;
	}				

}