package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CuCollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.util.Progress;

public class ChangeRecordSignatureProcessor extends RefactoringProcessor implements IDelegateUpdating{

	IType fType;

	ASTNode fNode;

	ClassInstanceCreation fClassInstanceCreation;

	ITextSelection fSelection;

	private StubTypeContext fContextCuStartEnd;

	private List<ParameterInfo> fParameterInfos;

	private int fVisibility;
	private CompilationUnitRewrite fBaseCuRewrite;

	private SearchResultGroup[] fOccurrences;

	private TextChangeManager fChangeManager;

	private static final String CONST_CLASS_DECL = "class A{";//$NON-NLS-1$
	private static final String CONST_ASSIGN = " i=";		//$NON-NLS-1$
	private static final String CONST_CLOSE = ";}";			//$NON-NLS-1$

	public ChangeRecordSignatureProcessor(IType type, ASTNode node, ITextSelection selection) {
		// fType is the record declaration.
		// The node is the ASTNode where the refactor has started.
		this.fType = type;
		this.fNode = node;
		this.fClassInstanceCreation= resolveClassInstanceCreation(node);
		this.fVisibility= JdtFlags.getVisibilityCode(this.fClassInstanceCreation.getType().resolveBinding());
		this.fSelection = selection;
		if (node != null) {
			this.fParameterInfos = getTypeParameters(this.fClassInstanceCreation);
		}
	}

	public List<ParameterInfo> getParameterInfos() {
		return fParameterInfos;
	}

	private List<ParameterInfo> getTypeParameters(ClassInstanceCreation cic) {
		IMethodBinding mbinding = cic.resolveConstructorBinding();
		try {
			IField[] recordTypes = fType.getRecordComponents();
			List<ParameterInfo> result= new ArrayList<>(recordTypes.length);
			for (int i=0; i < recordTypes.length; i++) {
				IField fieldType = recordTypes[i];
				ParameterInfo parameterInfo = new ParameterInfo(Signature.toString(fieldType.getTypeSignature()), fieldType.getElementName(), i);
				result.add(parameterInfo);
			}
			return result;
		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e);
			return new ArrayList<>(0);
		}
		/*if (mbinding == null) mbinding = cic.resolveConstructorBinding();
		ITypeBinding[] parametersTypes = mbinding.getParameterTypes();
		String[] parametersNames = mbinding.getParameterNames();
		parametersNames.clone();
		parametersTypes.clone();
		for (int i= 0; i < parametersTypes.length; i++) {
			ParameterInfo parameterInfo;
			//We don't have  var args for record parameters so we don't need to check them.
			parameterInfo= new ParameterInfo(parametersTypes[i].getName(), parametersNames[i], i);
			result.add(parameterInfo);
		}
		return result;*/
	}

	private ClassInstanceCreation resolveClassInstanceCreation(ASTNode node) {
		if (node instanceof ClassInstanceCreation) return (ClassInstanceCreation)node;
		else {
			if(node == null || node instanceof Statement || node instanceof CompilationUnit || node instanceof BodyDeclaration ) {
				return null;
			}
		}
		return resolveClassInstanceCreation(node.getParent());
	}

	public StubTypeContext getStubTypeContext() {
		if (fContextCuStartEnd == null)
			try {
				fContextCuStartEnd= TypeContextChecker.createStubTypeContext(getCu(), fBaseCuRewrite.getRoot(), fType.getSourceRange().getOffset());
			} catch (CoreException e) {
				//cannot do anything here
				throw new RuntimeException(e);
			}
		return fContextCuStartEnd;
	}

	private ICompilationUnit getCu() {
		return fType.getCompilationUnit();
	}

	@Override
	public boolean canEnableDelegateUpdating() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getDelegateUpdating() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDelegateUpdatingTitle(boolean plural) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getDeprecateDelegates() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDelegateUpdating(boolean updating) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDeprecateDelegates(boolean deprecate) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object[] getElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessorName() {
		return RefactoringCoreMessages.ChangeSignatureRefactoring_modify_RecordParameters;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= Checks.checkIfCuBroken(fType);
			if (result.hasFatalError()) {
				return result;
			}
			/*if (fType == null || !fType.exists()) {
				//Should I place a check for it?
			}*/
			pm.worked(1);
			if (fClassInstanceCreation == null) {
				System.out.println(" ----- HERE -------"); //$NON-NLS-1$
				return null;
			}

			if (pm.isCanceled())
				throw new OperationCanceledException();

			if (fBaseCuRewrite == null || !fBaseCuRewrite.getCu().equals(getCu())) {
				fBaseCuRewrite = new CompilationUnitRewrite(getCu());
				fBaseCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
			}

			for (RefactoringStatus status : TypeContextChecker.checkRecordTypesSyntax(fType, getParameterInfos())) {
				result.merge(status);
			}
			pm.worked(1);
			return result;
		} finally {
			pm.done();
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_checking_preconditions, 8);
		RefactoringStatus result= new RefactoringStatus();
		fBaseCuRewrite.clearASTAndImportRewrites();
		fBaseCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
		if (isSignatureSameAsInitial())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ChangeSignatureRefactoring_unchanged);
		checkForDuplicateParameterNames(result);
		if (result.hasFatalError())
			return result;
		result.merge(checkSignature());
		String binaryRefsDescription= Messages.format(RefactoringCoreMessages.ReferencesInBinaryContext_ref_in_binaries_description , BasicElementLabels.getJavaElementName(fType.getElementName()));
		ReferencesInBinaryContext binaryRefs= new ReferencesInBinaryContext(binaryRefsDescription);
		fOccurrences= findOccurrences(Progress.subMonitor(pm, 1), binaryRefs, result);
		binaryRefs.addErrorIfNecessary(result);
		createChangeManager(Progress.subMonitor(pm, 1), result);
		return result;
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus result) {
		pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_preview, 2);
		fChangeManager= new TextChangeManager();
		return null;
	}

	private SearchResultGroup[] findOccurrences(IProgressMonitor pm, ReferencesInBinaryContext binaryRefs, RefactoringStatus status) throws JavaModelException{
		CuCollectingSearchRequestor requestor= new CuCollectingSearchRequestor(binaryRefs) {
			@Override
			protected void acceptSearchMatch(ICompilationUnit unit, SearchMatch match) throws CoreException {
				// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=27236 :
				if (match instanceof MethodReferenceMatch) {
					MethodReferenceMatch mrm= (MethodReferenceMatch) match;
					if (mrm.isSynthetic()) {
						return;
					}
				}
				collectMatch(match);
			}
		};

		SearchPattern pattern;
		// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=226151 : don't find binary refs for constructors for now
		//return ConstructorReferenceFinder.getConstructorOccurrences(fMethod, pm, status);
		SearchPattern declPattern= SearchPattern.createPattern(fType, IJavaSearchConstants.DECLARATIONS, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (declPattern == null) {
			return new SearchResultGroup[0];
		}
		SearchPattern refPattern= SearchPattern.createPattern(fType, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (refPattern == null) {
			return new SearchResultGroup[0];
		}
		// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=226151 : do two searches
		try {
			SearchEngine engine= new SearchEngine();
			engine.search(declPattern, SearchUtils.getDefaultSearchParticipants(), createRefactoringScope(), requestor, new NullProgressMonitor());
			engine.search(refPattern, SearchUtils.getDefaultSearchParticipants(), createRefactoringScope(), requestor, pm);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return RefactoringSearchEngine.groupByCu(requestor.getResults(), status);
	}

	private IJavaSearchScope createRefactoringScope()  throws JavaModelException{
		return RefactoringScopeFactory.create(fType, true, false);
	}

	private void checkForDuplicateParameterNames(RefactoringStatus result){
		Set<String> found= new HashSet<>();
		Set<String> doubled= new HashSet<>();
		for (ParameterInfo info : fParameterInfos) {
			String newName= info.getNewName();
			if (found.contains(newName) && !doubled.contains(newName)){
				result.addFatalError(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_duplicate_name, BasicElementLabels.getJavaElementName(newName)));
				doubled.add(newName);
			} else {
				found.add(newName);
			}
		}
	}


	private void checkParameterNamesAndValues(RefactoringStatus result) {
		int i= 1;
		for (Iterator<ParameterInfo> iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= iter.next();
			if (info.isDeleted())
				continue;
			checkParameterName(result, info, i);
			if (result.hasFatalError())
				return;
			if (info.isAdded())	{
				checkParameterDefaultValue(result, info);
				if (result.hasFatalError())
					return;
			}
		}
	}

	private void checkParameterDefaultValue(RefactoringStatus result, ParameterInfo info) {
		if (info.getDefaultValue().trim().isEmpty()){
			String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_default_value, BasicElementLabels.getJavaElementName(info.getNewName()));
			result.addFatalError(msg);
			return;
		}
		if (! isValidExpression(info.getDefaultValue())){
			String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_invalid_expression, new String[]{info.getDefaultValue()});
			result.addFatalError(msg);
		}
	}

	private RefactoringStatus checkSignature() {
		RefactoringStatus result= new RefactoringStatus();
		checkParameterNamesAndValues(result);
		if (result.hasFatalError())
			return result;

		checkForDuplicateParameterNames(result);
		// Maybe I can skip this check and return results anyway.
		if (result.hasFatalError())
			return result;

		return result;

	}

	public static boolean isValidExpression(String string){
		String trimmed= string.trim();
		if ("".equals(trimmed)) //speed up for a common case //$NON-NLS-1$
			return false;
		StringBuilder cuBuff= new StringBuilder();
		cuBuff.append(CONST_CLASS_DECL)
			  .append("Object") //$NON-NLS-1$
			  .append(CONST_ASSIGN);
		int offset= cuBuff.length();
		cuBuff.append(trimmed)
			  .append(CONST_CLOSE);
		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setSource(cuBuff.toString().toCharArray());
		CompilationUnit cu= (CompilationUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, trimmed.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		return (selected instanceof Expression) &&
				trimmed.equals(cuBuff.substring(cu.getExtendedStartPosition(selected), cu.getExtendedStartPosition(selected) + cu.getExtendedLength(selected)));
	}

	private void checkParameterName(RefactoringStatus result, ParameterInfo info, int position) {
		if (info.getNewName().trim().length() == 0) {
			result.addFatalError(Messages.format(
					RefactoringCoreMessages.ChangeSignatureRefactoring_param_name_not_empty, Integer.toString(position)));
		} else {
			result.merge(Checks.checkTempName(info.getNewName(), fType));
		}
	}


	public boolean isSignatureSameAsInitial() throws JavaModelException {
		if (fType.getRecordComponents().length == 0 && fParameterInfos.isEmpty()) {
			return true;
		}
		if (areNamesSameAsInitial() && areParameterTypesSameAsInitial()) {
			return true;
		}
		return false;
	}

	private boolean areParameterTypesSameAsInitial() {
		for (ParameterInfo info : fParameterInfos) {
			if (! info.isAdded() && ! info.isDeleted() && info.isTypeNameChanged())
				return false;
		}
		return true;
	}

	public boolean areNamesSameAsInitial() {
		for (ParameterInfo info : fParameterInfos) {
			if (info.isRenamed())
				return false;
		}
		return true;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new CompositeChange(fType.getElementName());
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

}
