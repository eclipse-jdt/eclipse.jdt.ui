package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.CopyTargetEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.TextBuffer;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
  */
public class CopyIndentedTargetEdit extends CopyTargetEdit {

	private String fDestinationIndent;
	private int fSourceIndentLevel;
	private int fTabWidth;

	public CopyIndentedTargetEdit(int destOffset, int sourceIndentLevel, String destIndentString, int tabWidth) {
		super(destOffset);
		fSourceIndentLevel= sourceIndentLevel;
		fDestinationIndent= destIndentString;
		fTabWidth= tabWidth;
	}


	protected String getSourceContent() {
		String str= super.getSourceContent(); 
		
		int destIndentLevel= Strings.computeIndent(fDestinationIndent, fTabWidth);
		if (destIndentLevel == fSourceIndentLevel) {
			return str;
		}
		
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(str);
			int nLines= tracker.getNumberOfLines();
			if (nLines == 1) {
				return str;
			}
			
			StringBuffer buf= new StringBuffer();
			
			for (int i= 0; i < nLines; i++) {
				IRegion region= tracker.getLineInformation(i);
				int start= region.getOffset();
				int end= start + region.getLength();
				String line= str.substring(start, end);
				
				if (i > 0) { // no indent for first line (contained in the formatted string), no new line after last line
					buf.append(tracker.getLineDelimiter(i - 1));
					buf.append(fDestinationIndent); 
					buf.append(Strings.trimIndent(line, fSourceIndentLevel, fTabWidth));
				} else {
					buf.append(line);
				}
			}
			return buf.toString();
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return str;	
	}
	
}
