/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.search.internal.core.text.ITextSearchResultCollector;
import org.eclipse.search.internal.core.text.MatchLocator;
import org.eclipse.search.internal.core.text.TextSearchEngine;
import org.eclipse.search.internal.core.text.TextSearchScope;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


/**
 * Properties key hyperlink.
 * <p>
 * XXX:	This does not work for properties files coming from a JAR due to
 * 		missing J Core functionality. For details see:
 * 		https://bugs.eclipse.org/bugs/show_bug.cgi?id=22376
 * </p>
 * 
 * @since 3.1
 */
public class PropertyKeyHyperlink implements IHyperlink {

	
	private static class KeyReference extends PlatformObject implements IWorkbenchAdapter {
		private IStorage storage;
		private int offset;
		private int length;
	
		private KeyReference(IStorage storage, int offset, int length) {
			Assert.isNotNull(storage);
			this.storage= storage;
			this.offset= offset;
			this.length= length;
		}
		
		/*
		 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
		 */
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return this;
			else
				return super.getAdapter(adapter);
		}
		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object o) {
			return null;
		}
		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
		 */
		public ImageDescriptor getImageDescriptor(Object object) {
			IWorkbenchAdapter wbAdapter= (IWorkbenchAdapter)storage.getAdapter(IWorkbenchAdapter.class);
			if (wbAdapter != null)
				return wbAdapter.getImageDescriptor(storage);
			return null;
		}
		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
		 */
		public String getLabel(Object o) {
			Object[] args= new Object[] { storage.getFullPath(), new Integer(offset), new Integer(length) }; 
			return PropertiesFileEditorMessages.getFormattedString("OpenAction.SelectionDialog.elementLabel", args); //$NON-NLS-1$
		}
		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
		 */
		public Object getParent(Object o) {
			return null;
		}
	}

	
	private static class ResultCollector implements ITextSearchResultCollector {
		
		private List fResult;
		private IProgressMonitor fProgressMonitor;
		
		public ResultCollector(List result, IProgressMonitor progressMonitor) {
			fResult= result;
			fProgressMonitor= progressMonitor;
		}
		
		public void aboutToStart() throws CoreException {
			// do nothing;
		}

		public void accept(IResourceProxy proxy, int start, int length) throws CoreException {
			// Can cast to IFile because search only reports matches on IFile
			fResult.add(new KeyReference((IFile)proxy.requestResource(), start, length));
		}

		public void done() throws CoreException {
			// do nothing;
		}

		public IProgressMonitor getProgressMonitor() {
			return fProgressMonitor;
		}
	}

	
	private IRegion fRegion;
	private String fPropertiesKey;
	private Shell fShell;
	private IStorage fStorage;
	private ITextEditor fEditor;

	
	/**
	 * Creates a new properties key hyperlink.
	 * 
	 * @param region the region
	 * @param key the properties key
	 * @param editor the text editor
	 */
	public PropertyKeyHyperlink(IRegion region, String key, ITextEditor editor) {
		Assert.isNotNull(region);
		Assert.isNotNull(key);
		Assert.isNotNull(editor);
		
		fRegion= region;
		fPropertiesKey= key;
		fEditor= editor;
		IStorageEditorInput storageEditorInput= (IStorageEditorInput)fEditor.getEditorInput();
		fShell= fEditor.getEditorSite().getShell();
		try {
			fStorage= storageEditorInput.getStorage();
		} catch (CoreException e) {
			fStorage= null;
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#open()
	 */
	public void open() {
		if (!checkEnabled())
			return;
	
		// Search the key
		IResource resource= (IResource)fStorage;
		KeyReference[] references= null;
		if (resource != null)
			references= search(resource.getProject(), fPropertiesKey);
		
		if (references == null)
			return; // canceled by the user
		
		if (references.length == 0) {
			String message= PropertiesFileEditorMessages.getString("OpenAction.error.messageNoResult"); //$NON-NLS-1$
			showErrorInStatusLine(message);
			return;
		}
		
		open(references);
		
	}
	
	private boolean checkEnabled() {
		 // XXX: Can be removed once support for JARs is available (see class Javadoc for details)
		return fStorage instanceof IResource;
	}
	
	private void open(KeyReference[] keyReferences) {
		Assert.isLegal(keyReferences != null && keyReferences.length > 0);
		
		if (keyReferences.length == 1)
			open(keyReferences[0]);
		else
			open(select(keyReferences));
	}
	
	private KeyReference select(KeyReference[] keyReferences) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(fShell, new WorkbenchLabelProvider());
		dialog.setMultipleSelection(false);
		dialog.setTitle(PropertiesFileEditorMessages.getString("OpenAction.SelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(PropertiesFileEditorMessages.getString("OpenAction.SelectionDialog.message")); //$NON-NLS-1$
		dialog.setElements(keyReferences);
		
		if (dialog.open() == Window.OK) {
			Object[] result= dialog.getResult();
			if (result != null && result.length == 1)
			 return (KeyReference)result[0];
		}

		return null;
	}
		
	private void open(KeyReference keyReference) {
		if (keyReference == null)
			return;
		
		try {	
			IEditorPart part= EditorUtility.openInEditor(keyReference.storage, true);
			EditorUtility.revealInEditor(part, keyReference.offset, keyReference.length);
		} catch (JavaModelException e) {
			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
				IJavaStatusConstants.INTERNAL_ERROR, PropertiesFileEditorMessages.getString("OpenAction.error.message"), e)); //$NON-NLS-1$
			
			ErrorDialog.openError(fShell, 
				getErrorDialogTitle(),
				PropertiesFileEditorMessages.getString("OpenAction.error.messageProblems"), //$NON-NLS-1$
				e.getStatus());
		
		} catch (PartInitException x) {
							
			String message= null;
			
			IWorkbenchAdapter wbAdapter= (IWorkbenchAdapter)((IAdaptable)keyReference).getAdapter(IWorkbenchAdapter.class);
			if (wbAdapter != null)
				message= PropertiesFileEditorMessages.getFormattedString("OpenAction.error.messageArgs", //$NON-NLS-1$
						new String[] { wbAdapter.getLabel(keyReference), x.getLocalizedMessage() } );

			if (message == null)
				message= PropertiesFileEditorMessages.getFormattedString("OpenAction.error.message", x.getLocalizedMessage()); //$NON-NLS-1$
			
			MessageDialog.openError(fShell,
				PropertiesFileEditorMessages.getString("OpenAction.error.messageProblems"), //$NON-NLS-1$
				message);			
		}		
	}
	
	private String getErrorDialogTitle() {
		return PropertiesFileEditorMessages.getString("OpenAction.error.title"); //$NON-NLS-1$
	}
	
	private void showError(CoreException e) {
		ExceptionHandler.handle(e, fShell, getErrorDialogTitle(), PropertiesFileEditorMessages.getString("OpenAction.error.message")); //$NON-NLS-1$
	}
	
	private void showErrorInStatusLine(final String message) {
		fShell.getDisplay().beep();
		final IEditorStatusLine statusLine= (IEditorStatusLine)fEditor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null) {
			fShell.getDisplay().asyncExec(new Runnable() {
				/*
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					statusLine.setMessage(true, message, null);
				}
			});
		}
	}
	
	/**
	 * Searches references to the given key in the given scope.
	 * 
	 * @param scope the scope
	 * @param key the properties key
	 * @return the references or <code>null</code> if the search has been canceled by the user
	 */
	private KeyReference[] search(final IResource scope, final String key) {
		if (key == null)
			return new KeyReference[0];

		final List result= new ArrayList(5);

		try {
			fEditor.getEditorSite().getWorkbenchWindow().getWorkbench().getProgressService().busyCursorWhile(
				new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						ResultCollector collector= new ResultCollector(result, monitor);
						TextSearchEngine engine= new TextSearchEngine();
						engine.search(ResourcesPlugin.getWorkspace(), 
								createScope(scope), false,
								collector, new MatchLocator(key, true, false));
					}
				}
			);
		} catch (InvocationTargetException ex) {
			String message= PropertiesFileEditorMessages.getString("OpenAction.error.messageErrorSearchingKey"); //$NON-NLS-1$
			showError(new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, ex.getTargetException())));
		} catch (InterruptedException ex) {
			return null; // canceled
		}
		
		return (KeyReference[])result.toArray(new KeyReference[result.size()]);
	}

	private static TextSearchScope createScope(IResource scope) {
		TextSearchScope result= new TextSearchScope(""); //$NON-NLS-1$
		result.add(scope);
		
		// XXX: This should probably be configurable via preference
		result.addExtension("*.java"); //$NON-NLS-1$
		result.addExtension("*.xml"); //$NON-NLS-1$
		
		return result;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText() {
		return null;
	}
}
