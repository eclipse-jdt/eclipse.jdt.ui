package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

final class UpdateMethodReferenceEdit extends SimpleTextEdit {

		private String fOldName;
		
		public UpdateMethodReferenceEdit(int offset, int length, String newName, String oldName) {
			super(offset, length, newName);
			Assert.isNotNull(oldName);
			fOldName= oldName;			
		}
		
		private UpdateMethodReferenceEdit(TextRange range, String newName, String oldName) {
			super(range, newName);
			Assert.isNotNull(oldName);
			fOldName= oldName;			
		}

		/* non Java-doc
		 * @see TextEdit#copy0
		 */
		protected TextEdit copy0(TextEditCopier copier) {
			return new UpdateMethodReferenceEdit(getTextRange().copy(), getText(), fOldName);
		}

		/* non Java-doc
		 * @see TextEdit#connect(TextBufferEditor)
		 */
		public void connect(TextBuffer buffer) throws CoreException {
			TextRange range= getTextRange();
			String oldText= buffer.getContent(range.getOffset(), range.getLength());
			String oldMethodName= fOldName;
			int leftBracketIndex= oldText.indexOf("("); //$NON-NLS-1$
			if (leftBracketIndex == -1)
				return; 
			int offset= range.getOffset();
			int length= leftBracketIndex;
			oldText= oldText.substring(0, leftBracketIndex);
			int theDotIndex= oldText.lastIndexOf("."); //$NON-NLS-1$
			if (theDotIndex == -1) {
				setText(getText() + oldText.substring(oldMethodName.length()));
			} else {
				String subText= oldText.substring(theDotIndex);
				int oldNameIndex= subText.indexOf(oldMethodName) + theDotIndex;
				String ending= oldText.substring(theDotIndex, oldNameIndex) + getText();
				oldText= oldText.substring(0, oldNameIndex + oldMethodName.length());
				length= oldNameIndex + oldMethodName.length();
				setText(oldText.substring(0, theDotIndex) + ending);
			}			
			setTextRange(new TextRange(offset, length));
		}
	}
