package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import java.lang.reflect.InvocationTargetException;

/**
 * The wizard specified by the <code>JDIAttachLauncher</code> to
 * designate the host, port and project for the launch.
 */
public class JDIAttachLauncherWizard extends Wizard implements ILaunchWizard {

	protected String fMode;

	protected IStructuredSelection fSelection;

	protected ILauncher fLauncher;

	/**
	 * @see Wizard#addPages
	 */
	public void addPages() {
		setNeedsProgressMonitor(true);
		addPage(new JDIAttachLauncherWizardPage());
	}

	/**
	 * Configures the attach launch and starts the launch
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					JDIAttachLauncherWizardPage page= (JDIAttachLauncherWizardPage) getContainer().getCurrentPage();
					// do the launching
					String port= page.getPort();
					String host= page.getHost();
					page.setPreferenceValues();
					JDIAttachLauncher launcher= getLauncher();
					launcher.setPort(port);
					launcher.setHost(host);
					launcher.setAllowTerminate(page.getAllowTerminate());
					launcher.doLaunch(fSelection.getFirstElement(), fLauncher);
				}
			});
		} catch (InvocationTargetException ite) {
			return false;
		} catch (InterruptedException ie) {
			return false;
		}
		return true;
	}

	protected JDIAttachLauncher getLauncher() {
		return (JDIAttachLauncher) fLauncher.getDelegate();
	}

	public void init(ILauncher launcher, String mode, IStructuredSelection selection) {
		fMode= mode;
		fSelection= selection;
		fLauncher= launcher;
		setWindowTitle(DebugUIUtils.getResourceString("jdi_attach_launcher_wizard.title"));
	}

}
