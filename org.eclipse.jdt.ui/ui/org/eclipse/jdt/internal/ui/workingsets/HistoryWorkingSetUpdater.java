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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class HistoryWorkingSetUpdater implements IWorkingSetUpdater {

	public static final String ID= "org.eclipse.jdt.internal.ui.HistoryWorkingSet"; //$NON-NLS-1$
	
	private class Tracker extends EditorTracker {
		public void editorOpened(IEditorPart part) {
			IAdaptable file= getInput(part);
			if (file == null)
				return;
			fOpenFiles.add(file);
		}
		public void editorClosed(IEditorPart part) {
			IAdaptable file= getInput(part);
			if (file == null)
				return;
			fOpenFiles.remove(file);
			
		}
		private IAdaptable getInput(IEditorPart part) {
			IEditorInput input= part.getEditorInput();
			if (!(input instanceof IFileEditorInput))
				return null;
			
			if (part instanceof CompilationUnitEditor) {
				return JavaCore.create(((IFileEditorInput)input).getFile());
			}
			return ((IFileEditorInput)input).getFile();
		}
	}
	
	private IWorkingSet fWorkingSet;
	private Set fOpenFiles;
	private Tracker fTracker;
	private IElementChangedListener fJavaListener;
	private int fMaxElements= 15;
	
	public HistoryWorkingSetUpdater() {
		fOpenFiles= new HashSet();
		initListeners();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void add(IWorkingSet workingSet) {
		Assert.isTrue(fWorkingSet == null);
		fWorkingSet= workingSet;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean remove(IWorkingSet workingSet) {
		Assert.isTrue(fWorkingSet == workingSet);
		fWorkingSet= null;
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean contains(IWorkingSet workingSet) {
		return fWorkingSet == workingSet;
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
		if (fJavaListener != null) {
			JavaCore.removeElementChangedListener(fJavaListener);
			fJavaListener= null;
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
		fJavaListener= new IElementChangedListener() {
			public void elementChanged(ElementChangedEvent event) {
				if (fWorkingSet == null)
					return;
				processDelta(event.getDelta());
			}
			public void processDelta(IJavaElementDelta delta) {
				IJavaElement element= delta.getElement();
				int type= element.getElementType();
				int kind= delta.getKind();
				int flags= delta.getFlags();
				if (type == IJavaElement.COMPILATION_UNIT) {
					if (kind == IJavaElementDelta.CHANGED && (flags & IJavaElementDelta.F_PRIMARY_RESOURCE) != 0) {
						elementSaved(element);
					} else if (kind == IJavaElementDelta.REMOVED) {
						if ((delta.getFlags() & IJavaElementDelta.F_MOVED_TO) != 0) {
							elementMoved(element, delta.getMovedToElement());
						} else {
							elementRemoved(element);
						}
					}
				} else {
					IJavaElementDelta[] children= delta.getAffectedChildren();
					for (int i= 0; i < children.length; i++) {
						processDelta(children[i]);
					}
				}
				IResourceDelta[] resourceDeltas= delta.getResourceDeltas();
				if (resourceDeltas != null) {
					for (int i= 0; i < resourceDeltas.length; i++) {
						processDelta(resourceDeltas[i]);
					}
				}
			}
			private void processDelta(IResourceDelta outerDelta) {
				try {
					outerDelta.accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta) throws CoreException {
							IResource resource= delta.getResource();
							if (resource.getType() != IResource.FILE)
								return true;
							IFile file= (IFile)resource;
							if ((delta.getKind() & IResourceDelta.CHANGED) != 0) {
								if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
									elementSaved(file);
								}
							} else if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
								if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
									IFile newFile= ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
									if (!newFile.exists()) {
										elementRemoved(file);
									} else {
										elementMoved(file, newFile);
									}
								} else {
									elementRemoved(file);
								}
							}
							return false;
						}
					});
				} catch (CoreException e) {
				}
			}
		};
		JavaCore.addElementChangedListener(fJavaListener, ElementChangedEvent.POST_CHANGE);
	}
	
	private void elementSaved(IAdaptable element) {
		if (!fOpenFiles.contains(element))
			return;
		updateHistory(element);
	}
	
	private void elementRemoved(IAdaptable element) {
		fOpenFiles.remove(element);
		List elements= getElements();
		if (elements.remove(element)) {
			setElements(elements);
		}
	}
	
	private void elementMoved(IAdaptable oldElement, IAdaptable newElement) {
		List elements= getElements();
		int index= elements.indexOf(oldElement);
		if (index == -1)
			return;
		elements.set(index, newElement);
		fOpenFiles.remove(oldElement);
		fOpenFiles.add(newElement);
		setElements(elements);
	}
	
	private void updateHistory(IAdaptable element) {
		List elements= getElements();
		int index= elements.indexOf(element);
		if (index != -1) {
			elements.remove(index);
			elements.add(0, element);
		} else {
			if (elements.size() == fMaxElements) {
				elements.remove(elements.size() - 1);
			}
			elements.add(0, element);
		}
		setElements(elements);
	}

	private List getElements() {
		return new ArrayList(Arrays.asList(fWorkingSet.getElements()));
	}

	private void setElements(List elements) {
		fWorkingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
	}
}
