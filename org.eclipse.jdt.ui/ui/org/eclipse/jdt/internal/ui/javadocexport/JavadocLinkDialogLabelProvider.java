/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import java.net.URL;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;


public class JavadocLinkDialogLabelProvider extends JavaUILabelProvider {

	public String getText(Object element) {
		String text = super.getText(element);
		if ((element instanceof IJavaProject)
			|| (element instanceof IPackageFragmentRoot)) {

			String doc = ""; //$NON-NLS-1$
			try {
				URL url = JavaUI.getJavadocBaseLocation((IJavaElement) element);
				if (url != null) {
					doc = url.toExternalForm();
					Object[] args= new Object[] { text, doc };
					return JavadocExportMessages.getFormattedString("JavadocLinkDialogLabelProvider.configuredentry", args); //$NON-NLS-1$
				} else {
					return JavadocExportMessages.getFormattedString("JavadocLinkDialogLabelProvider.notconfiguredentry", text); //$NON-NLS-1$
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return text;

	}

}
