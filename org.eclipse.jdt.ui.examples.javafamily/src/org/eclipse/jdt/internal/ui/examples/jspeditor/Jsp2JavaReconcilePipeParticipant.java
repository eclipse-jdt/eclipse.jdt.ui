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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.source.ITagHandler;
import org.eclipse.jface.text.source.ITagHandlerFactory;
import org.eclipse.jface.text.source.ITranslator;

import org.eclipse.jsp.JspTranslator;
import org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant;
import org.eclipse.text.reconcilerpipe.AnnotationAdapter;
import org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant;
import org.eclipse.text.reconcilerpipe.IReconcileResult;
import org.eclipse.text.reconcilerpipe.ITextModel;
import org.eclipse.text.reconcilerpipe.TextModelAdapter;

/**
 * This reconcile pipe participant has a JSP source document as 
 * input model and maintains a document that contains the Java
 * source.
 *
 * @since 3.0
 */
public class Jsp2JavaReconcilePipeParticipant extends AbstractReconcilePipeParticipant {
	
	private TextModelAdapter fModel;
	private ITranslator fJspTranslator;
	private ITagHandlerFactory fTagHandlerFactory;

	/**
	 * Creates the last reconcile participant of the pipe.
	 */
	public Jsp2JavaReconcilePipeParticipant() {
		initialize();
	}

	/**
	 * Creates an intermediate reconcile participant which adds
	 * the given participant to the pipe.
	 */
	public Jsp2JavaReconcilePipeParticipant(IReconcilePipeParticipant participant) {
		super(participant);
		initialize();
	}
	
	protected void initialize()  {
		fJspTranslator= new JspTranslator();
		fTagHandlerFactory= new Jsp2JavaTagHandlerFactory();
		fJspTranslator.setTagHandlerFactory(fTagHandlerFactory);
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant#reconcileModel(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion) {
		Assert.isTrue(getInputModel() instanceof TextModelAdapter, "wrong model"); //$NON-NLS-1$

		System.out.println("reconciling jsp2java...");
		
		Reader reader= new StringReader(((TextModelAdapter)fInputModel).getDocument().get());
		try {
			String javaSource= fJspTranslator.translate(reader, "Demo");
			fModel= new TextModelAdapter(new Document(javaSource));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// This participant does not create own results
		return null;
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant#getModel()
	 */
	public ITextModel getModel() {
		return fModel;
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant#convertToInputModel(org.eclipse.text.reconcilerpipe.IReconcileResult[])
	 */
	protected IReconcileResult[] convertToInputModel(IReconcileResult[] inputResults) {
		// the "only" thing we need to do is to adapt the positions
		int[] smap= fJspTranslator.getSmap();

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
				pos.offset += ((TextModelAdapter)getInputModel()).getDocument().getLineOffset(jspLine-1); // document is 0-based, smap is 1-based
				
				String jspLineStr= ((TextModelAdapter)getInputModel()).getDocument().get(((TextModelAdapter)getInputModel()).getDocument().getLineOffset(jspLine-1), ((TextModelAdapter)getInputModel()).getDocument().getLineLength(jspLine-1));

				// XXX: Once partitioner is in place the partition can be used to ease section detection
				ITagHandler handler= fTagHandlerFactory.findHandler(jspLineStr);
				pos.offset += handler.backTranslateOffsetInLine(jspLineStr, null, relativeLineOffsetInJava);

			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		return inputResults;
	}
}