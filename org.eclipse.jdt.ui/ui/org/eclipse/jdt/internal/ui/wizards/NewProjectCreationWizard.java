/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.IConfigurationElement;import org.eclipse.core.runtime.IExecutableExtension;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.launcher.VMWizardPage;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.ui.IPerspectiveDescriptor;import org.eclipse.ui.IPerspectiveRegistry;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PlatformUI;import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewProjectCreationWizard extends NewElementWizard implements IExecutableExtension {

	public static final String NEW_PROJECT_WIZARD_ID= "org.eclipse.jdt.ui.wizards.NewProjectCreationWizard";
		
	private static final String WIZARD_TITLE= "NewProjectCreationWizard.title";
	private static final String PREFIX_OP_ERROR= "NewProjectCreationWizard.op_error.";
	
	private static final String WZ_TITLE= "NewProjectCreationWizard.MainPage.title";
	private static final String WZ_DESC= "NewProjectCreationWizard.MainPage.description";

	private NewJavaProjectWizardPage fJavaPage;
	private WizardNewProjectCreationPage fMainPage;
	private VMWizardPage fVMPage;
	private IConfigurationElement fConfigElement;

	public NewProjectCreationWizard() {
		super();
		
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(JavaPlugin.getResourceString(WIZARD_TITLE));
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("id");
		fMainPage.setTitle(JavaPlugin.getResourceString(WZ_TITLE));
		fMainPage.setDescription(JavaPlugin.getResourceString(WZ_DESC));
		addPage(fMainPage);
		IWorkspaceRoot root= JavaPlugin.getWorkspace().getRoot();
		fJavaPage= new NewJavaProjectWizardPage(root, fMainPage);
		addPage(fJavaPage);
		fVMPage= new VMWizardPage(fMainPage);
		addPage(fVMPage);
	}		
	

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fJavaPage.getRunnable());
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			if (!ExceptionHandler.handle(e.getTargetException(), getShell(), JavaPlugin.getResourceBundle(), PREFIX_OP_ERROR)) {
				MessageDialog.openError(getShell(), "Error", e.getTargetException().getMessage());
			}
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		fVMPage.finish();
		updatePerspective();
		selectAndReveal(fJavaPage.getNewJavaProject().getProject());
		return true;
	}
		
	/**
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}
	/**
	 * Updates the perspective for the active page within the window.
	 */
	protected void updatePerspective() {
		// When there is no config info the wizard didn't get created
		// from the New wizard. Don't switch the perspective in this
		// case since we are already in the Java context.
		if (fConfigElement == null)
			return;
			
		// Read final persp from config.
		String perspID = fConfigElement.getAttribute("finalPerspective");
		if (perspID == null)
			return;
			
		// Map persp id to descriptor.
		IPerspectiveRegistry reg= PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor persp= reg.findPerspectiveWithId(perspID);
		if (persp == null)
			return;
	
		// Get the active page.
		IWorkbenchWindow dw= JavaPlugin.getActiveWorkbenchWindow();
		if (dw ==  null)
			return;
		IWorkbenchPage page= dw.getActivePage();
		if (page == null)
			return;
	
		// Set the perspective.
		page.setPerspective(persp);
	}

}