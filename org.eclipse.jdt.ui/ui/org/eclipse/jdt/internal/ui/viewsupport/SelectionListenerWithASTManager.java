/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

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
	
	
	private static class PartListenerGroup {
		private IEditorPart fPart;
		private Job fCurrentJob;
		private ListenerList fAstListeners;
		
		public PartListenerGroup(IEditorPart part) {
			fPart= part;
			fCurrentJob= null;
			fAstListeners= new ListenerList();
		}

		public boolean isEmpty() {
			return fAstListeners.isEmpty();
		}

		public void install(ISelectionListenerWithAST listener) {
			fAstListeners.add(listener);
		}
		
		public void uninstall(ISelectionListenerWithAST listener) {
			fAstListeners.remove(listener);
		}
		
		public void fireSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
		}
		
		public void firePostSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
			final IJavaElement input= getJavaElement();
			if (input == null) {
				return;
			}
			
			fCurrentJob= new Job(JavaUIMessages.getString("SelectionListenerWithASTManager.job.title")) { //$NON-NLS-1$
				public IStatus run(IProgressMonitor monitor) {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					synchronized (PartListenerGroup.this) {
						return calculateASTandInform(input, selection, monitor);
					}
				}
			};
			fCurrentJob.setPriority(Job.DECORATE);
			fCurrentJob.setSystem(true);
			fCurrentJob.schedule();
		}
		
		private IJavaElement getJavaElement() {
			IEditorInput editorInput= fPart.getEditorInput();
			if (editorInput != null)
				return (IJavaElement)editorInput.getAdapter(IJavaElement.class);
			else
				return null;
		}
		
		protected IStatus calculateASTandInform(IJavaElement input, ITextSelection selection, IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// create AST		
			CompilationUnit astRoot= JavaPlugin.getDefault().getASTProvider().getAST(input, true, true, monitor);
		
			if (astRoot != null && !monitor.isCanceled()) {
				Object[] listeners= fAstListeners.getListeners();
				for (int i= 0; i < listeners.length; i++) {
					((ISelectionListenerWithAST) listeners[i]).selectionChanged(fPart, selection, astRoot);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}
				return Status.OK_STATUS;			
			}
			return Status.CANCEL_STATUS;
		}
	}
	
	
	private static class WorkbenchWindowListener {
		private ISelectionService fSelectionService;
		private ISelectionListener fSelectionListener, fPostSelectionListener;
		private Map fPartListeners; // key: IEditorPart, value: PartListenerGroup
		
		public WorkbenchWindowListener(ISelectionService service) {
			fSelectionService= service;
			fPartListeners= new HashMap();
			fSelectionListener= new ISelectionListener() {
				public void selectionChanged(IWorkbenchPart part, ISelection selection) {
					doSelectionChanged(part, selection);
				}
			};
			
			fPostSelectionListener= new ISelectionListener() {
				public void selectionChanged(IWorkbenchPart part, ISelection selection) {
					doPostSelectionChanged(part, selection);
				}
			};
			
		}
		
		public boolean isEmpty() {
			return fPartListeners.isEmpty();
		}
		
		
		public void install(IEditorPart part, ISelectionListenerWithAST listener) {
			if (fPartListeners.isEmpty()) {
				fSelectionService.addPostSelectionListener(fPostSelectionListener);
				fSelectionService.addSelectionListener(fSelectionListener);
			}
			
			PartListenerGroup listenerGroup= (PartListenerGroup) fPartListeners.get(part);
			if (listenerGroup == null) {
				listenerGroup= new PartListenerGroup(part);
				fPartListeners.put(part, listenerGroup);
			}
			listenerGroup.install(listener);
		}
		
		public void uninstall(IEditorPart part, ISelectionListenerWithAST listener) {
			PartListenerGroup listenerGroup= (PartListenerGroup) fPartListeners.get(part);
			if (listenerGroup == null) {
				return;
			}
			listenerGroup.uninstall(listener);
			if (listenerGroup.isEmpty()) {
				fPartListeners.remove(part);
				if (fPartListeners.isEmpty()) {
					fSelectionService.removePostSelectionListener(fPostSelectionListener);
					fSelectionService.removePostSelectionListener(fSelectionListener);
				}
			}
		}		
		
		protected void doPostSelectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part instanceof IEditorPart && selection instanceof ITextSelection) { // only editor parts are interesting
				PartListenerGroup listenerGroup= (PartListenerGroup) fPartListeners.get(part);
				if (listenerGroup != null) {
					listenerGroup.firePostSelectionChanged((ITextSelection) selection);
				}
			}
		}
		
		protected void doSelectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part instanceof IEditorPart && selection instanceof ITextSelection) { // only editor parts are interesting
				PartListenerGroup listenerGroup= (PartListenerGroup) fPartListeners.get(part);
				if (listenerGroup != null) {
					listenerGroup.fireSelectionChanged((ITextSelection) selection);
				}
			}
		}	
	}
	
	private Map fListenerGroups;
	
	private SelectionListenerWithASTManager() {
		fListenerGroups= new HashMap();
	}
	
	/**
	 * Registers a selection listener for the given editor part. 
	 * @param part The editor part to listen to.
	 * @param listener The listener to register.
	 */
	public void addListener(IEditorPart part, ISelectionListenerWithAST listener) {
		ISelectionService service= part.getSite().getWorkbenchWindow().getSelectionService();
		WorkbenchWindowListener windowListener= (WorkbenchWindowListener) fListenerGroups.get(service);
		if (windowListener == null) {
			windowListener= new WorkbenchWindowListener(service);
			fListenerGroups.put(service, windowListener);
		}
		windowListener.install(part, listener);
	}

	/**
	 * Unregisters a selection listener.
	 * @param part The editor part the listener was registered.
	 * @param listener The listener to unregister.
	 */
	public void removeListener(IEditorPart part, ISelectionListenerWithAST listener) {
		ISelectionService service= part.getSite().getWorkbenchWindow().getSelectionService();
		WorkbenchWindowListener windowListener= (WorkbenchWindowListener) fListenerGroups.get(service);
		if (windowListener != null) {
			windowListener.uninstall(part, listener);
			if (windowListener.isEmpty()) {
				fListenerGroups.remove(service);
			}
		}
	}
	
	/**
	 * Forces a selection changed event that is sent to all listeners registered to the given editor
	 * part. The event is sent from a background thread: this method call can return before the listeners
	 * are informed.
	 */
	public void forceSelectionChange(IEditorPart part, ITextSelection selection) {
		ISelectionService service= part.getSite().getWorkbenchWindow().getSelectionService();
		WorkbenchWindowListener windowListener= (WorkbenchWindowListener) fListenerGroups.get(service);
		if (windowListener != null) {
			windowListener.doPostSelectionChanged(part, selection);
		}
	}}
