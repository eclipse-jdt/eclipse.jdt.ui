/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.cus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class MoveCompilationUnitRefactoring extends Refactoring{
	/*
	* NOTE: This class will be split.
	*/
	 
	private IPackageFragment fNewPackage;
	private ICompilationUnit fCompilationUnit;
	
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private boolean fNeedsImportToCurrentPackage;
	 
	public MoveCompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, IPackageFragment newPackage, ICompilationUnit compilationUnit){
		super();
		Assert.isNotNull(newPackage, "newPackage");
		Assert.isNotNull(compilationUnit, "compilation unit");
		Assert.isNotNull(changeCreator, "change creator");
		fNewPackage= newPackage;
		fCompilationUnit= compilationUnit;
		fTextBufferChangeCreator= changeCreator;		
	}
	
	public MoveCompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit compilationUnit){
		super();
		Assert.isNotNull(compilationUnit, "compilation unit");
		Assert.isNotNull(changeCreator, "change creator");
		fCompilationUnit= compilationUnit;
		fTextBufferChangeCreator= changeCreator;
	}
		
	public String getName() {
		return "Move Compilation Unit \"" + fCompilationUnit.getElementName() + "\" to: " + fNewPackage.getElementName();
	}
	
	public ICompilationUnit getCompilationUnit(){
		return fCompilationUnit;
	}
	
	public void setNewPackage(IPackageFragment pack){
		Assert.isNotNull(pack);
		fNewPackage= pack;
	}
	
	private static IPackageFragment getPackage(ICompilationUnit cu){
		return (IPackageFragment)cu.getParent();
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		/*
		 * not checked preconditions:
		 *  a. native methods in locally defined types in this package (too expensive - requires AST analysis)
		 */
		pm.beginTask("", 46);
		pm.subTask("Checking preconditions");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(fNewPackage));
		pm.worked(1);
		
		if (fNewPackage.getCompilationUnit(fCompilationUnit.getElementName()).exists())
			result.addFatalError("Compilation unit \"" + fCompilationUnit.getElementName() + "\" already exists in " + fNewPackage.getElementName());
		pm.worked(1);
		
		result.merge(checkNewPackage());
		pm.worked(1);
		
		result.merge(Checks.checkForNativeMethods(fCompilationUnit));
		pm.worked(1);
		result.merge(Checks.checkForMainMethods(fCompilationUnit));
		pm.worked(1);
		if (result.hasFatalError())
			return result;
		
		fNeedsImportToCurrentPackage= needsImportToCurrentPackage(new SubProgressMonitor(pm, 3, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL), fCompilationUnit);
		if (fNeedsImportToCurrentPackage)
			result.merge(checkTopLevelTypeNameConflict(fNewPackage, fCompilationUnit));
							
		result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 11, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL))));
		pm.worked(5);
		result.merge(checkReferencesInOurCompilationUnit(new SubProgressMonitor(pm, 11, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
		result.merge(checkReferencesInOtherCompilationUnits(new SubProgressMonitor(pm, 11, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
		pm.done();
		return result;
	}
	
	private List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			fOccurrences= RefactoringSearchEngine.search(pm, getScope(), createSearchPattern());	
		}	
		return fOccurrences;
	}

	private static RefactoringStatus checkTopLevelTypeNameConflict(IPackageFragment newPack, ICompilationUnit cu) throws JavaModelException{
		IPackageFragment oldPack= getPackage(cu);
		RefactoringStatus result= new RefactoringStatus(); 
		IType[] importedTypes= getOutsideVisibleTypes(oldPack, true);
		IType[] newPackageTypes= getOutsideVisibleTypes(newPack, true);
		Map typeNames= new HashMap(newPackageTypes.length);
		for (int i= 0; i < newPackageTypes.length; i++)
			typeNames.put(newPackageTypes[i].getElementName(), newPackageTypes[i]);
			
		for (int i= 0; i < importedTypes.length; i++){
			IType type= (IType)typeNames.get(importedTypes[i].getElementName());
			if (type != null)
				result.addError("Possible name conflict between types " + importedTypes[i].getFullyQualifiedName() + " and " + type.getFullyQualifiedName()+ " in \"" + cu.getElementName() + " \"");
		}	
		return result;
	}
	
	/* non java-doc
	 * to avoid code duplication with package visible and protected members lookup
	 */
	private static interface MemberValidator{
		public boolean isAccepted(IMember member) throws JavaModelException;
	}
	 
	private static MemberValidator createPackageVisibleMemberValidator(){
		return new MemberValidator(){
			public boolean isAccepted(IMember member) throws JavaModelException{
				int flags= member.getFlags();
				return (! Flags.isPublic(flags)) && (! Flags.isPrivate(flags)) && (! Flags.isProtected(flags));
			};
		};
	}
	private static MemberValidator createProtectedMemberValidator(){
		return new MemberValidator(){
			public boolean isAccepted(IMember member) throws JavaModelException{
				return Flags.isProtected(member.getFlags());
			};
		};
	}
			
	private static List getMembers(MemberValidator validator, IMember[] members) throws JavaModelException{
		if (members == null)
			return null;
		List list= new ArrayList(members.length);
		for (int i= 0; i < members.length; i++){
			if (validator.isAccepted(members[i]))
				list.add(members[i]);
		}
		return list;
	}
	
	private static List getMembers(MemberValidator validator, IType type) throws JavaModelException{
		/*
		 * this can happen if you declare a top-level type as private
		 * it is a compiler error but this code will just ignore such cases
		 */
		if (Flags.isPrivate(type.getFlags()))
			return null;
		IType[] types= type.getTypes();
		List l= new ArrayList(3);
		if (types != null){
			for (int i= 0; i < types.length; i++){
				if (validator.isAccepted(types[i]))
					l.add(types[i]);
				if (! Flags.isPrivate(types[i].getFlags()))	
					l.addAll(getMembers(validator, types[i]));
			}
		}
		
		l.addAll(getMembers(validator, type.getMethods()));
		l.addAll(getMembers(validator, type.getFields()));
		return l;
	}
	
	private static List getMembers(MemberValidator validator, ICompilationUnit cu) throws JavaModelException{
		IType[] types= cu.getTypes();
		if (types == null || types.length == 0)
			return null;	
		ArrayList list= new ArrayList(3);
		for (int i= 0; i < types.length; i++){
			//on top level everything is visible
			if (validator.isAccepted(types[i]))
				list.add(types[i]);
			list.addAll(getMembers(validator, types[i]));
		}
		return list;
	}
	
	private RefactoringStatus checkReferencesInOurCompilationUnit(String flagLabel, MemberValidator validator, IProgressMonitor pm) throws JavaModelException{
		List members= new ArrayList();
		IResource ourResource= getResource(fCompilationUnit);
		
		ICompilationUnit[] cus= getPackage(fCompilationUnit).getCompilationUnits();
		//must be at least one - ours
		for (int i= 0; i < cus.length; i++){
			if (! ourResource.equals(getResource(cus[i])))
				members.addAll(getMembers(validator, cus[i]));
		}
		
		if (members.isEmpty())
			return null;
		ISearchPattern pattern= createSearchPattern(members);
		RefactoringStatus result= new RefactoringStatus();
		List grouped= RefactoringSearchEngine.search(pm, createCompilationUnitScope(fCompilationUnit), pattern);
		for (Iterator iter= grouped.iterator(); iter.hasNext(); ){
			List searchResults= (List)iter.next();
			IResource resource= ((SearchResult)searchResults.get(0)).getResource();
			if (resource.equals(ourResource)){
				// can't get more info: don't know which one it really was
				result.addError("A " + flagLabel + " java element declared outside of \"" + resource.getProjectRelativePath() + "\" is referenced in there. ");
				// show only 1 error - not a list
				return result;
			}	
		}
		return result;
			
	}
	
	private RefactoringStatus checkReferencesInOurCompilationUnit(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2);
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkReferencesInOurCompilationUnit("protected", createProtectedMemberValidator(), new SubProgressMonitor(pm, 1)));
		pm.worked(1);
		result.merge(checkReferencesInOurCompilationUnit("package-visible", createPackageVisibleMemberValidator(), new SubProgressMonitor(pm, 1)));
		pm.worked(1);
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkReferencesInOtherCompilationUnits(String flagLabel, MemberValidator validator, IProgressMonitor pm) throws JavaModelException{
		List members= getMembers(validator, fCompilationUnit);
		if (members == null)
			return null;
		ISearchPattern pattern= createSearchPattern(members);
		RefactoringStatus result= new RefactoringStatus();
		List grouped= RefactoringSearchEngine.search(pm, createPackageScope(getPackage(fCompilationUnit)), pattern);
		IResource ourResource= getResource(fCompilationUnit);
		for (Iterator iter= grouped.iterator(); iter.hasNext(); ){
			List searchResults= (List)iter.next();
			IResource resource= ((SearchResult)searchResults.get(0)).getResource();
			if (! resource.equals(ourResource))
				/*
				 * can't get more info: don't know which one it really was
				 */
				result.addError("A "+ flagLabel + " java element declared in \"" + fCompilationUnit.getElementName() +"\" is referenced in \"" + resource.getProjectRelativePath() + "\" (refactoring may result in compile errors)");
		}
		return result;
		
	}
	
	private RefactoringStatus checkReferencesInOtherCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2);
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkReferencesInOtherCompilationUnits("protected", createProtectedMemberValidator(), new SubProgressMonitor(pm, 1)));
		pm.worked(1);
		result.merge(checkReferencesInOtherCompilationUnits("package-visible", createPackageVisibleMemberValidator(), new SubProgressMonitor(pm, 1)));
		pm.worked(1);
		pm.done();
		return result;
	}
	
	private static boolean isVisibleOutsidePackage(IType type) throws JavaModelException {
		/*
		 * we just ignore cases where a top-level type has flags other than 'public'
		 * otherwise it is a compiler error anyway.
		 */
		boolean isTopLevel= Checks.isTopLevel(type);
		int flags= type.getFlags();
		if ((! Flags.isPublic(flags)) && (! Flags.isProtected(flags)))
			return false;
		if (isTopLevel)
			//if it's top-level, then it must be public (the other possiblity has been checked before)
		 	return true;
		else
			return isVisibleOutsidePackage(type.getDeclaringType());
	}

	private static IJavaSearchScope createPackageScope(IPackageFragment pack) throws JavaModelException {
		return SearchEngine.createJavaSearchScope(new IResource[]{pack.getUnderlyingResource()});
	}
	
	private static IJavaSearchScope createCompilationUnitScope(ICompilationUnit cu) throws JavaModelException {
		return SearchEngine.createJavaSearchScope(new IResource[]{getResource(cu)});
	}
	
	private static ISearchPattern createSearchPattern(List types){
		ISearchPattern pattern= null;
		for (Iterator iter= types.iterator(); iter.hasNext();){
			IJavaElement javaElement= (IJavaElement)iter.next();
			ISearchPattern newPattern= SearchEngine.createSearchPattern(javaElement, IJavaSearchConstants.ALL_OCCURRENCES);
			if (pattern == null)
				pattern= newPattern;
			else
				pattern= SearchEngine.createOrSearchPattern(pattern, newPattern);
		}
		return pattern;
	}
	
	private static RefactoringStatus checkNameConflicts(IType type, IType[] types) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		String name= type.getElementName();
		boolean isTopLevel= Checks.isTopLevel(type);
		for (int i= 0; i < types.length; i++){
			if (name.equals(types[i].getElementName())){
				ICompilationUnit cu= types[i].getCompilationUnit();
				if (isTopLevel && Checks.isTopLevel(types[i]))
					result.addError("Type " + types[i].getElementName() + " exists in " + getPackage(cu).getElementName() + " (compilation unit: \"" + cu.getElementName() + "\")");
				else 
					result.addError("Possible name conflict between type " + name 
								+ " (from " + type.getCompilationUnit().getElementName() + ") and " 
								+  types[i].getElementName() + " declared in: " + getResource(cu).getProjectRelativePath());
			}
		}
		return result;
	}
	
	private static RefactoringStatus checkCompilationUnit(ICompilationUnit cu, IType[] newTypes) throws JavaModelException {
		IType[] oldTypes= cu.getAllTypes();
		if (oldTypes == null || oldTypes.length == 0)
			return null;

		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < newTypes.length; i++){	
			result.merge(checkNameConflicts(newTypes[i], oldTypes));
		}
		return result;
	}
	
	private RefactoringStatus checkNewPackage() throws JavaModelException {
		ICompilationUnit[] cus= fNewPackage.getCompilationUnits();
		if (cus == null || cus.length == 0) 
			return null;
			
		IType[] types= fCompilationUnit.getAllTypes();
		if (types == null || types.length == 0)
			return null;
		
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++){
			result.merge(checkCompilationUnit(cus[i], types));
		}
		return result;	
	}	
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(fCompilationUnit));
		pm.done();
		return result;
	}
	
	public RefactoringStatus checkPackage() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (((IPackageFragment)fCompilationUnit.getParent()).equals(fNewPackage))
			result.addFatalError("Please choose another package");
		result.merge(checkAvailability(fNewPackage));
		return result;
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("creating change", 3 + getOccurrences(null).size());
		CompositeChange builder= new CompositeChange();
		pm.worked(1);
		addLocalImportUpdate(builder);
		pm.worked(1);
		modifyTypeReferences(pm, builder);
		builder.addChange(new MoveCompilationUnitChange(fCompilationUnit, fNewPackage));
		pm.done();
		fOccurrences= null;
		return builder;
	}
	
	private IType getPublicType() throws JavaModelException{
		IType[] types= fCompilationUnit.getTypes();
		if (types == null)
			return null;
		for (int i= 0; i < types.length; i++){
			if (Flags.isPublic(types[i].getFlags()))
				return types[i];
		}
		return null;
	}
	
	private ISearchPattern createSearchPattern() throws JavaModelException{
		/*
		 * we just ignore the case when more than one top-level type is public.
		 * it's a compiler error anyway.
		 */
		IType publicType= getPublicType();
		if (publicType == null)
			return null; 
		else
			return SearchEngine.createSearchPattern(publicType, IJavaSearchConstants.REFERENCES);
	}
	
	/*
	 * non java-doc
	 * search engine feature: reports ImportDeclarations as not qualified ones
	 * the workaround is to explicitly check for that case
	 * do not have to check isOnDemand() - if it was reported as a reference to a type it must be ok
	 */
	private static boolean isQualifiedReference(SearchResult searchResult){
		return searchResult.isQualified() 
			|| (searchResult.getEnclosingElement() instanceof IImportDeclaration);	
	}
		
	private void modifyTypeReferences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException {
		//DebugUtils.dumpCollectionCollection("references:", getOccurrences(null));
		for (Iterator iter= getOccurrences(null).iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ICompilationUnit cu= (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource());
			ITextBufferChange change= fTextBufferChangeCreator.create("update type reference", cu);
			boolean importNeeded= false;
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				SearchResult searchResult= (SearchResult)subIter.next();
				boolean isQualified= isQualifiedReference(searchResult);
				importNeeded= importNeeded || (!isQualified);
				if (isQualified)
					change.addSimpleTextChange(createTextChange(searchResult));
			}
			if (importNeeded && (!cu.equals(fCompilationUnit)))
				addImport(change, fNewPackage, cu);
			if (!change.isEmpty())	
				builder.addChange(change);
			pm.worked(1);
		}
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		/*
		 * Most of these should not be created because they are simple type references.
		 * otherwise they're displayed in the compare viewer as no-ops
		 */
		HackFinder.fixMeSoon("see comment");
		
		SimpleReplaceTextChange change= new SimpleReplaceTextChange("Type Reference Update", searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewPackage.getElementName()) {
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String oldText= buffer.getContent(getOffset(), getLength());
				String packageName= getPackage(fCompilationUnit).getElementName();
				if (getPackage(fCompilationUnit).isDefaultPackage()){
					setLength(0);
				} else if (! oldText.startsWith(packageName)){
					//no action - simple reference
					setLength(0);
					setText("");
				} else{
					setLength(packageName.length());					
				}
				return null;
			}
		};
		return change;
	}
	//----------
	private static List getOutsideVisibleTypes(IType type) throws JavaModelException{
		IType[] types= type.getTypes();
		List foundTypes= new ArrayList();
		for (int i= 0; i < types.length; i++){
			int flags= types[i].getFlags();
			if (Flags.isPublic(flags) || Flags.isProtected(flags)){
				foundTypes.add(types[i]);
				foundTypes.addAll(getOutsideVisibleTypes(types[i]));
			}	
		}
		return foundTypes;
	}
	
	private static List getOutsideVisibleTypes(ICompilationUnit cu, boolean topLevelOnly) throws JavaModelException{
		IType[] types= cu.getTypes();
		List publicTypes= new ArrayList();
		for (int i= 0; i < types.length; i++){
			//normally, there can be only 1
			if (Flags.isPublic(types[i].getFlags())){
				publicTypes.add(types[i]);
				if (! topLevelOnly)
					publicTypes.addAll(getOutsideVisibleTypes(types[i]));
			}	
		}
		return publicTypes;
	}
	private static IType[] getOutsideVisibleTypes(IPackageFragment pack, boolean topLevelOnly) throws JavaModelException{
		ICompilationUnit[] cus= pack.getCompilationUnits();
		
		List publicTypes= new ArrayList();
		for (int i= 0; i < cus.length; i++)
			publicTypes.addAll(getOutsideVisibleTypes(cus[i], topLevelOnly));
		return (IType[])publicTypes.toArray(new IType[publicTypes.size()]);
	}
	
	private static boolean needsImportToCurrentPackage(IProgressMonitor pm, ICompilationUnit cu) throws JavaModelException{
		IPackageFragment pack= getPackage(cu);
		IType[] types= getOutsideVisibleTypes(pack, false);
		if (types == null || types.length == 0)
			return false;
		ISearchPattern pattern= null;	
		for (int i= 0; i < types.length; i++){
			if (types[i].getCompilationUnit().equals(cu))
				continue;
			ISearchPattern newPattern= SearchEngine.createSearchPattern(types[i], IJavaSearchConstants.ALL_OCCURRENCES);
			if (pattern == null)
				pattern= newPattern;
			else 
				pattern= SearchEngine.createOrSearchPattern(pattern, newPattern);	
		}

		/*
		 * does not need the import if all references are qualified
		 */
		List grouped= RefactoringSearchEngine.search(pm, createCompilationUnitScope(cu), pattern);
		if (grouped.isEmpty())
			return false;
		Assert.isTrue(grouped.size() == 1, "should not find references in more than 1 file");
		List searchResults = (List)grouped.get(0);
		for (Iterator iter= searchResults.iterator(); iter.hasNext();){
			if (!((SearchResult)iter.next()).isQualified())
				return true;
		}
		return false;
	}
	
	/* non java doc
	 * returns <code>true</code> if the import declaration has been added and <code>false</code> otherwise.
	 */
	private static boolean addImport(ITextBufferChange change, IPackageFragment pack, ICompilationUnit cu) throws JavaModelException {
		if (cu.getImport(pack.getElementName() + ".*").exists())
			return false;
					
		IImportContainer importContainer= cu.getImportContainer();
		int start;
		if (!importContainer.exists()){
			IPackageDeclaration packageDecl= cu.getPackageDeclaration(getPackage(cu).getElementName());
			if (!packageDecl.exists())
				start= 0;
			else{
				ISourceRange sr= packageDecl.getSourceRange();
				start= sr.getOffset() + sr.getLength() - 1;
			}	
		} else{
			ISourceRange sr= importContainer.getSourceRange();
			start= sr.getOffset() + sr.getLength() - 1;
		}	
			
		String newImportText= "\nimport " + pack.getElementName() + ".*;\n";
		change.addInsert("add import " + pack.getElementName(), start + 1, newImportText);
		return true;
	}
	
	private void addLocalImportUpdate(CompositeChange builder) throws JavaModelException {
		ITextBufferChange change= fTextBufferChangeCreator.create("Update Import", fCompilationUnit);
		/*
		 * could remove useless import declarations
		 */
		if (fNeedsImportToCurrentPackage && addImport(change, getPackage(fCompilationUnit), fCompilationUnit))
			builder.addChange(change);	
	}
}