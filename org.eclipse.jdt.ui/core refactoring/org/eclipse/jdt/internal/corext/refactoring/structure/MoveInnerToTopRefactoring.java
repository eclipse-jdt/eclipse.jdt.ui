package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTWithExistingFlattener;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TemplateUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class MoveInnerToTopRefactoring extends Refactoring{
	
	private IType fType;
	private TextChangeManager fChangeManager;
	private final ImportEditManager fImportEditManager;
	private final CodeGenerationSettings fCodeGenerationSettings;
	private String fEnclosingInstanceFieldName= "a";//XXX
	private ASTNodeMappingManager fASTManager; 
	
	public MoveInnerToTopRefactoring(IType type, CodeGenerationSettings codeGenerationSettings){
		Assert.isTrue(type.exists());
		Assert.isNotNull(codeGenerationSettings);
		fType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fImportEditManager= new ImportEditManager(codeGenerationSettings);
		fASTManager= new ASTNodeMappingManager();
	}
	
	public IType getInputType(){
		return fType;
	}
	
	public RefactoringStatus checkEnclosingInstanceName(String name){
		RefactoringStatus result= Checks.checkFieldName(name);
		if (! Checks.startsWithLowerCase(name))
			result.addWarning("By convention, all names of instance fields and local variables start with lowercase letters"); 
		return result;	
	}
	
	public void setEnclosingInstanceName(String name){
		Assert.isNotNull(name);
		fEnclosingInstanceFieldName= name;
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fType);	
		if (result.hasFatalError())
			return result;
		if (Checks.isTopLevel(fType))
			return RefactoringStatus.createFatalErrorStatus("This refactoring is available only on nested types.");
		if (! JdtFlags.isStatic(fType)) //XXX for now
			return RefactoringStatus.createFatalErrorStatus("This refactoring is available only on static nested types.");
		return result;
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
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fType);
		if (orig == null || ! orig.exists()){
			String key= "The selected type has been deleted from ''{0}''";
			String message= MessageFormat.format(key, new String[]{getInputTypeCu().getElementName()});
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fType= orig;
		
		return Checks.checkIfCuBroken(fType);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		

			if (isInputTypeStatic())
				result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			
			if (getInputTypePackage().getCompilationUnit(getNameForNewCu()).exists()){
				String pattern= "Compilation Unit named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{getNameForNewCu(), getInputTypePackage().getElementName()});
				result.addFatalError(message);
			}	
			result.merge(Checks.checkCompilationUnitName(getNameForNewCu()));
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

	private boolean isInputTypeStatic() throws JavaModelException {
		return JdtFlags.isStatic(fType);
	}


	private RefactoringStatus checkInterfaceTypeName() throws JavaModelException {
		IType type= Checks.findTypeInPackage(getInputTypePackage(), fType.getElementName());
		if (type == null || ! type.exists())
			return null;
		String pattern= "Type named ''{0}'' already exists in package ''{1}''";
		String message= MessageFormat.format(pattern, new String[]{fType.getElementName(), getInputTypePackage().getElementName()});
		return RefactoringStatus.createFatalErrorStatus(message);
	}
	
	private IPackageFragment getInputTypePackage() {
		return fType.getPackageFragment();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Move Nested Type to Top Level";
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Creating change", 1);
			CompositeChange builder= new CompositeChange("Move Nested Type to Top Level");
			builder.addAll(fChangeManager.getAllChanges());
			builder.add(createCompilationUnitForMovedType(new SubProgressMonitor(pm, 1)));
			return builder;	
		} catch(CoreException e){
			throw new JavaModelException(e);
		}	
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 3); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			cutType(manager);
			removeUnusedImports(new SubProgressMonitor(pm, 1));
			updateTypeReferences(manager, new SubProgressMonitor(pm, 1));
			if (! isInputTypeStatic() && fType.isClass())
				updateConstructorReferences(manager, new SubProgressMonitor(pm, 1));
			fImportEditManager.fill(manager);
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateTypeReferences(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ASTNode[] nodes= ASTNodeSearchUtil.findReferenceNodes(fType, fASTManager, pm);
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			TextEdit edit= createReferenceUpdateEdit(node);
			ICompilationUnit cu= fASTManager.getCompilationUnit(node);
			if (edit != null){
				manager.get(cu).addTextEdit("Update Type Reference", edit);
				if (! getInputTypePackage().equals(cu.getParent()))
					fImportEditManager.addImportTo(getNewFullyQualifiedNameOfInputType(), cu);
			}	
			if (node.getParent() instanceof ClassInstanceCreation){
				MultiTextEdit multiedit= createConstructorReferenceUpdateEdit((ClassInstanceCreation)node.getParent());
				if (multiedit != null)
					manager.get(cu).addTextEdit("Update Construcor Reference", multiedit);
			}	
		}
	}

	private TextEdit createReferenceUpdateEdit(ASTNode node) {
		if (node.getNodeType() == ASTNode.QUALIFIED_NAME){
			return createReferenceUpdateEditForName((QualifiedName)node);
		} else if (node.getNodeType() == ASTNode.SIMPLE_TYPE){
			return createReferenceUpdateEditForName(((SimpleType)node).getName());
		} else
			return null;
	}

	private TextEdit createReferenceUpdateEditForName(Name name){
		if (isFullyQualifiedName(name))
			return SimpleTextEdit.createReplace(name.getStartPosition(), name.getLength(), getNewFullyQualifiedNameOfInputType());
		return SimpleTextEdit.createReplace(name.getStartPosition(), name.getLength(), fType.getElementName());
	}
	
	private boolean isFullyQualifiedName(Name name) {
		ASTWithExistingFlattener flattener= new ASTWithExistingFlattener();
		name.accept(flattener);
		String result= flattener.getResult();
		return result.equals(JavaElementUtil.createSignature(fType));
	}

	private String getNewFullyQualifiedNameOfInputType() {
		return fType.getPackageFragment().getElementName() + "." + fType.getElementName();
	}


	private void removeUnusedImports(IProgressMonitor pm) throws CoreException {
		IType[] types= getTypesReferencedOnlyInInputType(pm);
		for (int i= 0; i < types.length; i++) {
			fImportEditManager.removeImportTo(types[i], getInputTypeCu());
		}
	}
	
	private IType[] getTypesReferencedOnlyInInputType(IProgressMonitor pm) throws JavaModelException{
		//XXX
		return new IType[0];
	}

	private void cutType(TextChangeManager manager) throws CoreException {
		DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(fType, getInputTypeCu());
		manager.get(getInputTypeCu()).addTextEdit("Cut type", edit);
	}

	private ICompilationUnit getInputTypeCu() {
		return WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
	}

	//----- methods related to creation of the new cu -------
	private IChange createCompilationUnitForMovedType(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCuWC= getInputTypePackage().getCompilationUnit(getNameForNewCu());
		return new CreateTextFileChange(createPathForNewCu(), createSourceForNewCu(newCuWC, pm), true);	
	}

	private String createSourceForNewCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2);
		StringBuffer buff= new StringBuffer();
		buff.append(createCuSourcePrefix(new SubProgressMonitor(pm, 1), newCu));
		buff.append(createTypeSource(new SubProgressMonitor(pm, 1)));
		pm.done();
		return buff.toString();
	}

	private String createCuSourcePrefix(IProgressMonitor pm, ICompilationUnit newCu) throws CoreException{
		pm.beginTask("", 1);
		StringBuffer buffer= new StringBuffer();
		if (fCodeGenerationSettings.createFileComments)
			buffer.append(TemplateUtil.createFileCommentsSource(newCu));
		if (! getInputTypePackage().isDefaultPackage())	
			buffer.append(createPackageDeclarationSource());
		buffer.append(createImportsSource(new SubProgressMonitor(pm, 1)));
		buffer.append(getLineSeperator());
		ICodeFormatter codeFormatter= ToolFactory.createCodeFormatter();
		pm.done();
		return codeFormatter.format(buffer.toString(), 0, null, getLineSeperator());
	}
	
	private String createImportsSource(IProgressMonitor pm) throws JavaModelException {
		IType[] typesReferencedInInputType= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[]{fType}, pm);
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < typesReferencedInInputType.length; i++) {
			IType iType= typesReferencedInInputType[i];
			if (! isImplicityImported(iType))
				buff.append("import ").append(JavaElementUtil.createSignature(iType)).append(";");
		}
		return buff.toString();
	}

	private boolean isImplicityImported(IType iType) {
		return iType.getParent().getElementName().equals("java.lang") || iType.getPackageFragment().equals(getInputTypePackage());
	}

	private String createTypeSource(IProgressMonitor pm) throws JavaModelException {
		String updatedTypeSource= getUpdatedInputTypeSource(pm);
		StringBuffer updatedTypeSourceBuffer= new StringBuffer(updatedTypeSource);
		ISourceRange[] ranges= getRangesOfUnneededModifiers();
		SourceRange.reverseSortByOffset(ranges);
		int typeoffset= getTypeDefinitionOffset().getOffset();
		for (int i= 0; i < ranges.length; i++) {
			ISourceRange iSourceRange= ranges[i];
			int offset= iSourceRange.getOffset()  - typeoffset;
			//add 1 to length to remove the space after
			updatedTypeSourceBuffer.delete(offset, offset + iSourceRange.getLength() + 1);
		}
		CodeBlock cb= new CodeBlock(updatedTypeSourceBuffer.toString());
		StringBuffer buffer= new StringBuffer();
		cb.fill(buffer, "", getLineSeperator());
		return buffer.toString().trim();
	}

	private String getUpdatedInputTypeSource(IProgressMonitor pm) throws JavaModelException {
		if (isInputTypeStatic())
			return MemberMoveUtil.computeNewSource(fType, pm, fImportEditManager, new IType[]{fType});

		String rawSource= SourceReferenceSourceRangeComputer.computeSource(fType);	
		//XXX
		return rawSource;
	}

	private ISourceRange getTypeDefinitionOffset() throws JavaModelException {
		return SourceReferenceSourceRangeComputer.computeSourceRange(fType, fType.getCompilationUnit().getSource());
	}

	private ISourceRange[] getRangesOfUnneededModifiers() throws JavaModelException {
		try {
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(fType.getCompilationUnit().getBuffer().getCharacters());
			scanner.resetTo(fType.getSourceRange().getOffset(), fType.getNameRange().getOffset());
			List result= new ArrayList(2);
			int token= scanner.getNextToken();
			while(token != ITerminalSymbols.TokenNameEOF){
				switch (token){
					case ITerminalSymbols.TokenNamestatic:
					case ITerminalSymbols.TokenNameprotected:
					case ITerminalSymbols.TokenNameprivate:
						result.add(new SourceRange(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenStartPosition() +1));
						break;
				}
				token= scanner.getNextToken();
			}
			return (ISourceRange[]) result.toArray(new ISourceRange[result.size()]);
		} catch (InvalidInputException e) {
			return new ISourceRange[0];
		}
	}

	private String createPackageDeclarationSource() {
		return "package " + getInputTypePackage().getElementName() + ";";//$NON-NLS-2$ //$NON-NLS-1$
	}
	
	private IPath createPathForNewCu() throws JavaModelException {
		return ResourceUtil.getFile(getInputTypeCu()).getFullPath()
										.removeLastSegments(1)
										.append(getNameForNewCu());
	}

	private String getNameForNewCu() {
		return fType.getElementName() + ".java";
	}

	private static String getLineSeperator() {
		return System.getProperty("line.separator", "\n");//$NON-NLS-1$ //$NON-NLS-2$
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	//---
	private void updateConstructorReferences(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ASTNode[] constructorReferenceNodes= getConstructorReferenceNodes(pm);
		String editName= "Update Constructor Reference";
		for (int i= 0; i < constructorReferenceNodes.length; i++) {
			ASTNode refNode= constructorReferenceNodes[i];
			MultiTextEdit textEdit= createConstructorReferenceUpdateEdit(refNode);
			if (textEdit != null) //XXX
				manager.get(fASTManager.getCompilationUnit(refNode)).addTextEdit(editName, textEdit);
		}
	}

	private ASTNode[] getConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		ISearchPattern pattern= createPatternForConstructorReferences(fType);
		if (pattern == null){
			if (JavaElementUtil.getAllConstructors(fType).length != 0)
				return new ASTNode[0];
			return getImplicitConstructorReferenceNodes(pm);	
		}	
		return ASTNodeSearchUtil.searchNodes(scope, pattern, fASTManager, pm);
	}

	private ASTNode[] getImplicitConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException {
		ITypeHierarchy hierarchy= fType.newTypeHierarchy(pm);
		IType[] subTypes= hierarchy.getAllSubtypes(fType);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			if (! subTypes[i].isBinary())
				result.addAll(getAllSuperConstructorInvocations(subTypes[i]));
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	//Collection of ASTNodes
	private Collection getAllSuperConstructorInvocations(IType iType) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(iType);
		List result= new ArrayList(constructors.length);
		for (int i= 0; i < constructors.length; i++) {
			ASTNode superCall= getSuperConstructorCall(constructors[i]);
			if (superCall != null)
				result.add(superCall);
		}
		return result;
	}

	private ASTNode getSuperConstructorCall(IMethod constructor) throws JavaModelException {
		Assert.isTrue(constructor.isConstructor());
		MethodDeclaration constructorNode= getMethodDeclarationNode(constructor);
		Assert.isTrue(constructorNode.isConstructor());
		Block body= constructorNode.getBody();
		Assert.isNotNull(body);
		List statements= body.statements();
		if (! statements.isEmpty() && statements.get(0) instanceof SuperConstructorInvocation)
			return (SuperConstructorInvocation)statements.get(0);
		return null;
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException {
		Selection selection= Selection.createFromStartLength(iMethod.getNameRange().getOffset(), iMethod.getNameRange().getLength());
		SelectionAnalyzer selectionAnalyzer= new SelectionAnalyzer(selection, true);
		fASTManager.getAST(iMethod.getCompilationUnit()).accept(selectionAnalyzer);
		ASTNode node= selectionAnalyzer.getFirstSelectedNode();
		if (node == null)
			node= selectionAnalyzer.getLastCoveringNode();
		if (node == null)	
			return null;
		return (MethodDeclaration)ASTNodes.getParent(node, MethodDeclaration.class);
	}
	
	private MultiTextEdit createConstructorReferenceUpdateEdit(ASTNode refNode) throws JavaModelException {
		Assert.isTrue(! (refNode instanceof ClassInstanceCreation));
		if (refNode instanceof SuperConstructorInvocation){
			SuperConstructorInvocation sci= (SuperConstructorInvocation)refNode;
			MultiTextEdit multi= new MultiTextEdit();
			TextEdit insertExpression= createInsertExpressionAsParamaterEdit(sci);
			if (insertExpression != null)
				multi.add(insertExpression);
			if (sci.getExpression() != null)	
				multi.add(createCutExpressionEdit(sci));	
			return multi;			
		}
		return null;
	}

	private MultiTextEdit createConstructorReferenceUpdateEdit(ClassInstanceCreation cic) throws JavaModelException {
		MultiTextEdit multi= new MultiTextEdit();
		TextEdit insertExpression= createInsertExpressionAsParamaterEdit(cic);
		if (insertExpression != null)
			multi.add(insertExpression);
		if (cic.getExpression() != null)	
			multi.add(createCutExpressionEdit(cic));	
		return multi;
	}

	private TextEdit createInsertExpressionAsParamaterEdit(ClassInstanceCreation cic) throws JavaModelException{
		 if (isInsideInputType(cic))
		 	return null;
		String text= createEnclosingInstanceCreationString(cic);
		if (! cic.arguments().isEmpty())
			text += ", ";
		return SimpleTextEdit.createInsert(computeOffsetForFirstArgument(cic), text);
	}

	private TextEdit createInsertExpressionAsParamaterEdit(SuperConstructorInvocation sci) throws JavaModelException{
		 if (isInsideInputType(sci))
		 	return null;
		String text= createEnclosingInstanceCreationString(sci);
		if (! sci.arguments().isEmpty())
			text += ", ";
		return SimpleTextEdit.createInsert(computeOffsetForFirstArgument(sci), text);
	}

	private TextEdit createCutExpressionEdit(ClassInstanceCreation cic) throws JavaModelException {
		return createCutExpressionEdit(cic.getExpression(), ITerminalSymbols.TokenNamenew);
	}

	private TextEdit createCutExpressionEdit(SuperConstructorInvocation sci) throws JavaModelException {
		return createCutExpressionEdit(sci.getExpression(), ITerminalSymbols.TokenNamesuper);
	}

	private TextEdit createCutExpressionEdit(Expression expression, int separatingToken) throws JavaModelException  {
		try {
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(getCompilationUnitSource(expression));
			scanner.resetTo(ASTNodes.getExclusiveEnd(expression), scanner.getSource().length);
			int token= scanner.getNextToken();
			Assert.isTrue(token == ITerminalSymbols.TokenNameDOT);
			token= scanner.getNextToken();
			Assert.isTrue(token == separatingToken);
			int cutStart= expression.getStartPosition();
			int cutEnd= scanner.getCurrentTokenStartPosition();
			int cutLength= cutEnd - cutStart;
			return SimpleTextEdit.createDelete(cutStart, cutLength);
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
	}
	
	private char[] getCompilationUnitSource(ASTNode node) throws JavaModelException{
		return fASTManager.getCompilationUnit(node).getBuffer().getCharacters();
	}

	private int computeOffsetForFirstArgument(ClassInstanceCreation cic) throws JavaModelException {
		return computeOffsetForFirstArgument(cic.arguments(), fASTManager.getCompilationUnit(cic), ASTNodes.getExclusiveEnd(cic.getName()));
	}
	
	private int computeOffsetForFirstArgument(SuperConstructorInvocation sci) throws JavaModelException {
		int scanStart;
		if (sci.getExpression() == null)
			scanStart= sci.getStartPosition();
		else
			scanStart= ASTNodes.getExclusiveEnd(sci.getExpression());	
		return computeOffsetForFirstArgument(sci.arguments(), fASTManager.getCompilationUnit(sci), scanStart);
	}

	private int computeOffsetForFirstArgument(List arguments, ICompilationUnit cu, int scanStartPosition) throws JavaModelException {
		try {
			if (! arguments.isEmpty())
				return ((Expression)arguments.get(0)).getStartPosition();
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(cu.getBuffer().getCharacters());
			scanner.resetTo(scanStartPosition, scanner.getSource().length);
			int token= scanner.getNextToken();
			while(token != ITerminalSymbols.TokenNameLPAREN)
				token= scanner.getNextToken();
			Assert.isTrue(token == ITerminalSymbols.TokenNameLPAREN);
			return scanner.getCurrentTokenEndPosition() + 1;
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
	}
		
	private String createEnclosingInstanceCreationString(ClassInstanceCreation cic) throws JavaModelException {
		Expression expression= cic.getExpression();
		if (expression != null)
			return fASTManager.getCompilationUnit(cic).getBuffer().getText(expression.getStartPosition(), expression.getLength());
		if (isInsideSubclassOfDeclaringType(cic))
			return "this";
		else if (isInsideTypeNestedInDeclaringType(cic))
			return getInputType().getDeclaringType().getElementName() + ".this";
		return null;
	}

	private String createEnclosingInstanceCreationString(SuperConstructorInvocation sci) throws JavaModelException {
		Expression expression= sci.getExpression();
		if (expression != null)
			return fASTManager.getCompilationUnit(sci).getBuffer().getText(expression.getStartPosition(), expression.getLength());
		if (isInsideSubclassOfDeclaringType(sci))
			return "this";
		else if (isInsideTypeNestedInDeclaringType(sci))
			return getInputType().getDeclaringType().getElementName() + ".this";
		return null;
	}

	private boolean isInsideSubclassOfDeclaringType(ASTNode node) {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		TypeDeclaration typeDeclar= getInnerMostTypeDeclaration(node);
		Assert.isNotNull(typeDeclar);
		
		AnonymousClassDeclaration anon= (AnonymousClassDeclaration)ASTNodes.getParent(node, AnonymousClassDeclaration.class);
		boolean isAnonymous= anon != null && ASTNodes.isParent(anon, typeDeclar);
		if (isAnonymous)
			return isSubclassBindingOfEnclosingType(anon.resolveBinding());
		return isSubclassBindingOfEnclosingType(typeDeclar.resolveBinding());
	}
	
	private boolean isSubclassBindingOfEnclosingType(ITypeBinding binding){
		while(binding != null){
			if (isEnclosingTypeBinding(binding))
				return true;
			binding= binding.getSuperclass();	
		}	
		return false;
	}

	private boolean isInsideTypeNestedInDeclaringType(ASTNode node) {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		TypeDeclaration typeDeclar= getInnerMostTypeDeclaration(node);
		Assert.isNotNull(typeDeclar);
		ITypeBinding enclosing= typeDeclar.resolveBinding();
		while(enclosing != null){
			if (isEnclosingTypeBinding(enclosing))
				return true;
			enclosing= enclosing.getDeclaringClass();	
		}		
		return false;
	}

	private boolean isInsideInputType(ASTNode node) throws JavaModelException {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		ISourceRange range= fType.getSourceRange();
		return (node.getStartPosition() >= range.getOffset()
					&& ASTNodes.getExclusiveEnd(node) <= range.getOffset() + range.getLength());
	}

	private static TypeDeclaration getInnerMostTypeDeclaration(ASTNode node) {
		return (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
	}

	private boolean isEnclosingTypeBinding(ITypeBinding binding) {
		if (binding == null)
			return false;
		return Bindings.getFullyQualifiedName(binding).equals(JavaElementUtil.createSignature(fType.getDeclaringType()));
	}

	private String createReadAccessForEnclosingInstance() {
		return "this." + Character.toLowerCase(fEnclosingInstanceFieldName.charAt(0)) + fEnclosingInstanceFieldName.substring(1);
	}

	private static ISearchPattern createPatternForConstructorReferences(IType type) throws JavaModelException {
		return RefactoringSearchEngine.createSearchPattern(JavaElementUtil.getAllConstructors(type), IJavaSearchConstants.REFERENCES);
	}
	
}
