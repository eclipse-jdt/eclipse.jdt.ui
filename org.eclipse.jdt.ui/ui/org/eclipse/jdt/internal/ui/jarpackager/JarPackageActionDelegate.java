/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.ClassNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This abstract action delegate offers base functionality used by
 * other JAR Package based action delegates.
 * .
 */
public abstract class JarPackageActionDelegate implements IWorkbenchWindowActionDelegate {

	private IStructuredSelection fSelection;
	private IWorkbench fWorkbench;

	/*
	 * @see IWorkbenchWindowActionDelegate
	 */
	public void dispose() {
	}
	/**
	 * Returns the active shell.
	 */
	protected Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}
	/*
	 * @see IWorkbenchWindowActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		fWorkbench= window.getWorkbench();
	}
	/*
	 * @see IWorkbenchActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection)
			fSelection= (IStructuredSelection)selection;
		else
			fSelection= StructuredSelection.EMPTY;
	}
	/**
	 * Returns the description file for the first description file in
	 * the selection. Use this method if this action is only active if
	 * one single file is selected.
	 */
	protected IFile getDescriptionFile(IStructuredSelection selection) {
		return (IFile)selection.getFirstElement();
	}
	/**
	 * Returns a description file for each description file in
	 * the selection. Use this method if this action allows multiple
	 * selection.
	 */
	protected IFile[] getDescriptionFiles(IStructuredSelection selection) {
		IFile[] files= new IFile[selection.size()];
		Iterator iter= selection.iterator();
		int i= 0;
		while (iter.hasNext())
			files[i++]= (IFile)iter.next();
		return files;
	}
	/**
	 * Reads the JAR package spec from file.
	 */
	protected JarPackage readJarPackage(IFile description) throws CoreException, IOException {
		Assert.isLegal(description.isAccessible());
		Assert.isNotNull(description.getFileExtension());
		Assert.isLegal(description.getFileExtension().equals(JarPackage.DESCRIPTION_EXTENSION));
		JarPackageReader reader= null;
		JarPackage jarPackage= null;
		try {
			reader= new JarPackageReader(description.getContents());
			jarPackage= reader.readXML();
		} finally {
			if (reader != null)
				reader.close();
			return jarPackage;
		}
	}

	protected IWorkbench getWorkbench() {
		if (fWorkbench == null)
			fWorkbench= JavaPlugin.getActiveWorkbenchWindow().getWorkbench();
		if (fWorkbench == null)
			fWorkbench= JavaPlugin.getDefault().getWorkbench();
		return fWorkbench;
	}

	protected IStructuredSelection getSelection() {
		return fSelection;
	}
}