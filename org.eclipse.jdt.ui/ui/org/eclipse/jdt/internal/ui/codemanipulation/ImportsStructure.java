/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import java.util.ArrayList;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IDocument;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IImportContainer;import org.eclipse.jdt.core.IImportDeclaration;import org.eclipse.jdt.core.IPackageDeclaration;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.Signature;import org.eclipse.jdt.internal.ui.util.DocumentManager;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class ImportsStructure implements IImportsStructure {
	
	private ICompilationUnit fCompilationUnit;
	private ArrayList fPackageEntries;
	
	private int fImportOnDemandThreshold;
	
	private boolean fReplaceExistingImports;
	
	/**
	 * Create an ImportsStructure for a compilation unit with existing
	 * imports. New imports are added next to the existing import that
	 * has the best match
	 */
	public ImportsStructure(ICompilationUnit cu) throws JavaModelException {
		fCompilationUnit= cu;
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
		
		fImportOnDemandThreshold= Integer.MAX_VALUE;
		fReplaceExistingImports= false;
	}

	/**
	 * Create an ImportsStructure for a compilation unit where exiting imports should be
	 * completly ignored. Create will replace all existing imports 
	 * @param preferenceOrder Defines the prefered order of imports.
	 * @param importThreshold Defines the number of imports in a package needed to introduce a
	 * import on demand instead (e.g. java.util.*)
	 */
	public ImportsStructure(ICompilationUnit cu, String[] preferenceOrder, int importThreshold) {
		fCompilationUnit= cu;
		int nEntries= preferenceOrder.length;
		
		fPackageEntries= new ArrayList(20 + nEntries);
		for (int i= 0; i < nEntries; i++) {
			PackageEntry entry= new PackageEntry(preferenceOrder[i], i);
			fPackageEntries.add(entry);
		}			
		
		fImportOnDemandThreshold= importThreshold;
		fReplaceExistingImports= true;
	}	
		
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
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
		String bestName= "";
		
		for (int i= 0; i < fPackageEntries.size(); i++) {
			boolean isBetterMatch;
			
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			String currName= curr.getName();
			int currMatchLen= getMatchLen(currName, newName);
			
			if (currMatchLen > bestMatchLen) {
				isBetterMatch= true;
			} else if (currMatchLen == bestMatchLen) {		
				if (currMatchLen == newName.length() && currMatchLen == currName.length()) {
					// dulicate entry and complete match
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

	/**
	 * @deprecated
	 */
	public void sortIn(String qualifiedTypeName) {
		addImport(qualifiedTypeName);
	}
	
	/**
	 * @deprecated
	 */
	public void sortIn(String packageName, String typeName) {
		addImport(packageName, typeName);
	}	

	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * the best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 */			
	public void addImport(String qualifiedTypeName) {
		String packName= Signature.getQualifier(qualifiedTypeName);
		String typeName= Signature.getSimpleName(qualifiedTypeName);
		sortIn(packName, typeName);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * the best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param packageName The package name of the type to import
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	public void addImport(String packageName, String typeName) {
		String fullTypeName= JavaModelUtility.concatenateName(packageName, typeName);
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
				PackageEntry packEntry= new PackageEntry(packageName, bestMatch.getCategory());
				packEntry.add(decl);
				int index= fPackageEntries.indexOf(bestMatch);
				if (cmp < 0) { 	// sort in ahead of best match
					fPackageEntries.add(index, packEntry);
				} else {		// sort in after best match
					fPackageEntries.add(index + 1, packEntry);
				}
			}
		}
	}
	
	/**
	 * Add a new import declaration that is sorted in the structure using
	 * the best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param packageName The package name of the type to import
	 * @param enclosingTypeName Name of the enclosing type (dor-separated)
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	public void sortIn(String packageName, String enclosingTypeName, String typeName) {
		sortIn(JavaModelUtility.concatenateName(packageName, enclosingTypeName), typeName);
	}	
		
	private static int getMatchLen(String s, String t) {
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
	 * Creates all new elements in the import structure
	 * Returns all created IImportDeclaration. Does not return null
	 */	
	public IImportDeclaration[] create(boolean save, IProgressMonitor monitor) throws CoreException {
		DocumentManager docManager= new DocumentManager(fCompilationUnit);
		docManager.connect();
		ArrayList created= new ArrayList();
		try {
			performCreate(created, docManager.getDocument());
			if (save) {
				docManager.save(null);
			}	
		} finally {
			docManager.disconnect();
			if (monitor != null) {
				monitor.done();
			}
		}
		IImportDeclaration[] result= new IImportDeclaration[created.size()];
		created.toArray(result);
		return result;
	}		
		
	private int getPackageStatementEndPos() throws JavaModelException {
		IPackageDeclaration[] packDecls= fCompilationUnit.getPackageDeclarations();
		if (packDecls != null && packDecls.length > 0) {
			ISourceRange range= packDecls[0].getSourceRange();
			return range.getOffset() + range.getLength();  // semicolon is included

		}
		return 0;
	}
	
	
	private void performCreate(ArrayList created, IDocument doc) throws JavaModelException {
		int importsStart, importsLen;

		String[] lineDelims= doc.getLegalLineDelimiters();
		String lineDelim= lineDelims.length > 0 ? lineDelims[0] : "\n";
		
		int lastPos;
		StringBuffer buf= new StringBuffer();
		IImportContainer container= fCompilationUnit.getImportContainer();
		if (container.exists()) {
			ISourceRange importSourceRange= container.getSourceRange();
			importsStart= importSourceRange.getOffset();
			importsLen= importSourceRange.getLength();
			if (!fReplaceExistingImports) {
				buf.append(container.getSource());
			}
			lastPos= buf.length();
		} else {
			importsStart= getPackageStatementEndPos();
			importsLen= 0;
			buf.append(lineDelim);
			if (importsStart != 0) {
				lastPos= buf.length();
			} else {
				lastPos= 0;
			}
		}
		
		// all (top level) types in this cu
		IType[] topLevelTypes= fCompilationUnit.getTypes();
	
		int lastCategory= -1;
		
		// create from last to first to not invalidate positions
		for (int i= fPackageEntries.size() -1; i >= 0; i--) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			int nImports= pack.getNumberOfImports();
			if (nImports > 0) {
				String packName= pack.getName();
				if (isImportNeeded(packName, topLevelTypes)) {
					// add empty line
					if (pack.getCategory() != lastCategory) {
						if (lastCategory != -1) {
							buf.insert(lastPos, lineDelim);
						}
						lastCategory= pack.getCategory();
					}
					
					if (nImports >= fImportOnDemandThreshold) {
						// assume no existing imports
						String starimport= packName + ".*";
						lastPos= insertImport(buf, lastPos, starimport, lineDelim);
						created.add(fCompilationUnit.getImport(starimport));
					} else {
						for (int j= nImports - 1; j >= 0; j--) {
							IImportDeclaration currDecl= pack.getImportAt(j);
							if (fReplaceExistingImports || !currDecl.exists()) {
								lastPos= insertImport(buf, lastPos, currDecl.getElementName(), lineDelim);
								created.add(currDecl);
							} else {
								lastPos= currDecl.getSourceRange().getOffset() - importsStart;
							}
						}
					}
				}
			}
		}
		if (!created.isEmpty()) {
			try {
				doc.replace(importsStart, importsLen, buf.toString());
			} catch (BadLocationException e) {
				// can not happen
				e.printStackTrace();
			}
		}
	}
	
	private boolean isImportNeeded(String packName, IType[] cuTypes) {
		if (packName.length() == 0 || "java.lang".equals(packName)) {
			return false;
		}
		if (cuTypes.length > 0) {
			if (packName.equals(cuTypes[0].getPackageFragment().getElementName())) {
				return false;
			}
			for (int i= 0; i < cuTypes.length; i++) {
				if (packName.equals(JavaModelUtility.getFullyQualifiedName(cuTypes[i]))) {
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
		name.append("import ");
		name.append(importName);
		name.append(';');
		if (pos < buf.length()) {
			name.append(lineDelim);
		}		
		buf.insert(pos, name.toString());
		return pos;
	}
	
	
	
	/*
	 * Internal element for the import structure: A container for imports
	 * of all types from the same package
	 */
	private static class PackageEntry {
		
		private String fName;
		private ArrayList fImportEntries;
		private int fCategory;
				
		public PackageEntry(String name, int category) {
			fName= name;
			fImportEntries= new ArrayList(5);
			fCategory= category;
		}	
		
		public int findInsertPosition(String fullImportName) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				IImportDeclaration curr= getImportAt(i);
				int cmp= fullImportName.compareTo(curr.getElementName());
				
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
		
		/*public IImportDeclaration findTypeName(String typeName) {
			int nInports= fImportEntries.size();
			if (nInports > 0) {
				String fullName= StubUtility.getFullTypeName(fName, typeName);
				for (int i= 0; i < nInports; i++) {
					IImportDeclaration curr= getImportAt(i);
					if (!curr.isOnDemand()) {
						if (fullName.equals(curr.getElementName())) {
							return curr;
						}
					}
				}
			}
			return null;
		}*/
		
		public final IImportDeclaration getImportAt(int index) {
			return (IImportDeclaration)fImportEntries.get(index);
		}	
				
		public int getNumberOfImports() {
			return fImportEntries.size();
		}
			
		public String getName() {
			return fName;
		}
		
		public int getCategory() {
			return fCategory;
		}
	
	}	

	

}