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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerHelpRegistry;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaAnnotationIterator;
import org.eclipse.jdt.internal.ui.text.java.IJavaCompletionProposal;


public class JavaCorrectionProcessor implements IContentAssistProcessor {


	private static class CorrectionsComparator implements Comparator {
		
		private static Collator fgCollator= Collator.getInstance();
		
		public int compare(Object o1, Object o2) {
			if ((o1 instanceof IJavaCompletionProposal) && (o2 instanceof IJavaCompletionProposal)) {
				IJavaCompletionProposal e1= (IJavaCompletionProposal) o1;
				IJavaCompletionProposal e2= (IJavaCompletionProposal) o2;				
				int del= e2.getRelevance() - e1.getRelevance();
				if (del != 0) {
					return del;
				}
				return fgCollator.compare(e1.getDisplayString(), e2.getDisplayString());

			}				
			return fgCollator.compare(((ICompletionProposal) o1).getDisplayString(), ((ICompletionProposal) o2).getDisplayString());
		}
	}
	
	private static IAssistProcessor[] fAssistProcessors= null;
	private static ICorrectionProcessor[] fCorrectionProcessors= null;
	
	private static ICorrectionProcessor[] getCorrectionProcessors() {
		if (fCorrectionProcessors == null) {
			fCorrectionProcessors= new ICorrectionProcessor[] {
				new QuickFixProcessor()			};
		}
		return fCorrectionProcessors;
	}
	
	private static IAssistProcessor[] getAssistProcessors() {
		if (fAssistProcessors == null) {
			fAssistProcessors= new IAssistProcessor[] {
				new QuickAssistProcessor(),
				new QuickTemplateProcessor()
			};
		}
		return fAssistProcessors;
	}	
	
	public static boolean hasCorrections(int problemId) {
		ICorrectionProcessor[] processors= getCorrectionProcessors();
		for (int i= 0; i < processors.length; i++) {
			if (processors[i].hasCorrections(problemId)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasCorrections(IJavaAnnotation annotation) {
		int problemId= annotation.getId();
		if (problemId == -1) {
			if (annotation instanceof MarkerAnnotation) {
				return hasCorrections(((MarkerAnnotation) annotation).getMarker());
			}
			return false;
		} else {
			return hasCorrections(problemId);
		}
	}
	
	public static boolean hasCorrections(IMarker marker) {
		if (marker == null || !marker.exists())
			return false;
		
		try {
			if (marker.isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)) {
				int problemId= marker.getAttribute(IJavaModelMarker.ID, -1);
				return problemId != -1 && hasCorrections(problemId);
			} else {
				IMarkerHelpRegistry registry= PlatformUI.getWorkbench().getMarkerHelpRegistry();
				return registry != null && registry.hasResolutions(marker);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return false;
		}
	}	
	
	private IEditorPart fEditor;

	/**
	 * Constructor for JavaCorrectionProcessor.
	 */
	public JavaCorrectionProcessor(IEditorPart editor) {
		fEditor= editor;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());

		ArrayList proposals= new ArrayList();
		if (model != null) {
			processProblemAnnotations(cu, model, viewer, documentOffset, proposals);
		}
		
		if (proposals.isEmpty()) {
			proposals.add(new NoCorrectionProposal(null, null));
		}
		ICompletionProposal[] res= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
		Arrays.sort(res, new CorrectionsComparator());
		return res;
	}
	
	private boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	

	private void processProblemAnnotations(ICompilationUnit cu, IAnnotationModel model, ITextViewer viewer, int offset, ArrayList proposals) {
		CorrectionContext context= new CorrectionContext(cu);
		
		HashSet idsProcessed= new HashSet();
		Iterator iter= new JavaAnnotationIterator(model, true);
		while (iter.hasNext()) {
			IJavaAnnotation annot= (IJavaAnnotation) iter.next();
			Position pos= model.getPosition((Annotation) annot);
			if (isAtPosition(offset, pos)) {
				int problemId= annot.getId();
				if (problemId != -1) {
					if (idsProcessed.add(new Integer(problemId))) { // collect only once per problem id
						context.initialize(pos.getOffset(), pos.getLength(), annot.getId(), annot.getArguments());
						collectCorrections(context, proposals);
						if (proposals.isEmpty()) {
							//proposals.add(new NoCorrectionProposal(pp, annot.getMessage()));
						}
					}	
				} else {
					if (annot instanceof MarkerAnnotation) {
						IMarker marker= ((MarkerAnnotation) annot).getMarker();
						IMarkerResolution[] res= PlatformUI.getWorkbench().getMarkerHelpRegistry().getResolutions(marker);
						if (res.length > 0) {
							for (int i= 0; i < res.length; i++) {
								proposals.add(new MarkerResolutionProposal(res[i], marker));
							}
						}
					}
				}
			}
		}

		int length= viewer != null ? viewer.getSelectedRange().y : 0;
		context.initialize(offset, length, 0, null);
		collectAssists(context, proposals);
	}
	
	public static void collectCorrections(CorrectionContext context, ArrayList proposals) {
		ICorrectionProcessor[] processors= getCorrectionProcessors();
		for (int i= 0; i < processors.length; i++) {
			try {
				processors[i].process(context, proposals);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}
	
	public static void collectAssists(CorrectionContext context, ArrayList proposals) {
		IAssistProcessor[] processors= getAssistProcessors();
		for (int i= 0; i < processors.length; i++) {
			try {
				processors[i].process(context, proposals);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}	
	
	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}
}
