package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.ProblemNodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MoveSourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.MoveTargetEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ModifyParametersRefactoring extends Refactoring {
	
	private final List fParameterInfos;
	private TextChangeManager fChangeManager;
	private IMethod fMethod;
	private IMethod[] fRippleMethods;
	private SearchResultGroup[] fOccurrences;

	public ModifyParametersRefactoring(IMethod method){
		fMethod= method;
		fParameterInfos= createParameterInfoList(method);
	}

	private static List createParameterInfoList(IMethod method) {
		try {
			String[] typeNames= method.getParameterTypes();
			String[] oldNames= method.getParameterNames();
			List result= new ArrayList(typeNames.length);
			for (int i= 0; i < oldNames.length; i++){
				result.add(new ParameterInfo(Signature.toString(typeNames[i]), oldNames[i], i));
			}
			return result;
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return new ArrayList(0);
		}		
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("ModifyParamatersRefactoring.modify_Parameters"); //$NON-NLS-1$
	}
	
	public IMethod getMethod() {
		return fMethod;
	}
	
	/**
	 * 	 * @return List of <code>ParameterInfo</code> objects.	 */
	public List getParameterInfos(){
		return fParameterInfos;
	}
	
	public RefactoringStatus checkNewNames(){
		RefactoringStatus result= new RefactoringStatus();
		if (result.isOK())
			result.merge(checkForDuplicateNames());
		if (result.isOK())	
			result.merge(checkAllNames());
		return result;
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fMethod));
		if (result.hasFatalError())
			return result;
			
		//XXX disable for constructors  - broken. see bug 23585
		if (fMethod.isConstructor())
			return RefactoringStatus.createFatalErrorStatus("This refactoring is not implemented for constructors");

		if (fMethod.getNumberOfParameters() == 0)
			return RefactoringStatus.createFatalErrorStatus("This refactoring is not implemented for methods with no parameters");
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= Checks.checkIfCuBroken(fMethod);
			if (result.hasFatalError())
				return result;
			IMethod orig= (IMethod)WorkingCopyUtil.getOriginal(fMethod);
			if (orig == null || ! orig.exists()){
				String message= RefactoringCoreMessages.getFormattedString("ReorderParametersRefactoring.method_deleted", fMethod.getCompilationUnit().getElementName());//$NON-NLS-1$
				return RefactoringStatus.createFatalErrorStatus(message);
			}
			fMethod= orig;
			
			if (MethodChecks.isVirtual(fMethod)){
				result.merge(MethodChecks.checkIfComesFromInterface(getMethod(), new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;	
				
				result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;			
			} 
			if (fMethod.getDeclaringType().isInterface()){
				result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;
			}
				
			return result;
		} finally{
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.checking_preconditions"), 2); //$NON-NLS-1$
			if (areNamesSameAsInitial() && isOrderSameAsInitial())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.no_changes")); //$NON-NLS-1$
			
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewNames());
			if (mustAnalyzeAst()) 
				result.merge(analyzeAst()); 
			if (result.hasFatalError())
				return result;
			if (! isOrderSameAsInitial())	
				result.merge(checkReorderings(new SubProgressMonitor(pm, 1)));	

			if (result.hasFatalError())
				return result;
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	public String getMethodSignaturePreview() throws JavaModelException{
		StringBuffer buff= new StringBuffer();

		if (! getMethod().isConstructor())
			buff.append(getReturnTypeString());

		buff.append(getMethod().getElementName())
			.append(Signature.C_PARAM_START)
			.append(getMethodParameters())
			.append(Signature.C_PARAM_END);
		return buff.toString();
	}

	private RefactoringStatus checkForDuplicateNames(){
		RefactoringStatus result= new RefactoringStatus();
		Set found= new HashSet();
		Set doubled= new HashSet();
		for (Iterator iter = getParameterInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			String newName= info.getNewName();
			if (found.contains(newName) && !doubled.contains(newName)){
				result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameParametersRefactoring.duplicate_name", newName));//$NON-NLS-1$	
				doubled.add(newName);
			} else {
				found.add(newName);
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAllNames(){
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter = getParameterInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			String newName= info.getNewName();
			result.merge(Checks.checkFieldName(newName));	
			if (! Checks.startsWithLowerCase(newName))
				result.addWarning(RefactoringCoreMessages.getString("RenameParametersRefactoring.should_start_lowercase")); //$NON-NLS-1$
		}
		return result;			
	}

	private ICompilationUnit getCu() {
		return getMethod().getCompilationUnit();
	}
	
	private boolean mustAnalyzeAst() throws JavaModelException{
		if (JdtFlags.isAbstract(getMethod()))
			return false;
		else if (JdtFlags.isNative(getMethod()))
			return false;
		else if (getMethod().getDeclaringType().isInterface())
			return false;
		else 
			return true;
	}
	
	private RefactoringStatus analyzeAst() throws JavaModelException{		
		ICompilationUnit wc= null;
		try {
			RefactoringStatus result= new RefactoringStatus();
						
			Map map= getEditMapping();
			TextEdit[] allEdits= getAllEdits(map);
			
			CompilationUnit compliationUnitNode= AST.parseCompilationUnit(getCu(), true);
			TextChange change= new TextBufferChange(RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_Paremeters"), TextBuffer.create(getCu().getSource())); //$NON-NLS-1$
			change.setKeepExecutedTextEdits(true);
		
			wc= RefactoringAnalyzeUtil.getWorkingCopyWithNewContent(allEdits, change, getCu());
			CompilationUnit newCUNode= AST.parseCompilationUnit(wc, true);
			
			result.merge(RefactoringAnalyzeUtil.analyzeIntroducedCompileErrors(change, wc, newCUNode, compliationUnitNode));
			if (result.hasError())
				return result;

			ParameterInfo[] renamedInfos= getRenamedParameterNames();				
			for (int i= 0; i < renamedInfos.length; i++) {
				TextEdit[] paramRenameEdits= (TextEdit[])map.get(renamedInfos[i]);
				String fullKey= RefactoringAnalyzeUtil.getFullDeclarationBindingKey(paramRenameEdits, compliationUnitNode);
				MethodDeclaration methodNode= RefactoringAnalyzeUtil.getMethodDeclaration(allEdits[0], change, newCUNode);
				SimpleName[] problemNodes= ProblemNodeFinder.getProblemNodes(methodNode, paramRenameEdits, change, fullKey);
				result.merge(RefactoringAnalyzeUtil.reportProblemNodes(wc, problemNodes));
			}
			return result;
		} catch(CoreException e) {
			throw new JavaModelException(e);
		} finally{
			if (wc != null)
				wc.destroy();
		}
	}
	
	private String getReturnTypeString() throws IllegalArgumentException, JavaModelException {
		StringBuffer buff= new StringBuffer();
		String returnType = Signature.getReturnType(getMethod().getSignature());
		if (returnType.length() != 0) {
			buff.append(Signature.toString(returnType))
				  .append(' ');
		}
		return buff.toString();
	}

	private String getMethodParameters() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		int i= 0;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (i != 0)
				buff.append(", ");  //$NON-NLS-1$
			buff.append(createDeclarationString(info));
		}
		return buff.toString();
	}

	private boolean areNamesSameAsInitial() {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.getOldName().equals(info.getNewName()))
				return false;
		}
		return true;
	}

	private boolean isOrderSameAsInitial(){
		int i= 0;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.getOldIndex() != i)
				return false;
			if (info.isAdded())
				return false;
		}
		return true;
	}

	private RefactoringStatus checkReorderings(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ReorderParametersRefactoring.checking_preconditions"), 2); //$NON-NLS-1$

			RefactoringStatus result= new RefactoringStatus();
			fRippleMethods= RippleMethodFinder.getRelatedMethods(fMethod, new SubProgressMonitor(pm, 1), null);
			fOccurrences= getOccurrences(new SubProgressMonitor(pm, 1));			
			fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
			if (result.hasFatalError())
				return result;
				
			result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));
			result.merge(checkNativeMethods());
			result.merge(checkParameterNamesInRippleMethods());
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkParameterNamesInRippleMethods() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		List newParameterNames= getNewParameterNamesList();
		for (int i= 0; i < fRippleMethods.length; i++) {
			String[] paramNames= fRippleMethods[i].getParameterNames();
			for (int j= 0; j < paramNames.length; j++) {
				if (newParameterNames.contains(paramNames[j])){
					String pattern= "Method ''{0}'' already has a parameter named ''{1}''";
					String[] args= new String[]{JavaElementUtil.createMethodSignature(fRippleMethods[i]), paramNames[j]};
					String msg= MessageFormat.format(pattern, args);
					Context context= JavaSourceContext.create(fRippleMethods[i].getCompilationUnit(), fRippleMethods[i].getNameRange());
					result.addError(msg, context);
				}	
			}
		}
		return result;
	}
	
	private List getNewParameterNamesList() {
		List newNames= new ArrayList(0);
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isAdded())
				newNames.add(info.getNewName());
		}
		return newNames;
	}
	
	private RefactoringStatus checkNativeMethods() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fRippleMethods.length; i++) {
			if (JdtFlags.isNative(fRippleMethods[i])){
				String message= RefactoringCoreMessages.getFormattedString("ReorderParametersRefactoring.native", //$NON-NLS-1$
					new String[]{JavaElementUtil.createMethodSignature(fRippleMethods[i]), JavaModelUtil.getFullyQualifiedName(fRippleMethods[i].getDeclaringType())});
				result.addError(message, JavaSourceContext.create(fRippleMethods[i]));			
			}								
		}
		return result;
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	//--  changes ----
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.restructure_parameters"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.preparing_preview"), 3); //$NON-NLS-1$
		TextChangeManager manager= new TextChangeManager();

		//the sequence here is critical 
		addReorderings(new SubProgressMonitor(pm, 1), manager);

		addNewParameters(new SubProgressMonitor(pm, 1), manager);

		if (! areNamesSameAsInitial())
			addRenamings(new SubProgressMonitor(pm, 1), manager);
		else
			pm.worked(1);	

		return manager;
	}

	//ParameterInfo -> TextEdit[]
	private Map getEditMapping() throws JavaModelException {
		ParameterInfo[] renamedInfos= getRenamedParameterNames();	
		Map map= new HashMap();
		for (int i= 0; i < renamedInfos.length; i++) {
			TextEdit[] paramRenameEdits= getParameterRenameEdits(renamedInfos[i]);
			map.put(renamedInfos[i], paramRenameEdits);
		}
		return map;
	}
	
	//ParameterInfo -> TextEdit[]
	private static TextEdit[] getAllEdits(Map map){
		Collection result= new ArrayList();
		for (Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			TextEdit[] array= (TextEdit[])map.get(iter.next());
			result.addAll(Arrays.asList(array));	
		}
		return (TextEdit[]) result.toArray(new TextEdit[result.size()]);
	}

	private TextEdit[] getAllRenameEdits() throws JavaModelException {
		Collection edits= new ArrayList();
		ParameterInfo[] renamed= getRenamedParameterNames();
		for (int i= 0; i < renamed.length; i++) {
			edits.addAll(Arrays.asList(getParameterRenameEdits(renamed[i])));
		}
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}
		
	private ParameterInfo[] getRenamedParameterNames(){
		List result= new ArrayList();
		for (Iterator iterator = getParameterInfos().iterator(); iterator.hasNext();) {
			ParameterInfo info= (ParameterInfo)iterator.next();
			if (! info.isAdded() && ! info.getOldName().equals(info.getNewName()))
				result.add(info);
		}
		return (ParameterInfo[]) result.toArray(new ParameterInfo[result.size()]);
	}
	
	private TextEdit[] getParameterRenameEdits(ParameterInfo info) throws JavaModelException{
		Collection edits= new ArrayList(); 
		String oldName= info.getOldName();
		int[] offsets= ParameterOffsetFinder.findOffsets(getMethod(), oldName, true);
		Assert.isTrue(offsets.length > 0); //at least the method declaration
		for (int i= 0; i < offsets.length; i++){
			edits.add(getParameterRenameEdit(info, offsets[i]));
		};
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}	
	
	private TextEdit getParameterRenameEdit(ParameterInfo info, int occurrenceOffset){
		return SimpleTextEdit.createReplace(occurrenceOffset, info.getOldName().length(), info.getNewName());
	}

	private IJavaSearchScope createRefactoringScope()  throws JavaModelException{
		if (fRippleMethods.length == 1)	
			return RefactoringScopeFactory.create(fRippleMethods[0]);
		return SearchEngine.createWorkspaceScope();
	}
	
	private SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws JavaModelException{
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());	
	}
	
	private ISearchPattern createSearchPattern() throws JavaModelException{
		return RefactoringSearchEngine.createSearchPattern(fRippleMethods, IJavaSearchConstants.ALL_OCCURRENCES);
	}

	private void addRenamings(IProgressMonitor pm, TextChangeManager manager) throws JavaModelException {
		try{
			TextChange change= manager.get(WorkingCopyUtil.getWorkingCopyIfExists(getCu()));
			TextEdit[] edits= getAllRenameEdits();
			pm.beginTask(RefactoringCoreMessages.getString("RenameParametersRefactoring.preview"), edits.length);  //$NON-NLS-1$
			for (int i= 0; i < edits.length; i++) {
				change.addTextEdit(RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_method_parameter"), edits[i]); //$NON-NLS-1$
			}
		}catch (CoreException e)	{
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}
		
	private void addReorderings(IProgressMonitor pm, TextChangeManager manager) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ReorderParametersRefactoring.preview"), fOccurrences.length); //$NON-NLS-1$
			for (int i= 0; i < fOccurrences.length ; i++){
				SearchResultGroup group= fOccurrences[i]; 
				List regionArrays= getParameterOccurrenceSourceRangeArrays(group);
				for (Iterator iter= regionArrays.iterator(); iter.hasNext();) {
					ISourceRange[] sourceRanges= (ISourceRange[]) iter.next();
					ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(group.getCompilationUnit());
					if (cu == null)
						continue;
					addMoveEdits(manager.get(cu), sourceRanges);
				}
				pm.worked(1);
			}
		} catch (CoreException e){
			throw new JavaModelException(e);
		}	finally{
			pm.done();
		}		
	}
	
	private void addNewParameters(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		ASTNodeMappingManager astmappingManager= new ASTNodeMappingManager();
		int i= 0;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isAdded())
				continue;
			for (int j= 0; j < fOccurrences.length; j++) {
				SearchResultGroup group= fOccurrences[j];
				ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(group.getCompilationUnit());
				if (cu == null)
					continue;
				
				if (fMethod.getNumberOfParameters() == 0){
					addParameterToEmptyLists(group, i == 0, info, manager.get(cu), astmappingManager);
				} else {
					List declarationRegionArrays= getParameterDeclarationSourceRangeArrays(group);
					String declarationText= createDeclarationString(info);
					addParameter(declarationRegionArrays, i, manager.get(cu), declarationText);
	
					List referenceRegionArrays= getParameterReferenceSourceRangeArrays(group);
					String referenceText= info.getDefaultValue();
					addParameter(referenceRegionArrays, i, manager.get(cu), referenceText);
				}	
			}	
		}
	}

	private static String createDeclarationString(ParameterInfo info) {
		return info.getType() + " " + info.getNewName();
	}

	private static String createReferenceString(ParameterInfo info) {
		return info.getDefaultValue();
	}

	private void addParameterToEmptyLists(SearchResultGroup group, boolean isFirst, ParameterInfo info, TextChange change, ASTNodeMappingManager astmappingManager) throws JavaModelException {
		CompilationUnit cuNode= astmappingManager.getAST(group.getCompilationUnit());
		SearchResult[] results= group.getSearchResults();
		for (int i= 0; i < results.length; i++) {
			SearchResult result= results[i];
			Selection selection= Selection.createFromStartEnd(result.getStart(), result.getEnd()); 
			SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, true);
			cuNode.accept(analyzer);
			ASTNode node= analyzer.getFirstSelectedNode();
			String text;
			if (isReferenceNode(node))
				text= createReferenceString(info);
			else 
				text= createDeclarationString(info);
			if (! isFirst)
				text= ", " + text;
			int offset= skipFirstOpeningBracket(group.getCompilationUnit(), node);
			change.addTextEdit("add param", SimpleTextEdit.createInsert(offset, text));
		}
	}
	
	private static boolean isReferenceNode(ASTNode node){
		if (node instanceof SimpleName && node.getParent() instanceof MethodInvocation)
			return true;
		if (node instanceof ExpressionStatement)	
			return true;
		if (node instanceof MethodInvocation)	
			return true;
		if (node instanceof SuperMethodInvocation)	
			return true;
		if (node instanceof ClassInstanceCreation)	
			return true;
		if (node instanceof ConstructorInvocation)	
			return true;
		if (node instanceof SuperConstructorInvocation)	
			return true;
		return false;	
	}

	private static int skipFirstOpeningBracket(ICompilationUnit iCompilationUnit, ASTNode node) throws JavaModelException {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(iCompilationUnit.getBuffer().getCharacters());
		scanner.resetTo(node.getStartPosition(), ASTNodes.getExclusiveEnd(node));
		try {
			int token= scanner.getNextToken();
			while(token != ITerminalSymbols.TokenNameEOF){
				if (token == ITerminalSymbols.TokenNameLPAREN)
					return scanner.getCurrentTokenStartPosition() + scanner.getCurrentTokenSource().length;
				token= scanner.getNextToken();
			}
			return node.getStartPosition(); //fallback
		} catch (InvalidInputException e) {
			return node.getStartPosition(); //fallback
		}
	}


	private void addParameter(List parameterOccurrenceList, int paramIndex, TextChange change, String infoText){
		for (Iterator regionIter= parameterOccurrenceList.iterator(); regionIter.hasNext();) {
			ISourceRange[] sourceRanges= (ISourceRange[]) regionIter.next();
			int offset= paramIndex == 0 ? sourceRanges[0].getOffset(): sourceRanges[paramIndex-1].getOffset() + sourceRanges[paramIndex-1].getLength() ;
			String text= infoText;
			if (paramIndex == 0)
				text= text + ", ";
			else
				text= ", " + text;
			change.addTextEdit("add param", SimpleTextEdit.createInsert(offset, text));
		}
	}

	/**
	 * returns a List of ISourceRange[]
	 */
	private List getParameterReferenceSourceRangeArrays(SearchResultGroup group) throws JavaModelException{
		return ReorderParameterMoveFinder.findParameterReferenceSourceRanges(group);
	}

	/**
	 * returns a List of ISourceRange[]
	 */
	private List getParameterDeclarationSourceRangeArrays(SearchResultGroup group) throws JavaModelException{
		return ReorderParameterMoveFinder.findParameterDeclarationSourceRanges(group);
	}

	/**
	 * returns a List of ISourceRange[]
	 */
	private List getParameterOccurrenceSourceRangeArrays(SearchResultGroup group) throws JavaModelException{
		return ReorderParameterMoveFinder.findParameterSourceRanges(group);
	}

	private void addMoveEdits(TextChange change, ISourceRange[] ranges) {
		int[] permutation= createPermutation();
		validateArguments(ranges, permutation);
		List edits= new ArrayList(ranges.length * 2);
		for (int i= 0; i < ranges.length; i++) {
			if (i == permutation[i])
				continue;
			MoveSourceEdit source= new MoveSourceEdit(ranges[i].getOffset(), ranges[i].getLength());
			MoveTargetEdit target= new MoveTargetEdit(ranges[permutation[i]].getOffset());
			source.setTargetEdit(target);
			edits.add(source);
			edits.add(target);
		}
		change.addTextEdit(
			RefactoringCoreMessages.getString("ReorderParametersRefactoring.editName"),  //$NON-NLS-1$
			(TextEdit[]) edits.toArray(new TextEdit[edits.size()]));
	}

	private int[] createPermutation() {
		List integers= new ArrayList(fParameterInfos.size());
		for (int i= 0, n= fParameterInfos.size(); i < n; i++) {
			ParameterInfo info= (ParameterInfo)fParameterInfos.get(i);
			if (! info.isAdded())
				integers.add(new Integer(info.getOldIndex()));
		}
		int[] result= new int[integers.size()];
		for (int i= 0; i < result.length; i++) {
			result[i]= ((Integer)integers.get(i)).intValue();
		}
		return result;
	}
	
	private static void validateArguments(ISourceRange[] ranges, int[] permutation){
		Assert.isTrue(ranges.length == permutation.length);
		int[] copy= createCopy(permutation);
		Arrays.sort(copy);
		for (int i= 0; i < copy.length; i++) {
			Assert.isTrue(copy[i] == i);
		}
	}
	
	private static int[] createCopy(int[] orig){
		int[] result= new int[orig.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= orig[i];
		}
		return result;
	}
}
