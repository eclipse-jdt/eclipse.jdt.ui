/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.internal.core.refactoring.util.TextBufferChangeManager;

public class RenameTypeRefactoring extends Refactoring implements IRenameRefactoring{

	private IType fType;	private String fNewName;
	private SearchResultGroup[] fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	private boolean fUpdateReferences;

	public RenameTypeRefactoring(ITextBufferChangeCreator changeCreator, IType type) {
		Assert.isNotNull(type);
		Assert.isNotNull(changeCreator);
		Assert.isTrue(type.exists());
		fTextBufferChangeCreator= changeCreator;
		fType= type;
		fUpdateReferences= true; //default is yes
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getNewName
	 */
	public String getNewName(){
		return fNewName;
	}
	/* non java-doc
	 * @see IRenameRefactoring#setNewName
	 */
	public void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName
	 */
	public String getCurrentName(){
		return fType.getElementName();
	}
	
	/* non java-doc
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name",  //$NON-NLS-1$
														new String[]{fType.getFullyQualifiedName(), fNewName});
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */	
	public void setUpdateReferences(boolean update){
		fUpdateReferences= update;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */	
	public boolean canEnableUpdateReferences(){
		return true;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}

	//------------- Conditions -----------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(fType));
		if (isSpecialCase(fType))
			result.addFatalError(RefactoringCoreMessages.getString("RenameTypeRefactoring.special_case"));	 //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		return Checks.checkIfCuBroken(fType);
	}

	/* non java-doc
	 * @see IRenameRefactoring#checkNewName
	 */	
	public RefactoringStatus checkNewName(){
		Assert.isNotNull(fType, "type"); //$NON-NLS-1$
		Assert.isNotNull(fNewName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkTypeName(fNewName));
		if (Checks.isAlreadyNamed(fType, fNewName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameTypeRefactoring.choose_another_name"));	 //$NON-NLS-1$
		return result;
	}

	/* non java-doc
	 * @see Refactoring#checkInput
	 */		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		Assert.isNotNull(fType, "type"); //$NON-NLS-1$
		Assert.isNotNull(fNewName, "newName"); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		try{
			pm.beginTask("", 73); //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("RenameTypeRefactoring.checking")); //$NON-NLS-1$
			result.merge(checkNewName());
			if (result.hasFatalError())
				return result;
			result.merge(Checks.checkIfCuBroken(fType));
			if (result.hasFatalError())
				return result;
			
			pm.worked(2);
			result.merge(checkTypesInCompilationUnit());
			pm.worked(1);
			result.merge(checkForMethodsWithConstructorNames());
			pm.worked(1);
			result.merge(checkImportedTypes());	
			pm.worked(1);
			if (mustRenameCU())
				result.merge(Checks.checkCompilationUnitNewName(fType.getCompilationUnit(), fNewName));
			pm.worked(1);	
			if (isEnclosedInType(fType, fNewName))	
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed",  //$NON-NLS-1$
																			new String[]{fType.getFullyQualifiedName(), fNewName}));
			pm.worked(1);	
			if (enclosesType(fType, fNewName))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.encloses",  //$NON-NLS-1$
																			new String[]{fType.getFullyQualifiedName(), fNewName}));
			pm.worked(1);	
			if (typeNameExistsInPackage(fType.getPackageFragment(), fNewName))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.exists", //$NON-NLS-1$
																			new String[]{fNewName, fType.getPackageFragment().getElementName()}));
			pm.worked(1);	
			if (compilationUnitImportsType(fType.getCompilationUnit(), fNewName))	
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.imported", //$NON-NLS-1$
																			new Object[]{fNewName, getResource(fType).getFullPath()}));
			pm.worked(1);	
			result.merge(Checks.checkForNativeMethods(fType));
			pm.worked(1);	
			result.merge(Checks.checkForMainMethod(fType));
			pm.worked(1);	
			
			// before doing any expensive analysis
			if (result.hasFatalError())
				return result;
							
			result.merge(analyseEnclosedLocalTypes(fType, fNewName));
			pm.worked(1);

			// before doing _the really_ expensive analysis
			if (result.hasFatalError())
				return result;
			
			if (! fUpdateReferences)
				return result;
										
			result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 35))));

			if (pm.isCanceled())
				throw new OperationCanceledException();

			result.merge(analyzeAffectedCompilationUnits(new SubProgressMonitor(pm, 25)));
			
			return result;
		} finally {
			pm.done();
		}	
	}
	
	private static boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaModelException{
		Assert.isTrue(pack.exists(), RefactoringCoreMessages.getString("TypeRefactoring.assert.must_exist_1")); //$NON-NLS-1$
		Assert.isTrue(!pack.isReadOnly(), RefactoringCoreMessages.getString("TypeRefactoring.assert.read-only")); //$NON-NLS-1$
		
		/* ICompilationUnit.getType expects simple name*/  
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
		ICompilationUnit[] cus= pack.getCompilationUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return true;
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
		String dotTypeName= "." + typeName; //$NON-NLS-1$
		for (int i= 0; i < imports.length; i++){
			if (imports[i].getElementName().endsWith(dotTypeName))
				return true;
		}
		return false;
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
	
	private static boolean isSpecialCase(IType type) throws JavaModelException{
		return type.getPackageFragment().getElementName().equals("java.lang");	 //$NON-NLS-1$
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		return SearchEngine.createWorkspaceScope();
	}
	
	private ISearchPattern createSearchPattern() throws JavaModelException{
		return SearchEngine.createSearchPattern(fType, IJavaSearchConstants.REFERENCES);
	}
	
	private SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws JavaModelException{
		pm.subTask(RefactoringCoreMessages.getString("RenameTypeRefactoring.searching"));	 //$NON-NLS-1$
		fOccurrences= RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());
		return fOccurrences;
	}	
	private RefactoringStatus checkForMethodsWithConstructorNames()  throws JavaModelException{
		IMethod[] methods= fType.getMethods();
		for (int i= 0; i < methods.length; i++){
			if (methods[i].isConstructor())
				continue;
			RefactoringStatus check= Checks.checkIfConstructorName(methods[i], methods[i].getElementName(), fNewName);	
			if (check != null)
				return check;
		}
		return null;
	}	
	
	private RefactoringStatus checkImportedTypes() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IImportDeclaration[] imports= fType.getCompilationUnit().getImports();	
		for (int i= 0; i < imports.length; i++)
			analyzeImportDeclaration(imports[i], result);
		return result;
	}
	
	private RefactoringStatus checkTypesInCompilationUnit(){
		RefactoringStatus result= new RefactoringStatus();
		if (Checks.isTopLevel(fType)){
			ICompilationUnit cu= fType.getCompilationUnit();
			//ICompilationUnit.getType expects simple name
			if ((fNewName.indexOf(".") == -1) && cu.getType(fNewName).exists()) //$NON-NLS-1$
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.type_exists_in_cu", //$NON-NLS-1$
																		new String[]{fNewName, cu.getElementName()}));
		} else {
			if (fType.getDeclaringType().getType(fNewName).exists())
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.member_type_exists", //$NON-NLS-1$
																		new String[]{fNewName, fType.getDeclaringType().getFullyQualifiedName()}));
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
						result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.local_type_name", //$NON-NLS-1$
																				new String[]{type.getElementName(), newName}));
				if (typeDeclaration.methods != null){
					for (int i=0; i < typeDeclaration.methods.length; i++){
						if (typeDeclaration.methods[i].isNative())
							result.addWarning(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.local_type_native", type.getElementName()));  //$NON-NLS-1$
					}	
				}	
				return true;
			}
			
			public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
				if (new String(typeDeclaration.name).equals(newName)
				    && isInType(type.getElementName(), scope))
						result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed_type_name", //$NON-NLS-1$
																				new String[]{type.getElementName(), newName}));
						
				if (typeDeclaration.methods != null){
					for (int i=0; i < typeDeclaration.methods.length; i++){
						if (typeDeclaration.methods[i].isNative())
							result.addWarning(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed_type_native", type.getElementName())); //$NON-NLS-1$
					}	
				}	
				return true;
			}
		});
		return result;
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

	private boolean mustRenameCU() throws JavaModelException{
		return Checks.isTopLevel(fType)	&& (Flags.isPublic(fType.getFlags()));
	}
	
	private static ICompilationUnit getCompilationUnit(IImportDeclaration imp){
		return (ICompilationUnit)imp.getParent().getParent();
	}
	
	private static String getSimpleName(IImportDeclaration imp){
		String name= imp.getElementName();
		if (! imp.isOnDemand())
			return name;
		else
			return name.substring(0, name.length() - 2); //remove the '.*' suffix
	}
	
	private void analyzeImportedTypes(IType[] types, RefactoringStatus result, IImportDeclaration imp) throws JavaModelException{
		for (int i= 0; i < types.length; i++) {
			//could this be a problem (same package imports)?
			if (Flags.isPublic(types[i].getFlags()) && types[i].getElementName().equals(fNewName)){
				result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name_conflict1", //$NON-NLS-1$
																			new Object[]{types[i].getFullyQualifiedName(), getFullPath(getCompilationUnit(imp))}));
			}
		}
	}
	
	private void analyzeImportDeclaration(IImportDeclaration imp, RefactoringStatus result) throws JavaModelException{
		if (!imp.isOnDemand()){
			analyzeSingleTypeImport(imp, result);
			return;
		}
		
		IJavaElement imported= JavaModelUtility.convertFromImportDeclaration(imp);
		if (imported == null)
			return;
			
		if (imported instanceof IPackageFragment){
			ICompilationUnit[] cus= ((IPackageFragment)imported).getCompilationUnits();
			for (int i= 0; i < cus.length; i++) {
				analyzeImportedTypes(cus[i].getTypes(), result, imp);
			}	
		} else {
			//cast safe: see JavaModelUtility.convertFromImportDeclaration
			analyzeImportedTypes(((IType)imported).getTypes(), result, imp);
		}
	}
	
	private void analyzeSingleTypeImport(IImportDeclaration imp, RefactoringStatus result) throws JavaModelException{
		String name= getSimpleName(imp);
		IType importedType= (IType)JavaModelUtility.convertFromImportDeclaration(imp);
		if (! fNewName.equals(name.substring(name.lastIndexOf(".") + 1))) //$NON-NLS-1$
			return;
		
		Object[] message;
		if (importedType == null)
			message= new Object[]{name, getFullPath(getCompilationUnit(imp))};
		else
			message= new Object[]{importedType.getFullyQualifiedName(), getFullPath(getCompilationUnit(imp))};
				
		result.addError(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name_conflict1", //$NON-NLS-1$
																	message));
	}
	
	/*
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, getUnsavedFiles(), result);
		if (result.hasFatalError())
			return result;
			
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
		
		pm.beginTask("", fOccurrences.length); //$NON-NLS-1$
		RenameTypeASTAnalyzer analyzer= new RenameTypeASTAnalyzer(fNewName, fType);
		for (int i= 0; i < fOccurrences.length ; i++){
			analyzeCompilationUnit(pm, analyzer, fOccurrences[i], result);
			pm.worked(1);
			
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		return result;
	}
	
	private static String getFullPath(ICompilationUnit cu) throws JavaModelException{
		Assert.isTrue(cu.exists());
		return getResource(cu).getFullPath().toString();
	}
	
	private void analyzeCompilationUnit(IProgressMonitor pm, RenameTypeASTAnalyzer analyzer, SearchResultGroup searchResults, RefactoringStatus result)  throws JavaModelException {
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(searchResults.getResource()));
		pm.subTask(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.analyzing", cu.getElementName())); //$NON-NLS-1$
		if ((! cu.exists()) || (cu.isReadOnly()) || (!cu.isStructureKnown()))
			return;
		result.merge(analyzeCompilationUnit(cu));	
		result.merge(analyzer.analyze(searchResults.getSearchResults(), cu));
	}
	
	/* 
	 * all the analysis that can be done with no AST walking
	 */
	private RefactoringStatus analyzeCompilationUnit(ICompilationUnit cu) throws JavaModelException{
		/* ICompilationUnit.getType expects simple name */
		String name= fNewName;
		if (fNewName.indexOf(".") != -1) //$NON-NLS-1$
			name= fNewName.substring(0, fNewName.indexOf(".")); //$NON-NLS-1$
		if (! cu.getType(name).exists())
			return null;
			
		return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name_conflict2", //$NON-NLS-1$
																		new Object[]{fNewName, getResource(cu).getFullPath()}));
	}
	
	//------------- Changes ---------------
	
	/*
	 * non java-doc
	 * @see IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("RenameTypeRefactoring.creating_change"), 3); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange();
			addOccurrences(new SubProgressMonitor(pm, 2), builder);
			if (willRenameCU())
				builder.addChange(new RenameResourceChange(getResource(fType), fNewName + ".java")); //$NON-NLS-1$
			pm.worked(1);	
			return builder;	
		} finally{
			pm.done();
		}	
	}
	
	private boolean willRenameCU() throws JavaModelException{
		if (mustRenameCU())
			return true;
		if (! Checks.isTopLevel(fType))
			return false;
		if (! fType.getCompilationUnit().getElementName().equals(fType.getElementName() + ".java")) //$NON-NLS-1$
			return false;
		return Checks.checkCompilationUnitNewName(fType.getCompilationUnit(), fNewName).isOK();
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		pm.beginTask("", 5);
		TextBufferChangeManager manager= new TextBufferChangeManager(fTextBufferChangeCreator);
		
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 3));
		pm.worked(1);
		
		addTypeDeclarationUpdate(manager);
		pm.worked(1);
		
		addConstructorRenames(manager);
		pm.worked(1);

		//putting it together
		IChange[] changes= manager.getAllChanges();
		for (int i= 0; i < changes.length; i++){
			builder.addChange(changes[i]);
		}
	}
	
	private void addTypeDeclarationUpdate(TextBufferChangeManager manager) throws JavaModelException{
		String name= "Type declaration update";
		int typeNameLength= fType.getElementName().length();
		manager.addReplace(fType.getCompilationUnit(), name, fType.getNameRange().getOffset(), typeNameLength, fNewName);
	}
	
	private void addConstructorRenames(TextBufferChangeManager manager) throws JavaModelException{
		ICompilationUnit cu= fType.getCompilationUnit();
		IMethod[] methods= fType.getMethods();
		int typeNameLength= fType.getElementName().length();
		for (int i= 0; i < methods.length; i++){
			if (methods[i].isConstructor()) {
				/*
				 * constructor declarations cannot be fully qualified so we can use simple replace here
				 *
				 * if (methods[i].getNameRange() == null), then it's a binary file so it's wrong anyway 
				 * (checked as a precondition)
				 */				
				manager.addReplace(cu, RefactoringCoreMessages.getString("RenameTypeRefactoring.rename_constructor"), methods[i].getNameRange().getOffset(), typeNameLength, fNewName); //$NON-NLS-1$
			}
		}
	}
	
	private void addReferenceUpdates(TextBufferChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fOccurrences.length);
		for (int i= 0; i < fOccurrences.length; i++){
			IResource resource= fOccurrences[i].getResource();
			IJavaElement element= JavaCore.create(resource);
			if (!(element instanceof ICompilationUnit))
				continue;
			
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				manager.addSimpleTextChange((ICompilationUnit)element, createTextChange(results[j]));
			}
			pm.worked(1);
		}
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		return new SimpleReplaceTextChange(RefactoringCoreMessages.getString("RenameTypeRefactoring.update_reference"), searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName) { //$NON-NLS-1$
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				//the match is guaranteed to end with the type name - so we only take the suffix
				Assert.isTrue(buffer.getContent(getOffset(), getLength()).endsWith(fType.getElementName()));
				setOffset(getOffset() + getLength() - fType.getElementName().length());
				setLength(fType.getElementName().length());
				return null;
			}
		};
	}
}
