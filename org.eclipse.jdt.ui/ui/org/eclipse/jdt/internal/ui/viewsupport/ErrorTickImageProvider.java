package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ErrorTickImageProvider extends JavaElementImageProvider {
	
	private static final int ERRORTICK_WARNING= JavaElementImageDescriptor.WARNING;
	private static final int ERRORTICK_ERROR= JavaElementImageDescriptor.ERROR;	
	
	/*
	 * @see JavaElementImageProvider#computeExtraAdornmentFlags(Object)
	 */
	protected int computeExtraAdornmentFlags(Object obj) {
		try {
			if (obj instanceof IJavaElement) {
				IJavaElement element= (IJavaElement) obj;
				if (!element.exists()) {
					return 0;
				}
				
				int type= element.getElementType();
				switch (type) {
					case IJavaElement.JAVA_PROJECT:
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
						return getErrorTicksFromMarkers(element.getCorrespondingResource(), IResource.DEPTH_INFINITE, null);
					case IJavaElement.PACKAGE_FRAGMENT:
					case IJavaElement.CLASS_FILE:
						return getErrorTicksFromMarkers(element.getCorrespondingResource(), IResource.DEPTH_ONE, null);
					case IJavaElement.COMPILATION_UNIT:
					case IJavaElement.PACKAGE_DECLARATION:
					case IJavaElement.IMPORT_DECLARATION:
					case IJavaElement.IMPORT_CONTAINER:
					case IJavaElement.TYPE:
					case IJavaElement.INITIALIZER:
					case IJavaElement.METHOD:
					case IJavaElement.FIELD:
						ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(element, IJavaElement.COMPILATION_UNIT);
						if (cu != null && cu.exists()) {
							// I assume that only source elements in compilation unit can have markers
							ISourceRange range= ((ISourceReference)element).getSourceRange();
							// working copy: look at annotation model
							if (cu.isWorkingCopy()) {
								return getErrorTicksFromWorkingCopy((ICompilationUnit) cu.getOriginalElement(), range);
							}
							return getErrorTicksFromMarkers(cu.getCorrespondingResource(), IResource.DEPTH_ONE, range);
						}
					default:
				}
			} else if (obj instanceof IResource) {
				return getErrorTicksFromMarkers((IResource) obj, IResource.DEPTH_INFINITE, null);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return 0;
	}
	
	private int getErrorTicksFromMarkers(IResource res, int depth, ISourceRange range) throws CoreException {
		if (res == null) { // for elements in archives
			return 0;
		}
		int info= 0;
		
		IMarker[] markers= res.findMarkers(IMarker.PROBLEM, true, depth);
		if (markers != null) {
			for (int i= 0; i < markers.length && (info != ERRORTICK_ERROR); i++) {
				IMarker curr= markers[i];
				if (range == null || isMarkerInRange(curr, range)) {
					int priority= curr.getAttribute(IMarker.SEVERITY, -1);
					if (priority == IMarker.SEVERITY_WARNING) {
						info= ERRORTICK_WARNING;
					} else if (priority == IMarker.SEVERITY_ERROR) {
						info= ERRORTICK_ERROR;
					}
				}
			}			
		}
		return info;
	}
	
	private boolean isMarkerInRange(IMarker marker, ISourceRange range) throws CoreException {
		if (marker.isSubtypeOf(IMarker.TEXT)) {
			int pos= marker.getAttribute(IMarker.CHAR_START, -1);
			int offset= range.getOffset();
			return (offset <= pos && offset + range.getLength() > pos);
		}
		return false;
	}
	
	
	private int getErrorTicksFromWorkingCopy(ICompilationUnit original, ISourceRange range) throws CoreException {
		int info= 0;
		if (!original.exists()) {
			return 0;
		}
		
		FileEditorInput editorInput= new FileEditorInput((IFile) original.getCorrespondingResource());
		IAnnotationModel model= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel(editorInput);
		if (model != null) {
			Iterator iter= model.getAnnotationIterator();
			while ((info != ERRORTICK_ERROR) && iter.hasNext()) {
				Annotation curr= (Annotation) iter.next();
				IMarker marker= isAnnotationInRange(model, curr, range);
				if (marker != null) {
					int priority= marker.getAttribute(IMarker.SEVERITY, -1);
					if (priority == IMarker.SEVERITY_WARNING) {
						info= ERRORTICK_WARNING;
					} else if (priority == IMarker.SEVERITY_ERROR) {
						info= ERRORTICK_ERROR;
					}
				}
			}
		}
		return info;
	}
			
	private IMarker isAnnotationInRange(IAnnotationModel model, Annotation annot, ISourceRange range) throws CoreException {
			if (annot instanceof MarkerAnnotation) {
				IMarker marker= ((MarkerAnnotation)annot).getMarker();
				if (marker.exists() && marker.isSubtypeOf(IMarker.PROBLEM)) {
					Position pos= model.getPosition(annot);
					if (pos.overlapsWith(range.getOffset(), range.getLength())) {
						return marker;
					}
				}
			}
				
		return null;
	}	
	
		
}
