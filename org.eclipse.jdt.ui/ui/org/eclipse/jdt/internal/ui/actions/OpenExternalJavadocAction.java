
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;


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
	
	private Object getSelectedElement() {
		if (fSelectionProvider == null) {
			return null;
		}
		
		IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_DO_CODERESOLVE | StructuredSelectionProvider.FLAGS_GET_EDITOR_INPUT);
		if (selection.size() != 1)
			return null;
			
		return selection.getFirstElement();
	}
		
	public void run() {
		Object selected= getSelectedElement();
		if (!(selected instanceof IJavaElement)) {
			return;
		}
		
		IJavaElement jelem= (IJavaElement) selected;
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		try {
			String labelName= JavaElementLabels.getElementLabel(jelem, JavaElementLabels.M_PARAMETER_TYPES);
			
			URL baseURL= JavaDocLocations.getJavadocBaseLocation(jelem);
			if (baseURL == null) {
				IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(jelem);
				if (root != null && root.getKind() == IPackageFragmentRoot.K_BINARY) {
					String message= "The documentation location for ''{0}'' has not been configured. For elements from libraries specify the Javadoc location URL on the property page of the parent JAR (''{1}'').";	
					showError(shell, MessageFormat.format(message, new String[] { labelName, root.getElementName() }));
				} else {
					IJavaElement annotatedElement= jelem.getJavaProject();
					String message= "The documentation location for ''{0}'' has not been configured. For elements from source specify the Javadoc location URL on the property page of the parent project (''{1}'').";	
					showError(shell, MessageFormat.format(message, new String[] { labelName, annotatedElement.getElementName() }));
				}
				return;
			}
			if ("file".equals(baseURL.getProtocol())) {
				URL noRefURL= JavaDocLocations.getJavaDocLocation(jelem, false);
				if (!(new File(noRefURL.getFile())).isFile()) {
					String message= "No documentation available for ''{0}'' in ''{1}''.";
					showError(shell, MessageFormat.format(message, new String[] { labelName, baseURL.toExternalForm() }));
					return;
				}
			}
		
			URL url= JavaDocLocations.getJavaDocLocation(jelem, true);
			if (url != null) {
				openInBrowser(url, shell);
			} 		
		} catch (CoreException e) {
			JavaPlugin.log(e);
			showError(shell, "Opening Javadoc failed. See log for details.");
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
		OpenExternalJavadocAction.openInBrowser(url, shell);
	}

	
	private static void showError(final Shell shell, final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(shell, "Open External Javadoc", message);
			}
		});
	}
}
		