/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.oldlauncher;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * The wizard specified by the <code>JUnitLauncher</code> launcher to
 * designate the elements to launch
 */
public class JUnitLaunchWizard extends Wizard implements ILaunchWizard {
	protected String fMode;
	protected ILauncher fLauncher;
	protected Object[] fLauncheables;
	protected boolean fSuccess;

	public JUnitLaunchWizard() {
		super();
	}

	public JUnitLaunchWizard(Object[] launcheables) {
		fLauncheables= launcheables;
	}

	/*
	 * @see Wizard#addPages
	 */
	public void addPages() {
		addPage(new JUnitLaunchWizardPage(fLauncheables, getLauncher(), fMode));
	}

	/**
	 * Sets the chosen launcher and elements and performs the launch.
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					JUnitLaunchWizardPage page= (JUnitLaunchWizardPage) getContainer().getCurrentPage();
					fSuccess= fLauncher.launch(page.getElements(), fMode);
				}
			});
		} catch (InterruptedException e) {
			// do nothing user canceled 
		} catch (InvocationTargetException e) {
			Throwable te= e.getTargetException();
			JUnitPlugin.log(te);
			MessageDialog.openError(getShell(), "Could not launch JUnit", te.getMessage());			
		}
		return fSuccess;
	}

	/*
	 * @see ILauncher#getDelegate()
	 */
	protected JUnitBaseLauncherDelegate getLauncher() {
		return (JUnitBaseLauncherDelegate) fLauncher.getDelegate();
	}

	/*
	 * @see ILaunchWizard
	 */
	 public void init(ILauncher launcher, String mode, IStructuredSelection selection) {
		fMode= mode;
		fLauncher= launcher;
		setNeedsProgressMonitor(true);
		setWindowTitle("JUnit Test Finder");
		if (fLauncheables == null) {
			fLauncheables= getLauncher().getLaunchableElements(selection);
		}
		if (fLauncheables == null) {
			fLauncheables= new Object[0];
		}

	}
}
