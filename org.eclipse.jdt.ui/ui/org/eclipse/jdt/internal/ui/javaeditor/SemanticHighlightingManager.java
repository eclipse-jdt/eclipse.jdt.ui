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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IColorManagerExtension;
import org.eclipse.jdt.internal.ui.text.JavaPresentationReconciler;

/**
 * Semantic highlighting manager
 * 
 * @since 3.0
 */
public class SemanticHighlightingManager implements IPropertyChangeListener {
	
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
	
	/**
	 * Highlighted ranges.
	 */
	public static class HighlightedRange extends Region {
		/** The highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}. */
		private String fKey;
		
		/**
		 * Initalize with the given offset, length and highlighting key.
		 * 
		 * @param offset
		 * @param length
		 * @param key the highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}
		 */
		public HighlightedRange(int offset, int length, String key) {
			super(offset, length);
			fKey= key;
		}
		
		/**
		 * @return the highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}
		 */
		public String getKey() {
			return fKey;
		}
		
		/*
		 * @see org.eclipse.jface.text.Region#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof HighlightedRange && fKey.equals(((HighlightedRange)o).getKey());
		}
		
		/*
		 * @see org.eclipse.jface.text.Region#hashCode()
		 */
		public int hashCode() {
			return super.hashCode() | fKey.hashCode();
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

	/** The editor */
	private JavaEditor fEditor;
	/** The source viewer */
	private ISourceViewer fSourceViewer;
	/** The color manager */
	private IColorManager fColorManager;
	/** The preference store */
	private IPreferenceStore fPreferenceStore;
	/** The presentation reconciler */
	private JavaPresentationReconciler fPresentationReconciler;
	
	/** The hardcoded ranges */
	private HighlightedRange[] fHardcodedRanges;
	
	/**
	 * Install the semantic highlighting on the given editor infrastructure
	 * 
	 * @param editor The Java editor
	 * @param sourceViewer The source viewer 
	 * @param colorManager The color manager
	 * @param preferenceStore The preference store
	 * @param backgroundPresentationReconciler the background presentation reconciler
	 */
	public void install(JavaEditor editor, ISourceViewer sourceViewer, IColorManager colorManager, IPreferenceStore preferenceStore, JavaPresentationReconciler backgroundPresentationReconciler) {
		if (sourceViewer instanceof ITextViewerExtension2 && sourceViewer instanceof ITextViewerExtension4) {
			fEditor= editor;
			fSourceViewer= sourceViewer;
			fColorManager= colorManager;
			fPreferenceStore= preferenceStore;
			fPresentationReconciler= backgroundPresentationReconciler;
			
			fPreferenceStore.addPropertyChangeListener(this);

			if (isEnabled())
				enable();
		}
	}
	
	/**
	 * Install the semantic highlighting on the given source viewer infrastructure. No reconciliation will be performed.
	 * 
	 * @param sourceViewer the source viewer 
	 * @param colorManager the color manager
	 * @param preferenceStore the preference store
	 */
	public void install(ISourceViewer sourceViewer, IColorManager colorManager, IPreferenceStore preferenceStore, HighlightedRange[] hardcodedRanges) {
		fHardcodedRanges= hardcodedRanges;
		install(null, sourceViewer, colorManager, preferenceStore, null);
	}
	
	/**
	 * Enable semantic highlighting.
	 */
	private void enable() {
		initializeHighlightings();
		
		fPresenter= new SemanticHighlightingPresenter();
		fPresenter.install(fSourceViewer, fPresentationReconciler);
		
		if (fEditor != null) {
			fReconciler= new SemanticHighlightingReconciler();
			fReconciler.install(fEditor, fSourceViewer, fPresenter, fSemanticHighlightings, fHighlightings);
		} else {
			fPresenter.updatePresentation(null, createHardcodedPositions(), new HighlightedPosition[0]);
		}
	}
	
	/**
	 * Computes the hardcoded postions from the hardcoded ranges
	 * 
	 * @return the hardcoded positions
	 */
	private HighlightedPosition[] createHardcodedPositions() {
		HighlightedPosition[] positions= new HighlightedPosition[fHardcodedRanges.length];
		for (int i= 0; i < fHardcodedRanges.length; i++) {
			HighlightedRange range= fHardcodedRanges[i];
			positions[i]= fPresenter.createHighlightedPosition(range.getOffset(), range.getLength(), getHighlighting(range.getKey()));
		}
		return positions;
	}

	/**
	 * Returns the higlighting corresponding to the given key.
	 * 
	 * @param key the highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}
	 * @return the corresponding highlighting
	 */
	private Highlighting getHighlighting(String key) {
		for (int i= 0; i < fSemanticHighlightings.length; i++) {
			SemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
			if (key.equals(semanticHighlighting.getPreferenceKey()))
				return fHighlightings[i];
		}
		return null;
	}

	/**
	 * Uninstall the semantic highlighting
	 */
	public void uninstall() {
		disable();
		
		if (fPreferenceStore != null) {
			fPreferenceStore.removePropertyChangeListener(this);
			fPreferenceStore= null;
		}
		
		fEditor= null;
		fSourceViewer= null;
		fColorManager= null;
		fPresentationReconciler= null;
	}

	/**
	 * Disable semantic highlighting.
	 */
	private void disable() {
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
	}

	/**
	 * @return <code>true</code> iff semantic highlighting is enabled in the preferences
	 */
	private boolean isEnabled() {
		return fPreferenceStore.getBoolean(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED);
	}
	
	/**
	 * Initialize semantic highlightings.
	 */
	private void initializeHighlightings() {
		fSemanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
		fHighlightings= new Highlighting[fSemanticHighlightings.length];
		
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
			SemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
			String colorKey= SemanticHighlightings.getColorPreferenceKey(semanticHighlighting);
			addColor(colorKey);
			
			String boldKey= SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting);
			int style= fPreferenceStore.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL;

			String italicKey= SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting);
			if (fPreferenceStore.getBoolean(italicKey))
				style |= SWT.ITALIC;

			fHighlightings[i]= new Highlighting(new TextAttribute(fColorManager.getColor(PreferenceConverter.getColor(fPreferenceStore, colorKey)), null, style));
		}
	}

	/**
	 * Dispose the semantic highlightings.
	 */
	private void disposeHighlightings() {
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++)
			removeColor(SemanticHighlightings.getColorPreferenceKey(fSemanticHighlightings[i]));
		
		fSemanticHighlightings= null;
		fHighlightings= null;
	}

	/*
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		handlePropertyChangeEvent(event);
	}

	/**
	 * Handle the given property change event
	 * 
	 * @param event The event
	 */
	private void handlePropertyChangeEvent(PropertyChangeEvent event) {
		if (fPreferenceStore == null)
			return; // Uninstalled during event notification
		
		if (PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED.equals(event.getProperty())) {
			if (isEnabled())
				enable();
			else
				disable();
			return;
		}
		
		if (!isEnabled())
			return;
		
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
				adaptToTextStyleChange(fHighlightings[i], event, SWT.BOLD);
				fPresenter.highlightingStyleChanged(fHighlightings[i]);
				continue;
			}
			
			String italicKey= SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting);
			if (italicKey.equals(event.getProperty())) {
				adaptToTextStyleChange(fHighlightings[i], event, SWT.ITALIC);
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
	
	private void adaptToTextStyleChange(Highlighting highlighting, PropertyChangeEvent event, int styleAttribute) {
		boolean eventValue= false;
		Object value= event.getNewValue();
		if (value instanceof Boolean)
			eventValue= ((Boolean) value).booleanValue();
		else if (IPreferenceStore.TRUE.equals(value))
			eventValue= true;
		
		TextAttribute oldAttr= highlighting.getTextAttribute();
		boolean activeValue= (oldAttr.getStyle() & styleAttribute) == styleAttribute;
		
		if (activeValue != eventValue) 
			highlighting.setTextAttribute(new TextAttribute(oldAttr.getForeground(), oldAttr.getBackground(), eventValue ? oldAttr.getStyle() | styleAttribute : oldAttr.getStyle() & ~styleAttribute));
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
