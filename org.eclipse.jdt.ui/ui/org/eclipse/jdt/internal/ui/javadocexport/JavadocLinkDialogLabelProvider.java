/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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
