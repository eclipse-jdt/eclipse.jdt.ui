/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

/**
 * The wizard specified by the <code>Launcher</code> launcher to
 * designate the elements to launch
 */
public class ApplicationWizard extends Wizard implements ILaunchWizard {
	protected String fMode;
	protected ILauncher fLauncher;
	protected List fLauncheables;
	protected boolean fSuccess;

	public ApplicationWizard() {
		super();
	}

	public ApplicationWizard(List launcheables) {
		fLauncheables= launcheables;
	}

	/**
	 * @see Wizard#addPages
	 */
	public void addPages() {
		addPage(new ApplicationWizardPage(fLauncheables, getLauncher(), fMode));
	}

	/**
	 * Sets the chosen launcher and elements and performs the launch.
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					ApplicationWizardPage page= (ApplicationWizardPage) getContainer().getCurrentPage();
					fSuccess= fLauncher.launch(page.getElements(), fMode);
				}
			});
		} catch (InterruptedException e) {
			// do nothing user canceled 
		} catch (InvocationTargetException e) {
			BaseLauncherUtil.logNshowException("JUnit wizard error", e);
		}
		return fSuccess;
	}

	/**
	 * @see ILauncher#getDelegate()
	 */
	protected BaseLauncher getLauncher() {
		return (BaseLauncher) fLauncher.getDelegate();
	}

	/**
	 * @see ILaunchWizard
	 */
	 public void init(ILauncher launcher, String mode, IStructuredSelection selection) {
		fMode= mode;
		fLauncher= launcher;
		setNeedsProgressMonitor(true);
		setWindowTitle("JUnit Test Selector");
	}
}
