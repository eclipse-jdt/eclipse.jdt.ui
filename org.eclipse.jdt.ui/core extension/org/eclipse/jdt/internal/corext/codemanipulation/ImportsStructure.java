/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

/**
 * Created on a Compilation unit, the ImportsStructure allows to add
 * Import Declarations that are added next to the existing import that
 * has the best match.
 */
public class ImportsStructure implements IImportsStructure {
		
	
	private ICompilationUnit fCompilationUnit;
	private ArrayList fPackageEntries;
	
	private int fImportOnDemandThreshold;
	
	private boolean fRestoreExistingImports;
	private boolean fFilterImplicitImports;
	private boolean fFindAmbiguousImports;
	
	private int fNumberOfImportsCreated;
	private boolean fHasChanges= false;
	
	private static final String JAVA_LANG= "java.lang";
	
	/**
	 * Creates an ImportsStructure for a compilation unit. New imports
	 * are added next to the existing import that is matching best. 
	 * @param preferenceOrder Defines the preferred order of imports.
	 * @param importThreshold Defines the number of imports in a package needed to introduce a
	 * import on demand instead (e.g. java.util.*).
	 * @param restoreExistingImports If set, existing imports are kept. No imports are deleted, only new added.
	 */	
	public ImportsStructure(ICompilationUnit cu, String[] preferenceOrder, int importThreshold, boolean restoreExistingImports) throws CoreException {
		fCompilationUnit= cu;
		synchronized (fCompilationUnit) {
			fCompilationUnit.reconcile();
		}
	
		IImportContainer container= cu.getImportContainer();
		
		fImportOnDemandThreshold= importThreshold;
		fRestoreExistingImports= restoreExistingImports && container.exists();
		fFilterImplicitImports= true;
		fFindAmbiguousImports= !restoreExistingImports;
		
		fPackageEntries= new ArrayList(20);
		
		if (fRestoreExistingImports) {
			TextBuffer buffer= null;
			try {
				buffer= aquireTextBuffer();
				addExistingImports(buffer, cu.getImports());
			} finally {
				if (buffer != null) {
					TextBuffer.release(buffer);
				}
			 }
		}	
		
		addPreferenceOrderHolders(preferenceOrder);
		
		fNumberOfImportsCreated= 0;
		fHasChanges= false;
	}
	
	private void addPreferenceOrderHolders(String[] preferenceOrder) {
		if (fPackageEntries.isEmpty()) {
			// all new: copy the elements
			for (int i= 0; i < preferenceOrder.length; i++) {
				PackageEntry entry= new PackageEntry(preferenceOrder[i], i);
				fPackageEntries.add(entry);
			}
		} else {
			// match the preference order entries to existing imports
			// entries not found are appended after the last successfully matched entry
			int currAppendIndex= 0; 
			
			for (int i= 0; i < preferenceOrder.length; i++) {
				String curr= preferenceOrder[i];
				int lastEntryFound= -1;
				PackageMatcher matcher= new PackageMatcher();
				
				
				// find an existing package entry that matches most 
				for (int k=0; k < fPackageEntries.size(); k++) {
					PackageEntry entry= (PackageEntry) fPackageEntries.get(k);
					if (entry.getName().startsWith(curr)) {
						int bestGroupId= entry.getGroupID(); // index in the index array or -1 if not assigned
						// compare entry name with curr and best order entry
						if (bestGroupId != -1) {
							matcher.initialize(entry.getName(), preferenceOrder[bestGroupId]);
							if (matcher.isBetterMatch(curr, true)) {
								entry.setGroupID(i);
								lastEntryFound= k;
							}
						} else {
							entry.setGroupID(i);
							lastEntryFound= k;
						}
					}
				}
		
				if (lastEntryFound == -1) {
					PackageEntry newEntry= new PackageEntry(curr, i);
					fPackageEntries.add(currAppendIndex, newEntry);
					currAppendIndex++;
				} else {
					currAppendIndex= lastEntryFound + 1;
				}			
			}
		}
	}

	
	private void addExistingImports(TextBuffer buffer, IImportDeclaration[] decls) throws JavaModelException {
		if (decls.length == 0) {
			return;
		}				
		PackageEntry currPackage= null;
			
		IImportDeclaration curr= decls[0];
		ISourceRange sourceRange= curr.getSourceRange();
		int currOffset= sourceRange.getOffset();
		int currLength= sourceRange.getLength();
		int currEndLine= buffer.getLineOfOffset(currOffset + currLength);
			
		for (int i= 1; i < decls.length; i++) {
			String name= curr.getElementName();
				
			String packName= Signature.getQualifier(name);
			if (currPackage == null || !packName.equals(currPackage.getName())) {
				currPackage= new PackageEntry(packName, -1);
				fPackageEntries.add(currPackage);
			}

			IImportDeclaration next= decls[i];
			sourceRange= next.getSourceRange();
			int nextOffset= sourceRange.getOffset();
			int nextLength= sourceRange.getLength();
			int nextOffsetLine= buffer.getLineOfOffset(nextOffset);

			// if next import is on a different line, modify the end position to the next line begin offset
			if (currEndLine < nextOffsetLine) {
				currEndLine++;
				nextOffset= buffer.getLineInformation(currEndLine).getOffset();
			}
			currPackage.add(new ImportDeclEntry(name, buffer.getContent(currOffset, nextOffset - currOffset)));
			currOffset= nextOffset;
			curr= next;
				
			// add a comment entry for spacing between imports
			if (currEndLine < nextOffsetLine) {
				nextOffset= buffer.getLineInformation(nextOffsetLine).getOffset();
				
				currPackage= new PackageEntry(); // create a comment package entry for this
				fPackageEntries.add(currPackage);
				currPackage.add(new ImportDeclEntry(null, buffer.getContent(currOffset, nextOffset - currOffset)));
					
				currOffset= nextOffset;
			}
			currEndLine= buffer.getLineOfOffset(nextOffset + nextLength);
		}

		String name= curr.getElementName();
		String packName= Signature.getQualifier(name);
		if (currPackage == null || !packName.equals(currPackage.getName())) {
			currPackage= new PackageEntry(packName, -1);
			fPackageEntries.add(currPackage);
		}
		ISourceRange range= curr.getSourceRange();			
		int endOffset= range.getOffset() + range.getLength();
		String content= buffer.getContent(currOffset, endOffset - currOffset) + buffer.getLineDelimiter();
		currPackage.add(new ImportDeclEntry(name, content));
	}		
		
	/**
	 * Returns the compilation unit of this import structure.
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/**
	 * Sets that implicit imports (types in default package, cu- package and
	 * 'java.lang') should not be created. Note that this is a heuristic filter and can
	 * lead to missing imports, e.g. in cases where a type is forced to be specified
	 * due to a name conflict.
	 * By default, the filter is enabled.
	 * @param filterImplicitImports The filterImplicitImports to set
	 */
	public void setFilterImplicitImports(boolean filterImplicitImports) {
		fFilterImplicitImports= filterImplicitImports;
	}
	
	/**
	 * When set searches for imports that can not be folded into on-demand
	 * imports but must be specified explicitly
	 */
	public void setFindAmbiguousImports(boolean findAmbiguousImports) {
		fFindAmbiguousImports= findAmbiguousImports;
	}	
			
	public static class PackageMatcher {
		private String fNewName;
		private String fBestName;
		private int fBestMatchLen;
		
		public PackageMatcher() {
		}
		
		public void initialize(String newName, String bestName) {
			fNewName= newName;
			fBestName= bestName;
			fBestMatchLen= getCommonPrefixLength(bestName, fNewName);
		}
		
		public boolean isBetterMatch(String currName, boolean preferCurr) {
			boolean isBetter;
			int currMatchLen= getCommonPrefixLength(currName, fNewName);
			int matchDiff= currMatchLen - fBestMatchLen;
			if (matchDiff == 0) {
				if (currMatchLen == fNewName.length() && currMatchLen == currName.length() && currMatchLen == fBestName.length()) {
					// duplicate entry and complete match
					isBetter= preferCurr;
				} else {
					isBetter= sameMatchLenTest(currName);
				}
			} else {
				isBetter= (matchDiff > 0); // curr has longer match
			}
			if (isBetter) {
				fBestName= currName;
				fBestMatchLen= currMatchLen;
			}
			return isBetter;
		}
				
		private boolean sameMatchLenTest(String currName) {
			int matchLen= fBestMatchLen;
			// known: bestName and currName differ from newName at position 'matchLen'
			// currName and bestName dont have to differ at position 'matchLen'

			// determine the order and return true if currName is closer to newName
			char newChar= getCharAt(fNewName, matchLen);
			char currChar= getCharAt(currName, matchLen);
			char bestChar= getCharAt(fBestName, matchLen);

			if (newChar < currChar) {
				if (bestChar < newChar) {								// b < n < c
					return (currChar - newChar) < (newChar - bestChar);	// -> (c - n) < (n - b)
				} else {												// n < b  && n < c
					return currName.compareTo(fBestName) < 0;			// -> (c < b)
				}
			} else {
				if (bestChar > newChar) {								// c < n < b
					return (newChar - currChar) < (bestChar - newChar);	// -> (n - c) < (b - n)
				} else {												// n > b  && n > c
					return fBestName.compareTo(currName) < 0;			// -> (c > b)
				}
			}
		}
			
	}

	private static int getCommonPrefixLength(String s, String t) {
		int len= Math.min(s.length(), t.length());
		for (int i= 0; i < len; i++) {
			if (s.charAt(i) != t.charAt(i)) {
				return i;
			}
		}
		return len;
	}

	private static char getCharAt(String str, int index) {
		if (str.length() > index) {
			return str.charAt(index);
		}
		return 0;
	}
	
	private PackageEntry findBestMatch(String newName) {
		if (fPackageEntries.isEmpty()) {
			return null;
		}
		int groupId= -1;
		int longestPrefix= -1;
		for (int i= 0; i < fPackageEntries.size(); i++) {
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			String name= curr.getName();
			if (newName.startsWith(name)) {
				int prefixLen= name.length();
				if (prefixLen == newName.length()) {
					return curr;
				}
				if ((newName.charAt(prefixLen) == '.') && prefixLen > longestPrefix) {
					longestPrefix= prefixLen;
					groupId= curr.getGroupID();
				}
			}
		}
		PackageEntry bestMatch= null;
		PackageMatcher matcher= new PackageMatcher();
		matcher.initialize(newName, "");
		for (int i= 0; i < fPackageEntries.size(); i++) {
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			if (!curr.isComment()) {
				if (groupId == -1 || curr.getGroupID() == groupId) {
					boolean preferrCurr= bestMatch == null || curr.getNumberOfImports() > bestMatch.getNumberOfImports();
					if (matcher.isBetterMatch(curr.getName(), preferrCurr)) {
						bestMatch= curr;
					}
				}
			}
		}
		return bestMatch;
	}
		
	public static boolean isImplicitImport(String qualifier, ICompilationUnit cu) {
		if (JAVA_LANG.equals(qualifier)) { //$NON-NLS-1$
			return true;
		}
		String packageName= cu.getParent().getElementName();
		if (qualifier.equals(packageName)) {
			return true;
		}
		String mainTypeName= JavaModelUtil.concatenateName(packageName, Signature.getQualifier(cu.getElementName()));
		return qualifier.equals(mainTypeName);
	}

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param binding The type binding of the type to be added
	 * @return Returns the name to use in the code: Simple name if the import
	 * was added, fully qualified type name if the import could not be added due
	 * to a conflict. 
	 */
	public String addImport(ITypeBinding binding) {
		String qualifiedName= binding.getQualifiedName();
		if (qualifiedName.length() > 0) {
			return addImport(qualifiedName);
		}
		return binding.getName();
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param typeContainerName The type container name (package name / outer type name) of the type to import
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	public String addImport(String typeContainerName, String typeName) {
		return addImport(JavaModelUtil.concatenateName(typeContainerName, typeName));
	}

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 */
	public String addImport(String qualifiedTypeName) {
		int bracketOffset= qualifiedTypeName.indexOf('[');
		if (bracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, bracketOffset)) + qualifiedTypeName.substring(bracketOffset);
		}
		return internalAddImport(qualifiedTypeName);
	}
		
	private String internalAddImport(String fullTypeName) {
		String typeContainerName= Signature.getQualifier(fullTypeName);
		String typeName= Signature.getSimpleName(fullTypeName);
		
		if (typeContainerName.length() == 0 && PrimitiveType.toCode(typeName) != null) {
			return fullTypeName;
		}
		
		if (!"*".equals(typeName)) { //$NON-NLS-1$
			String topLevelTypeName= Signature.getQualifier(fCompilationUnit.getElementName());
			
			if (typeName.equals(topLevelTypeName)) {
				if (!typeContainerName.equals(fCompilationUnit.getParent().getElementName())) {
					return fullTypeName;
				} else {
					return typeName;
				}
			}
			String existing= findImport(typeName);
			if (existing != null) {
				if (fullTypeName.equals(existing)) {
					return typeName;
				} else {
					return fullTypeName;
				}
			}
		}
		
		ImportDeclEntry decl= new ImportDeclEntry(fullTypeName, null);
			
		PackageEntry bestMatch= findBestMatch(typeContainerName);
		if (bestMatch == null) {
			PackageEntry packEntry= new PackageEntry(typeContainerName, -1);
			packEntry.add(decl);
			fPackageEntries.add(0, packEntry);
		} else {
			int cmp= typeContainerName.compareTo(bestMatch.getName());
			if (cmp == 0) {
				bestMatch.sortIn(decl);
			} else {
				// create a new packageentry
				int groupId= bestMatch.getGroupID();
				if (groupId != -1) {
					if (getCommonPrefixLength(typeContainerName, bestMatch.getName()) == 0) {
						groupId= -1;
					}
				}
				PackageEntry packEntry= new PackageEntry(typeContainerName, groupId);
				packEntry.add(decl);
				int index= fPackageEntries.indexOf(bestMatch);
				if (cmp < 0) { 	// insert ahead of best match
					fPackageEntries.add(index, packEntry);
				} else {		// insert after best match
					fPackageEntries.add(index + 1, packEntry);
				}
			}
		}
	
		fHasChanges= true;
		return typeName;
	}
	
	/**
	 * Removes an import from the structure.
	 */
	public void removeImport(String qualifiedTypeName) {
		String typeContainerName= Signature.getQualifier(qualifiedTypeName);
		int nPackages= fPackageEntries.size();
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			if (entry.getName().equals(typeContainerName)) {
				if (entry.remove(qualifiedTypeName)) {
					fHasChanges= true;
					return;
				}
			}
		}
	}

	/**
	 * Looks if there already is single import for the given type name.
	 */	
	public String findImport(String simpleName) {
		int nPackages= fPackageEntries.size();
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			ImportDeclEntry found= entry.find(simpleName);
			if (found != null) {
				return found.getElementName();
			}
		}
		return null;		
	}
	
	/**
	 * Creates all new elements in the import structure.
	 */	
	public void create(boolean save, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		
		TextBuffer buffer= null;
		try {
			buffer= aquireTextBuffer();
			
			TextRange textRange= getReplaceRange(buffer);

			String replaceString= getReplaceString(buffer, textRange);
			if (replaceString != null) {
				buffer.replace(textRange, replaceString);
				if (save) {
					TextBuffer.commitChanges(buffer, true, null);
				}
			}		
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
			monitor.done();
		}
	}
	
	private TextBuffer aquireTextBuffer() throws CoreException {
		ICompilationUnit cu= fCompilationUnit;
		if (cu.isWorkingCopy()) {
			cu= (ICompilationUnit) cu.getOriginalElement();
		}		
		IFile file= (IFile) cu.getResource();
		return TextBuffer.acquire(file);
	}
	
		
	/**
	 * Get the replace positons.
	 * @param textBuffer The textBuffer
	 */
	public TextRange getReplaceRange(TextBuffer textBuffer) throws JavaModelException {
		synchronized (fCompilationUnit) {
			fCompilationUnit.reconcile();
		}
		IImportContainer container= fCompilationUnit.getImportContainer();
		if (container.exists()) {
			ISourceRange importSourceRange= container.getSourceRange();
			int startPos= importSourceRange.getOffset();
			int endPos= startPos + importSourceRange.getLength();
			if (!Strings.isLineDelimiterChar(textBuffer.getChar(endPos - 1))) {
				// if not already after a new line, go to beginning of next line
				// (if last char in new line -> import ends with a comment, see 10557)
				int nextLine= textBuffer.getLineOfOffset(endPos) + 1;
				if (nextLine < textBuffer.getNumberOfLines()) {
					int stopPos= textBuffer.getLineInformation(nextLine).getOffset();
					// read to beginning of next character or beginning of next line
					while (endPos < stopPos && Character.isWhitespace(textBuffer.getChar(endPos))) {
						endPos++;
					}
				}
			}
			return new TextRange(startPos, endPos - startPos);
		} else {
			int start= getPackageStatementEndPos(textBuffer);
			return new TextRange(start, 0);
		}		
	}
	
	/**
	 * Returns the replace string or <code>null</code> if no replace is needed.
	 */
	public String getReplaceString(TextBuffer textBuffer, TextRange textRange) throws JavaModelException {
		int importsStart=  textRange.getOffset();
		int importsLen= textRange.getLength();
				
		String lineDelim= textBuffer.getLineDelimiter();
		
		StringBuffer buf= new StringBuffer();
				
		int nCreated= 0;
		PackageEntry lastPackage= null;
		
		
		HashSet starImportPackages= new HashSet();
		HashSet onDemandConflicts= new HashSet();
				
		evaluateStarImportConflicts(starImportPackages, onDemandConflicts);
		int nPackageEntries= fPackageEntries.size();
		for (int i= 0; i < nPackageEntries; i++) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			int nImports= pack.getNumberOfImports();
			if (nImports == 0 || fFilterImplicitImports && isImplicitImport(pack.getName(), fCompilationUnit)) {
				continue;
			}
			
			// add a space between two different groups by looking at the two adjacent imports
			if (lastPackage != null && !pack.isComment() && pack.getGroupID() != lastPackage.getGroupID()) {
				ImportDeclEntry last= lastPackage.getImportAt(lastPackage.getNumberOfImports() - 1);
				ImportDeclEntry first= pack.getImportAt(0);
				if (!lastPackage.isComment() && (last.isNew() || first.isNew())) {
					buf.append(lineDelim);
				}
			}
			lastPackage= pack;
			
			boolean doStarImport= starImportPackages.contains(pack.getName());
			if (doStarImport && (!fRestoreExistingImports || pack.find("*") == null)) { //$NON-NLS-1$
				String starImportString= pack.getName() + ".*"; //$NON-NLS-1$
				appendImportToBuffer(buf, starImportString, lineDelim);
				nCreated++;
			}
			
			for (int k= 0; k < nImports; k++) {
				ImportDeclEntry currDecl= pack.getImportAt(k);
				String content= currDecl.getContent();
				
				if (content == null) { // new entry
					if (!doStarImport || currDecl.isOnDemand() || onDemandConflicts.contains(currDecl.getSimpleName())) {
						appendImportToBuffer(buf, currDecl.getElementName(), lineDelim);
						nCreated++;
					}
				} else {
					buf.append(content);
				}
			}
		}
		
		if (importsLen == 0 && nCreated > 0) { // new import container
			if (fCompilationUnit.getPackageDeclarations().length > 0) { // package statement
				buf.insert(0, lineDelim);
			}
			// check if a space between import and first type is needed
			IType[] types= fCompilationUnit.getTypes();
			if (types.length > 0) {
				if (types[0].getSourceRange().getOffset() == importsStart) {
					buf.append(lineDelim);
				}
			}
		}
		fNumberOfImportsCreated= nCreated;
		
		String newContent= buf.toString();
		if (hasChanged(textBuffer, importsStart, importsLen, newContent)) {
			return newContent;
		}
		return null;
	}

	private void evaluateStarImportConflicts(HashSet starImportPackages, HashSet onDemandConflicts) throws JavaModelException {
		int nPackageEntries= fPackageEntries.size();
		for (int i= 0; i < nPackageEntries; i++) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			if (pack.hasStarImport(fImportOnDemandThreshold)) {
				starImportPackages.add(pack.getName());
			}
		}
		if (!fFindAmbiguousImports || starImportPackages.size() == 0) {
			return;
		}
		starImportPackages.add(fCompilationUnit.getParent().getElementName());
		starImportPackages.add(JAVA_LANG);
		
		ArrayList res= new ArrayList();
		HashSet typesInPackages= new HashSet();
		
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fCompilationUnit.getJavaProject() });
		AllTypesCache.getTypes(scope, IJavaSearchConstants.TYPE, null, res);
		for (int i= 0; i < res.size(); i++) {
			TypeInfo elem= (TypeInfo) res.get(i);
			if (starImportPackages.contains(elem.getTypeContainerName())) {
				if (!typesInPackages.add(elem.getTypeName())) {
					onDemandConflicts.add(elem.getTypeName());
				}
			}
		}
	}
	
	private boolean hasChanged(TextBuffer textBuffer, int offset, int length, String content) {
		if (content.length() != length) {
			return true;
		}
		for (int i= 0; i < length; i++) {
			if (content.charAt(i) != textBuffer.getChar(offset + i)) {
				return true;
			}
		}
		return false;	
	}

	private void appendImportToBuffer(StringBuffer buf, String importName, String lineDelim) {
		String str= "import " + importName + ";\n"; //$NON-NLS-1$ //$NON-NLS-2$
		buf.append(StubUtility.codeFormat(str, 0, lineDelim));
	}
	
	private int getPackageStatementEndPos(TextBuffer buffer) throws JavaModelException {
		IPackageDeclaration[] packDecls= fCompilationUnit.getPackageDeclarations();
		if (packDecls != null && packDecls.length > 0) {
			ISourceRange range= packDecls[0].getSourceRange();
			int line= buffer.getLineOfOffset(range.getOffset() + range.getLength());
			TextRegion region= buffer.getLineInformation(line + 1);
			if (region != null) {
				return region.getOffset();
			}
		}
		return 0;
	}
	
	public String printStructure() {
		int nPackages= fPackageEntries.size();
		StringBuffer buf= new StringBuffer("\n-----------------------\n");
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			entry.dumpStructure(buf);
		}
		return buf.toString();	
	}
	
	private static class ImportDeclEntry {
		
		private String fElementName;
		private String fContent;
		
		public ImportDeclEntry(String elementName, String existingContent) {
			fElementName= elementName;
			fContent= existingContent;
		}
		
		public String getElementName() {
			return fElementName;
		}
		
		public String getSimpleName() {
			return Signature.getSimpleName(fElementName);
		}		
		
		public boolean isOnDemand() {
			return fElementName != null && fElementName.endsWith(".*"); //$NON-NLS-1$
		}
			
		public boolean isNew() {
			return fContent == null;
		}
		
		public boolean isComment() {
			return fElementName == null;
		}
		
		public String getContent() {
			return fContent;
		}
				
	}
	
	/*
	 * Internal element for the import structure: A container for imports
	 * of all types from the same package
	 */
	private static class PackageEntry {
		private String fName;
		private ArrayList fImportEntries;
		private int fGroup;
	
		/**
		 * Comment package entry
		 */
		public PackageEntry() {
			this("!", -1); //$NON-NLS-1$
		}
	
	
		/**
		 * @param name Name of the package entry. e.g. org.eclipse.jdt.ui, containing imports like
		 * org.eclipse.jdt.ui.JavaUI.
		 * @param group The index of the preference order entry assigned
		 *    different group ids will result in spacers between the entries
		 */
		public PackageEntry(String name, int group) {
			fName= name;
			fImportEntries= new ArrayList(5);
			fGroup= group;
		}	
		
		public int findInsertPosition(String fullImportName) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment()) {
					if (fullImportName.compareTo(curr.getElementName()) <= 0) {
						return i;
					}
				}
			}
			return nInports;
		}
				
		public void sortIn(ImportDeclEntry imp) {
			String fullImportName= imp.getElementName();
			int insertPosition= -1;
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment()) {
					int cmp= fullImportName.compareTo(curr.getElementName());
					if (cmp == 0) {
						return; // exists already
					} else if (cmp < 0 && insertPosition == -1) {
						insertPosition= i;
					}
				}
			}
			if (insertPosition == -1) {
				fImportEntries.add(imp);
			} else {
				fImportEntries.add(insertPosition, imp);
			}
		}
		
		
		public void add(ImportDeclEntry imp) {
			fImportEntries.add(imp);
		}
		
		public ImportDeclEntry find(String simpleName) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment()) {
					String name= curr.getElementName();
					if (name.endsWith(simpleName)) {
						int dotPos= name.length() - simpleName.length() - 1;
						if ((dotPos == -1) || (dotPos > 0 && name.charAt(dotPos) == '.')) {
							return curr;
						}
					}						
				}
			}
			return null;
		}		
		
		public boolean remove(String fullName) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment() && fullName.equals(curr.getElementName())) {
					fImportEntries.remove(i);
					return true;
				}
			}
			return false;
		}		
		
		public final ImportDeclEntry getImportAt(int index) {
			return (ImportDeclEntry) fImportEntries.get(index);
		}
		
		public boolean hasStarImport(int threshold) {
			if (isComment() || isDefaultPackage()) { // can not star import default package
				return false;
			}

			int count= 0;
			boolean containsNew= false;
			int nImports= getNumberOfImports();
			for (int i= 0; i < nImports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (curr.isOnDemand()) {
					return true;
				}
				if (!curr.isComment()) {
					count++;
					containsNew |= curr.isNew();
				}
			}
			return (count >= threshold) && containsNew;
		}
		
		public int getNumberOfImports() {
			return fImportEntries.size();
		}	
			
		public String getName() {
			return fName;
		}
		
		public int getGroupID() {
			return fGroup;
		}
		
		public void setGroupID(int groupID) {
			fGroup= groupID;
		}
				
		public ImportDeclEntry getLast() {
			int nImports= getNumberOfImports();
			if (nImports > 0) {
				return getImportAt(nImports - 1);
			}
			return null;
		}
		
		public boolean isComment() {
			return "!".equals(fName); //$NON-NLS-1$
		}
		
		public boolean isDefaultPackage() {
			return fName.length() == 0;
		}
		
		public void dumpStructure(StringBuffer buf) {
			if (isComment()) {
				buf.append("comment\n");
			} else {
				buf.append(fName); buf.append(", groupId: "); buf.append(fGroup); buf.append("\n");
				int nImports= getNumberOfImports();
				for (int i= 0; i < nImports; i++) {
					ImportDeclEntry curr= getImportAt(i);
					buf.append("  ");
					buf.append(curr.getSimpleName());
					if (curr.isNew()) {
						buf.append(" (new)");
					}
					buf.append("\n");
				}
			}
		}
	}	

	/**
	 * Gets the number of imports created.
	 * @return Returns a int
	 */
	public int getNumberOfImportsCreated() {
		return fNumberOfImportsCreated;
	}


	/**
	 * Returns <code>true</code> if imports have been added or removed.
	 * @return boolean
	 */
	public boolean hasChanges() {
		return fHasChanges;
	}



}