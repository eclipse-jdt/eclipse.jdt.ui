package org.eclipse.jdt.internal.ui.text.correction;


import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.Util;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;
import org.eclipse.jdt.internal.ui.text.java.IJavaCompletionProposal;

/**
  */
public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

	public static class CorrectionMarkerResolution implements IMarkerResolution {
	
		private CorrectionContext fCorrectionContext;
		private IJavaCompletionProposal fProposal;
	
		/**
		 * Constructor for CorrectionMarkerResolution.
		 */
		public CorrectionMarkerResolution(CorrectionContext context, IJavaCompletionProposal proposal) {
			fCorrectionContext= context;
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
				ICompilationUnit cu= fCorrectionContext.getCompilationUnit();
				
				IEditorPart part= EditorUtility.isOpenInEditor(cu);
				if (part == null) {
					part= EditorUtility.openInEditor(cu);
					if (part instanceof ITextEditor) {
						((ITextEditor) part).selectAndReveal(fCorrectionContext.getOffset(), fCorrectionContext.getLength());
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
		return JavaCorrectionProcessor.hasCorrections(id);
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
				if (input != null) { // only works with element open in editor
					CorrectionContext context= findCorrectionContext(input, marker);
					if (context != null) {
						ArrayList proposals= new ArrayList();
						JavaCorrectionProcessor.collectCorrections(context, proposals);
						int nProposals= proposals.size();
						IMarkerResolution[] resolutions= new IMarkerResolution[nProposals];
						for (int i= 0; i < nProposals; i++) {
							resolutions[i]= new CorrectionMarkerResolution(context, (IJavaCompletionProposal) proposals.get(i));
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
	
	private CorrectionContext findCorrectionContext(IEditorInput input, IMarker marker) throws JavaModelException {
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
							ICompilationUnit cu= getCompilationUnit(marker);
							return new CorrectionContext(JavaModelUtil.toWorkingCopy(cu), pos.getOffset(), pos.getLength(), annot.getId(), annot.getArguments());
						}
					}
				}
			}
		} else { // not open in editor
			ICompilationUnit cu= getCompilationUnit(marker);
			int id= marker.getAttribute(IJavaModelMarker.ID, -1);
			int start= marker.getAttribute(IMarker.CHAR_START, -1);
			int end= marker.getAttribute(IMarker.CHAR_END, -1);
			String[] arguments= Util.getProblemArgumentsFromMarker(marker.getAttribute(IJavaModelMarker.ARGUMENTS, "")); //$NON-NLS-1$
			if (cu != null && id != -1 && start != -1 && end != -1 && arguments != null) {
				return new CorrectionContext(JavaModelUtil.toWorkingCopy(cu), start, end - start, id, arguments);
			}			
		}
		return null;
	}


}
