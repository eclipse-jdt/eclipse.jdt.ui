package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
  */
public interface ICorrectionProcessor {
	
	/**
	 * Collects corrections or code manipulations for the given context
	 * @param context Defines current compilation unit, position and -if at a problem location- problem ID.
	 * @param resultingCollections 
	 */
	
	void process(ICorrectionContext context, List resultingCollections) throws CoreException;
	
}
