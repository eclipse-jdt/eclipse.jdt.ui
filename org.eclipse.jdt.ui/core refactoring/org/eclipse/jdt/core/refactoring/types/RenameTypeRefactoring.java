/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.types;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.JavaModelUtility;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.RenameResourceChange;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameTypeRefactoring extends TypeRefactoring implements IRenameRefactoring{

	private String fNewName;
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameTypeRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IType type, String newName){
		super(scope, type);
		Assert.isNotNull(newName, "new name");
		Assert.isNotNull(changeCreator, "change creator");
		fNewName= newName;
		fTextBufferChangeCreator= changeCreator;
	}
	
	public RenameTypeRefactoring(ITextBufferChangeCreator changeCreator, IType type) {
		super(type);
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
	}
	
	public final String getNewName(){
		return fNewName;
	}

	/**
	 * @see IRenameRefactoring#setNewName
	 */
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/**
	 * @see IRenameRefactoring#getCurrentName
	 */
	public final String getCurrentName(){
		return getType().getElementName();
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return "Rename type:" + getType().getFullyQualifiedName() + " to:" + fNewName;
	}

	//------------- Conditions -----------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		Assert.isNotNull(getType(), "type");
		Assert.isNotNull(fNewName, "newName");
		RefactoringStatus result= new RefactoringStatus();
		try{
			pm.beginTask("checking preconditions...", 41);
			HackFinder.fixMeSoon("why do i have to do this to make the task name visible?");
			pm.subTask("");
			result.merge(checkNewName());
			if (result.hasFatalError())
				return result;
			
			pm.worked(2);
			result.merge(checkTypesInCompilationUnit());
			pm.worked(1);
			result.merge(checkImportedTypes());	
			pm.worked(1);
			if (mustRenameCU())
				result.merge(Checks.checkCompilationUnitNewName(getType().getCompilationUnit(), fNewName + ".java"));
			pm.worked(1);	
			if (isEnclosedInType(getType(), fNewName))	
				result.addError("Type " + getType().getFullyQualifiedName() + " is enclosed in a type named " + fNewName);
			pm.worked(1);	
			if (enclosesType(getType(), fNewName))
				result.addError("Type " + getType().getFullyQualifiedName() + " encloses a type named " + fNewName);
			pm.worked(1);	
			if (typeNameExistsInPackage(getType().getPackageFragment(), fNewName))
				result.addError("Type " + fNewName + " already exists in package " + getType().getPackageFragment().getElementName());
			pm.worked(1);	
			if (compilationUnitImportsType(getType().getCompilationUnit(), fNewName))	
				result.addError("Type " + fNewName + " is imported (single-type-import) in " + getResource(getType()).getFullPath() + " (a compilation unit must not import and declare a type with the same name)");
			pm.worked(1);	
			result.merge(Checks.checkForNativeMethods(getType()));
			pm.worked(1);	
			result.merge(Checks.checkForMainMethod(getType()));
			pm.worked(1);	
			
			// before doing any expensive analysis
			if (result.hasFatalError())
				return result;
				
			result.merge(analyseEnclosedLocalTypes(getType(), fNewName));
			pm.worked(1);

			// before doing _the really_ expensive analysis
			if (result.hasFatalError())
				return result;
									
			//if (Flags.isPublic(getType().getFlags()))
			//	result.merge(analyzeImportDeclarations(new SubProgressMonitor(pm, 8, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
				
			pm.worked(1);
			result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 11))));
			pm.worked(1);
			result.merge(analyzeAffectedCompilationUnits(new SubProgressMonitor(pm, 7)));
		} finally {
			pm.done();
		}	
		return result;
	}
	
	private List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			pm.subTask("searching for references");	
			fOccurrences= RefactoringSearchEngine.search(pm, getScope(), createSearchPattern());
		}	
		return fOccurrences;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getType()));
		if (!Checks.isTopLevel(getType()))
			result.addFatalError("Only applicable to top level types");
		if (isSpecialCase(getType()))
			result.addFatalError("It is a special case");	
		return result;
	}
	
	public RefactoringStatus checkNewName(){
		Assert.isNotNull(getType(), "type");
		Assert.isNotNull(fNewName, "new name");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkTypeName(fNewName));
		if (Checks.isAlreadyNamed(getType(), fNewName))
			result.addFatalError("Please choose another name.");	
		return result;
	}
	
	private RefactoringStatus checkImportedTypes() throws JavaModelException{
		IImportDeclaration[] imports= getType().getCompilationUnit().getImports();
		if (imports == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < imports.length; i++)
			analyzeImportDeclaration(imports[i], result);
		return result;
	}
	
	private RefactoringStatus checkTypesInCompilationUnit(){
		RefactoringStatus result= new RefactoringStatus();
		if (Checks.isTopLevel(getType())){
			ICompilationUnit cu= (ICompilationUnit)getType().getParent();
			HackFinder.fixMeSoon("ICompilation unit feature - walkback if not simple name");
			if ((fNewName.indexOf(".") == -1) && cu.getType(fNewName).exists())
				result.addError("Type " + fNewName + " already exists in " + cu.getElementName());
		} else {
			if (getType().getDeclaringType().getType(fNewName).exists())
				result.addError("Member type " + fNewName + " already exists in " + getType().getDeclaringType().getFullyQualifiedName());
		}
		return result;
	}
	
	/*
	 * Checks if the specified type has locally defined types with the specified names
	 * and if it declares a native method
	 */ 	
	private static RefactoringStatus analyseEnclosedLocalTypes(final IType type, final String newName) throws JavaModelException{
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(getResource(type)));
		final RefactoringStatus result= new RefactoringStatus();
		cu.accept(new AbstractSyntaxTreeVisitorAdapter(){
			public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
				if ( new String(typeDeclaration.name).equals(newName)
					&& isInType(type.getElementName(), scope))
						result.addError("A local type enclosed in type " + type.getElementName() + " is already named " + newName);
				if (typeDeclaration.methods != null){
					for (int i=0; i < typeDeclaration.methods.length; i++){
						if (typeDeclaration.methods[i].isNative())
							result.addWarning("A local type enclosed in type " + type.getElementName() + " declares a native method");
					}	
				}	
				return true;
			}
			
			public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
				if (new String(typeDeclaration.name).equals(newName)
				    && isInType(type.getElementName(), scope))
						result.addError("A type enclosed in type " + type.getElementName() + " is already named " + newName);
				if (typeDeclaration.methods != null){
					for (int i=0; i < typeDeclaration.methods.length; i++){
						if (typeDeclaration.methods[i].isNative())
							result.addWarning("A type enclosed in type " + type.getElementName() + " declares a native method");
					}	
				}	
				return true;
			}
		});
		return result;
	}

	private boolean mustRenameCU() throws JavaModelException{
		return Checks.isTopLevel(getType())	&& (Flags.isPublic(getType().getFlags()));
	}
	
	//-----	analyzing import declarations in other compilation units
	
	/* (non java-doc)
	 * checks if any of the SearchResults on the list is an <code>IImportDeclaration</code> on demand
	 */
	private static IImportDeclaration findImportOnDemand(List searchResults){
		for (Iterator iter= searchResults.iterator(); iter.hasNext(); ){
			SearchResult searchResult= (SearchResult)iter.next();
			IJavaElement element= searchResult.getEnclosingElement();
			if ((element instanceof IImportDeclaration)
			 	 && ((IImportDeclaration)element).isOnDemand()){
				return (IImportDeclaration)element;
			}	
		}
		return null;
	}
	
	private static ICompilationUnit getCompilationUnit(IImportDeclaration imp){
		return (ICompilationUnit)imp.getParent().getParent();
	}
	
	private String getSimpleName(IImportDeclaration imp){
		String name= imp.getElementName();
		if (imp.isOnDemand())
			return name;
		else
			return name.substring(0, name.length() - 2); //remove the '.*' suffix
	}
	
	private void analyzeImportedTypes(IType[] types, RefactoringStatus result, IImportDeclaration imp) throws JavaModelException{
		if (types == null)
			return;
		for (int i= 0; i < types.length; i++) {
			//could this be a problem (same package imports)?
			if (Flags.isPublic(types[i].getFlags()) && types[i].getElementName().equals(fNewName)){
				result.addError("Possible name conflict with " + types[i].getFullyQualifiedName() + " in " + getFullPath(getCompilationUnit(imp)));
			}
		}
	}
	
	private void analyzeImportDeclaration(IImportDeclaration imp, RefactoringStatus result) throws JavaModelException{
		String name= getSimpleName(imp);
		
		if (!imp.isOnDemand()){
			IType importedType= (IType)JavaModelUtility.convertFromImportDeclaration(imp);
			if (name.substring(name.lastIndexOf(".") + 1).equals(fNewName))
				result.addError("Possible name conflict with " + importedType.getFullyQualifiedName() + " in "+ getFullPath(getCompilationUnit(imp)));
			return;
		}
		IJavaElement imported= JavaModelUtility.convertFromImportDeclaration(imp);
		if (imported instanceof IPackageFragment){
			ICompilationUnit[] cus= ((IPackageFragment)imported).getCompilationUnits();
			if (cus != null){
				for (int i= 0; i < cus.length; i++) {
					analyzeImportedTypes(cus[i].getTypes(), result, imp);
				}	
			}
		} else {
			//cast safe: see JavaModelUtility.convertFromImportDeclaration
			analyzeImportedTypes(((IType)imported).getTypes(), result, imp);
		}
	}
	
	private RefactoringStatus analyzeImportDeclarations(IProgressMonitor pm) throws JavaModelException{
		IPackageFragment thisPackage= getType().getPackageFragment();
		if (thisPackage.isDefaultPackage())
			return null;
		RefactoringStatus result= new RefactoringStatus();	
		ISearchPattern pattern= SearchEngine.createSearchPattern(thisPackage, IJavaSearchConstants.REFERENCES);
		List grouped= RefactoringSearchEngine.search(pm, getScope(), pattern);
		
		for (Iterator iter= grouped.iterator(); iter.hasNext();){
			IImportDeclaration declaration= findImportOnDemand((List)iter.next());
			if (declaration == null)
				continue;
			IImportDeclaration[] imports= getCompilationUnit(declaration).getImports();
			if (imports == null)
				continue;
			for (int i= 0; i < imports.length; i++){
				analyzeImportDeclaration(imports[i], result);
			}
		}
		return result;
	}
	
	//-------------- AST visitor-based analysis
	
	/*
	 * (non java-doc)
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		Iterator iter= fOccurrences.iterator();
		pm.beginTask("", fOccurrences.size());
		RenameTypeASTAnalyzer analyzer= new RenameTypeASTAnalyzer(fNewName, getType());
		while (iter.hasNext()){
			analyzeCompilationUnit(pm, analyzer, (List)iter.next(), result);
			pm.worked(1);
		}
		return result;
	}
	
	private void analyzeCompilationUnit(IProgressMonitor pm, RenameTypeASTAnalyzer analyzer, List searchResults, RefactoringStatus result)  throws JavaModelException {
		SearchResult searchResult= (SearchResult)searchResults.get(0);
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(searchResult.getResource()));
		pm.subTask("analyzing \"" + cu.getElementName()+ "\"");
		if ((! cu.exists()) || (cu.isReadOnly()) || (!cu.isStructureKnown()))
			return;
		result.merge(analyzeCompilationUnit(cu));	
		result.merge(analyzer.analyze(searchResults, cu));
	}
	
	/* non java-doc
	 * all the analysis that can be done with no AST walking
	 */
	private RefactoringStatus analyzeCompilationUnit(ICompilationUnit cu) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		HackFinder.fixMeSoon("ICompilation unit feature - walkback if not simple name");
		String name;
		if (fNewName.indexOf(".") != -1)
			name= fNewName.substring(0, fNewName.indexOf("."));
		else name= fNewName;	
		if (cu.getType(name).exists())
			result.addError("Possible name conflict with type " + fNewName + " declared in \"" + getResource(cu).getFullPath() + "\"");
		return result;
	}
	
	//------------- Changes ---------------

	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		CompositeChange builder= new CompositeChange();
		pm.beginTask("creating change", getOccurrences(null).size() + 1);
		addOccurrences(pm, builder);
		if (mustRenameCU())
			builder.addChange(new RenameResourceChange(getResource(getType()), fNewName));
		pm.worked(1);	
		pm.done();	
		return builder;	
	}
	
	private void addConstructorRenames(ITextBufferChange change) throws JavaModelException{
		IMethod[] methods= getType().getMethods();
		int typeNameLength= getType().getElementName().length();
		for (int i= 0; i < methods.length; i++){
			if (methods[i].isConstructor()) {
				/*
				 * constructor declarations cannot be fully qualified so we can use simple replace here
				 *
				 * if (methods[i].getNameRange() == null), then it's a binary file so it's wrong anyway 
				 * (checked as a precondition)
				 */
				change.addReplace("Constructor rename", methods[i].getNameRange().getOffset(), typeNameLength, fNewName);
			}
		}	
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		IResource ourResource= getResource(getType());
		for (Iterator iter= getOccurrences(null).iterator(); iter.hasNext();){
			List l= (List)iter.next();
			IResource resource= ((SearchResult)l.get(0)).getResource();
			ITextBufferChange change= fTextBufferChangeCreator.create("Rename Type", (ICompilationUnit)JavaCore.create(resource));
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			if (resource.equals(ourResource)){
				/* there is at least one Occurrence in the compilation unit that declares our type
				 * (the type delaration itself) so this code is always executed exactly once
				 */
				addConstructorRenames(change);
			}	
			builder.addChange(change);
			pm.worked(1);
		}
		HackFinder.fixMeSoon("maybe add dispose() method?");
		fOccurrences= null; //to prevent memory leak
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		SimpleReplaceTextChange change= new SimpleReplaceTextChange("Type Reference Update", searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName) {
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String packageName= getType().getPackageFragment().getElementName();
				String oldTypeName= getType().getElementName();
				String oldText= buffer.getContent(getOffset(), getLength());
				if (!oldTypeName.equals(packageName) 
				  && oldText.startsWith(packageName) 
				  && (! getType().getPackageFragment().isDefaultPackage())
				  && (! getText().startsWith(packageName))){
					setLength(oldText.indexOf(oldTypeName) + oldTypeName.length());
					oldText= oldText.substring(0, getLength());
					setText(packageName + oldText.substring(packageName.length(), oldText.indexOf(oldTypeName)) + getText());
				}	
				return null;
			}
		};
		return change;
	}

		
	private ISearchPattern createSearchPattern() throws JavaModelException{
		return SearchEngine.createSearchPattern(getType(), IJavaSearchConstants.ALL_OCCURRENCES);
	}
	
	static String getFullPath(ICompilationUnit cu){
		/*
		 * we catch the exception mainly in order to avoid dealing with it in the ast visitor
		 */
		Assert.isTrue(cu.exists());
		IPath path= null;
		try {
			return getResource(cu).getFullPath().toString();
		} catch (JavaModelException e){
			return cu.getElementName();
		}
	}
	
	private static boolean isInType(String typeName, Scope scope){
		Scope current= scope;
		while (current != null){
			if (current instanceof ClassScope){
				if (typeName.equals(new String(((ClassScope)current).referenceContext.name)))
					return true;
			}
			current= current.parent;
		}
		return false;
	}
					
	private static boolean isSpecialCase(IType type) throws JavaModelException{
		return type.getPackageFragment().getElementName().equals("java.lang");	
	}
	
	private static boolean isEnclosedInType(IType type, String newName) {
		IType enclosing= type.getDeclaringType();
		while (enclosing != null){
			if (newName.equals(enclosing.getElementName()))
				return true;
			else 
				enclosing= enclosing.getDeclaringType();	
		}
		return false;
	}
	
	private static boolean enclosesType(IType type, String newName) throws JavaModelException{
		IType[] enclosedTypes= type.getTypes();
		for (int i= 0; i < enclosedTypes.length; i++){
			if (newName.equals(enclosedTypes[i].getElementName()) || enclosesType(enclosedTypes[i], newName))
				return true;
		}
		return false;
	}
	
	private static boolean compilationUnitImportsType(ICompilationUnit cu, String typeName) throws JavaModelException{
		IImportDeclaration[] imports= cu.getImports();
		String dotTypeName= "." + typeName;
		for (int i= 0; i < imports.length; i++){
			if (imports[i].getElementName().endsWith(dotTypeName))
				return true;
		}
		return false;
	}
	
}
