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
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Properties file editor.
 * 
 * @since 3.1
 */
public class PropertiesFileEditor extends TextEditor {

	/** Open action. */
	protected OpenAction fOpenAction;


	/**
	 * Creates a new properties file editor.
	 */
	public PropertiesFileEditor() {
		setDocumentProvider(JavaPlugin.getDefault().getPropertiesFileDocumentProvider());
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		setPreferenceStore(store);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		setSourceViewerConfiguration(new PropertiesFileSourceViewerConfiguration(textTools.getColorManager(), store, this, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING));
	}

	
	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#createActions()
	 */
	protected void createActions() {
		super.createActions();
		
		fOpenAction= new OpenAction(this);
		fOpenAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
		setAction(JdtActionConstants.OPEN, fOpenAction);
	}
	
	/*
	 * @see TextEditor#createAnnotationAccess()
	 */
	protected IAnnotationAccess createAnnotationAccess() {
		return new DefaultMarkerAnnotationAccess();
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {			

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			((PropertiesFileSourceViewerConfiguration) getSourceViewerConfiguration()).handlePropertyChangeEvent(event);
			
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return ((PropertiesFileSourceViewerConfiguration)getSourceViewerConfiguration()).affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}
	
	
	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV };
				}

			};
		}
		return super.getAdapter(adapter);
	}

	/*
	 * @see org.eclipse.ui.texteditor.StatusTextEditor#updateStatusField(java.lang.String)
	 */
	protected void updateStatusField(String category) {
		super.updateStatusField(category);
		if (getEditorSite() != null) {
			getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
			getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(null);
		}
	}
	
	/* 
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#getSourceViewer()
	 */
	ISourceViewer internalGetSourceViewer() {
		return getSourceViewer();
	}
}
