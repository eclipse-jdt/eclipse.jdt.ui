package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeHierarchy;

public interface IImplementMethodQuery {
	
	/**
	 * Selects methods. Returns <code>null</code> if user pressed cancel.
	 */
	public IMethod[] select(IMethod[] elements, IMethod[] defaultSelected, ITypeHierarchy typeHierarchy);

}

