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

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IColorManagerExtension;
import org.eclipse.jdt.internal.ui.text.JavaPresentationReconciler;

/**
 * Semantic highlighting manager
 * 
 * @since 3.0
 */
public class SemanticHighlightingManager {
	
	/**
	 * Highlighting.
	 */
	static class Highlighting { // TODO: rename to HighlightingStyle
		
		/** Text attribute */
		private TextAttribute fTextAttribute;
		
		/**
		 * Initialize with the given text attribute.
		 * @param textAttribute The text attribute
		 */
		public Highlighting(TextAttribute textAttribute) {
			setTextAttribute(textAttribute);
		}
		
		/**
		 * @return Returns the text attribute.
		 */
		public TextAttribute getTextAttribute() {
			return fTextAttribute;
		}
		
		/**
		 * @param background The background to set.
		 */
		public void setTextAttribute(TextAttribute textAttribute) {
			fTextAttribute= textAttribute;
		}
	}
	
	/**
	 * Highlighted Positions.
	 */
	static class HighlightedPosition extends Position {
		
		/** Highlighting of the position */
		private Highlighting fStyle;
		
		/** Lock object */
		private Object fLock;
		
		/**
		 * Initialize the styled positions with the given offset, length and foreground color.
		 * 
		 * @param offset The position offset
		 * @param length The position length
		 * @param highlighting The position's highlighting
		 * @param lock The lock object
		 */
		public HighlightedPosition(int offset, int length, Highlighting highlighting, Object lock) {
			super(offset, length);
			fStyle= highlighting;
			fLock= lock;
		}
		
		/**
		 * @return Returns a corresponding style range.
		 */
		public StyleRange createStyleRange() {
			return new StyleRange(getOffset(), getLength(), fStyle.getTextAttribute().getForeground(), fStyle.getTextAttribute().getBackground(), fStyle.getTextAttribute().getStyle());
		}
		
		/**
		 * Uses reference equality for the highlighting.
		 * 
		 * @param offset The offset
		 * @param length The length
		 * @param highlighting The highlighting
		 * @return <code>true</code> iff the given offset, length and highlighting are equal to the internal ones.
		 */
		public boolean isEqual(int offset, int length, Highlighting highlighting) {
			synchronized (fLock) {
				return !isDeleted() && getOffset() == offset && getLength() == length && fStyle == highlighting;
			}
		}

		/**
		 * Is this position contained in the given range (inclusive)? Synchronizes on position updater.
		 * 
		 * @param offset The range offset
		 * @param length The range length
		 * @return <code>true</code> iff this position is not delete and contained in the given range.
		 */
		public boolean isContained(int offset, int length) {
			synchronized (fLock) {
				return !isDeleted() && offset <= getOffset() && offset + length >= getOffset() + getLength();
			}
		}

		public void update(int offset, int length) {
			synchronized (fLock) {
				super.setOffset(offset);
				super.setLength(length);
			}
		}
		
		/*
		 * @see org.eclipse.jface.text.Position#setLength(int)
		 */
		public void setLength(int length) {
			synchronized (fLock) {
				super.setLength(length);
			}
		}
		
		/*
		 * @see org.eclipse.jface.text.Position#setOffset(int)
		 */
		public void setOffset(int offset) {
			synchronized (fLock) {
				super.setOffset(offset);
			}
		}
		
		/*
		 * @see org.eclipse.jface.text.Position#delete()
		 */
		public void delete() {
			synchronized (fLock) {
				super.delete();
			}
		}
		
		/*
		 * @see org.eclipse.jface.text.Position#undelete()
		 */
		public void undelete() {
			synchronized (fLock) {
				super.undelete();
			}
		}
		
		/**
		 * @return Returns the highlighting.
		 */
		public Highlighting getHighlighting() {
			return fStyle;
		}
	}
	
	/** Semantic highlighting presenter */
	private SemanticHighlightingPresenter fPresenter;
	/** Semantic highlighting reconciler */
	private SemanticHighlightingReconciler fReconciler;
	
	/** Semantic highlightings */
	private SemanticHighlighting[] fSemanticHighlightings;
	/** Highlightings */
	private Highlighting[] fHighlightings;

	/** The color manager */
	private IColorManager fColorManager;
	/** The preference store */
	private IPreferenceStore fPreferenceStore;
	
	/**
	 * Install the semantic highlighting on the given editor infrastructure
	 * 
	 * @param editor The compilation unit editor
	 * @param sourceViewer The source viewer 
	 * @param colorManager The color manager
	 * @param preferenceStore The preference store
	 */
	public void install(CompilationUnitEditor editor, ISourceViewer sourceViewer, IColorManager colorManager, IPreferenceStore preferenceStore, PresentationReconciler backgroundPresentationReconciler) {
		if (sourceViewer instanceof ITextViewerExtension2 && sourceViewer instanceof ITextViewerExtension4 && backgroundPresentationReconciler instanceof JavaPresentationReconciler) {
			fColorManager= colorManager;
			fPreferenceStore= preferenceStore;

			initializeHighlightings();
			
			fPresenter= new SemanticHighlightingPresenter();
			fPresenter.install(editor, sourceViewer, backgroundPresentationReconciler);
			
			fReconciler= new SemanticHighlightingReconciler();
			fReconciler.install(editor, fPresenter, fSemanticHighlightings, fHighlightings);
		}
	}
	
	/**
	 * Uninstall the semantic highlighting
	 */
	public void uninstall() {
		if (fReconciler != null) {
			fReconciler.uninstall();
			fReconciler= null;
		}
		
		if (fPresenter != null) {
			fPresenter.uninstall();
			fPresenter= null;
		}
		
		if (fSemanticHighlightings != null)
			disposeHighlightings();
		
		fColorManager= null;
		fPreferenceStore= null;
	}

	/**
	 * 
	 */
	private void initializeHighlightings() {
		fSemanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
		fHighlightings= new Highlighting[fSemanticHighlightings.length];
		
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
			SemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
			String colorKey= SemanticHighlightings.getColorPreferenceKey(semanticHighlighting);
			String boldKey= SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting);
			addColor(colorKey);
			fHighlightings[i]= new Highlighting(new TextAttribute(fColorManager.getColor(PreferenceConverter.getColor(fPreferenceStore, colorKey)), null, fPreferenceStore.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL));
		}
	}

	/**
	 * 
	 */
	private void disposeHighlightings() {
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++)
			removeColor(SemanticHighlightings.getColorPreferenceKey(fSemanticHighlightings[i]));
		
		fSemanticHighlightings= null;
		fHighlightings= null;
	}

	/**
	 * Handle the given property change event
	 * 
	 * @param event The event
	 */
	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
			SemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
			
			String colorKey= SemanticHighlightings.getColorPreferenceKey(semanticHighlighting);
			if (colorKey.equals(event.getProperty())) {
				adaptToTextForegroundChange(fHighlightings[i], event);
				fPresenter.highlightingStyleChanged(fHighlightings[i]);
				continue;
			}
			
			String boldKey= SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting);
			if (boldKey.equals(event.getProperty())) {
				adaptToTextStyleChange(fHighlightings[i], event);
				fPresenter.highlightingStyleChanged(fHighlightings[i]);
				continue;
			}
		}
	}

	private void adaptToTextForegroundChange(Highlighting highlighting, PropertyChangeEvent event) {
		RGB rgb= null;
		
		Object value= event.getNewValue();
		if (value instanceof RGB)
			rgb= (RGB) value;
		else if (value instanceof String)
			rgb= StringConverter.asRGB((String) value);
			
		if (rgb != null) {
			
			String property= event.getProperty();
			
			if (fColorManager instanceof IColorManagerExtension) {
				IColorManagerExtension ext= (IColorManagerExtension) fColorManager;
				ext.unbindColor(property);
				ext.bindColor(property, rgb);
			}
			
			TextAttribute oldAttr= highlighting.getTextAttribute();
			highlighting.setTextAttribute(new TextAttribute(fColorManager.getColor(property), oldAttr.getBackground(), oldAttr.getStyle()));
		}
	}
	
	private void adaptToTextStyleChange(Highlighting highlighting, PropertyChangeEvent event) {
		boolean bold= false;
		Object value= event.getNewValue();
		if (value instanceof Boolean)
			bold= ((Boolean) value).booleanValue();
		else if (IPreferenceStore.TRUE.equals(value))
			bold= true;
		
		TextAttribute oldAttr= highlighting.getTextAttribute();
		boolean isBold= (oldAttr.getStyle() == SWT.BOLD);
		if (isBold != bold) 
			highlighting.setTextAttribute(new TextAttribute(oldAttr.getForeground(), oldAttr.getBackground(), bold ? SWT.BOLD : SWT.NORMAL));
	}
	
	private void addColor(String colorKey) {
		RGB rgb= PreferenceConverter.getColor(fPreferenceStore, colorKey);
		if (fColorManager instanceof IColorManagerExtension) {
			IColorManagerExtension ext= (IColorManagerExtension) fColorManager;
			ext.unbindColor(colorKey);
			ext.bindColor(colorKey, rgb);
		}
	}
	
	private void removeColor(String colorKey) {
		if (fColorManager instanceof IColorManagerExtension)
			((IColorManagerExtension) fColorManager).unbindColor(colorKey);
	}
}
