package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Used by the JavaElementLabelProvider to evaluate the error tick state of
 * an element.
 */
public class MarkerErrorTickProvider implements IErrorTickProvider {
	
	/*
	 * @see IErrorTickProvider#getErrorInfo
	 */
	public int getErrorInfo(Object obj) {
		int info= 0;
		try {
			IResource res= null;
			
			ISourceRange range= null;
			
			int depth= IResource.DEPTH_INFINITE;
			
			if (obj instanceof IJavaElement) {
				IJavaElement element= (IJavaElement) obj;
			
				int type= element.getElementType();
				
				if (type == IJavaElement.JAVA_PROJECT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT
					|| type == IJavaElement.PACKAGE_FRAGMENT || type == IJavaElement.CLASS_FILE || type == IJavaElement.COMPILATION_UNIT) {
					res= element.getCorrespondingResource();
					if (type == IJavaElement.PACKAGE_FRAGMENT) {
						depth= IResource.DEPTH_ONE;
					}
				} else if (element instanceof ISourceReference) {
					// I assume that only source elements in compilation unit can have markers
					ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(element, IJavaElement.COMPILATION_UNIT);
					if (cu != null) {
						res= element.getUnderlyingResource();
						range= ((ISourceReference)element).getSourceRange();
					}
				}
			} else if (obj instanceof IResource) {
				res= (IResource) obj;
			}
			
			if (res != null) {
				IMarker[] markers= res.findMarkers(IMarker.PROBLEM, true, depth);
				if (markers != null) {
					info= accumulateProblems(markers, info, range);
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}
		return info;
	}
	
	private int accumulateProblems(IMarker[] markers, int info, ISourceRange range) throws CoreException {
		if (markers != null) {	
			for (int i= 0; i < markers.length && (info != ERRORTICK_ERROR); i++) {
				IMarker curr= markers[i];
				if (range == null || isInRange(curr, range)) {
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
	
	private boolean isInRange(IMarker marker, ISourceRange range) throws CoreException {
		if (marker.isSubtypeOf(IMarker.TEXT)) {
			int pos= marker.getAttribute(IMarker.CHAR_START, -1);
			int offset= range.getOffset();
			return (offset <= pos && offset + range.getLength() > pos);
		}
		return false;
	}	

}

