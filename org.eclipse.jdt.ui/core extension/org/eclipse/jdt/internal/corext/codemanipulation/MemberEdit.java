/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public final class MemberEdit extends SimpleTextEdit {
	
	public static final int INSERT_BEFORE= 0;			// fMember is sibling
	public static final int INSERT_AFTER= 1;				// fMember is sibling
	public static final int ADD_AT_BEGINNING= 2;		// fMember is parent
	public static final int ADD_AT_END= 3;				// fMember is parent
	public static final int REPLACE= 4;						// fMember is element to be replace
	
	private IJavaElement fMember;
	private int fInsertionKind;
	private String[] fSource;
	private int fTabWidth;
	private boolean fUseFormatter= false;
	private int fEmptyLinesBetweenMembers= 1;
	private boolean fAddLineSeparators= true;
	

	public MemberEdit(IJavaElement member, int insertionKind, String[] source, int tabWidth) {
		Assert.isNotNull(member);
		fMember= member;
		
		Assert.isTrue(insertionKind >= INSERT_BEFORE && fInsertionKind <= REPLACE);
		fInsertionKind= insertionKind;
		
		Assert.isNotNull(source);
		fSource= source;
		
		Assert.isTrue(tabWidth >= 0);
		fTabWidth= tabWidth;
	}
	
	public void setUseFormatter(boolean useFormatter) {
		fUseFormatter= useFormatter;
	}
	
	public void setAddLineSeparators(boolean addLineSeparators) {
		fAddLineSeparators= addLineSeparators;
	}
	
	public void setEmptyLinesBetweenMembers(int value) {
		fEmptyLinesBetweenMembers= value;
	}
	
	/* non Java-doc
	 * @see TextEdit#getCopy
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		MemberEdit result= new MemberEdit(fMember, fInsertionKind, fSource, fTabWidth);
		result.setUseFormatter(fUseFormatter);
		result.setEmptyLinesBetweenMembers(fEmptyLinesBetweenMembers);
		return result;
	}
	
	/* non Java-doc
	 * @see TextEdit#getModifiedElement
	 */
	public Object getModifiedElement() {
		if (fInsertionKind == ADD_AT_BEGINNING || fInsertionKind == ADD_AT_END || fInsertionKind == REPLACE)
			return fMember;
		return fMember.getParent();
	}
		
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {		
		StringBuffer sb= new StringBuffer();
		String lineDelimiter= buffer.getLineDelimiter();

		String s;
		IScanner scanner= null;
		ISourceRange range= getSourceRange();
		int start= range.getOffset();
		int end= start + range.getLength();
		int offset= -1;	// where to insert the lines
		int length= 0;		// length of range to be replaced
		
		switch (fInsertionKind) {
			
		case REPLACE:
			offset= start;
			length= range.getLength();
			sb.append(getSource(getLineIndent(buffer), lineDelimiter, false));
			break;
			
		case ADD_AT_BEGINNING:	// add text at end of container
			switch (fMember.getElementType()) {
			case IJavaElement.TYPE:
				// find first opening '{' at beginning of type
				scanner= ToolFactory.createScanner(true, true, false, false);
				scanner.setSource(buffer.getContent(start, range.getLength()).toCharArray());
				int emptyLines= 0;
				boolean sawClosingBracket= false;
				try {
					int token;
					while ((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
						if (token == ITerminalSymbols.TokenNameLBRACE)
							break;
					}
					offset= start + scanner.getCurrentTokenEndPosition() + 1;
					// count the number of empty lines
					while ((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
						switch (token) {
						case ITerminalSymbols.TokenNameWHITESPACE:
							s= extract(buffer, start, scanner);
							emptyLines+= countEmptyLines(s);
							continue;
						case ITerminalSymbols.TokenNameRBRACE:
							sawClosingBracket= true;
							break;
						default:
							break;
						}
						break;
					}
					
				} catch (InvalidInputException e) {
					throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
				}
				sb.append(lineDelimiter);
				sb.append(getSource(getLineIndent(buffer)+1, lineDelimiter, true));
				if (sawClosingBracket) {
//					if (emptyLines == 0)
//						fill(sb, 1, lineDelimiter);
				} else {
					if (emptyLines < fEmptyLinesBetweenMembers)
						fill(sb, fEmptyLinesBetweenMembers+1, lineDelimiter);
				}
				break;
			default:
				Assert.isTrue(false); // ADD_AT_BEGINNING implemented only for IJavaElement.TYPE
				return;
			}
			break;
			
		case ADD_AT_END:	// add text add end of container
			switch (fMember.getElementType()) {
			case IJavaElement.TYPE:
				// find last closing '}' at end of type
				scanner= ToolFactory.createScanner(true, true, false, false);
				scanner.setSource(buffer.getContent(start, range.getLength()).toCharArray());
				try {
					int pos= -1;
					int token;
					while ((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
						if (token == ITerminalSymbols.TokenNameRBRACE)
							pos= scanner.getCurrentTokenStartPosition();	// remember the starting position of all '}'
					}
					if (pos >= 0)
						offset= start+pos;	// the last '}'
				} catch (InvalidInputException e) {
					throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
				}
				sb.append(getSource(getLineIndent(buffer)+1, lineDelimiter, true));
				sb.append(lineDelimiter);
				break;

			case IJavaElement.COMPILATION_UNIT:
				fill(sb, fEmptyLinesBetweenMembers+1, lineDelimiter);
				sb.append(getSource(0, lineDelimiter, true));
				// insert at the very end of the file
				offset= buffer.getLength();
				break;
			default:
				Assert.isTrue(false); // ADD_AT_END implemented only for TYPE and COMPILATION_UNIT
				return;
			}
			break;
			
		case INSERT_AFTER:
			fill(sb, fEmptyLinesBetweenMembers+1, lineDelimiter);
			sb.append(getSource(getLineIndent(buffer), lineDelimiter, true));
			
			offset= end;
			break;
			
		case INSERT_BEFORE:
			sb.append(getSource(getLineIndent(buffer), lineDelimiter, true));
			fill(sb, fEmptyLinesBetweenMembers+1, lineDelimiter);
		
			int line= buffer.getLineOfOffset(range.getOffset());
			TextRegion region= buffer.getLineInformation(line);
			offset= region.getOffset();
			break;
		
		default:
			Assert.isTrue(false); // unknown insertion kind
			break;
		}
		
		Assert.isTrue(offset >= 0); // we better should have a valid insertion point

		setTextRange(new TextRange(offset, length));
		setText(sb.toString());
		
		super.connect(buffer);
	}
	
	private static String extract(TextBuffer buffer, int offset, IScanner scanner) {
		int start= scanner.getCurrentTokenStartPosition();
		int length= scanner.getCurrentTokenEndPosition() - start + 1;
		return buffer.getContent(offset+start, length);
	}
	
	private static int countEmptyLines(String line) {
		int emptyLines= 0;
		int l= line.length();
		for (int i= 0; i < l; i++) {
			char c= line.charAt(i);
			if (c == '\r') {
				if ((i < (l - 1)) && line.charAt(i+1) == '\n') {
					emptyLines++;
					i++;
				}
			} else if (c == '\n')
				emptyLines++;
		}
		return emptyLines;
	}

	// private helpers
	
	private ISourceRange getSourceRange() throws CoreException {
		return ((ISourceReference)fMember).getSourceRange();
	}
		
	/**
	 */
	private int getLineIndent(TextBuffer buffer) throws CoreException {
		int offset= getSourceRange().getOffset();
		int line= buffer.getLineOfOffset(offset);
		return buffer.getLineIndent(line, fTabWidth);
	}
	
	/**
	 * Prepares the text lines for insertion.
	 */
	private String getSource(int initialIndentationLevel, String lineDelimiter,
													boolean indentFirstLine) {
		
		StringBuffer buffer= new StringBuffer();
		int last= fSource.length-1;
		
		Strings.trimIndentation(fSource, fTabWidth);
		
		String indent= ""; //$NON-NLS-1$
		if (!fUseFormatter)
			indent= CodeFormatterUtil.createIndentString(initialIndentationLevel);
		for (int i= 0; i < fSource.length; i++) {
			if (! fUseFormatter) {
				if (i > 0 || (i == 0 && indentFirstLine))
					buffer.append(indent);
			}
			buffer.append(fSource[i]);
			if (i < last && fAddLineSeparators)
				buffer.append(lineDelimiter);
		}		

		if (fUseFormatter) {
			ICodeFormatter formatter= ToolFactory.createCodeFormatter();
			return formatter.format(buffer.toString(), initialIndentationLevel, null, lineDelimiter);
		}
		return buffer.toString();
	}
	
	/**
	 * Appends the given string count-times to the buffer.
	 * Used to insert empty lines between members and to add
	 * the identation to lines.
	 */
	private void fill(StringBuffer buffer, int count, String s) {
		for (int i= 0; i < count; i++)
			buffer.append(s);
	}
}

