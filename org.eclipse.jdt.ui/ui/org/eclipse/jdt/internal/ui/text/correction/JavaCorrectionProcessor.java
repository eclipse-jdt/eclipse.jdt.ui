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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IMarker;

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
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import org.eclipse.ui.ide.IDE;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;


public class JavaCorrectionProcessor implements IContentAssistProcessor {

	private static final String QUICKFIX_PROCESSOR_CONTRIBUTION_ID= "quickFixProcessors"; //$NON-NLS-1$
	private static final String QUICKASSIST_PROCESSOR_CONTRIBUTION_ID= "quickAssistProcessors"; //$NON-NLS-1$
		
	private static ContributedProcessorDescriptor[] fContributedAssistProcessors= null;
	private static ContributedProcessorDescriptor[] fContributedCorrectionProcessors= null;
	private static String fErrorMessage;
	
	private static ContributedProcessorDescriptor[] getProcessorDescriptors(String contributionId) {
		IConfigurationElement[] elements= Platform.getExtensionRegistry().getConfigurationElementsFor(JavaUI.ID_PLUGIN, contributionId);
		ArrayList res= new ArrayList(elements.length);
		
		for (int i= 0; i < elements.length; i++) {
			ContributedProcessorDescriptor desc= new ContributedProcessorDescriptor(elements[i]);
			IStatus status= desc.checkSyntax();
			if (status.isOK()) {
				res.add(desc);
			} else {
				JavaPlugin.log(status);
			}
		}
		return (ContributedProcessorDescriptor[]) res.toArray(new ContributedProcessorDescriptor[res.size()]);		
	}
	
	private static ContributedProcessorDescriptor[] getCorrectionProcessors() {
		if (fContributedCorrectionProcessors == null) {
			fContributedCorrectionProcessors= getProcessorDescriptors(QUICKFIX_PROCESSOR_CONTRIBUTION_ID);
		}
		return fContributedCorrectionProcessors;
	}
	
	private static ContributedProcessorDescriptor[] getAssistProcessors() {
		if (fContributedAssistProcessors == null) {
			fContributedAssistProcessors= getProcessorDescriptors(QUICKASSIST_PROCESSOR_CONTRIBUTION_ID);
		}
		return fContributedAssistProcessors;
	}
		
	public static boolean hasCorrections(ICompilationUnit cu, int problemId) {
		ContributedProcessorDescriptor[] processors= getCorrectionProcessors();
		SafeHasCorrections collector= new SafeHasCorrections(cu, problemId);
		for (int i= 0; i < processors.length; i++) {
			collector.process(processors[i]);
			if (collector.hasCorrections()) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isQuickFixableType(Annotation annotation) {
		return (annotation instanceof IJavaAnnotation || annotation instanceof SimpleMarkerAnnotation) && !annotation.isMarkedDeleted();
	}
	
	
	public static boolean hasCorrections(Annotation annotation) {
		if (annotation instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
			int problemId= javaAnnotation.getId();
			if (problemId != -1) {
				ICompilationUnit cu= javaAnnotation.getCompilationUnit();
				if (cu != null) {
					return hasCorrections(cu, problemId);
				}
			}
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
		ContributedProcessorDescriptor[] processors= getAssistProcessors();
		SafeHasAssist collector= new SafeHasAssist(context);

		for (int i= 0; i < processors.length; i++) {
			collector.process(processors[i]);
			if (collector.hasAssists()) {
				return true;
			}
		}
		return false;
	}	
	
	private JavaCorrectionAssistant fAssistant;

	/*
	 * Constructor for JavaCorrectionProcessor.
	 */
	public JavaCorrectionProcessor(JavaCorrectionAssistant assistant) {
		fAssistant= assistant;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		IEditorPart part= fAssistant.getEditor();
		
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(part.getEditorInput());
		IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(part.getEditorInput());

		int length= viewer != null ? viewer.getSelectedRange().y : 0;
		AssistContext context= new AssistContext(cu, documentOffset, length);

		fErrorMessage= null;
		ArrayList proposals= new ArrayList();
		if (model != null) {
			processAnnotations(context, model, proposals);
		}
		if (proposals.isEmpty()) {
			proposals.add(new ChangeCorrectionProposal(CorrectionMessages.getString("NoCorrectionProposal.description"), null, 0, null));  //$NON-NLS-1$
		}
		
		ICompletionProposal[] res= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
		Arrays.sort(res, JavaCompletionProposalComparator.getInstance());
		return res;
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
		if (!fAssistant.isUpdatedOffset()) {
			collectAssists(context, problemLocations, proposals);
		}
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
	
	private static abstract class SafeCorrectionProcessorAccess implements ISafeRunnable {
		private MultiStatus fMulti= null;
		private ContributedProcessorDescriptor fDescriptor;
		
		public void process(ContributedProcessorDescriptor[] desc) {
			for (int i= 0; i < desc.length; i++) {
				fDescriptor= desc[i];
				Platform.run(this);
			}
		}
		
		public void process(ContributedProcessorDescriptor desc) {
			fDescriptor= desc;
			Platform.run(this);
		}
		
		public void run() throws Exception {
			safeRun(fDescriptor);
		}
		
		protected abstract void safeRun(ContributedProcessorDescriptor processor) throws Exception;

		public void handleException(Throwable exception) {
			if (fMulti == null) {
				fMulti= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, CorrectionMessages.getString("JavaCorrectionProcessor.error.status"), null); //$NON-NLS-1$
			}
			fMulti.merge(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, CorrectionMessages.getString("JavaCorrectionProcessor.error.status"), exception)); //$NON-NLS-1$
		}
		
		public IStatus getStatus() {
			if (fMulti == null) {
				return Status.OK_STATUS;
			}
			return fMulti;
		}
		
	}
	
	private static class SafeCorrectionCollector extends SafeCorrectionProcessorAccess {
		private final IInvocationContext fContext;
		private final IProblemLocation[] fLocations;
		private final ArrayList fProposals;

		public SafeCorrectionCollector(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
			fContext= context;
			fLocations= locations;
			fProposals= proposals;
		}
		
		public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
			IQuickFixProcessor curr= (IQuickFixProcessor) desc.getProcessor(fContext.getCompilationUnit());
			if (curr != null) {
				IJavaCompletionProposal[] res= curr.getCorrections(fContext, fLocations);
				if (res != null) {
					for (int k= 0; k < res.length; k++) {
						fProposals.add(res[k]);
					}
				}
			}
		}
	}
	
	private static class SafeAssistCollector extends SafeCorrectionProcessorAccess {
		private final IInvocationContext fContext;
		private final IProblemLocation[] fLocations;
		private final ArrayList fProposals;

		public SafeAssistCollector(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
			fContext= context;
			fLocations= locations;
			fProposals= proposals;
		}
		
		public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
			IQuickAssistProcessor curr= (IQuickAssistProcessor) desc.getProcessor(fContext.getCompilationUnit());
			if (curr != null) {
				IJavaCompletionProposal[] res= curr.getAssists(fContext, fLocations);
				if (res != null) {
					for (int k= 0; k < res.length; k++) {
						fProposals.add(res[k]);
					}
				}				
			}	
		}
	}
	
	private static class SafeHasAssist extends SafeCorrectionProcessorAccess {
		private final IInvocationContext fContext;
		private boolean fHasAssists;

		public SafeHasAssist(IInvocationContext context) {
			fContext= context;
			fHasAssists= false;
		}
		
		public boolean hasAssists() {
			return fHasAssists;
		}
		
		public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
			IQuickAssistProcessor processor= (IQuickAssistProcessor) desc.getProcessor(fContext.getCompilationUnit());
			if (processor != null && processor.hasAssists(fContext)) {
				fHasAssists= true;				
			}		
		}
	}
	
	private static class SafeHasCorrections extends SafeCorrectionProcessorAccess {
		private final ICompilationUnit fCu;
		private final int fProblemId;
		private boolean fHasCorrections;

		public SafeHasCorrections(ICompilationUnit cu, int problemId) {
			fCu= cu;
			fProblemId= problemId;
			fHasCorrections= false;
		}
		
		public boolean hasCorrections() {
			return fHasCorrections;
		}
		
		public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
			IQuickFixProcessor processor= (IQuickFixProcessor) desc.getProcessor(fCu);
			if (processor != null && processor.hasCorrections(fCu, fProblemId)) {
				fHasCorrections= true;
			}
		}
	}
	

	public static IStatus collectCorrections(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
		ContributedProcessorDescriptor[] processors= getCorrectionProcessors();
		SafeCorrectionCollector collector= new SafeCorrectionCollector(context, locations, proposals);
		collector.process(processors);
		
		IStatus status= collector.getStatus();
		if (!status.isOK()) {
			fErrorMessage= CorrectionMessages.getString("JavaCorrectionProcessor.error.quickfix.message"); //$NON-NLS-1$
		}
		return status;
	}
	
	public static IStatus collectAssists(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
		ContributedProcessorDescriptor[] processors= getAssistProcessors();
		SafeAssistCollector collector= new SafeAssistCollector(context, locations, proposals);
		collector.process(processors);

		IStatus status= collector.getStatus();
		if (!status.isOK()) {
			fErrorMessage= CorrectionMessages.getString("JavaCorrectionProcessor.error.quickassist.message"); //$NON-NLS-1$
		}
		return status;
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
		return fErrorMessage;
	}
}
