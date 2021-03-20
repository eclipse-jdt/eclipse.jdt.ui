/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;

import org.osgi.framework.Bundle;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.actions.AbstractOpenWizardAction;
import org.eclipse.jdt.ui.actions.OpenNewClassWizardAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * A type wizard is added to the type drop down if it has a paramater 'javatype':
 *     <wizard
 *         name="My Type Wizard"
 *         icon="icons/wiz.png"
 *         category="mycategory"
 *         id="xx.MyWizard">
 *         <class class="org.xx.MyWizard">
 *             <parameter name="javatype" value="true"/>
 *         </class>
 *         <description>
 *             My Type Wizard
 *         </description>
 *      </wizard>
 */
public class NewTypeDropDownAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate2 {

	public static class OpenTypeWizardAction extends AbstractOpenWizardAction {

		private final static String ATT_NAME = "name";//$NON-NLS-1$
		private final static String ATT_CLASS = "class";//$NON-NLS-1$
		private final static String ATT_ICON = "icon";//$NON-NLS-1$
		private static final String TAG_DESCRIPTION = "description";	//$NON-NLS-1$
		private static final String TAG_ID = "id";	//$NON-NLS-1$

		private IConfigurationElement fConfigurationElement;

		public OpenTypeWizardAction(IConfigurationElement element) {
			fConfigurationElement= element;
			setId(getIdFromConfig(fConfigurationElement));
			setText(element.getAttribute(ATT_NAME));

			String description= getDescriptionFromConfig(fConfigurationElement);
			setDescription(description);
			setToolTipText(description);
			setImageDescriptor(getIconFromConfig(fConfigurationElement));
		}

		private String getIdFromConfig(IConfigurationElement config) {
			return config.getAttribute(TAG_ID);
		}

		private String getDescriptionFromConfig(IConfigurationElement config) {
			IConfigurationElement [] children = config.getChildren(TAG_DESCRIPTION);
			if (children.length>=1) {
				return children[0].getValue();
			}
			return ""; //$NON-NLS-1$
		}

		private ImageDescriptor getIconFromConfig(IConfigurationElement config) {
			String iconName = config.getAttribute(ATT_ICON);
			if (iconName != null) {
				Bundle bundle= Platform.getBundle(config.getContributor().getName());
				return JavaPluginImages.createImageDescriptor(bundle, new Path(iconName), true);
			}
			return null;
		}

		@Override
		protected INewWizard createWizard() throws CoreException {
			return (INewWizard) CoreUtility.createExtension(fConfigurationElement, ATT_CLASS);
		}
	}



	private final static String TAG_WIZARD = "wizard";//$NON-NLS-1$
	private final static String ATT_JAVATYPE = "javatype";//$NON-NLS-1$

	private final static String TAG_PARAMETER = "parameter";//$NON-NLS-1$
	private final static String TAG_NAME = "name";//$NON-NLS-1$
	private final static String TAG_VALUE = "value";//$NON-NLS-1$

	private static final String PL_NEW = "newWizards"; //$NON-NLS-1$
	private static final String TAG_CLASS = "class"; //$NON-NLS-1$

	private Menu fMenu;

	private Shell fWizardShell;

	public NewTypeDropDownAction() {
		fMenu= null;
		setMenuCreator(this);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_CLASS_WIZARD_ACTION);
	}

	@Override
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu= null;
		}
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public Menu getMenu(Control parent) {
		if (fMenu == null) {
			fMenu= new Menu(parent);
			for (OpenTypeWizardAction curr : getActionFromDescriptors()) {
				curr.setShell(fWizardShell);
				ActionContributionItem item= new ActionContributionItem(curr);
				item.fill(fMenu, -1);
			}

		}
		return fMenu;
	}

	@Override
	public void run() {
		new OpenNewClassWizardAction().run();
	}

	public static OpenTypeWizardAction[] getActionFromDescriptors() {
		ArrayList<OpenTypeWizardAction> containers= new ArrayList<>();

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(PlatformUI.PLUGIN_ID, PL_NEW);
		if (extensionPoint != null) {
			for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
				if (TAG_WIZARD.equals(element.getName()) && isJavaTypeWizard(element)) {
					containers.add(new OpenTypeWizardAction(element));
				}
			}
		}
		return containers.toArray(new OpenTypeWizardAction[containers.size()]);
	}

	private static boolean isJavaTypeWizard(IConfigurationElement element) {
		for (IConfigurationElement classElement : element.getChildren(TAG_CLASS)) {
			for (IConfigurationElement curr : classElement.getChildren(TAG_PARAMETER)) {
				if (ATT_JAVATYPE.equals(curr.getAttribute(TAG_NAME))) {
					return Boolean.parseBoolean(curr.getAttribute(TAG_VALUE));
				}
			}
		}
		// old way, deprecated
		if (Boolean.parseBoolean(element.getAttribute(ATT_JAVATYPE))) {
			return true;
		}
		return false;
	}


	@Override
	public void init(IWorkbenchWindow window) {
		fWizardShell= window.getShell();
	}

	@Override
	public void run(IAction action) {
		run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
