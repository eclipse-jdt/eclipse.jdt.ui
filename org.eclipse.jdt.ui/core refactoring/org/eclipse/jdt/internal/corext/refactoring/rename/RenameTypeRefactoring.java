/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextPosition;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.JavaModelUtility;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

public class RenameTypeRefactoring extends Refactoring implements IRenameRefactoring, ITextUpdatingRefactoring, IReferenceUpdatingRefactoring{

	private IType fType;
	private String fNewName;
	private SearchResultGroup[] fOccurrences;
	private boolean fUpdateReferences;
	
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;

	public RenameTypeRefactoring(IType type) {
		Assert.isTrue(type.exists());
		//Assert.isTrue(! type.getCompilationUnit().isWorkingCopy());
		fType= type;
		fUpdateReferences= true; //default is yes
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
	}
	
	/*
	 * @see ITextUpdatingRefactoring#canEnableTextUpdating()
	 */
	public boolean canEnableTextUpdating() {
		return true;
	}
	
	/*
	 * @see ITextUpdatingRefactoring#getUpdateJavaDoc()
	 */
	public boolean getUpdateJavaDoc() {
		return fUpdateJavaDoc;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateComments()
	 */
	public boolean getUpdateComments() {
		return fUpdateComments;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateStrings()
	 */
	public boolean getUpdateStrings() {
		return fUpdateStrings;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateJavaDoc(boolean)
	 */
	public void setUpdateJavaDoc(boolean update) {
		fUpdateJavaDoc= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateComments(boolean)
	 */
	public void setUpdateComments(boolean update) {
		fUpdateComments= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateStrings(boolean)
	 */
	public void setUpdateStrings(boolean update) {
		fUpdateStrings= update;
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
		IType orig= (IType)WorkingCopyUtil.getOriginal(fType);
		if (orig == null || ! orig.exists())
			return RefactoringStatus.createFatalErrorStatus("Please save the compilation unit '" + fType.getCompilationUnit().getElementName()+ "' before performing this refactoring.");
		fType= orig;
		
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
			
			result.merge(checkEnclosingTypes());
			pm.worked(1);	
			
			result.merge(checkEnclosedTypes());
			pm.worked(1);	
			
			result.merge(checkTypesInPackage());
			pm.worked(1);	
			
			result.merge(checkTypesImportedInCu());
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

	private RefactoringStatus checkTypesImportedInCu() throws JavaModelException{
		IImportDeclaration imp= getImportedType(fType.getCompilationUnit(), fNewName);
		
		if (imp == null)
			return null;	
			
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.imported", //$NON-NLS-1$
											new Object[]{fNewName, getResource(fType).getFullPath()});
		IJavaElement grandParent= imp.getParent().getParent();
		if (grandParent instanceof ICompilationUnit)
			return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(imp));

		return null;	
	}
	
	private RefactoringStatus checkTypesInPackage() throws JavaModelException{
		IType type= findTypeInPackage(fType.getPackageFragment(), fNewName);
		if (type == null)
			return null;
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.exists", //$NON-NLS-1$
																	new String[]{fNewName, fType.getPackageFragment().getElementName()});
		return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(type));
	}
	
	private RefactoringStatus checkEnclosedTypes() throws JavaModelException{
		IType enclosedType= findEnclosedType(fType, fNewName);
		if (enclosedType == null)
			return null;
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.encloses",  //$NON-NLS-1$
																		new String[]{fType.getFullyQualifiedName(), fNewName});
		return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(enclosedType));
	}
	
	private RefactoringStatus checkEnclosingTypes() throws JavaModelException{
		IType enclosingType= findEnclosingType(fType, fNewName);
		if (enclosingType == null)
			return null;
			
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed",//$NON-NLS-1$
								new String[]{fType.getFullyQualifiedName(), fNewName});
		return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(enclosingType));
	}
	
	private static IType findTypeInPackage(IPackageFragment pack, String name) throws JavaModelException{
		Assert.isTrue(pack.exists(), RefactoringCoreMessages.getString("TypeRefactoring.assert.must_exist_1")); //$NON-NLS-1$
		Assert.isTrue(!pack.isReadOnly(), RefactoringCoreMessages.getString("TypeRefactoring.assert.read-only")); //$NON-NLS-1$
		
		/* ICompilationUnit.getType expects simple name*/  
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
		ICompilationUnit[] cus= pack.getCompilationUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return cus[i].getType(name);
		}
		return null;
	}
	
	private static IType findEnclosedType(IType type, String newName) throws JavaModelException{
		IType[] enclosedTypes= type.getTypes();
		for (int i= 0; i < enclosedTypes.length; i++){
			if (newName.equals(enclosedTypes[i].getElementName()) || findEnclosedType(enclosedTypes[i], newName) != null)
				return enclosedTypes[i];
		}
		return null;
	}
		
	private static IType findEnclosingType(IType type, String newName) {
		IType enclosing= type.getDeclaringType();
		while (enclosing != null){
			if (newName.equals(enclosing.getElementName()))
				return enclosing;
			else 
				enclosing= enclosing.getDeclaringType();	
		}
		return null;
	}
	
	private static IImportDeclaration getImportedType(ICompilationUnit cu, String typeName) throws JavaModelException{
		IImportDeclaration[] imports= cu.getImports();
		String dotTypeName= "." + typeName; //$NON-NLS-1$
		for (int i= 0; i < imports.length; i++){
			if (imports[i].getElementName().endsWith(dotTypeName))
				return imports[i];
		}
		return null;
	}
	
	private static boolean isSpecialCase(IType type) throws JavaModelException{
		return type.getPackageFragment().getElementName().equals("java.lang");	 //$NON-NLS-1$
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		return RefactoringScopeFactory.create(fType);
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
	
	private RefactoringStatus checkTypesInCompilationUnit() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (! Checks.isTopLevel(fType)){ //the other case checked in checkTypesInPackage
			IType siblingType= fType.getDeclaringType().getType(fNewName);
			if (siblingType.exists()){
				String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.member_type_exists", //$NON-NLS-1$
																		new String[]{fNewName, fType.getDeclaringType().getFullyQualifiedName()});
				result.addError(msg, JavaSourceContext.create(siblingType));
			}
		}
		return result;
	}
	
	/*
	 * Checks if the specified type has locally defined types with the specified names
	 * and if it declares a native method
	 */ 	
	private static RefactoringStatus analyseEnclosedLocalTypes(final IType type, final String newName) throws JavaModelException{
		final ICompilationUnit cunit= type.getCompilationUnit();
		final CompilationUnit cu= (CompilationUnit)cunit;
		final RefactoringStatus result= new RefactoringStatus();
		cu.accept(new AbstractSyntaxTreeVisitorAdapter(){
			public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
				if ( new String(typeDeclaration.name).equals(newName) 	&& isInType(type.getElementName(), scope)){
						String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.local_type_name", //$NON-NLS-1$
																				new String[]{type.getElementName(), newName});
						result.addError(msg, JavaSourceContext.create(cunit, createSourceRange(typeDeclaration)));
					}															
				if (typeDeclaration.methods != null){
					for (int i=0; i < typeDeclaration.methods.length; i++){
						AbstractMethodDeclaration method= typeDeclaration.methods[i];
						if (method.isNative()){
							String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.local_type_native", type.getElementName());//$NON-NLS-1$
							result.addWarning(msg, JavaSourceContext.create(cunit, createSourceRange(method)));  
						}	
					}	
				}	
				return true;
			}
			
			public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
				if (new String(typeDeclaration.name).equals(newName)   && isInType(type.getElementName(), scope)){
					String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed_type_name", //$NON-NLS-1$
																					new String[]{type.getElementName(), newName});
					result.addError(msg, JavaSourceContext.create(cunit, createSourceRange(typeDeclaration)));
				}																
						
				if (typeDeclaration.methods != null){
					for (int i=0; i < typeDeclaration.methods.length; i++){
						AbstractMethodDeclaration method= typeDeclaration.methods[i];
						if (method.isNative()){
							String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed_type_native", type.getElementName());//$NON-NLS-1$
							result.addWarning(msg, JavaSourceContext.create(cunit, createSourceRange(method))); 
						}	
					}	
				}	
				return true;
			}
		});
		return result;
	}
	
	private static SourceRange createSourceRange(AstNode node){
		return new SourceRange(node.sourceStart, node.sourceEnd - node.sourceStart + 1);
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
				String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name_conflict1", //$NON-NLS-1$
																			new Object[]{types[i].getFullyQualifiedName(), getFullPath(getCompilationUnit(imp))});
				result.addError(msg, JavaSourceContext.create(imp));
			}
		}
	}
	
	private void analyzeImportDeclaration(IImportDeclaration imp, RefactoringStatus result) throws JavaModelException{
		if (!imp.isOnDemand())
			return; //analyzed earlier
		
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
	
	/*
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
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
			pm.beginTask(RefactoringCoreMessages.getString("RenameTypeRefactoring.creating_change"), 4); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange();
			addOccurrences(new SubProgressMonitor(pm, 2), builder);
			if (willRenameCU())
				builder.add(new RenameResourceChange(getResource(fType), fNewName + ".java")); //$NON-NLS-1$
			pm.worked(1);	
			return builder;	
		} catch (CoreException e){
			throw new JavaModelException(e);
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
	
	private void addTextMatches(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		TextMatchFinder.findTextMatches(pm, createRefactoringScope(), this, manager);
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws CoreException{
		pm.beginTask("", 6);
		TextChangeManager manager= new TextChangeManager();
				
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 3));

		pm.worked(1);
		
		addTypeDeclarationUpdate(manager);
		pm.worked(1);
		
		addConstructorRenames(manager);
		pm.worked(1);
		
		pm.subTask("searching for text matches...");
		addTextMatches(manager, new SubProgressMonitor(pm, 1));

		//putting it together
		builder.addAll(manager.getAllChanges());
	}
	
	private void addTypeDeclarationUpdate(TextChangeManager manager) throws CoreException{
		String name= "Type declaration update";
		int typeNameLength= fType.getElementName().length();
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
		manager.get(cu).addTextEdit(name, SimpleTextEdit.createReplace(fType.getNameRange().getOffset(), typeNameLength, fNewName));
	}
	
	private void addConstructorRenames(TextChangeManager manager) throws CoreException{
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
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
				String name= RefactoringCoreMessages.getString("RenameTypeRefactoring.rename_constructor");
				manager.get(cu).addTextEdit(name, SimpleTextEdit.createReplace(methods[i].getNameRange().getOffset(), typeNameLength, fNewName));
			}
		}
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fOccurrences.length);
		for (int i= 0; i < fOccurrences.length; i++){
			IResource resource= fOccurrences[i].getResource();
			IJavaElement element= JavaCore.create(resource);
			if (!(element instanceof ICompilationUnit))
				continue;
					
			ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
			String name= RefactoringCoreMessages.getString("RenameTypeRefactoring.update_reference");
			SearchResult[] results= fOccurrences[i].getSearchResults();

			for (int j= 0; j < results.length; j++){
				SearchResult searchResult= results[j];
				int offset= searchResult.getStart();
				int length= searchResult.getEnd() - searchResult.getStart();
				manager.get(wc).addTextEdit(name, new UpdateTypeReference(offset, length, fNewName, fType.getElementName()));
			}
			pm.worked(1);
		}
	}

	//-----------------
	private static class UpdateTypeReference extends SimpleTextEdit {
	
		private String fOldName;
		
		UpdateTypeReference(int offset, int length, String newName, String oldName) {
			super(offset, length, newName);
			Assert.isNotNull(oldName);
			fOldName= oldName;			
		}
		
		private UpdateTypeReference(TextPosition position, String newName, String oldName) {
			super(position, newName);
			Assert.isNotNull(oldName);
			fOldName= oldName;			
		}

		/* non Java-doc
		 * @see TextEdit#copy
		 */
		public TextEdit copy() {
			return new UpdateTypeReference(getTextPosition().copy(), getText(), fOldName);
		}

		/* non Java-doc
		 * @see TextEdit#connect(TextBuffer)
		 */
		public void connect(TextBuffer buffer) throws CoreException {
			TextPosition pos= getTextPosition();
			int offset= pos.getOffset() + pos.getLength() - fOldName.length();
			int length= fOldName.length();
			TextPosition newPos= new TextPosition(offset, length);
			setTextPosition(newPos);
		}
	}
}
