/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.corext.javadoc.SingleCharReader;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.HTML2TextReader;

public class JavadocHelpContext implements IContext {
	
	
	public static void displayHelp(String contextId, Object[] selected) throws CoreException {
		IContext context= HelpSystem.getContext(contextId);
		if (context != null) {
			if (selected != null && selected.length > 0) {
				context= new JavadocHelpContext(context, selected);
			}
			WorkbenchHelp.displayHelp(context);
		}
	}
	
	
	private static class JavaUIHelpResource implements IHelpResource {

		private IJavaElement fElement;
		private String fUrl;

		public JavaUIHelpResource(IJavaElement element, String url) {
			fElement= element;
			fUrl= url;
		}

		public String getHref() {
			return fUrl;
		}

		public String getLabel() {
			String label= JavaElementLabels.getTextLabel(fElement, JavaElementLabels.ALL_DEFAULT);
			return JavaUIMessages.getFormattedString("JavaUIHelp.link.label", label); //$NON-NLS-1$
		}
	}	
	

	private IHelpResource[] fHelpResources;
	private String fText;

	public JavadocHelpContext(IContext context, Object[] elements) throws CoreException {
		Assert.isNotNull(elements);

		List helpResources= new ArrayList();

		if (context != null) {
			IHelpResource[] resources= context.getRelatedTopics();
			if (resources != null) {
				for (int j= 0; j < resources.length; j++) {
					helpResources.add(resources[j]);
				}
			}
		}

		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IJavaElement) {
				IJavaElement element= (IJavaElement) elements[i];
				if (fText == null) {
					fText= retrieveText(element);
				} else {
					fText= ""; // no doc on multiple selection //$NON-NLS-1$
				}					
				
				URL url= JavaUI.getJavadocLocation(element, true);
				if (url == null || doesNotExist(url)) {
					IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
					if (root != null) {
						url= JavaUI.getJavadocBaseLocation(element);
						if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
							element= element.getJavaProject();
						} else {
							element= root;
						}
						url= JavaUI.getJavadocLocation(element, false);
					}
				}
				if (url != null) {
					IHelpResource javaResource= new JavaUIHelpResource(element, url.toExternalForm() + "?noframes=true" ); //$NON-NLS-1$
					helpResources.add(javaResource);
				}
			}
		}
		fHelpResources= (IHelpResource[]) helpResources.toArray(new IHelpResource[helpResources.size()]);
		if (fText == null || fText.length() == 0) {
			if (context != null) {
				fText= context.getText();
			}
		}
		if (fText != null && fText.length() == 0) {
			fText= null; // see 14207 
		}
		
	}

	private boolean doesNotExist(URL url) {
		if (url.getProtocol().equals("file")) { //$NON-NLS-1$
			File file= new File(url.getFile());
			return !file.exists();
		}
		return false;
	}

	private String retrieveText(IJavaElement elem) throws JavaModelException {
		if (elem instanceof IMember) {
			try {
				SingleCharReader reader= JavaDocAccess.getJavaDoc((IMember) elem, true);
				if (reader != null) {
					HTML2TextReader htmlReader= new HTML2TextReader(reader, null);
					String str= htmlReader.getString();

					BreakIterator breakIterator= BreakIterator.getSentenceInstance();
					breakIterator.setText(str);
					return str.substring(0, breakIterator.next());
				}
			} catch (IOException e) {
				JavaPlugin.log(e); // ignore
			}
		}
		return ""; //$NON-NLS-1$
	}

	public IHelpResource[] getRelatedTopics() {
		return fHelpResources;
	}

	public String getText() {
		return fText;
	}

}

