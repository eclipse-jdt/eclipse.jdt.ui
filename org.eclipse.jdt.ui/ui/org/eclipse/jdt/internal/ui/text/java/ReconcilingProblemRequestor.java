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

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.core.Util;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorPreferencePage;



/**
 * Collects the problems found when reconciling a compilation unit and combines
 * those with the compilation problems.
 */
public class ReconcilingProblemRequestor implements IProblemRequestor {
	
	/**
	 * Tagging interface
	 */
	public static interface IMarkerProblem {
	};
	
	private class MarkerProblem extends  DefaultProblem implements IMarkerProblem {
		public MarkerProblem( char[] originatingFileName, String message, int id, String[] stringArguments,
						int severity, int startPosition, int endPosition, int line) {
			super(originatingFileName, message, id, stringArguments, severity, startPosition, endPosition, line);
		}
	};
	
	
	
	private ITextEditor fEditor;
	private List fProblems= new ArrayList();
	
	
	/**
	 * Creates a new <code>ReconcilingProblemRequestor</code>  for the given editor.
	 * @param editor the editor whose input is reconciled
	 */
	public ReconcilingProblemRequestor(ITextEditor editor) {
		fEditor= editor;
	}
	
	/*
	 * @see IProblemRequestor#acceptProblem(int, int, int, int, String, int)
	 */
	public void acceptProblem(IProblem problem) {
		fProblems.add(problem);
	}
	
	/**
	 * Returns the problems found for the editor's input.
	 */
	public IProblem[] getProblems() {
		return (IProblem[]) fProblems.toArray(new IProblem[fProblems.size()]);
	}
	
	/**
	 * Initializes the problem requestor. After that call it is ready to accept problems.
	 */
	public void init() {
		fProblems.clear();
		if (fEditor != null && JavaEditorPreferencePage.showProblems() && JavaEditorPreferencePage.showCompileTimeProblems())
				addMarkerProblems();
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
		
		return new MarkerProblem(originationFileName, message, id, args, severity, currPosition.getOffset(), currPosition.getOffset() + currPosition.getLength() - 1, line);
	}
}
