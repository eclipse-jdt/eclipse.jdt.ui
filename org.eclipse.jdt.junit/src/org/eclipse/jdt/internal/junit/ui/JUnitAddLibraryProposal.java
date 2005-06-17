/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jul 16, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.jdt.internal.junit.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.junit.wizards.WizardMessages;

/**
 * @author egamma
 */
public final class JUnitAddLibraryProposal implements IJavaCompletionProposal {
	private final IInvocationContext fContext;

	public JUnitAddLibraryProposal(IInvocationContext context) {
		fContext= context;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return 0;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {   
		IJavaProject project= fContext.getCompilationUnit().getJavaProject();
		try {
			addJUnitToBuildPath(JUnitPlugin.getActiveWorkbenchShell(), project);
			// force a reconcile
			int offset= fContext.getSelectionOffset();
			int length= fContext.getSelectionLength();
			String s= document.get(offset, length);
			document.replace(offset, length, s);
		} catch (JavaModelException e) {
			ErrorDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), JUnitMessages.JUnitAddLibraryProposal_title, JUnitMessages.JUnitAddLibraryProposal_cannotAdd, e.getStatus());  

		} catch (BadLocationException e) {
			//ignore
		}
	}
	
	
	public static void addJUnitToBuildPath(Shell shell, IJavaProject project) throws JavaModelException {
		IProject junitProject= ResourcesPlugin.getWorkspace().getRoot().getProject("org.junit"); //$NON-NLS-1$
		IClasspathEntry entry;
		if (junitProject.exists()) {
			entry= JavaCore.newProjectEntry(junitProject.getFullPath());
		} else {
			IPath junitHome= new Path(JUnitPlugin.JUNIT_HOME);
			IPath sourceHome= new Path("ECLIPSE_HOME"); //$NON-NLS-1$
			entry= JavaCore.newVariableEntry(
				junitHome.append("junit.jar"),   //$NON-NLS-1$
				//TODO: find a better solution than declaring a classpath variable
				sourceHome.append("plugins/org.eclipse.jdt.source_3.1.0/src/org.junit_3.8.1/junitsrc.zip"),  //$NON-NLS-1$
				null
			);
		}
		addToClasspath(shell, project, entry);
	}	
	
	private static void addToClasspath(Shell shell, final IJavaProject project, IClasspathEntry entry) throws JavaModelException {
		IClasspathEntry[] oldEntries= project.getRawClasspath();
		for (int i= 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(entry)) {
				return;
			}
		}
		int nEntries= oldEntries.length;
		final IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries]= entry;
		// fix for 64974 OCE in New JUnit Test Case wizard while workspace is locked [JUnit] 
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.setRawClasspath(newEntries, monitor);
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {	
				ErrorDialog.openError(shell, WizardMessages.NewTestClassWizPage_cannot_add_title, WizardMessages.NewTestClassWizPage_cannot_add_message, ((CoreException)t).getStatus());  
			}
		} catch (InterruptedException e) {
			return;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fContext.getSelectionOffset(), fContext.getSelectionLength());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return JUnitMessages.JUnitAddLibraryProposal_info; 
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return JUnitMessages.JUnitAddLibraryProposal_label; 
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}
}
