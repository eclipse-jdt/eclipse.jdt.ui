package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.internal.corext.textmanipulation.CopySourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.Strings;

/**
 * Copy source that changes the indention of the copied range.
  */
public final class CopyIndentedSourceEdit extends CopySourceEdit {

	private String fDestinationIndent;
	private int fSourceIndentLevel;
	private int fTabWidth;

	public CopyIndentedSourceEdit(int offset, int length) {
		super(offset, length);
		initialize(0, "", 4);
	}

	public void initialize(int sourceIndentLevel, String destIndentString, int tabWidth) {
		fSourceIndentLevel= sourceIndentLevel;
		fDestinationIndent= destIndentString;
		fTabWidth= tabWidth;
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		TextRange range= getTextRange();
		CopyIndentedSourceEdit result= new CopyIndentedSourceEdit(range.getOffset(), range.getLength());
		result.initialize(fSourceIndentLevel, fDestinationIndent, fTabWidth);
		return result;
	}

	protected String computeContent(TextBuffer buffer) {
		String str= super.computeContent(buffer); 
		
		int destIndentLevel= Strings.computeIndent(fDestinationIndent, fTabWidth);
		if (destIndentLevel == fSourceIndentLevel) {
			return str;
		}
		return Strings.changeIndent(str, fSourceIndentLevel, fTabWidth, fDestinationIndent, buffer.getLineDelimiter());
	}
}
