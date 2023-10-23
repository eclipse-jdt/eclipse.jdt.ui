/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Reconciling strategy for Java parts in JSP files.
 *
 * @since 3.0
 */
public class JspReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private final IReconcileStep fFirstStep;
	private final ITextEditor fTextEditor;
	private IProgressMonitor fProgressMonitor;

	public JspReconcilingStrategy(ISourceViewer sourceViewer, ITextEditor textEditor) {
		fTextEditor= textEditor;
		IReconcileStep javaReconcileStep= new JavaReconcileStep(getFile());
		fFirstStep= new Jsp2JavaReconcileStep(javaReconcileStep);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	@Override
	public void setDocument(IDocument document) {
		fFirstStep.setInputModel(new DocumentAdapter(document));
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		removeTemporaryAnnotations();
		process(fFirstStep.reconcile(dirtyRegion, subRegion));
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	@Override
	public void reconcile(IRegion partition) {
		removeTemporaryAnnotations();
		process(fFirstStep.reconcile(partition));
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
		fFirstStep.setProgressMonitor(monitor);
		fProgressMonitor= monitor;

	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	 */
	@Override
	public void initialReconcile() {
		fFirstStep.reconcile(null);

	}

	private void process(final IReconcileResult[] results) {

		if (results == null)
			return;

		IRunnableWithProgress runnable= new WorkspaceModifyOperation(null) 	 {
			/*
			 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
			 */
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
				for (IReconcileResult r : results) {
					if (fProgressMonitor != null && fProgressMonitor.isCanceled())
						return;
					if (!(r instanceof AnnotationAdapter)) {
						continue;
					}
					AnnotationAdapter result= (AnnotationAdapter) r;
					Position pos= result.getPosition();
					Annotation annotation= result.createAnnotation();
					getAnnotationModel().addAnnotation(annotation, pos);
				}
			}
		};
		try {
			runnable.run(null);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private IAnnotationModel getAnnotationModel()  {
		return fTextEditor.getDocumentProvider().getAnnotationModel(fTextEditor.getEditorInput());
	}

	/*
	 * XXX: A "real" implementation must be smarter
	 * 		i.e. don't remove and add the annotations
	 * 		which are the same.
	 */
	private void removeTemporaryAnnotations() {
		Iterator iter= getAnnotationModel().getAnnotationIterator();
		while (iter.hasNext())  {
			Object annotation= iter.next();
			if (annotation instanceof Annotation)  {
				Annotation extension= (Annotation)annotation;
					if (!extension.isPersistent())
						getAnnotationModel().removeAnnotation((Annotation)annotation);
			}
		}
	}

	private IFile getFile() {
		IEditorInput input= fTextEditor.getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;

		return ((IFileEditorInput)input).getFile();
	}
}
