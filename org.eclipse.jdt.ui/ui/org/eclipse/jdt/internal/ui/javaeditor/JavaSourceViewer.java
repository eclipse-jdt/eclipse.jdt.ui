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

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.text.SmartBackspaceManager;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;



public class JavaSourceViewer extends ProjectionViewer implements IPropertyChangeListener {

	/**
	 * Text operation code for requesting the outline for the current input.
	 */
	public static final int SHOW_OUTLINE= 51;

	/**
	 * Text operation code for requesting the outline for the element at the current position.
	 */
	public static final int OPEN_STRUCTURE= 52;

	/**
	 * Text operation code for requesting the hierarchy for the current input.
	 */
	public static final int SHOW_HIERARCHY= 53;

	private IInformationPresenter fOutlinePresenter;
	private IInformationPresenter fStructurePresenter;
	private IInformationPresenter fHierarchyPresenter;

	/**
	 * This viewer's foreground color.
	 * @since 3.0
	 */
	private Color fForegroundColor;
	/** 
	 * The viewer's background color.
	 * @since 3.0
	 */
	private Color fBackgroundColor;
	/**
	 * This viewer's selection foreground color.
	 * @since 3.0
	 */
	private Color fSelectionForegroundColor;
	/** 
	 * The viewer's selection background color.
	 * @since 3.0
	 */
	private Color fSelectionBackgroundColor;
	/**
	 * The preference store.
	 * 
	 * @since 3.0
	 */
	private IPreferenceStore fPreferenceStore;
	/**
	 * Is this source viewer configured?
	 * 
	 * @since 3.0
	 */
	private boolean fIsConfigured;
	/**
	 * The backspace manager of this viewer.
	 * 
	 * @since 3.0
	 */
	private SmartBackspaceManager fBackspaceManager;
	
	public JavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles, IPreferenceStore store) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
		setPreferenceStore(store);
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#createFormattingContext()
	 * @since 3.0
	 */
	public IFormattingContext createFormattingContext() {

		IFormattingContext context= new CommentFormattingContext();
		Map map= new Hashtable(JavaCore.getOptions());
		
		context.storeToMap(PreferenceConstants.getPreferenceStore(), map, false);
		context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, map);
		
		return context;
	}

	/*
	 * @see ITextOperationTarget#doOperation(int)
	 */
	public void doOperation(int operation) {
		if (getTextWidget() == null)
			return;

		switch (operation) {
			case SHOW_OUTLINE:
				fOutlinePresenter.showInformation();
				return;
			case OPEN_STRUCTURE:
				fStructurePresenter.showInformation();
				return;
			case SHOW_HIERARCHY:
				fHierarchyPresenter.showInformation();
				return;	
		}
		
		super.doOperation(operation);
	}

	/*
	 * @see ITextOperationTarget#canDoOperation(int)
	 */
	public boolean canDoOperation(int operation) {
		if (operation == SHOW_OUTLINE)
			return fOutlinePresenter != null;
		if (operation == OPEN_STRUCTURE)
			return fStructurePresenter != null;
		if (operation == SHOW_HIERARCHY)
			return fHierarchyPresenter != null;			
			
		return super.canDoOperation(operation);
	}

	/*
	 * @see ISourceViewer#configure(SourceViewerConfiguration)
	 */
	public void configure(SourceViewerConfiguration configuration) {
		super.configure(configuration);
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fOutlinePresenter= ((JavaSourceViewerConfiguration)configuration).getOutlinePresenter(this, false);
			fOutlinePresenter.install(this);
		}
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fStructurePresenter= ((JavaSourceViewerConfiguration)configuration).getOutlinePresenter(this, true);
			fStructurePresenter.install(this);
		}
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fHierarchyPresenter= ((JavaSourceViewerConfiguration)configuration).getHierarchyPresenter(this, true);
			fHierarchyPresenter.install(this);
            
			if (fPreferenceStore != null) {
				fPreferenceStore.addPropertyChangeListener(this);
				initializeViewerColors();
			}
		}
		fIsConfigured= true;
	}
	
    
	protected void initializeViewerColors() {
		if (fPreferenceStore != null) {
			
			StyledText styledText= getTextWidget();
			
			// ----------- foreground color --------------------
			Color color= fPreferenceStore.getBoolean(PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR)
			? null
			: createColor(fPreferenceStore, PreferenceConstants.EDITOR_FOREGROUND_COLOR, styledText.getDisplay());
			styledText.setForeground(color);
			
			if (fForegroundColor != null)
				fForegroundColor.dispose();
			
			fForegroundColor= color;
			
			// ---------- background color ----------------------
			color= fPreferenceStore.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR)
			? null
			: createColor(fPreferenceStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR, styledText.getDisplay());
			styledText.setBackground(color);
			
			if (fBackgroundColor != null)
				fBackgroundColor.dispose();
			
			fBackgroundColor= color;
			
			// ----------- selection foreground color --------------------
			color= fPreferenceStore.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR)
				? null
				: createColor(fPreferenceStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, styledText.getDisplay());
			styledText.setSelectionForeground(color);
				
			if (fSelectionForegroundColor != null)
				fSelectionForegroundColor.dispose();
			
			fSelectionForegroundColor= color;
			
			// ---------- selection background color ----------------------
			color= fPreferenceStore.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR)
				? null
				: createColor(fPreferenceStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, styledText.getDisplay());
			styledText.setSelectionBackground(color);
				
			if (fSelectionBackgroundColor != null)
				fSelectionBackgroundColor.dispose();
				
			fSelectionBackgroundColor= color;
		}
    }

    /**
     * Creates a color from the information stored in the given preference store.
     * Returns <code>null</code> if there is no such information available.
     * 
     * @param store the store to read from
     * @param key the key used for the lookup in the preference store
     * @param display the display used create the color
     * @return the created color according to the specification in the preference store
     * @since 3.0
     */
    private Color createColor(IPreferenceStore store, String key, Display display) {
    
        RGB rgb= null;      
        
        if (store.contains(key)) {
            
            if (store.isDefault(key))
                rgb= PreferenceConverter.getDefaultColor(store, key);
            else
                rgb= PreferenceConverter.getColor(store, key);
        
            if (rgb != null)
                return new Color(display, rgb);
        }
        
        return null;
    }

	/*
	 * @see org.eclipse.jface.text.source.ISourceViewerExtension2#unconfigure()
	 * @since 3.0
	 */
	public void unconfigure() {
		if (fOutlinePresenter != null) {
			fOutlinePresenter.uninstall();	
			fOutlinePresenter= null;
		}
		if (fStructurePresenter != null) {
			fStructurePresenter.uninstall();
			fStructurePresenter= null;
		}
		if (fHierarchyPresenter != null) {
			fHierarchyPresenter.uninstall();
			fHierarchyPresenter= null;
		}
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
			fForegroundColor= null;
		}
		if (fBackgroundColor != null) {
			fBackgroundColor.dispose();
			fBackgroundColor= null;
		}
		if (fPreferenceStore != null)
			fPreferenceStore.removePropertyChangeListener(this);
		
		super.unconfigure();
		
		fIsConfigured= false;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#rememberSelection()
	 */
	public Point rememberSelection() {
		return super.rememberSelection();
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#restoreSelection()
	 */
	public void restoreSelection() {
		super.restoreSelection();
	}

	/*
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (PreferenceConstants.EDITOR_FOREGROUND_COLOR.equals(property)
				|| PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR.equals(property)
				|| PreferenceConstants.EDITOR_BACKGROUND_COLOR.equals(property)
				|| PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR.equals(property))
		{
			initializeViewerColors();
		}		
	}

	/**
	 * Sets the preference store on this viewer.
	 * 
	 * @param store the preference store
	 * 
	 * @since 3.0
	 */
	public void setPreferenceStore(IPreferenceStore store) {
		if (fIsConfigured && fPreferenceStore != null)
			fPreferenceStore.removePropertyChangeListener(this);
		
		fPreferenceStore= store;

		if (fIsConfigured && fPreferenceStore != null) {
			fPreferenceStore.addPropertyChangeListener(this);
			initializeViewerColors();
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#createControl(org.eclipse.swt.widgets.Composite, int)
	 */
	protected void createControl(Composite parent, int styles) {
		super.createControl(parent, styles);

		fBackspaceManager= new SmartBackspaceManager();
	}
	
	/**
	 * Returns the backspace manager for this viewer.
	 * 
	 * @return the backspace manager for this viewer, or <code>null</code> if
	 *         there is none
	 * @since 3.0
	 */
	public SmartBackspaceManager getBackspaceManager() {
		return fBackspaceManager;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#handleDispose()
	 */
	protected void handleDispose() {
		if (fBackspaceManager != null) {
			fBackspaceManager.uninstall();
			fBackspaceManager= null;
		}

		super.handleDispose();
	}
}
