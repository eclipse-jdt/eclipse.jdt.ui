/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

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

import org.eclipse.jdt.internal.core.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.core.codemanipulation.TextPosition;
import org.eclipse.jdt.internal.core.codemanipulation.TextRegion;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

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
	 */
	public ImportsStructure(ICompilationUnit cu) throws JavaModelException {
		fCompilationUnit= cu;
		fImportOnDemandThreshold= Integer.MAX_VALUE;
		fRestoreExistingImports= cu.getImportContainer().exists();
		fFilterImplicitImports= true;		
		
		IImportDeclaration[] decls= cu.getImports();
		fPackageEntries= new ArrayList(decls.length + 5);
		
		PackageEntry curr= null;
		for (int i= 0; i < decls.length; i++) {			
			String packName= Signature.getQualifier(decls[i].getElementName());
			if (curr == null || !packName.equals(curr.getName())) {
				curr= new PackageEntry(packName, -1);
				fPackageEntries.add(curr);
			}
			curr.add(decls[i]);
		}
	}

	/**
	 * Creates an ImportsStructure for a compilation unit where existing imports should be
	 * completly ignored. Create will replace all existing imports 
	 * @param preferenceOrder Defines the preferred order of imports.
	 * @param importThreshold Defines the number of imports in a package needed to introduce a
	 * import on demand instead (e.g. java.util.*)
	 */
	public ImportsStructure(ICompilationUnit cu, String[] preferenceOrder, int importThreshold) {
		fCompilationUnit= cu;
		fImportOnDemandThreshold= importThreshold;
		fRestoreExistingImports= false;
		fFilterImplicitImports= true;		
		
		int nEntries= preferenceOrder.length;
		
		fPackageEntries= new ArrayList(20 + nEntries);
		for (int i= 0; i < nEntries; i++) {
			PackageEntry entry= new PackageEntry(preferenceOrder[i], i);
			fPackageEntries.add(entry);
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
		int bestMatchLen= -1;
		PackageEntry bestMatch= null;
		String bestName= ""; //$NON-NLS-1$
		
		for (int i= 0; i < fPackageEntries.size(); i++) {
			boolean isBetterMatch;
			
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			String currName= curr.getName();
			int currMatchLen= getCommonPrefixLength(currName, newName);
			
			if (currMatchLen > bestMatchLen) {
				isBetterMatch= true;
			} else if (currMatchLen == bestMatchLen) {		
				if (currMatchLen == newName.length() && currMatchLen == currName.length() && currMatchLen == bestName.length()) {
					// duplicate entry and complete match
					isBetterMatch= curr.getNumberOfImports() > bestMatch.getNumberOfImports();
				} else {
					isBetterMatch= sameMatchLenTest(newName, bestName, currName, currMatchLen);
				}
			} else {
				isBetterMatch= false;
			}
			
			if (isBetterMatch) {
				bestMatchLen= currMatchLen;
				bestMatch= curr;
				bestName= currName;
			}
		}
		return bestMatch;
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
		String packName= Signature.getQualifier(qualifiedTypeName);
		String typeName= Signature.getSimpleName(qualifiedTypeName);
		addImport(packName, typeName);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param packageName The package name of the type to import
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	public void addImport(String packageName, String typeName) {
		String fullTypeName= JavaModelUtil.concatenateName(packageName, typeName);
		IImportDeclaration decl= fCompilationUnit.getImport(fullTypeName);
			
		PackageEntry bestMatch= findBestMatch(packageName);
		if (bestMatch == null) {
			PackageEntry packEntry= new PackageEntry(packageName, -1);
			packEntry.add(decl);
			fPackageEntries.add(packEntry);
		} else {
			int cmp= packageName.compareTo(bestMatch.getName());
			if (cmp == 0) {
				bestMatch.sortIn(decl);
			} else {
				// create a new packageentry
				PackageEntry packEntry= new PackageEntry(packageName, bestMatch.getGroupID());
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
	 * Returns all created IImportDeclaration. Does not return null
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
			
			TextPosition textPosition= getReplacePositions(buffer);

			String replaceString= getReplaceString(buffer, textPosition);
			if (replaceString != null) {
				buffer.replace(textPosition.getOffset(), textPosition.getLength(), replaceString);
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
	 * Get the replace positons .
	 */
	public TextPosition getReplacePositions(TextBuffer textBuffer) throws JavaModelException {
		IImportContainer container= fCompilationUnit.getImportContainer();
		if (container.exists()) {
			ISourceRange importSourceRange= container.getSourceRange();
			return new TextPosition(importSourceRange.getOffset(), importSourceRange.getLength());
		} else {
			int start= getPackageStatementEndPos(textBuffer);
			return new TextPosition(start, 0);
		}		
	}
	
	/**
	 * Returns the replace string or <code>null</code> if no replace is needed.
	 */
	public String getReplaceString(TextBuffer textBuffer, TextPosition textPosition) throws JavaModelException {
		int importsStart=  textPosition.getOffset();
		int importsLen= textPosition.getLength();
				
		String lineDelim= textBuffer.getLineDelimiter();
		
		StringBuffer buf= new StringBuffer();
		
		IImportContainer container= fCompilationUnit.getImportContainer();
		if (fRestoreExistingImports) {
			buf.append(container.getSource());
		}
		
		int lastPos= buf.length();
		
		// all (top level) types in this cu
		IType[] topLevelTypes= fCompilationUnit.getTypes();
	
		int lastGroupID= -1;
		int nCreated= 0;
		
		// create from last to first to not invalidate positions
		for (int i= fPackageEntries.size() -1; i >= 0; i--) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			int nImports= pack.getNumberOfImports();
			if (nImports > 0) {
				String packName= pack.getName();
				if (!fFilterImplicitImports || isImportNeeded(packName, topLevelTypes)) {
					// add empty line
					if (pack.getGroupID() != lastGroupID) {
						if (lastGroupID != -1) {
							buf.insert(lastPos, lineDelim);
						}
						lastGroupID= pack.getGroupID();
					}
					
					if (nImports >= fImportOnDemandThreshold) {
						// assume no existing imports
						String starimport= packName + ".*"; //$NON-NLS-1$
						lastPos= insertImport(buf, lastPos, starimport, lineDelim);
						nCreated++;
					} else {
						for (int j= nImports - 1; j >= 0; j--) {
							IImportDeclaration currDecl= pack.getImportAt(j);
							if (!fRestoreExistingImports || !currDecl.exists()) {
								lastPos= insertImport(buf, lastPos, currDecl.getElementName(), lineDelim);
								nCreated++;
							} else {
								lastPos= currDecl.getSourceRange().getOffset() - importsStart;
							}
						}
					}
				}
			}
		}
		
		if (!container.exists() && nCreated > 0) {
			buf.append(lineDelim);	// nl after import (<nl+>)
			if (importsStart > 0) { // package statement
				buf.insert(0, lineDelim);  //<pack><nl><nl*><import><nl+><nl-pack><cl>
			}
			// check if a space between import and first type is needed
			IType[] types= fCompilationUnit.getTypes();
			if (types.length > 0) {
				if (types[0].getSourceRange().getOffset() == importsStart) {
					buf.append(lineDelim);
				}
			}
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
	
	private int insertImport(StringBuffer buf, int pos, String importName, String lineDelim) {
		StringBuffer name= new StringBuffer();
		if (pos > 0 && pos == buf.length()) {
			buf.append(lineDelim);
			pos= buf.length();
		}
		name.append("import "); //$NON-NLS-1$
		name.append(importName);
		name.append(';');
		if (pos < buf.length()) {
			name.append(lineDelim);
		}		
		buf.insert(pos, name.toString());
		return pos;
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
	
	/*
	 * Internal element for the import structure: A container for imports
	 * of all types from the same package
	 */
	private static class PackageEntry {
		
		private String fName;
		private ArrayList fImportEntries;
		private int fGroup;
				
		public PackageEntry(String name, int group) {
			fName= name;
			fImportEntries= new ArrayList(5);
			fGroup= group;
		}	
		
		public int findInsertPosition(String fullImportName) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				IImportDeclaration curr= getImportAt(i);
				if (fullImportName.compareTo(curr.getElementName()) <= 0) {
					return i;
				}
			}
			return nInports;
		}
				
		public void sortIn(IImportDeclaration imp) {
			String fullImportName= imp.getElementName();
			int insertPosition= -1;
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				int cmp= fullImportName.compareTo(getImportAt(i).getElementName());
				if (cmp == 0) {
					return; // exists already
				} else if (cmp < 0 && insertPosition == -1) {
					insertPosition= i;
				}
			}
			if (insertPosition == -1) {
				fImportEntries.add(imp);
			} else {
				fImportEntries.add(insertPosition, imp);
			}
		}
		
		public void add(IImportDeclaration imp) {
			fImportEntries.add(imp); 
		}
		
		public final IImportDeclaration getImportAt(int index) {
			return (IImportDeclaration)fImportEntries.get(index);
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
	
	}	

}