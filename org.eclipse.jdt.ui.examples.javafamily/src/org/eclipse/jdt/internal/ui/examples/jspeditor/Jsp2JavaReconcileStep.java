/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.AbstractReconcileStep;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcilableModel;
import org.eclipse.jface.text.source.translation.ITranslator;

import org.eclipse.jsp.JspTranslator;

/**
 * This reconcile step has a JSP source document as 
 * input model and maintains a document that contains the Java
 * source.
 *
 * @since 3.0
 */
public class Jsp2JavaReconcileStep extends AbstractReconcileStep {
	
	private DocumentAdapter fModel;
	private ITranslator fJspTranslator;

	/**
	 * Creates the last reconcile step of the pipe.
	 */
	public Jsp2JavaReconcileStep() {
		initialize();
	}

	/**
	 * Creates an intermediate reconcile step which adds
	 * the given step to the pipe.
	 */
	public Jsp2JavaReconcileStep(IReconcileStep step) {
		super(step);
		initialize();
	}
	
	protected void initialize()  {
		fJspTranslator= new JspTranslator();
		fJspTranslator.setTagHandlerFactory(new Jsp2JavaTagHandlerFactory());
	}

	/*
	 * @see AbstractReconcileStep#reconcileModel(DirtyRegion, IRegion)
	 */
	protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion) {
		Assert.isTrue(getInputModel() instanceof DocumentAdapter, "wrong model"); //$NON-NLS-1$

		System.out.println("reconciling jsp2java..."); //$NON-NLS-1$
		
		Reader reader= new StringReader(((DocumentAdapter)fInputModel).getDocument().get());
		try {
			String javaSource= fJspTranslator.translate(reader, "Demo"); //$NON-NLS-1$
			fModel= new DocumentAdapter(new Document(javaSource));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// This reconcile step does not create own results
		return null;
	}

	/*
	 * @see AbstractReconcileStep#getModel()
	 */
	public IReconcilableModel getModel() {
		return fModel;
	}

	/*
	 * @see AbstractReconcileStep#convertToInputModel(IReconcileResult[])
	 */
	protected IReconcileResult[] convertToInputModel(IReconcileResult[] inputResults) {

		if (inputResults == null)
			return null;

		// the "only" thing we need to do is to adapt the positions
		int[] smap= fJspTranslator.getLineMapping();

		for (int i= 0; i < inputResults.length; i++) {				
		
			if (isCanceled())
				return null;

			if (!(inputResults[i] instanceof AnnotationAdapter))
				continue;
				
			AnnotationAdapter result= (AnnotationAdapter)inputResults[i];
			Position pos= result.getPosition();
			int javaLine;
			try {
				javaLine= fModel.getDocument().getLineOfOffset(pos.offset);
				
				// Adjust offset to be relative to line beginning
				pos.offset -= fModel.getDocument().getLineOffset(javaLine);
				int relativeLineOffsetInJava= pos.offset;

				int jspLine= smap[javaLine + 1]; // document is 0-based, smap is 1-based
				
				// Add Jsp line offset
				pos.offset += ((DocumentAdapter)getInputModel()).getDocument().getLineOffset(jspLine-1); // document is 0-based, smap is 1-based
				
				String jspLineStr= ((DocumentAdapter)getInputModel()).getDocument().get(((DocumentAdapter)getInputModel()).getDocument().getLineOffset(jspLine-1), ((DocumentAdapter)getInputModel()).getDocument().getLineLength(jspLine-1));

				// XXX: Once partitioner is in place the partition can be used to ease section detection
				int offsetInLine= fJspTranslator.backTranslateOffsetInLine(jspLineStr, null, relativeLineOffsetInJava, null);
				if (offsetInLine > 0)
					pos.offset += offsetInLine;

			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		return inputResults;
	}
}
