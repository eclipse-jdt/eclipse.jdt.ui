package org.eclipse.jdt.ui.text.java;

import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;

/**
  */
public interface IJavadocCompletionProcessor {
	
	/**
	 * Flags used by <code>computeCompletionProposals</code>. Specifies that only
	 * proposals should be returned that match the case of the prefix in the
	 * code.
	 */
	int RESTRICT_TO_MATCHING_CASE= 1;


	/**
	 * Returns information about possible contexts based on the
	 * specified location within the compilation unit.
	 *
	 * @param cu the working copy of the compilation unit which is used to
	 * compute the possible contexts
	 * @param offset an offset within the compilation unit for which context information should be computed
	 * @return an array of context information objects or <code>null</code> if no context could be found
	 */
	IContextInformation[] computeContextInformation(ICompilationUnit cu, int offset);


	/**
	 * Returns a list of completion proposals based on the specified location
	 * within the compilation unit.
	 *
	 * @param cu the working copy of the compilation unit in which the
	 * completion request has been called.
	 * @param offset an offset within the compilation unit for which completion
	 * proposals should be computed
	 * @param the length of the current selection.
	 * @param flags settings for the code assist. Flags as defined in this
	 * interface, e.g. <code>RESTRICT_TO_MATCHING_CASE</code>.
	 * @return an array of completion proposals or <code>null</code> if no
	 * proposals could be found
     */
	IJavaCompletionProposal[] computeCompletionProposals(ICompilationUnit cu, int offset, int length, int flags);


	/**
	 * Returns the reason why this completion processor was unable to produce
	 * any completion proposals or context information.
	 *
	 * @return an error message or <code>null</code> if no error occurred
	 */
	String getErrorMessage();
}
