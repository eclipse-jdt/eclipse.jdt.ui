package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

/**
 * Deletes ISourceManipulations such as IMethod or IType from its container. Works only
 * on working copies.
 */
public class DeleteISourceManipulationsAction extends JavaUIAction implements ISelectionChangedListener {
	
	protected final static String ERROR_TITLE= "error.delete.title";
	protected final static String ERROR_MESSAGE= "error.delete.message";

	
	protected JavaOutlinePage fPage;
	protected ResourceBundle fBundle;
	protected String fPrefix;
	
	public DeleteISourceManipulationsAction(ResourceBundle bndl, String prefix, JavaOutlinePage page) {
		super(bndl, prefix);
		fBundle= bndl;
		fPrefix= prefix;
		fPage= page;
	}
	
	/**
	 * @see ISelectionAction#selectionChanged
	 */	
	public void selectionChanged(SelectionChangedEvent e) {
		ISelection selection= e.getSelection();
		setEnabled((selection instanceof IStructuredSelection) && !selection.isEmpty());
	}
		
	/**
	 * @see Action#actionPerformed
	 */
	public void run() {
		try {
			ISourceManipulation[] args= getArgs(fPage.getSelection());
			if (args != null) {
				for (int i= 0; i < args.length; i++)
					args[i].delete(true, /*IProgressMonitor*/ null);
			}
		} catch (JavaModelException x) {
			ErrorDialog.openError(getShell(), fBundle.getString(fPrefix + ERROR_TITLE), fBundle.getString(fPrefix + ERROR_MESSAGE), x.getStatus());
		}
	}
	
	protected Shell getShell() {
		return fPage.getControl().getShell();
	}
	
	protected void getChildren(IParent container, Vector v) throws JavaModelException {
		IJavaElement[] children= container.getChildren();
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				if (children[i] instanceof ISourceManipulation)
					v.addElement(children[i]);
				else if (children[i] instanceof IParent)
					getChildren((IParent) children[i], v);
			}
		}
	}
	
	protected ISourceManipulation[] getArgs(ISelection selection) throws JavaModelException {
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			Vector v= new Vector();
			Iterator iter=  ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object o= iter.next();
				if (o instanceof ISourceManipulation)
					v.addElement(o);
				else if (o instanceof IParent)
					getChildren((IParent) o, v);
			}
			
			ISourceManipulation[] args= new ISourceManipulation[v.size()];
			v.copyInto(args);
			return args;
		}
		return null;
	}
}
