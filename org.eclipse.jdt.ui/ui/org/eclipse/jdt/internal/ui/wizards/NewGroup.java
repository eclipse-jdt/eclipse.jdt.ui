/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IWorkbench;
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
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class NewGroup extends ContextMenuGroup {
	
	private final static String WIZARDS_ADD= "NewGroup.wizards_add";
	private final static String WIZARDS_ADD_PROJECT= "NewGroup.wizards_add_project";
	private final static String WIZARDS_ADD_PACKAGE= "NewGroup.wizards_add_package";
	private final static String WIZARDS_ADD_CLASS= "NewGroup.wizards_add_class";
	private final static String WIZARDS_ADD_INTERFACE= "NewGroup.wizards_add_interface";		
	private final static String WIZARDS_ADD_PACKAGEROOT= "NewGroup.wizards_add_packageroot";	
	private final static String WIZARDS_ADD_SNIPPET= "NewGroup.wizards_add_snippet";
	
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
	
		String label= JavaPlugin.getResourceString(WIZARDS_ADD_PACKAGE + ".label");
		Class[] acceptedTypes= new Class[] { IJavaProject.class, IPackageFragmentRoot.class };			
		fNewPackageAction= new OpenPackageWizardAction(label, acceptedTypes);
		fNewPackageAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPACKAGE);
		fNewPackageAction.setToolTipText(JavaPlugin.getResourceString(WIZARDS_ADD_PACKAGE + ".tooltip"));
		fNewPackageAction.setDescription(JavaPlugin.getResourceString(WIZARDS_ADD_PACKAGE + ".description"));			
		
		label= JavaPlugin.getResourceString(WIZARDS_ADD_CLASS + ".label");
		acceptedTypes= new Class[] { IJavaProject.class, IPackageFragmentRoot.class,
			IPackageFragment.class, ICompilationUnit.class, IClassFile.class };		
		
		fNewClassAction= new OpenClassWizardAction(label, acceptedTypes);
		fNewClassAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWCLASS);
		fNewClassAction.setToolTipText(JavaPlugin.getResourceString(WIZARDS_ADD_CLASS + ".tooltip"));
		fNewClassAction.setDescription(JavaPlugin.getResourceString(WIZARDS_ADD_CLASS + ".description"));			
			
		label= JavaPlugin.getResourceString(WIZARDS_ADD_INTERFACE + ".label");
		fNewInterfaceAction= new OpenInterfaceWizardAction(label, acceptedTypes);
		fNewInterfaceAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWINTERFACE);	
		fNewInterfaceAction.setToolTipText(JavaPlugin.getResourceString(WIZARDS_ADD_INTERFACE + ".tooltip"));
		fNewInterfaceAction.setDescription(JavaPlugin.getResourceString(WIZARDS_ADD_INTERFACE + ".description"));

		acceptedTypes= new Class[] { IJavaProject.class };
		label= JavaPlugin.getResourceString(WIZARDS_ADD_SNIPPET + ".label");
		fNewSnippetAction= new OpenSnippetWizardAction(label, acceptedTypes);
		fNewSnippetAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWSNIPPET);	
		fNewSnippetAction.setToolTipText(JavaPlugin.getResourceString(WIZARDS_ADD_SNIPPET + ".tooltip"));
		fNewSnippetAction.setDescription(JavaPlugin.getResourceString(WIZARDS_ADD_SNIPPET + ".description"));

		label= JavaPlugin.getResourceString(WIZARDS_ADD_PACKAGEROOT + ".label");
		acceptedTypes= new Class[] { IJavaProject.class };
		fNewPackageRootAction= 
			new AbstractOpenWizardAction(label, acceptedTypes, false) {
				protected Wizard createWizard() { return new NewPackageRootCreationWizard(); }
				protected boolean shouldAcceptElement(Object obj) { return !isOnBuildPath(obj); }
			};
		fNewPackageRootAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPACKROOT);	
		fNewPackageRootAction.setToolTipText(JavaPlugin.getResourceString(WIZARDS_ADD_PACKAGEROOT + ".tooltip"));
		fNewPackageRootAction.setDescription(JavaPlugin.getResourceString(WIZARDS_ADD_PACKAGEROOT + ".description"));

			
		// find out if the new project wizard is registered in the registry
		if (isNewProjectWizardRegistered()) {
			label= JavaPlugin.getResourceString(WIZARDS_ADD_PROJECT + ".label");
			acceptedTypes= new Class[] { IJavaModel.class };
			fNewProjectAction= new OpenProjectWizardAction(label, acceptedTypes);
			fNewProjectAction.setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPROJECT);
			fNewProjectAction.setToolTipText(JavaPlugin.getResourceString(WIZARDS_ADD_PROJECT + ".tooltip"));
			fNewProjectAction.setDescription(JavaPlugin.getResourceString(WIZARDS_ADD_PROJECT + ".description"));
				
		} else {
			fNewProjectAction= null;
		}
		
		fNewWizardAction= new NewWizardAction();			
		fActionsCreated= true;	
	}
	
	static boolean isOnBuildPath(Object obj) {
		try {
			if (obj instanceof IJavaElement) {
				return JavaModelUtility.isOnBuildPath((IJavaElement)obj);
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return false;
	}
	
	static boolean isInArchive(Object obj) {
		if (obj instanceof IJavaElement) {
			IPackageFragmentRoot root= JavaModelUtility.getPackageFragmentRoot((IJavaElement)obj);
			return (root != null) && root.isArchive();
		}
		return false;
	}	
	
	
	private boolean isNewProjectWizardRegistered() {
		// XXX: check if still required
		/*IPluginRegistry registry= JavaPlugin.getDefault().getPluginRegistry();
		IExtensionPoint extPoint= registry.getExtensionPoint("org.eclipse.itp.desktop.new");
		if (extPoint != null) {
			IConfigurationElement[] conf= extPoint.getConfigurationElements();
			for (int i= 0; i < conf.length; i++) {
				if (NewProjectCreationWizard.NEW_PROJECT_WIZARD_ID.equals(conf[i].getAttribute("id"))) {
					return true;
				}
			}
		}*/
		return true;
	}
		
	/**
	 * fill the context menu with new-wizard actions
	 */
	public void fill(IMenuManager menu, GroupContext context) {
		createActions();
		
		int i= 0;
		MenuManager manager= new MenuManager(JavaPlugin.getResourceString(WIZARDS_ADD));
		if (fNewProjectAction != null && fNewProjectAction.canActionBeAdded()) {
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