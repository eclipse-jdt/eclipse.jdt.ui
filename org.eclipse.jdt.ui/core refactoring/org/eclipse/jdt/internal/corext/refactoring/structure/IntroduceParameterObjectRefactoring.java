/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.dom.TypeBindingVisitor;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class IntroduceParameterObjectRefactoring extends ChangeSignatureRefactoring {

	private final class ParameterObjectCreator implements IDefaultValueAdvisor {
		public Expression createDefaultExpression(List invocationArguments, ParameterInfo addedInfo, List parameterInfos,
				MethodDeclaration enclosingMethod, boolean isRecursive, CompilationUnitRewrite cuRewrite) {
			final AST ast= cuRewrite.getAST();
			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			if (isRecursive && canReuseParameterObject(invocationArguments, addedInfo, parameterInfos, enclosingMethod)) {
				return ast.newSimpleName(addedInfo.getNewName());
			}
			ClassInstanceCreation classCreation= ast.newClassInstanceCreation();

			int startPosition= enclosingMethod != null ? enclosingMethod.getStartPosition() : cuRewrite.getRoot().getStartPosition();
			classCreation.setType(fParameterObjectFactory.createType(fCreateAsTopLevel, cuRewrite, startPosition));
			List constructorArguments= classCreation.arguments();
			for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				if (isValidField(pi)) {
					if (pi.isOldVarargs()) {
						boolean isLastParameter= !iter.hasNext();
						constructorArguments.addAll(computeVarargs(invocationArguments, pi, isLastParameter, cuRewrite));
					} else {
						Expression exp= (Expression) invocationArguments.get(pi.getOldIndex());
						importNodeTypes(exp, cuRewrite);
						constructorArguments.add(moveNode(exp, rewrite));
					}
				}
			}
			return classCreation;
		}

		public Type createType(String newTypeName, int startPosition, CompilationUnitRewrite cuRewrite) {
			return fParameterObjectFactory.createType(fCreateAsTopLevel, cuRewrite, startPosition);
		}

		private boolean canReuseParameterObject(List invocationArguments, ParameterInfo addedInfo, List parameterInfos,
				MethodDeclaration enclosingMethod) {
			Assert.isNotNull(enclosingMethod);
			List parameters= enclosingMethod.parameters();
			for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				if (isValidField(pi)) {
					if (!pi.isInlined())
						return false;
					ASTNode node= (ASTNode) invocationArguments.get(pi.getOldIndex());
					if (!isParameter(pi, node, parameters, addedInfo.getNewName())) {
						return false;
					}
				}
			}
			return true;
		}

		private List computeVarargs(List invocationArguments, ParameterInfo varArgPI, boolean isLastParameter, CompilationUnitRewrite cuRewrite) {
			boolean isEmptyVarArg= varArgPI.getOldIndex() >= invocationArguments.size();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getAST();
			ASTNode lastNode= isEmptyVarArg ? null : (ASTNode) invocationArguments.get(varArgPI.getOldIndex());
			List constructorArguments= new ArrayList();
			if (lastNode instanceof ArrayCreation) {
				ArrayCreation creation= (ArrayCreation) lastNode;
				ITypeBinding arrayType= creation.resolveTypeBinding();
				if (arrayType != null && arrayType.isAssignmentCompatible(varArgPI.getNewTypeBinding())) {
					constructorArguments.add(moveNode(creation, rewrite));
					return constructorArguments;
				}
			}
			if (isLastParameter) {
				// copy all varargs
				for (int i= varArgPI.getOldIndex(); i < invocationArguments.size(); i++) {
					ASTNode node= (ASTNode) invocationArguments.get(i);
					importNodeTypes(node, cuRewrite);
					constructorArguments.add(moveNode(node, rewrite));
				}
			} else { // new signature would be String...args, int
				if (lastNode instanceof NullLiteral) {
					NullLiteral nullLiteral= (NullLiteral) lastNode;
					constructorArguments.add(moveNode(nullLiteral, rewrite));
				} else {
					ArrayCreation creation= ast.newArrayCreation();
					creation.setType((ArrayType) importBinding(varArgPI.getNewTypeBinding(), cuRewrite));
					ArrayInitializer initializer= ast.newArrayInitializer();
					List expressions= initializer.expressions();
					for (int i= varArgPI.getOldIndex(); i < invocationArguments.size(); i++) {
						ASTNode node= (ASTNode) invocationArguments.get(i);
						importNodeTypes(node, cuRewrite);
						expressions.add(moveNode(node, rewrite));
					}
					if (expressions.isEmpty())
						creation.dimensions().add(ast.newNumberLiteral("0")); //$NON-NLS-1$
					else
						creation.setInitializer(initializer);
					constructorArguments.add(creation);
				}
			}
			return constructorArguments;
		}

		public Type importBinding(ITypeBinding newTypeBinding, CompilationUnitRewrite cuRewrite) {
			Type type= cuRewrite.getImportRewrite().addImport(newTypeBinding, cuRewrite.getAST());
			cuRewrite.getImportRemover().registerAddedImports(type);
			return type;
		}

		private void importNodeTypes(ASTNode node, final CompilationUnitRewrite cuRewrite) {
			ASTResolving.visitAllBindings(node, new TypeBindingVisitor() {
				public boolean visit(ITypeBinding nodeBinding) {
					importBinding(nodeBinding, cuRewrite);
					return false;
				}
			});
		}
	}

	private boolean isParameter(ParameterInfo pi, ASTNode node, List enclosingMethodParameters, String qualifier) {
		if (node instanceof Name) {
			Name name= (Name) node;
			IVariableBinding binding= ASTNodes.getVariableBinding(name);
			if (binding != null && binding.isParameter()) {
				return binding.getName().equals(getNameInScope(pi, enclosingMethodParameters));
			} else {
				if (node instanceof QualifiedName) {
					QualifiedName qn= (QualifiedName) node;
					return qn.getFullyQualifiedName().equals(JavaModelUtil.concatenateName(qualifier, getNameInScope(pi, enclosingMethodParameters)));
				}
			}
		}
		return false;
	}

	private final class RewriteParameterBody extends BodyUpdater {
		private boolean fParameterClassCreated= false;

		public void updateBody(MethodDeclaration methodDeclaration, final CompilationUnitRewrite cuRewrite, RefactoringStatus result) throws CoreException {
			// ensure that the parameterObject is imported
			fParameterObjectFactory.createType(fCreateAsTopLevel, cuRewrite, methodDeclaration.getStartPosition());
			if (cuRewrite.getCu().equals(getCompilationUnit()) && !fParameterClassCreated) {
				createParameterClass(methodDeclaration, cuRewrite);
				fParameterClassCreated= true;
			}
			Block body= methodDeclaration.getBody();
			final List parameters= methodDeclaration.parameters();
			if (body != null) { // abstract methods don't have bodies
				final ASTRewrite rewriter= cuRewrite.getASTRewrite();
				ListRewrite bodyStatements= rewriter.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				List managedParams= getParameterInfos();
				for (Iterator iter= managedParams.iterator(); iter.hasNext();) {
					final ParameterInfo pi= (ParameterInfo) iter.next();
					if (isValidField(pi)) {
						if (isReadOnly(pi, body, parameters, null)) {
							body.accept(new ASTVisitor(false) {

								public boolean visit(SimpleName node) {
									updateSimpleName(rewriter, pi, node, parameters, cuRewrite.getCu().getJavaProject());
									return false;
								}

							});
							pi.setInlined(true);
						} else {
							ExpressionStatement initializer= fParameterObjectFactory.createInitializer(pi, getParameterName(), cuRewrite);
							bodyStatements.insertFirst(initializer, null);
						}
					}
				}
			}


		}

		private void updateSimpleName(ASTRewrite rewriter, ParameterInfo pi, SimpleName node, List enclosingParameters, IJavaProject project) {
			AST ast= rewriter.getAST();
			IBinding binding= node.resolveBinding();
			Expression replacementNode= fParameterObjectFactory.createFieldReadAccess(pi, getParameterName(), ast, project);
			if (binding instanceof IVariableBinding) {
				IVariableBinding variable= (IVariableBinding) binding;
				if (variable.isParameter() && variable.getName().equals(getNameInScope(pi, enclosingParameters))) {
					rewriter.replace(node, replacementNode, null);
				}
			} else {
				ASTNode parent= node.getParent();
				if (! (parent instanceof QualifiedName || parent instanceof FieldAccess || parent instanceof SuperFieldAccess)) {
					if (node.getIdentifier().equals(getNameInScope(pi, enclosingParameters))) {
						rewriter.replace(node, replacementNode, null);
					}
				}
			}
		}

		private boolean isReadOnly(final ParameterInfo pi, Block block, final List enclosingMethodParameters, final String qualifier) {
			class NotWrittenDetector extends ASTVisitor {
				boolean notWritten= true;

				public boolean visit(SimpleName node) {
					if (isParameter(pi, node, enclosingMethodParameters, qualifier) && ASTResolving.isWriteAccess(node))
						notWritten= false;
					return false;
				}

				public boolean visit(SuperFieldAccess node) {
					return false;
				}
			}
			NotWrittenDetector visitor= new NotWrittenDetector();
			block.accept(visitor);
			return visitor.notWritten;
		}

		public boolean needsParameterUsedCheck() {
			return false;
		}

	}

	private static final String PARAMETER_CLASS_APPENDIX= "Parameter"; //$NON-NLS-1$

	private static final String DEFAULT_PARAMETER_OBJECT_NAME= "parameterObject"; //$NON-NLS-1$

	private MethodDeclaration fMethodDeclaration;

	private ParameterObjectFactory fParameterObjectFactory;

	private boolean fCreateAsTopLevel= true;

	private ParameterInfo fParameterObjectReference;

	public IntroduceParameterObjectRefactoring(IMethod method) throws JavaModelException {
		super(method);
		Assert.isNotNull(method);
		initializeFields(method);
		setBodyUpdater(new RewriteParameterBody());
		setDefaultValueAdvisor(new ParameterObjectCreator());
	}

	private void initializeFields(IMethod method) throws JavaModelException {
		fParameterObjectFactory= new ParameterObjectFactory();
		String methodName= method.getElementName();
		String className= String.valueOf(Character.toUpperCase(methodName.charAt(0)));
		if (methodName.length() > 1)
			className+= methodName.substring(1);
		className+= PARAMETER_CLASS_APPENDIX;

		fParameterObjectReference= ParameterInfo.createInfoForAddedParameter(className, DEFAULT_PARAMETER_OBJECT_NAME);
		fParameterObjectFactory.setClassName(className);

		List parameterInfos= super.getParameterInfos();
		for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (!pi.isAdded()) {
				pi.setCreateField(true);
			}
		}

		IType declaringType= method.getDeclaringType();
		Assert.isNotNull(declaringType);
		fParameterObjectFactory.setPackage(declaringType.getPackageFragment().getElementName());

		updateReferenceType();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		IMethod context= getMethod();
		// TODO: Check for availability
		status.merge(Checks.checkTypeName(fParameterObjectFactory.getClassName(), context));
		status.merge(Checks.checkIdentifier(getParameterName(), context));
		if (status.hasFatalError())
			return status;
		status.merge(super.checkFinalConditions(pm));
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		status.merge(super.checkInitialConditions(pm));
		if (status.hasFatalError())
			return status;
		CompilationUnit astRoot= getBaseCuRewrite().getRoot();
		ISourceRange nameRange= getMethod().getNameRange();
		ASTNode selectedNode= NodeFinder.perform(astRoot, nameRange.getOffset(), nameRange.getLength());
		if (selectedNode == null) {
			return mappingErrorFound(status, selectedNode);
		}
		fMethodDeclaration= (MethodDeclaration) ASTNodes.getParent(selectedNode, MethodDeclaration.class);
		if (fMethodDeclaration == null) {
			return mappingErrorFound(status, selectedNode);
		}
		IMethodBinding resolveBinding= fMethodDeclaration.resolveBinding();
		if (resolveBinding == null) {
			if (!processCompilerError(status, selectedNode))
				status.addFatalError(RefactoringCoreMessages.SelfEncapsulateField_type_not_resolveable);
			return status;
		}

		ITypeBinding declaringClass= resolveBinding.getDeclaringClass();
		if (fParameterObjectFactory.getPackage() == null)
			fParameterObjectFactory.setPackage(declaringClass.getPackage().getName());
		if (fParameterObjectFactory.getEnclosingType() == null)
			fParameterObjectFactory.setEnclosingType(declaringClass.getQualifiedName());

		List parameterInfos= super.getParameterInfos();
		for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (!pi.isAdded()) {
				if (pi.getOldName().equals(pi.getNewName())) // may have been
																// set to
																// something
																// else after
																// creation
					pi.setNewName(getFieldName(pi));
			}
		}
		if (!parameterInfos.contains(fParameterObjectReference)) {
			parameterInfos.add(0, fParameterObjectReference);
		}
		Map bindingMap= new HashMap();
		for (Iterator iter= fMethodDeclaration.parameters().iterator(); iter.hasNext();) {
			SingleVariableDeclaration sdv= (SingleVariableDeclaration) iter.next();
			bindingMap.put(sdv.getName().getIdentifier(), sdv.resolveBinding());
		}
		for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (pi != fParameterObjectReference)
				pi.setOldBinding((IVariableBinding) bindingMap.get(pi.getOldName()));
		}
		fParameterObjectFactory.setVariables(parameterInfos);
		return status;
	}

	protected boolean shouldReport(IProblem problem, CompilationUnit cu) {
		if (!super.shouldReport(problem,cu))
			return false;
		ASTNode node= ASTNodeSearchUtil.getAstNode(cu, problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart());
		if (node instanceof Type) {
			Type type= (Type) node;
			if (problem.getID() == IProblem.UndefinedType && getClassName().equals(ASTNodes.getTypeName(type))) {
				return false;
			}
		}
		if (node instanceof Name) {
			Name name= (Name) node;
			if (problem.getID() == IProblem.ImportNotFound && getPackage().indexOf(name.getFullyQualifiedName()) != -1)
				return false;
		}
		return true;
	}

	public String getClassName() {
		return fParameterObjectFactory.getClassName();
	}

	public ITypeBinding getContainingClass() {
		return fMethodDeclaration.resolveBinding().getDeclaringClass();
	}

	private String getMappingErrorMessage() {
		return Messages.format(RefactoringCoreMessages.IntroduceParameterObjectRefactoring_cannotalanyzemethod_mappingerror, new String[] {});
	}

	public String getFieldName(ParameterInfo element) {
		IJavaProject javaProject= getCompilationUnit().getJavaProject();
		String stripped= NamingConventions.removePrefixAndSuffixForArgumentName(javaProject, element.getOldName());
		int dim= element.getNewTypeBinding() != null ? element.getNewTypeBinding().getDimensions() : 0;
		return StubUtility.getVariableNameSuggestions(StubUtility.INSTANCE_FIELD, javaProject, stripped, dim, null, true)[0];
	}


	public String getName() {
		return RefactoringCoreMessages.IntroduceParameterObjectRefactoring_refactoring_name;
	}

	protected String doGetRefactoringChangeName() {
		return getName();
	}

	public String getParameterName() {
		return fParameterObjectReference.getNewName();
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		RefactoringStatus refactoringStatus= new RefactoringStatus();
		refactoringStatus.merge(super.initialize(arguments));
		return refactoringStatus;
	}

	public boolean isCreateGetter() {
		return fParameterObjectFactory.isCreateGetter();
	}

	public boolean isCreateSetter() {
		return fParameterObjectFactory.isCreateSetter();
	}

	public boolean isCreateAsTopLevel() {
		return fCreateAsTopLevel;
	}

	/**
	 * Checks if the given parameter info has been selected for field creation
	 * 
	 * @param pi parameter info
	 * @return true if the given parameter info has been selected for field
	 *         creation
	 */
	private boolean isValidField(ParameterInfo pi) {
		return pi.isCreateField() & !pi.isAdded();
	}

	private RefactoringStatus mappingErrorFound(RefactoringStatus result, ASTNode node) {
		if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0 && processCompilerError(result, node))
			return result;
		result.addFatalError(getMappingErrorMessage());
		return result;
	}

	public void moveFieldDown(ParameterInfo selected) {
		fParameterObjectFactory.moveDown(selected);
	}

	public void moveFieldUp(ParameterInfo selected) {
		fParameterObjectFactory.moveUp(selected);
	}

	private boolean processCompilerError(RefactoringStatus result, ASTNode node) {
		Message[] messages= ASTNodes.getMessages(node, ASTNodes.INCLUDE_ALL_PARENTS);
		if (messages.length == 0)
			return false;
		result.addFatalError(Messages.format(RefactoringCoreMessages.IntroduceParameterObjectRefactoring_cannotanalysemethod_compilererror,
				new String[] { messages[0].getMessage() }));
		return true;
	}

	public void setClassName(String className) {
		fParameterObjectFactory.setClassName(className);
		updateReferenceType();
	}

	private void updateReferenceType() {
		if (fCreateAsTopLevel)
			fParameterObjectReference.setNewTypeName(JavaModelUtil.concatenateName(fParameterObjectFactory.getPackage(), fParameterObjectFactory
					.getClassName()));
		else
			fParameterObjectReference.setNewTypeName(JavaModelUtil.concatenateName(fParameterObjectFactory.getEnclosingType(),
					fParameterObjectFactory.getClassName()));
	}

	public void setCreateGetter(boolean createGetter) {
		fParameterObjectFactory.setCreateGetter(createGetter);
	}

	public void setCreateSetter(boolean createSetter) {
		fParameterObjectFactory.setCreateSetter(createSetter);
	}

	public void setPackageName(String packageName) {
		fParameterObjectFactory.setPackage(packageName);
		updateReferenceType();
	}

	public void setParameterName(String paramName) {
		this.fParameterObjectReference.setNewName(paramName);
	}

	public void setCreateAsTopLevel(boolean topLevel) {
		this.fCreateAsTopLevel= topLevel;
		updateReferenceType();
	}

	public void updateParameterPosition() {
		fParameterObjectFactory.updateParameterPosition(fParameterObjectReference);
	}

	private void createParameterClass(MethodDeclaration methodDeclaration, CompilationUnitRewrite cuRewrite) throws CoreException {
		if (fCreateAsTopLevel) {
			IJavaProject javaProject= getCompilationUnit().getJavaProject();
			IPackageFragment packageFragment= getPackageFragmentRoot().getPackageFragment(fParameterObjectFactory.getPackage());
			if (!packageFragment.exists()) {
				fOtherChanges.add(new CreatePackageChange(packageFragment));
			}

			ICompilationUnit unit= packageFragment.getCompilationUnit(fParameterObjectFactory.getClassName() + ".java"); //$NON-NLS-1$
			Assert.isTrue(!unit.exists());
			createTopLevelParameterObject(javaProject, unit);
		} else {
			ASTRewrite rewriter= cuRewrite.getASTRewrite();
			TypeDeclaration enclosingType= (TypeDeclaration) methodDeclaration.getParent();
			ListRewrite bodyRewrite= rewriter.getListRewrite(enclosingType, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			TypeDeclaration classDeclaration= fParameterObjectFactory.createClassDeclaration(enclosingType.getName().getFullyQualifiedName(),
					cuRewrite);
			classDeclaration.modifiers().add(rewriter.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			classDeclaration.modifiers().add(rewriter.getAST().newModifier(ModifierKeyword.STATIC_KEYWORD));
			bodyRewrite.insertBefore(classDeclaration, methodDeclaration, null);
		}
	}

	private void createTopLevelParameterObject(IJavaProject javaProject, ICompilationUnit unit) throws JavaModelException, CoreException {
		ICompilationUnit workingCopy= unit.getWorkingCopy(null);

		try {
			// create stub with comments and dummy type
			String lineDelimiter= StubUtility.getLineDelimiterUsed(javaProject);
			String fileComment= getFileComment(workingCopy, lineDelimiter);
			String typeComment= getTypeComment(workingCopy, lineDelimiter);
			String content= CodeGeneration.getCompilationUnitContent(workingCopy, fileComment, typeComment,
					"class " + getClassName() + "{}", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
			workingCopy.getBuffer().setContents(content);

			CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(workingCopy);
			ASTRewrite rewriter= cuRewrite.getASTRewrite();
			CompilationUnit root= cuRewrite.getRoot();
			AST ast= cuRewrite.getAST();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();

			// retrieve&replace dummy type with real class
			ListRewrite types= rewriter.getListRewrite(root, CompilationUnit.TYPES_PROPERTY);
			ASTNode dummyType= (ASTNode) types.getOriginalList().get(0);
			String newTypeName= JavaModelUtil.concatenateName(fParameterObjectFactory.getPackage(), fParameterObjectFactory.getClassName());
			TypeDeclaration classDeclaration= fParameterObjectFactory.createClassDeclaration(newTypeName, cuRewrite);
			classDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			Javadoc javadoc= (Javadoc) dummyType.getStructuralProperty(TypeDeclaration.JAVADOC_PROPERTY);
			rewriter.set(classDeclaration, TypeDeclaration.JAVADOC_PROPERTY, javadoc, null);
			types.replace(dummyType, classDeclaration, null);

			// Apply rewrites and discard workingcopy
			// Using CompilationUnitRewrite.createChange() leads to strange
			// results
			String charset= ResourceUtil.getFile(getCompilationUnit()).getCharset(false);
			Document document= new Document(content);
			try {
				rewriter.rewriteAST().apply(document);
				TextEdit rewriteImports= importRewrite.rewriteImports(null);
				rewriteImports.apply(document);
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
						RefactoringCoreMessages.IntroduceParameterObjectRefactoring_parameter_object_creation_error, e));
			}
			String docContent= document.get();
			CreateCompilationUnitChange compilationUnitChange= new CreateCompilationUnitChange(unit, docContent, charset);
			fOtherChanges.add(compilationUnitChange);
		} finally {
			workingCopy.discardWorkingCopy();
		}
	}

	public IPackageFragmentRoot getPackageFragmentRoot() {
		return (IPackageFragmentRoot) getCompilationUnit().getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
	}

	protected String getFileComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		if (StubUtility.doAddComments(parentCU.getJavaProject())) {
			return CodeGeneration.getFileComment(parentCU, lineDelimiter);
		}
		return null;

	}

	protected String getTypeComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		if (StubUtility.doAddComments(parentCU.getJavaProject())) {
			StringBuffer typeName= new StringBuffer();
			typeName.append(fParameterObjectFactory.getClassName());
			String[] typeParamNames= new String[0];
			String comment= CodeGeneration.getTypeComment(parentCU, typeName.toString(), typeParamNames, lineDelimiter);
			if (comment != null && isValidComment(comment)) {
				return comment;
			}
		}
		return null;
	}

	private boolean isValidComment(String template) {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(template.toCharArray());
		try {
			int next= scanner.getNextToken();
			while (TokenScanner.isComment(next)) {
				next= scanner.getNextToken();
			}
			return next == ITerminalSymbols.TokenNameEOF;
		} catch (InvalidInputException e) {
		}
		return false;
	}

	public String getPackage() {
		return fParameterObjectFactory.getPackage();
	}

	public void setPackage(String typeQualifier) {
		fParameterObjectFactory.setPackage(typeQualifier);
	}

	private String getNameInScope(ParameterInfo pi, List enclosingMethodParameters) {
		Assert.isNotNull(enclosingMethodParameters);
		boolean emptyVararg= pi.getOldIndex() >= enclosingMethodParameters.size();
		if (!emptyVararg) {
			SingleVariableDeclaration svd= (SingleVariableDeclaration) enclosingMethodParameters.get(pi.getOldIndex());
			return svd.getName().getIdentifier();
		}
		return null;
	}

	public String getNewTypeName() {
		return fParameterObjectReference.getNewTypeName();
	}

	public ICompilationUnit getCompilationUnit() {
		return getBaseCuRewrite().getCu();
	}

}
