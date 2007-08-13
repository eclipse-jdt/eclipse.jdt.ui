/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class CodeFormatFix implements IFix {
	
	public static IFix createCleanUp(ICompilationUnit cu, boolean format, boolean removeTrailingWhitespacesAll, boolean removeTrailingWhitespacesIgnorEmpty, boolean correctIndentation) throws CoreException {
		if (!format && !removeTrailingWhitespacesAll && !removeTrailingWhitespacesIgnorEmpty && !correctIndentation)
			return null;
		
		if (format) {
			Map fomatterSettings= new HashMap(cu.getJavaProject().getOptions(true));
			
			String content= cu.getBuffer().getContents();
			Document document= new Document(content);
			
			TextEdit edit= CodeFormatterUtil.reformat(CodeFormatter.K_COMPILATION_UNIT, content, 0, TextUtilities.getDefaultLineDelimiter(document), fomatterSettings);
			if (edit == null || !edit.hasChildren())
				return null;
			
			String label= MultiFixMessages.CodeFormatFix_description;
			TextChange change= new CompilationUnitChange(label, cu);
			change.setEdit(edit);
			
			CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
			group.addTextEdit(edit);
			change.addTextEditGroup(group);
			
			return new CodeFormatFix(change, cu);
		} else if (removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty || correctIndentation) {
			try {
				CompilationUnitChange change= new CompilationUnitChange("", cu); //$NON-NLS-1$
				MultiTextEdit multiEdit= new MultiTextEdit();
				change.setEdit(multiEdit);
				
				if (correctIndentation && removeTrailingWhitespacesAll) {
					removeTrailingWhitespacesAll= false;
					removeTrailingWhitespacesIgnorEmpty= true;
				}
				
				Document document= new Document(cu.getBuffer().getContents());
				if (removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty) {
					String label= MultiFixMessages.CodeFormatFix_RemoveTrailingWhitespace_changeDescription;
					CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
					
					int lineCount= document.getNumberOfLines();
					for (int i= 0; i < lineCount; i++) {
						
						IRegion region= document.getLineInformation(i);
						if (region.getLength() == 0)
							continue;
						
						int lineStart= region.getOffset();
						int lineExclusiveEnd= lineStart + region.getLength();
						int j= getIndexOfRightMostNoneWhitspaceCharacter(lineStart, lineExclusiveEnd - 1, document);
						
						if (removeTrailingWhitespacesAll) {
							j++;
							if (j < lineExclusiveEnd) {
								DeleteEdit edit= new DeleteEdit(j, lineExclusiveEnd - j);
								multiEdit.addChild(edit);
								group.addTextEdit(edit);
							}
						} else if (removeTrailingWhitespacesIgnorEmpty) {
							if (j >= lineStart) {
								if (document.getChar(j) == '*' && getIndexOfRightMostNoneWhitspaceCharacter(lineStart, j - 1, document) < lineStart) {
									j++;
								}
								j++;
								if (j < lineExclusiveEnd) {
									DeleteEdit edit= new DeleteEdit(j, lineExclusiveEnd - j);
									multiEdit.addChild(edit);
									group.addTextEdit(edit);
								}
							}
						}
					}
					
					if (multiEdit.hasChildren()) {
						change.addTextEditGroup(group);
					}
				} 
					
				if (correctIndentation) {
					TextEdit edit= IndentAction.indent(document, cu.getJavaProject());
					if (edit != null) {
						
						String label= MultiFixMessages.CodeFormatFix_correctIndentation_changeGroupLabel;
						CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
						
						if (edit instanceof MultiTextEdit) {
							TextEdit[] children= ((MultiTextEdit)edit).getChildren();
							for (int i= 0; i < children.length; i++) {
								edit.removeChild(children[i]);
								multiEdit.addChild(children[i]);
								group.addTextEdit(children[i]);
							}
						} else {
							multiEdit.addChild(edit);
							group.addTextEdit(edit);
						}
						
						change.addTextEditGroup(group);
					}
				}
				
				if (!multiEdit.hasChildren())
					return null;
				
				return new CodeFormatFix(change, cu);
			} catch (BadLocationException x) {
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "", x)); //$NON-NLS-1$
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the index in document of a none whitespace character 
	 * between start (inclusive) and end (inclusive) such that if 
	 * more then one such character the index returned is the largest
	 * possible (closest to end). Returns start - 1 if no such character. 
	 * 
	 * @param start
	 * @param end
	 * @param document
	 * @return the position or start - 1
	 * @throws BadLocationException
	 */
	private static int getIndexOfRightMostNoneWhitspaceCharacter(int start, int end, Document document) throws BadLocationException {
		int position= end;
		while (position >= start && Character.isWhitespace(document.getChar(position)))
			position--;
		
		return position;
	}

	private final ICompilationUnit fCompilationUnit;
	private final TextChange fChange;
	
	public CodeFormatFix(TextChange change, ICompilationUnit compilationUnit) {
		fChange= change;
		fCompilationUnit= compilationUnit;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		return fChange;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getCompilationUnit()
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getDescription()
	 */
	public String getDescription() {
		return MultiFixMessages.CodeFormatFix_description;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getStatus()
	 */
	public IStatus getStatus() {
		return StatusInfo.OK_STATUS;
	}
	
}
