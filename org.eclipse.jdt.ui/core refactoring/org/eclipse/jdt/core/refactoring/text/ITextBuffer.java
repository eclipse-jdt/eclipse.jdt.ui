/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.text;

/**
 * An interace to access the source of a compilation unit without knowing how the compilation 
 * unit is stored and without knowing how the compilation unit is best manipulated.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface ITextBuffer {
		
	/**
	 * Returns the number of characters in this text buffer.
	 *
	 * @return the number of characters in this text buffer
	 */
	public int getLength();
	
	/**
	 * Returns the character at the given offset in this text buffer.
	 *
	 * @param offset a text buffer offset
	 * @return the character at the offset
       * @exception  IndexOutOfBoundsException  if the <code>offset</code> 
       *  argument is negative or not less than the length of this text buffer.
       */
	public char getChar(int offset);
	
	/**
	 * Returns the whole content of the text buffer.
	 *
	 * @return the whole content of the text buffer
	 */
	public String getContent();
	 
	/**
	 * Returns length characters starting from the specified position.
	 *
	 * @return the characters specified by the given text region. Returns <code>
	 *  null</code> if text range is illegal
	 */
	public String getContent(int offset, int length);
	
	/**
	 * Returns the line delimiter used for the given line number. Returns <code>
	 * null</code> if the line number is out of range.
	 *
	 * @return the line delimiter used by the given line number or <code>null</code>
	 */
	public String getLineDelimiter(int line); 
	
	/**
	 * Returns the line for the given line number. If there isn't any line for
	 * the given line number, <code>null</code> is returned.
	 *
	 * @return the line for the given line number or <code>null</code>
	 */
	public String getLineContent(int line);
	
	/**
	 * Returns a description of the specified line. The line is described by its
	 * offset and its length excluding the line's delimiter. Returns <code>null</code>
	 * if the line doesn't exist.
	 *
	 * @param line the line of interest
	 * @return a line description or <code>null</code> if the given line doesn't
	 *  exist
	 */
	public ITextRegion getLineInformation(int line);
	
	/**
	 * Returns the line number that contains the given position. If there isn't any
	 * line containing the position, -1 is returned.
	 *
	 * @return the line number that contains the given offset or -1 if such line
	 *  doesn't exist
	 */ 
	public int getLineOfOffset(int offset);

	/**
	 * Returns the line that contains the given position. If there isn't any
	 * line that contains the position, <code>null</code> is returned. The returned 
	 * string is a copy and doesn't contain the line delimiter.
	 *
	 * @return the line that contains the given offset or <code>null</code> if line
	 *  doesn't exist
	 */ 
	public String getLineContentOfOffset(int offset);

	/**
	 * Returns a description of the line that encloses the given offset. The line is described 
	 * by its offset and its length excluding the line's delimiter. Returns <code>null</code>
	 * if the line doesn't exist.
	 *
	 * @param line the line of interest
	 * @return a line description or  <code>null</code> if the line doesn't exist 
	 */
	public ITextRegion getLineInformationOfOffset(int offset);
	
	/**
	 * Converts the text determined by the region [offset, length] into an array of lines. 
	 * The lines are copies of the original lines and don't contain any line delimiter 
	 * characters.
	 *
	 * @return the text converted into an array of strings. Returns <code>null</code> if the 
	 *  region lies outside the source. 
	 */
	public String[] convertIntoLines(int offset, int length);
	
	/**
	 * Subsitutes the given text for the specified text range. Returns <code>true</code>
	 * if replacing was possible. Otherwise <code>false</code> is returned.
	 *
	 * @param offset the document offset
	 * @param length the length of the specified range
	 * @param text the substitution text
       * @exception  IndexOutOfBoundsException  if the text range [offset, length] 
       *  is invalid.	 
	 */
	public void replace(int offset, int length, String text);	
}