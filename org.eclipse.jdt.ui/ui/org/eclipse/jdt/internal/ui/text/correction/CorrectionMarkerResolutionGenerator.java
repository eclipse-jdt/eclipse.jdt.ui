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
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

/**
  */
public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

	public static class CorrectionMarkerResolution extends WorkbenchMarkerResolution {

		private ICompilationUnit fCompilationUnit;
		private int fOffset;
		private int fLength;
		private IJavaCompletionProposal fProposal;
		private final IMarker fMarker;

		/**
		 * Constructor for CorrectionMarkerResolution.
		 * @param marker 
		 */
		public CorrectionMarkerResolution(ICompilationUnit cu, int offset, int length, IJavaCompletionProposal proposal, IMarker marker) {
			fCompilationUnit= cu;
			fOffset= offset;
			fLength= length;
			fProposal= proposal;
			fMarker= marker;
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

		/* (non-Javadoc)
		 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#canBeGroupedWith(org.eclipse.ui.views.markers.WorkbenchMarkerResolution)
		 */
		public boolean canBeGroupedWith(WorkbenchMarkerResolution resolution) {
			if (!(resolution instanceof CorrectionMarkerResolution))
				return false;
			
			String proposalId= getProposalId();
			if (proposalId == null)
				return false;
			
			CorrectionMarkerResolution cmr= ((CorrectionMarkerResolution)resolution);
			String otherProposalId= cmr.getProposalId();
			if (otherProposalId == null)
				return false;
			
			return otherProposalId.equals(proposalId);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#getUpdatedResolution()
		 */
		public WorkbenchMarkerResolution getUpdatedResolution() {
			String proposalId= getProposalId();
			if (proposalId == null)
				return null;

			IMarkerResolution[] resolutions= internalGetResolutions(fMarker);
			for (int i= 0; i < resolutions.length; i++) {
				CorrectionMarkerResolution resolution= (CorrectionMarkerResolution)resolutions[i];
				String otherProposalId= resolution.getProposalId();
				if (otherProposalId != null && otherProposalId.equals(proposalId)) {
					return resolution;
				}
			}
			return null;
		}

		public String getProposalId() {
			if (!(fProposal instanceof ICommandAccess))
				return null;
			
			ICommandAccess proposal= (ICommandAccess)fProposal;
			String commandId= proposal.getCommandId();
			if (commandId == null)
				return null;
			
			if (!commandId.startsWith(CorrectionCommandInstaller.COMMAND_PREFIX))
				return null;
			
			return commandId;
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
		return internalHasResolutions(marker);
	}

	/* (non-Javadoc)
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return internalGetResolutions(marker);
	}
	
	private static boolean internalHasResolutions(IMarker marker) {
		int id= marker.getAttribute(IJavaModelMarker.ID, -1);
		ICompilationUnit cu= getCompilationUnit(marker);
		return cu != null && JavaCorrectionProcessor.hasCorrections(cu, id);
	}
	
	private static IMarkerResolution[] internalGetResolutions(IMarker marker) {
		if (!internalHasResolutions(marker)) {
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
						if (!hasProblem (context.getASTRoot().getProblems(), location)) 
							return NO_RESOLUTIONS;
						
						ArrayList proposals= new ArrayList();
						JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
						Collections.sort(proposals, new CompletionProposalComparator());

						int nProposals= proposals.size();
						IMarkerResolution[] resolutions= new IMarkerResolution[nProposals];
						for (int i= 0; i < nProposals; i++) {
							resolutions[i]= new CorrectionMarkerResolution(context.getCompilationUnit(), location.getOffset(), location.getLength(), (IJavaCompletionProposal) proposals.get(i), marker);
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

	private static boolean hasProblem(IProblem[] problems, IProblemLocation location) {
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (problem.getID() == location.getProblemId() && problem.getSourceStart() == location.getOffset())
				return true;
		}
		return false;
	}

	private static ICompilationUnit getCompilationUnit(IMarker marker) {
		IResource res= marker.getResource();
		if (res instanceof IFile && res.isAccessible()) {
			IJavaElement element= JavaCore.create((IFile) res);
			if (element instanceof ICompilationUnit)
				return (ICompilationUnit) element;
		}
		return null;
	}

	private static IProblemLocation findProblemLocation(IEditorInput input, IMarker marker) {
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
