package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.ResultCollector;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.viewsupport.JavaImageLabelProvider;

public class CompletionEvaluator {
	
	protected final static String[] fgTagProposals= {
		"@author",
		"@deprecated",
		"@exception",
		"@link",
		"@param",
		"@return",
		"@see", "@serial", "@serialData", "@serialField", "@since",
		"@throws",
		"@version"
	};
	
	protected final static String[] fgHTMLProposals= {
		"<code>", "</code>",
		"<br>",
		"<b>", "</b>",
		"<i>", "</i>",
		"<pre>", "</pre>"
	};	
	
	private ICompilationUnit fCompilationUnit;
	private IDocument fDocument;
	private int fCurrentPos;
	
	private JavaElementLabelProvider fLabelProvider;
	private List fResult;
	
	public CompletionEvaluator(ICompilationUnit cu, IDocument doc, int pos) {
		fCompilationUnit= cu;
		fDocument= doc;
		fCurrentPos= pos;
		fResult= new ArrayList();
		
		fLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT + JavaElementLabelProvider.SHOW_CONTAINER);
	}
	
	private static boolean isWordPart(char ch) {
		return Character.isJavaIdentifierPart(ch) || (ch == '#') || (ch == '.') || (ch == '/');
	}
			
	private static int findCharBeforeWord(IDocument doc, int lineBeginPos, int pos) {		
		int currPos= pos - 1;
		if (currPos > lineBeginPos) {
			try {
				while (currPos > lineBeginPos && isWordPart(doc.getChar(currPos))) {
					currPos--;
				}
				return currPos;
			} catch (BadLocationException e) {
			}
		}
		return pos;
	}
	
	private static int findLastWhitespace(IDocument doc, int lineBeginPos, int pos) {
		try {
			int currPos= pos - 1;
			while (currPos >= lineBeginPos && Character.isWhitespace(doc.getChar(currPos))) {
				currPos--;
			}
			return currPos + 1;
		} catch (BadLocationException e) {
		}
		return pos;	
	}
	
	private static int findClosingCharacter(IDocument doc, int pos, int end, char endChar) throws BadLocationException {
		char ch;
		int curr= pos;
		while (curr < end && (ch= doc.getChar(curr)) != endChar) {
			curr++;
		}
		if (curr < end) {
			return curr + 1;
		}
		return pos;
	}
	
	private static int findReplaceEndPos(IDocument doc, String newText, String oldText, int pos) {
		if (oldText.length() == 0 || oldText.equals(newText)) {
			return pos;
		}
		
		try {
			IRegion lineInfo= doc.getLineInformationOfOffset(pos);
			int end= lineInfo.getOffset() + lineInfo.getLength();
			
			if (newText.endsWith(">")) {
				// for html, search the tag end character
				return findClosingCharacter(doc, pos, end, '>');
			} else {
				char ch= 0;
				int pos1= pos;
				while (pos1 < end && Character.isJavaIdentifierPart(ch= doc.getChar(pos1))) {
					pos1++;
				}
				if (pos1 < end) {
					// for method references, search the closing bracket
					if ((ch == '(') && newText.endsWith(")")) {
						return findClosingCharacter(doc, pos1, end, ')');
					} 
					
				}
				return pos1;
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return pos;
	}
		
	public ICompletionProposal[] computeProposals() throws JavaModelException {
		evalProposals();
		ICompletionProposal[] res= new ICompletionProposal[fResult.size()];
		fResult.toArray(res);
		fResult.clear();
		return res;
	}	
	
	private void evalProposals() throws JavaModelException {
		try {
			
			IRegion info= fDocument.getLineInformationOfOffset(fCurrentPos);
			int lineBeginPos= info.getOffset();
	
			int word1Begin= findCharBeforeWord(fDocument, lineBeginPos, fCurrentPos);
			if (word1Begin == fCurrentPos) {
				return;
			}
			char firstChar= fDocument.getChar(word1Begin);
			if (firstChar == '@') {
				String prefix= fDocument.get(word1Begin, fCurrentPos - word1Begin);
				addProposals(prefix, fgTagProposals, JavaPluginImages.IMG_OBJS_JAVADOCTAG);
				return;
			} else if (firstChar == '<') {
				String prefix= fDocument.get(word1Begin, fCurrentPos - word1Begin);
				addProposals(prefix, fgHTMLProposals, JavaPluginImages.IMG_OBJS_HTMLTAG);
				return;
			} else if (!Character.isWhitespace(firstChar)) {
				return;
			}
			String prefix= fDocument.get(word1Begin + 1, fCurrentPos - word1Begin - 1);
				 
			// could be a composed java doc construct (@param, @see ...)
			int word2End= findLastWhitespace(fDocument, lineBeginPos, word1Begin);
			if (word2End != lineBeginPos) {
				// find the word before the prefix
				int word2Begin= findCharBeforeWord(fDocument, lineBeginPos, word2End);
				if (fDocument.getChar(word2Begin) == '@') {
					String tag= fDocument.get(word2Begin, word2End - word2Begin);
					if (addArgumentProposals(tag, prefix)) {
						return;
					}
				}
			}
			addAllTags(prefix);
		} catch (BadLocationException e) {
			// ignore
		}
	}

	private void addAllTags(String prefix) {
		String jdocPrefix= "@" + prefix;
		for (int i= 0; i < fgTagProposals.length; i++) {
			String curr= fgTagProposals[i];
			if (curr.startsWith(jdocPrefix)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), null));
			}		
		}
		String htmlPrefix= "<" + prefix;
		for (int i= 0; i < fgHTMLProposals.length; i++) {
			String curr= fgHTMLProposals[i];
			if (curr.startsWith(htmlPrefix)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_HTMLTAG), null));
			}		
		}
	}
	
	private void addProposals(String prefix, String[] choices, String imageName) {	
		for (int i= 0; i < choices.length; i++) {
			String curr= choices[i];
			if (curr.startsWith(prefix)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(imageName), null));
			}
		}
	}
	
	private void addProposals(String prefix, IJavaElement[] choices) {	
		for (int i= 0; i < choices.length; i++) {
			IJavaElement elem= choices[i];
			String curr= getReplaceString(elem);			
			if (curr.startsWith(prefix)) {
				String info= getProposalInfo(elem);
				fResult.add(createCompletion(curr, prefix, fLabelProvider.getText(elem), fLabelProvider.getImage(elem), info));
			}
		}
	}
	
	private String getProposalInfo(IJavaElement elem) {
		if (elem instanceof IMember) {
			try {
				return JavaDocAccess.getJavaDocText((IMember)elem);
			} catch (JavaModelException e) {
				JavaPlugin.getDefault().log(e.getStatus());
			}
		}
		return null;
	}
	
	
	private String getReplaceString(IJavaElement elem) {
		if (elem instanceof IMethod) {
			IMethod meth= (IMethod)elem;
			StringBuffer buf= new StringBuffer();
			buf.append(meth.getElementName());
			buf.append('(');
			String[] types= meth.getParameterTypes();
			int last= types.length - 1;
			for (int i= 0; i <= last; i++) {
				buf.append(Signature.toString(types[i]));
				if (i != last) {
					buf.append(", ");
				}
			}
			buf.append(')');
			return buf.toString();
		} else {
			return elem.getElementName();
		}
	}	
	
	/**
	 * Returns true if case is handeled
	 */
	private boolean addArgumentProposals(String tag, String argument) throws JavaModelException {	
		if ("@see".equals(tag) || "@link".equals(tag)) {
			evalSeeTag(argument);
			return true;
		} else if ("@param".equals(tag)) {
			IJavaElement elem= fCompilationUnit.getElementAt(fCurrentPos);
			if (elem instanceof IMethod) {
				String[] names= ((IMethod)elem).getParameterNames();
				addProposals(argument, names, JavaPluginImages.IMG_MISC_DEFAULT);
			}
			return true;
		} else if ("@throws".equals(tag) || "@exception".equals(tag)) {
			IJavaElement elem= fCompilationUnit.getElementAt(fCurrentPos);
			if (elem instanceof IMethod) {
				String[] exceptions= ((IMethod)elem).getExceptionTypes();
				for (int i= 0; i < exceptions.length; i++) {
					String curr= Signature.toString(exceptions[i]);
					if (curr.startsWith(argument)) {
						fResult.add(createCompletion(curr, argument, curr, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS), null));
					}
				}
			}
			return true;
		} else if ("@serialData".equals(tag)) {
			IJavaElement elem= fCompilationUnit.getElementAt(fCurrentPos);
			if (elem instanceof IField) {
				JavaImageLabelProvider iprovider= new JavaImageLabelProvider(0);
				String name= ((IField)elem).getElementName();
				fResult.add(createCompletion(name, argument, name, fLabelProvider.getImage(elem), null));
			}
			return true;
		}
		return false;
	}
	
	private void evalSeeTag(String arg) throws JavaModelException {
		int wordStart= fCurrentPos - arg.length();
		int pidx= arg.indexOf('#');
		if (pidx == -1) {
			ICompletionProposal[] completions= getTypeNameCompletion(wordStart);
			if (completions != null) {
				for (int i= 0; i < completions.length; i++) {
					fResult.add(completions[i]);
				}
			}
		} else {
			IType parent= null;
			if (pidx > 0) {
				// method or field 
				parent= getTypeNameResolve(wordStart, wordStart + pidx);
			} else {
				// '@see #foo'
				IJavaElement elem= fCompilationUnit.getElementAt(wordStart);
				if (elem != null) {
					parent= (IType)JavaModelUtility.getParent(elem, IJavaElement.TYPE);
				}
			}
				
			if (parent != null) {
				int nidx= arg.indexOf('(', pidx);
				if (nidx == -1) {
					nidx= arg.length();
				}
				String prefix= arg.substring(pidx + 1, nidx);			
			
				addProposals(prefix, parent.getMethods());
				addProposals(prefix, parent.getFields());
			}
		}
	}
	
	private ICompletionProposal[] getTypeNameCompletion(int wordStart) throws JavaModelException {
		ICompilationUnit preparedCU= createPreparedCU(wordStart, fCurrentPos);
		if (preparedCU != null) {
			ResultCollector collector= new ResultCollector();
			collector.reset(preparedCU.getJavaProject());
			try {
				preparedCU.codeComplete(fCurrentPos, collector);
			} finally {
				preparedCU.destroy();
			}
			return collector.getResults();
		}
		return null;
	}
		
	private IType getTypeNameResolve(int wordStart, int wordEnd) throws JavaModelException {
		ICompilationUnit preparedCU= createPreparedCU(wordStart, wordEnd);
		if (preparedCU != null) {
			try {
				IJavaElement[] elements= preparedCU.codeSelect(wordStart, wordEnd - wordStart);
				if (elements != null && elements.length == 1 && elements[0] instanceof IType) {
					return (IType) elements[0];
				}
			} finally {
				preparedCU.getBuffer().setContents(fCompilationUnit.getBuffer().getCharacters());
				preparedCU.destroy();
			}
		}
		return null;
	}
	
	private ICompilationUnit createPreparedCU(int wordStart, int wordEnd) throws JavaModelException {
		IJavaElement elem= fCompilationUnit.getElementAt(fCurrentPos);
		if (!(elem instanceof ISourceReference)) {
			return null;
		}
		int startpos= ((ISourceReference)elem).getSourceRange().getOffset();
		// 1GEYJ5Z: ITPJUI:WINNT - smoke120: code assist in @see tag can result data loss
		char[] content= (char[]) fCompilationUnit.getBuffer().getCharacters().clone();
		if (wordStart < content.length) {
			for (int i= startpos; i < wordStart; i++) {
				content[i]= ' ';
			}
		}
		
		if (wordEnd + 2 < content.length) {
			// XXX: workaround for 1GAVL08
			content[wordEnd]= ' ';
			content[wordEnd + 1]= 'x';
		}
				
		ICompilationUnit cu= fCompilationUnit;
		if (cu.isWorkingCopy()) {
			cu= (ICompilationUnit) cu.getOriginalElement();
		}
		ICompilationUnit newCU= (ICompilationUnit) cu.getWorkingCopy();
		newCU.getBuffer().setContents(content);
		return newCU;
	}


	private ICompletionProposal createCompletion(String newText, String oldText, String labelText, Image image, String info) {
		int startpos= fCurrentPos - oldText.length();
		int endPos= findReplaceEndPos(fDocument, newText, oldText, fCurrentPos);			
		return new CompletionProposal(newText, startpos, endPos - startpos, newText.length(), image, labelText, null, info);
	}

}