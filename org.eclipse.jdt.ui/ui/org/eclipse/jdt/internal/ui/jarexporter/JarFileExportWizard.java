package org.eclipse.jdt.internal.ui.jarexporter;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.core.runtime.Platform;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Standard workbench wizard for exporting resources from the workspace
 * to a Java Archive (JAR) file.
 * <p>
 * This class may be instantiated and used without further configuration;
 * this class is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * <pre>
 * IWizard wizard = new JarFileExportWizard();
 * wizard.init(workbench, selection);
 * WizardDialog dialog = new WizardDialog(shell, wizard);
 * dialog.open();
 * </pre>
 * During the call to <code>open</code>, the wizard dialog is presented to the
 * user. When the user hits Finish, the user-selected workspace resources 
 * are exported to the user-specified zip file, the dialog closes, and the call
 * to <code>open</code> returns.
 * </p>
 * <p>
 * [Issue: JAR files are not generic enough to belong in the Eclipse
 *  platform. This class should move to the Java tooling component.
 * ]
 * </p>
 */
public class JarFileExportWizard extends Wizard implements IExportWizard {
	private IWorkbench workbench;
	private IStructuredSelection selection;
	private WizardJarFileExportPage1 mainPage;
	/**
	 * Creates a wizard for exporting workspace resources to a JAR file.
	 */
	public JarFileExportWizard() {
		AbstractUIPlugin plugin= (AbstractUIPlugin) Platform.getPlugin(PlatformUI.PLUGIN_ID);
		IDialogSettings workbenchSettings= plugin.getDialogSettings();
		IDialogSettings section= workbenchSettings.getSection("JarFileExportWizard");
		if (section == null)
			section= workbenchSettings.addNewSection("JarFileExportWizard");
		setDialogSettings(section);
	}

	/* (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public void addPages() {
		super.addPages();
		mainPage= new WizardJarFileExportPage1(selection);
		addPage(mainPage);
	}

	/**
	 * Returns the image descriptor with the given relative path.
	 */
	private ImageDescriptor getImageDescriptor(String relativePath) {
		try {
			AbstractUIPlugin plugin= (AbstractUIPlugin) Platform.getPlugin(PlatformUI.PLUGIN_ID);
			URL installURL= plugin.getDescriptor().getInstallURL();
			URL url= new URL(installURL, "icons/basic/" + relativePath);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// Should not happen
			return null;
		}
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbenchWizard.
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		this.workbench= workbench;
		selection= currentSelection;

		setWindowTitle("Export");
		setDefaultPageImageDescriptor(getImageDescriptor("wizban/exportjar_wiz.gif"));
		setNeedsProgressMonitor(true);
	}

	/* (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public boolean performFinish() {
		return mainPage.finish();
	}

}
