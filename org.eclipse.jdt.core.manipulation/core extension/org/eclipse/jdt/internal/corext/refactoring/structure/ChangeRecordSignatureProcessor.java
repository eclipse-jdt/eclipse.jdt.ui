package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.TextEditGroup;

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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
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
import org.eclipse.jdt.internal.corext.refactoring.code.Invocations;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
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

	private IDefaultValueAdvisor fDefaultValueAdvisor;

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

	class NullOccurrenceUpdate extends OccurrenceUpdate<ASTNode> {
		private ASTNode fNode;
		protected NullOccurrenceUpdate(ASTNode node, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, null, result);
			fNode= node;
		}
		@Override
		public void updateNode() throws JavaModelException {
			int start= fNode.getStartPosition();
			int length= fNode.getLength();
			String msg= "Cannot update found node: nodeType=" + fNode.getNodeType() + "; "  //$NON-NLS-1$//$NON-NLS-2$
					+ fNode.toString() + "[" + start + ", " + length + "] in " + fCuRewrite.getCu();  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			JavaManipulationPlugin.log(new Exception(msg + ":\n" + fCuRewrite.getCu().getSource().substring(start, start + length))); //$NON-NLS-1$
			fResult.addError(msg, JavaStatusContext.create(fCuRewrite.getCu(), fNode));
		}
		@Override
		protected ListRewrite getParamgumentsRewrite() {
			return null;
		}
		@Override
		protected ASTNode createNewParamgument(ParameterInfo info, List<ParameterInfo> parameterInfos, List<ASTNode> nodes) {
			return null;
		}
	}

	class RecordDeclarationUpdate extends OccurrenceUpdate<SingleVariableDeclaration> {

		private RecordDeclaration fRecDecl;

		protected RecordDeclarationUpdate(RecordDeclaration decl, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, cuRewrite.createGroupDescription(RefactoringCoreMessages.ChangeSignatureRefactoring_change_signature), result);
			fRecDecl= decl;
		}

		@Override
		public void updateNode() throws CoreException {
			changeParamguments();
			reshuffleElements();
		}

		@Override
		protected ListRewrite getParamgumentsRewrite() {
			return getASTRewrite().getListRewrite(fRecDecl, RecordDeclaration.RECORD_COMPONENTS_PROPERTY);
		}
		@Override
		protected SingleVariableDeclaration createNewParamgument(ParameterInfo info, List<ParameterInfo> parameterInfos, List<SingleVariableDeclaration> nodes) {
			// TODO Auto-generated method stub
			return null;
		}

		protected SingleVariableDeclaration getParameter(int index) {
			return (SingleVariableDeclaration) fRecDecl.recordComponents().get(index);
		}

		@Override
		protected void changeParamgumentName(ParameterInfo info) {
			SingleVariableDeclaration param= getParameter(info.getOldIndex());
			if (!info.getOldName().equals(param.getName().getIdentifier()))
				return; //don't change if original parameter name != name in rippleMethod

			String msg= RefactoringCoreMessages.ChangeSignatureRefactoring_update_parameter_references;
			TextEditGroup description= fCuRewrite.createGroupDescription(msg);
			TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(param, false);
			analyzer.perform();
			// @param tags are updated in changeJavaDocTags()
			for (SimpleName occurrence : analyzer.getReferenceAndDeclarationNodes()) {
				getASTRewrite().set(occurrence, SimpleName.IDENTIFIER_PROPERTY, info.getNewName(), description);
			}
		}

		@Override
		protected void changeParamgumentType(ParameterInfo info) {
			SingleVariableDeclaration oldParam= getParameter(info.getOldIndex());
			SingleVariableDeclaration oldSVDParam= oldParam;
			replaceTypeNode(oldSVDParam.getType(), ParameterInfo.stripEllipsis(info.getNewTypeName()), info.getNewTypeBinding());
			//removeExtraDimensions(oldSVDParam); <-- Shouldn't be needed
		}

	}

	abstract class OccurrenceUpdate <N extends ASTNode>{
	      protected final CompilationUnitRewrite fCuRewrite;
	      protected final TextEditGroup fDescription;
	      protected RefactoringStatus fResult;

	      protected OccurrenceUpdate(CompilationUnitRewrite cuRewrite, TextEditGroup description, RefactoringStatus result) {
	          fCuRewrite = cuRewrite;
	          fDescription = description;
	          fResult = result;
	      }

	      protected final ASTRewrite getASTRewrite() {
	          return fCuRewrite.getASTRewrite();
	      }

	      public abstract void updateNode() throws CoreException;

	      /**
			 * @return ListRewrite of parameters or arguments
			 * */
	      protected abstract ListRewrite getParamgumentsRewrite();

	      protected abstract N createNewParamgument(ParameterInfo info, List<ParameterInfo> parameterInfos, List<N> nodes);

	      protected final void reshuffleElements() {
	    	  ListRewrite listRewrite= getParamgumentsRewrite();
	    	  Map<N, N> newOldMap= new LinkedHashMap<>();
	    	  List<N> nodes= listRewrite.getRewrittenList();
	    	  Iterator<N> rewriteIter= nodes.iterator();
	    	  List<N> original= listRewrite.getOriginalList();
	    	  for (N n : original) {
	    		  newOldMap.put(rewriteIter.next(),n);
	    		 }
	    	  List<N> newNodes= new ArrayList<>();
	    	  for (ParameterInfo info : fParameterInfos) {
	    		  int oldIndex= info.getOldIndex();
	    		  if (info.isAdded()) {
	    			  N newParamgument= createNewParamgument(info, fParameterInfos, nodes);
	    			  if (newParamgument != null)
	    				  newNodes.add(newParamgument);
	    		  } else {
	    			  N oldNode= nodes.get(oldIndex);
	    			  N movedNode= moveNode(oldNode, getASTRewrite());
	    			  newNodes.add(movedNode);
	    		  }
	    	  }
	    	  Iterator<N> nodesIter= nodes.iterator();
				Iterator<N> newIter= newNodes.iterator();
				//replace existing nodes with new ones:
				while (nodesIter.hasNext() && newIter.hasNext()) {
					ASTNode node= nodesIter.next();
					ASTNode newNode= newIter.next();
					if (!ASTNodes.isExistingNode(node)) //XXX:should better be addressed in ListRewriteEvent.replaceEntry(ASTNode, ASTNode)
						listRewrite.replace(newOldMap.get(node), newNode, fDescription);
					else
						listRewrite.replace(node, newNode, fDescription);
				}
				//remove remaining existing nodes:
				while (nodesIter.hasNext()) {
					ASTNode node= nodesIter.next();
					if (!ASTNodes.isExistingNode(node))
						listRewrite.remove(newOldMap.get(node), fDescription);
					else
						listRewrite.remove(node, fDescription);
				}
				//add additional new nodes:
				while (newIter.hasNext()) {
					ASTNode node= newIter.next();
					listRewrite.insertLast(node, fDescription);
				}

	      }

	      protected final void changeParamguments() {
				for (ParameterInfo info : getParameterInfos()) {
					if (info.isAdded() || info.isDeleted())
						continue;

					if (info.isRenamed())
						changeParamgumentName(info);

					if (info.isTypeNameChanged())
						changeParamgumentType(info);
				}
			}

			/**
			 * @param info the parameter info
			 */
			protected void changeParamgumentName(ParameterInfo info) {
				// no-op
			}

			/**
			 * @param info the parameter info
			 */
			protected void changeParamgumentType(ParameterInfo info) {
				// no-op
			}

			protected final void replaceTypeNode(Type typeNode, String newTypeName, ITypeBinding newTypeBinding){
				Type newTypeNode= createNewTypeNode(newTypeName, newTypeBinding);
				getASTRewrite().replace(typeNode, newTypeNode, fDescription);
				//registerImportRemoveNode(typeNode);
				getTightSourceRangeComputer().addTightSourceNode(typeNode);
			}

			protected final TightSourceRangeComputer getTightSourceRangeComputer() {
				return (TightSourceRangeComputer) fCuRewrite.getASTRewrite().getExtendedSourceRangeComputer();
			}

			protected final Type createNewTypeNode(String newTypeName, ITypeBinding newTypeBinding) {
				Type newTypeNode;
				return (Type) getASTRewrite().createStringPlaceholder(newTypeName, ASTNode.SIMPLE_TYPE);
			}
	}


	public List<ParameterInfo> getParameterInfos() {
		return fParameterInfos;
	}

	private List<ParameterInfo> getTypeParameters(ClassInstanceCreation cic) {
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
				//return RefactoringStatus.createFatalErrorStatus("Could not find record instantiation at cursor position."); //$NON-NLS-1$
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

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus result) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_preview, 2);
		fChangeManager= new TextChangeManager();
		for (SearchResultGroup occurrence : fOccurrences) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			SearchResultGroup group= occurrence;
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			CompilationUnitRewrite cuRewrite;
			if (cu.equals(getCu())) {
				cuRewrite= fBaseCuRewrite;
			} else {
				cuRewrite= new CompilationUnitRewrite(cu);
				cuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
			}
			//IntroduceParameterObjectRefactoring needs to update declarations first:
			List<OccurrenceUpdate<? extends ASTNode>> deferredUpdates= new ArrayList<>();
			for (ASTNode node : ASTNodeSearchUtil.findNodes(group.getSearchResults(), cuRewrite.getRoot())) {
				OccurrenceUpdate<? extends ASTNode> update= createOccurrenceUpdate(node, cuRewrite, result);
				if (update instanceof RecordDeclarationUpdate) {
					update.updateNode();
				} else {
					deferredUpdates.add(update);
				}
			}
		}
		pm.done();
		return fChangeManager;
	}

	private OccurrenceUpdate<? extends ASTNode> createOccurrenceUpdate(ASTNode node, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
		System.out.println(node.getParent() + " " + node.getClass()); //$NON-NLS-1$
		if (node instanceof SimpleName && node.getParent() instanceof RecordDeclaration)
			return new RecordDeclarationUpdate((RecordDeclaration) node.getParent(), cuRewrite, result);
	    if (Invocations.isInvocationWithArguments(node))
	    	return new NullOccurrenceUpdate(node, cuRewrite, result);
	    return new NullOccurrenceUpdate(node, cuRewrite, result);
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

		// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=226151 : don't find binary refs for constructors for now
		//return ConstructorReferenceFinder.getConstructorOccurrences(fMethod, pm, status);
		SearchPattern declPattern= SearchPattern.createPattern(fType, IJavaSearchConstants.DECLARATIONS, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (declPattern == null) {
			return new SearchResultGroup[0];
		}
		SearchPattern refPattern= SearchPattern.createPattern(fType.getElementName(), IJavaSearchConstants.CONSTRUCTOR, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
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

	/**
	 * If this occurrence update is called from within a declaration update
	 * (i.e., to update the call inside the newly created delegate), the old
	 * node does not yet exist and therefore cannot be a move target.
	 *
	 * Normally, always use createMoveTarget as this has the advantage of
	 * being able to add changes inside changed nodes (for example, a method
	 * call within a method call, see test case #4) and preserving comments
	 * inside calls.
	 * @param oldNode original node
	 * @param rewrite an AST rewrite
	 * @return the node to insert at the target location
	 */
	protected <T extends ASTNode> T moveNode(T oldNode, ASTRewrite rewrite) {
		T movedNode;
		if (ASTNodes.isExistingNode(oldNode))
			movedNode= ASTNodes.createMoveTarget(rewrite, oldNode); //node must be one of ast
		else
			movedNode= ASTNodes.copySubtree(rewrite.getAST(), oldNode);
		return movedNode;
	}

}
