/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jface.text.Position;

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModel;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

class NLSSearchResultRequestor extends SearchRequestor {
	/*
	 * Matches are added to fResult. Element (group key) is IJavaElement or FileEntry.
	 */

	private static final StringMatcher fgGetClassNameMatcher= new StringMatcher("*.class.getName()*", false, false);  //$NON-NLS-1$

	private NLSSearchResult fResult;
	private IFile fPropertiesFile;
	private Properties fProperties;
	private HashSet fUsedPropertyNames;

	public NLSSearchResultRequestor(IFile propertiesFile, NLSSearchResult result) {
		fPropertiesFile= propertiesFile;
		fResult= result;
	}

	/*
	 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
	 */
	public void beginReporting() {
		loadProperties();
		fUsedPropertyNames= new HashSet(fProperties.size());
	}
	
	/*
	 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
	 */
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		int offset= match.getOffset();
		int length= match.getLength();
		if (offset == -1 || length == -1)
			return;
		
		if (! (match.getElement() instanceof IJavaElement))
			return;
		IJavaElement javaElement= (IJavaElement) match.getElement();
		
		// ignore matches in import declarations:
		if (javaElement.getElementType() == IJavaElement.IMPORT_DECLARATION)
			return;
		if (javaElement.getElementType() == IJavaElement.CLASS_FILE)
			return; //matches in import statements of class files
		if (javaElement.getElementType() == IJavaElement.TYPE)
			return; //classes extending the accessor class and workaround for bug 61286
		
		// heuristic: ignore matches in resource bundle name field:
		if (javaElement.getElementType() == IJavaElement.FIELD) {
			IField field= (IField) javaElement;
			String source= field.getSource();
			if (source != null && fgGetClassNameMatcher.match(source))
				return;
		}
		
		// found reference to NLS Wrapper - now check if the key is there:
		Position mutableKeyPosition= new Position(offset, length);
		//TODO: What to do if argument string not found? Currently adds a match with type name.
		String key= findKey(mutableKeyPosition, offset, javaElement);
		if (key != null && isKeyDefined(key))
			return;

		fResult.addMatch(new Match(javaElement, mutableKeyPosition.getOffset(), mutableKeyPosition.getLength()));
	}

	public void reportUnusedPropertyNames(IProgressMonitor pm) {
		//Don't use endReporting() for long running operation.
		pm.beginTask("", fProperties.size()); //$NON-NLS-1$
		boolean hasUnused= false;		
		pm.setTaskName(NLSSearchMessages.getString("NLSSearchResultRequestor.searching")); //$NON-NLS-1$
		String message= NLSSearchMessages.getFormattedString("NLSSearchResultCollector.unusedKeys", fPropertiesFile.getName()); //$NON-NLS-1$
		FileEntry groupElement= new FileEntry(fPropertiesFile, message);
		
		for (Enumeration enumeration= fProperties.propertyNames(); enumeration.hasMoreElements();) {
			String propertyName= (String) enumeration.nextElement();
			if (!fUsedPropertyNames.contains(propertyName)) {
				addMatch(groupElement, propertyName);
				hasUnused= true;
			}
			pm.worked(1);
		}
		if (hasUnused)
			fResult.setUnusedGroup(groupElement);
		pm.done();
	}

	private void addMatch(FileEntry groupElement, String propertyName) {
		/* 
		 * TODO (bug 63794): Should read in .properties file with our own reader and not
		 * with Properties.load(InputStream) . Then, we can remember start position and
		 * original version (not interpreting escape characters) for each property.
		 * 
		 * The current workaround is to escape the key again before searching in the
		 * .properties file. However, this can fail if the key is escaped in a different
		 * manner than what PropertyFileDocumentModel.unwindEscapeChars(.) produces.
		 */
		String escapedPropertyName= PropertyFileDocumentModel.unwindEscapeChars(propertyName);
		int start= findPropertyNameStartPosition(escapedPropertyName);
		int length;
		if (start == -1) { // not found -> report at beginning
			start= 0;
			length= 0;
		} else {
			length= escapedPropertyName.length();
		}
		fResult.addMatch(new Match(groupElement, start, length));
	}

	/**
	 * Checks if the key is defined in the property file
	 * and adds it to the list of used properties.
	 */
	private boolean isKeyDefined(String key) {
		if (key == null)
			return true; // Parse error - don't check key

		if (key != null && fProperties.getProperty(key) != null) {
			fUsedPropertyNames.add(key);
			return true;
		}
		return false;
	}
	
	/**
	 * Finds the key defined by the given match. The assumption is that
	 * the key is the first argument and it is a string i.e. quoted ("...").
	 * 
	 * @param keyPositionResult reference parameter: will be filled with the position of the found key
	 * @param typeNameStart start offset of search result
	 * @param enclosingElement enclosing java element
	 * @return a string denoting the key, null if no key can be found
	 */
	private String findKey(Position keyPositionResult, int typeNameStart, IJavaElement enclosingElement) throws CoreException {
		if (enclosingElement instanceof ISourceReference) {
			int sourceRangeOffset= ((ISourceReference) enclosingElement).getSourceRange().getOffset();
			String source= ((ISourceReference) enclosingElement).getSource();
			if (source == null)
				return null; //e.g. a class file without source
			source= source.substring(typeNameStart - sourceRangeOffset);
			
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(source.toCharArray());
			
			try {
				int tok= scanner.getNextToken();
				// skip type and method names:
				while (tok != ITerminalSymbols.TokenNameEOF && 
						(tok == ITerminalSymbols.TokenNameIdentifier || tok == ITerminalSymbols.TokenNameDOT)) {
					tok= scanner.getNextToken();
				}
				// next must be '('
				if (tok == ITerminalSymbols.TokenNameEOF || tok != ITerminalSymbols.TokenNameLPAREN)
					return null;
				tok= scanner.getNextToken();
				// next must be key string:
				if (tok == ITerminalSymbols.TokenNameEOF || tok != ITerminalSymbols.TokenNameStringLiteral)
					return null;
				// found it:
				int keyStart= scanner.getCurrentTokenStartPosition() + 1;
				int keyEnd= scanner.getCurrentTokenEndPosition();
				keyPositionResult.setOffset(typeNameStart + keyStart);
				keyPositionResult.setLength(keyEnd - keyStart);
				return source.substring(keyStart, keyEnd);
			} catch (InvalidInputException e) {
				return null;
			}
		}
		return null;
	}
	
	/**
	 * Finds the start position in the property file. We assume that
	 * the key is the first match on a line.
	 * 
	 * @return	the start position of the property name in the file, -1 if not found
	 */
	private int findPropertyNameStartPosition(String propertyName) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19319
		InputStream stream= null;
		LineReader lineReader= null;
		String encoding;
		try {
			encoding= fPropertiesFile.getCharset();
		} catch (CoreException e1) {
			encoding= "ISO-8859-1";  //$NON-NLS-1$
		}
		try {
			stream= fPropertiesFile.getContents();
			lineReader= new LineReader(stream, encoding);
		} catch (CoreException cex) {
			// failed to get input stream
			JavaPlugin.log(cex);
			return -1;
		} catch (IOException e) {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ce) {
					JavaPlugin.log(ce);
				}
			}
			return -1;
		}
		int start= 0;
		try {
			StringBuffer buf= new StringBuffer(80);
			int eols= lineReader.readLine(buf);
			int keyLength= propertyName.length();
			while (eols > 0) {
				String line= buf.toString();
				int i= line.indexOf(propertyName);
				int charPos= i + keyLength;
				char terminatorChar= 0;
				boolean hasNoValue= (charPos >= line.length());
				if (i > -1 && !hasNoValue)
					terminatorChar= line.charAt(charPos);
				if (line.trim().startsWith(propertyName) &&
						(hasNoValue || Character.isWhitespace(terminatorChar) || terminatorChar == '=')) {
					start += line.indexOf(propertyName);
					eols= -17; // found key
				} else {
					start += line.length() + eols;
					buf.setLength(0);
					eols= lineReader.readLine(buf);
				}
			}
			if (eols != -17)
				start= -1; //key not found in file. See bug 63794. This can happen if the key contains escaped characters.
		} catch (IOException ex) {
			JavaPlugin.log(ex);			
			return -1;
		} finally {
			try {
				lineReader.close();
			} catch (IOException ex) {
				JavaPlugin.log(ex);
			}
		}
		return start;
	}

	private void loadProperties() {
		Set duplicateKeys= new HashSet();
		fProperties= new Properties(duplicateKeys);
		InputStream stream;
		try {
			stream= new BufferedInputStream(fPropertiesFile.getContents());
		} catch (CoreException ex) {
			fProperties= new Properties();
			return;
		}
		try {
			fProperties.load(stream);
		} catch (IOException ex) {
			fProperties= new Properties();
			return;
		} finally {
			try {
				stream.close();
			} catch (IOException ex) {
			}
			reportDuplicateKeys(duplicateKeys);
		}
	}

	private void reportDuplicateKeys(Set duplicateKeys) {
		if (duplicateKeys.size() == 0)
			return;
		
		String message= NLSSearchMessages.getFormattedString("NLSSearchResultCollector.duplicateKeys", fPropertiesFile.getName()); //$NON-NLS-1$
		FileEntry groupElement= new FileEntry(fPropertiesFile, message);
		Iterator iter= duplicateKeys.iterator();
		while (iter.hasNext()) {
			String propertyName= (String) iter.next();
			addMatch(groupElement, propertyName);
		}
		fResult.setDuplicatesGroup(groupElement);
	}

}
