package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.IErrorTickProvider;

public class AnnotationErrorTickProvider implements IErrorTickProvider {

	private IAnnotationModel fAnnotationModel;

	public AnnotationErrorTickProvider(IAnnotationModel model) {
		fAnnotationModel= model;
	}
	
	public AnnotationErrorTickProvider(ITextEditor editor) {
		this(editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput()));
	}	


	/**
	 * @see IErrorTickProvider#getErrorInfo(IJavaElement)
	 */
	public int getErrorInfo(IJavaElement element) {
		int info= 0;
		if (element instanceof ISourceReference) {
			try {
				ISourceRange range= ((ISourceReference)element).getSourceRange();
			
				Iterator iter= fAnnotationModel.getAnnotationIterator();
				while ((info != ERRORTICK_ERROR) && iter.hasNext()) {
					Annotation curr= (Annotation) iter.next();
					IMarker marker= isApplicable(curr, range);
					if (marker != null) {
						int priority= marker.getAttribute(IMarker.SEVERITY, -1);
						if (priority == IMarker.SEVERITY_WARNING) {
							info= ERRORTICK_WARNING;
						} else if (priority == IMarker.SEVERITY_ERROR) {
							info= ERRORTICK_ERROR;
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
		return info;
	}
		
	private IMarker isApplicable(Annotation annot, ISourceRange range) {
		try {
			if (annot instanceof MarkerAnnotation) {
				IMarker marker= ((MarkerAnnotation)annot).getMarker();
				if (marker.exists() && marker.isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)) {
					Position pos= fAnnotationModel.getPosition(annot);
					if (pos.overlapsWith(range.getOffset(), range.getLength())) {
						return marker;
					}
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}						
		return null;
	}
}

