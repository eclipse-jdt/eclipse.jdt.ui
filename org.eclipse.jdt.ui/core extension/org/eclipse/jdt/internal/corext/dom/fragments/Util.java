package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;

/**
 * This class houses a collection of static methods which do not refer to,
 * or otherwise depend on, other classes in this package.  Each 
 * package-visible method is called by more than one other class in this
 * package.  Since they do not depend on other classes in this package, 
 * they could be moved to some less specialized package. */
class Util {
	static boolean rangeIncludesNonWhitespaceOutsideRange(SourceRange first, SourceRange second, IBuffer buffer) {
		if(!first.covers(second))
			return false;

		if(!isJustWhitespace(first.getOffset(), second.getOffset(), buffer))
			return true;
		if(!isJustWhitespace(second.getOffset() + second.getLength(), first.getOffset() + first.getLength(), buffer))				
			return true;
		return false;		
	}
	private static boolean isJustWhitespace(int start, int end, IBuffer buffer) {
		Assert.isTrue(start <= end);
		return 0 == buffer.getText(start, end - start).trim().length();
	}
}
