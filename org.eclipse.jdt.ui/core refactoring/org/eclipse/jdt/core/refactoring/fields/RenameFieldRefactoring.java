/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.fields;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
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

import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
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
public class RenameFieldRefactoring extends FieldRefactoring implements IRenameRefactoring{
	
	private String fNewName;
	
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;

	public RenameFieldRefactoring(ITextBufferChangeCreator changeCreator, IField field){
		super(field);
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
		correctScope();
	}
	
	public RenameFieldRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IField field, String newName){
		super(scope, field);
		Assert.isNotNull(changeCreator, "change creator");
		Assert.isNotNull(newName, "new name");
		fTextBufferChangeCreator= changeCreator;
		fNewName= newName;
		correctScope();
	}
	
	/* non java-doc
	 * narrow down the scope
	 */ 
	private void correctScope(){
		if (getField().isBinary())
			return;
		try{
			//only the declaring compilation unit
			if (Flags.isPrivate(getField().getFlags()))
				setScope(SearchEngine.createJavaSearchScope(new IResource[]{getResource(getField())}));
		} catch (JavaModelException e){
			//do nothing
		}
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
		return getField().getElementName();
	}
		
	public final String getNewName(){
		return fNewName;
	}
	
	protected final ITextBufferChangeCreator getChangeCreator(){
		return fTextBufferChangeCreator;
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	 public String getName(){
	 	return "Rename field " + getField().getElementName() + " to: " + getNewName();
	 }
	
	// -------------- Preconditions -----------------------
	
	/**
	 * @see Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getField()));	
		return result;
	}
	
	/**
	 * @see IRenameRefactoring#checkNewName
	 */
	public RefactoringStatus checkNewName() {
		RefactoringStatus result= new RefactoringStatus();
		
		result.merge(Checks.checkFieldName(getNewName()));
			
		if (Checks.isAlreadyNamed(getField(), getNewName()))
			result.addFatalError("Choose another name.");
		if (getField().getDeclaringType().getField(getNewName()).exists())
			result.addError("Field with this name already defined");
		return result;
	}
	
	/**
	 * @see Refactoring#checkInput
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 9);
		pm.subTask("checking preconditions");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkNewName());
		pm.worked(1);
		result.merge(checkEnclosingHierarchy());
		pm.worked(1);
		result.merge(checkNestedHierarchy(getField().getDeclaringType()));
		pm.worked(1);
		result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 3))));
		result.merge(analyzeAffectedCompilationUnits(new SubProgressMonitor(pm, 3)));
		pm.done();
		return result;
	}
	
	
	private RefactoringStatus checkNestedHierarchy(IType type) throws JavaModelException {
		IType[] nestedTypes= type.getTypes();
		if (nestedTypes == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();	
		for (int i= 0; i < nestedTypes.length; i++){
			IField otherField= nestedTypes[i].getField(getNewName());
			if (otherField.exists())
				result.addWarning("After renaming, the field " + getField().getElementName() + " will be hidden in the scope of the field "+ getNewName() + " declared in type " + nestedTypes[i].getFullyQualifiedName());
			result.merge(checkNestedHierarchy(nestedTypes[i]));	
		}	
		return result;
	}
	
	private RefactoringStatus checkEnclosingHierarchy() throws JavaModelException {
		IType current= getField().getDeclaringType();
		if (Checks.isTopLevel(current))
			return null;
		RefactoringStatus result= new RefactoringStatus();
		while (current != null){
			IField otherField= current.getField(getNewName());
			if (otherField.exists())
				result.addWarning("After renaming, the field named " + getNewName() + " declared in type " + current.getFullyQualifiedName() + " will be hidden in the scope of the field " + getField().getElementName());
			current= current.getDeclaringType();
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
		pm.beginTask("", fOccurrences.size());
		Iterator iter= fOccurrences.iterator();
		RenameFieldASTAnalyzer analyzer= new RenameFieldASTAnalyzer(fNewName, getField());
		while (iter.hasNext()){
			analyzeCompilationUnit(pm, analyzer, (List)iter.next(), result);
		}
		return result;
	}
	
	private void analyzeCompilationUnit(IProgressMonitor pm, RenameFieldASTAnalyzer analyzer, List searchResults, RefactoringStatus result)  throws JavaModelException {
		SearchResult searchResult= (SearchResult)searchResults.get(0);
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(searchResult.getResource()));
		pm.subTask("analyzing \"" + cu.getElementName() + "\"");
		if ((! cu.exists()) || (cu.isReadOnly()) || (!cu.isStructureKnown()))
			return;
		result.merge(analyzer.analyze(searchResults, cu));
	}
	
	// ---------- Changes -----------------
	/**
	 * @see IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("creating rename package change", 7);
		CompositeChange builder= new CompositeChange();
		getOccurrences(new SubProgressMonitor(pm, 6));
		addOccurrences(pm, builder);
		pm.worked(1);
		pm.done();
		return builder; 
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		return new SimpleReplaceTextChange("Field Reference Update", searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), getNewName()){
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String oldText= buffer.getContent(getOffset(), getLength());
				if (oldText.startsWith("this.") && (! getText().startsWith("this."))){
					setText("this." + getText());
					setLength(getLength());
				}
				return null;
			}
		};
	}	

	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		List grouped= getOccurrences(null);
		for (Iterator iter= grouped.iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ITextBufferChange change= getChangeCreator().create("Rename Field", (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource()));
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			builder.addChange(change);
			pm.worked(1);
		}
		HackFinder.fixMeSoon("maybe add dispose() method?");
		setOccurrences(null); //to prevent memory leak
	}
	
	//--------------------------------------
	
	protected ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(getField(), IJavaSearchConstants.ALL_OCCURRENCES);
	}
	
	protected List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			pm.subTask("searching for references");	
			fOccurrences= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 6), getScope(), createSearchPattern());
		}	
		return fOccurrences;
	}
	
	/*package*/ final void setOccurrences(List Occurrences){
		fOccurrences= Occurrences;
	}
}