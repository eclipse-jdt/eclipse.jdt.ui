package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IMember;


/**
 * Query object to let operations callback the actions.
 * Example is a callback to ask if a existing method should be replaced.
 */
public interface IRequestQuery {
	
	// return codes
	public static final int CANCEL= 0;
	public static final int NO= 1;
	public static final int YES= 2;
	public static final int YES_ALL= 3;
	
	/**
	 * Do the callback. Returns YES, NO, YES_ALL or CANCEL
	 */
	int doQuery(IMember member);
}
