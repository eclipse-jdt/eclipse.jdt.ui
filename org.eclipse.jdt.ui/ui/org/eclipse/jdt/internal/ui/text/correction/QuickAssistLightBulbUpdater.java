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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationPresentation;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaAnnotationIterator;
import org.eclipse.jdt.internal.ui.viewsupport.ISelectionListenerWithAST;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionListenerWithASTManager;

/**
 *
 */
public class QuickAssistLightBulbUpdater {

	public static class AssistAnnotation extends Annotation implements IAnnotationPresentation {
		
		//XXX: To be fully correct this should be a non-static fields in QuickAssistLightBulbUpdater 
		private static final int LAYER;
		
		static {
			Annotation annotation= new Annotation("org.eclipse.jdt.ui.error", false, null); //$NON-NLS-1$
			// XXX: This should be offered by Editor plug-in
			AnnotationPreference preference= new AnnotationPreferenceLookup().getAnnotationPreference(annotation);
			if (preference != null)
				LAYER= preference.getPresentationLayer() + 1;
			else
				LAYER= IAnnotationAccessExtension.DEFAULT_LAYER + 1;
			
		}
		
		private Image fImage;
		
		public AssistAnnotation() {
		}
		
		/*
		 * @see org.eclipse.jface.text.source.IAnnotationPresentation#getLayer()
		 */
		public int getLayer() {
			return LAYER;
		}
		
		private Image getImage() {
			if (fImage == null) {
				fImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_ASSIST);
			}
			return fImage;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.source.Annotation#paint(org.eclipse.swt.graphics.GC, org.eclipse.swt.widgets.Canvas, org.eclipse.swt.graphics.Rectangle)
		 */
		public void paint(GC gc, Canvas canvas, Rectangle r) {
			drawImage(getImage(), gc, canvas, r, SWT.CENTER, SWT.TOP);
		}
		
	}
	
	private final Annotation fAnnotation;
	private boolean fIsAnnotationShown;
	private IEditorPart fEditor;
	private ITextViewer fViewer;
	
	private ISelectionListenerWithAST fListener;
	private IPropertyChangeListener fPropertyChangeListener;
	
	public QuickAssistLightBulbUpdater(IEditorPart part, ITextViewer viewer) {
		fEditor= part;
		fViewer= viewer;
		fAnnotation= new AssistAnnotation();
		fIsAnnotationShown= false;
		fPropertyChangeListener= null;
	}
	
	public boolean isSetInPreferences() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_QUICKASSIST_LIGHTBULB);
	}
	
	private void installSelectionListener() {
		fListener= new ISelectionListenerWithAST() {
			public void selectionChanged(IEditorPart part, ITextSelection selection, CompilationUnit astRoot) {
				doSelectionChanged(selection.getOffset(), selection.getLength(), astRoot);
			}
		};
		SelectionListenerWithASTManager.getDefault().addListener(fEditor, fListener);
	}
	
	private void uninstallSelectionListener() {
		if (fListener != null) {
			SelectionListenerWithASTManager.getDefault().removeListener(fEditor, fListener);
			fListener= null;
		}
		IAnnotationModel model= getAnnotationModel();
		if (model != null) {
			removeLightBulb(model);
		}
	}	
	
	public void install() {
		if (isSetInPreferences()) {
			installSelectionListener();
		}
		if (fPropertyChangeListener == null) {
			fPropertyChangeListener= new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					doPropertyChanged(event.getProperty());
				}
			};
			PreferenceConstants.getPreferenceStore().addPropertyChangeListener(fPropertyChangeListener);		
		}
	}
	
	public void uninstall() {
		uninstallSelectionListener();
		if (fPropertyChangeListener != null) {
			PreferenceConstants.getPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);		
			fPropertyChangeListener= null;
		}
	}
	
	protected void doPropertyChanged(String property) {
		if (property.equals(PreferenceConstants.APPEARANCE_QUICKASSIST_LIGHTBULB)) {
			if (isSetInPreferences()) {
				ICompilationUnit cu= getCompilationUnit();
				if (cu != null) {
					installSelectionListener();
					Point point= fViewer.getSelectedRange();
					CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
					doSelectionChanged(point.x, point.y, astRoot);
				}
			} else {
				uninstallSelectionListener();
			}			
		}
	}	
	
	private ICompilationUnit getCompilationUnit() {
		IEditorInput input= fEditor.getEditorInput();
		Object elem= input.getAdapter(IJavaElement.class);
		if (elem instanceof ICompilationUnit) {
			return (ICompilationUnit) elem;
		}
		return null;
	}
	
	private IAnnotationModel getAnnotationModel() {
		return JavaUI.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
	}
	
	private IDocument getDocument() {
		return JavaUI.getDocumentProvider().getDocument(fEditor.getEditorInput());
	}
	
	
	private void doSelectionChanged(int offset, int length, CompilationUnit astRoot) {
		
		final IAnnotationModel model= getAnnotationModel();
		final ICompilationUnit cu= getCompilationUnit();
		if (model == null || cu == null) {
			return;
		}
		
		final AssistContext context= new AssistContext(cu, offset, length);
		context.setASTRoot(astRoot);
		
		boolean hasQuickFix= hasQuickFixLightBulb(model, context.getSelectionOffset());
		if (hasQuickFix) {
			removeLightBulb(model);
			return; // there is already a quick fix light bulb at the new location
		}
		
		calculateLightBulb(model, context);
	}
	
	/*
	 * Needs to be called synchronized
	 */
	private void calculateLightBulb(IAnnotationModel model, IInvocationContext context) {
		boolean needsAnnotation= JavaCorrectionProcessor.hasAssists(context);
		if (fIsAnnotationShown) {
			model.removeAnnotation(fAnnotation);
		}
		if (needsAnnotation) {
			model.addAnnotation(fAnnotation, new Position(context.getSelectionOffset(), context.getSelectionLength()));
		}
		fIsAnnotationShown= needsAnnotation;
	}	
		
	private void removeLightBulb(IAnnotationModel model) {
		synchronized (this) {
			if (fIsAnnotationShown) {
				model.removeAnnotation(fAnnotation);
				fIsAnnotationShown= false;
			}
		}
	}
	
	/*
	 * Tests if there is already a quick fix light bulb on the current line
	 */	
	private boolean hasQuickFixLightBulb(IAnnotationModel model, int offset) {
		try {
			IDocument document= getDocument();
			int currLine= document.getLineOfOffset(offset);
			
			Iterator iter= new JavaAnnotationIterator(model, true);
			while (iter.hasNext()) {
				IJavaAnnotation annot= (IJavaAnnotation) iter.next();
				Position pos= model.getPosition((Annotation) annot);
				if (pos != null) {
					int startLine= document.getLineOfOffset(pos.getOffset());
					if (startLine == currLine && JavaCorrectionProcessor.hasCorrections(annot)) {
						return true;
					}
				}
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return false;
	}


}
