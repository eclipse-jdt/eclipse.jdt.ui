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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.HTML2TextReader;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

public class JavaUIHelp {

	public static void setHelp(StructuredViewer viewer, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(viewer, contextId);
		viewer.getControl().addHelpListener(listener);
	}

	public static void setHelp(JavaEditor editor, StyledText text, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(editor, contextId);
		text.addHelpListener(listener);
	}

	private static class JavaUIHelpListener implements HelpListener {

		private StructuredViewer fViewer;
		private String fContextId;
		private JavaEditor fEditor;

		public JavaUIHelpListener(StructuredViewer viewer, String contextId) {
			fViewer= viewer;
			fContextId= contextId;
		}

		public JavaUIHelpListener(JavaEditor editor, String contextId) {
			fContextId= contextId;
			fEditor= editor;
		}

		/*
		* @see HelpListener#helpRequested(HelpEvent)
		* 
		*/
		public void helpRequested(HelpEvent e) {
			IHelp help= WorkbenchHelp.getHelpSupport();
			if (help == null) {
				return; // no help support installed
			}

			try {
				Object[] selected= null;
				if (fViewer != null) {
					ISelection selection= fViewer.getSelection();
					if (selection instanceof IStructuredSelection) {
						selected= ((IStructuredSelection) selection).toArray();
					}
				} else if (fEditor != null) {
					selected= SelectionConverter.codeResolve(fEditor);
				}

				IContext context= help.getContext(fContextId);
				if (selected != null && selected.length > 0) {
					context= new JavaUIContext(context, selected);
				}

				WorkbenchHelp.displayHelp(context);
			} catch (CoreException ex) {
				JavaPlugin.log(ex);
			}
		}
	}
	
	private static class JavaUIContext implements IContext {

		private IHelpResource[] fHelpResources;
		private String fText;

		public JavaUIContext(IContext context, Object[] elements) throws CoreException {
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
					
					URL url= JavaDocLocations.getJavadocLocation(element, true);
					if (url == null || doesNotExist(url)) {
						IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
						if (root != null) {
							url= JavaDocLocations.getJavadocBaseLocation(element);
							if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
								element= element.getJavaProject();
							} else {
								element= root;
							}
							url= JavaDocLocations.getJavadocLocation(element, false);
						}
					}
					if (url != null) {
						IHelpResource javaResource= new JavaUIHelpResource(element, url.toExternalForm());
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