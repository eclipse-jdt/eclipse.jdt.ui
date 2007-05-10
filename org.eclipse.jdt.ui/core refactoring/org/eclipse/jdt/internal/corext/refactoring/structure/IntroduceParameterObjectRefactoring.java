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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
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

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
import org.eclipse.jdt.core.dom.MethodInvocation;
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
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker.IProblemVerifier;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class IntroduceParameterObjectRefactoring extends ChangeSignatureRefactoring {

	private final class RewriteArguments implements IDefaultValueAdvisor {
		public Expression createDefaultExpression(CompilationUnitRewrite cuRewrite, ParameterInfo info, List parameterInfos, List nodes, boolean isRecursive) {
			final AST ast= cuRewrite.getAST();
			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Map piMapping= new LinkedHashMap();
			boolean allInlined=isRecursive;
			for (ListIterator iter= parameterInfos.listIterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				if (isValidField(pi)) {
					piMapping.put(pi.getOldName(), pi);
					if (pi.isInlined() && allInlined) {
						ASTNode node= (ASTNode) nodes.get(pi.getOldIndex());
						if (!isParameter(pi, node)) {
							allInlined= false;
						}
					} else
						allInlined= false;
				}
			}
			if (allInlined) {
				return ast.newSimpleName(info.getNewName());
			}
			ClassInstanceCreation invocation= ast.newClassInstanceCreation();
			invocation.setType(fParameterObjectFactory.createType(fCreateAsTopLevel, cuRewrite));
			List arguments= invocation.arguments();
			for (Iterator iter= piMapping.values().iterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				if (pi.isOldVarargs()) {
					if (iter.hasNext()) { // not new vararg
						if (pi.getOldIndex() < nodes.size() && (nodes.get(pi.getOldIndex()) instanceof NullLiteral)) {
							NullLiteral nullLiteral= (NullLiteral) nodes.get(pi.getOldIndex());
							arguments.add(moveNode(nullLiteral, rewrite, piMapping));
						} else {
							ArrayCreation creation= ast.newArrayCreation();
							creation.setType((ArrayType) importBinding(pi.getNewTypeBinding(), cuRewrite));
							ArrayInitializer initializer= ast.newArrayInitializer();
							List expressions= initializer.expressions();
							for (int i= pi.getOldIndex(); i < nodes.size(); i++) {
								ASTNode node= (ASTNode) nodes.get(i);
								importNodeTypes(node, cuRewrite);
								expressions.add(moveNode(node, rewrite, piMapping));
							}
							if (expressions.size() != 0)
								creation.setInitializer(initializer);
							else
								creation.dimensions().add(ast.newNumberLiteral("0")); //$NON-NLS-1$
							arguments.add(creation);
						}
					} else {
						// An unpack of existing array creations would be
						// cool
						boolean needsCopy= true;
						if (pi.getOldIndex() < nodes.size() && nodes.get(pi.getOldIndex()) instanceof ArrayCreation) {
							ArrayCreation creation= (ArrayCreation) nodes.get(pi.getOldIndex());
							ITypeBinding binding= creation.resolveTypeBinding();
							if (binding != null && binding.isAssignmentCompatible(pi.getNewTypeBinding())) {
								arguments.add(moveNode(creation, rewrite, piMapping));
								needsCopy= false;
							}
						}
						if (needsCopy) {// last argument was either a number of
							// args (including 0) or an incompatible
							// array
							for (int i= pi.getOldIndex(); i < nodes.size(); i++) {
								ASTNode node= (ASTNode) nodes.get(i);
								importNodeTypes(node, cuRewrite);
								arguments.add(moveNode(node, rewrite, piMapping));
							}
						}
					}
				} else {
					Expression exp= (Expression) nodes.get(pi.getOldIndex());
					importNodeTypes(exp, cuRewrite);
					arguments.add(moveNode(exp, rewrite, piMapping));
				}
			}
			return invocation;
		}

		private void importNodeTypes(ASTNode node, final CompilationUnitRewrite cuRewrite) {

			ASTResolving.visitAllBindings(node, new TypeBindingVisitor() {

				public boolean visit(ITypeBinding nodeBinding) {
					Type type= cuRewrite.getImportRewrite().addImport(nodeBinding, cuRewrite.getAST());
					cuRewrite.getImportRemover().registerAddedImports(type);
					return false;
				}

			});
		}
	}

	private boolean isParameter(ParameterInfo pi, ASTNode node) {
		if (node instanceof Name) {
			Name name= (Name) node;
			IVariableBinding binding= ASTNodes.getVariableBinding(name);
			if (binding != null && binding.isParameter() && binding.getName().equals(pi.getOldName()))
				return true;
		}
		return false;
	}
	
	protected ASTNode moveNode(final ASTNode oldNode, final ASTRewrite rewrite, final Map piMapping) {
		class SimpleNameRewriter extends ASTVisitor {
			private Expression fNewNode;

			SimpleNameRewriter() {
				super(false);
			}

			public boolean visit(SimpleName node) {
				ParameterInfo pi= (ParameterInfo) piMapping.get(node.getIdentifier());
				if (pi != null && pi.isInlined()) {
					if (oldNode == node) {
						fNewNode= updateSimpleName(rewrite, pi, node, false);
					} else {
						updateSimpleName(rewrite, pi, node, true);
					}
				}
				return false;
			}
		}
		SimpleNameRewriter visitor= new SimpleNameRewriter();
		oldNode.accept(visitor);
		if (visitor.fNewNode != null)
			return moveNode(visitor.fNewNode, rewrite);
		return moveNode(oldNode, rewrite);
	}

	private final class RewriteParameterBody extends BodyUpdater {
		private boolean fParameterClassCreated= false;

		private boolean isReadOnly(final ParameterInfo pi, Block block) {
			class NotWrittenDetector extends ASTVisitor {
				boolean notWritten= true;

				public boolean visit(FieldAccess node) {
					return false;
				}

				public boolean visit(SimpleName node) {
					if (isParameter(pi, node) && ASTResolving.isWriteAccess(node))
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

		public void updateBody(final MethodDeclaration methodDeclaration, CompilationUnitRewrite cuRewrite, RefactoringStatus result)
				throws CoreException {
			final ASTRewrite rewriter= cuRewrite.getASTRewrite();
			fParameterObjectFactory.createType(fCreateAsTopLevel, cuRewrite);
			TypeDeclaration parent= (TypeDeclaration) methodDeclaration.getParent();
			if (cuRewrite.getCu().equals(fCompilationUnit) && !fParameterClassCreated) {
				createParameterClass(methodDeclaration, parent, cuRewrite);
				fParameterClassCreated= true;
			}
			Block body= methodDeclaration.getBody();
			if (body != null) {
				ListRewrite block= rewriter.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				final List managedParams= getParameterInfos();
				for (Iterator iter= managedParams.iterator(); iter.hasNext();) {
					final ParameterInfo pi= (ParameterInfo) iter.next();
					if (isValidField(pi)) {
						if (isReadOnly(pi, body)) {
							body.accept(new ASTVisitor(false) {

								public boolean visit(SimpleName node) {
									updateSimpleName(rewriter, pi, node, true);
									return false;
								}

								public boolean visit(MethodInvocation node) {
									IMethodBinding nodeBinding= node.resolveMethodBinding();
									if (nodeBinding != null && nodeBinding.getMethodDeclaration() == methodDeclaration.resolveBinding()) {
										return false; // do not update
										// methods that will
										// be updated by CMS
									}
									return true;
								}
							});
							pi.setInlined(true);
						} else {
							ExpressionStatement initializer= fParameterObjectFactory.createInitializer(pi, getParameterName(), cuRewrite);
							block.insertFirst(initializer, null);
						}
					}
				}
			}
		}
	}

	private static final String PARAMETER_CLASS_APPENDIX= "Parameter"; //$NON-NLS-1$

	private static final String DEFAULT_PARAMETER_OBJECT_NAME= "parameterObject"; //$NON-NLS-1$

	private MethodDeclaration fMethodDeclaration;

	private ICompilationUnit fCompilationUnit;

	private int fOffset;

	private int fLength;

	private ParameterObjectFactory fParameterObjectFactory;

	private boolean fCreateAsTopLevel= true;

	private ParameterInfo fParameterObjectReference;

	public IntroduceParameterObjectRefactoring(IMethod method) throws JavaModelException {
		super(method);
		if (method != null)
			initializeFields(method);
		setBodyUpdater(new RewriteParameterBody());
		setDefaultValueAdvisor(new RewriteArguments());
		List parameterInfos= getParameterInfos();
		for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (!pi.isAdded()) {
				pi.setCreateField(true);
				pi.setNewName(getFieldName(pi));
			}
		}
	}

	public Type importBinding(ITypeBinding newTypeBinding, CompilationUnitRewrite cuRewrite) {
		Type type= cuRewrite.getImportRewrite().addImport(newTypeBinding, cuRewrite.getAST());
		cuRewrite.getImportRemover().registerAddedImports(type);
		return type;
	}

	private void initializeFields(IMethod method) throws JavaModelException {
		fCompilationUnit= method.getCompilationUnit();
		fOffset= method.getNameRange().getOffset();
		fLength= method.getNameRange().getLength();
		fParameterObjectFactory= new ParameterObjectFactory(fCompilationUnit);
		String methodName= method.getElementName();
		String className= String.valueOf(Character.toUpperCase(methodName.charAt(0)));
		if (methodName.length() > 1)
			className+= methodName.substring(1);
		className+= PARAMETER_CLASS_APPENDIX;
		fParameterObjectReference= ParameterInfo.createInfoForAddedParameter(className, getParameterName());
		fParameterObjectFactory.setClassName(className);
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		status.merge(fParameterObjectFactory.checkConditions());
		status.merge(Checks.checkIdentifier(getParameterName()));
		if (status.hasFatalError())
			return status;
		status.merge(super.checkFinalConditions(pm));
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		status.merge(super.checkInitialConditions(pm));
		status.merge(Checks.checkAvailability(fCompilationUnit));
		if (status.hasFatalError())
			return status;
		CompilationUnit astRoot= getBaseCuRewrite().getRoot();
		ASTNode node= NodeFinder.perform(astRoot, fOffset, fLength);
		if (node == null) {
			return mappingErrorFound(status, node);
		}
		fMethodDeclaration= (MethodDeclaration) ASTNodes.getParent(node, MethodDeclaration.class);
		if (fMethodDeclaration == null) {
			return mappingErrorFound(status, node);
		}
		IMethodBinding resolveBinding= fMethodDeclaration.resolveBinding();
		if (resolveBinding == null) {
			if (!processCompilerError(status, node))
				status.addFatalError(RefactoringCoreMessages.SelfEncapsulateField_type_not_resolveable);
			return status;
		}

		ITypeBinding declaringClass= resolveBinding.getDeclaringClass();
		String qualifiedName= declaringClass.getPackage().getName();
		if (fParameterObjectFactory.getPackage() == null)
			fParameterObjectFactory.setPackage(qualifiedName);
		if (fParameterObjectFactory.getEnclosingType() == null)
			fParameterObjectFactory.setEnclosingType(declaringClass.getQualifiedName());
		List parameterInfos= super.getParameterInfos();
		if (!parameterInfos.contains(fParameterObjectReference)) {
			parameterInfos.add(0, fParameterObjectReference);
		}
		List parameters= fMethodDeclaration.parameters();
		Map bindingMap= new HashMap();
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
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

	protected IProblemVerifier doGetProblemVerifier() {
		return new IProblemVerifier() {

			public boolean isError(IProblem problem, ASTNode node) {
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

		};
	}

	public Change createChange(IProgressMonitor pm) {
		return super.createChange(pm);
	}

	public String getClassName() {
		String className= fParameterObjectFactory.getClassName();
		return className;
	}

	public ITypeBinding getContainingClass() {
		return fMethodDeclaration.resolveBinding().getDeclaringClass();
	}

	private String getMappingErrorMessage() {
		return Messages.format(RefactoringCoreMessages.IntroduceParameterObjectRefactoring_cannotalanyzemethod_mappingerror, new String[] {});
	}

	public String getFieldName(ParameterInfo element) {
		String paramName= element.getOldName();
		IJavaProject javaProject= fCompilationUnit.getJavaProject();
		String stripped= NamingConventions.removePrefixAndSuffixForArgumentName(javaProject, paramName);
		int dim= element.getNewTypeBinding() != null ? element.getNewTypeBinding().getDimensions() : 0;
		return StubUtility.getVariableNameSuggestions(StubUtility.INSTANCE_FIELD, javaProject, stripped, dim, null, true)[0];
	}


	public IMethodBinding getMethodBinding() {
		return fMethodDeclaration.resolveBinding();
	}

	public String getName() {
		return RefactoringCoreMessages.IntroduceParameterObjectRefactoring_refactoring_name;
	}

	protected String doGetRefactoringChangeName() {
		return getName();
	}

	public String getParameterName() {
		if (fParameterObjectReference == null)
			return DEFAULT_PARAMETER_OBJECT_NAME;
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
		Assert.isTrue(fParameterObjectReference != null);
		fParameterObjectReference.setNewTypeName(className);
	}

	public void setCreateComments(boolean selection) {
		fParameterObjectFactory.setCreateComments(selection);
	}

	public void setCreateGetter(boolean createGetter) {
		fParameterObjectFactory.setCreateGetter(createGetter);
	}

	public void setCreateSetter(boolean createSetter) {
		fParameterObjectFactory.setCreateSetter(createSetter);
	}

	public void setPackageName(String packageName) {
		fParameterObjectFactory.setPackage(packageName);
	}

	public void setParameterName(String paramName) {
		Assert.isTrue(fParameterObjectReference != null);
		this.fParameterObjectReference.setNewName(paramName);
	}

	public void setCreateAsTopLevel(boolean topLevel) {
		this.fCreateAsTopLevel= topLevel;
	}

	public void updateParameterPosition() {
		fParameterObjectFactory.updateParameterPosition(fParameterObjectReference);
	}

	private void createParameterClass(MethodDeclaration methodDeclaration, TypeDeclaration parent, CompilationUnitRewrite cuRewrite)
			throws CoreException {
		if (fCreateAsTopLevel) {
			IJavaProject javaProject= fCompilationUnit.getJavaProject();
			IPackageFragmentRoot fragment= getPackageFragmentRoot();
			IPackageFragment packageFragment= fragment.getPackageFragment(fParameterObjectFactory.getPackage());
			if (!packageFragment.exists()) {
				fOtherChanges.add(new CreatePackageChange(packageFragment));
			}
			ICompilationUnit unit= packageFragment.getCompilationUnit(fParameterObjectFactory.getClassName() + ".java"); //$NON-NLS-1$
			ICompilationUnit workingCopy= null;
			workingCopy= unit.getWorkingCopy(null);
			try {
				String lineDelimiter= StubUtility.getLineDelimiterUsed(javaProject);
				String fileComment= getFileComment(workingCopy, lineDelimiter);
				String typeComment= getTypeComment(workingCopy, lineDelimiter);
				String content= CodeGeneration.getCompilationUnitContent(workingCopy, fileComment, typeComment,
						"class " + getClassName() + "{}", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
				workingCopy.getBuffer().setContents(content);

				cuRewrite= new CompilationUnitRewrite(workingCopy);
				ASTRewrite rewriter= cuRewrite.getASTRewrite();
				CompilationUnit root= cuRewrite.getRoot();
				AST ast= cuRewrite.getAST();
				ImportRewrite importRewrite= cuRewrite.getImportRewrite();
				ListRewrite types= rewriter.getListRewrite(root, CompilationUnit.TYPES_PROPERTY);
				ASTNode oldType= (ASTNode) types.getOriginalList().get(0);
				TypeDeclaration classDeclaration= fParameterObjectFactory.createClassDeclaration(workingCopy, JavaModelUtil.concatenateName(
						fParameterObjectFactory.getPackage(), fParameterObjectFactory.getClassName()), cuRewrite);
				classDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
				Javadoc javadoc= (Javadoc) oldType.getStructuralProperty(TypeDeclaration.JAVADOC_PROPERTY);
				rewriter.set(classDeclaration, TypeDeclaration.JAVADOC_PROPERTY, javadoc, null);
				types.replace(oldType, classDeclaration, null);

				String charset= ResourceUtil.getFile(fCompilationUnit).getCharset(false);
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
		} else {
			ASTRewrite rewriter= cuRewrite.getASTRewrite();
			ListRewrite bodyRewrite= rewriter.getListRewrite(parent, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			TypeDeclaration classDeclaration= fParameterObjectFactory.createClassDeclaration(fCompilationUnit, parent.getName()
					.getFullyQualifiedName(), cuRewrite);
			classDeclaration.modifiers().add(rewriter.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			classDeclaration.modifiers().add(rewriter.getAST().newModifier(ModifierKeyword.STATIC_KEYWORD));
			bodyRewrite.insertBefore(classDeclaration, methodDeclaration, null);
		}
	}

	public IPackageFragmentRoot getPackageFragmentRoot() {
		return (IPackageFragmentRoot) fCompilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
	}

	protected String getFileComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		if (fParameterObjectFactory.isCreateComments()) {
			return CodeGeneration.getFileComment(parentCU, lineDelimiter);
		}
		return null;

	}

	protected String getTypeComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		if (fParameterObjectFactory.isCreateComments()) {
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

	public String getEnclosingType() {
		return fParameterObjectFactory.getEnclosingType();
	}

	public String getPackage() {
		return fParameterObjectFactory.getPackage();
	}

	public void setEnclosingType(String enclosingType) {
		fParameterObjectFactory.setEnclosingType(enclosingType);
	}

	public void setPackage(String typeQualifier) {
		fParameterObjectFactory.setPackage(typeQualifier);
	}

	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	public IPackageFragment getPackageFragment() throws JavaModelException {
		return (IPackageFragment) fCompilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
	}

	public String getEnclosingPackage() {
		return fMethodDeclaration.resolveBinding().getDeclaringClass().getPackage().getName();
	}

	private Expression updateSimpleName(ASTRewrite rewriter, ParameterInfo pi, SimpleName node, boolean replaceNode) {
		AST ast= rewriter.getAST();
		IBinding binding= node.resolveBinding();
		Expression replacementNode= fParameterObjectFactory.createFieldReadAccess(pi, getParameterName(), ast);
		if (binding instanceof IVariableBinding) {
			IVariableBinding variable= (IVariableBinding) binding;
			if (variable.isParameter() && variable.getName().equals(pi.getOldName())) {
				if (replaceNode)
					rewriter.replace(node, replacementNode, null);
				return replacementNode;
			}
		} else {
			ASTNode parent= node.getParent();
			if (! (parent instanceof QualifiedName || parent instanceof FieldAccess || parent instanceof SuperFieldAccess)) {
				if (node.getIdentifier().equals(pi.getOldName())) {
					if (replaceNode)
						rewriter.replace(node, replacementNode, null);
					return replacementNode;
				}
			}
		}
		return null;
	}

}
