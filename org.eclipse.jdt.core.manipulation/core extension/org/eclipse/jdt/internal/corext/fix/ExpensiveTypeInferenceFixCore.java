/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.code.Invocations;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class ExpensiveTypeInferenceFixCore extends CompilationUnitRewriteOperationsFixCore {

	// JLS 3.8 excludes these spellings from TypeIdentifier. Older libraries may still declare them.
	private static final Set<String> RESTRICTED_TYPE_NAMES= Set.of("var", "yield", "record", "sealed", "permits"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	public static ExpensiveTypeInferenceFixCore createFix(CompilationUnit root, IProblemLocation[] problems) {
		if (!(root.getJavaElement() instanceof ICompilationUnit unit) || problems.length == 0) {
			return null;
		}
		Map<MethodInvocation, List<Type>> calls= findCandidates(root, problems);
		if (calls.isEmpty()) {
			return null;
		}
		Map<MethodInvocation, List<Type>> accepted= validateCandidates(root, unit, calls);
		return accepted.isEmpty() ? null : new ExpensiveTypeInferenceFixCore(root, accepted);
	}

	private static Map<MethodInvocation, List<Type>> findCandidates(CompilationUnit root, IProblemLocation[] problems) {
		Map<MethodInvocation, List<Type>> calls= new LinkedHashMap<>();
		ScopeAnalyzer scope= new ScopeAnalyzer(root);
		ImportRewrite imports= null;
		for (IProblemLocation problem : problems) {
			if (problem.getProblemId() != IProblem.PotentiallyExpensiveTypeInference || !(problem.getCoveredNode(root) instanceof SimpleName name)
					|| name.getLocationInParent() != MethodInvocation.NAME_PROPERTY || name.getStartPosition() != problem.getOffset() || name.getLength() != problem.getLength()) {
				continue;
			}
			MethodInvocation call= (MethodInvocation) name.getParent();
			if (calls.containsKey(call) || !canInsertTypeArguments(call)) {
				continue;
			}
			if (imports == null) {
				imports= StubUtility.createImportRewrite(root, true);
			}
			try {
				calls.put(call, typeArguments(call, imports, scope));
			} catch (IllegalArgumentException e) {
				// A binary type or package can have a name that is not a Java source identifier.
			}
		}
		return calls;
	}

	private static Map<MethodInvocation, List<Type>> validateCandidates(CompilationUnit root, ICompilationUnit unit, Map<MethodInvocation, List<Type>> calls) {
		// Resolve all proposed spellings together. An inferred type can be inaccessible, or its
		// qualified name can resolve to a different type when a package name is shadowed.
		try {
			String original= unit.getSource();
			if (original == null || original.length() != root.getLength()) {
				return Map.of();
			}
			Document preview= new Document(original);
			ASTRewrite rewrite= ASTRewrite.create(root.getAST());
			Map<MethodInvocation, ITrackedNodePosition> positions= new LinkedHashMap<>();
			for (var entry : calls.entrySet()) {
				MethodInvocation call= entry.getKey();
				insertArguments(rewrite, call, entry.getValue(), null);
				positions.put(call, rewrite.track(call.getName()));
			}
			Map<String, String> options= unit.getOptions(true);
			rewrite.rewriteAST(preview, options).apply(preview, TextEdit.UPDATE_REGIONS);
			ASTParser parser= ASTParser.newParser(root.getAST().apiLevel());
			parser.setProject(unit.getJavaProject());
			parser.setWorkingCopyOwner(unit.getOwner());
			parser.setUnitName(unit.getPath().toString());
			parser.setResolveBindings(true);
			options.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, "0"); //$NON-NLS-1$
			options.put(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, JavaCore.DISABLED);
			options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaCore.ERROR);
			options.put(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, JavaCore.ERROR);
			parser.setCompilerOptions(options);
			parser.setSource(preview.get().toCharArray());
			CompilationUnit checked= (CompilationUnit) parser.createAST(null);
			IProblem[] diagnostics= checked.getProblems();
			Map<Integer, MethodInvocation> byOffset= new HashMap<>();
			positions.forEach((call, location) -> byOffset.put(location.getStartPosition(), call));
			Map<MethodInvocation, List<Type>> accepted= new LinkedHashMap<>();
			checked.accept(new ASTVisitor() {
				@Override
				public boolean visit(MethodInvocation edited) {
					MethodInvocation call= byOffset.get(edited.getName().getStartPosition());
					if (call != null && preservesBinding(call, edited, diagnostics)) {
						accepted.put(call, calls.get(call));
					}
					return true;
				}
			});
			return accepted;
		} catch (JavaModelException | BadLocationException e) {
			JavaManipulationPlugin.log(e);
			return Map.of();
		}
	}

	private static void insertArguments(ASTRewrite rewrite, MethodInvocation call, List<Type> types, TextEditGroup group) {
		ListRewrite arguments= Invocations.getInferredTypeArgumentsRewrite(rewrite, call);
		types.forEach(type -> arguments.insertLast(type, group));
	}

	private static List<Type> typeArguments(MethodInvocation call, ImportRewrite imports, ScopeAnalyzer scope) {
		// Do not introduce imports: accepting or skipping one edit must not change name lookup
		// at another call or elsewhere in the compilation unit.
		IBinding[] visible= scope.getDeclarationsInScope(call.getStartPosition(), ScopeAnalyzer.TYPES);
		var context= new ImportRewriteContext() {
			@Override
			public int findInContext(String qualifier, String name, int kind) {
				for (IBinding binding : visible) {
					if (binding instanceof ITypeBinding type && type.getTypeDeclaration().getName().equals(name)) {
						return type.getTypeDeclaration().getQualifiedName().equals(qualifier + '.' + name) ? RES_NAME_FOUND : RES_NAME_CONFLICT;
					}
				}
				int result= imports.getDefaultImportRewriteContext().findInContext(qualifier, name, kind);
				if (result == RES_NAME_UNKNOWN && "java.lang".equals(qualifier)) { //$NON-NLS-1$
					return RES_NAME_FOUND;
				}
				return result == RES_NAME_FOUND ? RES_NAME_FOUND : RES_NAME_CONFLICT;
			}
		};
		return Arrays.stream(Invocations.getInferredTypeArguments(call))
				.map(type -> imports.addImport(type, call.getAST(), context, TypeLocation.TYPE_ARGUMENT)).toList();
	}

	private static boolean preservesBinding(MethodInvocation original, MethodInvocation edited, IProblem[] diagnostics) {
		IMethodBinding binding= edited.resolveMethodBinding();
		// A parameterized method key identifies the declaration and its actual type arguments.
		if (binding == null || binding.isRecovered() || edited.typeArguments().isEmpty() || !original.resolveMethodBinding().getKey().equals(binding.getKey())
				|| !Arrays.stream(binding.getTypeArguments()).allMatch(ExpensiveTypeInferenceFixCore::isConcreteType)) {
			return false;
		}
		int firstType= ((Type) edited.typeArguments().get(0)).getStartPosition();
		int selector= edited.getName().getStartPosition();
		int end= edited.getStartPosition() + edited.getLength();
		for (IProblem problem : diagnostics) {
			int start= problem.getSourceStart();
			if ((problem.isError() && start < end && problem.getSourceEnd() >= edited.getStartPosition())
					|| (start >= firstType && start < selector)) {
				return false;
			}
		}
		return true;
	}

	private static boolean canInsertTypeArguments(MethodInvocation call) {
		if (!call.typeArguments().isEmpty() || call.getExpression() == null) {
			return false;
		}
		ITypeBinding receiver= call.getExpression().resolveTypeBinding();
		IMethodBinding method= call.resolveMethodBinding();
		if (receiver == null || receiver.isRecovered() || receiver.isTypeVariable() || receiver.isIntersectionType() || receiver.isCapture()
				|| method == null || method.isRecovered() || method.isRawMethod() || !method.isVarargs() || (receiver.isRawType() && !Modifier.isStatic(method.getModifiers()))
				|| method.getTypeArguments().length == 0 || !Arrays.stream(method.getTypeArguments()).allMatch(ExpensiveTypeInferenceFixCore::isConcreteType)) {
			return false;
		}
		// An enclosing overload could change when a nested poly expression becomes standalone.
		ASTNode expression= call;
		while (expression.getParent() instanceof ParenthesizedExpression) {
			expression= expression.getParent();
		}
		ASTNode parent= expression.getParent();
		if (!(parent instanceof VariableDeclarationFragment || parent instanceof ExpressionStatement
				|| parent instanceof ReturnStatement && ASTResolving.findParentMethodDeclaration(call) != null
				|| parent instanceof Assignment assignment && assignment.getOperator() == Assignment.Operator.ASSIGN && assignment.getRightHandSide() == expression)) {
			return false;
		}
		int arity= call.arguments().size();
		IMethodBinding declaration= method.getMethodDeclaration();
		return !hasCompetingOverload(receiver, declaration, arity)
				&& Bindings.visitHierarchy(receiver, type -> !hasCompetingOverload(type, declaration, arity));
	}

	private static boolean isConcreteType(ITypeBinding type) {
		if (!(type.isClass() || type.isInterface() || type.isEnum() || type.isRecord()) || type.isRecovered() || type.isRawType() || RESTRICTED_TYPE_NAMES.contains(type.getErasure().getName())
				|| type.isAnonymous() || type.isLocal() || type.isIntersectionType() || type.isDeprecated() || type.getTypeAnnotations().length != 0) {
			return false;
		}
		ITypeBinding owner= type.getDeclaringClass();
		return (owner == null || isConcreteType(Modifier.isStatic(type.getModifiers()) ? owner.getTypeDeclaration() : owner))
				&& Arrays.stream(type.getTypeArguments()).allMatch(ExpensiveTypeInferenceFixCore::isConcreteType);
	}

	private static boolean hasCompetingOverload(ITypeBinding type, IMethodBinding method, int arity) {
		if (type.isRecovered()) {
			return true;
		}
		for (IMethodBinding other : type.getDeclaredMethods()) {
			if (other.getName().equals(method.getName()) && !method.isEqualTo(other.getMethodDeclaration())) {
				int parameters= other.getParameterTypes().length;
				if (parameters == arity || (other.isVarargs() && arity >= parameters - 1)) {
					return true;
				}
			}
		}
		return false;
	}

	private ExpensiveTypeInferenceFixCore(CompilationUnit root, Map<MethodInvocation, List<Type>> calls) {
		super(FixMessages.ExpensiveTypeInferenceFix_description, root, new CompilationUnitRewriteOperation() {
			@Override
			public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) {
				TextEditGroup group= createTextEditGroup(FixMessages.ExpensiveTypeInferenceFix_description, cuRewrite);
				calls.forEach((call, types) -> insertArguments(cuRewrite.getASTRewrite(), call, types, group));
			}
		});
	}
}
