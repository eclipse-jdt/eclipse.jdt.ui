package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.core.Util;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;



public class JavaReconcilingStrategy implements IReconcilingStrategy {
	
	
	/**
	 * Internal problem requestor.
	 */
	static class ProblemRequestor implements IProblemRequestor {
		
		private CompilationUnitEditor fEditor;
		
		public ProblemRequestor(CompilationUnitEditor editor) {
			fEditor= editor;
		}
		
		private List fProblems= new ArrayList();
		
		/*
		 * @see IProblemRequestor#acceptProblem(int, int, int, int, String, int)
		 */
		public void acceptProblem(IProblem problem) {
			fProblems.add(problem);
		}
		
		public List getProblems() {
			return new ArrayList(fProblems);
		}
		
		public void init() {
			fProblems.clear();
			if (fEditor != null) {
				addMarkerProblems();
			}
		}
		
		private void addMarkerProblems() {
			IAnnotationModel model= fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
			Iterator iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				try {
					Annotation curr= (Annotation) iter.next();
					if (curr instanceof MarkerAnnotation) {
						IMarker marker= ((MarkerAnnotation)curr).getMarker();
						if (marker.exists() && marker.isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)) {
							fProblems.add(createProblemFromMarker(marker, model.getPosition(curr)));
						}
					}
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			}
		}
		
		private IProblem createProblemFromMarker(IMarker marker, Position currPosition) {
			String fileName= marker.getResource().getName();
			char[] originationFileName= new char[fileName.length()];
			fileName.getChars(0, fileName.length(), originationFileName, 0);

			int id= marker.getAttribute(IJavaModelMarker.ID, -1);
			String message= marker.getAttribute(IMarker.MESSAGE, "");
			
			String[] args= Util.getProblemArgumentsFromMarker(marker.getAttribute(IJavaModelMarker.ARGUMENTS, ""));
			
			int line= marker.getAttribute(IMarker.LINE_NUMBER, -1);
			
			int markerSeverity= marker.getAttribute(IMarker.SEVERITY, -1);
			int severity=  markerSeverity == IMarker.SEVERITY_ERROR ? ProblemSeverities.Error : ProblemSeverities.Warning;
			
			return new DefaultProblem(originationFileName, message, id, args, severity, currPosition.getOffset(), currPosition.getOffset() + currPosition.getLength() - 1, line);
		}
		

	};
	
	
	private ICompilationUnit fUnit;
	private IEditorPart fEditor;
	private CompilationUnitEditor fCompilationUnitEditor;
	private IWorkingCopyManager fManager;
	private ProblemRequestor fProblemRequestor;
	
	public JavaReconcilingStrategy(IEditorPart editor) {
		
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		
		if (fEditor instanceof CompilationUnitEditor)
			fCompilationUnitEditor= (CompilationUnitEditor) fEditor;
			
			
		fProblemRequestor = new ProblemRequestor(fCompilationUnitEditor);
	}
	
	private void reconcile() {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());		
		if (unit != null) {
			synchronized (unit) {
				try {
					
					// reconcile
					fProblemRequestor.init();
					unit.reconcile(fProblemRequestor);
					
					if (fCompilationUnitEditor != null) {
						
						fCompilationUnitEditor.setProblems(fProblemRequestor.getProblems());
						
						if (!WorkInProgressPreferencePage.synchronizeOutlineOnCursorMove()) {
							Shell shell= fCompilationUnitEditor.getSite().getShell();
							if (shell != null && !shell.isDisposed()) {
								shell.getDisplay().asyncExec(new Runnable() {
									public void run() {
										fCompilationUnitEditor.synchronizeOutlinePageSelection();
									}
								});
							}
						}
					}
						
				} catch (JavaModelException x) {
					// ignored
				}
			}
		}
	}
	
	/**
	 * @see IReconcilingStrategy#reconcile(IRegion)
	 */
	public void reconcile(IRegion partition) {
		reconcile();
	}
	
	/**
	 * @see IReconcilingStrategy#reconcile(DirtyRegion, IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile();
	}
	
	/**
	 * @see IReconcilingStrategy#setDocument(IDocument)
	 */
	public void setDocument(IDocument document) {
	}
}