package org.eclipse.jdt.internal.corext.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;


public class JavaDocAccess {
	
	private static int findCommentEnd(IBuffer buffer, int start, int end) {
		for (int i= start; i < end; i++) {
			char ch= buffer.getChar(i);
			if (ch == '*' && (i + 1 < end) && buffer.getChar(i + 1) == '/') {
				return i + 2;
			}
		}
		return -1;
	}	

	/**
	 * Gets a reader for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 */
	public static SingleCharReader getJavaDoc(IMember member) throws JavaModelException {
		IBuffer buf= member.isBinary() ? member.getClassFile().getBuffer() : member.getCompilationUnit().getBuffer();
		if (buf == null) {
			// no source attachment found
			return null;
		}
		ISourceRange range= member.getSourceRange();
		int start= range.getOffset();
		int length= range.getLength();
		if (length >= 5 && buf.getChar(start) == '/'
			&& buf.getChar(start + 1) == '*' && buf.getChar(start + 2) == '*') {

			int end= findCommentEnd(buf, start + 3, start + length);
			if (end != -1) {
				return new JavaDocCommentReader(buf, start, end);
			}
		}
		return null;
	}
	
	/**
	 * Gets a text content for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 */
	public static String getJavaDocTextString(IMember member) throws JavaModelException {
		try {
			SingleCharReader rd= getJavaDoc(member);
			if (rd != null)
				return rd.getString();
				
		} catch (IOException e) {
			throw new JavaModelException(e, IStatus.ERROR);
		}
		
		return null;
	}	
		

}