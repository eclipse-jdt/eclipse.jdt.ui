/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathContainerWizard;

/**
 * Wraps a PropertyPage around a ClasspathContainerWizard.
 * It is required, that the wizard consists of exactly one page.
 */
public class ClasspathContainerPreferencePage extends PropertyPage {

	private static final class PropertyPageWizardContainer implements IWizardContainer {

		private final IWizard fWizard;
		private final PropertyPage fPage;
		private String fMessage;

		private PropertyPageWizardContainer(PropertyPage page, IWizard wizard) {
			Assert.isLegal(wizard.getPageCount() == 1);

			fPage= page;
			fWizard= wizard;
		}

		public IWizardPage getCurrentPage() {
			return fWizard.getPages()[0];
		}

		public Shell getShell() {
			return fPage.getShell();
		}

		public void showPage(IWizardPage page) {
		}

		public void updateButtons() {
			fPage.setValid(fWizard.canFinish());
		}

		public void updateMessage() {
			IWizardPage page= getCurrentPage();

			String message= fPage.getMessage();
			if (message != null && fMessage == null)
				fMessage= message;

			if (page.getErrorMessage() != null) {
				fPage.setMessage(page.getErrorMessage(), ERROR);
			} else if (page instanceof IMessageProvider) {
				IMessageProvider messageProvider= (IMessageProvider) page;
				if (messageProvider.getMessageType() != IMessageProvider.NONE) {
					fPage.setMessage(messageProvider.getMessage(), messageProvider.getMessageType());
				} else {
					if (messageProvider.getMessage() != null && fMessage == null)
						fMessage= messageProvider.getMessage();

					fPage.setMessage(fMessage, NONE);
				}
			} else {
				fPage.setErrorMessage(null);
			}
		}

		public void updateTitleBar() {
			IWizardPage page= getCurrentPage();
			String name= page.getTitle();
			if (name == null)
				name= page.getName();

			fPage.setMessage(name);
		}

		public void updateWindowTitle() {
		}

		public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
			dialog.run(fork, cancelable, runnable);
		}
	}

	private ClasspathContainerWizard fWizard;
	private IJavaProject fJavaProject;
	private IClasspathEntry fEntry;
	private Composite fWizardPageContainer;

	public ClasspathContainerPreferencePage() {
		noDefaultAndApplyButton();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setElement(IAdaptable element) {
		super.setElement(element);

		ClassPathContainer container= (ClassPathContainer) element;
		fJavaProject= container.getJavaProject();
		fEntry= container.getClasspathEntry();
	}

	protected Control createContents(final Composite parent) {
		fWizardPageContainer= new Composite(parent, SWT.NONE);
		fWizardPageContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		fWizardPageContainer.setLayout(layout);

		createWizardPageContent(fWizardPageContainer);

		return fWizardPageContainer;
	}

	private void createWizardPageContent(Composite parent) {
		fWizard= createWizard();
		if (fWizard == null)
			return;

		Composite messageComposite= new Composite(parent, SWT.NONE);
		messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		messageComposite.setLayout(layout);

		Label messageLabel= new Label(messageComposite, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		IWizardPage page= fWizard.getPages()[0];
		fWizard.createPageControls(parent);

		if (page.getControl() == null)
			page.createControl(parent);

		Control pageControl= page.getControl();
		if (pageControl.getLayoutData() == null)
			pageControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setPageName(page);
		setDescription(page, messageLabel);

		page.setVisible(true);
		
		setValid(fWizard.canFinish());
	}

	private void setPageName(IWizardPage page) {
		String name= page.getTitle();
		if (name == null)
			name= page.getName();

		setMessage(name);
	}

	private void setDescription(IWizardPage page, Label messageLabel) {
		String description= null;
		if (page.getDescription() != null) {
			description= page.getDescription();
		} else if (page instanceof IMessageProvider) {
			IMessageProvider messageProvider= (IMessageProvider) page;
			if (messageProvider.getMessageType() == IMessageProvider.NONE) {
				description= messageProvider.getMessage();
			}
		}

		if (description != null) {
			messageLabel.setText(description);
		} else {
			messageLabel.setVisible(false);
		}
	}

	private ClasspathContainerWizard createWizard() {
		try {
			IJavaProject project= fJavaProject;
			IClasspathEntry[] entries= project.getRawClasspath();
			ClasspathContainerWizard result= new ClasspathContainerWizard(fEntry, project, entries);

			result.addPages();
			PropertyPageWizardContainer wizardContainer= new PropertyPageWizardContainer(this, result);
			wizardContainer.updateButtons();
			wizardContainer.updateMessage();
			result.setContainer(wizardContainer);

			return result;
		} catch (JavaModelException e) {
			String title= ActionMessages.ConfigureContainerAction_error_title;
			String message= ActionMessages.ConfigureContainerAction_error_creationfailed_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performOk() {
		fWizard.performFinish();
		applyContainerChange();
		fWizard.dispose();

		return super.performOk();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performCancel() {
		fWizard.performCancel();
		fWizard.dispose();

		return super.performCancel();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void performApply() {
		fWizard.performFinish();
		applyContainerChange();
		fWizard.dispose();

		rebuildWizardPage();

		super.performApply();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void performDefaults() {
		fWizard.performCancel();
		fWizard.dispose();

		rebuildWizardPage();

		super.performDefaults();
	}

	/**
	 * Rebuilds the wizard page based on the current classpath entry
	 */
	private void rebuildWizardPage() {
		Control[] children= fWizardPageContainer.getChildren();
		for (int i= 0; i < children.length; i++) {
			children[i].dispose();
		}

		createWizardPageContent(fWizardPageContainer);
		fWizardPageContainer.getParent().layout(true, true);
	}

	/**
	 * Apply the changes to the classpath
	 */
	private void applyContainerChange() {
		IClasspathEntry[] created= fWizard.getNewEntries();
		if (created == null || created.length != 1)
			return;

		IClasspathEntry result= created[0];
		if (result == null || result.equals(fEntry))
			return;

		try {
			IClasspathEntry[] entries= fJavaProject.getRawClasspath();

			int idx= indexInClasspath(entries, fEntry);
			if (idx == -1)
				return;

			final IClasspathEntry[] newEntries= new IClasspathEntry[entries.length];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[idx]= result;

			IRunnableContext context= new ProgressMonitorDialog(getShell());
			context= PlatformUI.getWorkbench().getProgressService();
			context.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						fJavaProject.setRawClasspath(newEntries, fJavaProject.getOutputLocation(), monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});

			fEntry= result;

		} catch (JavaModelException e) {
			String title= ActionMessages.ConfigureContainerAction_error_title;
			String message= ActionMessages.ConfigureContainerAction_error_creationfailed_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch (InvocationTargetException e) {
			String title= ActionMessages.ConfigureContainerAction_error_title;
			String message= ActionMessages.ConfigureContainerAction_error_applyingfailed_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch (InterruptedException e) {
			// user cancelled
		}
	}

	protected static int indexInClasspath(IClasspathEntry[] entries, IClasspathEntry entry) {
		for (int i= 0; i < entries.length; i++) {
			if (entries[i].equals(entry)) {
				return i;
			}
		}
		return -1;
	}

}
