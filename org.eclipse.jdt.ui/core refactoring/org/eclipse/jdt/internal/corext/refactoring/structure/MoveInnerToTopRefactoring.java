package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTWithExistingFlattener;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.code.CodeRefactoringUtil;
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
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class MoveInnerToTopRefactoring extends Refactoring{
	
	private static final String THIS_KEYWORD= "this"; //$NON-NLS-1$
	private IType fType;
	private TextChangeManager fChangeManager;
	private final ImportEditManager fImportEditManager;
	private final CodeGenerationSettings fCodeGenerationSettings;
	private String fEnclosingInstanceFieldName;
	private ASTNodeMappingManager fASTManager;
	private DeleteSourceReferenceEdit fCutTypeEdit; 
	
	public MoveInnerToTopRefactoring(IType type, CodeGenerationSettings codeGenerationSettings){
		Assert.isTrue(type.exists());
		Assert.isNotNull(codeGenerationSettings);
		fType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fImportEditManager= new ImportEditManager(codeGenerationSettings);
		fASTManager= new ASTNodeMappingManager();
		fEnclosingInstanceFieldName= getInitialNameForEnclosingInstanceField();
	}

	private String getInitialNameForEnclosingInstanceField() {
		if (getEnclosingType() == null)
			return ""; //$NON-NLS-1$
		String name= getEnclosingType().getElementName();
		if (name.equals("")) //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
	
	public IType getInputType(){
		return fType;
	}
	
	private IType getEnclosingType(){
		return fType.getDeclaringType();
	}
	
	public String getEnclosingInstanceName(){
		return fEnclosingInstanceFieldName;
	}
	
	public RefactoringStatus checkEnclosingInstanceName(String name){
		RefactoringStatus result= Checks.checkFieldName(name);
		if (! Checks.startsWithLowerCase(name))
			result.addWarning("By convention, all names of instance fields and local variables start with lowercase letters"); 
			
		if (fType.getField(name).exists()){
			String pattern= "A field named ''{0}'' is already declared in type ''{1}'' ";
			String msg= MessageFormat.format(pattern, new String[]{name, fType.getElementName()});
			result.addError(msg, JavaSourceContext.create(fType.getField(name)));
		}	
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
		pm.beginTask("", 2);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		

			if (isInputTypeStatic())
				result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			
			if (getInputTypePackage().getCompilationUnit(getNameForNewCu()).exists()){
				String pattern= "Compilation Unit named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{getNameForNewCu(), getInputTypePackage().getElementName()});
				result.addFatalError(message);
			}	
			result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			result.merge(Checks.checkCompilationUnitName(getNameForNewCu()));
			result.merge(checkConstructorParameterNames(new SubProgressMonitor(pm, 1)));
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkConstructorParameterNames(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		ASTNode[] nodes= getConstructorDeclarationNodes(pm);
		for (int i= 0; i < nodes.length; i++) {
			MethodDeclaration constructor= (MethodDeclaration)ASTNodes.getParent(nodes[i], MethodDeclaration.class);
			for (Iterator iter= constructor.parameters().iterator(); iter.hasNext();) {
				SingleVariableDeclaration param= (SingleVariableDeclaration) iter.next();
				if (fEnclosingInstanceFieldName.equals(param.getName().getIdentifier())){
					String pattern= "Name ''{0}'' is used as a parameter name in one of the constructors of type ''{1}'' ";
					String msg= MessageFormat.format(pattern, new String[]{param.getName().getIdentifier(), fType.getElementName()});
					result.addError(msg, JavaSourceContext.create(getInputTypeCu(), param));
				}
			}
		}
		return result;
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
			pm.beginTask("", 4); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			cutType(manager);
			removeUnusedImports(new SubProgressMonitor(pm, 1));
			updateTypeReferences(manager, new SubProgressMonitor(pm, 1));
			if (isInputTypeStatic())
				pm.worked(2);
			else {
				addEnclosingInstanceDeclaration(manager);
				removeUnusedTypeModifiers(manager);
				modifyAccessesToMembersFromEnclosingInstance(manager);
				updateConstructorReferences(manager, new SubProgressMonitor(pm, 1));
				if (JavaElementUtil.getAllConstructors(fType).length == 0){
					addConstructor(manager);
					pm.worked(1);
				} else {
					modifyConstructors(manager, new SubProgressMonitor(pm, 1));
				}	
			}
			fImportEditManager.fill(manager);
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void modifyAccessesToMembersFromEnclosingInstance(TextChangeManager manager) throws CoreException {
		TypeDeclaration typeDeclaration= getTypeDeclarationNode();
		MemberAccessNodeCollector collector= new MemberAccessNodeCollector(getEnclosingType());
		typeDeclaration.accept(collector);
		modifyAccessToMethodsFromEnclosingInstance(manager, collector.getMethodInvocations());
		modifyAccessToFieldsFromEnclosingInstance(manager, collector.getFieldAccesses());
		modifyAccessToFieldsFromEnclosingInstance(manager, collector.getSimpleNames());
	}
	
	private static boolean isStatic(IBinding binding){
		return Modifier.isStatic(binding.getModifiers());
	}
	
	private void modifyAccessToFieldsFromEnclosingInstance(TextChangeManager manager, SimpleName[] simpleNames) throws CoreException {
		for (int i= 0; i < simpleNames.length; i++) {
			SimpleName simpleName= simpleNames[i];
			IBinding vb= simpleName.resolveBinding();
			if (vb == null)
				continue;
			String text;
			if (isStatic(vb))
				text= JavaModelUtil.getTypeQualifiedName(getEnclosingType()) + '.';
			else
				text= createReadAccessForEnclosingInstance() + '.';
			int offset= simpleName.getStartPosition();
			manager.get(getInputTypeCu()).addTextEdit("Update field access", SimpleTextEdit.createInsert(offset, text)); 
		}
	}

	private void modifyAccessToFieldsFromEnclosingInstance(TextChangeManager manager, FieldAccess[] fieldAccesses) throws CoreException {
		for (int i= 0; i < fieldAccesses.length; i++) {
			FieldAccess field= fieldAccesses[i];
			if (field.getExpression() != null)
				continue;
			IVariableBinding vb= resolveFieldBinding(field);
			if (vb == null)
				continue;
			String text;
			if (isStatic(vb))
				text= JavaModelUtil.getTypeQualifiedName(getEnclosingType()) + '.';
			else
				text= createReadAccessForEnclosingInstance() + '.';
			int offset= field.getStartPosition();
			manager.get(getInputTypeCu()).addTextEdit("Update field access", SimpleTextEdit.createInsert(offset, text));
		}
	}
	
	private void modifyAccessToMethodsFromEnclosingInstance(TextChangeManager manager, MethodInvocation[] methodInvocations) throws CoreException {
		for (int i= 0; i < methodInvocations.length; i++) {
			MethodInvocation method= methodInvocations[i];
			if (method.getExpression() != null)
				continue;
			IMethodBinding mb= resolveMethodBinding(method);
			if (mb == null)
				continue;
			String text;
			if (isStatic(mb))
				text= JavaModelUtil.getTypeQualifiedName(getEnclosingType()) + '.';
			else
				text= createReadAccessForEnclosingInstance() + '.';
			int offset= method.getStartPosition();
			manager.get(getInputTypeCu()).addTextEdit("Update method invocation", SimpleTextEdit.createInsert(offset, text));
		}
	}

	private TypeDeclaration getTypeDeclarationNode() throws JavaModelException {
		Selection selection= Selection.createFromStartLength(fType.getNameRange().getOffset(), fType.getNameRange().getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, true);
		fASTManager.getAST(getInputTypeCu()).accept(analyzer);
		return getInnerMostTypeDeclaration(analyzer.getFirstSelectedNode());
	}

	private void modifyConstructors(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ASTNode[] constructorNodes= getConstructorDeclarationNodes(pm);
		for (int i= 0; i < constructorNodes.length; i++) {
			MethodDeclaration decl= (MethodDeclaration)ASTNodes.getParent(constructorNodes[i], MethodDeclaration.class);
			Assert.isTrue(decl.isConstructor());
			manager.get(getInputTypeCu()).addTextEdit("Add parameter to constructor", createAddParameterToConstructorEdit(decl));
			manager.get(getInputTypeCu()).addTextEdit("Set enclosing instance field", createSetEnclosingInstanceFieldEdit(decl));
		}
	}

	private TextEdit createSetEnclosingInstanceFieldEdit(MethodDeclaration decl) throws CoreException {
		Block body= decl.getBody();
		List statements= body.statements();
		if (statements.isEmpty()){
			int  indentationLevel= 1 + CodeRefactoringUtil.getIndentationLevel(decl, ResourceUtil.getFile(getInputTypeCu()));
			String src= createEnclosingInstanceInitialization();
			String formattedCode= ToolFactory.createCodeFormatter().format(src, indentationLevel, null, getLineSeperator());
			int offset= body.getStartPosition() + 1; //XXX to skip the '{'
			return SimpleTextEdit.createInsert(offset, getLineSeperator() + formattedCode);
		} else {
			Statement first= (Statement)statements.get(0);
			if (first instanceof ConstructorInvocation){
				ConstructorInvocation ci= (ConstructorInvocation)first;
				int offsetForArg= computeOffsetForFirstArgumentOrParameter(ci.arguments(), getCompilationUnit(first), first.getStartPosition());
				String src= ci.arguments().isEmpty() ? fEnclosingInstanceFieldName: fEnclosingInstanceFieldName + ", ";
				return SimpleTextEdit.createInsert(offsetForArg, src);
			} else {			
				Statement last= (Statement)statements.get(statements.size() - 1);
				int  indentationLevel= CodeRefactoringUtil.getIndentationLevel(last, ResourceUtil.getFile(getInputTypeCu()));
				String formattedCode= format(createEnclosingInstanceInitialization(), indentationLevel);
				return SimpleTextEdit.createInsert(ASTNodes.getExclusiveEnd(last), getLineSeperator() + formattedCode);
			}	
		}
	}

	private TextEdit createAddParameterToConstructorEdit(MethodDeclaration decl) throws JavaModelException {
		int scanStart= ASTNodes.getExclusiveEnd(decl.getName());
		int offset= computeOffsetForFirstArgumentOrParameter(decl.parameters(), getInputTypeCu(), scanStart);
		String parameterDeclarationSource= createDeclarationForEnclosingInstance();
		if (decl.parameters().isEmpty())
			return SimpleTextEdit.createInsert(offset, parameterDeclarationSource);
		else 
			return SimpleTextEdit.createInsert(offset, parameterDeclarationSource + ", ");
	}

	private void addConstructor(TextChangeManager manager) throws CoreException {
		int tabWidth= CodeFormatterUtil.getTabWidth();
		String constSource= format(fType.getElementName() + '(' + createDeclarationForEnclosingInstance() + "){" + 
		                 		createEnclosingInstanceInitialization() + '}', 0);
		String[] constLines= Strings.convertIntoLines(constSource);
		MemberEdit constEdit= new MemberEdit(fType, MemberEdit.ADD_AT_BEGINNING, constLines, tabWidth);
		manager.get(getInputTypeCu()).addTextEdit("Add constructor", constEdit);
	}

	private void addEnclosingInstanceDeclaration(TextChangeManager manager) throws CoreException {
		int tabWidth= CodeFormatterUtil.getTabWidth();
		String[] fieldSource= new String[]{"private " + createDeclarationForEnclosingInstance() + ';'};
		MemberEdit memberEdit= new MemberEdit(fType, MemberEdit.ADD_AT_BEGINNING, fieldSource, tabWidth);
		manager.get(getInputTypeCu()).addTextEdit("Add enclosing instance declaration", memberEdit);
	}

	private void removeUnusedTypeModifiers(TextChangeManager manager) throws CoreException {
		ISourceRange[] modifiesRanges= getRangesOfUnneededModifiers();
		for (int i= 0; i < modifiesRanges.length; i++) {
			//add 1 to remove the space after the modifier
			TextEdit edit= SimpleTextEdit.createDelete(modifiesRanges[i].getOffset(), modifiesRanges[i].getOffset() + 1);		
			manager.get(getInputTypeCu()).addTextEdit("Delete Unused Modifier", edit);
		}
	}

	private void updateTypeReferences(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ASTNode[] nodes= ASTNodeSearchUtil.findReferenceNodes(fType, fASTManager, pm);
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			ICompilationUnit cu= getCompilationUnit(node);
			
			ImportDeclaration enclosingImport= getEnclosingImportDeclaration(node);
			if (enclosingImport != null){
				updateReferenceInImport(enclosingImport, node);
			} else {
				TextEdit edit= createReferenceUpdateEdit(node);
				if (edit != null){
					manager.get(cu).addTextEdit("Update Type Reference", edit);
					if (! getInputTypePackage().equals(cu.getParent()))
						fImportEditManager.addImportTo(getNewFullyQualifiedNameOfInputType(), cu);
				}	
			}
			if (node.getParent() instanceof ClassInstanceCreation){
				MultiTextEdit multiedit= createConstructorReferenceUpdateEdit((ClassInstanceCreation)node.getParent());
				if (multiedit != null)
					manager.get(cu).addTextEdit("Update Constructor Reference", multiedit);
			}	
		}
	}
	
	private ICompilationUnit getCompilationUnit(ASTNode node){
		return fASTManager.getCompilationUnit(node);
	}

	private void updateReferenceInImport(ImportDeclaration enclosingImport, ASTNode node) throws JavaModelException {
		IBinding importBinding= enclosingImport.resolveBinding();
		if (!(importBinding instanceof ITypeBinding))
			return;
		fImportEditManager.removeImportTo(getSourceOfImport(enclosingImport, importBinding), getCompilationUnit(node));
		fImportEditManager.addImportTo(getSourceForModifiedImport(node), getCompilationUnit(node));	
	}

	private String getSourceOfImport(ImportDeclaration enclosingImport, IBinding importBinding){
		String fullyQualifiedTypeName= Bindings.getFullyQualifiedImportName((ITypeBinding)importBinding);
		if (enclosingImport.isOnDemand())
			return fullyQualifiedTypeName +".*";
		else
			return fullyQualifiedTypeName;
	}

	private String getSourceForModifiedImport(ASTNode node) throws JavaModelException {
		ImportDeclaration enclosingImport= getEnclosingImportDeclaration(node);
		ICompilationUnit cu= getCompilationUnit(node);
		int start= enclosingImport.getName().getStartPosition();
		int end= ASTNodes.getExclusiveEnd(enclosingImport);
		String rawImportSource= cu.getBuffer().getText(start, end - start);
		String newFullyQualifiedName= new StringBuffer(rawImportSource)
														.replace(0, ASTNodes.getExclusiveEnd(node)-start, getNewFullyQualifiedNameOfInputType())
														.toString();
		return newFullyQualifiedName.substring(0, newFullyQualifiedName.length() - 1);
	}
	
	private static ImportDeclaration getEnclosingImportDeclaration(ASTNode node){
		return (ImportDeclaration)ASTNodes.getParent(node, ImportDeclaration.class);
	}

	private TextEdit createReferenceUpdateEdit(ASTNode node) {
		if (node.getNodeType() == ASTNode.QUALIFIED_NAME)
			return createReferenceUpdateEditForName((QualifiedName)node);
		else if (node.getNodeType() == ASTNode.SIMPLE_TYPE)
			return createReferenceUpdateEditForName(((SimpleType)node).getName());
		else
			return null;
	}

	private TextEdit createReferenceUpdateEditForName(Name name){
		if (name instanceof SimpleName)	
			return null;
		if (isFullyQualifiedName(name))
			return SimpleTextEdit.createReplace(name.getStartPosition(), name.getLength(), getNewFullyQualifiedNameOfInputType());
		return SimpleTextEdit.createReplace(name.getStartPosition(), name.getLength(), fType.getElementName());
	}
	
	private boolean isFullyQualifiedName(Name name) {
		ASTWithExistingFlattener flattener= new ASTWithExistingFlattener();
		name.accept(flattener);
		return flattener.getResult().equals(JavaElementUtil.createSignature(fType));
	}

	private String getNewFullyQualifiedNameOfInputType() {
		return fType.getPackageFragment().getElementName() + '.' + fType.getElementName();
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
		fCutTypeEdit= new DeleteSourceReferenceEdit(fType, getInputTypeCu());
		manager.get(getInputTypeCu()).addTextEdit("Cut type", fCutTypeEdit);
	}

	private ICompilationUnit getInputTypeCu() {
		return WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
	}

	private IChange createCompilationUnitForMovedType(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCuWC= getInputTypePackage().getCompilationUnit(getNameForNewCu());
		return new CreateTextFileChange(createPathForNewCu(), createSourceForNewCu(newCuWC, pm), true);	
	}
	
	private String createSourceForNewCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		StringBuffer buff= new StringBuffer();
		buff.append(createCuSourcePrefix(new SubProgressMonitor(pm, 1), newCu));
		buff.append(createTypeSource(new SubProgressMonitor(pm, 1)));
		return buff.toString();
	}

	private String createCuSourcePrefix(IProgressMonitor pm, ICompilationUnit newCu) throws CoreException{
		pm.beginTask("", 1); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer();
		if (fCodeGenerationSettings.createFileComments)
			buffer.append(TemplateUtil.createFileCommentsSource(newCu));
		if (! getInputTypePackage().isDefaultPackage())	
			buffer.append(createPackageDeclarationSource());
		buffer.append(createImportsSource(new SubProgressMonitor(pm, 1)));
		buffer.append(getLineSeperator());
		pm.done();
		return format(buffer.toString(), 0);
	}
	
	private String createImportsSource(IProgressMonitor pm) throws JavaModelException {
		IType[] typesReferencedInInputType= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[]{fType}, pm);
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < typesReferencedInInputType.length; i++) {
			IType iType= typesReferencedInInputType[i];
			if (! isImplicityImported(iType))
				buff.append("import ").append(JavaElementUtil.createSignature(iType)).append(';');
		}
		return buff.toString();
	}

	private boolean isImplicityImported(IType iType) {
		return iType.getParent().getElementName().equals("java.lang") || iType.getPackageFragment().equals(getInputTypePackage());
	}

	private String createTypeSource(IProgressMonitor pm) throws CoreException {
		return allignSourceBlock(computeUnalignedTypeSourceBlock(pm));
	}
	
	private String computeUnalignedTypeSourceBlock(IProgressMonitor pm) throws CoreException{
		if (! isInputTypeStatic()){
			TextChange textChange= fChangeManager.get(getInputTypeCu());
			textChange.getPreviewContent();
			return ((DeleteSourceReferenceEdit)textChange.getExecutedTextEdit(fCutTypeEdit)).getContent();
		}	
		String updatedTypeSource= MemberMoveUtil.computeNewSource(fType, pm, fImportEditManager, new IType[]{fType});
		StringBuffer updatedTypeSourceBuffer= new StringBuffer(updatedTypeSource);
		ISourceRange[] ranges= getRangesOfUnneededModifiers();
		SourceRange.reverseSortByOffset(ranges);
		int typeoffset= getTypeDefinitionOffset();
		for (int i= 0; i < ranges.length; i++) {
			ISourceRange iSourceRange= ranges[i];
			int offset= iSourceRange.getOffset()  - typeoffset;
			//add 1 to length to remove the space after
			updatedTypeSourceBuffer.delete(offset, offset + iSourceRange.getLength() + 1);
		}
		return updatedTypeSourceBuffer.toString();
	}
	
	private String allignSourceBlock(String typeCodeBlock){
		CodeBlock cb= new CodeBlock(typeCodeBlock);
		StringBuffer buffer= new StringBuffer();
		cb.fill(buffer, "", getLineSeperator()); //$NON-NLS-1$
		return buffer.toString().trim();
	}

	private int getTypeDefinitionOffset() throws JavaModelException {
		return SourceReferenceSourceRangeComputer.computeSourceRange(fType, fType.getCompilationUnit().getSource()).getOffset();
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
		return "package " + getInputTypePackage().getElementName() + ';';//$NON-NLS-1$
	}
	
	private IPath createPathForNewCu() throws JavaModelException {
		return ResourceUtil.getFile(getInputTypeCu()).getFullPath()
										.removeLastSegments(1)
										.append(getNameForNewCu());
	}

	private String getNameForNewCu() {
		return fType.getElementName() + ".java";
	}

	private String getLineSeperator() {
		try {
			return StubUtility.getLineDelimiterUsed(fType);
		} catch (JavaModelException e) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private void updateConstructorReferences(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ASTNode[] constructorReferenceNodes= getConstructorReferenceNodes(pm);
		for (int i= 0; i < constructorReferenceNodes.length; i++) {
			ASTNode refNode= constructorReferenceNodes[i];
			if (refNode instanceof SuperConstructorInvocation){
				updateConstructorReferenceInSuperCall(manager, (SuperConstructorInvocation)refNode);
			} else if (refNode.getParent() instanceof SuperConstructorInvocation){
				//XXX workaround for bug 23527
				SuperConstructorInvocation sci= (SuperConstructorInvocation)refNode.getParent();
				if (refNode != sci.getExpression())
					continue;
				IMethodBinding cb= sci.resolveConstructorBinding();
				if (cb == null)
					continue;
				if (isCorrespondingTypeBinding(cb.getDeclaringClass(), fType))
					updateConstructorReferenceInSuperCall(manager, sci);
			}
		}
	}
	
	private void updateConstructorReferenceInSuperCall(TextChangeManager manager, SuperConstructorInvocation sci) throws CoreException{
		MultiTextEdit textEdit= createConstructorReferenceUpdateEdit(sci);
		if (textEdit != null)
			manager.get(getCompilationUnit(sci)).addTextEdit("Update Constructor Reference", textEdit);
	}

	private ASTNode[] getConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		ISearchPattern pattern= createConstructorSearchPattern(fType, IJavaSearchConstants.REFERENCES);
		if (pattern == null){
			if (JavaElementUtil.getAllConstructors(fType).length != 0)
				return new ASTNode[0];
			return getImplicitConstructorReferenceNodes(pm);	
		}	
		return ASTNodeSearchUtil.searchNodes(scope, pattern, fASTManager, pm);
	}
	
	private ASTNode[] getConstructorDeclarationNodes(IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		ISearchPattern pattern= createConstructorSearchPattern(fType, IJavaSearchConstants.DECLARATIONS);
		if (pattern == null)
			return new ASTNode[0];
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
	private Collection getAllSuperConstructorInvocations(IType type) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
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
	
	private MultiTextEdit createConstructorReferenceUpdateEdit(SuperConstructorInvocation sci) throws JavaModelException {
		MultiTextEdit multi= new MultiTextEdit();
		TextEdit insertExpression= createInsertExpressionAsParamaterEdit(sci);
		if (insertExpression != null)
			multi.add(insertExpression);
		if (sci.getExpression() != null)	
			multi.add(createCutExpressionEdit(sci));	
		if (! multi.hasChildren())
			return null;	
		return multi;			
	}

	private MultiTextEdit createConstructorReferenceUpdateEdit(ClassInstanceCreation cic) throws JavaModelException {
		MultiTextEdit multi= new MultiTextEdit();
		TextEdit insertExpression= createInsertExpressionAsParamaterEdit(cic);
		if (insertExpression != null)
			multi.add(insertExpression);
		if (cic.getExpression() != null)	
			multi.add(createCutExpressionEdit(cic));
		if (! multi.hasChildren())
			return null;	
		return multi;
	}

	private TextEdit createInsertExpressionAsParamaterEdit(ClassInstanceCreation cic) throws JavaModelException{
		String text= createEnclosingInstanceCreationString(cic);
		if (! cic.arguments().isEmpty())
			text += ", ";
		return SimpleTextEdit.createInsert(computeOffsetForFirstArgument(cic), text);
	}

	private TextEdit createInsertExpressionAsParamaterEdit(SuperConstructorInvocation sci) throws JavaModelException{
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
		return getCompilationUnit(node).getBuffer().getCharacters();
	}

	private int computeOffsetForFirstArgument(ClassInstanceCreation cic) throws JavaModelException {
		return computeOffsetForFirstArgumentOrParameter(cic.arguments(), getCompilationUnit(cic), ASTNodes.getExclusiveEnd(cic.getName()));
	}
	
	private int computeOffsetForFirstArgument(SuperConstructorInvocation sci) throws JavaModelException {
		int scanStart;
		if (sci.getExpression() == null)
			scanStart= sci.getStartPosition();
		else
			scanStart= ASTNodes.getExclusiveEnd(sci.getExpression());	
		return computeOffsetForFirstArgumentOrParameter(sci.arguments(), getCompilationUnit(sci), scanStart);
	}

	private static int computeOffsetForFirstArgumentOrParameter(List arguments, ICompilationUnit cu, int scanStartPosition) throws JavaModelException {
		try {
			if (! arguments.isEmpty())
				return ((ASTNode)arguments.get(0)).getStartPosition();
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
			return getExpressionString(expression);
		else if (isInsideSubclassOfDeclaringType(cic))
			return THIS_KEYWORD;
		else if (isInsideInputType(cic))
			return createReadAccessForEnclosingInstance();
		else if (isInsideTypeNestedInDeclaringType(cic))
			return getEnclosingType().getElementName() + '.' + THIS_KEYWORD;
		return null;
	}

	private String createEnclosingInstanceCreationString(SuperConstructorInvocation sci) throws JavaModelException {
		Expression expression= sci.getExpression();
		if (expression != null)
			return getExpressionString(expression);
		else if (isInsideSubclassOfDeclaringType(sci))
			return THIS_KEYWORD;
		else if (isInsideInputType(sci))
			return createReadAccessForEnclosingInstance();
		else if (isInsideTypeNestedInDeclaringType(sci))
			return getEnclosingType().getElementName() + '.' + THIS_KEYWORD;
		return null;
	}
	
	private String getExpressionString(Expression expression) throws JavaModelException{
		return getCompilationUnit(expression).getBuffer().getText(expression.getStartPosition(), expression.getLength());
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
		return isCorrespondingTypeBinding(binding, getEnclosingType());
	}
	
	private static boolean isCorrespondingTypeBinding(ITypeBinding binding, IType type) {
		if (binding == null)
			return false;
		return Bindings.getFullyQualifiedName(binding).equals(JavaElementUtil.createSignature(type));
	}	

	private String createReadAccessForEnclosingInstance() {
		return THIS_KEYWORD + '.' + fEnclosingInstanceFieldName;
	}

	private String createEnclosingInstanceInitialization() {
		return createReadAccessForEnclosingInstance() + '=' + fEnclosingInstanceFieldName + ';';
	}

	private String createDeclarationForEnclosingInstance() {
		return JavaModelUtil.getTypeQualifiedName(getEnclosingType()) + ' ' + fEnclosingInstanceFieldName;
	}
		
	private static ISearchPattern createConstructorSearchPattern(IType type, int limitTo) throws JavaModelException {
		return RefactoringSearchEngine.createSearchPattern(JavaElementUtil.getAllConstructors(type), limitTo);
	}
	
	private static ITypeBinding getDeclaringTypeBinding(MethodInvocation methodInvocation){
		IMethodBinding binding= resolveMethodBinding(methodInvocation);
		if (binding == null)
			return null;
		return binding.getDeclaringClass();
	}

	private static ITypeBinding getDeclaringTypeBinding(FieldAccess fieldAccess){
		IVariableBinding varBinding= resolveFieldBinding(fieldAccess);
		if (varBinding == null)
			return null;
		return varBinding.getDeclaringClass();
	}

	private static IMethodBinding resolveMethodBinding(MethodInvocation method) {
		IBinding binding= method.getName().resolveBinding();
		if (binding instanceof IMethodBinding)
			return (IMethodBinding)binding;
		return null;
	}
	
	private static IVariableBinding resolveFieldBinding(FieldAccess fieldAccess) {
		return resolveFieldBinding(fieldAccess.getName());
	}

	private static IVariableBinding resolveFieldBinding(SimpleName simpleName) {
		IBinding binding= simpleName.resolveBinding();
		if (binding instanceof IVariableBinding)
			return (IVariableBinding)binding;
		return null;
	}
	
	private String format(String src, int indentationLevel){
		return ToolFactory.createCodeFormatter().format(src, indentationLevel, null, getLineSeperator());
	}
	
	private static class MemberAccessNodeCollector extends ASTVisitor{
		private final List fMethodAccesses= new ArrayList(0);
		private final List fFieldAccesses= new ArrayList(0);
		private final List fTypeAccesses= new ArrayList(0);
		private final List fSimpleNames= new ArrayList(0);
		
		private final IType fType;
		MemberAccessNodeCollector(IType type){
			fType= type;
		}
		MethodInvocation[] getMethodInvocations(){
			return (MethodInvocation[]) fMethodAccesses.toArray(new MethodInvocation[fMethodAccesses.size()]);
		}
		FieldAccess[] getFieldAccesses(){
			return (FieldAccess[]) fFieldAccesses.toArray(new FieldAccess[fFieldAccesses.size()]);
		}
		SimpleName[] getSimpleNames(){
			return (SimpleName[]) fSimpleNames.toArray(new SimpleName[fSimpleNames.size()]);
		}
		
		public boolean visit(MethodInvocation node) {
			ITypeBinding declaringClassBinding= MoveInnerToTopRefactoring.getDeclaringTypeBinding(node);
			if (declaringClassBinding != null) {
				if (MoveInnerToTopRefactoring.isCorrespondingTypeBinding(declaringClassBinding, fType)){
					fMethodAccesses.add(node);
					return false;
				}	
			}		
			return super.visit(node);
		}
		
		public boolean visit(FieldAccess node) {
			ITypeBinding declaringClassBinding= MoveInnerToTopRefactoring.getDeclaringTypeBinding(node);
			if (declaringClassBinding != null){
				if (MoveInnerToTopRefactoring.isCorrespondingTypeBinding(declaringClassBinding, fType)){
					fFieldAccesses.add(node);
					return false;
				}	
			}	
			return super.visit(node);
		}

		public boolean visit(SimpleName node) {
			if (node.getParent() instanceof QualifiedName)
				return super.visit(node);
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding){
				IVariableBinding vb= (IVariableBinding)binding;
				if (vb.isField()){
					if (MoveInnerToTopRefactoring.isCorrespondingTypeBinding(vb.getDeclaringClass(), fType)){
						fSimpleNames.add(node);
						return false;
					}
				}
			} 
			return super.visit(node);
		}
	}
}
