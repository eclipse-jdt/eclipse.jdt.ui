package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.IJavaElement;

public interface IErrorTickManager {
	
	public static final int ERRORTICK_WARNING= 1;
	public static final int ERRORTICK_ERROR= 2;	
	
	/**
	 * @returns an set (specified by an int) using the constants
	 * ERRORTICK_ERROR, ERRORTICK_WARNING
	 */
	int getErrorInfo(IJavaElement element);	

}

