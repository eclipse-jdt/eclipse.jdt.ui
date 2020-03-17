/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.refactoring.util.TextEditUtil;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;

public class CodeFormatFix implements ICleanUpFix {

	public static ICleanUpFix createCleanUp(ICompilationUnit cu, IRegion[] regions, boolean format, boolean removeTrailingWhitespacesAll, boolean removeTrailingWhitespacesIgnorEmpty, boolean correctIndentation) throws CoreException {
		if (!format && !removeTrailingWhitespacesAll && !removeTrailingWhitespacesIgnorEmpty && !correctIndentation)
			return null;

		ArrayList<CategorizedTextEditGroup> groups= new ArrayList<>();

		MultiTextEdit formatEdit= new MultiTextEdit();
		if (format) {
			Map<String, String> formatterSettings= FormatterProfileManager.getProjectSettings(cu.getJavaProject());

			String content= cu.getBuffer().getContents();
			Document document= new Document(content);
			String lineDelemiter= TextUtilities.getDefaultLineDelimiter(document);
			int kind = (JavaModelUtil.isModuleInfo(cu) ? CodeFormatter.K_MODULE_INFO : CodeFormatter.K_COMPILATION_UNIT) | CodeFormatter.F_INCLUDE_COMMENTS;

			TextEdit edit;
			if (regions == null) {
				edit= CodeFormatterUtil.reformat(kind, content, 0, lineDelemiter, formatterSettings);
			} else {
				if (regions.length == 0)
					return null;

				edit= CodeFormatterUtil.reformat(kind, content, regions, 0, lineDelemiter, formatterSettings);
			}
			if (edit != null && (!(edit instanceof MultiTextEdit) || edit.hasChildren())) {
				formatEdit.addChild(edit);
				if (!TextEditUtil.isPacked(formatEdit)) {
					formatEdit= TextEditUtil.flatten(formatEdit);
				}
				if (removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty) {
					// look for inserted javadoc comments that end with a space and remove trailing space
					Map<String, String> settings= DefaultCodeFormatterConstants.getJavaConventionsSettings();
					if (JavaCore.INSERT.equals(settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS))
							|| JavaCore.INSERT.equals(settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS))) {
						if (edit instanceof MultiTextEdit) {
							TextEdit[] edits= edit.getChildren();
							if (trimInsertedJavadocComments(edits)) {
								edit.removeChildren();
								edit.addChildren(edits);
							}
						} else if (edit instanceof ReplaceEdit) {
							TextEdit[] edits= new TextEdit[] { edit };
							if (trimInsertedJavadocComments(edits)) {
								formatEdit.removeChild(edit);
								formatEdit.addChild(edits[0]);
							}
						}
					}
				}
				String label= MultiFixMessages.CodeFormatFix_description;
				CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
				group.addTextEdit(edit);

				groups.add(group);
			}
		}


		MultiTextEdit otherEdit= new MultiTextEdit();
		if ((removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty || correctIndentation)) {
			try {
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

				// Don't apply correct indentation if already formatting all lines
				if (correctIndentation && (!format || regions != null)) {
					JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
					TextEdit edit= IndentAction.indent(document, cu.getJavaProject());
					if (edit != null) {

						String label= MultiFixMessages.CodeFormatFix_correctIndentation_changeGroupLabel;
						CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));

						if (edit instanceof MultiTextEdit) {
							for (TextEdit child : ((MultiTextEdit)edit).getChildren()) {
								edit.removeChild(child);
								if (!TextEditUtil.overlaps(formatEdit, child) && !TextEditUtil.overlaps(otherEdit, child)) {
									otherEdit.addChild(child);
									group.addTextEdit(child);
								}
							}
						} else {
							if (!TextEditUtil.overlaps(formatEdit, edit) && !TextEditUtil.overlaps(otherEdit, edit)) {
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

		for (CategorizedTextEditGroup group : groups) {
			change.addTextEditGroup(group);
		}

		return new CodeFormatFix(change);
	}

	private static boolean trimInsertedJavadocComments(TextEdit[] edits) {
		boolean modified= false;
		for (int i= 0; i < edits.length; ++i) {
			if (edits[i] instanceof ReplaceEdit) {
				ReplaceEdit replaceEdit= (ReplaceEdit)edits[i];
				String text= replaceEdit.getText();
				if (text.length() > 1 &&
						(text.charAt(0) == ' ' || text.charAt(0) == '\t') &&
						text.endsWith("* ")) { //$NON-NLS-1$
					edits[i]= new ReplaceEdit(replaceEdit.getOffset(), replaceEdit.getLength(), replaceEdit.getText().substring(1));
					modified= true;
				}
			}
		}
		return modified;
	}

	/**
	 * Returns the index in document of a none whitespace character between start (inclusive) and
	 * end (inclusive) such that if more then one such character the index returned is the largest
	 * possible (closest to end). Returns start - 1 if no such character.
	 *
	 * @param start the start
	 * @param end the end
	 * @param document the document
	 * @return the position or start - 1
	 * @exception BadLocationException if the offset is invalid in this document
	 */
	private static int getIndexOfRightMostNoneWhitspaceCharacter(int start, int end, IDocument document) throws BadLocationException {
		int position= end;
		while (position >= start && Character.isWhitespace(document.getChar(position)))
			position--;

		return position;
	}

	private final CompilationUnitChange fChange;

	public CodeFormatFix(CompilationUnitChange change) {
		fChange= change;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return fChange;
	}
}
