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
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.workingsets.dyn.DynamicWorkingSetImplementation;

public class HistoryWorkingSet extends DynamicWorkingSetImplementation {

	public static final String ID= "org.eclipse.jdt.internal.ui.historyWorkingSet"; //$NON-NLS-1$
	
	private static final String FACTORY_ID= ID;
	
	private static final Comparator HISTROY_COMPARATOR= new Comparator() {
		public int compare(Object o1, Object o2) {
			long h1= ((HistoryElement)o1).getHistoryStamp();
			long h2= ((HistoryElement)o2).getHistoryStamp();
			if (h1 > h2)
				return -1;
			if (h1 < h2)
				return 1;
			return 0;
		}
	};
	
	private class Tracker extends EditorTracker {
		public void editorOpened(IEditorPart part) {
			IFile file= getFile(part);
			if (file == null)
				return;
			fOpenFiles.add(file);
		}
		public void editorClosed(IEditorPart part) {
			IFile file= getFile(part);
			if (file == null)
				return;
			fOpenFiles.remove(file);
			
		}
		private IFile getFile(IEditorPart part) {
			IEditorInput input= part.getEditorInput();
			if (input instanceof IFileEditorInput)
				return ((IFileEditorInput)input).getFile();
			return null;
		}
	}
	
	private List fElements;
	private Set fOpenFiles;
	private Tracker fTracker;
	private IResourceChangeListener fResourceListener;
	private int fMaxElements= 15;
	
	/* package */ HistoryWorkingSet() {
		fElements= new ArrayList();
		fOpenFiles= new HashSet();
		initListeners();
	}
	
	/* package */ HistoryWorkingSet(IMemento memento) {
		restoreState(memento);
		fOpenFiles= new HashSet();
		initListeners();
	}

	public boolean loadEager() {
		return true;
	}
	
	public String getId() {
		return ID;
	}
	
	public IAdaptable[] getElements() {
		IAdaptable[] result= new IAdaptable[fElements.size()];
		for (int i= 0; i < result.length; i++) {
			result[i]= ((HistoryElement)fElements.get(i)).getModelElement();
		}
		return result;
	}

	public boolean isEditable() {
		return false;
	}

	public String getFactoryId() {
		return FACTORY_ID;
	}

	public void saveState(IMemento memento) {
		for (Iterator iter= fElements.iterator(); iter.hasNext();) {
			HistoryElement element= (HistoryElement)iter.next();
			Mementos.saveItem(memento.createChild(Mementos.TAG_ITEM), element);
		}
	}
	
	private void restoreState(IMemento memento) {
        IMemento[] itemMementos= memento.getChildren(Mementos.TAG_ITEM);
        fElements= new ArrayList();
		for (int i = 0; i < itemMementos.length; i++) {
			HistoryElement element= (HistoryElement)Mementos.restoreItem(itemMementos[i]);
			if (element != null && element.isValid())
				fElements.add(element);
		}
	}
	
	public void dispose() {
		if (fTracker != null) {
			IWorkbench workbench= PlatformUI.getWorkbench();
			workbench.removeWindowListener(fTracker);
			IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
			for (int i= 0; i < windows.length; i++) {
				windows[i].removePageListener(fTracker);
				IWorkbenchPage[] pages= windows[i].getPages();
				for (int j= 0; j < pages.length; j++) {
					pages[j].removePartListener(fTracker);
				}
			}
			fTracker= null;
		}
		if (fResourceListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceListener);
			fResourceListener= null;
		}
	}
	
	private void initListeners() {
		fTracker= new Tracker();
		IWorkbench workbench= PlatformUI.getWorkbench();
		workbench.addWindowListener(fTracker);
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			windows[i].addPageListener(fTracker);
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int j= 0; j < pages.length; j++) {
				pages[j].addPartListener(fTracker);
			}
		}
		fResourceListener= new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta) throws CoreException {
							IResource resource= delta.getResource();
							if (resource.getType() != IResource.FILE)
								return true;
							IFile file= (IFile)resource;
							if ((delta.getKind() & IResourceDelta.CHANGED) != 0) {
								if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
									fileSaved(file);
								}
							} else if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
								if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
									fileMoved(file, delta.getMovedToPath());
								} else {
									fileRemoved(file);
								}
							}
							return false;
						}
					});
				} catch (CoreException e) {
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceListener, IResourceChangeEvent.POST_CHANGE);
	}
	
	private void fileSaved(IFile file) {
		if (!fOpenFiles.contains(file))
			return;
		updateHistoryElement(file);
	}
	
	private void fileRemoved(IFile file) {
		HistoryElement element= new HistoryElement(file);
		fOpenFiles.remove(file);
		if (fElements.remove(element)) {
			fireContentChanged();
		}
	}
	
	private void fileMoved(IFile file, IPath destination) {
		int index= fElements.indexOf(new HistoryElement(file));
		if (index == -1)
			return;
		IFile newFile= ResourcesPlugin.getWorkspace().getRoot().getFile(destination);
		if (!newFile.exists()) {
			fileRemoved(file);
			return;
		}
		HistoryElement element= (HistoryElement)fElements.get(index);
		element.setFile(newFile);
		fOpenFiles.remove(file);
		fOpenFiles.add(newFile);
		fireContentChanged();
	}
	
	private void updateHistoryElement(IFile file) {
		HistoryElement element= new HistoryElement(file);
		int index= fElements.indexOf(element);
		if (index != -1) {
			((HistoryElement)fElements.get(index)).update();
			Collections.sort(fElements, HISTROY_COMPARATOR);
		} else {
			if (fElements.size() == fMaxElements) {
				fElements.remove(fElements.size() - 1);
			}
			fElements.add(0, element);
		}
		fireContentChanged();
	}	
}
