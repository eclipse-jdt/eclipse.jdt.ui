package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class ExtractConstantRefactoring extends Refactoring {

	public static final String PUBLIC= "public"; //$NON-NLS-1$
	public static final String PROTECTED= "protected"; //$NON-NLS-1$
	public static final String PACKAGE= ""; //$NON-NLS-1$
	public static final String PRIVATE= "private"; //$NON-NLS-1$

	private static abstract class ExpressionChecker extends ASTVisitor {

		private final IExpressionFragment fExpression;
		protected boolean fResult= true;

		public ExpressionChecker(IExpressionFragment ex) {
			fExpression= ex;
		}
		public boolean check() {
			fResult= true;
			fExpression.getAssociatedNode().accept(this);
			return fResult;
		}
	}

	private static class LoadTimeConstantChecker extends ExpressionChecker {
		public LoadTimeConstantChecker(IExpressionFragment ex) {
			super(ex);
		}

		public boolean visit(SuperFieldAccess node) {
			fResult= false;
			return false;
		}
		public boolean visit(SuperMethodInvocation node) {
			fResult= false;
			return false;
		}
		public boolean visit(ThisExpression node) {
			fResult= false;
			return false;
		}
		public boolean visit(FieldAccess node) {
			fResult= new LoadTimeConstantChecker((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(node.getExpression())).check();
			return false;
		}
		public boolean visit(MethodInvocation node) {
			if(node.getExpression() == null) {
				visitName(node.getName());	
			} else {
				fResult= new LoadTimeConstantChecker((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(node.getExpression())).check();
			}
			
			return false;
		}
		public boolean visit(QualifiedName node) {
			return visitName(node);
		}
		public boolean visit(SimpleName node) {
			return visitName(node);
		}
		
		private boolean visitName(Name name) {
			fResult= checkName(name);
			return false; //Do not descend further                 
		}
		
		private boolean checkName(Name name) {
			IBinding binding= name.resolveBinding();
			if(binding == null)
				return true;  /* If the binding is null because of compile errors etc., 
				                  scenarios which may have been deemed unacceptable in
				                  the presence of semantic information will be admitted. */
			
			// If name represents a member:
			if(binding instanceof IVariableBinding || binding instanceof IMethodBinding)
				return isMemberReferenceValidInClassInitialization(name);
			else if(binding instanceof ITypeBinding)
				return true;	
			else {
					/*  IPackageBinding is not expected, as a package name not
					    used as a type name prefix is not expected in such an
					    expression.  Other types are not expected either.
					 */
					Assert.isTrue(false);
					return true;		
			}
		}

		private boolean isMemberReferenceValidInClassInitialization(Name name) {
			IBinding binding= name.resolveBinding();
			Assert.isTrue(binding instanceof IVariableBinding || binding instanceof IMethodBinding);

			if(name instanceof SimpleName)
				return Modifier.isStatic(binding.getModifiers());
			else {
				Assert.isTrue(name instanceof QualifiedName);
				return checkName(((QualifiedName) name).getQualifier());
			}
		}
	}

	private static class StaticFinalConstantChecker extends ExpressionChecker {
		public StaticFinalConstantChecker(IExpressionFragment ex) {
			super(ex);
		}
		
		public boolean visit(SuperFieldAccess node) {
			fResult= false;
			return false;
		}
		public boolean visit(SuperMethodInvocation node) {
			fResult= false;
			return false;
		}
		public boolean visit(ThisExpression node) {
			fResult= false;
			return false;
		}

		public boolean visit(QualifiedName node) {
			return visitName(node);
		}
		public boolean visit(SimpleName node) {
			return visitName(node);
		}
		private boolean visitName(Name name) {
			IBinding binding= name.resolveBinding();
			if(binding == null) { 
				/* If the binding is null because of compile errors etc., 
				   scenarios which may have been deemed unacceptable in
				   the presence of semantic information will be admitted. 
				   Descend deeper.
				 */
				 return true;
			}
			
			int modifiers= binding.getModifiers();	
			if(binding instanceof IVariableBinding) {
				if (!(Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))) {
					fResult= false;
					return false;
				}		
			} else if(binding instanceof IMethodBinding) {
				if (!Modifier.isStatic(modifiers)) {
					fResult= false;
					return false;
				}
			} else if(binding instanceof ITypeBinding) {
				return false; // It's o.k.  Don't descend deeper.
		
			} else {
					/*  IPackageBinding is not expected, as a package name not
					    used as a type name prefix is not expected in such an
					    expression.  Other types are not expected either.
					 */
					Assert.isTrue(false);
					return false;		
			}
			
			//Descend deeper:
			return true;
		}
	}

	private static final String MODIFIER= "static final"; //$NON-NLS-1$

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	private final CodeGenerationSettings fSettings;

	private IExpressionFragment fSelectedExpression;
	private boolean fReplaceAllOccurrences= true; //default value

	private String fAccessModifier= PRIVATE; //default value
	private String fConstantName= ""; //$NON-NLS-1$;
	private CompilationUnit fCompilationUnitNode;

	private boolean fSelectionAllStaticFinal;
	private boolean fAllStaticFinalCheckPerformed= false;
	
	private List fBodyDeclarations;
	
	//Constant Declaration Location
	private BodyDeclaration fToInsertAfter;
	private boolean fInsertFirst;

	public ExtractConstantRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		Assert.isNotNull(settings);
		
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		
		fCu= cu;
		fSettings= settings;
	}

	public String getName() {
		return RefactoringCoreMessages.getString("ExtractConstantRefactoring.name"); //$NON-NLS-1$
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}
	
	public void setAccessModifier(String am) {
		Assert.isTrue(
			am == PRIVATE || am == PROTECTED || am == PACKAGE || am == PUBLIC
		);
		fAccessModifier= am;
	}
	
	public String getAccessModifier() {
		return fAccessModifier;	
	}

	public String guessConstantName() throws JavaModelException {
		//TODO: add guessing
		return fConstantName;
	}

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 6); //$NON-NLS-1$

			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[] { fCu }));
			if (result.hasFatalError())
				return result;

			if (!fCu.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractConstantRefactoring.syntax_error")); //$NON-NLS-1$

			initializeAST();

			return checkSelection(new SubProgressMonitor(pm, 5));
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}
	}
	
	public boolean selectionAllStaticFinal() {
		Assert.isTrue(fAllStaticFinalCheckPerformed);
		return fSelectionAllStaticFinal;
	}

	private void checkAllStaticFinal() 
		throws JavaModelException
	{
		fSelectionAllStaticFinal= isStaticFinalConstant(getSelectedExpression());
		fAllStaticFinalCheckPerformed= true;
	}

	private static boolean isStaticFinalConstant(IExpressionFragment ex) {
		return new StaticFinalConstantChecker(ex).check();
	}
	
	private static boolean isLoadTimeConstant(IExpressionFragment ex) 
		throws JavaModelException
	{
		return new LoadTimeConstantChecker(ex).check();
	}

	private String getModifier() {
		return getAccessModifier() + " " + MODIFIER;	
	}
	
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$

			IExpressionFragment selectedExpression= getSelectedExpression();

			if (selectedExpression == null) {
				String message= RefactoringCoreMessages.getString("ExtractConstantRefactoring.select_expression"); //$NON-NLS-1$
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
			}
			pm.worked(1);

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkExpressionBinding() throws JavaModelException {
		return checkExpressionFragmentIsRValue(getSelectedExpression());

		
	}
	
	private RefactoringStatus checkExpressionFragmentIsRValue(IExpressionFragment ex) 
		throws JavaModelException
	{
		/* Moved this functionality to Checks, to allow sharing with
		   ExtractTempRefactoring, others */
		switch(Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:	return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("ExtractConstantRefactoring.select_expression"), null, null, RefactoringStatusCodes.EXPRESSION_NOT_RVALUE);
			case Checks.NOT_RVALUE_VOID:	return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("ExtractConstantRefactoring.no_void"), null, null, RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID);
			case Checks.IS_RVALUE:			return new RefactoringStatus();
			default:						Assert.isTrue(false); return null;
		}		
	}

	// !!! --
	private void initializeAST() throws JavaModelException {
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkExpressionBinding());
		if(result.hasFatalError())
			return result;
		checkAllStaticFinal();

		IExpressionFragment selectedExpression= getSelectedExpression();
		if (selectedExpression.getAssociatedExpression() instanceof NullLiteral)
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractConstantRefactoring.null_literals"))); //$NON-NLS-1$
		else if (!isLoadTimeConstant(selectedExpression))
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractConstantRefactoring.not_load_time_constant"))); //$NON-NLS-1$
		
		return result;
	}

	public void setConstantName(String newName) {
		Assert.isNotNull(newName);
		fConstantName= newName;
	}

	public String getConstantName() {
		return fConstantName;
	}

	/**
	 * This method performs checks on the constant name which are
	 * quick enough to be performed every time the ui input component
	 * contents are changed.
	 */
	public RefactoringStatus checkConstantNameOnChange() 
		throws JavaModelException
	{
		if(fieldExistsInThisType())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("ExtractConstantRefactoring.field_exists", getConstantName())); //$NON-NLS-1$
		return Checks.checkConstantName(getConstantName());
	}
	
	private boolean fieldExistsInThisType() 
		throws JavaModelException
	{
		return getContainingType().getField(getConstantName()).exists();
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("ExtractConstantRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
		
		/* Note: some checks are performed on change of input widget
		 * values. (e.g. see ExtractConstantRefactoring.checkConstantNameOnChange())
		 */ 
		
		//TODO: possibly add more checking for name conflicts that might
		//      lead to a change in behaviour
		
		RefactoringStatus result= checkCompilation();
		
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkCompilation() throws JavaModelException {
		try{
			RefactoringStatus result= new RefactoringStatus();
			
			TextEdit[] edits= getAllEdits();
			TextChange change= new TextBufferChange(RefactoringCoreMessages.getString("ExtractConstantRefactoring.rename"), TextBuffer.create(fCu.getSource())); //$NON-NLS-1$
			change.addTextEdit("", edits);//$NON-NLS-1$

			String newCuSource= change.getPreviewContent();
			CompilationUnit newCUNode= AST.parseCompilationUnit(newCuSource.toCharArray(), fCu.getElementName(), fCu.getJavaProject());
			result.merge(RefactoringAnalyzeUtil.analyzeIntroducedCompileErrors(newCuSource, newCUNode, fCompilationUnitNode));
	
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} 
	}

	// !! similar to ExtractTempRefactoring equivalent
	public String getConstantSignaturePreview() throws JavaModelException {
		return getModifier() + " " + getConstantTypeName() + " " + fConstantName; //$NON-NLS-2$//$NON-NLS-1$
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("ExtractConstantRefactoring.preview"), 3); //$NON-NLS-1$
			TextChange change= new CompilationUnitChange(RefactoringCoreMessages.getString("ExtractConstantRefactoring.extract_constant"), fCu); //$NON-NLS-1$
			addConstantDeclaration(change);
			pm.worked(1);
			addImportIfNeeded(change);
			pm.worked(1);
			addReplaceExpressionWithConstant(change);
			pm.worked(1);

			return change;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}
	}

	private TextEdit[] getAllEdits() throws CoreException {
		Collection edits= new ArrayList(3);
		edits.add(createConstantDeclarationEdit());
		TextEdit importEdit= createImportEditIfNeeded();
		if (importEdit != null)
			edits.add(importEdit);
		TextEdit[] replaceEdits= createReplaceExpressionWithConstantEdits();
		for (int i= 0; i < replaceEdits.length; i++) {
			edits.add(replaceEdits[i]);
		}
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}

	// !!! analogue in ExtractTempRefactoring
	private TextEdit createConstantDeclarationEdit() throws CoreException {
		
		String text;
		int insertOffset;
		if(insertFirst()) {
			BodyDeclaration first = (BodyDeclaration) getBodyDeclarations().next();
			insertOffset = first.getStartPosition();
			text= createConstantDeclarationSource(getInitializerSource()) + getLineDelimiter() + getIndent(first);

		} else {
			BodyDeclaration insertAfter = getNodeToInsertConstantDeclarationAfter();
			Assert.isNotNull(insertAfter);
			insertOffset= insertAfter.getStartPosition() + insertAfter.getLength(); 
			text= getLineDelimiter() + getIndent(insertAfter) + createConstantDeclarationSource(getInitializerSource());
		}
		
		return SimpleTextEdit.createInsert(insertOffset, text);
	}
	
	// !! almost identical to version in ExtractTempRefactoring
	private TextEdit createImportEditIfNeeded() throws JavaModelException {
		ITypeBinding type= getSelectedExpression().getAssociatedExpression().resolveTypeBinding();
		if (type.isPrimitive())
			return null;

		ImportEdit importEdit= new ImportEdit(fCu, fSettings);
		importEdit.addImport(Bindings.getFullyQualifiedImportName(type));
		if (importEdit.isEmpty())
			return null;
		else
			return importEdit;
	}

	// !!! very similar to (same as) equivalent in ExtractTempRefactoring
	private TextEdit[] createReplaceExpressionWithConstantEdits() throws JavaModelException {
		IASTFragment[] fragmentsToReplace= getFragmentsToReplace();
		TextEdit[] result= new TextEdit[fragmentsToReplace.length];
		for (int i= 0; i < fragmentsToReplace.length; i++) {
			IASTFragment fragment= fragmentsToReplace[i];
			int offset= fragment.getStartPosition();
			int length= fragment.getLength();
			result[i]= SimpleTextEdit.createReplace(offset, length, fConstantName);
		}
		return result;
	}

	// !!!
	private void addConstantDeclaration(TextChange change) throws CoreException {
		change.addTextEdit(RefactoringCoreMessages.getString("ExtractConstantRefactoring.declare_constant"), createConstantDeclarationEdit()); //$NON-NLS-1$
	}

	// !!! very similar to equivalent in ExtractTempRefactoring
	private void addImportIfNeeded(TextChange change) throws CoreException {
		TextEdit importEdit= createImportEditIfNeeded();
		if (importEdit != null)
			change.addTextEdit(RefactoringCoreMessages.getString("ExtractConstantRefactoring.update_imports"), importEdit); //$NON-NLS-1$
	}

	// !!! very similar to equivalent in ExtractTempRefactoring
	private void addReplaceExpressionWithConstant(TextChange change) throws JavaModelException {
		TextEdit[] edits= createReplaceExpressionWithConstantEdits();
		for (int i= 0; i < edits.length; i++) {
			change.addTextEdit(RefactoringCoreMessages.getString("ExtractConstantRefactoring.replace"), edits[i]); //$NON-NLS-1$		
		}
	}

	private void computeConstantDeclarationLocation() 
		throws JavaModelException
	{
		if(isDeclarationLocationComputed())
			return;

		BodyDeclaration lastStaticDependency= null;
		Iterator decls= getBodyDeclarations();
		
		Assert.isTrue(decls.hasNext()); /* Admissable selected expressions must occur
		                                   within a body declaration.  Thus, the 
		                                   class/interface in which such an expression occurs
		                                   must have at least one body declaration */
		
		while (decls.hasNext()) {
			BodyDeclaration decl= (BodyDeclaration) decls.next();
			
			int modifiers;
			if (decl instanceof FieldDeclaration)
				modifiers= ((FieldDeclaration) decl).getModifiers();
			else if (decl instanceof Initializer)
				modifiers= ((Initializer) decl).getModifiers();
			else {
				continue; /* this declaration is not a field declaration
				              or initializer, so the placement of the constant
				              declaration relative to it does not matter */
			}
			
			if (Modifier.isStatic(modifiers) && depends(getSelectedExpression(), decl))
				lastStaticDependency= decl;
		}
		
		if(lastStaticDependency == null)
			fInsertFirst= true;
		else
			fToInsertAfter= lastStaticDependency;
	}
	
	/** bd is a static field declaration or static initializer */
	private static boolean depends(IExpressionFragment selected, BodyDeclaration bd) {
		/* We currently consider selected to depend on bd only if db includes a declaration
		 * of a static field on which selected depends.
		 * 
		 * A more accurate strategy might be to also check if bd contains (or is) a
		 * static initializer containing code which changes the value of a static field on 
		 * which selected depends.  However, if a static is written to multiple times within
		 * during class initialization, it is difficult to predict which value should be used.
		 * This would depend on which value is used by expressions instances for which the new 
		 * constant will be substituted, and there may be many of these; in each, the
		 * static field in question may have taken on a different value (if some of these uses
		 * occur within static initializers).
		 */
		
		if(bd instanceof FieldDeclaration) {
			FieldDeclaration fieldDecl = (FieldDeclaration) bd;
			for(Iterator fragments = fieldDecl.fragments().iterator(); fragments.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.next();
				SimpleName staticFieldName = fragment.getName();
				if(selected.getSubFragmentsMatching(ASTFragmentFactory.createFragmentForFullSubtree(staticFieldName)).length != 0)
					return true;
			}
		}
		return false;
	}

	private boolean isDeclarationLocationComputed() {
		return fInsertFirst == true || fToInsertAfter != null;	
	}
	
	private boolean insertFirst() 
		throws JavaModelException
	{
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fInsertFirst;
	}
	
	private BodyDeclaration getNodeToInsertConstantDeclarationAfter() 
		throws JavaModelException
	{
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fToInsertAfter;
	}
	
	private Iterator getBodyDeclarations() 
		throws JavaModelException
	{
		if(fBodyDeclarations == null)
			fBodyDeclarations= getContainingTypeDeclarationNode().bodyDeclarations();
		return fBodyDeclarations.iterator();
	}

	// !!! similar to one in ExtractTempRefactoring
	//*without the trailing indent*
	private String createConstantDeclarationSource(String initializerSource) throws CoreException {
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		String dummyInitializer= "0"; //$NON-NLS-1$
		String semicolon= ";"; //$NON-NLS-1$
		String dummyDeclaration= getModifier() + " " + getConstantTypeName() + " " + fConstantName + " = " + dummyInitializer + semicolon; //$NON-NLS-1$ //$NON-NLS-2$
		int[] position= { dummyDeclaration.length() - dummyInitializer.length() - semicolon.length()};
		StringBuffer formattedDummyDeclaration= new StringBuffer(formatter.format(dummyDeclaration, 0, position, getLineDelimiter()));
		return formattedDummyDeclaration.replace(position[0], position[0] + dummyInitializer.length(), initializerSource).toString();
	}

	// !!! Almost duplicates getTempTypeName() in ExtractTempRefactoring
	private String getConstantTypeName() throws JavaModelException {
		IExpressionFragment selection= getSelectedExpression();
		Expression expression= selection.getAssociatedExpression();
		String name= expression.resolveTypeBinding().getName();
		if (!"".equals(name) || !(expression instanceof ClassInstanceCreation)) //$NON-NLS-1$
			return name;

		ClassInstanceCreation cic= (ClassInstanceCreation) expression;
		Assert.isNotNull(cic.getAnonymousClassDeclaration());
		
		return getNameIdentifier(cic.getName());
	}

	// !!! identical to method in ExtractTempRefactoring
	//recursive
	private static String getNameIdentifier(Name name) throws JavaModelException {
		if (name.isSimpleName())
			return ((SimpleName) name).getIdentifier();
		if (name.isQualifiedName()) {
			QualifiedName qn= (QualifiedName) name;
			return getNameIdentifier(qn.getQualifier()) + "." + qn.getName().getIdentifier(); //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return ""; //$NON-NLS-1$
	}

	private String getInitializerSource() throws JavaModelException {
		return removeTrailingSemicolons(fCu.getBuffer().getText(fSelectionStart, fSelectionLength));
	}

	// !!! same as method in ExtractTempRefactoring
	//recursive
	private static String removeTrailingSemicolons(String s) {
		String arg= s.trim();
		if (!arg.endsWith(";")) //$NON-NLS-1$
			return arg;
		return removeTrailingSemicolons(arg.substring(0, arg.length() - 1));
	}

	// !!! same as method in ExtractTempRefactoring
	private String getIndent(ASTNode insertAfter) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(getFile());
			int startLine= buffer.getLineOfOffset(insertAfter.getStartPosition());
			return CodeFormatterUtil.createIndentString(buffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth()));
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}

	private String getLineDelimiter() throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(getFile());
			return buffer.getLineDelimiter(buffer.getLineOfOffset(fSelectionStart));
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}

	private static boolean isStaticFieldOrStaticInitializer(BodyDeclaration node) {
		if(node instanceof MethodDeclaration || node instanceof TypeDeclaration)
			return false;
		
		int modifiers;
		if(node instanceof FieldDeclaration) {
			modifiers = ((FieldDeclaration) node).getModifiers();
		} else if(node instanceof Initializer) {
			modifiers = ((Initializer) node).getModifiers();
		} else {
			Assert.isTrue(false);
			return false;
		}
		
		if(!Modifier.isStatic(modifiers))
			return false;
		
		return true;
	}
	/**
	 *   Elements returned by next() are BodyDeclaration
	 *   instances.
	 */
	private Iterator getReplacementScope() throws JavaModelException {
		boolean declPredecessorReached= false;
		
		Collection scope= new ArrayList();
		for(Iterator bodyDeclarations = getBodyDeclarations(); bodyDeclarations.hasNext();) {
		    BodyDeclaration bodyDeclaration= (BodyDeclaration) bodyDeclarations.next();
		    
		    if(bodyDeclaration == getNodeToInsertConstantDeclarationAfter())
		    	declPredecessorReached= true;
		    
		    if(insertFirst() || declPredecessorReached || !isStaticFieldOrStaticInitializer(bodyDeclaration))
		    	scope.add(bodyDeclaration);
		}
		return scope.iterator();
	}

	private IASTFragment[] getFragmentsToReplace() throws JavaModelException {
		List toReplace = new ArrayList();
		if (fReplaceAllOccurrences) {
			Iterator replacementScope = getReplacementScope();
			while(replacementScope.hasNext()) {
				BodyDeclaration bodyDecl = (BodyDeclaration) replacementScope.next();
				IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(bodyDecl).getSubFragmentsMatching(getSelectedExpression());
				IASTFragment[] replaceableMatches = retainOnlyReplacableMatches(allMatches);
				for(int i = 0; i < replaceableMatches.length; i++)
					toReplace.add(replaceableMatches[i]);
			}
		} else if (canReplace(getSelectedExpression()))
			toReplace.add(getSelectedExpression());
		return (IASTFragment[]) toReplace.toArray(new IASTFragment[toReplace.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List result= new ArrayList(allMatches.length);
		for (int i= 0; i < allMatches.length; i++) {
			if (canReplace(allMatches[i]))
				result.add(allMatches[i]);
		}
		return (IASTFragment[]) result.toArray(new IASTFragment[result.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		if (node.getParent() instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) node.getParent();
			if (node.equals(vdf.getName()))
				return false;
		}
		if (node.getParent() instanceof ExpressionStatement)
			return false;
		return true;
	}

	private IExpressionFragment getSelectedExpression() throws JavaModelException {
		if(fSelectedExpression != null)
			return fSelectedExpression;
		
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCompilationUnitNode, fCu);
		
		if (selectedFragment instanceof IExpressionFragment)
			return fSelectedExpression= (IExpressionFragment) selectedFragment;
		else
			return null;
	}

	//returns non-null
	private TypeDeclaration getContainingTypeDeclarationNode() throws JavaModelException {
		TypeDeclaration result= (TypeDeclaration) ASTNodes.getParent(getSelectedExpression().getAssociatedNode(), TypeDeclaration.class);  
		Assert.isNotNull(result);
		return result;
	}

	private ITypeBinding getContainingTypeBinding() throws JavaModelException {
		ITypeBinding result= getContainingTypeDeclarationNode().resolveBinding();
		Assert.isNotNull(result);
		return result;
	}

	private IType getContainingType() throws JavaModelException {
		IType type= Binding2JavaModel.find(getContainingTypeBinding(), fCu.getJavaProject());
		Assert.isNotNull(type);

		return type;
	}

	// !!! - from ExtractTempRefactoring
	private IFile getFile() throws JavaModelException {
		return ResourceUtil.getFile(fCu);
	}
}
