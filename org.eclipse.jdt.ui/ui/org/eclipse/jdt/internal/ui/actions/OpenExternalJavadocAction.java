
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * On a selected member; opens the Javadoc in an external browser (if existing)
 */
public class OpenExternalJavadocAction extends Action implements IUpdate, IObjectActionDelegate {
	private StructuredSelectionProvider fSelectionProvider;
	
	/**
	 * Use only for IWorkbenchWindowActionDelegates!
	 */ 
	public OpenExternalJavadocAction() {
		this(null);
	}
	
	public OpenExternalJavadocAction(StructuredSelectionProvider provider) {
		super();
		setText("Open E&xternal Javadoc@Shift+F2");
		setDescription("Opens the Javadoc of the selected element in an external browser");
		setToolTipText("Opens the Javadoc of the selected element in an external browser");
		fSelectionProvider= provider;
	}
	
	public void update() {
		setEnabled(canOperateOn());
	}
	
	private boolean canOperateOn() {
		if (fSelectionProvider != null) {
			IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_GET_EDITOR_INPUT);
			return selection.size() == 1;
		}
		return false;
	}
	
	private IJavaElement getDocumentedElement() {
		if (fSelectionProvider == null) {
			return null;
		}
		
		IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_DO_CODERESOLVE | StructuredSelectionProvider.FLAGS_GET_EDITOR_INPUT);
		if (selection.size() != 1)
			return null;
		
		Object obj= selection.getFirstElement();
		if (obj instanceof IJavaElement) {
			IJavaElement elem= (IJavaElement) obj;
			int type= elem.getElementType();
			if (type == IJavaElement.JAVA_MODEL || type == IJavaElement.JAVA_PROJECT) {
				return null;
			}
			if (type == IJavaElement.IMPORT_DECLARATION) {
				return elem;
			}
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(elem);
			if (JavaDocLocations.getJavadocLocation(root.getPath()) != null) {
				return elem;
			}
		}
		return null;
	}
	
	public void run() {
		IJavaElement element= getDocumentedElement();
		if (element == null) {
			return;
		}
		try {
			URL url= JavaDocLocations.getJavaDocLocation(element);
			if (url != null) {
				openInBrowser(url, JavaPlugin.getActiveWorkbenchShell());
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			String title= "Open External Javadoc";
			String message= "Opening Javadoc failed. See log for details.";
			ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message, e.getStatus());
		}
	}
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		run();
	}
	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fSelectionProvider= StructuredSelectionProvider.createFrom(targetPart.getSite().getWorkbenchWindow().getSelectionService());
	}
	
	
	private static boolean webBrowserOpened = false;
	
	
	/**
	 * Copied from AboutPluginsDialog.openMoreInfo
	 */
	public static void openInBrowser(final URL url, final Shell shell) {
		//System.out.println("Opening " + url.toExternalForm());
		
		if (SWT.getPlatform().equals("win32")) {	//$NON-NLS-1$
			Program.launch(url.toString());
		} else {
			Thread launcher = new Thread("External Javadoc Launcher") {	//$NON-NLS-1$
				public void run() {
					try {
						if (webBrowserOpened) {
							Runtime.getRuntime().exec("netscape -remote openURL(" + url.toString() + ")");	//$NON-NLS-1$
						} else {
							Process p = Runtime.getRuntime().exec("netscape " + url.toString());	//$NON-NLS-1$
							webBrowserOpened = true;
							try {
								if (p != null)
									p.waitFor();
							} catch (InterruptedException e) {
								MessageDialog.openError(shell, "Open External Javadoc", e.getMessage());
							} finally {
								webBrowserOpened = false;
							}
						}
					} catch (IOException e) {
						MessageDialog.openError(shell, "Open External Javadoc", e.getMessage());
					}
				}
			};
			launcher.start();
		}		
	}
	
	
	
}