package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.CopyTargetEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Copy target that changes the indention of the copied range.
  */
public final class CopyIndentedTargetEdit extends CopyTargetEdit {

	private String fDestinationIndent;
	private int fSourceIndentLevel;
	private int fTabWidth;

	public CopyIndentedTargetEdit(int destOffset, int sourceIndentLevel, String destIndentString, int tabWidth) {
		super(destOffset);
		fSourceIndentLevel= sourceIndentLevel;
		fDestinationIndent= destIndentString;
		fTabWidth= tabWidth;
	}


	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		return new CopyIndentedTargetEdit(getTextRange().getOffset(), fSourceIndentLevel, fDestinationIndent, fTabWidth);
	}

	protected String getSourceContent() {
		String str= super.getSourceContent(); 
		
		int destIndentLevel= Strings.computeIndent(fDestinationIndent, fTabWidth);
		if (destIndentLevel == fSourceIndentLevel) {
			return str;
		}
		return Strings.changeIndent(str, fSourceIndentLevel, fTabWidth, fDestinationIndent);
	}

	
}
