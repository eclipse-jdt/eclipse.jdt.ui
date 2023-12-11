/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.ISelectionListener;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Infrastructure to share an AST for editor post selection listeners.
 */
public class SelectionListenerWithASTManager {

	private static SelectionListenerWithASTManager fgDefault;

	/**
	 * @return Returns the default manager instance.
	 */
	public static SelectionListenerWithASTManager getDefault() {
		if (fgDefault == null) {
			fgDefault= new SelectionListenerWithASTManager();
		}
		return fgDefault;
	}


	private final static class PartListenerGroup {
		private ITextEditor fPart;
		private ISelectionListener fPostSelectionListener;
		private ISelectionChangedListener fSelectionListener;
		private Job fCurrentJob;
		private ListenerList<ISelectionListenerWithAST> fAstListeners;
		/**
		 * Lock to avoid having more than one calculateAndInform job in parallel.
		 * Only jobs may synchronize on this as otherwise deadlocks are possible.
		 */
		private final Object fJobLock= new Object();

		public PartListenerGroup(ITextEditor editorPart) {
			fPart= editorPart;
			fCurrentJob= null;
			fAstListeners= new ListenerList<>(ListenerList.IDENTITY);

			fSelectionListener= event -> {
				ISelection selection= event.getSelection();
				if (selection instanceof ITextSelection) {
					fireSelectionChanged((ITextSelection) selection);
				}
			};

			fPostSelectionListener= (part, selection) -> {
				if (part == fPart && selection instanceof ITextSelection)
					firePostSelectionChanged((ITextSelection) selection);
			};
		}

		public boolean isEmpty() {
			return fAstListeners.isEmpty();
		}

		public void install(ISelectionListenerWithAST listener) {
			if (isEmpty()) {
				fPart.getEditorSite().getPage().addPostSelectionListener(fPostSelectionListener);
				ISelectionProvider selectionProvider= fPart.getSelectionProvider();
				if (selectionProvider != null)
						selectionProvider.addSelectionChangedListener(fSelectionListener);
			}
			fAstListeners.add(listener);
		}

		public void uninstall(ISelectionListenerWithAST listener) {
			fAstListeners.remove(listener);
			if (isEmpty()) {
				fPart.getEditorSite().getPage().removePostSelectionListener(fPostSelectionListener);
				ISelectionProvider selectionProvider= fPart.getSelectionProvider();
				if (selectionProvider != null)
					selectionProvider.removeSelectionChangedListener(fSelectionListener);
			}
		}

		/**
		 * A selection event has occurred.
		 *
		 * @param selection the selection
		 */
		public void fireSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
		}

		/**
		 * A post selection event has occurred.
		 *
		 * @param selection the selection
		 */
		public void firePostSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
			ITypeRoot input= EditorUtility.getEditorInputJavaElement(fPart, false);
			if (input == null) {
				return;
			}
			final ITypeRoot typeRoot= input;


			fCurrentJob= new Job(JavaUIMessages.SelectionListenerWithASTManager_job_title) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					synchronized (fJobLock) {
						return JavaCore.callReadOnly(() -> calculateASTandInform(typeRoot, selection, monitor));
					}
				}
			};
			fCurrentJob.setPriority(Job.DECORATE);
			fCurrentJob.setSystem(true);
			fCurrentJob.schedule();
		}

		protected IStatus calculateASTandInform(ITypeRoot input, ITextSelection selection, IProgressMonitor monitor) {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// create AST
			try {
				CompilationUnit astRoot= SharedASTProviderCore.getAST(input, SharedASTProviderCore.WAIT_ACTIVE_ONLY, monitor);

				if (astRoot != null && !monitor.isCanceled()) {
					Iterator<ISelectionListenerWithAST> listeners;
					synchronized (PartListenerGroup.this) { // sync probably doesn't make sense here...
						listeners= fAstListeners.iterator();
					}
					while (listeners.hasNext()) {
						ISelectionListenerWithAST listener= listeners.next();
						listener.selectionChanged(fPart, selection, astRoot);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
					return Status.OK_STATUS;
				}
			} catch (OperationCanceledException e) {
				// thrown when canceling the AST creation
			}
			return Status.CANCEL_STATUS;
		}
	}


	private Map<ITextEditor, PartListenerGroup> fListenerGroups;

	private SelectionListenerWithASTManager() {
		fListenerGroups= new HashMap<>();
	}

	/**
	 * Registers a selection listener for the given editor part.
	 * @param part The editor part to listen to.
	 * @param listener The listener to register.
	 */
	public void addListener(ITextEditor part, ISelectionListenerWithAST listener) {
		synchronized (this) {
			PartListenerGroup partListener= fListenerGroups.get(part);
			if (partListener == null) {
				partListener= new PartListenerGroup(part);
				fListenerGroups.put(part, partListener);
			}
			partListener.install(listener);
		}
	}

	/**
	 * Unregisters a selection listener.
	 * @param part The editor part the listener was registered.
	 * @param listener The listener to unregister.
	 */
	public void removeListener(ITextEditor part, ISelectionListenerWithAST listener) {
		synchronized (this) {
			PartListenerGroup partListener= fListenerGroups.get(part);
			if (partListener != null) {
				partListener.uninstall(listener);
				if (partListener.isEmpty()) {
					fListenerGroups.remove(part);
				}
			}
		}
	}
}
