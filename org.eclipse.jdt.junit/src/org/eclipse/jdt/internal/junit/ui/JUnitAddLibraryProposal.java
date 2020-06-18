/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

public final class JUnitAddLibraryProposal implements IJavaCompletionProposal {

	private final IInvocationContext fContext;
	private final boolean fIsJunit4;
	private final int fRelevance;

	public JUnitAddLibraryProposal(boolean isJunit4, IInvocationContext context, int relevance) {
		fIsJunit4= isJunit4;
		fContext= context;
		fRelevance= relevance;
	}

	@Override
	public int getRelevance() {
		return fRelevance;
	}

	@Override
	public void apply(IDocument document) {
		IJavaProject project= fContext.getCompilationUnit().getJavaProject();
		Shell shell= JUnitPlugin.getActiveWorkbenchShell();
		try {
			IClasspathEntry entry= null;
			if (fIsJunit4) {
				entry= BuildPathSupport.getJUnit4ClasspathEntry();
			} else {
				entry= BuildPathSupport.getJUnit3ClasspathEntry();
			}
			if (entry != null) {
				addToClasspath(shell, project, entry, new BusyIndicatorRunnableContext());
			}
			// force a reconcile
			int offset= fContext.getSelectionOffset();
			int length= fContext.getSelectionLength();
			String s= document.get(offset, length);
			document.replace(offset, length, s);
		} catch (CoreException e) {
			ErrorDialog.openError(shell, JUnitMessages.JUnitAddLibraryProposal_title, JUnitMessages.JUnitAddLibraryProposal_cannotAdd, e.getStatus());
		} catch (BadLocationException e) {
			//ignore
		}
	}

	private static boolean addToClasspath(Shell shell, final IJavaProject project, IClasspathEntry entry, IRunnableContext context) throws JavaModelException {
		IClasspathEntry[] oldEntries= project.getRawClasspath();
		ArrayList<IClasspathEntry> newEntries= new ArrayList<>(oldEntries.length + 1);
		boolean added= false;
		for (IClasspathEntry curr : oldEntries) {
			if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				IPath path= curr.getPath();
				if (path.equals(entry.getPath())) {
					return true; // already on build path
				} else if (path.matchingFirstSegments(entry.getPath()) > 0) {
					if (!added) {
						curr= entry; // replace
						added= true;
					} else {
						curr= null;
					}
				}
			} else if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				IPath path= curr.getPath();
				if (path.segmentCount() > 0 && JUnitCorePlugin.JUNIT_HOME.equals(path.segment(0))) {
					if (!added) {
						curr= entry; // replace
						added= true;
					} else {
						curr= null;
					}
				}
			}
			if (curr != null) {
				newEntries.add(curr);
			}
		}
		if (!added) {
			newEntries.add(entry);
		}

		final IClasspathEntry[] newCPEntries= newEntries.toArray(new IClasspathEntry[newEntries.size()]);
		// fix for 64974 OCE in New JUnit Test Case wizard while workspace is locked [JUnit]
		try {
			context.run(true, false, monitor -> {
				try {
					project.setRawClasspath(newCPEntries, monitor);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			});
			return true;
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {
				ErrorDialog.openError(shell, JUnitMessages.JUnitAddLibraryProposal_title, JUnitMessages.JUnitAddLibraryProposal_cannotAdd, ((CoreException)t).getStatus());
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}

	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(fContext.getSelectionOffset(), fContext.getSelectionLength());
	}

	@Override
	public String getAdditionalProposalInfo() {
		if (fIsJunit4) {
			return JUnitMessages.JUnitAddLibraryProposal_junit4_info;
		}
		return JUnitMessages.JUnitAddLibraryProposal_info;
	}

	@Override
	public String getDisplayString() {
		if (fIsJunit4) {
			return JUnitMessages.JUnitAddLibraryProposa_junit4_label;
		}
		return JUnitMessages.JUnitAddLibraryProposal_label;
	}

	@Override
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
}
