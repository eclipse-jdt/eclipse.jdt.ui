/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditUtil;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class CodeFormatFix implements IFix {
	
	public static IFix createCleanUp(ICompilationUnit cu, IRegion[] regions, boolean format, boolean removeTrailingWhitespacesAll, boolean removeTrailingWhitespacesIgnorEmpty, boolean correctIndentation) throws CoreException {
		if (!format && !removeTrailingWhitespacesAll && !removeTrailingWhitespacesIgnorEmpty && !correctIndentation)
			return null;
		
		ArrayList groups= new ArrayList();
		
		MultiTextEdit formatEdit= new MultiTextEdit();		
		if (format) {
			Map formatterSettings= new HashMap(cu.getJavaProject().getOptions(true));
			
			String content= cu.getBuffer().getContents();
			Document document= new Document(content);
			String lineDelemiter= TextUtilities.getDefaultLineDelimiter(document);
						
			TextEdit edit;
			if (regions == null) {
				edit= CodeFormatterUtil.reformat(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, content, 0, lineDelemiter, formatterSettings);
			} else {
				if (regions.length == 0)
					return null;
  
				IRegion[] adaptedRegions;
				if (isCommentFormattingEnabled(cu.getJavaProject())) {
					adaptedRegions= adaptRegions(regions, cu);
				} else {
					adaptedRegions= regions;
				}
				edit= CodeFormatterUtil.reformat(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, content, adaptedRegions, 0, lineDelemiter, formatterSettings);
			}
			if (edit != null && (!(edit instanceof MultiTextEdit) || edit.hasChildren())) {
				formatEdit.addChild(edit);
				if (!TextEditUtil.isPacked(formatEdit)) {
					formatEdit= TextEditUtil.flatten(formatEdit);
				}

				String label= MultiFixMessages.CodeFormatFix_description;
				CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
				group.addTextEdit(edit);

				groups.add(group);
			}
		}

		MultiTextEdit otherEdit= new MultiTextEdit();
		if ((removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty || correctIndentation) && (!format || regions != null)) {
			try {
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
								if (!TextEditUtil.overlaps(formatEdit, edit)) {
									otherEdit.addChild(edit);
									group.addTextEdit(edit);
								}
							}
						} else if (removeTrailingWhitespacesIgnorEmpty) {
							if (j >= lineStart) {
								if (document.getChar(j) == '*' && getIndexOfRightMostNoneWhitspaceCharacter(lineStart, j - 1, document) < lineStart) {
									j++;
								}
								j++;
								if (j < lineExclusiveEnd) {
									DeleteEdit edit= new DeleteEdit(j, lineExclusiveEnd - j);
									if (!TextEditUtil.overlaps(formatEdit, edit)) {
										otherEdit.addChild(edit);
										group.addTextEdit(edit);
									}
								}
							}
						}
					}
					
					if (otherEdit.hasChildren()) {
						groups.add(group);
					}
				} 
					
				if (correctIndentation) {
					JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
					TextEdit edit= IndentAction.indent(document, cu.getJavaProject());
					if (edit != null) {
						
						String label= MultiFixMessages.CodeFormatFix_correctIndentation_changeGroupLabel;
						CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
						
						if (edit instanceof MultiTextEdit) {
							TextEdit[] children= ((MultiTextEdit)edit).getChildren();
							for (int i= 0; i < children.length; i++) {
								TextEdit child= children[i];
								edit.removeChild(child);
								if (!TextEditUtil.overlaps(formatEdit, child)) {
									otherEdit.addChild(child);
									group.addTextEdit(child);
								}
							}
						} else {
							if (!TextEditUtil.overlaps(formatEdit, edit)) {
								otherEdit.addChild(edit);
								group.addTextEdit(edit);
							}
						}
						
						groups.add(group);
					}
				}
				
			} catch (BadLocationException x) {
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "", x)); //$NON-NLS-1$
			}
		}
		
		TextEdit resultEdit= TextEditUtil.merge(formatEdit, otherEdit);
		if (!resultEdit.hasChildren())
			return null;
		
		CompilationUnitChange change= new CompilationUnitChange("", cu); //$NON-NLS-1$
		change.setEdit(resultEdit);

		for (int i= 0, size= groups.size(); i < size; i++) {
			TextEditGroup group= (TextEditGroup) groups.get(i);
			change.addTextEditGroup(group);
		}

		return new CodeFormatFix(change);
	}

	/**
	 * Adapt regions: If a change is within a comment then the complete comment region needs to be formatted.
	 * 
	 * @param changedRegions the change regions
	 * @param unit the compilation unit containing the regions
	 * @return changed regions adapted to the comment regions size
	 * @throws JavaModelException
	 */
	private static IRegion[] adaptRegions(IRegion[] changedRegions, ICompilationUnit unit) throws JavaModelException {
		Document document= new Document(unit.getBuffer().getContents());
		JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
		try {
			ArrayList result= new ArrayList();
			ITypedRegion[] typedRegions= TextUtilities.computePartitioning(document, IJavaPartitions.JAVA_PARTITIONING, 0, document.getLength(), false);
			
			int typedRegionIndex= getNextCommentRegion(typedRegions, 0);
			
			int i= 0;
			while (i < changedRegions.length) {
				if (typedRegionIndex == -1) {
					result.add(changedRegions[i]);
					i++;
				} else {
					ITypedRegion commentRegion= typedRegions[typedRegionIndex];

					while (changedRegions[i].getOffset() + changedRegions[i].getLength() < commentRegion.getOffset()) {
						result.add(changedRegions[i]);
						i++;
						if (i >= changedRegions.length)
							return (IRegion[]) result.toArray(new IRegion[result.size()]);
					}
					
					int commentRegionEnd= commentRegion.getOffset() + commentRegion.getLength();
					if (changedRegions[i].getOffset() < commentRegionEnd) {
						int regionStart= Math.min(changedRegions[i].getOffset(), commentRegion.getOffset());

						i++;
						while (i < changedRegions.length && changedRegions[i].getOffset() < commentRegionEnd) {
							i++;
						}
						i--;
						
						int regionEnd= Math.max(changedRegions[i].getOffset() + changedRegions[i].getLength(), commentRegionEnd);
						result.add(new Region(regionStart, regionEnd - regionStart));
						i++;
					}

					typedRegionIndex= getNextCommentRegion(typedRegions, typedRegionIndex + 1);
				}
			}

			return (IRegion[]) result.toArray(new IRegion[result.size()]);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			return changedRegions;
		}
	}

	private static int getNextCommentRegion(ITypedRegion[] typedRegions, int index) {
		while (index < typedRegions.length) {
			ITypedRegion region= typedRegions[index];
			String type= region.getType();

			if (IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type)) {
				return index;
			} else if (IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(type)) {
				return index;
			} else if (IJavaPartitions.JAVA_DOC.equals(type)) {
				return index;
			}
			index++;
		}
		return -1;
	}

	private static boolean isCommentFormattingEnabled(IJavaProject javaProject) {
		HashMap preferences= new HashMap(javaProject.getOptions(true));

		if (DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT)))
			return true;

		if (DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT)))
			return true;

		if (DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT)))
			return true;

		return false;
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

	private final CompilationUnitChange fChange;
	
	public CodeFormatFix(CompilationUnitChange change) {
		fChange= change;
	}

	/**
	 * {@inheritDoc}
	 */
	public CompilationUnitChange createChange() throws CoreException {
		return fChange;
	}
}
