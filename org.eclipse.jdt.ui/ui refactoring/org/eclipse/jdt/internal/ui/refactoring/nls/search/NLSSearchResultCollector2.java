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
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

import org.eclipse.jface.text.Position;

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

class NLSSearchResultCollector2 implements IJavaSearchResultCollector {
	/*
	 * Matches are added to fResult. Element (group key) is IJavaElement or FileEntry.
	 */

	private static final StringMatcher fgGetClassNameMatcher= new StringMatcher("*.class.getName()*", false, false);  //$NON-NLS-1$

	private IProgressMonitor fMonitor;
	private NLSSearchResult fResult;
	private IFile fPropertiesFile;
	private Properties fProperties;
	private HashSet fUsedPropertyNames;

	public NLSSearchResultCollector2(IFile propertiesFile, IProgressMonitor monitor, NLSSearchResult result) {
		fPropertiesFile= propertiesFile;
		fMonitor= monitor;
		fResult= result;
	}

	/*
	 * @see IJavaSearchResultCollector#aboutToStart()
	 */
	public void aboutToStart() {
		loadProperties();
		fUsedPropertyNames= new HashSet(fProperties.size());
	}
	
	/*
	 * @see IJavaSearchResultCollector#accept(IResource, int, int, IJavaElement, int)
	 */
	public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
		if (enclosingElement == null)
			return;
		
		// ignore matches in import declarations:
		if (enclosingElement.getElementType() == IJavaElement.IMPORT_DECLARATION)
			return;
		
		// heuristic: ignore matches in resource bundle name field:
		if (enclosingElement.getElementType() == IJavaElement.FIELD) {
			IField field= (IField)enclosingElement;
			String source= field.getSource();
			if (source != null && fgGetClassNameMatcher.match(source))
				return;
		}
		/*
		 * Found reference to NLS Wrapper - now check if the key is there
		 */
		Position keyPosition= new Position(start, Math.max(0, end - start));
		String key= findKey(end, enclosingElement, keyPosition); //TODO: updates keyPosition!
		if (key != null && isKeyDefined(key))
			return;

		start= keyPosition.getOffset();
		
		fResult.addMatch(new Match(enclosingElement, Math.max(start, 0), keyPosition.getLength()));
	}

	/*
	 * @see IJavaSearchResultCollector#done()
	 */
	public void done() {
		markUnusedPropertyNames();
	}

	/*
	 * @see IJavaSearchResultCollector#getProgressMonitor()
	 */
	public IProgressMonitor getProgressMonitor() {
		return fMonitor;
	}

	/**
	 * Checks if the key is defined in the property file
	 */
	private boolean isKeyDefined(String key) {
		// Parse error - don't check key
		if (key == null)
			return true;

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
	 * @return a string denoting the key, null if no key can be found
	 */
	private String findKey(int end, IJavaElement enclosingElement, Position keyPosition) throws CoreException {
		if (enclosingElement instanceof ISourceReference) {
			int offset= ((ISourceReference) enclosingElement).getSourceRange().getOffset();
			int searchStart= end - offset;
			int matchStart= end;
			String source= ((ISourceReference) enclosingElement).getSource();
			if (source == null || searchStart >= source.length())
				return null;
			source= source.substring(searchStart);
			if (source.charAt(0) != '.' || Character.isWhitespace(source.charAt(0)))
				// TODO: doesn't work with whitespace after class name! Need to scan.
				return null;
			int firstBraket= source.indexOf('(');

			int searchEnd= source.indexOf(')');
			if (searchEnd == -1 || firstBraket == -1 || searchEnd <= firstBraket)
				return null;

			int firstQuote= source.indexOf('"');
			matchStart += firstQuote + 1;
			int secondQuote= source.indexOf('"', firstQuote + 1);

			if (secondQuote == -1 || secondQuote <= firstQuote)
				return null;

			keyPosition.setOffset(matchStart);
			keyPosition.setLength(Math.max(1, secondQuote - firstQuote - 1));
			return source.substring(firstQuote + 1, secondQuote);
		}
		return null;
	}

	private void markUnusedPropertyNames() {
		boolean hasUnused= false;		
		String message= NLSSearchMessages.getFormattedString("NLSSearchResultCollector.unusedKeys", fPropertiesFile.getName()); //$NON-NLS-1$
		FileEntry key= new FileEntry(fPropertiesFile, message);
		
		for (Enumeration enum= fProperties.propertyNames(); enum.hasMoreElements();) {
			String propertyName= (String) enum.nextElement();
			if (!fUsedPropertyNames.contains(propertyName)) {
				int start= findPropertyNameStartPosition(propertyName);
				fResult.addMatch(new Match(key, Math.max(start, 0), propertyName.length()));
				hasUnused= true;
			}
		}
		if (hasUnused)
			fResult.setUnusedGroup(key);
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
		try {
			stream= fPropertiesFile.getContents();
			lineReader= new LineReader(stream);
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
				if (line.trim().startsWith(propertyName) && (hasNoValue || Character.isWhitespace(terminatorChar) || terminatorChar == '=')) {
					start += line.indexOf(propertyName);
					eols= -1;
				} else {
					start += line.length() + eols;
					buf.setLength(0);
					eols= lineReader.readLine(buf);
				}
			}
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
		FileEntry key= new FileEntry(fPropertiesFile, message);
		Iterator iter= duplicateKeys.iterator();
		while (iter.hasNext()) {
			String propertyName= (String) iter.next();
			int start= findPropertyNameStartPosition(propertyName);
			fResult.addMatch(new Match(key, Math.max(start, 0), propertyName.length()));
		}
		fResult.setDuplicatesGroup(key);
	}

}
