package org.eclipse.jdt.internal.ui.packageview;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.Iterator;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaUIStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

 
/**
 * Open a resource (ClassFile, CompilationUnit, or ordinary resource) 
 * from the PackageViewer
 */
public class OpenResourceAction extends SelectionProviderAction implements ISelectionChangedListener {
	
	private final static String ERROR_OPEN_JAVA= "OpenResourceAction.openJava.error.";
	private final static String ERROR_OPEN_CLASS= "OpenResourceAction.openClass.error.";
	
	
	public OpenResourceAction(ISelectionProvider provider) {
		super(provider, "&Open");
		setDescription("Edit the file");
	}
	
	public void run() {
		Iterator iter= getStructuredSelection().iterator();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		while (iter.hasNext()) {
			
			Object element= iter.next();
			
			try {				
				EditorUtility.openInEditor(element);			
			} catch (JavaModelException x) {
				
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
					IJavaUIStatus.INTERNAL_ERROR, JavaPlugin.getResourceString(ERROR_OPEN_JAVA + "log"), x));
				
				ErrorDialog.openError(shell, 
					JavaPlugin.getResourceString(ERROR_OPEN_JAVA + "title"), 
					JavaPlugin.getResourceString(ERROR_OPEN_JAVA + "message"), 
					x.getStatus());
			
			} catch (PartInitException x) {
								
				String name= null;
				String key= null;
				
				if (element instanceof IClassFile) {
					name= ((IClassFile) element).getElementName();
					key= ERROR_OPEN_CLASS;
				} else if (element instanceof ICompilationUnit) {
					name= ((ICompilationUnit) element).getElementName();
					key= ERROR_OPEN_JAVA;
				} else if (element instanceof IResource) {
					name= ((IResource) element).getName();
					key= ERROR_OPEN_JAVA;
				}
				
				if (name != null && key != null) {
					MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),
						JavaPlugin.getResourceString(key + "title"), 
						JavaPlugin.getFormattedString(key + "message", 
							new String[] { name, x.getMessage() } ));			
				}
			}
		}
	}
	
	/**
	 * Set self's enablement based upon the currently-selected Resource elements
	 */
	public void selectionChanged(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty()) {
			setEnabled(false);
			return;
		}		
		Iterator resources= selection.iterator();
		if (resources == null) {
			setEnabled(false);
			return;
		}
		while (resources.hasNext()) {
			Object element= resources.next();
			if (canOpen(element)) {
				setEnabled(true);
			} else {
				setEnabled(false); 
				return;
			}
		}
	}
	
	public void update() {
		selectionChanged(getStructuredSelection());
	}
	
	boolean canOpen(Object o) {
		try {
			return (EditorUtility.getEditorInput(o) != null);
		} catch (JavaModelException x) {
		}
		return false;
	}
}