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

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.OpenStrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import org.eclipse.search.internal.core.text.ITextSearchResultCollector;
import org.eclipse.search.internal.core.text.MatchLocator;
import org.eclipse.search.internal.core.text.TextSearchEngine;
import org.eclipse.search.internal.core.text.TextSearchScope;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This action opens a tool (internal editor or view or an external
 * application) for the element at the given location.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p> 
 * <p>
 * FIXME: Work in progress
 * 			- does not yet reveal the key in the opened editor
 * 			- only IFile's are currently supported
 * </p>
 * 
 * @since 3.1
 */
public class OpenAction extends SelectionDispatchAction {
	
	
	private static class KeyReference {
		private IFile file;
		private int offset;
		private int length;

		private KeyReference(IFile file, int offset, int length) {
			this.file= file;
			this.offset= offset;
			this.length= length;
		}
	}
	
	private static class ResultCollector implements ITextSearchResultCollector {
		
		private List fResult;
		private IProgressMonitor fProgressMonitor;
		
		public ResultCollector(List result) {
			fResult= result;
			fProgressMonitor= new NullProgressMonitor();
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
	

	private PropertiesFileEditor fEditor;

	
	/**
	 * Creates a new <code>OpenAction</code>.
	 * 
	 * @param editor the Properties file editor which provides the context information for this action
	 */
	public OpenAction(PropertiesFileEditor editor) {
		super(editor.getEditorSite());
		fEditor= editor;
		setText(PropertiesFileEditorMessages.getString("OpenAction.label")); //$NON-NLS-1$
		setToolTipText(PropertiesFileEditorMessages.getString("OpenAction.tooltip")); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(ITextSelection selection) {
		if (selection == null || selection.isEmpty())
			return false;
		
		return fEditor.getEditorInput() instanceof IFileEditorInput;
	}
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		if (!checkEnabled(selection))
			return;

		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		
		int offset= selection.getOffset();
		ITypedRegion partition= null;
		try {
			IStorageEditorInput storageEditorInput= (IStorageEditorInput)fEditor.getEditorInput();
			IDocument document= fEditor.getDocumentProvider().getDocument(storageEditorInput);
			if (document instanceof IDocumentExtension3)
				partition= ((IDocumentExtension3)document).getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, offset, false);
			
			// Check whether it is the correct partition
			if (partition == null || !IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())) {
				showNoKeyErrorInStatusLine();
				return;
			}
			
			// Check whether the partition covers the selection
			if (offset + selection.getLength() > partition.getOffset() + partition.getLength()) {
				showNoKeyErrorInStatusLine();
				return;
			}
			
			// Extract the key from the partition (which contains key and assignment
			String key= document.get(partition.getOffset(), partition.getLength()).trim();
			
			// Check whether the key is valid
			Properties properties= new Properties();
			properties.load(new StringBufferInputStream(document.get()));
			if (properties.getProperty(key) == null) {
				showNoKeyErrorInStatusLine();
				return;
			}
			
			// Search the key
			IResource resource= (IResource)storageEditorInput.getAdapter(IResource.class);
			KeyReference[] references;
			if (resource != null)
				references= search(resource.getProject(), key);
			else
				references= search(ResourcesPlugin.getWorkspace().getRoot(), key);
			
			if (references == null || references.length == 0) {
				String message= PropertiesFileEditorMessages.getString("OpenAction.error.messageNoResult"); //$NON-NLS-1$
				showErrorInStatusLine(message);
				return;
			}
			
			open(references);
			
		} catch (BadLocationException ex) {
			String message= PropertiesFileEditorMessages.getString("OpenAction.error.messageErrorResolvingSelection"); //$NON-NLS-1$
			showError(new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, ex)));
		} catch (BadPartitioningException ex) {
			String message= PropertiesFileEditorMessages.getString("OpenAction.error.messageErrorResolvingSelection"); //$NON-NLS-1$
			showError(new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, ex)));
		} catch (IOException ex) {
			String message= PropertiesFileEditorMessages.getString("OpenAction.error.messageErrorResolvingSelection"); //$NON-NLS-1$
			showError(new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, ex)));
		}
	}
	
	private void showNoKeyErrorInStatusLine() {
		showErrorInStatusLine(PropertiesFileEditorMessages.getString("OpenAction.error.messageBadSelection")); //$NON-NLS-1$
	}

	private void open(KeyReference[] elements) {
		if (elements == null)
			return;
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i].file;
			try {
				element= getElementToOpen(element);
				boolean activateOnOpen= fEditor != null ? true : OpenStrategy.activateOnOpen();
				OpenActionUtil.open(element, activateOnOpen);
			} catch (JavaModelException e) {
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
					IJavaStatusConstants.INTERNAL_ERROR, PropertiesFileEditorMessages.getString("OpenAction.error.message"), e)); //$NON-NLS-1$
				
				ErrorDialog.openError(getShell(), 
					getDialogTitle(),
					PropertiesFileEditorMessages.getString("OpenAction.error.messageProblems"),  //$NON-NLS-1$
					e.getStatus());
			
			} catch (PartInitException x) {
								
				String name= null;
				
				if (element instanceof IJavaElement) {
					name= ((IJavaElement) element).getElementName();
				} else if (element instanceof IStorage) {
					name= ((IStorage) element).getName();
				} else if (element instanceof IResource) {
				}
				
				if (name != null) {
					MessageDialog.openError(getShell(),
						PropertiesFileEditorMessages.getString("OpenAction.error.messageProblems"),  //$NON-NLS-1$
						PropertiesFileEditorMessages.getFormattedString("OpenAction.error.messageArgs",  //$NON-NLS-1$
						new String[] { name, x.getMessage() } ));			
				}
			}		
		}
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 */
	public Object getElementToOpen(Object object) throws JavaModelException {
		return object;
	}	
	
	private String getDialogTitle() {
		return PropertiesFileEditorMessages.getString("OpenAction.error.title"); //$NON-NLS-1$
	}
	
	private void showError(CoreException e) {
		ExceptionHandler.handle(e, getShell(), getDialogTitle(), PropertiesFileEditorMessages.getString("OpenAction.error.message")); //$NON-NLS-1$
	}
	
	private void showErrorInStatusLine(String message) {
		IEditorStatusLine statusLine= (IEditorStatusLine)fEditor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null)
			statusLine.setMessage(true, message, null);
		getShell().getDisplay().beep();
	}
	
	private static KeyReference[] search(IResource scope, String key) {
		if (key == null)
			return new KeyReference[0];
		
		List result= new ArrayList(5);
		ResultCollector collector= new ResultCollector(result);
		TextSearchEngine engine= new TextSearchEngine();
		engine.search(ResourcesPlugin.getWorkspace(), 
				createScope(scope), false,
				collector, new MatchLocator(key, true, false));
		
		return (KeyReference[])result.toArray(new KeyReference[result.size()]);
	}

	private static TextSearchScope createScope(IResource resource) {
		TextSearchScope result= new TextSearchScope(""); //$NON-NLS-1$
		result.add(resource);
		result.addExtension("*.java"); //$NON-NLS-1$
		result.addExtension("*.xml"); //$NON-NLS-1$
		return result;
	}
}
