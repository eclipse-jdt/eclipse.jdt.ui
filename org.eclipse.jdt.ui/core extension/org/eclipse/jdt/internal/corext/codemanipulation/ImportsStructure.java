/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

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
	
	/**
	 * Creates an ImportsStructure for a compilation unit with existing
	 * imports. New imports are added next to the existing import that
	 * is matching best.
	 * @deprecated Use ImportsStructure(cu, new String[0], Integer.MAX_VALUE, true) instead
	 */
	public ImportsStructure(ICompilationUnit cu) throws JavaModelException {
		this(cu, new String[0], Integer.MAX_VALUE, true);
	}

	/**
	 * Creates an ImportsStructure for a compilation unit where existing imports should be
	 * completly ignored. Create will replace all existing imports 
	 * @param preferenceOrder Defines the preferred order of imports.
	 * @param importThreshold Defines the number of imports in a package needed to introduce a
	 * import on demand instead (e.g. java.util.*)
	 * @deprecated Use ImportsStructure(cu, preferenceOrder, importThreshold, false) instead
	 */
	public ImportsStructure(ICompilationUnit cu, String[] preferenceOrder, int importThreshold) throws JavaModelException {
		this(cu, preferenceOrder, importThreshold, false);
	}

	/**
	 * Creates an ImportsStructure for a compilation unit. New imports
	 * are added next to the existing import that is matching best. 
	 * @param preferenceOrder Defines the preferred order of imports.
	 * @param importThreshold Defines the number of imports in a package needed to introduce a
	 * import on demand instead (e.g. java.util.*).
	 * @param restoreExistingImports If set, existing imports are kept. No imports are deleted, only new added.
	 */	
	public ImportsStructure(ICompilationUnit cu, String[] preferenceOrder, int importThreshold, boolean restoreExistingImports) throws JavaModelException {
		fCompilationUnit= cu;
		
		IImportContainer container= cu.getImportContainer();
		
		fImportOnDemandThreshold= importThreshold;
		fRestoreExistingImports= restoreExistingImports && container.exists();
		fFilterImplicitImports= true;
		
		fPackageEntries= new ArrayList(20);
		
		if (fRestoreExistingImports) {
			addExistingImports(cu.getImports(), container.getSourceRange().getOffset());
		}
		addPreferenceOrderHolders(preferenceOrder);
	}
	
	
	private void addPreferenceOrderHolders(String[] preferenceOrder) {
		if (fPackageEntries.size() == 0) {
			// all new: copy the elements
			for (int i= 0; i < preferenceOrder.length; i++) {
				PackageEntry entry= new PackageEntry(preferenceOrder[i], i, false);
				fPackageEntries.add(entry);
			}
		} else {
			// match the preference order entries to existing imports
			// entries not found are appended after the last successfully matched entry
			int currAppendIndex= 0; 
			
			for (int i= 0; i < preferenceOrder.length; i++) {
				String curr= preferenceOrder[i];
				int lastEntryFound= -1;
				for (int k=0; k < fPackageEntries.size(); k++) {
					PackageEntry entry= (PackageEntry) fPackageEntries.get(k);
					if (entry.getName().startsWith(curr)) {
						int bestGroupId= entry.getGroupID();
						if (bestGroupId == -1 || isBetterMatch(curr, entry, (PackageEntry) fPackageEntries.get(bestGroupId))) {
							entry.setGroupID(i);
							lastEntryFound= k;
						}
						
					}
				}
		
				if (lastEntryFound == -1) {
					PackageEntry newEntry= new PackageEntry(curr, i, false);
					fPackageEntries.add(currAppendIndex, newEntry);
					currAppendIndex++;
				} else {
					currAppendIndex= lastEntryFound + 1;
				}			
			}
		}
	}
	
	private void addExistingImports(IImportDeclaration[] decls, int containerOffset) throws JavaModelException {
		PackageEntry curr= null;
		for (int i= 0; i < decls.length; i++) {
			String name= decls[i].getElementName();
			String packName= Signature.getQualifier(name);
			if (curr == null || !packName.equals(curr.getName())) {
				curr= new PackageEntry(packName, -1, true);
				fPackageEntries.add(curr);
			}
			curr.add(new ImportDeclEntry(name, decls[i].getSourceRange().getOffset() - containerOffset, false));
		}
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
			
	private static boolean sameMatchLenTest(String newName, String bestName, String currName, int matchLen) {				
		// known: bestName and currName differ from newName at position 'matchLen'
		// currName and bestName dont have to differ at position 'matchLen'
		
		// determine the order and return true if currName is closer to newName
		char newChar= getCharAt(newName, matchLen);
		char currChar= getCharAt(currName, matchLen);		
		char bestChar= getCharAt(bestName, matchLen);
		
		if (newChar < currChar) {
			if (bestChar < newChar) {								// b < n < c
				return (currChar - newChar) < (newChar - bestChar);	// -> (c - n) < (n - b)
			} else {												// n < b  && n < c
				return currName.compareTo(bestName) < 0;			// -> (c < b)
			}
		} else {
			if (bestChar > newChar) {								// c < n < b
				return (newChar - currChar) < (bestChar - newChar);	// -> (n - c) < (b - n)
			} else {												// n > b  && n > c
				return bestName.compareTo(currName) < 0;			// -> (c > b)
			}
		}
	}
	
	private PackageEntry findBestMatch(String newName) {
		if (fPackageEntries.isEmpty()) {
			return null;
		}
		
		PackageEntry bestMatch= (PackageEntry) fPackageEntries.get(0);
		
		for (int i= 1; i < fPackageEntries.size(); i++) {
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			if (isBetterMatch(newName, curr, bestMatch)) {
				bestMatch= curr;
			}
		}
		return bestMatch;
	}
	
	private boolean isBetterMatch(String newName, PackageEntry curr, PackageEntry bestMatch) {
		String currName= curr.getName();
		String bestName= bestMatch.getName();
		int currMatchLen= getCommonPrefixLength(currName, newName);
		int bestMatchLen= getCommonPrefixLength(bestName, newName);
		
		if (currMatchLen > bestMatchLen) {
			return true;
		} else if (currMatchLen == bestMatchLen) {		
			if (currMatchLen == newName.length() && currMatchLen == currName.length() && currMatchLen == bestName.length()) {
				// duplicate entry and complete match
				return curr.getNumberOfImports() > bestMatch.getNumberOfImports();
			} else {
				return sameMatchLenTest(newName, bestName, currName, currMatchLen);
			}
		} else {
			return false;
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

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 */			
	public void addImport(String qualifiedTypeName) {
		String typeContainerName= Signature.getQualifier(qualifiedTypeName);
		String typeName= Signature.getSimpleName(qualifiedTypeName);
		addImport(typeContainerName, typeName);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param typeContainerName The type container name (package name / outer type name) of the type to import
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	public void addImport(String typeContainerName, String typeName) {
		String fullTypeName= JavaModelUtil.concatenateName(typeContainerName, typeName);
		ImportDeclEntry decl= new ImportDeclEntry(fullTypeName, -1, false);
			
		PackageEntry bestMatch= findBestMatch(typeContainerName);
		if (bestMatch == null) {
			PackageEntry packEntry= new PackageEntry(typeContainerName, -1, false);
			packEntry.add(decl);
			fPackageEntries.add(packEntry);
		} else {
			int cmp= typeContainerName.compareTo(bestMatch.getName());
			if (cmp == 0) {
				bestMatch.sortIn(decl);
			} else {
				// create a new packageentry
				PackageEntry packEntry= new PackageEntry(typeContainerName, bestMatch.getGroupID(), false);
				packEntry.add(decl);
				int index= fPackageEntries.indexOf(bestMatch);
				if (cmp < 0) { 	// insert ahead of best match
					fPackageEntries.add(index, packEntry);
				} else {		// insert after best match
					fPackageEntries.add(index + 1, packEntry);
				}
			}
		}
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param packageName The package name of the type to import
	 * @param enclosingTypeName Name of the enclosing type (dor-separated)
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	public void addImport(String packageName, String enclosingTypeName, String typeName) {
		addImport(JavaModelUtil.concatenateName(packageName, enclosingTypeName), typeName);
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
			ICompilationUnit cu= fCompilationUnit;
			if (cu.isWorkingCopy()) {
				cu= (ICompilationUnit) cu.getOriginalElement();
			}
			
			IFile file= (IFile) cu.getUnderlyingResource();
			buffer= TextBuffer.acquire(file);
			
			TextRange textRange= getReplaceRange(buffer);

			String replaceString= getReplaceString(buffer, textRange);
			if (replaceString != null) {
				buffer.replace(textRange, replaceString);
			}		
			if (save) {
				TextBuffer.commitChanges(buffer, true, null);
			}
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
			monitor.done();
		}
	}	
		
		
	/**
	 * Get the replace positons.
	 * @param textBuffer The textBuffer
	 */
	public TextRange getReplaceRange(TextBuffer textBuffer) throws JavaModelException {
		IImportContainer container= fCompilationUnit.getImportContainer();
		if (container.exists()) {
			ISourceRange importSourceRange= container.getSourceRange();
			return new TextRange(importSourceRange.getOffset(), importSourceRange.getLength());
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
				
		// all (top level) types in this cu
		IType[] topLevelTypes= fCompilationUnit.getTypes();
	
		int lastGroupID= -1;
		int nCreated= 0;
		
		if (fRestoreExistingImports) {
			buf.append(textBuffer.getContent(importsStart, importsLen));
			buf.append(lineDelim);
		}
		int posProcessed= buf.length();

		// create from last to first to not invalidate positions
		for (int i= fPackageEntries.size() -1; i >= 0; i--) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			int nImports= pack.getNumberOfImports();
			if (nImports > 0) {
				if (fFilterImplicitImports && !isImportNeeded(pack.getName(), topLevelTypes)) {
					continue;
				}

				// add empty line
				if (pack.getGroupID() != lastGroupID) {
					if (lastGroupID != -1) {
						if (!fRestoreExistingImports || !pack.isExisting() || needsSpacer(textBuffer, importsStart, importsLen, importsStart + posProcessed)) {
							buf.insert(posProcessed, lineDelim);
						}
					}
					lastGroupID= pack.getGroupID();
				}
					
				boolean starImport= pack.doesNeedStarImport(fImportOnDemandThreshold);
				int starImportPos= posProcessed;
				int currCreated= nCreated;
				for (int j= nImports - 1; j >= 0; j--) {
					ImportDeclEntry currDecl= pack.getImportAt(j);
					int offset= currDecl.getOffset();
					if (offset == -1) {
						if (!starImport) {
							insertImport(buf, posProcessed, currDecl.getElementName(), lineDelim);	
						}
						nCreated++;
					} else {
						posProcessed= offset;
					}
				}
				// do it last -> goes to the top of the block
				if (starImport && currCreated != nCreated) {
					String starImportString= pack.getName() + ".*";
					insertImport(buf, starImportPos, starImportString, lineDelim);
					nCreated++;
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
		} else if (buf.length() >= lineDelim.length()) {
			buf.setLength(buf.length() - lineDelim.length());
		}
		String newContent= buf.toString();
		if (hasChanged(textBuffer, importsStart, importsLen, newContent)) {
			return newContent;
		}
		return null;
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
	
	private boolean isImportNeeded(String packName, IType[] cuTypes) {
		if (packName.length() == 0 || "java.lang".equals(packName)) { //$NON-NLS-1$
			return false;
		}
		if (cuTypes.length > 0) {
			if (packName.equals(cuTypes[0].getPackageFragment().getElementName())) {
				return false;
			}
			for (int i= 0; i < cuTypes.length; i++) {
				if (packName.equals(JavaModelUtil.getFullyQualifiedName(cuTypes[i]))) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean needsSpacer(TextBuffer buf, int importStart, int importLength, int pos) {
		if (pos == importLength) {			
			return true;
		}
		int currLine= buf.getLineOfOffset(pos);
		if (currLine != -1) {
			TextRegion lastLineRegion= buf.getLineInformation(currLine - 1);
			if (lastLineRegion != null && lastLineRegion.getOffset() >= importStart) {
				int end=  lastLineRegion.getOffset() + lastLineRegion.getLength();
				for (int i= lastLineRegion.getOffset(); i < end; i++) {
					if (!Character.isWhitespace(buf.getChar(i))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	

	private void insertImport(StringBuffer buf, int pos, String importName, String lineDelim) {
		StringBuffer name= new StringBuffer();
		name.append("import "); //$NON-NLS-1$
		name.append(importName);
		name.append(';');
		name.append(lineDelim);
		buf.insert(pos, name.toString());
	}
	
	private int getPackageStatementEndPos(TextBuffer buffer) throws JavaModelException {
		IPackageDeclaration[] packDecls= fCompilationUnit.getPackageDeclarations();
		if (packDecls != null && packDecls.length > 0) {
			int line= buffer.getLineOfOffset(packDecls[0].getSourceRange().getOffset());
			TextRegion region= buffer.getLineInformation(line + 1);
			if (region != null) {
				return region.getOffset();
			}
		}
		return 0;
	}
	
	
	private static class ImportDeclEntry {
		private String fElementName;
		private int fOffset;
		private boolean fIsComment;
		
		public ImportDeclEntry(String elementName, int existingOffset, boolean isComment) {
			fElementName= elementName;
			fOffset= existingOffset;
			fIsComment= isComment;
		}
		
		public int getOffset() {
			return fOffset;
		}
		
		public String getElementName() {
			return fElementName;
		}
		
		public boolean isOnDemand() {
			return fElementName.endsWith(".*");
		}
		
		public boolean isComment() {
			return fIsComment;
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
		private boolean fExisting;
		private int fNumberOfComments;
		
		
		/**
		 * @param name Name of the package entry. e.g. org.eclipse.jdt.ui, containing imports like
		 * org.eclipse.jdt.ui.JavaUI.
		 * @param group The index of the preference order entry assigned
		 *    different group ids will result in spacers between the entries
		 * @param existing Set if the group is existing in the imports to be restored.
		 */
		public PackageEntry(String name, int group, boolean existing) {
			fName= name;
			fImportEntries= new ArrayList(5);
			fGroup= group;
			fExisting= existing;
			fNumberOfComments= 0;
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
			if (imp.isComment()) {
				fNumberOfComments++;
			}
		}
		
		
		public void add(ImportDeclEntry imp) {
			fImportEntries.add(imp);
			if (imp.isComment()) {
				fNumberOfComments++;
			}			
		}
		
		public final ImportDeclEntry getImportAt(int index) {
			return (ImportDeclEntry) fImportEntries.get(index);
		}	
				
		public boolean doesNeedStarImport(int threshold) {
			return fImportEntries.size() - fNumberOfComments >= threshold;
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
		
		public boolean isExisting() {
			return fExisting;
		}
	
	}	

}