/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.JavadocExportWizardPage;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;
import org.eclipse.jdt.ui.wizards.JavadocExportWizardPage.IJavadocExportWizardPageContainer;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

import org.w3c.dom.Element;

public class ContributedJavadocWizardPage extends NewElementWizardPage implements IJavadocExportWizardPageContainer {
	
	private static class ErrorJavadocExportWizardPage extends JavadocExportWizardPage {

		public Control createContents(Composite parent) {
			Label label= new Label(parent, SWT.NONE);
			label.setText(JavadocExportMessages.ContributedJavadocWizardPage_error_create_page);
			return label;
		}
	}

	private static final String ATT_EXTENSION = "javadocExportWizardPage"; //$NON-NLS-1$

	private static final String ATT_ID = "id"; //$NON-NLS-1$
	private static final String ATT_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String ATT_PAGE_CLASS = "class"; //$NON-NLS-1$
	
	
	private IConfigurationElement fConfigElement;
	private JavadocExportWizardPage fPage;
	private final JavadocOptionsManager fStore;

	public ContributedJavadocWizardPage(IConfigurationElement configElement, JavadocOptionsManager store) {
		super(configElement.getAttribute(ATT_ID));
		fConfigElement = configElement;
		fStore= store;
		fPage= null;
		
		setTitle(JavadocExportMessages.JavadocWizardPage_javadocwizardpage_description); 
		setDescription(fConfigElement.getAttribute(ATT_DESCRIPTION));
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Control createContents= getPage().createContents(parent);
		setControl(createContents);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewElementWizardPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		getPage().setVisible(visible);
		super.setVisible(visible);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.JavadocExportWizardPage.IJavadocExportWizardPageContainer#getRunnableContext()
	 */
	public IRunnableContext getRunnableContext() {
		return getContainer();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.JavadocExportWizardPage.IJavadocExportWizardPageContainer#statusUpdated()
	 */
	public void statusUpdated() {
		updateStatus(getPage().getStatus());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#performHelp()
	 */
	public void performHelp() {
		getPage().performHelp();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		if (fPage != null) {
			fPage.dispose();
			fPage= null;
		}
		super.dispose();
	}
	
	public String[] getConfiguredJavadocOptions() {
		ArrayList vmArgs= new ArrayList();
		ArrayList toolArgs= new ArrayList();
		fStore.getArgumentArray(vmArgs, toolArgs);
		return (String[]) toolArgs.toArray(new String[toolArgs.size()]);
	}

	public String[] getConfiguredVMOptions() {
		ArrayList vmArgs= new ArrayList();
		ArrayList toolArgs= new ArrayList();
		fStore.getArgumentArray(vmArgs, toolArgs);
		return (String[]) vmArgs.toArray(new String[vmArgs.size()]);
	}
	
	public IJavaElement[] getSelectedJavaElements() {
		IJavaElement[] sourceElements= fStore.getSourceElements();
		if (sourceElements != null) {
			return (IJavaElement[]) sourceElements.clone();
		}
		return new IJavaElement[0];
	}
	
	public void updateArguments(ArrayList vmOptions, ArrayList toolOptions) {
		getPage().updateArguments(vmOptions, toolOptions);
	}

	public void updateAntScript(Element xmlDocument) {
		getPage().updateAntScript(xmlDocument);
	}

	private JavadocExportWizardPage getPage() {
		if (fPage == null) {
			try {
				Object elem= CoreUtility.createExtension(fConfigElement, ATT_PAGE_CLASS);
				if (elem instanceof JavadocExportWizardPage) {
					fPage= (JavadocExportWizardPage) elem;
					fPage.setContainer(this);
					statusUpdated();
					return fPage;
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
			fPage= new ErrorJavadocExportWizardPage();
		}
		return fPage;
	}
	
	public static ContributedJavadocWizardPage[] getContributedPages(JavadocOptionsManager store) {
		ArrayList pages= new ArrayList();
		
		IConfigurationElement[] elements= Platform.getExtensionRegistry().getConfigurationElementsFor(JavaUI.ID_PLUGIN, ATT_EXTENSION); 
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement curr= elements[i];
			String id= curr.getAttribute(ATT_ID);
			String description= curr.getAttribute(ATT_DESCRIPTION);
			String pageClassName= curr.getAttribute(ATT_PAGE_CLASS);
			
			if (id == null || description == null || pageClassName == null) {
				JavaPlugin.logErrorMessage("Invalid extension " + curr.toString()); //$NON-NLS-1$
				continue;
			}
			pages.add(new ContributedJavadocWizardPage(elements[i], store));
		}
		return (ContributedJavadocWizardPage[]) pages.toArray(new ContributedJavadocWizardPage[pages.size()]);
	}





}
