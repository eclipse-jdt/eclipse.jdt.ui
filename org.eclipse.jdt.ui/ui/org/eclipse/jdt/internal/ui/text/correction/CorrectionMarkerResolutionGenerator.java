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
package org.eclipse.jdt.internal.ui.text.correction;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;

/**
  */
public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

	public static class CorrectionMarkerResolution implements IMarkerResolution, IMarkerResolution2 {
	
		private ICompilationUnit fCompilationUnit;
		private int fOffset;
		private int fLength;
		private IJavaCompletionProposal fProposal;
	
		/**
		 * Constructor for CorrectionMarkerResolution.
		 */
		public CorrectionMarkerResolution(ICompilationUnit cu, int offset, int length, IJavaCompletionProposal proposal) {
			fCompilationUnit= cu;
			fOffset= offset;
			fLength= length;
			fProposal= proposal;
		}
	
		/* (non-Javadoc)
		 * @see IMarkerResolution#getLabel()
		 */
		public String getLabel() {
			return fProposal.getDisplayString();
		}
	
		/* (non-Javadoc)
		 * @see IMarkerResolution#run(IMarker)
		 */
		public void run(IMarker marker) {
			try {				
				IEditorPart part= EditorUtility.isOpenInEditor(fCompilationUnit);
				if (part == null) {
					part= EditorUtility.openInEditor(fCompilationUnit);
					if (part instanceof ITextEditor) {
						((ITextEditor) part).selectAndReveal(fOffset, fLength);
					}					
				}
				if (part != null) {				
					IEditorInput input= part.getEditorInput();
					IDocument doc= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getDocument(input);
					fProposal.apply(doc);
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
		 */
		public String getDescription() {
			return fProposal.getAdditionalProposalInfo();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getImage()
		 */
		public Image getImage() {
			return fProposal.getImage();
		}
	
	}
	
	private static final IMarkerResolution[] NO_RESOLUTIONS= new IMarkerResolution[0];


	/**
	 * Constructor for CorrectionMarkerResolutionGenerator.
	 */
	public CorrectionMarkerResolutionGenerator() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		int id= marker.getAttribute(IJavaModelMarker.ID, -1);
		ICompilationUnit cu= getCompilationUnit(marker);
		return cu != null && JavaCorrectionProcessor.hasCorrections(cu, id);
	}	

	/* (non-Javadoc)
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return NO_RESOLUTIONS; 
		}
		
		try {
			ICompilationUnit cu= getCompilationUnit(marker);
			if (cu != null) {
				IEditorInput input= EditorUtility.getEditorInput(cu);
				if (input != null) {
					IProblemLocation location= findProblemLocation(input, marker);
					if (location != null) {
						
						IInvocationContext context= new AssistContext(cu,  location.getOffset(), location.getLength());
						ArrayList proposals= new ArrayList();
						JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
						Collections.sort(proposals, JavaCompletionProposalComparator.getInstance());
						
						int nProposals= proposals.size();
						IMarkerResolution[] resolutions= new IMarkerResolution[nProposals];
						for (int i= 0; i < nProposals; i++) {
							resolutions[i]= new CorrectionMarkerResolution(context.getCompilationUnit(), location.getOffset(), location.getLength(), (IJavaCompletionProposal) proposals.get(i));
						}
						return resolutions;
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return NO_RESOLUTIONS; 
	}

	private ICompilationUnit getCompilationUnit(IMarker marker) {
		IResource res= marker.getResource();
		if (res instanceof IFile && res.isAccessible()) {
			IJavaElement element= JavaCore.create((IFile) res);
			if (element instanceof ICompilationUnit)
				return (ICompilationUnit) element;
		}
		return null;
	}
	
	private IProblemLocation findProblemLocation(IEditorInput input, IMarker marker) {
		IAnnotationModel model= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel(input);
		if (model != null) { // open in editor
			Iterator iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				Object curr= iter.next();
				if (curr instanceof JavaMarkerAnnotation) {
					JavaMarkerAnnotation annot= (JavaMarkerAnnotation) curr;
					
					if (marker.equals(annot.getMarker())) {
						Position pos= model.getPosition(annot);
						if (pos != null) {
							return new ProblemLocation(pos.getOffset(), pos.getLength(), annot);
						}
					}
				}
			}
		} else { // not open in editor
			ICompilationUnit cu= getCompilationUnit(marker);
			int id= marker.getAttribute(IJavaModelMarker.ID, -1);
			int start= marker.getAttribute(IMarker.CHAR_START, -1);
			int end= marker.getAttribute(IMarker.CHAR_END, -1);
			int severity= marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			String[] arguments= CorrectionEngine.getProblemArguments(marker);
			if (cu != null && id != -1 && start != -1 && end != -1 && arguments != null) {
				boolean isError= (severity == IMarker.SEVERITY_ERROR);
				return new ProblemLocation(start, end - start, id, arguments, isError);
			}			
		}
		return null;
	}


}
