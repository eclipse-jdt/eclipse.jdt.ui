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

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant;
import org.eclipse.text.reconcilerpipe.IReconcileResult;
import org.eclipse.text.reconcilerpipe.TextModelAdapter;

/**
 * Reconciling strategy for Java parts in JSP files.
 *
 * @since 3.0
 */
public class JspReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	/**
	 * Simple data structure to hold the reconcile result.
	 */
	static class Result implements IReconcileResult {
		
		private int startOffset;
		private int endOffset;
		private String word;

		Result(int startOffset, int endOffset, String word) {
			this.startOffset= startOffset;
			this.endOffset= endOffset;
			this.word= word;
		}
	}


	private IReconcilePipeParticipant fFirstParticipant;
	private HashMap fOffsetToMarkerMap;
	private ITextEditor fTextEditor;
	
	
	public JspReconcilingStrategy(ISourceViewer sourceViewer, ITextEditor textEditor) {
		fTextEditor= textEditor;
		IReconcilePipeParticipant javaParticipant= new JavaReconcilePipeParticipant(getFile());
		fFirstParticipant= new Jsp2JavaReconcilePipeParticipant(javaParticipant);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		fFirstParticipant.setInputModel(new TextModelAdapter(document));
	}
	
	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		initializeProblemMarkers();
		process(fFirstParticipant.reconcile(dirtyRegion, subRegion));
		removeRemainingMarkers();
	}
	
	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		initializeProblemMarkers();
		process(fFirstParticipant.reconcile(partition));
		removeRemainingMarkers();
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		fFirstParticipant.setProgressMonitor(monitor);
		
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile() {
		fFirstParticipant.reconcile(null);
		
	}

	private void process(IReconcileResult[] results) {
		
		if (results == null)
			return;
		
		for (int i= 0; i < results.length; i++) {				
			if (!(results[i] instanceof Result))
				continue;
		
			Result result= (Result)results[i];
		
			// Check if marker already exists.
			Integer offset= new Integer(result.startOffset);
			IMarker marker= (IMarker)fOffsetToMarkerMap.get(offset);
			if (marker != null && marker.getAttribute(IMarker.MESSAGE, "").equals(result.word)) //$NON-NLS-1$
				fOffsetToMarkerMap.remove(offset);
			else {
				Map attributes= new HashMap(4);
				attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
				attributes.put(IMarker.CHAR_START, offset);
				attributes.put(IMarker.CHAR_END, new Integer(result.endOffset));
				attributes.put(IMarker.MESSAGE, result.word);
				try {
					MarkerUtilities.createMarker(getFile(), attributes, "org.eclipse.jdt.core.problem"); //$NON-NLS-1$
				} catch (CoreException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}
	
	private void initializeProblemMarkers() {
		IMarker[] markers;
		try {
			markers= getFile().findMarkers("org.eclipse.jdt.core.problem", true, IResource.DEPTH_INFINITE); //$NON-NLS-1$
		} catch (CoreException e) {
			fOffsetToMarkerMap= new HashMap();
			return;
		}
	
		fOffsetToMarkerMap= new HashMap(markers.length);
		for (int i= 0; i < markers.length; i++) {
			int offset= markers[i].getAttribute(IMarker.CHAR_START, -1);
			if (offset != -1 && markers[i].exists())
				fOffsetToMarkerMap.put(new Integer(offset), markers[i]);
		}
	}

	private void removeRemainingMarkers() {
		IMarker[] markers= (IMarker[])fOffsetToMarkerMap.values().toArray(new IMarker[fOffsetToMarkerMap.values().size()]);
		try {
			ResourcesPlugin.getWorkspace().deleteMarkers(markers);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private IFile getFile() {
		IEditorInput input= fTextEditor.getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;
		
		return ((IFileEditorInput)input).getFile();			
	}
}
