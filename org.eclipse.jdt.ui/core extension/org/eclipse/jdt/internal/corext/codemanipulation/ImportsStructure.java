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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;

import org.eclipse.jdt.internal.corext.ValidateEditException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.compare.JavaTokenComparator;

/**
 * Created on a Compilation unit, the ImportsStructure allows to add
 * Import Declarations that are added next to the existing import that
 * has the best match.
 */
public final class ImportsStructure implements IImportsStructure {
	
	private ICompilationUnit fCompilationUnit;
	private ArrayList fPackageEntries;
	
	private int fImportOnDemandThreshold;
	
	private boolean fFilterImplicitImports;
	private boolean fFindAmbiguousImports;
	
	private List fImportsCreated;
	private boolean fHasChanges= false;
	private IRegion fReplaceRange;
	
	private static final String JAVA_LANG= "java.lang"; //$NON-NLS-1$
	
	/**
	 * Creates an ImportsStructure for a compilation unit. New imports
	 * are added next to the existing import that is matching best. 
	 * @param cu The compilation unit
	 * @param preferenceOrder Defines the preferred order of imports.
	 * @param importThreshold Defines the number of imports in a package needed to introduce a
	 * import on demand instead (e.g. java.util.*).
	 * @param restoreExistingImports If set, existing imports are kept. No imports are deleted, only new added.
	 * @throws CoreException
	 */	
	public ImportsStructure(ICompilationUnit cu, String[] preferenceOrder, int importThreshold, boolean restoreExistingImports) throws CoreException {
		fCompilationUnit= cu;
		JavaModelUtil.reconcile(cu);
	
		IImportContainer container= cu.getImportContainer();
		
		fImportOnDemandThreshold= importThreshold;
		fFilterImplicitImports= true;
		fFindAmbiguousImports= !restoreExistingImports;
		
		fPackageEntries= new ArrayList(20);
		
		IProgressMonitor monitor= new NullProgressMonitor();
		IDocument document= null;
		try {
			document= aquireDocument(monitor);
			if (restoreExistingImports && container.exists()) {
				addExistingImports(document, cu.getImports());
			}
			fReplaceRange= evaluateReplaceRange(document);
			
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		} finally {
			if (document != null) {
				releaseDocument(document, monitor);
			}
		 }
		
		addPreferenceOrderHolders(preferenceOrder);
		
		fImportsCreated= null;
		fHasChanges= false;
	}
	
	private void addPreferenceOrderHolders(String[] preferenceOrder) {
		if (fPackageEntries.isEmpty()) {
			// all new: copy the elements
			for (int i= 0; i < preferenceOrder.length; i++) {
				PackageEntry entry= new PackageEntry(preferenceOrder[i]);
				fPackageEntries.add(entry);
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
						String currPrefEntry= preferenceOrder[i];
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
					if (bestGroupIndex != -1) {
						entry.setGroupID(preferenceOrder[bestGroupIndex]);
						lastAssigned[bestGroupIndex]= entry; // remember last entry 
					}
				}
			}
			// fill in not-assigned categories, keep partial order
			int currAppendIndex= 0;
			for (int i= 0; i < lastAssigned.length; i++) {
				PackageEntry entry= lastAssigned[i];
				if (entry == null) {
					PackageEntry newEntry= new PackageEntry(preferenceOrder[i]);
					fPackageEntries.add(currAppendIndex, newEntry);
					currAppendIndex++;
				} else {
					currAppendIndex= fPackageEntries.indexOf(entry) + 1;
				}
			}
		}
	}

	
	private void addExistingImports(IDocument document, IImportDeclaration[] decls) throws JavaModelException, BadLocationException {
		if (decls.length == 0) {
			return;
		}				
		PackageEntry currPackage= null;
			
		IImportDeclaration curr= decls[0];
		ISourceRange sourceRange= curr.getSourceRange();
		int currOffset= sourceRange.getOffset();
		int currLength= sourceRange.getLength();
		int currEndLine= document.getLineOfOffset(currOffset + currLength);
			
		for (int i= 1; i < decls.length; i++) {
			String name= curr.getElementName();
			boolean isStatic= Flags.isStatic(curr.getFlags());
				
			String packName= Signature.getQualifier(name);
			if (currPackage == null || !packName.equals(currPackage.getName())) {
				currPackage= new PackageEntry(packName, null);
				fPackageEntries.add(currPackage);
			}

			IImportDeclaration next= decls[i];
			sourceRange= next.getSourceRange();
			int nextOffset= sourceRange.getOffset();
			int nextLength= sourceRange.getLength();
			int nextOffsetLine= document.getLineOfOffset(nextOffset);

			// if next import is on a different line, modify the end position to the next line begin offset
			if (currEndLine < nextOffsetLine) {
				currEndLine++;
				nextOffset= document.getLineInformation(currEndLine).getOffset();
			}
			currPackage.add(new ImportDeclEntry(name, document.get(currOffset, nextOffset - currOffset), isStatic));
			currOffset= nextOffset;
			curr= next;
				
			// add a comment entry for spacing between imports
			if (currEndLine < nextOffsetLine) {
				nextOffset= document.getLineInformation(nextOffsetLine).getOffset();
				
				currPackage= new PackageEntry(); // create a comment package entry for this
				fPackageEntries.add(currPackage);
				currPackage.add(new ImportDeclEntry(null, document.get(currOffset, nextOffset - currOffset), false));
					
				currOffset= nextOffset;
			}
			currEndLine= document.getLineOfOffset(nextOffset + nextLength);
		}

		String name= curr.getElementName();
		boolean isStatic= Flags.isStatic(curr.getFlags());
		String packName= Signature.getQualifier(name);
		if (currPackage == null || !packName.equals(currPackage.getName())) {
			currPackage= new PackageEntry(packName, null);
			fPackageEntries.add(currPackage);
		}
		ISourceRange range= curr.getSourceRange();			
		int endOffset= range.getOffset() + range.getLength();
		String content= document.get(currOffset, endOffset - currOffset) + TextUtilities.getDefaultLineDelimiter(document);
		currPackage.add(new ImportDeclEntry(name, content, isStatic));
	}		
		
	/**
	 * @return Returns the compilation unit of this import structure.
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
	 * @param findAmbiguousImports The new value
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
	
	private PackageEntry findBestMatch(String newName) {
		if (fPackageEntries.isEmpty()) {
			return null;
		}
		String groupId= null;
		int longestPrefix= -1;
		for (int i= 0; i < fPackageEntries.size(); i++) {
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			String currGroup= curr.getGroupID();
			if (currGroup != null && newName.startsWith(currGroup)) {
				int prefixLen= currGroup.length();
				if (prefixLen == newName.length()) {
					return curr;
				}
				if ((newName.charAt(prefixLen) == '.') && prefixLen > longestPrefix) {
					longestPrefix= prefixLen;
					groupId= currGroup;
				}
			}
		}
		PackageEntry bestMatch= null;
		PackageMatcher matcher= new PackageMatcher();
		matcher.initialize(newName, ""); //$NON-NLS-1$
		for (int i= 0; i < fPackageEntries.size(); i++) {
			PackageEntry curr= (PackageEntry) fPackageEntries.get(i);
			if (!curr.isComment()) {
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
	 * @param ast The ast to create the type for
	 * @return Returns the a new AST node that is either simple if the import was successful or 
	 * fully qualified type name if the import could not be added due to a conflict. 
	 */
	public Type addImport(ITypeBinding binding, AST ast) {
		if (binding.isPrimitive()) {
			return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
		}
		
		ITypeBinding normalizedBinding= Bindings.normalizeTypeBinding(binding);
		if (normalizedBinding == null) {
			return ast.newSimpleType(ast.newSimpleName("invalid")); //$NON-NLS-1$
		}
		
		if (normalizedBinding.isTypeVariable()) {
			// no import
			return ast.newSimpleType(ast.newSimpleName(Bindings.getRawName(binding)));
		}
		if (normalizedBinding.isWildcardType()) {
			WildcardType wcType= ast.newWildcardType();
			ITypeBinding bound= normalizedBinding.getBound();
			if (bound != null) {
				Type boundType= addImport(bound, ast);
				wcType.setBound(boundType, normalizedBinding.isUpperbound());
			}
			return wcType;
		}
		
		if (normalizedBinding.isArray()) {
			Type elementType= addImport(normalizedBinding.getElementType(), ast);
			return ast.newArrayType(elementType, normalizedBinding.getDimensions());
		}
		
		String qualifiedName= Bindings.getRawQualifiedName(normalizedBinding);
		if (qualifiedName.length() > 0) {
			String res= internalAddImport(qualifiedName);
			
			ITypeBinding[] typeArguments= normalizedBinding.getTypeArguments();
			if (typeArguments.length > 0) {
				Type erasureType= ast.newSimpleType(ast.newSimpleName(res));
				ParameterizedType paramType= ast.newParameterizedType(erasureType);
				List arguments= paramType.typeArguments();
				

				for (int i= 0; i < typeArguments.length; i++) {
					arguments.add(addImport(typeArguments[i], ast));
				}
				return paramType;
			}
			return ast.newSimpleType(ast.newSimpleName(res));
		}
		return ast.newSimpleType(ast.newSimpleName(Bindings.getRawName(normalizedBinding)));
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
		
		if (binding.isPrimitive() || binding.isTypeVariable()) {
			return binding.getName();
		}
		
		ITypeBinding normalizedBinding= Bindings.normalizeTypeBinding(binding);
		if (normalizedBinding == null) {
			return "invalid"; //$NON-NLS-1$
		}
		if (normalizedBinding.isWildcardType()) {
			StringBuffer res= new StringBuffer("?"); //$NON-NLS-1$
			ITypeBinding bound= normalizedBinding.getBound();
			if (bound != null) {
				if (normalizedBinding.isUpperbound()) {
					res.append(" extends "); //$NON-NLS-1$
				} else {
					res.append(" super "); //$NON-NLS-1$
				}
				res.append(addImport(bound));
			}
			return res.toString();
		}
		
		if (normalizedBinding.isArray()) {
			StringBuffer res= new StringBuffer(addImport(normalizedBinding.getElementType()));
			for (int i= normalizedBinding.getDimensions(); i > 0; i--) {
				res.append("[]"); //$NON-NLS-1$
			}
			return res.toString();
		}
		

		
		String qualifiedName= Bindings.getRawQualifiedName(normalizedBinding);
		if (qualifiedName.length() > 0) {
			String str= internalAddImport(qualifiedName);
			
			ITypeBinding[] typeArguments= normalizedBinding.getTypeArguments();
			if (typeArguments.length > 0) {
				StringBuffer res= new StringBuffer(str);
				res.append('<');
				for (int i= 0; i < typeArguments.length; i++) {
					if (i > 0) {
						res.append(','); //$NON-NLS-1$
					}
					res.append(addImport(typeArguments[i]));
				}
				res.append('>');
				return res.toString();
			}
			return str;
		}
		return Bindings.getRawName(normalizedBinding);
	}
		
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 * @return Returns either the simple type name if the import was succesful or else the qualified type name
	 */
	public String addImport(String qualifiedTypeName) {
		int angleBracketOffset= qualifiedTypeName.indexOf('<');
		if (angleBracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, angleBracketOffset)) + qualifiedTypeName.substring(angleBracketOffset);
		}
		int bracketOffset= qualifiedTypeName.indexOf('[');
		if (bracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, bracketOffset)) + qualifiedTypeName.substring(bracketOffset);
		}
		return internalAddImport(qualifiedTypeName);
	}
	
	
	public String addStaticImport(IBinding binding) {
		if (binding instanceof IVariableBinding) {
			ITypeBinding declaringType= ((IVariableBinding) binding).getDeclaringClass();
			return addStaticImport(Bindings.getRawQualifiedName(declaringType), binding.getName(), true);
		} else if (binding instanceof IMethodBinding) {
			ITypeBinding declaringType= ((IMethodBinding) binding).getDeclaringClass();
			return addStaticImport(Bindings.getRawQualifiedName(declaringType), binding.getName(), false);
		}
		return binding.getName();
	}
	
	/**
	 * Adds a new static import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param declaringTypeName The qualified name of the static's member declaring type
	 * @return Returns either the simple type name if the import was succesful or else the qualified type name
	 */
	public String addStaticImport(String declaringTypeName, String simpleName, boolean isField) {
		String containerName= Signature.getQualifier(declaringTypeName);
		String typeName= Signature.getSimpleName(declaringTypeName);
		String fullName= declaringTypeName + '.' + simpleName;
		
		if (containerName.length() == 0) {
			return declaringTypeName + '.' + simpleName;
		}
		if (!"*".equals(simpleName)) { //$NON-NLS-1$
			if (isField) {
				String existing= findStaticImport(null, null, simpleName);
				if (existing != null) {
					if (existing.equals(fullName)) {
						return simpleName;
					} else {
						return fullName;
					}
				}
			} else {
				String existing= findStaticImport(containerName, typeName, simpleName);
				if (existing != null) {
					return simpleName;
				}
			}
		}
		ImportDeclEntry decl= new ImportDeclEntry(fullName, null, true);
		
		sortIn(containerName, decl);
		return simpleName;
	}
	
	private String internalAddImport(String fullTypeName) {
		int idx= fullTypeName.lastIndexOf('.');	
		String typeContainerName, typeName;
		if (idx != -1) {
			typeContainerName= fullTypeName.substring(0, idx);
			typeName= fullTypeName.substring(idx + 1);
		} else {
			typeContainerName= ""; //$NON-NLS-1$
			typeName= fullTypeName;
		}
		
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
		
		ImportDeclEntry decl= new ImportDeclEntry(fullTypeName, null, false);
			
		sortIn(typeContainerName, decl);
		return typeName;
	}
	
	private void sortIn(String typeContainerName, ImportDeclEntry decl) {
		PackageEntry bestMatch= findBestMatch(typeContainerName);
		if (bestMatch == null) {
			PackageEntry packEntry= new PackageEntry(typeContainerName, null);
			packEntry.add(decl);
			fPackageEntries.add(0, packEntry);
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
				PackageEntry packEntry= new PackageEntry(typeContainerName, group);
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
	}

	/**
	 * Removes an import from the structure.
	 * @param qualifiedName The qualified type name to remove from the imports
	 * @return Returns <code>true</code> if the import was found and removed
	 */
	public boolean removeImport(String qualifiedName) {
		String typeContainerName= Signature.getQualifier(qualifiedName);
		int bracketOffset= qualifiedName.indexOf('[');
		if (bracketOffset != -1) {
			qualifiedName= qualifiedName.substring(0, bracketOffset);
		}		
		
		int nPackages= fPackageEntries.size();
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			if (entry.getName().equals(typeContainerName)) {
				if (entry.remove(qualifiedName, false)) {
					fHasChanges= true;
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Removes an import from the structure.
	 * @param qualifiedName The qualified member name to remove from the imports
	 * @return Returns <code>true</code> if the import was found and removed
	 */
	public boolean removeStaticImport(String qualifiedName) {
		String containerName= Signature.getQualifier(qualifiedName);
		
		int nPackages= fPackageEntries.size();
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			if (entry.getName().equals(containerName)) {
				if (entry.remove(qualifiedName, true)) {
					fHasChanges= true;
					return true;
				}
			}
		}
		return false;
	}
	
	
	/**
	 * Removes an import from the structure.
	 * @param binding The type to remove from the imports
	 * @return Returns <code>true</code> if the import was found and removed
	 */
	public boolean removeImport(ITypeBinding binding) {
		binding= Bindings.normalizeTypeBinding(binding);
		if (binding == null) {
			return false;
		}		
		String qualifiedName= Bindings.getRawQualifiedName(binding);
		if (qualifiedName.length() > 0) {
			return removeImport(qualifiedName);
		}
		return false;
	}	

	/**
	 * Looks if there already is single import for the given type name.
	 * @param simpleName The simple name to find
	 * @return Returns the qualified import name or <code>null</code>.
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
		
	public String findStaticImport(String typeContainerName, String typeSimpleName, String simpleName) {		
		int nPackages= fPackageEntries.size();
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			if (typeContainerName == null || entry.getName().equals(typeContainerName)) {
				ImportDeclEntry found= entry.findStatic(typeSimpleName, simpleName);
				if (found != null) {
					return found.getElementName();
				}
			}
		}
		return null;		
	}
	
	/**
	 * Creates all new elements in the import structure.
	 * @param save Save the CU after the import have been changed
	 * @param monitor The progress monitor to use
	 * @throws CoreException Thrown when the access to the CU failed
	 */	
	public void create(boolean save, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(CodeGenerationMessages.getString("ImportsStructure.operation.description"), 4); //$NON-NLS-1$
		
		IDocument document= null;
		try {
			document= aquireDocument(new SubProgressMonitor(monitor, 1));
			MultiTextEdit edit= getResultingEdits(document, new SubProgressMonitor(monitor, 1));
			if (edit.hasChildren()) {

				if (save) {
					commitDocument(document, edit, new SubProgressMonitor(monitor, 1));
				} else {
					edit.apply(document);
				}
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		} finally {
			if (document != null) {
				releaseDocument(document, new SubProgressMonitor(monitor, 1));
			}
			monitor.done();
		}
	}
	
	private IDocument aquireDocument(IProgressMonitor monitor) throws CoreException {
		if (JavaModelUtil.isPrimary(fCompilationUnit)) {
			IFile file= (IFile) fCompilationUnit.getResource();
			if (file.exists()) {
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				IPath path= fCompilationUnit.getPath();
				bufferManager.connect(path, monitor);
				return bufferManager.getTextFileBuffer(path).getDocument();
			}
		}
		monitor.done();
		return new Document(fCompilationUnit.getSource());
	}
	
	private void releaseDocument(IDocument document, IProgressMonitor monitor) throws CoreException {
		if (JavaModelUtil.isPrimary(fCompilationUnit)) {
			IFile file= (IFile) fCompilationUnit.getResource();
			if (file.exists()) {
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				bufferManager.disconnect(file.getFullPath(), monitor);
				return;
			}
		}
		fCompilationUnit.getBuffer().setContents(document.get());
		monitor.done();
	}
	
	private void commitDocument(IDocument document, MultiTextEdit edit, IProgressMonitor monitor) throws CoreException, MalformedTreeException, BadLocationException {
		if (JavaModelUtil.isPrimary(fCompilationUnit)) {
			IFile file= (IFile) fCompilationUnit.getResource();
			if (file.exists()) {
				IStatus status= Resources.makeCommittable(file, null);
				if (!status.isOK()) {
					throw new ValidateEditException(status);
				}
				edit.apply(document); // apply after file is commitable
				
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				bufferManager.getTextFileBuffer(file.getFullPath()).commit(monitor, true);
				return;
			}
		}
		// no commit possible, make sure changes are in
		edit.apply(document);
	}
		
	private IRegion evaluateReplaceRange(IDocument document) throws JavaModelException, BadLocationException {
		JavaModelUtil.reconcile(fCompilationUnit);

		IImportContainer container= fCompilationUnit.getImportContainer();
		if (container.exists()) {
			ISourceRange importSourceRange= container.getSourceRange();
			int startPos= importSourceRange.getOffset();
			int endPos= startPos + importSourceRange.getLength();
			if (!Strings.isLineDelimiterChar(document.getChar(endPos - 1))) {
				// if not already after a new line, go to beginning of next line
				// (if last char in new line -> import ends with a comment, see 10557)
				int nextLine= document.getLineOfOffset(endPos) + 1;
				if (nextLine < document.getNumberOfLines()) {
					int stopPos= document.getLineInformation(nextLine).getOffset();
					// read to beginning of next character or beginning of next line
					while (endPos < stopPos && Character.isWhitespace(document.getChar(endPos))) {
						endPos++;
					}
				}
			}
			return new Region(startPos, endPos - startPos);
		} else {
			int start= getPackageStatementEndPos(document);
			return new Region(start, 0);
		}		
	}
	
	public MultiTextEdit getResultingEdits(IDocument document, IProgressMonitor monitor) throws JavaModelException, BadLocationException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
		
			int importsStart=  fReplaceRange.getOffset();
			int importsLen= fReplaceRange.getLength();
					
			String lineDelim= TextUtilities.getDefaultLineDelimiter(document);
			boolean useSpaceBetween= useSpaceBetweenGroups();
			
			StringBuffer buf= new StringBuffer();
					
			int nCreated= 0;
			PackageEntry lastPackage= null;
			
			Set onDemandConflicts= null;
			if (fFindAmbiguousImports) {
				onDemandConflicts= evaluateStarImportConflicts(monitor);
			}
			
			int nPackageEntries= fPackageEntries.size();
			for (int i= 0; i < nPackageEntries; i++) {
				PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
				int nImports= pack.getNumberOfImports();
	
				if (fFilterImplicitImports && isImplicitImport(pack.getName(), fCompilationUnit)) {
					pack.removeAllNewNonStatic();
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
							buf.append(lineDelim);
						}
					}
				}
				lastPackage= pack;
				
				boolean doStarImport= pack.hasStarImport(fImportOnDemandThreshold, onDemandConflicts, false);
				if (doStarImport && (pack.find("*") == null)) { //$NON-NLS-1$
					String starImportString= pack.getName() + ".*"; //$NON-NLS-1$
					appendImportToBuffer(buf, starImportString, false, lineDelim);
					nCreated++;
				}
				
				for (int k= 0; k < nImports; k++) {
					ImportDeclEntry currDecl= pack.getImportAt(k);
					boolean isStatic= currDecl.isStatic();
					String content= currDecl.getContent();
					
					if (content == null) { // new entry
						if (!doStarImport || currDecl.isOnDemand() || isStatic || (onDemandConflicts != null && onDemandConflicts.contains(currDecl.getSimpleName()))) {
							appendImportToBuffer(buf, currDecl.getElementName(), isStatic, lineDelim);
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
			
			String newContent= buf.toString();
			String oldContent= document.get(importsStart, importsLen);
			
			return getTextEdit(oldContent, importsStart, newContent);
		} finally {
			monitor.done();
		}
	}
	
	private MultiTextEdit getTextEdit(String oldContent, int offset, String newContent) {
		MultiTextEdit edit= new MultiTextEdit();
		
		ITokenComparator leftSide= new JavaTokenComparator(newContent, false); 
		ITokenComparator rightSide= new JavaTokenComparator(oldContent, false);
		
		RangeDifference[] differences= RangeDifferencer.findRanges(leftSide, rightSide);
		for (int i= 0; i < differences.length; i++) {
			RangeDifference curr= differences[i];
			if (curr.kind() == RangeDifference.CHANGE) {
				int oldStart= rightSide.getTokenStart(curr.rightStart());
				int oldEnd= rightSide.getTokenStart(curr.rightEnd());
				int newStart= leftSide.getTokenStart(curr.leftStart());
				int newEnd= leftSide.getTokenStart(curr.leftEnd());
				String newString= newContent.substring(newStart, newEnd);
				edit.addChild(new ReplaceEdit(offset + oldStart, oldEnd - oldStart, newString));
			}
		}
		return edit;
	}
	
	
	/**
	 * @return  Probes if the formatter allows spaces between imports
	 */
	private boolean useSpaceBetweenGroups() {
		try {
			String sample= "import a.A;\n\n import b.B;\nclass C {}"; //$NON-NLS-1$
			TextEdit res= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, sample, 0, String.valueOf('\n'), fCompilationUnit.getJavaProject().getOptions(true));
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

	private Set evaluateStarImportConflicts(IProgressMonitor monitor) {
		int nPackageEntries= fPackageEntries.size();
		HashSet starImportPackages= new HashSet(nPackageEntries);
		for (int i= 0; i < nPackageEntries; i++) {
			PackageEntry pack= (PackageEntry) fPackageEntries.get(i);
			if (pack.hasStarImport(fImportOnDemandThreshold, null, false)) {
				starImportPackages.add(pack.getName());
			}
		}
		if (starImportPackages.isEmpty()) {
			return null;
		}
		
		starImportPackages.add(fCompilationUnit.getParent().getElementName());
		starImportPackages.add(JAVA_LANG);
		
		TypeInfo[] allTypes= AllTypesCache.getAllTypes(monitor); // all types in workspace, sorted by type name
		if (allTypes.length < 2) {
			return null;
		}
		
		HashSet onDemandConflicts= new HashSet();
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fCompilationUnit.getJavaProject() });
		String curr= allTypes[0].getTypeName();
		int offset= 0;
		int i= 1;
		while (i < allTypes.length) {
			String name= allTypes[i].getTypeName();
			if (!name.equals(curr)) {
				if (i - offset > 1 && isConflictingTypeName(allTypes, offset, i, starImportPackages, scope)) {
					onDemandConflicts.add(curr);
				}
				curr= name;
				offset= i;
			}
			i++;
		}
		if (i - offset > 1 && isConflictingTypeName(allTypes, offset, i, starImportPackages, scope)) {
			onDemandConflicts.add(curr);
		}
		return onDemandConflicts;
	}
	
	private boolean isConflictingTypeName(TypeInfo[] allTypes, int start, int end, HashSet starImportPackages, IJavaSearchScope scope) {
		String conflictingContainer= null; // set only when entry is validated to be conflicting
		for (int i= start; i < end; i++) {
			TypeInfo curr= allTypes[i];
			String containerName= curr.getTypeContainerName();
			if (!containerName.equals(conflictingContainer) && starImportPackages.contains(containerName) && curr.isEnclosed(scope)) {
				if (conflictingContainer != null) {
					return true;
				}
				conflictingContainer= containerName;
			}
		}
		return false;
	}
	
	private void appendImportToBuffer(StringBuffer buf, String importName, boolean isStatic, String lineDelim) {
		buf.append("import "); //$NON-NLS-1$
		if (isStatic) {
			buf.append("static "); //$NON-NLS-1$
		}
		buf.append(importName);
		buf.append(';');
		buf.append(lineDelim);
		// str= StubUtility.codeFormat(str, 0, lineDelim);
		
		if (fImportsCreated != null) {
			fImportsCreated.add(importName);
		}
	}
	
	private int getPackageStatementEndPos(IDocument document) throws JavaModelException, BadLocationException {
		IPackageDeclaration[] packDecls= fCompilationUnit.getPackageDeclarations();
		if (packDecls != null && packDecls.length > 0) {
			ISourceRange range= packDecls[0].getSourceRange();
			int line= document.getLineOfOffset(range.getOffset() + range.getLength());
			IRegion region= document.getLineInformation(line + 1);
			if (region != null) {
				IType[] types= fCompilationUnit.getTypes();
				if (types.length > 0) {
					return Math.min(types[0].getSourceRange().getOffset(), region.getOffset());
				}
				return region.getOffset();
			}
		}
		return 0;
	}
	
	public String toString() {
		int nPackages= fPackageEntries.size();
		StringBuffer buf= new StringBuffer("\n-----------------------\n"); //$NON-NLS-1$
		for (int i= 0; i < nPackages; i++) {
			PackageEntry entry= (PackageEntry) fPackageEntries.get(i);
			buf.append(entry.toString());
		}
		return buf.toString();	
	}
	
	private static final class ImportDeclEntry {
		
		private String fElementName;
		private String fContent;
		private final boolean fIsStatic;
		
		public ImportDeclEntry(String elementName, String existingContent, boolean isStatic) {
			fElementName= elementName;
			fContent= existingContent;
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
	private final static class PackageEntry {
		private String fName;
		private ArrayList fImportEntries;
		private String fGroup;
	
		/**
		 * Comment package entry
		 */
		public PackageEntry() {
			this("!", null); //$NON-NLS-1$
		}
		

		/**
		 * Comment group place holder entry (name equals group)
		 * @param name Name of the package entry. e.g. org.eclipse.jdt.ui, containing imports like
		 * org.eclipse.jdt.ui.JavaUI.
		 */
		public PackageEntry(String name) {
			this(name, name); //$NON-NLS-1$
		}	
	
		/**
		 * @param name Name of the package entry. e.g. org.eclipse.jdt.ui, containing imports like
		 * org.eclipse.jdt.ui.JavaUI.
		 * @param group The index of the preference order entry assigned
		 *    different group id's will result in spacers between the entries
		 */
		public PackageEntry(String name, String group) {
			fName= name;
			fImportEntries= new ArrayList(5);
			fGroup= group;
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
				if (!curr.isComment() && !curr.isStatic()) {
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
		
		public ImportDeclEntry findStatic(String typeSimpleName, String simpleName) {
			int nInports= fImportEntries.size();
			for (int i= 0; i < nInports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (!curr.isComment() && curr.isStatic()) {
					String name= curr.getElementName();
					int idx= name.lastIndexOf('.');
					if (idx > 0 && simpleName.equals(name.substring(idx + 1))) { // not -1 and not 0
						if (typeSimpleName != null) {
							int prexIdx= name.lastIndexOf('.', idx - 1);
							if (prexIdx != -1 && typeSimpleName.equals(name.substring(prexIdx + 1, idx))) { 
								return curr;
							}
						} else {
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
		
		public void removeAllNewNonStatic() {
			int nInports= fImportEntries.size();
			for (int i= nInports - 1; i >= 0; i--) {
				ImportDeclEntry curr= getImportAt(i);
				if (curr.isNew() && !curr.isStatic()) {
					fImportEntries.remove(i);
				}
			}
		}
		
		public ImportDeclEntry getImportAt(int index) {
			return (ImportDeclEntry) fImportEntries.get(index);
		}
		
		public boolean hasStarImport(int threshold, Set explicitImports, boolean forStaticImports) {
			if (isComment() || isDefaultPackage()) { // can not star import default package
				return false;
			}
			int nImports= getNumberOfImports();
			int count= 0;
			boolean containsNew= false;
			for (int i= 0; i < nImports; i++) {
				ImportDeclEntry curr= getImportAt(i);
				if (curr.isStatic() == forStaticImports) {
					if (curr.isOnDemand()) {
						return true;
					}
					if (!curr.isComment()) {
						count++;
						boolean isExplicit= (explicitImports != null) && explicitImports.contains(curr.getSimpleName());
						containsNew |= curr.isNew() && !isExplicit;
					}
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
				return fGroup.equals(other.getGroupID());
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
	
	public void setCreatedImportCollector(ArrayList list) {
	    fImportsCreated= list;
	}

	/**
	 * Returns <code>true</code> if imports have been added or removed.
	 * @return boolean
	 */
	public boolean hasChanges() {
		return fHasChanges;
	}



}
