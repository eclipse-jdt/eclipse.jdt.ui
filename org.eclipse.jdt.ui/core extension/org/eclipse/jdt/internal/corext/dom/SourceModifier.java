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

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.ISourceModifier;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.util.Strings;


public abstract class SourceModifier implements ISourceModifier {
	
	private String fDestinationIndent;
	private int fSourceIndentLevel;
	private int fTabWidth;
	
	private static class CopyModifier extends SourceModifier {
		
		public CopyModifier(int sourceIndentLevel, String destinationIndent, int tabWidth) {
			super(sourceIndentLevel, destinationIndent, tabWidth);
		}
		
		/* package */ void insertEdits(TextEdit root, List edits) {
			for (Iterator iter= edits.iterator(); iter.hasNext();) {
				root.add((TextEdit)iter.next());
			}
		}

	}
	
	private static class MoveModifier extends SourceModifier {
		
		public MoveModifier(int sourceIndentLevel, String destinationIndent, int tabWidth) {
			super(sourceIndentLevel, destinationIndent, tabWidth);
		}
		
		/* package */ void insertEdits(TextEdit root, List edits) {
			while(edits.size() > 0) {
				TextEdit edit= (TextEdit)edits.remove(0);
				insert(root, edit, edits);
			}
		}
		private static void insert(TextEdit parent, TextEdit edit, List edits) {
			if (!parent.hasChildren()) {
				parent.add(edit);
				return;
			}
			TextEdit[] children= parent.getChildren();
			// First dive down to find the right parent.
			for (int i= 0; i < children.length; i++) {
				TextEdit child= children[i];
				if (child.covers(edit)) {
					insert(child, edit, edits);
					return;
				} else if (edit.covers(child)) {
					parent.remove(i);
					edit.add(child);
				} else {
					IRegion intersect= intersect(edit, child);
					if (intersect != null) {
						TextEdit[] splits= splitEdit(edit, intersect);
						insert(child, splits[0], edits);
						edits.add(splits[1]);
					}
				}
			}
			parent.add(edit);
		}
		
		public static IRegion intersect(TextEdit op1, TextEdit op2) {
			int offset1= op1.getOffset();
			int length1= op1.getLength();
			int end1= offset1 + length1 - 1;
			int offset2= op2.getOffset();
			if (end1 < offset2)
				return null;
			int length2= op2.getLength();
			int end2= offset2 + length2 - 1;
			if (end2 < offset1)
				return null;
			if (offset1 < offset2) {
				int end= Math.max(end1, end2);
				return new Region(offset2, end - offset2 + 1);
			} else {
				int end= Math.max(end1, end2);
				return new Region(offset1, end - offset1 + 1); 
			}
		}
		
		private static TextEdit[] splitEdit(TextEdit edit, IRegion intersect) {
			if (edit.getOffset() != intersect.getOffset()) {
				return splitIntersectRight(edit, intersect);
			} else {
				return splitIntersectLeft(edit, intersect);
			}
		}
		
		private static TextEdit[] splitIntersectRight(TextEdit edit, IRegion intersect) {
			TextEdit[] result= new TextEdit[2];
			result[0]= new DeleteEdit(
				intersect.getOffset(), intersect.getLength());
			result[1]= new ReplaceEdit(
				edit.getOffset(), intersect.getOffset() - edit.getOffset(), getText(edit));
			return result;
		}
		
		private static TextEdit[] splitIntersectLeft(TextEdit edit, IRegion intersect) {
			TextEdit[] result= new TextEdit[2];
			result[0]= new ReplaceEdit(intersect.getOffset(), intersect.getLength(), getText(edit));
			result[1]= new DeleteEdit(
				intersect.getOffset() + intersect.getLength(),
				edit.getLength() - intersect.getLength());
			return result;
		}
		
		private static String getText(TextEdit edit) {
			if (edit instanceof ReplaceEdit)
				return ((ReplaceEdit)edit).getText();
			if (edit instanceof DeleteEdit)
				return ""; //$NON-NLS-1$
			if (edit instanceof InsertEdit)
				return ((InsertEdit)edit).getText();
			Assert.isTrue(false, "Cannot happen"); //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
	}
	
	public SourceModifier(int sourceIndentLevel, String destinationIndent, int tabWidth) {
		super();
		fDestinationIndent= destinationIndent;
		fSourceIndentLevel= sourceIndentLevel;
		fTabWidth= tabWidth;
	}
	
	public static SourceModifier createCopyModifier(int sourceIndentLevel, String destIndentString, int tabWidth) {
		return new CopyModifier(sourceIndentLevel, destIndentString, tabWidth);
	}
		
	public static SourceModifier createMoveModifier(int sourceIndentLevel, String destIndentString, int tabWidth) {
		return new MoveModifier(sourceIndentLevel, destIndentString, tabWidth);
	}
	
	public void addEdits(String source, TextEdit root) {
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
					result.add(new ReplaceEdit(offset, length, fDestinationIndent));
				} else {
					length= Strings.computeIndent(line, fTabWidth);
					result.add(new DeleteEdit(offset, length));
				}
			}
		} catch (BadLocationException cannotHappen) {
		}
		return result;
	}
	
	/* package */ abstract void insertEdits(TextEdit root, List edits);
}