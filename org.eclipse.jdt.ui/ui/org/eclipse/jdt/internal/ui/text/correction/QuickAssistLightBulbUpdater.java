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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaAnnotationIterator;

/**
 *
 */
public class QuickAssistLightBulbUpdater {

	public static class AssistAnnotation extends Annotation {
		
		private Image fImage;
		
		public AssistAnnotation() {
			setLayer(MarkerAnnotation.PROBLEM_LAYER + 1);
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
	
	private Job fCurrentJob;
	
	private ISelectionChangedListener fListener;
	private IPropertyChangeListener fPropertyChangeListener;
	
	public QuickAssistLightBulbUpdater(IEditorPart part, ITextViewer viewer) {
		fEditor= part;
		fViewer= viewer;
		fAnnotation= new AssistAnnotation();
		fIsAnnotationShown= false;
		fPropertyChangeListener= null;
		
		fCurrentJob= null;
	}
	
	public boolean isSetInPreferences() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_QUICKASSIST_LIGHTBULB);
	}
	
	private void installSelectionListener() {
		ISelectionProvider provider= fViewer.getSelectionProvider();
		if (provider instanceof IPostSelectionProvider) {
			fListener= new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					doSelectionChanged();
				}
			};
			((IPostSelectionProvider) provider).addPostSelectionChangedListener(fListener);
		}		
	}
	
	private void uninstallSelectionListener() {
		IAnnotationModel model= getAnnotationModel();
		if (fListener != null && model != null) {
			IPostSelectionProvider provider= (IPostSelectionProvider) fViewer.getSelectionProvider();
			provider.removePostSelectionChangedListener(fListener);
			fListener= null;
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
				installSelectionListener();
				doSelectionChanged();
			} else {
				uninstallSelectionListener();
			}			
		}
	}	
	
	private ICompilationUnit getCompilationUnit(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) input).getFile();
			IJavaElement element= JavaCore.create(file);
			if (element instanceof ICompilationUnit) {
				return JavaModelUtil.toWorkingCopy((ICompilationUnit) element);
			}
		}
		return null;
	}
	
	private IAnnotationModel getAnnotationModel() {
		return JavaUI.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
	}
	
	private IDocument getDocument() {
		return JavaUI.getDocumentProvider().getDocument(fEditor.getEditorInput());
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	private void doSelectionChanged() {
		
		final IAnnotationModel model= getAnnotationModel();
		final ICompilationUnit cu= getCompilationUnit(fEditor.getEditorInput());
		if (model == null || cu == null) {
			return;
		}
		
		Point point= fViewer.getSelectedRange();
		int offset= point.x;
		int length= point.y;
		final IInvocationContext context= new AssistContext(cu, offset, length);
		
		boolean hasQuickFix= hasQuickFixLightBulb(model, context.getSelectionOffset());
		if (hasQuickFix) {
			removeLightBulb(model);
			return; // there is already a quick fix light bulb at the new location
		}
		
		if (fCurrentJob != null) {
			fCurrentJob.cancel();
			fCurrentJob= null;
		}
				
		fCurrentJob= new Job(CorrectionMessages.getString("QuickAssistLightBulbUpdater.job.title")) { //$NON-NLS-1$
			public IStatus run(IProgressMonitor monitor) {
				synchronized (QuickAssistLightBulbUpdater.this) {
					if (this != fCurrentJob) {
						return Status.OK_STATUS;
					}
					calculateLightBulb(model, context);
					return Status.OK_STATUS;
				}
			}
		};
		fCurrentJob.setPriority(Job.DECORATE);
		fCurrentJob.schedule();
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
