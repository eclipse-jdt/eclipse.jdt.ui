/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.methods;
import java.util.ArrayList;import java.util.Arrays;import java.util.Iterator;import java.util.List;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.Flags;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.core.refactoring.tagging.IPreactivatedRefactoring;import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;import org.eclipse.jdt.internal.core.refactoring.AbstractRefactoringASTAnalyzer;import org.eclipse.jdt.internal.core.refactoring.Assert;import org.eclipse.jdt.internal.core.refactoring.Checks;import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameParametersRefactoring extends MethodRefactoring implements IPreactivatedRefactoring{

	private String[] fNewParameterNames;

	//helper 
	private String[] fOldParameterNames;
	
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameParametersRefactoring(ITextBufferChangeCreator changeCreator, IMethod method){
		super(method);
		setOldParameterNames();
		fTextBufferChangeCreator= changeCreator;
	}
	
	public RenameParametersRefactoring(ITextBufferChangeCreator changeCreator, IMethod method, String[] newParameterNames){
		this(changeCreator, method);
		fNewParameterNames= newParameterNames;
	}
	
	private void setOldParameterNames(){
		if (getMethod().isBinary()) 
			return;
		try{
			fOldParameterNames= getMethod().getParameterNames();
		} catch (JavaModelException e){
		}	
	}
	
	public String getName(){
		return "Rename Method Parameters";
	}
	
	private void checkParameterNames(String[] newParameterNames) {
		Assert.isNotNull(fOldParameterNames, "names must not be null");
		Assert.isTrue(newParameterNames.length > 0, "must have at least 1 parameter");
		Assert.isTrue(fOldParameterNames.length == newParameterNames.length, "must have the same number of parameters");	
		for (int i= 0; i < newParameterNames.length; i++){
			Assert.isNotNull(newParameterNames[i], "parameter name is null: " + i);
		}	
	}
	
	public void setNewParameterNames(String[] newParameterNames){
		checkParameterNames(newParameterNames);
		fNewParameterNames= newParameterNames;
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getMethod()));
		HackFinder.fixMeSoon("fix this");
		if (getMethod().isConstructor())
			result.addFatalError("Temporarily not available for constructors");
		if (fOldParameterNames == null || fOldParameterNames.length == 0)
			result.addFatalError("Only applicable to methods with parameters."); 
		return result;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (! getMethod().getCompilationUnit().isStructureKnown())
			result.addFatalError("Compilation unit that declares this method cannot be parsed correctly.");
		return result;
	}

	public RefactoringStatus checkNewNames(){
		if (fNewParameterNames == null || fNewParameterNames.length == 0)
			return new RefactoringStatus();
		RefactoringStatus result= new RefactoringStatus();
		if (fOldParameterNames.length != fNewParameterNames.length)
			result.addFatalError("Must have the same number of parameters");
		if (!anythingRenamed())
			result.addError("All parameter names are unchanged. Please enter new parameter names");
		if (result.isOK())
			result.merge(checkForDuplicateNames());
		if (result.isOK())	
			result.merge(checkAllNames());
		return result;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 10);
		pm.subTask("checking preconditions");
		result.merge(checkNewNames());
		pm.worked(3);
		/*
		 * only one resource is affected - no need to check its availability
		 * (done in MethodRefactoring::checkActivation)
		 */
		if (mustAnalyzeAst()) 
			result.merge(analyzeAst()); 
		pm.worked(7);
		pm.done();
		return result;
	}
	//------
	
	private boolean anythingRenamed(){
		for (int i= 0; i < fNewParameterNames.length; i++){
			if (! fNewParameterNames[i].equals(fOldParameterNames[i]))
				return true;
		}
		return false;
	}
	
	private String[] getSortedCopy(String[] array){
		//should we use arrayCopy?
		String[] copy= (String[])array.clone();
		Arrays.sort(copy);
		return copy;
	}
	
	private RefactoringStatus checkForDuplicateNames(){
		String[] sorted= getSortedCopy(fNewParameterNames);
		String last= null;
		for (int i= 0; i < sorted.length; i++){
			if (sorted[i].equals(last)){
				RefactoringStatus result= new RefactoringStatus();
				result.addError("Duplicate parameter name: " + last);
				return result;
			}
			last= sorted[i];
		}
		return null;
	}
	
	private RefactoringStatus checkAllNames(){
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fNewParameterNames.length; i++){
			result.merge(Checks.checkFieldName(fNewParameterNames[i]));
		}
		return result;			
	}
	
	private boolean mustAnalyzeAst() throws JavaModelException{
		int flags= getMethod().getFlags();
		if (Flags.isAbstract(flags))
			return false;
		else if (Flags.isNative(flags))
			return false;
		else if (getMethod().getDeclaringType().isInterface())
			return false;
		else 
			return true;
	}
	
	private RefactoringStatus analyzeAst() throws JavaModelException{		
		AbstractRefactoringASTAnalyzer analyzer= new RenameParameterASTAnalyzer(getMethod(), fNewParameterNames);
		return analyzer.analyze(getMethod().getCompilationUnit());
	}
	
	//-------- changes ----
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("creating change", 10);
		ITextBufferChange builder= fTextBufferChangeCreator.create("rename method parameters", getMethod().getCompilationUnit());
		List renamed= getRenamedParameterIndices(fOldParameterNames, fNewParameterNames);
		for (Iterator iter= renamed.iterator(); iter.hasNext() ;){
			Integer i= 	(Integer)iter.next();
			addParameterRenaming(i.intValue(), builder);
		}
		pm.done();
		return builder;
	}
	
	private List getRenamedParameterIndices(String[] oldNames, String[] newNames){
		List l= new ArrayList(oldNames.length);
		for (int i= 0; i < oldNames.length; i++){
			if (! oldNames[i].equals(newNames[i]))
				l.add(new Integer(i));
		}
		return l;
	}
	
	private void addParameterRenaming(int parameterIndex, ITextBufferChange builder) throws JavaModelException{
		List offsets= findParameterOccurrenceOffsets(parameterIndex);
		Assert.isTrue(offsets.size() > 0); //at least the method declaration
		for (Iterator iter= offsets.iterator(); iter.hasNext(); ){
			addParameterRenameChange(parameterIndex, ((Integer)iter.next()).intValue(), builder);
		};
	}
	
	private List findParameterOccurrenceOffsets(int parameterIndex) throws JavaModelException{
		return new ParameterOffsetFinder(getMethod(), parameterIndex).findOffsets();
	}
	
	private void addParameterRenameChange(int parameterIndex, int occurrenceOffset, ITextBufferChange builder){
		int length= fOldParameterNames[parameterIndex].length();
		String newName= fNewParameterNames[parameterIndex];
		String oldName= fOldParameterNames[parameterIndex];
		builder.addReplace("update parameter reference", occurrenceOffset, length, newName);
	}
}


