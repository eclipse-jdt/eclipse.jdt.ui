/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o inline call that is used in a field initializer 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringSessionDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.IInitializableRefactoringComponent;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDeprecationResolving;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

/*
 * Open items:
 *  - generate import statements for newly generated local variable declarations.
 *  - forbid cases like foo(foo(10)) when inlining foo().
 *  - case ref.foo(); and we want to inline foo. Inline a method in a different context;
 *  - optimize code when the method to be inlined returns an argument and that one is
 *    assigned to a paramter again. No need for a separate local (important to be able
 *    to revers extract method correctly).
 */
public class InlineMethodRefactoring extends CommentRefactoring implements IInitializableRefactoringComponent, IDeprecationResolving {

	public static final String ID_INLINE_METHOD= "org.eclipse.jdt.ui.inline.method"; //$NON-NLS-1$
	public static final String ATTRIBUTE_SELECTION= "selection"; //$NON-NLS-1$
	private static final String ATTRIBUTE_MODE= "mode"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELETE= "delete";	 //$NON-NLS-1$

	public static class Mode {
		private Mode() {
		}
		public static final Mode INLINE_ALL= new Mode();
		public static final Mode INLINE_SINGLE= new Mode();
	}

	private ICompilationUnit fInitialCUnit;
	private ASTNode fInitialNode;
	private TextChangeManager fChangeManager;
	private SourceProvider fSourceProvider;
	private TargetProvider fTargetProvider;
	private boolean fDeleteSource;
	private Mode fCurrentMode;
	private Mode fInitialMode;
	private int fSelectionStart;
	private int fSelectionLength;
	private final IMethod fMethod;

	/**
	 * Creates a new inline method refactoring.
	 * <p>
	 * This constructor is only used by <code>DelegateCreator</code>.
	 * </p>
	 * 
	 * @param method the method to inline
	 */
	public InlineMethodRefactoring(IMethod method) {
		Assert.isNotNull(method);
		Assert.isTrue(!method.isBinary());
		fMethod= method;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, ASTNode node, int offset, int length) {
		Assert.isNotNull(unit);
		Assert.isNotNull(node);
		fMethod= null;
		fInitialCUnit= unit;
		fInitialNode= node;
		fSelectionStart= offset;
		fSelectionLength= length;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, MethodInvocation node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_SINGLE;
		fDeleteSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, SuperMethodInvocation node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_SINGLE;
		fDeleteSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, ConstructorInvocation node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_SINGLE;
		fDeleteSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, MethodDeclaration node, int offset, int length) {
		this(unit, (ASTNode)node, offset, length);
		fSourceProvider= new SourceProvider(unit, node);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_ALL;
		fDeleteSource= true;
	}
	
	/**
	 * Creates a new inline constant refactoring
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param node the compilation unit node, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public static InlineMethodRefactoring create(ICompilationUnit unit, CompilationUnit node, int selectionStart, int selectionLength) {
		ASTNode target= getTargetNode(unit, node, selectionStart, selectionLength);
		if (target == null)
			return null;
		if (target.getNodeType() == ASTNode.METHOD_INVOCATION) {
			return new InlineMethodRefactoring(unit, (MethodInvocation)target, selectionStart, selectionLength);
		} else if (target.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return new InlineMethodRefactoring(unit, (MethodDeclaration)target, selectionStart, selectionLength);
		} else if (target.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
			return new InlineMethodRefactoring(unit, (SuperMethodInvocation)target, selectionStart, selectionLength);
		} else if (target.getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
			return new InlineMethodRefactoring(unit, (ConstructorInvocation)target, selectionStart, selectionLength);
		}
		return null;
	}
	
	public String getName() {
		return RefactoringCoreMessages.InlineMethodRefactoring_name; 
	}
	
	public boolean getDeleteSource() {
		return fDeleteSource;
	}

	public void setDeleteSource(boolean remove) {
		fDeleteSource= remove;
	}
	
	public Mode getInitialMode() {
		return fInitialMode;
	}
	
	public RefactoringStatus setCurrentMode(Mode mode) throws JavaModelException {
		if (fCurrentMode == mode)
			return new RefactoringStatus();
		Assert.isTrue(getInitialMode() == Mode.INLINE_SINGLE);
		fCurrentMode= mode;
		if (mode == Mode.INLINE_SINGLE) {
			fTargetProvider= TargetProvider.create(fInitialCUnit, (MethodInvocation)fInitialNode);
		} else {
			fTargetProvider= TargetProvider.create(
				fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration());
		}
		return fTargetProvider.checkActivation();
	}
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		if (fSourceProvider == null && Invocations.isInvocation(fInitialNode)) {
			fSourceProvider= resolveSourceProvider(result, fInitialCUnit, fInitialNode);
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
						} else {
							fDeleteSource= false;
						}
					}
					// do this after we have inlined the method calls. We still want
					// to generate the modifications.
					if (!nestedInvocations.isOK()) {
						result.merge(nestedInvocations);
						fDeleteSource= false;
					}
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
		if (fDeleteSource && fCurrentMode == Mode.INLINE_ALL) {
			TextChange change= fChangeManager.get(fSourceProvider.getCompilationUnit());
			TextEdit delete= fSourceProvider.getDeleteEdit();
			TextEditGroup description= new TextEditGroup(
				RefactoringCoreMessages.InlineMethodRefactoring_edit_delete, new TextEdit[] { delete }); 
			TextEdit root= change.getEdit();
			if (root != null) {
				// TODO instead of finding the right insert position the call inliner should
				// resuse the AST & rewriter of the source provide and we should rewrite the
				// whole AST at the end. However, since recursive calls aren't allowed there
				// shouldn't be a text edit overlap.
				// root.addChild(delete);
				TextChangeCompatibility.insert(root, delete);
			} else {
				change.setEdit(delete);
			}
			change.addTextEditGroup(description);
		}
		return new DynamicValidationStateChange(RefactoringCoreMessages.InlineMethodRefactoring_edit_inlineCall, fChangeManager.getAllChanges()) {
		
			public final ChangeDescriptor getDescriptor() {
				final Map arguments= new HashMap();
				arguments.put(JavaRefactoringDescriptor.INPUT, fInitialCUnit.getHandleIdentifier());
				arguments.put(ATTRIBUTE_SELECTION, new Integer(fSelectionStart).toString() + " " + new Integer(fSelectionLength).toString()); //$NON-NLS-1$
				arguments.put(ATTRIBUTE_DELETE, Boolean.valueOf(fDeleteSource).toString());
				arguments.put(ATTRIBUTE_MODE, new Integer(fCurrentMode == Mode.INLINE_ALL ? 1 : 0).toString());
				String project= null;
				IJavaProject javaProject= fInitialCUnit.getJavaProject();
				if (javaProject != null)
					project= javaProject.getElementName();
				final IMethodBinding binding= fSourceProvider.getDeclaration().resolveBinding();
				int flags= RefactoringDescriptor.STRUCTURAL_CHANGE;
				if (!Modifier.isPrivate(binding.getModifiers()))
					flags|= RefactoringDescriptor.MULTI_CHANGE;
				return new JavaRefactoringDescriptor(ID_INLINE_METHOD, project, Messages.format(RefactoringCoreMessages.InlineMethodRefactoring_descriptor_description, new String[] {BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED), BindingLabelProvider.getBindingLabel(binding.getDeclaringClass(), JavaElementLabels.ALL_FULLY_QUALIFIED)}), getComment(), arguments, flags);
			}
		}; 
	}
	
	private static SourceProvider resolveSourceProvider(RefactoringStatus status, ICompilationUnit unit, ASTNode invocation) throws JavaModelException {
		CompilationUnit root= (CompilationUnit)invocation.getRoot();
		IMethodBinding methodBinding= Invocations.resolveBinding(invocation);
		if (methodBinding == null) {
			status.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_error_noMethodDeclaration); 
			return null;
		}
		MethodDeclaration declaration= (MethodDeclaration)root.findDeclaringNode(methodBinding);
		if (declaration != null) {
			return new SourceProvider(unit, declaration);
		}
		IMethod method= (IMethod)methodBinding.getJavaElement();
		if (method != null) {
			ICompilationUnit source= method.getCompilationUnit();
			if (source == null) {
				status.addFatalError(Messages.format(RefactoringCoreMessages.InlineMethodRefactoring_error_classFile, method.getElementName())); 
				return null;
			}
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setSource(source);
			parser.setResolveBindings(true);
			CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
			ASTNode node= astRoot.findDeclaringNode(methodBinding.getKey());
			if (node instanceof MethodDeclaration) {
				return new SourceProvider(source, (MethodDeclaration) node);
			}
		}
		status.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_error_noMethodDeclaration); 
		return null;
	}
	
	private static ASTNode getTargetNode(ICompilationUnit unit, CompilationUnit root, int offset, int length) {
		ASTNode node= null;
		try {
			node= checkNode(NodeFinder.perform(root, offset, length, unit));
		} catch(JavaModelException e) {
			// Do nothing
		}
		if (node != null)
			return node;
		return checkNode(NodeFinder.perform(root, offset, length));
	}
	
	private static ASTNode checkNode(ASTNode node) {
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement)node).getExpression();
		}
		switch(node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return node;
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
		if (fDeleteSource) {
			file= getFile(fSourceProvider.getCompilationUnit());
			if (file != null && !result.contains(file))
				result.add(file);
		}
		return (IFile[])result.toArray(new IFile[result.size()]);
	}
	
	private IFile getFile(ICompilationUnit unit) {
		unit= JavaModelUtil.toOriginal(unit);
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
			final String delete= generic.getAttribute(ATTRIBUTE_DELETE);
			if (delete != null) {
				fDeleteSource= Boolean.valueOf(delete).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
			final String value= generic.getAttribute(ATTRIBUTE_MODE);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				int mode= 0;
				try {
					mode= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_MODE));
				}
				try {
					setCurrentMode(mode == 1 ? Mode.INLINE_ALL : Mode.INLINE_SINGLE);
				} catch (JavaModelException exception) {
					return RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage());
				}
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	public boolean canEnableDeprecationResolving() {
		return true;
	}

	public RefactoringSessionDescriptor createDeprecationResolution() {
		Assert.isNotNull(fMethod);
		final Map arguments= new HashMap();
		arguments.put(JavaRefactoringDescriptor.INPUT, fMethod.getHandleIdentifier());
		arguments.put(ATTRIBUTE_DELETE, Boolean.TRUE.toString());
		arguments.put(ATTRIBUTE_MODE, String.valueOf(1));
		String project= null;
		IJavaProject javaProject= fMethod.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaRefactorings.DEPRECATION_RESOLVING;
		try {
			if (!Flags.isPrivate(fMethod.getFlags()))
				flags|= RefactoringDescriptor.MULTI_CHANGE;
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
		}
		final RefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_INLINE_METHOD, project, Messages.format(RefactoringCoreMessages.InlineMethodRefactoring_deprecation_description, new String[] { JavaElementLabels.getTextLabel(fMethod, JavaElementLabels.ALL_FULLY_QUALIFIED)}), RefactoringCoreMessages.InlineMethodRefactoring_deprecation_comment, arguments, flags);
		return new RefactoringSessionDescriptor(new RefactoringDescriptor[] { descriptor}, RefactoringSessionDescriptor.VERSION_1_0, RefactoringCoreMessages.InlineMethodRefactoring_deprecation_comment);
	}
}