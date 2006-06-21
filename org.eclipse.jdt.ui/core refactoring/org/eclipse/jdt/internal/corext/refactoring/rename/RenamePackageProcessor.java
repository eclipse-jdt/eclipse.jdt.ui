/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenamePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor.ImportsManager.ImportChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.Changes;
import org.eclipse.jdt.internal.corext.refactoring.util.CommentAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

public class RenamePackageProcessor extends JavaRenameProcessor implements IReferenceUpdating, ITextUpdating, IQualifiedNameUpdating {
	
	private static final String ATTRIBUTE_QUALIFIED= "qualified"; //$NON-NLS-1$
	private static final String ATTRIBUTE_REFERENCES= "references"; //$NON-NLS-1$
	private static final String ATTRIBUTE_TEXTUAL_MATCHES= "textual"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PATTERNS= "patterns"; //$NON-NLS-1$
	private static final String ATTRIBUTE_HIERARCHICAL= "hierarchical"; //$NON-NLS-1$

	private IPackageFragment fPackage;
	
	private TextChangeManager fChangeManager;
	private ImportsManager fImportsManager;
	private QualifiedNameSearchResult fQualifiedNameSearchResult;
	
	private boolean fUpdateReferences;
	private boolean fUpdateTextualMatches;
	private boolean fUpdateQualifiedNames;
	private String fFilePatterns;
	private boolean fRenameSubpackages;

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renamePackageProcessor"; //$NON-NLS-1$

	/**
	 * Creates a new rename package processor.
	 * @param fragment the package fragment, or <code>null</code> if invoked by scripting
	 */
	public RenamePackageProcessor(IPackageFragment fragment) {
		fPackage= fragment;
		if (fPackage != null)
			setNewElementName(fPackage.getElementName());
		fUpdateReferences= true;
		fUpdateTextualMatches= false;
		fRenameSubpackages= false;
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fPackage);
	}
	
	public String getProcessorName(){
		return RefactoringCoreMessages.RenamePackageRefactoring_name;
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fPackage);
	}
	
	public Object[] getElements() {
		return new Object[] {fPackage};
	}
	
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fPackage, new RenameArguments(getNewElementName(), getUpdateReferences()), fRenameSubpackages);
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		Set combined= new HashSet();
		combined.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
		if (fRenameSubpackages) {
			IPackageFragment[] allPackages= JavaElementUtil.getPackageAndSubpackages(fPackage);
			for (int i= 0; i < allPackages.length; i++) {
				combined.addAll(Arrays.asList(ResourceUtil.getFiles(allPackages[i].getCompilationUnits())));
			}
		} else {
			combined.addAll(Arrays.asList(ResourceUtil.getFiles(fPackage.getCompilationUnits())));
		}
		if (fQualifiedNameSearchResult != null)
			combined.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
		return (IFile[]) combined.toArray(new IFile[combined.size()]);
	}
	
	//---- ITextUpdating -------------------------------------------------

	public boolean canEnableTextUpdating() {
		return true;
	}
	
	public boolean getUpdateTextualMatches() {
		return fUpdateTextualMatches;
	}

	public void setUpdateTextualMatches(boolean update) {
		fUpdateTextualMatches= update;
	}

	//---- IReferenceUpdating --------------------------------------
		
	public boolean canEnableUpdateReferences() {
		return true;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}	
	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}
	
	//---- IQualifiedNameUpdating ----------------------------------

	public boolean canEnableQualifiedNameUpdating() {
		return !fPackage.isDefaultPackage();
	}
	
	public boolean getUpdateQualifiedNames() {
		return fUpdateQualifiedNames;
	}
	
	public void setUpdateQualifiedNames(boolean update) {
		fUpdateQualifiedNames= update;
	}
	
	public String getFilePatterns() {
		return fFilePatterns;
	}
	
	public void setFilePatterns(String patterns) {
		Assert.isNotNull(patterns);
		fFilePatterns= patterns;
	}
	
	//----
	
	public boolean canEnableRenameSubpackages() throws JavaModelException {
		return fPackage.hasSubpackages();
	}
	
	public boolean getRenameSubpackages() {
		return fRenameSubpackages;
	}

	public void setRenameSubpackages(boolean rename) {
		fRenameSubpackages= rename;
	}	
	
	//---- IRenameProcessor ----------------------------------------------
	
	public final String getCurrentElementName(){
		return fPackage.getElementName();
	}
	
	public String getCurrentElementQualifier() {
		return ""; //$NON-NLS-1$
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkPackageName(newName);
		if (Checks.isAlreadyNamed(fPackage, newName))
			result.addFatalError(RefactoringCoreMessages.RenamePackageRefactoring_another_name); 
		result.merge(checkPackageInCurrentRoot(newName));
		return result;
	}
	
	public Object getNewElement(){
		IJavaElement parent= fPackage.getParent();
		if (!(parent instanceof IPackageFragmentRoot))
			return fPackage;//??
			
		IPackageFragmentRoot root= (IPackageFragmentRoot)parent;
		return root.getPackageFragment(getNewElementName());
	}
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}
	
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			pm.beginTask("", 23 + (fUpdateQualifiedNames ? 10 : 0)); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.RenamePackageRefactoring_checking); 
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewElementName(getNewElementName()));
			pm.worked(1);
			result.merge(checkForMainAndNativeMethods());
			pm.worked(2);
			
			if (fPackage.isReadOnly()){
				String message= Messages.format(RefactoringCoreMessages.RenamePackageRefactoring_Packagered_only, fPackage.getElementName()); 
				result.addFatalError(message);
			} else if (Resources.isReadOnly(fPackage.getResource())) {
				String message= Messages.format(RefactoringCoreMessages.RenamePackageRefactoring_resource_read_only, fPackage.getElementName());
				result.addError(message);
			}				
				
			result.merge(checkPackageName(getNewElementName()));
			if (result.hasFatalError())
				return result;
			
			fChangeManager= new TextChangeManager();
			fImportsManager= new ImportsManager();
			
			SubProgressMonitor subPm= new SubProgressMonitor(pm, 16);
			if (fRenameSubpackages) {
				IPackageFragment[] allSubpackages= JavaElementUtil.getPackageAndSubpackages(fPackage);
				subPm.beginTask("", allSubpackages.length); //$NON-NLS-1$
				for (int i= 0; i < allSubpackages.length; i++) {
					new PackageRenamer(allSubpackages[i], this, fChangeManager, fImportsManager).doRename(new SubProgressMonitor(subPm, 1), result);
				}
				subPm.done();
			} else {
				new PackageRenamer(fPackage, this, fChangeManager, fImportsManager).doRename(subPm, result);
			}
			
			fImportsManager.rewriteImports(fChangeManager, new SubProgressMonitor(pm, 3));
			
			if (fUpdateQualifiedNames)
				computeQualifiedNameMatches(new SubProgressMonitor(pm, 10));
			
			return result;
		} finally{
			pm.done();
		}	
	}
	
	public IPackageFragment getPackage() {
		return fPackage;
	}
	
	private RefactoringStatus checkForMainAndNativeMethods() throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		if (fRenameSubpackages) {
			IPackageFragment[] allSubpackages= JavaElementUtil.getPackageAndSubpackages(fPackage);
			for (int i= 0; i < allSubpackages.length; i++) {
				ICompilationUnit[] cus= allSubpackages[i].getCompilationUnits();
				for (int c= 0; c < cus.length; c++)
					result.merge(Checks.checkForMainAndNativeMethods(cus[c]));
			}
		} else {
			ICompilationUnit[] cus= fPackage.getCompilationUnits();
			for (int i= 0; i < cus.length; i++)
				result.merge(Checks.checkForMainAndNativeMethods(cus[i]));
		}
		return result;
	}
	
	/*
	 * returns true if the new name is ok if the specified root.
	 * if a package fragment with this name exists and has java resources,
	 * then the name is not ok.
	 */
	public static boolean isPackageNameOkInRoot(String newName, IPackageFragmentRoot root) throws CoreException {
		IPackageFragment pack= root.getPackageFragment(newName);
		if (! pack.exists())
			return true;
		else if (! pack.hasSubpackages()) //leaves are no good
			return false;			
		else if (pack.containsJavaResources())
			return false;
		else if (pack.getNonJavaResources().length != 0)
			return false;
		else 
			return true;	
	}
	
	private RefactoringStatus checkPackageInCurrentRoot(String newName) throws CoreException {
		if (isPackageNameOkInRoot(newName, getPackageFragmentRoot()))
			return null;
		else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenamePackageRefactoring_package_exists);
	}

	private IPackageFragmentRoot getPackageFragmentRoot() {
		return ((IPackageFragmentRoot)fPackage.getParent());
	}
	
	private RefactoringStatus checkPackageName(String newName) throws CoreException {		
		RefactoringStatus status= new RefactoringStatus();
		IPackageFragmentRoot[] roots= fPackage.getJavaProject().getPackageFragmentRoots();
		Set topLevelTypeNames= getTopLevelTypeNames();
		for (int i= 0; i < roots.length; i++) {
			if (! isPackageNameOkInRoot(newName, roots[i])){
				String message= Messages.format(RefactoringCoreMessages.RenamePackageRefactoring_aleady_exists, new Object[]{getNewElementName(), roots[i].getElementName()});
				status.merge(RefactoringStatus.createWarningStatus(message));
				status.merge(checkTypeNameConflicts(roots[i], newName, topLevelTypeNames)); 
			}
		}
		return status;
	}
	
	private Set getTopLevelTypeNames() throws CoreException {
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		Set result= new HashSet(2 * cus.length); 
		for (int i= 0; i < cus.length; i++) {
			result.addAll(getTopLevelTypeNames(cus[i]));
		}
		return result;
	}
	
	private static Collection getTopLevelTypeNames(ICompilationUnit iCompilationUnit) throws CoreException {
		IType[] types= iCompilationUnit.getTypes();
		List result= new ArrayList(types.length);
		for (int i= 0; i < types.length; i++) {
			result.add(types[i].getElementName());
		}
		return result;
	}
	
	private RefactoringStatus checkTypeNameConflicts(IPackageFragmentRoot root, String newName, Set topLevelTypeNames) throws CoreException {
		IPackageFragment otherPack= root.getPackageFragment(newName);
		if (fPackage.equals(otherPack))
			return null;
		ICompilationUnit[] cus= otherPack.getCompilationUnits();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++) {
			result.merge(checkTypeNameConflicts(cus[i], topLevelTypeNames));
		}
		return result;
	}
	
	private RefactoringStatus checkTypeNameConflicts(ICompilationUnit iCompilationUnit, Set topLevelTypeNames) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] types= iCompilationUnit.getTypes();
		String packageName= iCompilationUnit.getParent().getElementName();
		for (int i= 0; i < types.length; i++) {
			String name= types[i].getElementName();
			if (topLevelTypeNames.contains(name)){
				String[] keys= {packageName, name};
				String msg= Messages.format(RefactoringCoreMessages.RenamePackageRefactoring_contains_type, keys); 
				RefactoringStatusContext context= JavaStatusContext.create(types[i]);
				result.addError(msg, context);
			}	
		}
		return result;
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.RenamePackageRefactoring_creating_change, 1);
			final Map arguments= new HashMap();
			String project= null;
			IJavaProject javaProject= fPackage.getJavaProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			final int flags= JDTRefactoringDescriptor.JAR_IMPORTABLE | JDTRefactoringDescriptor.JAR_REFACTORABLE | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final String description= Messages.format(RefactoringCoreMessages.RenamePackageProcessor_descriptor_description_short, fPackage.getElementName());
			final String header= Messages.format(RefactoringCoreMessages.RenamePackageProcessor_descriptor_description, new String[] { fPackage.getElementName(), getNewElementName()});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			if (fRenameSubpackages)
				comment.addSetting(RefactoringCoreMessages.RenamePackageProcessor_rename_subpackages);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaRefactorings.RENAME_PACKAGE, project, description, comment.asString(), arguments, flags);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fPackage));
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, getNewElementName());
			if (fFilePatterns != null && !"".equals(fFilePatterns)) //$NON-NLS-1$
				arguments.put(ATTRIBUTE_PATTERNS, fFilePatterns);
			arguments.put(ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
			arguments.put(ATTRIBUTE_QUALIFIED, Boolean.valueOf(fUpdateQualifiedNames).toString());
			arguments.put(ATTRIBUTE_TEXTUAL_MATCHES, Boolean.valueOf(fUpdateTextualMatches).toString());
			arguments.put(ATTRIBUTE_HIERARCHICAL, Boolean.valueOf(fRenameSubpackages).toString());
			final DynamicValidationRefactoringChange result= new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.RenamePackageRefactoring_change_name);
			result.addAll(fChangeManager.getAllChanges());
			result.add(new RenamePackageChange(null, fPackage, getNewElementName(), comment.asString(), fRenameSubpackages));
			monitor.worked(1);
			return result;
		} finally {
			fChangeManager= null;
			fImportsManager= null;
			monitor.done();
		}
	}

	public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
		if (fQualifiedNameSearchResult != null) {
			try {
				return fQualifiedNameSearchResult.getSingleChange(Changes.getModifiedFiles(participantChanges));
			} finally {
				fQualifiedNameSearchResult= null;
			}
		} else {
			return null;
		}
	}
	
	private void computeQualifiedNameMatches(IProgressMonitor pm) throws CoreException {
		if (fQualifiedNameSearchResult == null)
			fQualifiedNameSearchResult= new QualifiedNameSearchResult();
		QualifiedNameFinder.process(fQualifiedNameSearchResult, fPackage.getElementName(), getNewElementName(), 
			fFilePatterns, fPackage.getJavaProject().getProject(), pm);
	}

	public String getNewPackageName(String oldSubPackageName) {
		String oldPackageName= getPackage().getElementName();
		return getNewElementName() + oldSubPackageName.substring(oldPackageName.length());
	}
	
	private static class PackageRenamer {
		private final IPackageFragment fPackage;
		private final RenamePackageProcessor fProcessor;
		private final TextChangeManager fTextChangeManager;
		private final ImportsManager fImportsManager;
		
		/** references to fPackage (can include star imports which also import namesake package fragments) */
		private SearchResultGroup[] fOccurrences;
		
		/** References in CUs from fOccurrences and fPackage to types in namesake packages.
		 * <p>These need an import with the old package name.
		 * <p>- from fOccurrences (without namesakes): may have shared star import
		 * 		(star-import not updated here, but for fOccurrences)
		 * <p>- from fPackage: may have unimported references to types of namesake packages
		 * <p>- both: may have unused imports of namesake packages.
		 * <p>Mutable List of SearchResultGroup. */
		private List fReferencesToTypesInNamesakes;
	
		/** References in CUs from namesake packages to types in fPackage.
		 * <p>These need an import with the new package name.
		 * <p>Mutable List of SearchResultGroup. */
		private List fReferencesToTypesInPackage;
		
		public PackageRenamer(IPackageFragment pack, RenamePackageProcessor processor, TextChangeManager textChangeManager, ImportsManager importsManager) {
			fPackage= pack;
			fProcessor= processor;
			fTextChangeManager= textChangeManager;
			fImportsManager= importsManager;
		}
	
		void doRename(IProgressMonitor pm, RefactoringStatus result) throws CoreException {
			pm.beginTask("", 16 + (fProcessor.getUpdateTextualMatches() ? 10 : 0)); //$NON-NLS-1$
			if (fProcessor.getUpdateReferences()){
				pm.setTaskName(RefactoringCoreMessages.RenamePackageRefactoring_searching);	 
				fOccurrences= getReferences(new SubProgressMonitor(pm, 4), result);	
				fReferencesToTypesInNamesakes= getReferencesToTypesInNamesakes(new SubProgressMonitor(pm, 4), result);
				fReferencesToTypesInPackage= getReferencesToTypesInPackage(new SubProgressMonitor(pm, 4), result);
				pm.setTaskName(RefactoringCoreMessages.RenamePackageRefactoring_checking); 
				result.merge(analyzeAffectedCompilationUnits());
				pm.worked(1);
			} else {
				fOccurrences= new SearchResultGroup[0];
				pm.worked(13);
			}
		
			if (result.hasFatalError())
				return;
			
			if (fProcessor.getUpdateReferences())
				addReferenceUpdates(new SubProgressMonitor(pm, 3));
			else
				pm.worked(3);
			
			if (fProcessor.getUpdateTextualMatches() && fPackage.equals(fProcessor.getPackage())) {
				pm.subTask(RefactoringCoreMessages.RenamePackageRefactoring_searching_text); 
				addTextMatches(new SubProgressMonitor(pm, 10));
			}
			
			pm.done();
		}
	
		private SearchResultGroup[] getReferences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
			IJavaSearchScope scope= RefactoringScopeFactory.create(fPackage);
			SearchPattern pattern= SearchPattern.createPattern(fPackage, IJavaSearchConstants.REFERENCES);
			return RefactoringSearchEngine.search(pattern, scope, pm, status);
		}
		
		private void addReferenceUpdates(IProgressMonitor pm) throws CoreException {
			pm.beginTask("", fOccurrences.length + fReferencesToTypesInPackage.size() + fReferencesToTypesInNamesakes.size()); //$NON-NLS-1$
			for (int i= 0; i < fOccurrences.length; i++){
				ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
				if (cu == null)
					continue;
				SearchMatch[] results= fOccurrences[i].getSearchResults();
				for (int j= 0; j < results.length; j++){
					SearchMatch result= results[j];
					IJavaElement enclosingElement= SearchUtils.getEnclosingJavaElement(result);
					if (enclosingElement instanceof IImportDeclaration) {
						IImportDeclaration importDeclaration= (IImportDeclaration) enclosingElement;
						String updatedImport= getUpdatedImport(importDeclaration);
						updateImport(cu, importDeclaration, updatedImport);
					} else { // is reference 
						TextChangeCompatibility.addTextEdit(fTextChangeManager.get(cu), RefactoringCoreMessages.RenamePackageRefactoring_update_reference, createTextChange(result));
					}
				}
				if (fReferencesToTypesInNamesakes.size() != 0) {
					SearchResultGroup typeRefsRequiringOldNameImport= extractGroupFor(cu, fReferencesToTypesInNamesakes);
					if (typeRefsRequiringOldNameImport != null)
						addTypeImports(typeRefsRequiringOldNameImport);
				}
				if (fReferencesToTypesInPackage.size() != 0) {
					SearchResultGroup typeRefsRequiringNewNameImport= extractGroupFor(cu, fReferencesToTypesInPackage);
					if (typeRefsRequiringNewNameImport != null)
						updateTypeImports(typeRefsRequiringNewNameImport);
				}
				pm.worked(1);
			}	
	
			if (fReferencesToTypesInNamesakes.size() != 0) {
				for (Iterator iter= fReferencesToTypesInNamesakes.iterator(); iter.hasNext();) {
					SearchResultGroup referencesToTypesInNamesakes= (SearchResultGroup) iter.next();
					addTypeImports(referencesToTypesInNamesakes);
					pm.worked(1);
				}
			}
			if (fReferencesToTypesInPackage.size() != 0) {
				for (Iterator iter= fReferencesToTypesInPackage.iterator(); iter.hasNext();) {
					SearchResultGroup namesakeReferencesToPackage= (SearchResultGroup) iter.next();
					updateTypeImports(namesakeReferencesToPackage);
					pm.worked(1);
				}
			} 
			pm.done();
		}
	
		/** Removes the found SearchResultGroup from the list iff found.
		 *  @param searchResultGroups List of SearchResultGroup
		 *  @return the SearchResultGroup for cu, or null iff not found */
		private static SearchResultGroup extractGroupFor(ICompilationUnit cu, List searchResultGroups) {
			for (Iterator iter= searchResultGroups.iterator(); iter.hasNext();) {
				SearchResultGroup group= (SearchResultGroup) iter.next();
				if (cu.equals(group.getCompilationUnit())) {
					iter.remove();
					return group;
				}
			}
			return null;
		}
		
		private TextEdit createTextChange(SearchMatch searchResult) {
			return new ReplaceEdit(searchResult.getOffset(), searchResult.getLength(), getNewPackageName());
		}
		
		private void addTextMatches(IProgressMonitor pm) throws CoreException {
			//TODO: check what TextMatchUpdater does with fProcessor
			//fOccurrences is enough; the others are only import statements
			TextMatchUpdater.perform(pm, RefactoringScopeFactory.create(fPackage), fProcessor, fTextChangeManager, fOccurrences);
		}
	
		private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException {
			//TODO: also for both fReferencesTo...; only check each CU once!
			RefactoringStatus result= new RefactoringStatus();
			fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
			if (result.hasFatalError())
				return result;
			
			result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
			return result;
		}
	
		/**
		 * @return search scope with
		 * <p>- fPackage and
		 * <p>- all CUs from fOccurrences which are not in a namesake package
		 */
		private IJavaSearchScope getPackageAndOccurrencesWithoutNamesakesScope() {
			List scopeList= new ArrayList();
			scopeList.add(fPackage);
			for (int i= 0; i < fOccurrences.length; i++) {
				ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
				if (cu == null)
					continue;
				IPackageFragment pack= (IPackageFragment) cu.getParent();
				if (! pack.getElementName().equals(fPackage.getElementName()))
					scopeList.add(cu);
			}
			return SearchEngine.createJavaSearchScope((IJavaElement[]) scopeList.toArray(new IJavaElement[scopeList.size()])); 
		}
	
		private List getReferencesToTypesInNamesakes(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
			pm.beginTask("", 2); //$NON-NLS-1$
			// e.g. renaming B-p.p; project C requires B, X and has ref to B-p.p and X-p.p;
			// goal: find refs to X-p.p in CUs from fOccurrences
			
			// (1) find namesake packages (scope: all packages referenced by CUs in fOccurrences and fPackage)
			IJavaElement[] elements= new IJavaElement[fOccurrences.length + 1]; 
			for (int i= 0; i < fOccurrences.length; i++) {
				elements[i]= fOccurrences[i].getCompilationUnit();
			}
			elements[fOccurrences.length]= fPackage;
			IJavaSearchScope namesakePackagesScope= RefactoringScopeFactory.createReferencedScope(elements);
			IPackageFragment[] namesakePackages= getNamesakePackages(namesakePackagesScope, new SubProgressMonitor(pm, 1));
			if (namesakePackages.length == 0) {
				pm.done();
				return new ArrayList(0);
			}
			
			// (2) find refs in fOccurrences and fPackage to namesake packages
			// (from fOccurrences (without namesakes): may have shared star import)
			// (from fPackage: may have unimported references to types of namesake packages)
			IType[] typesToSearch= getTypesInPackages(namesakePackages);
			if (typesToSearch.length == 0) {
				pm.done();
				return new ArrayList(0);
			}
			SearchPattern pattern= RefactoringSearchEngine.createOrPattern(typesToSearch, IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope= getPackageAndOccurrencesWithoutNamesakesScope();
			SearchResultGroup[] results= RefactoringSearchEngine.search(pattern, scope, new SubProgressMonitor(pm, 1), status);
			pm.done();
			return new ArrayList(Arrays.asList(results));
		}
	
		private List getReferencesToTypesInPackage(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
			pm.beginTask("", 2); //$NON-NLS-1$
			IJavaSearchScope referencedFromNamesakesScope= RefactoringScopeFactory.create(fPackage);
			IPackageFragment[] namesakePackages= getNamesakePackages(referencedFromNamesakesScope, new SubProgressMonitor(pm, 1));
			if (namesakePackages.length == 0) {
				pm.done();
				return new ArrayList(0);
			}
		
			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(namesakePackages);
			IType[] typesToSearch= getTypesInPackage(fPackage);
			if (typesToSearch.length == 0) {
				pm.done();
				return new ArrayList(0);
			}
			SearchPattern pattern= RefactoringSearchEngine.createOrPattern(typesToSearch, IJavaSearchConstants.REFERENCES);
			SearchResultGroup[] results= RefactoringSearchEngine.search(pattern, scope, new SubProgressMonitor(pm, 1), status);
			pm.done();
			return new ArrayList(Arrays.asList(results));
		}
	
		private IType[] getTypesInPackage(IPackageFragment packageFragment) throws JavaModelException {
			List types= new ArrayList();
			addContainedTypes(packageFragment, types);
			return (IType[]) types.toArray(new IType[types.size()]);
		}
	
		/**
		 * @return all package fragments in <code>scope</code> with same name as <code>fPackage</code>, excluding fPackage
		 */
		private IPackageFragment[] getNamesakePackages(IJavaSearchScope scope, IProgressMonitor pm) throws CoreException {
			SearchPattern pattern= SearchPattern.createPattern(fPackage.getElementName(), IJavaSearchConstants.PACKAGE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
			
			final List packageFragments= new ArrayList();
			SearchRequestor requestor= new SearchRequestor() {
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (RefactoringSearchEngine.isFiltered(match))
						return;
					IJavaElement enclosingElement= SearchUtils.getEnclosingJavaElement(match);
					if (enclosingElement instanceof IPackageFragment) {
						IPackageFragment pack= (IPackageFragment) enclosingElement;
						if (! fPackage.equals(pack))
							packageFragments.add(pack);
					}
				}
			};
			new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, pm);
			
			return (IPackageFragment[]) packageFragments.toArray(new IPackageFragment[packageFragments.size()]);
		}
		
		private IType[] getTypesInPackages(IPackageFragment[] packageFragments) throws JavaModelException {
			List types= new ArrayList();
			for (int i= 0; i < packageFragments.length; i++) {
				IPackageFragment pack= packageFragments[i];
				addContainedTypes(pack, types);
			}
			return (IType[]) types.toArray(new IType[types.size()]);
		}
	
		private void addContainedTypes(IPackageFragment pack, List typesCollector) throws JavaModelException {
			IJavaElement[] children= pack.getChildren();
			for (int c= 0; c < children.length; c++) {
				IJavaElement child= children[c];
				if (child instanceof ICompilationUnit) {
					typesCollector.addAll(Arrays.asList(((ICompilationUnit) child).getTypes()));
				}
			}
		}
		
		private void updateImport(ICompilationUnit cu, IImportDeclaration importDeclaration, String updatedImport) throws JavaModelException {
			ImportChange importChange= fImportsManager.getImportChange(cu);
			if (Flags.isStatic(importDeclaration.getFlags())) {
				importChange.removeStaticImport(importDeclaration.getElementName());
				importChange.addStaticImport(Signature.getQualifier(updatedImport), Signature.getSimpleName(updatedImport));
			} else {
				importChange.removeImport(importDeclaration.getElementName());
				importChange.addImport(updatedImport);
			}
		}
		
		/**
		 * Add new imports to types in <code>typeReferences</code> with package <code>fPackage</code>.
		 */
		private void addTypeImports(SearchResultGroup typeReferences) throws CoreException {
			SearchMatch[] searchResults= typeReferences.getSearchResults();
			for (int i= 0; i < searchResults.length; i++) {
				SearchMatch result= searchResults[i];
				IJavaElement enclosingElement= SearchUtils.getEnclosingJavaElement(result);
				if (! (enclosingElement instanceof IImportDeclaration)) {
					String reference= getNormalizedTypeReference(result);
					if (! reference.startsWith(fPackage.getElementName())) {
						// is unqualified
						reference= cutOffInnerTypes(reference);
						ImportChange importChange= fImportsManager.getImportChange(typeReferences.getCompilationUnit());
						importChange.addImport(fPackage.getElementName() + '.' + reference);
					}
				}
			}
		}
	
		/**
		 * Add new imports to types in <code>typeReferences</code> with package <code>fNewElementName</code>
		 * and remove old import with <code>fPackage</code>.
		 */
		private void updateTypeImports(SearchResultGroup typeReferences) throws CoreException {
			SearchMatch[] searchResults= typeReferences.getSearchResults();
			for (int i= 0; i < searchResults.length; i++) {
				SearchMatch result= searchResults[i];
				IJavaElement enclosingElement= SearchUtils.getEnclosingJavaElement(result);
				if (enclosingElement instanceof IImportDeclaration) {
					IImportDeclaration importDeclaration= (IImportDeclaration) enclosingElement;
					updateImport(typeReferences.getCompilationUnit(), importDeclaration, getUpdatedImport(importDeclaration));
				} else {
					String reference= getNormalizedTypeReference(result);
					if (! reference.startsWith(fPackage.getElementName())) {
						reference= cutOffInnerTypes(reference);
						ImportChange importChange= fImportsManager.getImportChange(typeReferences.getCompilationUnit());
						importChange.removeImport(fPackage.getElementName() + '.' + reference);
						importChange.addImport(getNewPackageName() + '.' + reference);
					} // else: already found & updated with package reference search
				}
			}
		}
		
		private static String getNormalizedTypeReference(SearchMatch searchResult) throws JavaModelException {
			ICompilationUnit cu= SearchUtils.getCompilationUnit(searchResult);
			String reference= cu.getBuffer().getText(searchResult.getOffset(), searchResult.getLength());
			//reference may be package-qualified -> normalize (remove comments, etc.):
			return CommentAnalyzer.normalizeReference(reference);
		}
		
		private static String cutOffInnerTypes(String reference) {
			int dotPos= reference.indexOf('.'); // cut off inner types
			if (dotPos != -1)
				reference= reference.substring(0, dotPos);
			return reference;
		}
		
		private String getUpdatedImport(IImportDeclaration importDeclaration) {
			String fullyQualifiedImportType= importDeclaration.getElementName();
			int offsetOfDotBeforeTypeName= fPackage.getElementName().length();
			String result= getNewPackageName() + fullyQualifiedImportType.substring(offsetOfDotBeforeTypeName);
			return result;
		}
	
		private String getNewPackageName() {
			return fProcessor.getNewPackageName(fPackage.getElementName());
		}
	}
	
	/**
	 * Collector for import additions/removals.
	 * Saves all changes for a one-pass rewrite.
	 */
	static class ImportsManager {
		public static class ImportChange {
			private ArrayList/*<String>*/ fStaticToRemove= new ArrayList();
			private ArrayList/*<String[2]>*/ fStaticToAdd= new ArrayList();
			private ArrayList/*<String>*/ fToRemove= new ArrayList();
			private ArrayList/*<String>*/ fToAdd= new ArrayList();
			
			public void removeStaticImport(String elementName) {
				fStaticToRemove.add(elementName);
			}
	
			public void addStaticImport(String declaringType, String memberName) {
				fStaticToAdd.add(new String[] {declaringType, memberName});
			}
			
			public void removeImport(String elementName) {
				fToRemove.add(elementName);
			}
			
			public void addImport(String elementName) {
				fToAdd.add(elementName);
			}
		}
		
		private HashMap/*<ICompilationUnit, ImportChange>*/ fImportChanges= new HashMap();
		
		public ImportChange getImportChange(ICompilationUnit cu) {
			ImportChange importChange= (ImportChange) fImportChanges.get(cu);
			if (importChange == null) {
				importChange= new ImportChange();
				fImportChanges.put(cu, importChange);
			}
			return importChange;
		}
	
		public void rewriteImports(TextChangeManager changeManager, IProgressMonitor pm) throws CoreException {
			for (Iterator iter= fImportChanges.entrySet().iterator(); iter.hasNext();) {
				Entry entry= (Entry) iter.next();
				ICompilationUnit cu= (ICompilationUnit) entry.getKey();
				ImportChange importChange= (ImportChange) entry.getValue();
				
				ImportRewrite importRewrite= StubUtility.createImportRewrite(cu, true);
				importRewrite.setFilterImplicitImports(false);
				for (Iterator iterator= importChange.fStaticToRemove.iterator(); iterator.hasNext();) {
					importRewrite.removeStaticImport((String) iterator.next());
				}
				for (Iterator iterator= importChange.fToRemove.iterator(); iterator.hasNext();) {
					importRewrite.removeImport((String) iterator.next());
				}
				for (Iterator iterator= importChange.fStaticToAdd.iterator(); iterator.hasNext();) {
					String[] toAdd= (String[]) iterator.next();
					importRewrite.addStaticImport(toAdd[0], toAdd[1], true);
				}
				for (Iterator iterator= importChange.fToAdd.iterator(); iterator.hasNext();) {
					importRewrite.addImport((String) iterator.next());
				}
				
				if (importRewrite.hasRecordedChanges()) {
					TextEdit importEdit= importRewrite.rewriteImports(pm);
					String name= RefactoringCoreMessages.RenamePackageRefactoring_update_imports; 
					TextChangeCompatibility.addTextEdit(changeManager.get(cu), name, importEdit);
				}
			}
		}
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaElement.PACKAGE_FRAGMENT)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaRefactorings.RENAME_PACKAGE);
				else
					fPackage= (IPackageFragment) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String patterns= extended.getAttribute(ATTRIBUTE_PATTERNS);
			if (patterns != null && !"".equals(patterns)) //$NON-NLS-1$
				fFilePatterns= patterns;
			else
				fFilePatterns= ""; //$NON-NLS-1$
			final String references= extended.getAttribute(ATTRIBUTE_REFERENCES);
			if (references != null) {
				fUpdateReferences= Boolean.valueOf(references).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REFERENCES));
			final String matches= extended.getAttribute(ATTRIBUTE_TEXTUAL_MATCHES);
			if (matches != null) {
				fUpdateTextualMatches= Boolean.valueOf(matches).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_TEXTUAL_MATCHES));
			final String qualified= extended.getAttribute(ATTRIBUTE_QUALIFIED);
			if (qualified != null) {
				fUpdateQualifiedNames= Boolean.valueOf(qualified).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_QUALIFIED));
			final String hierarchical= extended.getAttribute(ATTRIBUTE_HIERARCHICAL);
			if (hierarchical != null) {
				fRenameSubpackages= Boolean.valueOf(hierarchical).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_HIERARCHICAL));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
