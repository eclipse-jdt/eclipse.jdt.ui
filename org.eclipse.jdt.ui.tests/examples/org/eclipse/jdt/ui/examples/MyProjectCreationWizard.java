package org.eclipse.jdt.ui.examples;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

/**
<extension 
	point="org.eclipse.ui.newWizards">
  	<wizard
		id="org.eclipse.jdt.ui.examples.MyProjectCreationWizard"
		name="My project"
		class="org.eclipse.jdt.ui.examples.MyProjectCreationWizard"
		category="org.eclipse.jdt.ui.java"
		project="true"
		finalPerspective="org.eclipse.jdt.ui.JavaPerspective"
		icon="icons/full/ctool16/newjprj_wiz.gif">
		<description>My project</description>
    </wizard>	 
</extension>
 */   


public class MyProjectCreationWizard extends Wizard implements IExecutableExtension, INewWizard {

	private WizardNewProjectCreationPage fMainPage;
	private JavaCapabilityConfigurationPage fJavaPage;
	
	private IConfigurationElement fConfigElement;

	private IWorkbench fWorkbench;
	private IStructuredSelection fSelection;
	
	public MyProjectCreationWizard() {
		setWindowTitle("New");
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("NewProjectCreationWizard");
		fMainPage.setTitle("New");
		fMainPage.setDescription("Create a new XY project.");
		addPage(fMainPage);
		fJavaPage= new JavaCapabilityConfigurationPage() {
			public void setVisible(boolean visible) {
				// need to override to set the latest project
				updatePage();
				super.setVisible(visible);
			}
		};
		addPage(fJavaPage);
	}
	
	private void updatePage() {
		IJavaProject jproject= JavaCore.create(fMainPage.getProjectHandle());
		if (!jproject.equals(fJavaPage.getJavaProject())) {
			fJavaPage.init(jproject, null, null, false);	
		}
	}
	
	
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		try {		
			monitor.beginTask("Creating XY project...", 3); // 3 steps

			IProject project= fMainPage.getProjectHandle();
			IPath locationPath= fMainPage.getLocationPath();
		
			// create the project
			IProjectDescription desc= project.getWorkspace().newProjectDescription(project.getName());
			if (Platform.getLocation().equals(locationPath)) {
				locationPath= null;
			}
			desc.setLocation(locationPath);
			project.create(desc, new SubProgressMonitor(monitor, 1));
			project.open(new SubProgressMonitor(monitor, 1));
			
			updatePage();
			fJavaPage.configureJavaProject(new SubProgressMonitor(monitor, 1));
			// TODO: configure your page / nature
	
			// change to the perspective specified in the plugin.xml		
			BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
			BasicNewResourceWizard.selectAndReveal(project, fWorkbench.getActiveWorkbenchWindow());
			
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
				finishPage(monitor);
			}
		};
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			return false; // TODO: open error dialog and log
		} catch  (InterruptedException e) {
			return false; // canceled
		}
		return true;
	}
			
	/*
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>finishPage</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fWorkbench= workbench;
		fSelection= selection; 
	}

}