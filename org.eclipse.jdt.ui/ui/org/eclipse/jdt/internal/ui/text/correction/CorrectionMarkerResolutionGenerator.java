package org.eclipse.jdt.internal.ui.text.correction;


import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

/**
  */
public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator {

	public static class CorrectionMarkerResolution implements IMarkerResolution {
	
		private IEditorInput fEditorInput;
		private ChangeCorrectionProposal fProposal;
	
		/**
		 * Constructor for CorrectionMarkerResolution.
		 */
		public CorrectionMarkerResolution(IEditorInput editorInput, ChangeCorrectionProposal proposal) {
			fEditorInput= editorInput;
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
			IDocument doc= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getDocument(fEditorInput);
			if (doc != null) {
				fProposal.apply(doc);
			}
		}
	
	}


	/**
	 * Constructor for CorrectionMarkerResolutionGenerator.
	 */
	public CorrectionMarkerResolutionGenerator() {
		super();
	}

	/* (non-Javadoc)
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		int id= marker.getAttribute(IJavaModelMarker.ID, -1);
		if (!JavaCorrectionProcessor.hasCorrections(id)) {
			return new IMarkerResolution[0]; 
		}
		
		try {
			ICompilationUnit cu= getCompilationUnit(marker);
			if (cu != null && JavaModelUtil.isEditable(cu)) {
				IEditorInput input= EditorUtility.getEditorInput(cu);
				if (input != null) { // only works with element open in editor
					ProblemPosition pos= findProblemPosition(input, marker);
					if (pos != null) {
						ArrayList proposals= new ArrayList();
						JavaCorrectionProcessor.collectCorrections(pos, proposals);
						int nProposals= proposals.size();
						IMarkerResolution[] resolutions= new IMarkerResolution[nProposals];
						for (int i= 0; i < nProposals; i++) {
							resolutions[i]= new CorrectionMarkerResolution(input, (ChangeCorrectionProposal) proposals.get(i));
						}
						return resolutions;
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return new IMarkerResolution[0]; 
	}

	private ICompilationUnit getCompilationUnit(IMarker marker) {
		IResource res= marker.getResource();
		if (res instanceof IFile && res.isAccessible()) {
			return JavaCore.createCompilationUnitFrom((IFile) res);
		}
		return null;
	}
	
	private ProblemPosition findProblemPosition(IEditorInput input, IMarker marker) throws JavaModelException {
		IAnnotationModel model= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel(input);
		if (model != null) {
			Iterator iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				Object curr= iter.next();
				if (curr instanceof JavaMarkerAnnotation) {
					JavaMarkerAnnotation annot= (JavaMarkerAnnotation) curr;
					
					if (marker.equals(annot.getMarker())) {
						Position pos= model.getPosition(annot);
						if (pos != null) {
							ICompilationUnit cu= getCompilationUnit(marker);
							return new ProblemPosition(pos, annot, EditorUtility.getWorkingCopy(cu));
						}
					}
				}
			}
		}
		return null;
	}
}
