/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;


public class JavaCorrectionProcessor implements IContentAssistProcessor {

	private static final String QUICKFIX_PROCESSOR_CONTRIBUTION_ID= "quickFixProcessors"; //$NON-NLS-1$
	private static final String QUICKASSIST_PROCESSOR_CONTRIBUTION_ID= "quickAssistProcessors"; //$NON-NLS-1$

	private static ContributedProcessorDescriptor[] fContributedAssistProcessors= null;
	private static ContributedProcessorDescriptor[] fContributedCorrectionProcessors= null;

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
	private String fErrorMessage;

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

		Annotation[] annotations= fAssistant.getAnnotationsAtOffset();
		
		fErrorMessage= null;
		
		ICompletionProposal[] res= null;
		if (model != null && annotations != null) {
			ArrayList proposals= new ArrayList(10);
			IStatus status= collectProposals(context, model, annotations, true, !fAssistant.isUpdatedOffset(), proposals);
			res= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
			if (!status.isOK()) {
				fErrorMessage= status.getMessage();
				JavaPlugin.log(status);
			}
		}
		
		if (res == null || res.length == 0) {
			return new ICompletionProposal[] { new ChangeCorrectionProposal(CorrectionMessages.NoCorrectionProposal_description, null, 0, null) };
		}
		if (res.length > 1) {
			Arrays.sort(res, new CompletionProposalComparator());
		}
		return res;
	}

	public static IStatus collectProposals(IInvocationContext context, IAnnotationModel model, Annotation[] annotations, boolean addQuickFixes, boolean addQuickAssists, Collection proposals) {
		ArrayList problems= new ArrayList();
		
		// collect problem locations and corrections from marker annotations
		for (int i= 0; i < annotations.length; i++) {
			Annotation curr= annotations[i];
			if (curr instanceof IJavaAnnotation) {
				ProblemLocation problemLocation= getProblemLocation((IJavaAnnotation) curr, model);
				if (problemLocation != null) {
					problems.add(problemLocation);
				}
			} else if (addQuickFixes && curr instanceof SimpleMarkerAnnotation) {
				// don't collect if annotation is already a java annotation
				collectMarkerProposals((SimpleMarkerAnnotation) curr, proposals);
			}
		}
		MultiStatus resStatus= null;
		
		IProblemLocation[] problemLocations= (IProblemLocation[]) problems.toArray(new IProblemLocation[problems.size()]);
		if (addQuickFixes) {
			IStatus status= collectCorrections(context, problemLocations, proposals);
			if (!status.isOK()) {
				resStatus= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.ERROR, CorrectionMessages.JavaCorrectionProcessor_error_quickfix_message, null);
				resStatus.add(status);
			}
		}
		if (addQuickAssists) {
			IStatus status= collectAssists(context, problemLocations, proposals);
			if (!status.isOK()) {
				if (resStatus == null) {
					resStatus= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.ERROR, CorrectionMessages.JavaCorrectionProcessor_error_quickassist_message, null);
				}
				resStatus.add(status);
			}
		}
		if (resStatus != null) {
			return resStatus;
		}
		return Status.OK_STATUS;
	}
	
	private static ProblemLocation getProblemLocation(IJavaAnnotation javaAnnotation, IAnnotationModel model) {
		int problemId= javaAnnotation.getId();
		if (problemId != -1) {
			Position pos= model.getPosition((Annotation) javaAnnotation);
			if (pos != null) {
				return new ProblemLocation(pos.getOffset(), pos.getLength(), javaAnnotation); // java problems all handled by the quick assist processors
			}
		}
		return null;
	}

	private static void collectMarkerProposals(SimpleMarkerAnnotation annotation, Collection proposals) {
		IMarker marker= annotation.getMarker();
		IMarkerResolution[] res= IDE.getMarkerHelpRegistry().getResolutions(marker);
		if (res.length > 0) {
			for (int i= 0; i < res.length; i++) {
				proposals.add(new MarkerResolutionProposal(res[i], marker));
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
				fMulti= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, CorrectionMessages.JavaCorrectionProcessor_error_status, null);
			}
			fMulti.merge(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, CorrectionMessages.JavaCorrectionProcessor_error_status, exception));
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
		private final Collection fProposals;

		public SafeCorrectionCollector(IInvocationContext context, IProblemLocation[] locations, Collection proposals) {
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
		private final Collection fProposals;

		public SafeAssistCollector(IInvocationContext context, IProblemLocation[] locations, Collection proposals) {
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


	public static IStatus collectCorrections(IInvocationContext context, IProblemLocation[] locations, Collection proposals) {
		ContributedProcessorDescriptor[] processors= getCorrectionProcessors();
		SafeCorrectionCollector collector= new SafeCorrectionCollector(context, locations, proposals);
		collector.process(processors);

		return collector.getStatus();
	}

	public static IStatus collectAssists(IInvocationContext context, IProblemLocation[] locations, Collection proposals) {
		ContributedProcessorDescriptor[] processors= getAssistProcessors();
		SafeAssistCollector collector= new SafeAssistCollector(context, locations, proposals);
		collector.process(processors);

		return collector.getStatus();
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
