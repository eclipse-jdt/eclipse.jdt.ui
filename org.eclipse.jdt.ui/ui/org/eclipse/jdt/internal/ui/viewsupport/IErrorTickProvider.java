package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Used by the JavaElementLabelProvider to evaluate the error tick
 * of a element.
 */
public interface IErrorTickProvider {
	
	public static final int ERRORTICK_WARNING= 1;
	public static final int ERRORTICK_ERROR= 2;	
	
	/**
	 * Evaluate the error tick state of a Java element.
	 * @returns ERRORTICK_ERROR or ERRORTICK_WARNING
	 */
	int getErrorInfo(IJavaElement element);	

}

