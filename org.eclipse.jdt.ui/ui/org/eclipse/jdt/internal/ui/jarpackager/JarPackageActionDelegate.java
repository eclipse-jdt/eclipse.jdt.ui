/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.xml.sax.SAXException;

/**
 * This abstract action delegate offers base functionality used by
 * other JAR Package based action delegates.
 * .
 */
public abstract class JarPackageActionDelegate implements IActionDelegate {

	private IStructuredSelection fSelection;
	private IWorkbench fWorkbench;
	private JarPackageReader fReader= null;

	/**
	 * Returns the active shell.
	 */
	protected Shell getShell() {
		return getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/*
	 * @see IActionDelegate
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
	protected JarPackage readJarPackage(IFile description) throws CoreException, IOException, SAXException {
		Assert.isLegal(description.isAccessible());
		Assert.isNotNull(description.getFileExtension());
		Assert.isLegal(description.getFileExtension().equals(JarPackage.DESCRIPTION_EXTENSION));
		JarPackage jarPackage= null;
		try {
			fReader= new JarPackageReader(description.getContents());
			jarPackage= fReader.readXML();
		} finally {
			if (fReader != null)
				fReader.close();
		}
		return jarPackage;
	}

	protected IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}

	protected IStructuredSelection getSelection() {
		return fSelection;
	}
	
	protected JarPackageReader getReader() {
		return fReader;
	}
}