/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.TypeNameRequestor;

public final class ImportRewriteComputer {
	
	private ICompilationUnit fCompilationUnit;
	private ArrayList fPackageEntries;
	
	private int fImportOnDemandThreshold;
	
	private boolean fFilterImplicitImports;
	private boolean fFindAmbiguousImports;
	
	private List fImportsCreated;
	private List fStaticImportsCreated;

	private IRegion fReplaceRange;

	private int fFlags= 0;
	
	private static final int F_NEEDS_LEADING_DELIM= 2;
	private static final int F_NEEDS_TRAILING_DELIM= 4;
	
	private static final String JAVA_LANG= "java.lang"; //$NON-NLS-1$
	
	public ImportRewriteComputer(ICompilationUnit cu, CompilationUnit root, String[] preferenceOrder, int importThreshold, boolean restoreExistingImports) {
		fCompilationUnit= cu;
				
		fImportOnDemandThreshold= importThreshold;
		fFilterImplicitImports= true;
		fFindAmbiguousImports= true; //!restoreExistingImports;
		
		fPackageEntries= new ArrayList(20);
		fImportsCreated= new ArrayList();
		fStaticImportsCreated= new ArrayList();
		fFlags= 0;
		
		fReplaceRange= evaluateReplaceRange(root);
		if (restoreExistingImports) {
			addExistingImports(root, fReplaceRange);
		}

		PackageEntry[] order= new PackageEntry[preferenceOrder.length];
		for (int i= 0; i < order.length; i++) {
			String curr= preferenceOrder[i];
			if (curr.length() > 0 && curr.charAt(0) == '#') {
				curr= curr.substring(1);
				order[i]= new PackageEntry(curr, curr, true); // static import group
			} else {
				order[i]= new PackageEntry(curr, curr, false); // normal import group
			}
		}
		
		addPreferenceOrderHolders(order);
	}
	
	private void addPreferenceOrderHolders(PackageEntry[] preferenceOrder) {
		if (fPackageEntries.isEmpty()) {
			// all new: copy the elements
			for (int i= 0; i < preferenceOrder.length; i++) {
				fPackageEntries.add(preferenceOrder[i]);
			}
		} else {
			// match the preference order entries to existing imports
			// entries not found are appended after the last successfully matched entry
			
			PackageEntry[] lastAssigned= new PackageEntry[preferenceOrder.length];
			
			// find an existing package entry that matches most
			for (int k= 0; k < fPackageEntries.size(); k++) {
				PackageEntry entry= (PackageEntry) fPackageEntries.get(k);
				if (!entry.isComment()) {
					String currName= entry.getName();
					int currNameLen= currName.length();
					int bestGroupIndex= -1;
					int bestGroupLen= -1;
					for (int i= 0; i < preferenceOrder.length; i++) {
						boolean currPrevStatic= preferenceOrder[i].isStatic();
						if (currPrevStatic == entry.isStatic()) {
							String currPrefEntry= preferenceOrder[i].getName();
							int currPrefLen= currPrefEntry.length();
							if (currName.startsWith(currPrefEntry) && currPrefLen >= bestGroupLen) {
								if (currPrefLen == currNameLen || currName.charAt(currPrefLen) == '.') {
									if (bestGroupIndex == -1 || currPrefLen > bestGroupLen) {
										bestGroupLen= currPrefLen;
										bestGroupIndex= i;
									}
								}
							}
						}
					}
					if (bestGroupIndex != -1) {
						entry.setGroupID(preferenceOrder[bestGroupIndex].getName());
						lastAssigned[bestGroupIndex]= entry; // remember last entry 
					}
				}
			}
			// fill in not-assigned categories, keep partial order
			int currAppendIndex= 0;
			for (int i= 0; i < lastAssigned.length; i++) {
				PackageEntry entry= lastAssigned[i];
				if (entry == null) {
					PackageEntry newEntry= preferenceOrder[i];
					if (currAppendIndex == 0 && !newEntry.isStatic()) {
						currAppendIndex= getIndexAfterStatics();
					}
					fPackageEntries.add(currAppendIndex, newEntry);
					currAppendIndex++;
				} else {
					currAppendIndex= fPackageEntries.indexOf(entry) + 1;
				}
			}
		}
	}

	private static String getQualifier(ImportDeclaration decl) {
		String name= decl.getName().getFullyQualifiedName();
		return decl.isOnDemand() ? name : Signature.getQualifier(name);
	}

	private static String getFullName(ImportDeclaration decl) {
		String name= decl.getName().getFullyQualifiedName();
		return decl.isOnDemand() ? name + ".*": name; //$NON-NLS-1$
	}
	
	private void addExistingImports(CompilationUnit root, IRegion replaceRange) {
		List/*ImportDeclaration*/ decls= root.imports();
		if (decls.isEmpty()) {
			return;
		}				
		PackageEntry currPackage= null;
			
		ImportDeclaration curr= (ImportDeclaration) decls.get(0);
		int currOffset= curr.getStartPosition();
		int currLength= curr.getLength();
		int currEndLine= root.getLineNumber(currOffset + currLength);
		
		for (int i= 1; i < decls.size(); i++) {
			boolean isStatic= curr.isStatic();
			String name= getFullName(curr);
			String packName= getQualifier(curr);
			if (currPackage == null || currPackage.compareTo(packName, isStatic) != 0) {
				currPackage= new PackageEntry(packName, null, isStatic);
				fPackageEntries.add(currPackage);
			}

			ImportDeclaration next= (ImportDeclaration) decls.get(i);
			int nextOffset= next.getStartPosition();
			int nextLength= next.getLength();
			int nextOffsetLine= root.getLineNumber(nextOffset); 

			// if next import is on a different line, modify the end position to the next line begin offset
			if (currEndLine < nextOffsetLine) {
				currEndLine++;
				nextOffset= root.getPosition(currEndLine, 0);
			}
			currPackage.add(new ImportDeclEntry(name, isStatic, new Region(currOffset, nextOffset - currOffset)));
			currOffset= nextOffset;
			curr= next;
				
			// add a comment entry for spacing between imports
			if (currEndLine < nextOffsetLine) {
				nextOffset= root.getPosition(nextOffsetLine, 0);
				
				currPackage= new PackageEntry(); // create a comment package entry for this
				fPackageEntries.add(currPackage);
				currPackage.add(new ImportDeclEntry(null, false, new Region(currOffset, nextOffset - currOffset)));
					
				currOffset= nextOffset;
			}
			currEndLine= root.getLineNumber(nextOffset + nextLength);
		}

		boolean isStatic= curr.isStatic();
		String name= getFullName(curr);
		String packName= getQualifier(curr);
		if (currPackage == null || currPackage.compareTo(packName, isStatic) != 0) {
			currPackage= new PackageEntry(packName, null, isStatic);
			fPackageEntries.add(currPackage);
		}
		int length= replaceRange.getOffset() + replaceRange.getLength() - curr.getStartPosition();
		currPackage.add(new ImportDeclEntry(name, isStatic, new Region(curr.getStartPosition(), length)));
	}
			
	/**
	 * Sets that implicit imports (types in default package, CU- package and
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
	 * @param findAmbiguousImports The new value
	 */
	public void setFindAmbiguousImports(boolean findAmbiguousImports) {
		fFindAmbiguousImports= findAmbiguousImports;
	}	
			
	private static class PackageMatcher {
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
			// currName and bestName don't have to differ at position 'matchLen'

			// determine the order and return true if currName is closer to newName
			char newChar= getCharAt(fNewName, matchLen);
			char currChar= getCharAt(currName, matchLen);
			char bestChar= getCharAt(fBestName, matchLen);

			if (newChar < currChar) {
				if (bestChar < newChar) {								// b < n < c
					return (currChar - newChar) < (newChar - bestChar);	// -> (c - n) < (n - b)
				} else {												// n < b  && n < c
					if (currChar == bestChar) { // longer match between curr and best
						return false; // keep curr and best together, new should be before both
					} else {
						return currChar < bestChar; // -> (c < b)
					}
				}
			} else {
				if (bestChar > newChar) {								// c < n < b
					return (newChar - currChar) < (bestChar - newChar);	// -> (n - c) < (b - n)
				} else {												// n > b  && n > c
					if (currChar == bestChar) {  // longer match between curr and best
						return true; // keep curr and best together, new should be ahead of both
					} else {
						return currChar > bestChar; // -> (c > b)
					}
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
	
	private PackageEntry findBestMatch(String newName, boolean isStatic) {
		if (fPackageEntries.isEmpty()) {
			return null;
		}
		String groupId= null;
		int longestPrefix= -1;
		// find the matching group
		for (int i= 0; i < fPackageEntries.size(); i++) {
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			if (isStatic == curr.isStatic()) {
				String currGroup= curr.getGroupID();
				if (currGroup != null && newName.startsWith(currGroup)) {
					int prefixLen= currGroup.length();
					if (prefixLen == newName.length()) {
						return curr; // perfect fit, use entry
					}
					if ((newName.charAt(prefixLen) == '.') && prefixLen > longestPrefix) {
						longestPrefix= prefixLen;
						groupId= currGroup;
					}
				}
			}
		}
		PackageEntry bestMatch= null;
		PackageMatcher matcher= new PackageMatcher();
		matcher.initialize(newName, ""); //$NON-NLS-1$
		for (int i= 0; i < fPackageEntries.size(); i++) { // find the best match with the same group
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			if (!curr.isComment() && curr.isStatic() == isStatic) {
				if (groupId == null || groupId.equals(curr.getGroupID())) {
					boolean preferrCurr= (bestMatch == null) || (curr.getNumberOfImports() > bestMatch.getNumberOfImports());
					if (matcher.isBetterMatch(curr.getName(), preferrCurr)) {
						bestMatch= curr;
					}
				}
			}
		}
		return bestMatch;
	}
		
	private static boolean isImplicitImport(String qualifier, ICompilationUnit cu) {
		if (JAVA_LANG.equals(qualifier)) { 
			return true;
		}
		String packageName= cu.getParent().getElementName();
		if (qualifier.equals(packageName)) {
			return true;
		}
		String mainTypeName= JavaCore.removeJavaLikeExtension(cu.getElementName());
		if (packageName.length() == 0) {
			return qualifier.equals(mainTypeName);
		}
		return qualifier.equals(packageName +'.' + mainTypeName);
	}
	
	public void addImport(String fullTypeName, boolean isStatic) {
		String typeContainerName= Signature.getQualifier(fullTypeName);
		ImportDeclEntry decl= new ImportDeclEntry(fullTypeName, isStatic, null);
		sortIn(typeContainerName, decl, isStatic);
	}
	
	public boolean removeImport(String qualifiedName, boolean isStatic) {
		String containerName= Signature.getQualifier(qualifiedName);
		
		int nPackages= fPackageEntries.size();
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			if (entry.compareTo(containerName, isStatic) == 0) {
				if (entry.remove(qualifiedName, isStatic)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private int getIndexAfterStatics() {
		for (int i= 0; i < fPackageEntries.size(); i++) {
			if (!((PackageEntry) fPackageEntries.get(i)).isStatic()) {
				return i;
			}
		}
		return fPackageEntries.size();
	}
	
	
	private void sortIn(String typeContainerName, ImportDeclEntry decl, boolean isStatic) {
		PackageEntry bestMatch= findBestMatch(typeContainerName, isStatic);
		if (bestMatch == null) {
			PackageEntry packEntry= new PackageEntry(typeContainerName, null, isStatic);
			packEntry.add(decl);
			int insertPos= packEntry.isStatic() ? 0 : getIndexAfterStatics();
			fPackageEntries.add(insertPos, packEntry);
		} else {
			int cmp= typeContainerName.compareTo(bestMatch.getName());
			if (cmp == 0) {
				bestMatch.sortIn(decl);
			} else {
				// create a new package entry
				String group= bestMatch.getGroupID();
				if (group != null) {
					if (!typeContainerName.startsWith(group)) {
						group= null;
					}
				}
				PackageEntry packEntry= new PackageEntry(typeContainerName, group, isStatic);
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
			
	private IRegion evaluateReplaceRange(CompilationUnit root) {
		List imports= root.imports();
		if (!imports.isEmpty()) {
			ImportDeclaration first= (ImportDeclaration) imports.get(0);
			ImportDeclaration last= (ImportDeclaration) imports.get(imports.size() - 1);
			
			int startPos= first.getStartPosition(); // don't use extended range
			int endPos= root.getExtendedStartPosition(last) + root.getExtendedLength(last);
			int endLine= root.getLineNumber(endPos);
			if (endLine > 0) {
				int nextLinePos= root.getPosition(endLine + 1, 0);
				if (nextLinePos >= 0) {
					int firstTypePos= getFirstTypeBeginPos(root);
					if (firstTypePos != -1 && firstTypePos < nextLinePos) {
						endPos= firstTypePos;
					} else {
						endPos= nextLinePos;
					}
				}
			}
			return new Region(startPos, endPos - startPos);
		} else {
			int start= getPackageStatementEndPos(root);
			return new Region(start, 0);
		}		
	}
	
	public MultiTextEdit getResultingEdits(IProgressMonitor monitor) throws JavaModelException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {	
			int importsStart=  fReplaceRange.getOffset();
			int importsLen= fReplaceRange.getLength();
					
			String lineDelim= fCompilationUnit.findRecommendedLineSeparator();
			IBuffer buffer= fCompilationUnit.getBuffer();
			
			boolean useSpaceBetween= useSpaceBetweenGroups();
						
			int currPos= importsStart;
			MultiTextEdit resEdit= new MultiTextEdit();
			
			if ((fFlags & F_NEEDS_LEADING_DELIM) != 0) {
				// new import container
				resEdit.addChild(new InsertEdit(currPos, lineDelim));
			}
			
			PackageEntry lastPackage= null;
			
			Set onDemandConflicts= null;
			if (fFindAmbiguousImports) {
				onDemandConflicts= evaluateStarImportConflicts(monitor);
			}
			
			ArrayList stringsToInsert= new ArrayList();
			
			int nPackageEntries= fPackageEntries.size();
			for (int i= 0; i < nPackageEntries; i++) {
				PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
				int nImports= pack.getNumberOfImports();
	
				if (fFilterImplicitImports && !pack.isStatic() && isImplicitImport(pack.getName(), fCompilationUnit)) {
					pack.removeAllNew(onDemandConflicts);
					nImports= pack.getNumberOfImports();
				}
				if (nImports == 0) {
					continue;
				}
				
				if (useSpaceBetween) {
					// add a space between two different groups by looking at the two adjacent imports
					if (lastPackage != null && !pack.isComment() && !pack.isSameGroup(lastPackage)) {
						ImportDeclEntry last= lastPackage.getImportAt(lastPackage.getNumberOfImports() - 1);
						ImportDeclEntry first= pack.getImportAt(0);
						if (!lastPackage.isComment() && (last.isNew() || first.isNew())) {
							stringsToInsert.add(lineDelim);
						}
					}
				}
				lastPackage= pack;
				
				boolean isStatic= pack.isStatic();
				
				boolean doStarImport= pack.hasStarImport(fImportOnDemandThreshold, onDemandConflicts);
				if (doStarImport && (pack.find("*") == null)) { //$NON-NLS-1$
					String starImportString= pack.getName() + ".*"; //$NON-NLS-1$
					String str= getNewImportString(starImportString, isStatic, lineDelim);
					stringsToInsert.add(str);
				}
				
				for (int k= 0; k < nImports; k++) {
					ImportDeclEntry currDecl= pack.getImportAt(k);
					IRegion region= currDecl.getSourceRange();
					
					if (region == null) { // new entry
						if (!doStarImport || currDecl.isOnDemand() || (onDemandConflicts != null && onDemandConflicts.contains(currDecl.getSimpleName()))) {
							String str= getNewImportString(currDecl.getElementName(), isStatic, lineDelim);
							stringsToInsert.add(str);
						}
					} else {
						if (!doStarImport || currDecl.isOnDemand() || onDemandConflicts == null || onDemandConflicts.contains(currDecl.getSimpleName())) {
							int offset= region.getOffset();
							removeAndInsertNew(buffer, currPos, offset, stringsToInsert, resEdit);
							stringsToInsert.clear();
							currPos= offset + region.getLength();
						}
					}
				}
			}
			
			int end= importsStart + importsLen;
			removeAndInsertNew(buffer, currPos, end, stringsToInsert, resEdit);
			
			if (importsLen == 0) {
				if (!fImportsCreated.isEmpty() || !fStaticImportsCreated.isEmpty()) { // new import container
					if ((fFlags & F_NEEDS_TRAILING_DELIM) != 0) {
						resEdit.addChild(new InsertEdit(currPos, lineDelim));
					}
				} else {
					return new MultiTextEdit(); // no changes
				}
			}
			return resEdit;
		} finally {
			monitor.done();
		}
	}

	private void removeAndInsertNew(IBuffer buffer, int contentOffset, int contentEnd, ArrayList stringsToInsert, MultiTextEdit resEdit) {
		int pos= contentOffset;
		for (int i= 0; i < stringsToInsert.size(); i++) {
			String curr= (String) stringsToInsert.get(i);
			int idx= findInBuffer(buffer, curr, pos, contentEnd);
			if (idx != -1) {
				if (idx != pos) {
					resEdit.addChild(new DeleteEdit(pos, idx - pos));
				}
				pos= idx + curr.length();
			} else {
				resEdit.addChild(new InsertEdit(pos, curr));
			}
		}
		if (pos < contentEnd) {
			resEdit.addChild(new DeleteEdit(pos, contentEnd - pos));
		}
	}

	private int findInBuffer(IBuffer buffer, String str, int start, int end) {
		int pos= start;
		int len= str.length();
		if (pos + len > end || str.length() == 0) {
			return -1;
		}
		char first= str.charAt(0);
		int step= str.indexOf(first, 1);
		if (step == -1) {
			step= len;
		}
		while (pos + len <= end) {
			if (buffer.getChar(pos) == first) {
				int k= 1;
				while (k < len && buffer.getChar(pos + k) == str.charAt(k)) {
					k++;
				}
				if (k == len) {
					return pos; // found
				}
				if (k < step) {
					pos+= k;
				} else {
					pos+= step;
				}
			} else {
				pos++;
			}
		}
		return -1;
	}
	
	
	/**
	 * @return  Probes if the formatter allows spaces between imports
	 */
	private boolean useSpaceBetweenGroups() {
		try {
			String sample= "import a.A;\n\n import b.B;\nclass C {}"; //$NON-NLS-1$
			TextEdit res= ToolFactory.createCodeFormatter(fCompilationUnit.getJavaProject().getOptions(true)).format(CodeFormatter.K_COMPILATION_UNIT, sample, 0, sample.length(), 0, String.valueOf('\n'));
			Document doc= new Document(sample);
			res.apply(doc);
			int idx1= doc.search(0, "import", true, true, false); //$NON-NLS-1$
			int line1= doc.getLineOfOffset(idx1);
			int idx2= doc.search(idx1 + 1, "import", true, true, false); //$NON-NLS-1$
			int line2= doc.getLineOfOffset(idx2);
			return line2 - line1 > 1; 
		} catch (BadLocationException e) {
			// should not happen 
		}
		return true;
	}

	private Set evaluateStarImportConflicts(IProgressMonitor monitor) throws JavaModelException {
		//long start= System.currentTimeMillis();
		
		final HashSet/*String*/ onDemandConflicts= new HashSet();
		
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fCompilationUnit.getJavaProject() });

		ArrayList/*<char[][]>*/  starImportPackages= new ArrayList();
		ArrayList/*<char[][]>*/ simpleTypeNames= new ArrayList();
		int nPackageEntries= fPackageEntries.size();
		for (int i= 0; i < nPackageEntries; i++) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			if (!pack.isStatic() && pack.hasStarImport(fImportOnDemandThreshold, null)) {
				starImportPackages.add(pack.getName().toCharArray());
				for (int k= 0; k < pack.getNumberOfImports(); k++) {
					ImportDeclEntry curr= pack.getImportAt(k);
					if (!curr.isOnDemand() && !curr.isComment()) {
						simpleTypeNames.add(curr.getSimpleName().toCharArray());
					}
				}
			}
		}
		if (starImportPackages.isEmpty()) {
			return null;
		}
		
		starImportPackages.add(fCompilationUnit.getParent().getElementName().toCharArray());
		starImportPackages.add(JAVA_LANG.toCharArray());
		
		char[][] allPackages= (char[][]) starImportPackages.toArray(new char[starImportPackages.size()][]);
		char[][] allTypes= (char[][]) simpleTypeNames.toArray(new char[simpleTypeNames.size()][]);
		
		TypeNameRequestor requestor= new TypeNameRequestor() {
			HashMap foundTypes= new HashMap();
			
			private String getTypeContainerName(char[] packageName, char[][] enclosingTypeNames) {
				StringBuffer buf= new StringBuffer();
				buf.append(packageName);
				for (int i= 0; i < enclosingTypeNames.length; i++) {
					if (buf.length() > 0)
						buf.append('.');
					buf.append(enclosingTypeNames[i]);
				}
				return buf.toString();
			}
			
			public void acceptType(int flags, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
				String name= new String(simpleTypeName);
				String containerName= getTypeContainerName(packageName, enclosingTypeNames);
				
				String oldContainer= (String) foundTypes.put(name, containerName);
				if (oldContainer != null && !oldContainer.equals(containerName)) {
					onDemandConflicts.add(name);
				}
			}
		};
		new SearchEngine().searchAllTypeNames(allPackages, allTypes, scope, requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
		return onDemandConflicts;
	}
		
	private String getNewImportString(String importName, boolean isStatic, String lineDelim) {
		StringBuffer buf= new StringBuffer();
		buf.append("import "); //$NON-NLS-1$
		if (isStatic) {
			buf.append("static "); //$NON-NLS-1$
		}
		buf.append(importName);
		buf.append(';');
		buf.append(lineDelim);
		// str= StubUtility.codeFormat(str, 0, lineDelim);
		
		if (isStatic) {
			fStaticImportsCreated.add(importName);
		} else {
			fImportsCreated.add(importName);
		}
		return buf.toString();
	}
	
	private static int getFirstTypeBeginPos(CompilationUnit root) {
		List types= root.types();
		if (!types.isEmpty()) {
			return root.getExtendedStartPosition(((ASTNode) types.get(0)));
		}
		return -1;
	}
	
	
	private int getPackageStatementEndPos(CompilationUnit root) {
		PackageDeclaration packDecl= root.getPackage();
		if (packDecl != null) {
			int lineAfterPackage= root.getLineNumber(packDecl.getStartPosition() + packDecl.getLength()) + 1;
			int afterPackageStatementPos= root.getPosition(lineAfterPackage, 0);
			if (afterPackageStatementPos >= 0) {
				int firstTypePos= getFirstTypeBeginPos(root);
				if (firstTypePos != -1 && firstTypePos <= afterPackageStatementPos) {
					if (firstTypePos <= afterPackageStatementPos) {
						fFlags |= F_NEEDS_TRAILING_DELIM;
						if (firstTypePos == afterPackageStatementPos) {
							fFlags |= F_NEEDS_LEADING_DELIM;
						}
						return firstTypePos;
					}
				}
				fFlags |= F_NEEDS_LEADING_DELIM;
				return afterPackageStatementPos; // insert a line after after package statement
			}
		}
		fFlags |= F_NEEDS_TRAILING_DELIM;
		return 0;
	}
	
	public String toString() {
		int nPackages= fPackageEntries.size();
		StringBuffer buf= new StringBuffer("\n-----------------------\n"); //$NON-NLS-1$
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			if (entry.isStatic()) {
				buf.append("static "); //$NON-NLS-1$
			}
			buf.append(entry.toString());
		}
		return buf.toString();	
	}
	
	private static final class ImportDeclEntry {
		
		private String fElementName;
		private IRegion fSourceRange;
		private final boolean fIsStatic;
		
		public ImportDeclEntry(String elementName, boolean isStatic, IRegion sourceRange) {
			fElementName= elementName;
			fSourceRange= sourceRange;
			fIsStatic= isStatic;
		}
				
		public String getElementName() {
			return fElementName;
		}
		
		public int compareTo(String fullName, boolean isStatic) {
			int cmp= fElementName.compareTo(fullName);
			if (cmp == 0) {
				if (fIsStatic == isStatic) {
					return 0;
				}
				return fIsStatic ? -1 : 1;
			}
			return cmp;
		}
		
		public String getSimpleName() {
			return Signature.getSimpleName(fElementName);
		}		
		
		public boolean isOnDemand() {
			return fElementName != null && fElementName.endsWith(".*"); //$NON-NLS-1$
		}
		
		public boolean isStatic() {
			return fIsStatic;
		}
			
		public boolean isNew() {
			return fSourceRange == null;
		}
		
		public boolean isComment() {
			return fElementName == null;
		}
		
		public IRegion getSourceRange() {
			return fSourceRange;
		}
				
	}
	
	/*
	 * Internal element for the import structure: A container for imports
	 * of all types from the same package
	 */
	private final static class PackageEntry {
		
		public static PackageEntry createOnPlaceholderEntry(String preferenceOrder) {
			if (preferenceOrder.length() > 0 && preferenceOrder.charAt(0) == '#') {
				String curr= preferenceOrder.substring(1);
				return new PackageEntry(curr, curr, true);
			}
			return new PackageEntry(preferenceOrder, preferenceOrder, false);
		}
		
		private String fName;
		private ArrayList fImportEntries;
		private String fGroup;
		private boolean fIsStatic;
	
		/**
		 * Comment package entry
		 */
		public PackageEntry() {
			this("!", null, false); //$NON-NLS-1$
		}
		
		/**
		 * @param name Name of the package entry. e.g. org.eclipse.jdt.ui, containing imports like
		 * org.eclipse.jdt.ui.JavaUI.
		 * @param group The index of the preference order entry assigned
		 *    different group id's will result in spacers between the entries
		 */
		public PackageEntry(String name, String group, boolean isStatic) {
			fName= name;
			fImportEntries= new ArrayList(5);
			fGroup= group;
			fIsStatic= isStatic;
		}
		
		public boolean isStatic() {
			return fIsStatic;
		}
		
		public int compareTo(String name, boolean isStatic) {
			int cmp= fName.compareTo(name);
			if (cmp == 0) {
				if (fIsStatic == isStatic) {
					return 0;
				}
				return fIsStatic ? -1 : 1;
			}
			return cmp;
		}
						
		public void sortIn(ImportDeclEntry imp) {
			String fullImportName= imp.getElementName();
			int insertPosition= -1;
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment()) {
					int cmp= curr.compareTo(fullImportName, imp.isStatic());
					if (cmp == 0) {
						return; // exists already
					} else if (cmp > 0 && insertPosition == -1) {
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
		
		public boolean remove(String fullName, boolean isStaticImport) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment() && curr.compareTo(fullName, isStaticImport) == 0) {
					fImportEntries.remove(i);
					return true;
				}
			}
			return false;
		}
		
		public void removeAllNew(Set onDemandConflicts) {
			int nInports= fImportEntries.size();
			for (int i= nInports - 1; i >= 0; i--) {
				ImportDeclEntry curr= getImportAt(i);
				if (curr.isNew() /*&& (onDemandConflicts == null || onDemandConflicts.contains(curr.getSimpleName()))*/) {
					fImportEntries.remove(i);
				}
			}
		}
		
		public ImportDeclEntry getImportAt(int index) {
			return (ImportDeclEntry) fImportEntries.get(index);
		}
		
		public boolean hasStarImport(int threshold, Set explicitImports) {
			if (isComment() || isDefaultPackage()) { // can not star import default package
				return false;
			}
			int nImports= getNumberOfImports();
			int count= 0;
			boolean containsNew= false;
			for (int i= 0; i < nImports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (curr.isOnDemand()) {
					return true;
				}
				if (!curr.isComment()) {
					count++;
					boolean isExplicit= !curr.isStatic() && (explicitImports != null) && explicitImports.contains(curr.getSimpleName());
					containsNew |= curr.isNew() && !isExplicit;
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
		
		public String getGroupID() {
			return fGroup;
		}
		
		public void setGroupID(String groupID) {
			fGroup= groupID;
		}
		
		public boolean isSameGroup(PackageEntry other) {
			if (fGroup == null) {
				return other.getGroupID() == null;
			} else {
				return fGroup.equals(other.getGroupID()) && (fIsStatic == other.isStatic());
			}
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
		
		public String toString() {
			StringBuffer buf= new StringBuffer();
			if (isComment()) {
				buf.append("comment\n"); //$NON-NLS-1$
			} else {
				buf.append(fName); buf.append(", groupId: "); buf.append(fGroup); buf.append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
				int nImports= getNumberOfImports();
				for (int i= 0; i < nImports; i++) {
					ImportDeclEntry curr= getImportAt(i);
					buf.append("  "); //$NON-NLS-1$
					if (curr.isStatic()) {
						buf.append("static "); //$NON-NLS-1$
					}
					buf.append(curr.getSimpleName());
					if (curr.isNew()) {
						buf.append(" (new)"); //$NON-NLS-1$
					}
					buf.append("\n"); //$NON-NLS-1$
				}
			}
			return buf.toString();
		}
	}	
	
	public String[] getCreatedImports() {
	    return (String[]) fImportsCreated.toArray(new String[fImportsCreated.size()]);
	}
	
	public String[] getCreatedStaticImports() {
	    return (String[]) fStaticImportsCreated.toArray(new String[fStaticImportsCreated.size()]);
	}
}
