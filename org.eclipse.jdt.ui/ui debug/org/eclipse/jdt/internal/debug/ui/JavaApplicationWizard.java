package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.launcher.JavaApplicationLauncher;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

/**
 * The wizard specified by the <code>JavaApplication</code> launcher to
 * designate the elements to launch
 */
public class JavaApplicationWizard extends Wizard implements ILaunchWizard {
	protected String fMode;
	protected ILauncher fLauncher;
	protected List fLauncheables;

	public JavaApplicationWizard() {
	}

	public JavaApplicationWizard(List launcheables) {
		fLauncheables= launcheables;
	}

	/**
	 * @see Wizard#addPages
	 */
	public void addPages() {
		addPage(new JavaApplicationWizardPage(fLauncheables, getLauncher(), fMode));
	}

	/**
	 * Set the chosen launcher and elements
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					JavaApplicationWizardPage page= (JavaApplicationWizardPage) getContainer().getCurrentPage();
					// do the launching
					Object[] elements= page.getElements();
					fLauncher.launch(elements, fMode);
				}
			});
		} catch (InvocationTargetException ite) {
			return false;
		} catch (InterruptedException ie) {
			return false;
		}

		return true;
	}

	protected JavaApplicationLauncher getLauncher() {
		return (JavaApplicationLauncher) fLauncher.getDelegate();
	}

	public void init(ILauncher launcher, String mode, IStructuredSelection selection) {
		fMode= mode;
		fLauncher= launcher;
		if (fLauncheables == null)
			fLauncheables= getLauncher().getLaunchableElements(selection, fMode);
		if (fLauncheables == null)
			fLauncheables= Collections.EMPTY_LIST;
		setNeedsProgressMonitor(true);
		setWindowTitle(DebugUIUtils.getResourceString("java_application_wizard.title"));
	}

}
