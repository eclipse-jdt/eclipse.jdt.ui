package org.eclipse.jdt.ui;

/**
 * Interface to add to a Java content provider that
 * can return <code>IWorkingCopy</code> elements. 
 * 
 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider
 * @see org.eclipse.jdt.core.IWorkingCopy
 * 
 * @since 2.0 
 */
public interface IWorkingCopyProvider {
	/**
	 * Returns whether working copy elements are returned
	 * from this content provider.
	 * 
	 * @return whether working copy elements are provided.
	 */
	public boolean providesWorkingCopies();
}
