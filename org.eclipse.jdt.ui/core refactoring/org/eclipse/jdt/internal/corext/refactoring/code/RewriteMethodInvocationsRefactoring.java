/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.IInitializableRefactoringComponent;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.binary.StubCreator;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class RewriteMethodInvocationsRefactoring extends CommentRefactoring implements IInitializableRefactoringComponent {

	public static final String ID_REWRITE_METHOD_INVOCATIONS= "org.eclipse.jdt.ui.rewrite.method.invocations"; //$NON-NLS-1$
	private static final String ATTRIBUTE_MODE= "mode"; //$NON-NLS-1$

	public static class Mode {
		private Mode() {
		}
		public static final Mode REWRITE_ALL= new Mode();
		public static final Mode REWRITE_SINGLE= new Mode();
	}

	/**
	 * ICompilationUnit or IClassFile
	 */
	private IJavaElement fInitialUnit;
	private ASTNode fInitialNode;
	private TextChangeManager fChangeManager;
	private SourceProvider fSourceProvider;
	private TargetProvider fTargetProvider;
	
	private Mode fCurrentMode;
	private Mode fInitialMode;
	private int fSelectionStart;
	private int fSelectionLength;
	private String fBody;
	private String[] fParameterNames;

	private RewriteMethodInvocationsRefactoring(IJavaElement unit, ASTNode node, int offset, int length) {
		Assert.isNotNull(unit);
		Assert.isTrue(JavaModelUtil.isTypeContainerUnit(unit));
//		Assert.isTrue(JavaElementUtil.isSourceAvailable((ISourceReference) unit));
		Assert.isNotNull(node);
		fInitialUnit= unit;
		fInitialNode= node;
		fSelectionStart= offset;
		fSelectionLength= length;
	}

	private RewriteMethodInvocationsRefactoring(ICompilationUnit unit, MethodInvocation node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.REWRITE_SINGLE;
	}

	private RewriteMethodInvocationsRefactoring(ICompilationUnit unit, SuperMethodInvocation node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.REWRITE_SINGLE;
	}

	private RewriteMethodInvocationsRefactoring(ICompilationUnit unit, ConstructorInvocation node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.REWRITE_SINGLE;
	}

	private RewriteMethodInvocationsRefactoring(IJavaElement unit, MethodDeclaration node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(node);
		fInitialMode= fCurrentMode= Mode.REWRITE_ALL;
	}
	
	/**
	 * Creates a new inline method refactoring
	 * @param unit the compilation unit, class file, or <code>null</code> if invoked by scripting
	 * @param node the compilation unit node, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	//TODO: scripting will throw NPEs!
	public static RewriteMethodInvocationsRefactoring create(IJavaElement unit, CompilationUnit node, int selectionStart, int selectionLength) {
		ASTNode target= getTargetNode(unit, node, selectionStart, selectionLength);
		if (target == null)
			return null;
		if (target.getNodeType() == ASTNode.METHOD_DECLARATION) {
			
			return new RewriteMethodInvocationsRefactoring(unit, (MethodDeclaration)target, selectionStart, selectionLength);
		} else {
			ICompilationUnit cu= (ICompilationUnit) unit;
			if (target.getNodeType() == ASTNode.METHOD_INVOCATION) {
				return new RewriteMethodInvocationsRefactoring(cu, (MethodInvocation)target, selectionStart, selectionLength);
			} else if (target.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
				return new RewriteMethodInvocationsRefactoring(cu, (SuperMethodInvocation)target, selectionStart, selectionLength);
			} else if (target.getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
				return new RewriteMethodInvocationsRefactoring(cu, (ConstructorInvocation)target, selectionStart, selectionLength);
			}
		}
		return null;
	}
	
	public String getName() {
		return RefactoringCoreMessages.InlineMethodRefactoring_name; 
	}
	
	public boolean canEnableDeleteSource() {
		return ! (fSourceProvider.getTypeContainerUnit() instanceof IClassFile);
	}
	
	public Mode getInitialMode() {
		return fInitialMode;
	}
	
	public RefactoringStatus setCurrentMode(Mode mode) throws JavaModelException {
		if (fCurrentMode == mode)
			return new RefactoringStatus();
		Assert.isTrue(getInitialMode() == Mode.REWRITE_SINGLE);
		fCurrentMode= mode;
		if (mode == Mode.REWRITE_SINGLE) {
			fTargetProvider= TargetProvider.create((ICompilationUnit) fInitialUnit, (MethodInvocation)fInitialNode);
		} else {
			fTargetProvider= TargetProvider.create(fSourceProvider.getDeclaration());
		}
		return fTargetProvider.checkActivation();
	}
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		if (fSourceProvider == null) {
			fSourceProvider= resolveSourceProvider(result);
			if (result.hasFatalError())
				return result;
		}
		fTargetProvider.setSourceProvider(fSourceProvider);
		result.merge(fSourceProvider.checkActivation());
		result.merge(fTargetProvider.checkActivation());
		return result;
	}
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 20); //$NON-NLS-1$
		fChangeManager= new TextChangeManager();
		RefactoringStatus result= new RefactoringStatus();
		fSourceProvider.initialize();
		fTargetProvider.initialize();
		pm.setTaskName(RefactoringCoreMessages.InlineMethodRefactoring_searching);
		RefactoringStatus searchStatus= new RefactoringStatus();
		ICompilationUnit[] units= fTargetProvider.getAffectedCompilationUnits(searchStatus, new SubProgressMonitor(pm, 1));
		if (searchStatus.hasFatalError()) {
			result.merge(searchStatus);
			return result;
		}
		IFile[] filesToBeModified= getFilesToBeModified(units);
		result.merge(Checks.validateModifiesFiles(filesToBeModified, getValidationContext()));
		if (result.hasFatalError())
			return result;
		result.merge(ResourceChangeChecker.checkFilesToBeChanged(filesToBeModified, new SubProgressMonitor(pm, 1)));
		checkOverridden(result, new SubProgressMonitor(pm, 4));
		IProgressMonitor sub= new SubProgressMonitor(pm, 15);
		sub.beginTask("", units.length * 3); //$NON-NLS-1$
		for (int c= 0; c < units.length; c++) {
			ICompilationUnit unit= units[c];
			sub.subTask(Messages.format(RefactoringCoreMessages.InlineMethodRefactoring_processing,  unit.getElementName())); 
			CallInliner inliner= null;
			try {
				boolean added= false;
				MultiTextEdit root= new MultiTextEdit();
				CompilationUnitChange change= (CompilationUnitChange)fChangeManager.get(unit);
				change.setEdit(root);
				BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(unit, new SubProgressMonitor(pm, 1));
				if (bodies.length == 0)
					continue;
				inliner= new CallInliner(unit, (CompilationUnit) bodies[0].getRoot(), fSourceProvider);
				for (int b= 0; b < bodies.length; b++) {
					BodyDeclaration body= bodies[b];
					inliner.initialize(body);
					RefactoringStatus nestedInvocations= new RefactoringStatus();
					ASTNode[] invocations= removeNestedCalls(nestedInvocations, unit, 
						fTargetProvider.getInvocations(body, new SubProgressMonitor(sub, 2)));
					for (int i= 0; i < invocations.length; i++) {
						ASTNode invocation= invocations[i];
						result.merge(inliner.initialize(invocation, fTargetProvider.getStatusSeverity()));
						if (result.hasFatalError())
							break;
						if (result.getSeverity() < fTargetProvider.getStatusSeverity()) {
							added= true;
							TextEditGroup group= new TextEditGroup(RefactoringCoreMessages.InlineMethodRefactoring_edit_inline); 
							change.addTextEditGroup(group);
							result.merge(inliner.perform(group)); 
						}
					}
					// do this after we have inlined the method calls. We still want
					// to generate the modifications.
					result.merge(nestedInvocations);
				}
				if (!added) {
					fChangeManager.remove(unit);
				} else {
					root.addChild(inliner.getModifications());
					ImportRewrite rewrite= inliner.getImportEdit();
					if (rewrite.hasRecordedChanges()) {
						TextEdit edit= rewrite.rewriteImports(null);
						if (edit instanceof MultiTextEdit ? ((MultiTextEdit)edit).getChildrenSize() > 0 : true) {
							root.addChild(edit);
							change.addTextEditGroup(
								new TextEditGroup(RefactoringCoreMessages.InlineMethodRefactoring_edit_import, new TextEdit[] {edit})); 
						}
					}
				}
			} finally {
				if (inliner != null)
					inliner.dispose();
			}
			sub.worked(1);
			if (sub.isCanceled())
				throw new OperationCanceledException();
		}
		result.merge(searchStatus);
		sub.done();
		pm.done();
		return result;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		return new DynamicValidationStateChange(RefactoringCoreMessages.InlineMethodRefactoring_edit_inlineCall, fChangeManager.getAllChanges()) {
		
			public final ChangeDescriptor getDescriptor() {
				final Map arguments= new HashMap();
				String project= null;
				IJavaProject javaProject= fInitialUnit.getJavaProject();
				if (javaProject != null)
					project= javaProject.getElementName();
				final IMethodBinding binding= fSourceProvider.getDeclaration().resolveBinding();
				int flags= RefactoringDescriptor.STRUCTURAL_CHANGE;
				if (!Modifier.isPrivate(binding.getModifiers()))
					flags|= RefactoringDescriptor.MULTI_CHANGE;
				final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_REWRITE_METHOD_INVOCATIONS, project, Messages.format(RefactoringCoreMessages.InlineMethodRefactoring_descriptor_description, new String[] {BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED), BindingLabelProvider.getBindingLabel(binding.getDeclaringClass(), JavaElementLabels.ALL_FULLY_QUALIFIED)}), getComment(), arguments, flags);
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fInitialUnit));
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_SELECTION, new Integer(fSelectionStart).toString() + " " + new Integer(fSelectionLength).toString()); //$NON-NLS-1$
				arguments.put(ATTRIBUTE_MODE, new Integer(fCurrentMode == Mode.REWRITE_ALL ? 1 : 0).toString());
				return new RefactoringChangeDescriptor(descriptor);
			}
		}; 
	}
	
	private SourceProvider resolveSourceProvider(RefactoringStatus status) throws JavaModelException {
		IMethodBinding methodBinding;
		switch(fInitialNode.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				methodBinding= (IMethodBinding)((MethodInvocation)fInitialNode).getName().resolveBinding();
				break;
			case ASTNode.SUPER_METHOD_INVOCATION:
				methodBinding= (IMethodBinding)((SuperMethodInvocation)fInitialNode).getName().resolveBinding();
				break;
			case ASTNode.CONSTRUCTOR_INVOCATION:
				methodBinding= ((ConstructorInvocation)fInitialNode).resolveConstructorBinding();
				break;
			case ASTNode.METHOD_DECLARATION:
				methodBinding= ((MethodDeclaration)fInitialNode).resolveBinding();
				break;
			default:
				status.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_error_noMethodDeclaration); 
				return null;
		}
		
		final IMethod method= (IMethod)methodBinding.getJavaElement();
		if (method != null) {
			IJavaElement unit;
			IDocument source;
			CompilationUnit methodDeclarationAstRoot;
			ICompilationUnit methodCu= method.getCompilationUnit();
			if (methodCu != null) {
				unit= methodCu;
//				source= methodCu.getBuffer().getContents();
				
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setSource(methodCu);
				parser.setFocalPosition(method.getNameRange().getOffset());
				CompilationUnit compilationUnit= (CompilationUnit) parser.createAST(null);
				MethodDeclaration methodDecl= (MethodDeclaration) NodeFinder.perform(compilationUnit, method.getNameRange()).getParent();
				AST ast= compilationUnit.getAST();
				ASTRewrite rewrite= ASTRewrite.create(ast);
				Block newBody= ast.newBlock();
				newBody.statements().add(rewrite.createStringPlaceholder(fBody, ASTNode.EMPTY_STATEMENT));
				rewrite.replace(methodDecl.getBody(), newBody, null);
				List parameters= methodDecl.parameters();
				for (int i= 0; i < parameters.size(); i++) {
					SingleVariableDeclaration parameter= (SingleVariableDeclaration) parameters.get(i);
					rewrite.set(parameter.getName(), SimpleName.IDENTIFIER_PROPERTY, fParameterNames[i], null);
				}
				TextEdit textEdit= rewrite.rewriteAST();
				Document document= new Document(methodCu.getBuffer().getContents());
				try {
					textEdit.apply(document);
				} catch (MalformedTreeException e) {
					JavaPlugin.log(e);
				} catch (BadLocationException e) {
					JavaPlugin.log(e);
				}
				source= document;
				
				methodDeclarationAstRoot= new RefactoringASTParser(AST.JLS3).parse(source.get(), methodCu, true, true, null);
				
			} else {
				IClassFile classFile= method.getClassFile();
				//TODO: use source if available?
				StubCreator stubCreator= new StubCreator(true) {
					protected void appendMethodBody(IMethod currentMethod) throws JavaModelException {
						if (currentMethod.equals(method)) {
							fBuffer.append(fBody);
						} else {
							super.appendMethodBody(currentMethod);
						}
					}
					/*
					 * @see org.eclipse.jdt.internal.corext.refactoring.binary.StubCreator#appendMethodParameterName(org.eclipse.jdt.core.IMethod, int)
					 */
					protected void appendMethodParameterName(IMethod currentMethod, int index) {
						if (currentMethod.equals(method)) {
							fBuffer.append(fParameterNames[index]);
						} else {
							super.appendMethodParameterName(currentMethod, index);
						}
					}
				};
				
				String stub= stubCreator.createStub(classFile.getType(), null);
				source= new Document(stub);
				methodDeclarationAstRoot= new RefactoringASTParser(AST.JLS3).parse(stub, classFile, true, true, null);
				unit= classFile;
			}
			ASTNode node= methodDeclarationAstRoot.findDeclaringNode(methodBinding.getKey());
			if (node instanceof MethodDeclaration) {
				return new SourceProvider(unit, source, (MethodDeclaration) node);
			}
		}
		status.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_error_noMethodDeclaration); 
		return null;
	}
	
	private static ASTNode getTargetNode(IJavaElement unit, CompilationUnit root, int offset, int length) {
		ASTNode node= null;
		try {
			node= checkNode(findNode(unit, root, offset, length), unit);
		} catch(JavaModelException e) {
			// Do nothing
		}
		if (node != null)
			return node;
		return checkNode(NodeFinder.perform(root, offset, length), unit);
	}

	private static ASTNode findNode(IJavaElement unit, CompilationUnit root, int offset, int length) throws JavaModelException {
		if (unit instanceof ICompilationUnit)
			return NodeFinder.perform(root, offset, length, (ICompilationUnit) unit);
		else
			return NodeFinder.perform(root, offset, length, (IClassFile) unit);
	}
	
	private static ASTNode checkNode(ASTNode node, IJavaElement unit) {
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement)node).getExpression();
		}
		switch(node.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
					return node;
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return unit instanceof ICompilationUnit ? node : null; // don't start on invocations in binary
		}
		return null;
	}
	
	private IFile[] getFilesToBeModified(ICompilationUnit[] units) {
		List result= new ArrayList(units.length + 1);
		IFile file;
		for (int i= 0; i < units.length; i++) {
			file= getFile(units[i]);
			if (file != null)
				result.add(file);
		}
		return (IFile[])result.toArray(new IFile[result.size()]);
	}
	
	private IFile getFile(ICompilationUnit unit) {
		unit= unit.getPrimary();
		IResource resource= unit.getResource();
		if (resource != null && resource.getType() == IResource.FILE)
			return (IFile)resource;
		return null;
	}
	
	private void checkOverridden(RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 9); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.InlineMethodRefactoring_checking_overridden); 
		MethodDeclaration decl= fSourceProvider.getDeclaration();
		IMethod method= (IMethod) decl.resolveBinding().getJavaElement();
		if (method == null || Flags.isPrivate(method.getFlags())) {
			pm.worked(8);
			return;
		}
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= type.newTypeHierarchy(new SubProgressMonitor(pm, 6));
		checkSubTypes(status, method, hierarchy.getAllSubtypes(type), new SubProgressMonitor(pm, 1));
		checkSuperClasses(status, method, hierarchy.getAllSuperclasses(type), new SubProgressMonitor(pm, 1));
		checkSuperInterfaces(status, method, hierarchy.getAllSuperInterfaces(type), new SubProgressMonitor(pm, 1));
		pm.setTaskName(""); //$NON-NLS-1$
	}

	private void checkSubTypes(RefactoringStatus result, IMethod method, IType[] types, IProgressMonitor pm) {
		checkTypes(
			result, method, types, 
			RefactoringCoreMessages.InlineMethodRefactoring_checking_overridden_error,
			pm);
	}
	
	private void checkSuperClasses(RefactoringStatus result, IMethod method, IType[] types, IProgressMonitor pm) {
		checkTypes(
			result, method, types, 
			RefactoringCoreMessages.InlineMethodRefactoring_checking_overrides_error,
			pm);
	}

	private void checkSuperInterfaces(RefactoringStatus result, IMethod method, IType[] types, IProgressMonitor pm) {
		checkTypes(
			result, method, types, 
			RefactoringCoreMessages.InlineMethodRefactoring_checking_implements_error,
			pm);
	}
	private void checkTypes(RefactoringStatus result, IMethod method, IType[] types, String key, IProgressMonitor pm) {
		pm.beginTask("", types.length); //$NON-NLS-1$
		for (int i= 0; i < types.length; i++) {
			pm.worked(1);
			IMethod[] overridden= types[i].findMethods(method);
			if (overridden != null && overridden.length > 0) {
				result.addError(
					Messages.format(key, types[i].getElementName()), 
					JavaStatusContext.create(overridden[0]));
			}
		}
	}
	
	private ASTNode[] removeNestedCalls(RefactoringStatus status, ICompilationUnit unit, ASTNode[] invocations) {
		if (invocations.length <= 1)
			return invocations;
		ASTNode[] parents= new ASTNode[invocations.length];
		for (int i= 0; i < invocations.length; i++) {
			parents[i]= invocations[i].getParent();
		}
		for (int i= 0; i < invocations.length; i++) {
			removeNestedCalls(status, unit, parents, invocations, i);
		}
		List result= new ArrayList();
		for (int i= 0; i < invocations.length; i++) {
			if (invocations[i] != null)
				result.add(invocations[i]);
		}
		return (ASTNode[])result.toArray(new ASTNode[result.size()]);
	}
	
	private void removeNestedCalls(RefactoringStatus status, ICompilationUnit unit, ASTNode[] parents, ASTNode[] invocations, int index) {
		ASTNode invocation= invocations[index];
		for (int i= 0; i < parents.length; i++) {
			ASTNode parent= parents[i];
			while (parent != null) {
				if (parent == invocation) {
					status.addError(RefactoringCoreMessages.InlineMethodRefactoring_nestedInvocation,  
						JavaStatusContext.create(unit, parent));
					invocations[index]= null;
				}
				parent= parent.getParent();
			}
		}
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments generic= (JavaRefactoringArguments) arguments;
			final String value= generic.getAttribute(ATTRIBUTE_MODE);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				int mode= 0;
				try {
					mode= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_MODE));
				}
				try {
					setCurrentMode(mode == 1 ? Mode.REWRITE_ALL : Mode.REWRITE_SINGLE);
				} catch (JavaModelException exception) {
					return RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage());
				}
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	public void setBody(String body, String[] parameterNames) {
		//TODO: validate parameter name count and body
		fBody= body;
		fParameterNames= parameterNames;
	}
}
