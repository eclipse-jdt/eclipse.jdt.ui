/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.help.IContext;
import org.eclipse.help.IHelp;
import org.eclipse.help.IHelpResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.javadoc.SingleCharReader;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.HTML2TextReader;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

public class JavaUIHelp {

	public static void setHelp(StructuredViewer viewer, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(viewer, contextId);
		viewer.getControl().addHelpListener(listener);
	}

	private static class JavaUIHelpListener implements HelpListener {

		private StructuredViewer fViewer;
		private String fContextId;

		public JavaUIHelpListener(StructuredViewer viewer, String contextId) {
			fViewer= viewer;
			fContextId= contextId;
		}

		/*
		* @see HelpListener#helpRequested(HelpEvent)
		* 
		*/
		public void helpRequested(HelpEvent e) {

			ISelection selection= fViewer.getSelection();
			Object selected= SelectionUtil.getSingleElement(selection);

			IHelp help= WorkbenchHelp.getHelpSupport();

			IContext context= help.getContext(fContextId);
			if (context == null) {
				return;
			}

			if (selected instanceof IJavaElement) {
				context= new JavaUIContext(context, (IJavaElement) selected);
			}

			WorkbenchHelp.displayHelp(context);
		}

	}

	private static class JavaUIContext implements IContext {

		private IJavaElement fElement;
		private IHelpResource[] fHelpResources;
		private IContext fContext;
		private String fText;

		public JavaUIContext(IContext context, IJavaElement element) {
			Assert.isNotNull(element);
			Assert.isNotNull(context);

			fElement= element;
			List helpResources= new ArrayList();
			fContext= context;

			IHelpResource[] resources= context.getRelatedTopics();
			if (resources != null) {
				for (int j= 0; j < resources.length; j++) {
					helpResources.add(resources[j]);
				}
			}

			try {
				URL url= JavaDocLocations.getJavaDocLocation(fElement, true);

				if (url == null || doesNotExist(url)) {

					if (fElement.getElementType() != IJavaElement.JAVA_PROJECT) {
						//must insure that what is sent here is lower in the hiarchy then IJavaProject
						IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(fElement);
						if (root != null)
							if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
								url= JavaDocLocations.getJavaDocLocation(root, true);
								element= root;
							} else if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
								url= JavaDocLocations.getJavaDocLocation(root.getJavaProject(), true);
								element= root.getJavaProject();
							}
					}
				}

				if (url != null) {
					String extForm= url.toExternalForm();

					IHelpResource javaResource= new JavaUIHelpResource(element, extForm);
					helpResources.add(javaResource);
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}

			fHelpResources= (IHelpResource[]) helpResources.toArray(new IHelpResource[helpResources.size()]);
		}

		private boolean doesNotExist(URL url) {
			if (url.getProtocol().equals("file")) { //$NON-NLS-1$
				File file= new File(url.getFile());
				return !file.exists();
			}
			return false;
		}

		public IHelpResource[] getRelatedTopics() {
			return fHelpResources;
		}

		public String getText() {

			if (fElement instanceof IMember) {

				try {
					SingleCharReader reader= JavaDocAccess.getJavaDoc((IMember) fElement, true);
					if (reader != null) {

						HTML2TextReader htmlReader= new HTML2TextReader(reader, null);
						String str= htmlReader.getString();

						BreakIterator breakIterator= BreakIterator.getSentenceInstance();
						breakIterator.setText(str);
						return str.substring(0, breakIterator.next());

					} else
						return fContext.getText();
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				} catch (IOException e) {
					JavaPlugin.log(e);
				}

			}

			return fContext.getText();

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

}