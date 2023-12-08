/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.text.Position;

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModel;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.util.StringMatcher;


class NLSSearchResultRequestor extends SearchRequestor {

	/**
	 * Warning-free alias for <code>ITerminalSymbols.TokenNameIdentifier</code>.
	 */
	@SuppressWarnings("deprecation")
	protected static final int InternalTokenNameIdentifier= ITerminalSymbols.TokenNameIdentifier;

	/*
	 * Matches are added to fResult. Element (group key) is IJavaElement or FileEntry.
	 */

	private static final StringMatcher fgGetClassNameMatcher= new StringMatcher("*.class.getName()*", false, false);  //$NON-NLS-1$

	/**
	 * Object to indicate that no key has been found.
	 * @see #findKey(Position, IJavaElement)
	 * @since 3.6
	 */
	private static final String NO_KEY= ""; //$NON-NLS-1$

	private NLSSearchResult fResult;
	private IFile fPropertiesFile;
	private Properties fProperties;
	private Properties fSpecifiedAsUsedProperties;
	private HashSet<String> fUsedPropertyNames;

	public NLSSearchResultRequestor(IFile propertiesFile, NLSSearchResult result) {
		fPropertiesFile= propertiesFile;
		fResult= result;
	}

	/*
	 * @see org.eclipse.jdt.core.search.SearchRequestor#beginReporting()
	 */
	@Override
	public void beginReporting() {
		loadProperties();
		fUsedPropertyNames= new HashSet<>(fProperties.size());
	}

	/*
	 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
	 */
	@Override
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		if (match.getAccuracy() == SearchMatch.A_INACCURATE)
			return;

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

		if (javaElement instanceof ISourceReference) {
			String source= ((ISourceReference) javaElement).getSource();
			if (source != null) {
				if (source.contains("NLS.initializeMessages")) //$NON-NLS-1$
					return;
			}
		}

		// found reference to NLS Wrapper - now check if the key is there:
		Position mutableKeyPosition= new Position(offset, length);
		//TODO: What to do if argument string not found? Currently adds a match with type name.
		String key= findKey(mutableKeyPosition, javaElement);
		if (key == null || isKeyDefined(key))
			return;

		ICompilationUnit[] allCompilationUnits= JavaModelUtil.getAllCompilationUnits(new IJavaElement[] {javaElement});
		Object element= javaElement;
		if (allCompilationUnits != null && allCompilationUnits.length == 1)
			element= allCompilationUnits[0];

		fResult.addMatch(new Match(element, mutableKeyPosition.getOffset(), mutableKeyPosition.getLength()));
	}

	public void reportUnusedPropertyNames(IProgressMonitor pm) {
		//Don't use endReporting() for long running operation.
		pm.beginTask("", fProperties.size()); //$NON-NLS-1$
		boolean hasUnused= false;
		pm.setTaskName(NLSSearchMessages.NLSSearchResultRequestor_searching);
		FileEntry groupElement= new FileEntry(fPropertiesFile, NLSSearchMessages.NLSSearchResultCollector_unusedKeys);

		for (Enumeration<?> enumeration= fProperties.propertyNames(); enumeration.hasMoreElements();) {
			String propertyName= (String) enumeration.nextElement();
			if (!fUsedPropertyNames.contains(propertyName) && !fSpecifiedAsUsedProperties.containsKey(propertyName)) {
				addMatch(groupElement, propertyName);
				hasUnused= true;
			}
			pm.worked(1);
		}
		if (hasUnused)
			fResult.addFileEntryGroup(groupElement);
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
		String escapedPropertyName= PropertyFileDocumentModel.escape(propertyName, false);
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
	 *
	 * @param key the key
	 * @return <code>true</code> if the key is defined, <code>false</code> otherwise
	 */
	private boolean isKeyDefined(String key) {
		if (NO_KEY.equals(key))
			return false;

		fUsedPropertyNames.add(key);
		if (fProperties.getProperty(key) != null) {
			return true;
		}
		return false;
	}

	public boolean hasPropertyKey(String key) {
		return fProperties.containsKey(key);
	}

	public boolean isUsedPropertyKey(String key) {
		return fUsedPropertyNames.contains(key);
	}

	public boolean isSpecifiedAsUsed(String key) {
		return fSpecifiedAsUsedProperties.containsKey(key);
	}

	/**
	 * Finds the key defined by the given match. The assumption is that the key is the only argument
	 * and it is a string literal i.e. quoted ("...") or a string constant i.e. 'static final
	 * String' defined in the same class.
	 *
	 * @param keyPositionResult reference parameter: will be filled with the position of the found
	 *            key
	 * @param enclosingElement enclosing java element
	 * @return a string denoting the key, {@link #NO_KEY} if no key can be found and
	 *         <code>null</code> otherwise
	 * @throws CoreException if a problem occurs while accessing the <code>enclosingElement</code>
	 */
	private String findKey(Position keyPositionResult, IJavaElement enclosingElement) throws CoreException {
		ICompilationUnit unit= (ICompilationUnit)enclosingElement.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (unit == null)
			return null;

		String source= unit.getSource();
		if (source == null)
			return null;

		IJavaProject javaProject= unit.getJavaProject();
		IScanner scanner= null;
		if (javaProject != null) {
			String complianceLevel= javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
			String sourceLevel= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
			scanner= ToolFactory.createScanner(false, false, false, sourceLevel, complianceLevel);
		} else {
			scanner= ToolFactory.createScanner(false, false, false, false);
		}
		scanner.setSource(source.toCharArray());
		scanner.resetTo(keyPositionResult.getOffset() + keyPositionResult.getLength(), source.length());

		try {
			if (scanner.getNextToken() != ITerminalSymbols.TokenNameDOT)
				return null;

			if (scanner.getNextToken() != InternalTokenNameIdentifier) // assuming that unit is not module-info.java
				return null;

			String src= new String(scanner.getCurrentTokenSource());
			int tokenStart= scanner.getCurrentTokenStartPosition();
			int tokenEnd= scanner.getCurrentTokenEndPosition();

			if (scanner.getNextToken() == ITerminalSymbols.TokenNameLPAREN) {
				// Old school
				// next must be key string. Ignore methods which do not take a single String parameter (Bug 295040).
				int nextToken= scanner.getNextToken();
				if (nextToken != ITerminalSymbols.TokenNameStringLiteral && nextToken != InternalTokenNameIdentifier)
					return null;

				tokenStart= scanner.getCurrentTokenStartPosition();
				tokenEnd= scanner.getCurrentTokenEndPosition();
				int token;
				while ((token= scanner.getNextToken()) == ITerminalSymbols.TokenNameDOT) {
					if ((nextToken= scanner.getNextToken()) != InternalTokenNameIdentifier) {
							return null;
					}
					tokenStart= scanner.getCurrentTokenStartPosition();
					tokenEnd= scanner.getCurrentTokenEndPosition();
				}
				if (token != ITerminalSymbols.TokenNameRPAREN)
					return null;

				if (nextToken == ITerminalSymbols.TokenNameStringLiteral) {
					keyPositionResult.setOffset(tokenStart + 1);
					keyPositionResult.setLength(tokenEnd - tokenStart - 1);
					return source.substring(tokenStart + 1, tokenEnd);
				} else if (nextToken == InternalTokenNameIdentifier) {
					keyPositionResult.setOffset(tokenStart);
					keyPositionResult.setLength(tokenEnd - tokenStart + 1);
					IType parentClass= (IType)enclosingElement.getAncestor(IJavaElement.TYPE);
					String identifier= source.substring(tokenStart, tokenEnd + 1);
					for (IField field : parentClass.getFields()) {
						if (field.getElementName().equals(identifier)) {
							if (!"String".equals(Signature.getSignatureSimpleName(field.getTypeSignature()))) { //$NON-NLS-1$
								return null;
							}
							Object obj= field.getConstant();
							return obj instanceof String ? ((String)obj).substring(1, ((String)obj).length() - 1) : NO_KEY;
						}
					}
				}
				return NO_KEY;
			} else {
				IJavaElement[] keys= unit.codeSelect(tokenStart, tokenEnd - tokenStart + 1);

				// an interface can't be a key
				if (keys.length == 1 && keys[0].getElementType() == IJavaElement.TYPE && ((IType) keys[0]).isInterface())
					return null;

				keyPositionResult.setOffset(tokenStart);
				keyPositionResult.setLength(tokenEnd - tokenStart + 1);
				return src;
			}
		} catch (InvalidInputException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}

	/**
	 * Finds the start position in the property file. We assume that
	 * the key is the first match on a line.
	 *
	 * @param propertyName the property name
	 * @return	the start position of the property name in the file, -1 if not found
	 */
	private int findPropertyNameStartPosition(String propertyName) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19319
		String encoding;
		try {
			encoding= fPropertiesFile.getCharset();
		} catch (CoreException e1) {
			encoding= "ISO-8859-1";  //$NON-NLS-1$
		}
		try (InputStream stream= createInputStream(fPropertiesFile); LineReader lineReader= new LineReader(stream, encoding)) {
			int start= 0;
			StringBuffer buf= new StringBuffer(80);
			int eols= lineReader.readLine(buf);
			int keyLength= propertyName.length();
			while (eols > 0) {
				String line= buf.toString();
				int i= line.indexOf(propertyName);
				int charPos= i + keyLength;
				char terminatorChar= 0;
				boolean hasNoValue= (charPos >= line.length());
				if (i > -1 && !hasNoValue) {
					terminatorChar= line.charAt(charPos);
				}
				if (line.trim().startsWith(propertyName) &&
						(hasNoValue || Character.isWhitespace(terminatorChar) || terminatorChar == '=')) {
					start+= line.indexOf(propertyName);
					eols= -17; // found key
				} else {
					start+= line.length() + eols;
					buf.setLength(0);
					eols= lineReader.readLine(buf);
				}
			}
			if (eols != -17) {
				start= -1; //key not found in file. See bug 63794. This can happen if the key contains escaped characters.
			}
			return start;
		} catch (CoreException | IOException ex) {
			JavaPlugin.log(ex);
			return -1;
		}
	}

	private void loadProperties() {
		Set<Object> duplicateKeys= new HashSet<>();
		fProperties= new Properties(duplicateKeys);
		Set<Object> duplicateKeys2= new HashSet<>();
		fSpecifiedAsUsedProperties= new Properties(duplicateKeys2);
		try (InputStream stream= new BufferedInputStream(createInputStream(fPropertiesFile))) {
			fProperties.load(stream);
		} catch (CoreException | IOException ex) {
			fProperties= new Properties();
			return;
		} finally {
			reportDuplicateKeys(duplicateKeys);
		}
		if (!"properties".equalsIgnoreCase(fPropertiesFile.getFileExtension())) { //$NON-NLS-1$
			return;
		}
		String propertyFileName= fPropertiesFile.getName();
		String ignorePropertyFileName=
				propertyFileName.substring(0, propertyFileName.length() - ".properties".length()).concat(NLSSearchQuery.NLS_USED_PROPERTIES_EXT); //$NON-NLS-1$
		IFile ignoredPropertiesFile= (IFile) fPropertiesFile.getParent().findMember(ignorePropertyFileName);
		if (ignoredPropertiesFile != null) {
			try (InputStream stream= new BufferedInputStream(createInputStream(ignoredPropertiesFile))) {
				fSpecifiedAsUsedProperties.load(stream);
			} catch (CoreException | IOException ex) {
				fSpecifiedAsUsedProperties= new Properties();
			}
		}
	}

	private InputStream createInputStream(IFile propertiesFile) throws CoreException {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		if (manager != null) {
			ITextFileBuffer buffer= manager.getTextFileBuffer(propertiesFile.getFullPath(), LocationKind.IFILE);
			if (buffer != null) {
				return new ByteArrayInputStream(buffer.getDocument().get().getBytes());
			}
		}

		return propertiesFile.getContents();
	}

	private void reportDuplicateKeys(Set<Object> duplicateKeys) {
		if (duplicateKeys.isEmpty())
			return;

		FileEntry groupElement= new FileEntry(fPropertiesFile, NLSSearchMessages.NLSSearchResultCollector_duplicateKeys);
		Iterator<Object> iter= duplicateKeys.iterator();
		while (iter.hasNext()) {
			String propertyName= (String) iter.next();
			addMatch(groupElement, propertyName);
		}
		fResult.addFileEntryGroup(groupElement);
	}

}
