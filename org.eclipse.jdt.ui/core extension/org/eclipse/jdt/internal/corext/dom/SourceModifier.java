/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.ISourceModifier;
import org.eclipse.jdt.internal.corext.textmanipulation.Regions;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.Strings;


public abstract class SourceModifier implements ISourceModifier {
	
	private String fDestinationIndent;
	private int fSourceIndentLevel;
	private int fTabWidth;
	
	private static class CopyModifier extends SourceModifier {
		/* package */ void insertEdits(TextEdit root, List edits) {
			for (Iterator iter= edits.iterator(); iter.hasNext();) {
				root.add((TextEdit)iter.next());
			}
		}
	}
	
	private static class MoveModifier extends SourceModifier {
		/* package */ void insertEdits(TextEdit root, List edits) {
			while(edits.size() > 0) {
				SimpleTextEdit edit= (SimpleTextEdit)edits.remove(0);
				insert(root, edit, edits);
			}
		}
		private static void insert(TextEdit parent, SimpleTextEdit edit, List edits) {
			if (!parent.hasChildren()) {
				parent.add(edit);
				return;
			}
			TextRange editRange= edit.getTextRange();
			TextEdit[] children= parent.getChildren();
			// First dive down to find the right parent.
			for (int i= 0; i < children.length; i++) {
				TextEdit child= children[i];
				TextRange childRange= child.getTextRange();
				if (Regions.covers(childRange, editRange)) {
					insert(child, edit, edits);
					return;
				} else if (Regions.covers(editRange, childRange)) {
					parent.remove(i);
					edit.add(child);
				} else {
					TextRange intersect= Regions.intersect(editRange, childRange);
					if (intersect != null) {
						SimpleTextEdit[] splits= splitEdit(edit, editRange, intersect);
						insert(child, splits[0], edits);
						edits.add(splits[1]);
					}
				}
			}
			parent.add(edit);
		}
		
		private static SimpleTextEdit[] splitEdit(SimpleTextEdit edit, TextRange editRange, TextRange intersect) {
			if (editRange.getOffset() != intersect.getOffset()) {
				return splitIntersectRight(edit, editRange, intersect);
			} else {
				return splitIntersectLeft(edit, editRange, intersect);
			}
		}
		
		private static SimpleTextEdit[] splitIntersectRight(SimpleTextEdit edit, TextRange editRange, TextRange intersect) {
			SimpleTextEdit[] result= new SimpleTextEdit[2];
			result[0]= SimpleTextEdit.createDelete(
				intersect.getOffset(), intersect.getLength());
			result[1]= SimpleTextEdit.createReplace(
				editRange.getOffset(), intersect.getOffset() - editRange.getOffset(), edit.getText());
			return result;
		}
		
		private static SimpleTextEdit[] splitIntersectLeft(SimpleTextEdit edit, TextRange editRange, TextRange intersect) {
			SimpleTextEdit[] result= new SimpleTextEdit[2];
			result[0]= SimpleTextEdit.createReplace(intersect.getOffset(), intersect.getLength(), edit.getText());
			result[1]= SimpleTextEdit.createDelete(
				intersect.getOffset() + intersect.getLength(),
				editRange.getLength() - intersect.getLength());
			return result;
		}
	}
	
	public static SourceModifier createCopyModifier() {
		return new CopyModifier();
	}
	
	public static SourceModifier createMoveModifier() {
		return new MoveModifier();
	}
	
	public static SourceModifier createMoveModifier(int sourceIndentLevel, String destIndentString, int tabWidth) {
		SourceModifier result= new MoveModifier();
		result.initialize(sourceIndentLevel, destIndentString, tabWidth);
		return result;
	}
	
	public void initialize(int sourceIndentLevel, String destIndentString, int tabWidth) {
		fSourceIndentLevel= sourceIndentLevel;
		fDestinationIndent= destIndentString;
		fTabWidth= tabWidth;
	}
	public boolean isInitialized() {
		return fSourceIndentLevel != -1;
	}
	public void addEdits(String source, TextEdit root) {
		Assert.isTrue(isInitialized(), "CopyIndentedSourceEdit never initialized"); //$NON-NLS-1$
		List edits= createChangeIndentEdits(source);
		insertEdits(root, edits);
	}
	
	public ISourceModifier copy() {
		// We are state less
		return this;
	}
	
	private List createChangeIndentEdits(String source) {
		List result= new ArrayList();
		int destIndentLevel= Strings.computeIndent(fDestinationIndent, fTabWidth);
		if (destIndentLevel == fSourceIndentLevel) {
			return result;
		}
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(source);
			int nLines= tracker.getNumberOfLines();
			if (nLines == 1)
				return result;
			for (int i= 1; i < nLines; i++) {
				IRegion region= tracker.getLineInformation(i);
				int offset= region.getOffset();
				String line= source.substring(offset, offset + region.getLength());
				int length= Strings.computeIndentLength(line, fSourceIndentLevel, fTabWidth);
				if (length >= 0) {
					result.add(SimpleTextEdit.createReplace(offset, length, fDestinationIndent));
				} else {
					length= Strings.computeIndent(line, fTabWidth);
					result.add(SimpleTextEdit.createDelete(offset, length));
				}
			}
		} catch (BadLocationException cannotHappen) {
		}
		return result;
	}
	
	/* package */ abstract void insertEdits(TextEdit root, List edits);
}