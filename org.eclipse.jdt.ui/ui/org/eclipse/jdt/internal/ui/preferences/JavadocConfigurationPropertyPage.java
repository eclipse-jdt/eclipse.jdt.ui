/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Property page used to set the project's Javadoc location for sources
 */
public class JavadocConfigurationPropertyPage extends PropertyPage implements IStatusChangeListener {

	private JavadocConfigurationBlock fJavadocConfigurationBlock;

	public JavadocConfigurationPropertyPage() {
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		IJavaElement elem= getJavaElement();
		if (elem instanceof IPackageFragmentRoot)
			setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.IsPackageFragmentRoot.description")); //$NON-NLS-1$
		else if (elem instanceof IJavaProject)
			setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.IsJavaProject.description")); //$NON-NLS-1$
		else
			setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.IsIncorrectElement.description")); //$NON-NLS-1$

		super.createControl(parent);
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		IJavaElement elem= getJavaElement();
		fJavadocConfigurationBlock= new JavadocConfigurationBlock(elem, getShell(), this);
		Control control= fJavadocConfigurationBlock.createContents(parent);
		WorkbenchHelp.setHelp(control, IJavaHelpContextIds.JAVADOC_CONFIGURATION_PROPERTY_PAGE);

		return control;
	}

	private IJavaElement getJavaElement() {
		IAdaptable adaptable= getElement();
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem == null) {

			IResource resource= (IResource) adaptable.getAdapter(IResource.class);
			//special case when the .jar is a file
			try {
				if (resource instanceof IFile) {
					IProject proj= resource.getProject();
					if (proj.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jproject= JavaCore.create(proj);
						IPackageFragmentRoot root= jproject.findPackageFragmentRoot(resource.getFullPath());
						if (root != null && root.isArchive()) {
							elem= root;
						}
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return elem;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fJavadocConfigurationBlock.performDefaults();
		super.performDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		return fJavadocConfigurationBlock.performOk();
	}

	/**
	 * @see IStatusChangeListener#statusChanged(IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

}