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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IMarker;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerHelpRegistry;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.spelling.WordQuickFixProcessor;


/**
 * PropertiesFileCorrectionProcessor.
 * 
 * @since 3.1
 */
public class PropertiesFileCorrectionProcessor implements IContentAssistProcessor {

	private static String fgErrorMessage;
	private static WordQuickFixProcessor fgWordQuickFixProcessor= new WordQuickFixProcessor();
	
	public static boolean isQuickFixableType(Annotation annotation) {
		return (annotation instanceof IJavaAnnotation || annotation instanceof SimpleMarkerAnnotation) && !annotation.isMarkedDeleted();
	}
	
	public static boolean hasCorrections(Annotation annotation) {
		if (annotation instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
			int problemId= javaAnnotation.getId();
			if (problemId != -1)
				return fgWordQuickFixProcessor.hasCorrections(null, problemId);
		}
		if (annotation instanceof SimpleMarkerAnnotation) {
			return hasCorrections(((SimpleMarkerAnnotation) annotation).getMarker());
		}
		return false;
	}
	
	private static boolean hasCorrections(IMarker marker) {
		if (marker == null || !marker.exists())
			return false;
			
		IMarkerHelpRegistry registry= IDE.getMarkerHelpRegistry();
		return registry != null && registry.hasResolutions(marker);
	}
	
	public static boolean hasAssists(IInvocationContext context) {
		return false;
	}	
	
	private PropertiesFileCorrectionAssistant fAssistant;

	
	/*
	 * Constructor for JavaCorrectionProcessor.
	 */
	public PropertiesFileCorrectionProcessor(PropertiesFileCorrectionAssistant assistant) {
		fAssistant= assistant;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		IEditorPart part= fAssistant.getEditor();
		
		int length= viewer != null ? viewer.getSelectedRange().y : 0;
		AssistContext context= new AssistContext(null, documentOffset, length);

		fgErrorMessage= null;
		ArrayList proposals= new ArrayList();
		
		IAnnotationModel model= null;
		IEditorInput input= part.getEditorInput();
		if (!(input instanceof IStorageEditorInput))
			return null;
		
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= null;
		try {
			path= ((IStorageEditorInput)input).getStorage().getFullPath();
			if (path == null)
				return null;
			manager.connect(path, null);
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			return null;
		}
		
		try {
			model= manager.getTextFileBuffer(path).getAnnotationModel();
			if (model != null) {
				processAnnotations(context, model, proposals);
			}
			if (proposals.isEmpty()) {
				proposals.add(new ChangeCorrectionProposal(CorrectionMessages.getString("NoCorrectionProposal.description"), null, 0, null));  //$NON-NLS-1$
			}
			
			ICompletionProposal[] res= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
			Arrays.sort(res, JavaCompletionProposalComparator.getInstance());
			return res;
		} finally {
			try {
				manager.disconnect(path, null);
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
	}
	
	private boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	
	private void processAnnotations(IInvocationContext context, IAnnotationModel model, ArrayList proposals) {
		int offset= context.getSelectionOffset();
		
		ArrayList problems= new ArrayList();
		Iterator iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= (Annotation) iter.next();
			if (isQuickFixableType(annotation)) {
				Position pos= model.getPosition(annotation);
				if (isAtPosition(offset, pos)) {
					processAnnotation(annotation, pos, problems, proposals);
				}
			}
		}	
		IProblemLocation[] problemLocations= (IProblemLocation[]) problems.toArray(new IProblemLocation[problems.size()]);
		collectCorrections(context, problemLocations, proposals);
	}
	
	private void processAnnotation(Annotation curr, Position pos, List problems, List proposals) {
		if (curr instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) curr;
			int problemId= javaAnnotation.getId();
			if (problemId != -1) {
				problems.add(new ProblemLocation(pos.getOffset(), pos.getLength(), javaAnnotation));
				return; // java problems all handled by the quick assist processors
			}
		}
		if (curr instanceof SimpleMarkerAnnotation) {
			IMarker marker= ((SimpleMarkerAnnotation) curr).getMarker();
			IMarkerResolution[] res= IDE.getMarkerHelpRegistry().getResolutions(marker);
			if (res.length > 0) {
				for (int i= 0; i < res.length; i++) {
					proposals.add(new MarkerResolutionProposal(res[i], marker));
				}
			}
		}
	}

	public static void collectCorrections(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
		try {
			IJavaCompletionProposal[] res= fgWordQuickFixProcessor.getCorrections(context, locations);
			if (res != null) {
				for (int k= 0; k < res.length; k++) {
					proposals.add(res[k]);
				}
			}
		} catch (Exception e) {
			fgErrorMessage= CorrectionMessages.getString("JavaCorrectionProcessor.error.quickfix.message"); //$NON-NLS-1$
			JavaPlugin.log(e);
		}
	}
	
	public static void collectAssists(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
		// no quick assists
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
		return fgErrorMessage;
	}
}
