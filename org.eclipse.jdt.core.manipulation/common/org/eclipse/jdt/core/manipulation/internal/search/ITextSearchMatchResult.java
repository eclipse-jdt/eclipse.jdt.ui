package org.eclipse.jdt.core.manipulation.internal.search;

import org.eclipse.core.resources.IFile;

/**
 * A {@link ITextSearchMatchResult} gives access to a pattern match found by the {@link ITextSearchRunner}.
 * <p>
 * Please note that <code>{@link ITextSearchMatchResult}</code> objects <b>do not
 * </b> have value semantic. The state of the object might change over
 * time especially since objects are reused for different call backs. Clients shall not keep a reference to
 * a {@link ITextSearchMatchResult} element.
 * </p>
 * <p>
 * This class should only be implemented by implementors of a {@link ITextSearchRunner}.
 * </p>
 * @since 3.2
 */
public interface ITextSearchMatchResult {

	/**
	 * Returns the file the match was found in.
	 *
	 * @return the file the match was found.
	 */
	public IFile getFile();

	/**
	 * Returns the offset of this search match.
	 *
	 * @return the offset of this search match
	 */
	public int getMatchOffset();

	/**
	 * Returns the length of this search match.
	 *
	 * @return the length of this search match
	 */
	public int getMatchLength();

	/**
	 * Returns the length of this file's content.
	 *
	 * @return the length of this file's content.
	 */
	public int getFileContentLength();

	/**
	 * Returns a character of the file's content at the given offset
	 *
	 * @param offset the offset
	 * @return the character at the given offset
	 * @throws IndexOutOfBoundsException an {@link IndexOutOfBoundsException} is
	 * thrown when the <code>offset</code> is negative or not less than the file content's length.
	 */
	public char getFileContentChar(int offset);

	/**
	 * Returns the file's content at the given offsets.
	 *
	 * @param offset the offset of the requested content
	 * 	@param length the of the requested content
	 * @return the substring of the file's content
	 * @throws IndexOutOfBoundsException an {@link IndexOutOfBoundsException} is
	 * thrown when the <code>offset</code> or the <code>length</code> are negative
	 * or when <code>offset + length</code> is not less than the file content's length.
	 */
	public String getFileContent(int offset, int length);

}
