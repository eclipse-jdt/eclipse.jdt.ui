package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

/**
 * The wizard specified by the JDIAttachLauncher to
 * designate the host, port and whether to allow termination of the remove VM.
 */
public class JDIAttachLauncherWizard extends Wizard implements ILaunchWizard {

	protected IStructuredSelection fSelection;

	protected ILauncher fLauncher;
	
	protected boolean fLastLaunchSuccessful;

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
					page.setPreferenceValues();
					JDIAttachLauncher launcher= getLauncher();
					launcher.setPort(page.getPort());
					launcher.setHost(page.getHost());
					launcher.setAllowTerminate(page.getAllowTerminate());
					fLastLaunchSuccessful= launcher.doLaunch(fSelection.getFirstElement(), fLauncher);
				}
			});
		} catch (InvocationTargetException ite) {
			return false;
		} catch (InterruptedException ie) {
			return false;
		}
		return fLastLaunchSuccessful;
	}

	/**
	 * @see ILauncher#getDelegate()
	 */
	protected JDIAttachLauncher getLauncher() {
		return (JDIAttachLauncher) fLauncher.getDelegate();
	}

	/**
	 * @see ILaunchWizard
	 */
	public void init(ILauncher launcher, String mode, IStructuredSelection selection) {
		fSelection= selection;
		fLauncher= launcher;
		setWindowTitle(DebugUIUtils.getResourceString("jdi_attach_launcher_wizard.title"));
	}
}
