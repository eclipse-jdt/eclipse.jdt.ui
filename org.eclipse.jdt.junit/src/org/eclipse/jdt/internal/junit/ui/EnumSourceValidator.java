/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

import org.eclipse.jdt.ui.CodeStyleConfiguration;

/**
 * Validates and modifies a single direct {@code @EnumSource} on a parameterized test.
 *
 * <p>The exclusion action is deliberately conservative. It is available only when the
 * selected invocation can be mapped unambiguously to one enum constant and when changing
 * the annotation preserves its existing semantics. Methods with multiple argument sources,
 * repeatable {@code @EnumSource} annotations, or non-EXCLUDE name filters are rejected.
 *
 * @since 3.17
 */
public final class EnumSourceValidator {

	private static final String JUNIT5_PARAMETERIZED_TEST= "org.junit.jupiter.params.ParameterizedTest"; //$NON-NLS-1$
	private static final String ENUM_SOURCE_ANNOTATION= "org.junit.jupiter.params.provider.EnumSource"; //$NON-NLS-1$
	private static final String ENUM_SOURCES_ANNOTATION= "org.junit.jupiter.params.provider.EnumSources"; //$NON-NLS-1$
	private static final String ARGUMENTS_SOURCE_ANNOTATION= "org.junit.jupiter.params.provider.ArgumentsSource"; //$NON-NLS-1$
	private static final String ARGUMENTS_SOURCES_ANNOTATION= "org.junit.jupiter.params.provider.ArgumentsSources"; //$NON-NLS-1$

	private static final String MODE_INCLUDE= "INCLUDE"; //$NON-NLS-1$
	private static final String MODE_EXCLUDE= "EXCLUDE"; //$NON-NLS-1$
	private static final String MEMBER_VALUE= "value"; //$NON-NLS-1$
	private static final String MEMBER_MODE= "mode"; //$NON-NLS-1$
	private static final String MEMBER_NAMES= "names"; //$NON-NLS-1$
	private static final String MEMBER_FROM= "from"; //$NON-NLS-1$
	private static final String MEMBER_TO= "to"; //$NON-NLS-1$

	private static final Pattern INVOCATION_INDEX_PATTERN=
			Pattern.compile("\\[test-template-invocation:#(\\d+)\\]"); //$NON-NLS-1$

	private enum ArgumentsSourceStatus {
		FOUND,
		NOT_FOUND,
		UNKNOWN
	}

	static final class ExclusionTarget {
		private final IMethod fMethod;
		private final String fEnumConstantName;
		private final int fRemainingValues;

		ExclusionTarget(IMethod method, String enumConstantName, int remainingValues) {
			fMethod= method;
			fEnumConstantName= enumConstantName;
			fRemainingValues= remainingValues;
		}

		IMethod getMethod() {
			return fMethod;
		}

		String getEnumConstantName() {
			return fEnumConstantName;
		}

		int getRemainingValues() {
			return fRemainingValues;
		}
	}

	private static final class ParsedEnumSource {
		private final ICompilationUnit fCompilationUnit;
		private final CompilationUnit fAstRoot;
		private final Annotation fAnnotation;
		private final String fMode;
		private final List<String> fNames;
		private final List<String> fEffectiveValues;
		private final boolean fSupportsExclusion;

		ParsedEnumSource(ICompilationUnit compilationUnit, CompilationUnit astRoot, Annotation annotation,
				String mode, List<String> names, List<String> effectiveValues, boolean supportsExclusion) {
			fCompilationUnit= compilationUnit;
			fAstRoot= astRoot;
			fAnnotation= annotation;
			fMode= mode;
			fNames= names;
			fEffectiveValues= effectiveValues;
			fSupportsExclusion= supportsExclusion;
		}
	}

	static ExclusionTarget findExclusionTarget(TestCaseElement testCaseElement) {
		if (testCaseElement == null) {
			return null;
		}

		TestSuiteElement parent= testCaseElement.getParent();
		if (parent == null) {
			return null;
		}

		IMethod method= TestMethodFinder.findMethodForParameterizedTest(parent);
		if (method == null) {
			return null;
		}

		try {
			ParsedEnumSource parsed= parse(method);
			if (parsed == null || !parsed.fSupportsExclusion) {
				return null;
			}

			int invocationIndex= getInvocationIndex(testCaseElement, parsed.fEffectiveValues.size());
			if (invocationIndex < 0) {
				return null;
			}

			String enumConstantName= parsed.fEffectiveValues.get(invocationIndex);
			return new ExclusionTarget(method, enumConstantName, parsed.fEffectiveValues.size() - 1);
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns the enum constant for a one-based JUnit invocation index.
	 *
	 * @param method the parameterized test method
	 * @param invocationIndex one-based invocation index
	 * @return the enum constant name, or <code>null</code> if the source is unsupported
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static String getEnumConstantForInvocation(IMethod method, int invocationIndex) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		if (parsed == null || !parsed.fSupportsExclusion
				|| invocationIndex < 1 || invocationIndex > parsed.fEffectiveValues.size()) {
			return null;
		}
		return parsed.fEffectiveValues.get(invocationIndex - 1);
	}

	/**
	 * Returns whether the given enum constant can safely be excluded.
	 *
	 * @param method the parameterized test method
	 * @param enumConstantName the enum constant name
	 * @return <code>true</code> if the transformation is supported
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static boolean canExcludeEnumValue(IMethod method, String enumConstantName) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		return parsed != null && parsed.fSupportsExclusion
				&& parsed.fEffectiveValues.contains(enumConstantName)
				&& !parsed.fNames.contains(enumConstantName);
	}

	/**
	 * Returns whether the method has one supported {@code @EnumSource} in EXCLUDE mode.
	 *
	 * @param method the method to inspect
	 * @return <code>true</code> if EXCLUDE mode is active
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static boolean isExcludeMode(IMethod method) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		return parsed != null && parsed.fSupportsExclusion && MODE_EXCLUDE.equals(parsed.fMode);
	}

	/**
	 * Returns excluded enum constant names for one supported direct {@code @EnumSource}.
	 *
	 * @param method the method to inspect
	 * @return excluded names, never <code>null</code>
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static List<String> getExcludedNames(IMethod method) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		if (parsed == null || !parsed.fSupportsExclusion || !MODE_EXCLUDE.equals(parsed.fMode)) {
			return new ArrayList<>();
		}
		return new ArrayList<>(parsed.fNames);
	}

	/**
	 * Adds an enum constant to the EXCLUDE filter while preserving all unrelated annotation
	 * members such as {@code from} and {@code to}.
	 *
	 * @param method the parameterized test method
	 * @param enumConstantName the enum constant to exclude
	 * @return <code>true</code> if the source was changed
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static boolean excludeEnumValue(IMethod method, String enumConstantName) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		if (parsed == null || !parsed.fSupportsExclusion
				|| !parsed.fEffectiveValues.contains(enumConstantName)
				|| parsed.fNames.contains(enumConstantName)) {
			return false;
		}

		AST ast= parsed.fAstRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(parsed.fAstRoot, true);
		List<ASTNode> removedNodes= new ArrayList<>();

		if (parsed.fAnnotation instanceof NormalAnnotation) {
			NormalAnnotation annotation= (NormalAnnotation) parsed.fAnnotation;
			ListRewrite valuesRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);

			MemberValuePair modePair= findMemberValuePair(annotation, MEMBER_MODE);
			if (!MODE_EXCLUDE.equals(parsed.fMode)) {
				Expression excludeMode= createExcludeModeExpression(ast, importRewrite);
				if (modePair == null) {
					modePair= ast.newMemberValuePair();
					modePair.setName(ast.newSimpleName(MEMBER_MODE));
					modePair.setValue(excludeMode);
					valuesRewrite.insertLast(modePair, null);
				} else {
					removedNodes.add(modePair.getValue());
					rewrite.replace(modePair.getValue(), excludeMode, null);
				}
			}

			MemberValuePair namesPair= findMemberValuePair(annotation, MEMBER_NAMES);
			if (namesPair == null) {
				namesPair= ast.newMemberValuePair();
				namesPair.setName(ast.newSimpleName(MEMBER_NAMES));
				ArrayInitializer names= ast.newArrayInitializer();
				names.expressions().add(newStringLiteral(ast, enumConstantName));
				namesPair.setValue(names);
				valuesRewrite.insertLast(namesPair, null);
			} else if (namesPair.getValue() instanceof ArrayInitializer) {
				ArrayInitializer names= (ArrayInitializer) namesPair.getValue();
				ListRewrite namesRewrite= rewrite.getListRewrite(names, ArrayInitializer.EXPRESSIONS_PROPERTY);
				namesRewrite.insertLast(newStringLiteral(ast, enumConstantName), null);
			} else {
				ArrayInitializer names= ast.newArrayInitializer();
				names.expressions().add(ASTNode.copySubtree(ast, namesPair.getValue()));
				names.expressions().add(newStringLiteral(ast, enumConstantName));
				rewrite.replace(namesPair.getValue(), names, null);
			}
		} else {
			NormalAnnotation replacement= ast.newNormalAnnotation();
			replacement.setTypeName((Name) ASTNode.copySubtree(ast, parsed.fAnnotation.getTypeName()));

			if (parsed.fAnnotation instanceof SingleMemberAnnotation) {
				MemberValuePair valuePair= ast.newMemberValuePair();
				valuePair.setName(ast.newSimpleName(MEMBER_VALUE));
				valuePair.setValue((Expression) ASTNode.copySubtree(ast,
						((SingleMemberAnnotation) parsed.fAnnotation).getValue()));
				replacement.values().add(valuePair);
			}

			MemberValuePair modePair= ast.newMemberValuePair();
			modePair.setName(ast.newSimpleName(MEMBER_MODE));
			modePair.setValue(createExcludeModeExpression(ast, importRewrite));
			replacement.values().add(modePair);

			MemberValuePair namesPair= ast.newMemberValuePair();
			namesPair.setName(ast.newSimpleName(MEMBER_NAMES));
			ArrayInitializer names= ast.newArrayInitializer();
			names.expressions().add(newStringLiteral(ast, enumConstantName));
			namesPair.setValue(names);
			replacement.values().add(namesPair);

			rewrite.replace(parsed.fAnnotation, replacement, null);
		}

		return applyChanges(parsed, rewrite, importRewrite, removedNodes);
	}

	/**
	 * Removes one enum constant from the EXCLUDE filter.
	 *
	 * @param method the parameterized test method
	 * @param enumConstantName the enum constant to re-include
	 * @return <code>true</code> if the source was changed
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static boolean removeValueFromExclusion(IMethod method, String enumConstantName) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		if (parsed == null || !parsed.fSupportsExclusion || !MODE_EXCLUDE.equals(parsed.fMode)
				|| !parsed.fNames.contains(enumConstantName)) {
			return false;
		}
		if (parsed.fNames.size() == 1) {
			return removeExcludeMode(method);
		}
		if (!(parsed.fAnnotation instanceof NormalAnnotation)) {
			return false;
		}

		NormalAnnotation annotation= (NormalAnnotation) parsed.fAnnotation;
		MemberValuePair namesPair= findMemberValuePair(annotation, MEMBER_NAMES);
		if (namesPair == null) {
			return false;
		}

		ASTRewrite rewrite= ASTRewrite.create(parsed.fAstRoot.getAST());
		ASTNode removedNode= null;
		if (namesPair.getValue() instanceof ArrayInitializer) {
			ArrayInitializer names= (ArrayInitializer) namesPair.getValue();
			for (Object value : names.expressions()) {
				Expression expression= (Expression) value;
				if (enumConstantName.equals(expression.resolveConstantExpressionValue())) {
					rewrite.getListRewrite(names, ArrayInitializer.EXPRESSIONS_PROPERTY).remove(expression, null);
					removedNode= expression;
					break;
				}
			}
		} else if (enumConstantName.equals(namesPair.getValue().resolveConstantExpressionValue())) {
			removedNode= namesPair;
			rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY).remove(namesPair, null);
		}

		if (removedNode == null) {
			return false;
		}
		List<ASTNode> removedNodes= new ArrayList<>();
		removedNodes.add(removedNode);
		return applyChanges(parsed, rewrite,
				CodeStyleConfiguration.createImportRewrite(parsed.fAstRoot, true), removedNodes);
	}

	/**
	 * Removes {@code mode} and {@code names}, preserving every other annotation member.
	 *
	 * @param method the parameterized test method
	 * @return <code>true</code> if the source was changed
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static boolean removeExcludeMode(IMethod method) throws JavaModelException {
		ParsedEnumSource parsed= parse(method);
		if (parsed == null || !parsed.fSupportsExclusion || !MODE_EXCLUDE.equals(parsed.fMode)
				|| !(parsed.fAnnotation instanceof NormalAnnotation)) {
			return false;
		}

		NormalAnnotation annotation= (NormalAnnotation) parsed.fAnnotation;
		ASTRewrite rewrite= ASTRewrite.create(parsed.fAstRoot.getAST());
		ListRewrite valuesRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);

		List<ASTNode> removedNodes= new ArrayList<>();
		MemberValuePair modePair= findMemberValuePair(annotation, MEMBER_MODE);
		if (modePair != null) {
			valuesRewrite.remove(modePair, null);
			removedNodes.add(modePair);
		}
		MemberValuePair namesPair= findMemberValuePair(annotation, MEMBER_NAMES);
		if (namesPair != null) {
			valuesRewrite.remove(namesPair, null);
			removedNodes.add(namesPair);
		}
		if (removedNodes.isEmpty()) {
			return false;
		}

		return applyChanges(parsed, rewrite,
				CodeStyleConfiguration.createImportRewrite(parsed.fAstRoot, true), removedNodes);
	}

	private static ParsedEnumSource parse(IMethod method) throws JavaModelException {
		ICompilationUnit compilationUnit= method.getCompilationUnit();
		if (compilationUnit == null) {
			return null;
		}

		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		MethodDeclaration methodDeclaration= findMethodDeclaration(astRoot, method);
		if (methodDeclaration == null) {
			return null;
		}

		Annotation enumSource= findSingleDirectEnumSource(methodDeclaration);
		if (enumSource == null) {
			return null;
		}

		IAnnotationBinding enumSourceBinding= enumSource.resolveAnnotationBinding();
		IMethodBinding methodBinding= methodDeclaration.resolveBinding();
		if (enumSourceBinding == null || methodBinding == null) {
			return null;
		}

		String mode= getMode(enumSourceBinding);
		List<String> names= getNames(enumSourceBinding);
		if (mode == null || names == null) {
			return null;
		}

		boolean supportsExclusion= MODE_EXCLUDE.equals(mode)
				|| MODE_INCLUDE.equals(mode) && names.isEmpty();

		ITypeBinding enumType= getEnumType(enumSourceBinding, methodBinding);
		List<String> enumConstants= getEnumConstants(enumType);
		if (enumConstants.isEmpty()) {
			return null;
		}

		String from= getStringMember(enumSourceBinding, MEMBER_FROM);
		String to= getStringMember(enumSourceBinding, MEMBER_TO);
		List<String> effectiveValues= computeEffectiveValues(enumConstants, from, to, mode, names);
		if (effectiveValues == null) {
			supportsExclusion= false;
			effectiveValues= new ArrayList<>();
		}

		return new ParsedEnumSource(compilationUnit, astRoot, enumSource, mode, names,
				effectiveValues, supportsExclusion);
	}

	private static MethodDeclaration findMethodDeclaration(CompilationUnit astRoot, IMethod method) {
		MethodDeclaration[] result= new MethodDeclaration[1];
		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				IMethodBinding binding= node.resolveBinding();
				if (binding != null && method.equals(binding.getJavaElement())) {
					result[0]= node;
				}
				return false;
			}
		});
		return result[0];
	}

	private static Annotation findSingleDirectEnumSource(MethodDeclaration methodDeclaration) {
		boolean hasDirectParameterizedTest= false;
		Annotation enumSource= null;
		int argumentSourceCount= 0;

		for (Object modifier : methodDeclaration.modifiers()) {
			if (!(modifier instanceof Annotation)) {
				continue;
			}
			Annotation annotation= (Annotation) modifier;
			IAnnotationBinding binding= annotation.resolveAnnotationBinding();
			if (binding == null || binding.getAnnotationType() == null) {
				return null;
			}

			ITypeBinding annotationType= binding.getAnnotationType();
			String qualifiedName= annotationType.getQualifiedName();
			if (JUNIT5_PARAMETERIZED_TEST.equals(qualifiedName)) {
				hasDirectParameterizedTest= true;
			}
			if (ENUM_SOURCE_ANNOTATION.equals(qualifiedName)) {
				enumSource= annotation;
				argumentSourceCount++;
			} else if (ENUM_SOURCES_ANNOTATION.equals(qualifiedName)) {
				argumentSourceCount += 2;
			} else {
				ArgumentsSourceStatus status=
						hasArgumentsSourceMetaAnnotation(annotationType, new HashSet<>());
				if (status == ArgumentsSourceStatus.UNKNOWN) {
					return null;
				}
				if (status == ArgumentsSourceStatus.FOUND) {
					argumentSourceCount++;
				}
			}
		}

		return hasDirectParameterizedTest && argumentSourceCount == 1 ? enumSource : null;
	}

	private static ArgumentsSourceStatus hasArgumentsSourceMetaAnnotation(ITypeBinding annotationType,
			Set<String> visited) {
		if (annotationType == null) {
			return ArgumentsSourceStatus.UNKNOWN;
		}

		String qualifiedName= annotationType.getQualifiedName();
		if (ARGUMENTS_SOURCE_ANNOTATION.equals(qualifiedName)
				|| ARGUMENTS_SOURCES_ANNOTATION.equals(qualifiedName)) {
			return ArgumentsSourceStatus.FOUND;
		}

		String key= annotationType.getKey();
		String visitedKey= key == null || key.isEmpty() ? qualifiedName : key;
		if (visitedKey == null || visitedKey.isEmpty()) {
			return ArgumentsSourceStatus.UNKNOWN;
		}
		if (!visited.add(visitedKey)) {
			return ArgumentsSourceStatus.NOT_FOUND;
		}

		boolean unknown= false;
		for (IAnnotationBinding metaAnnotation : annotationType.getAnnotations()) {
			if (metaAnnotation == null) {
				unknown= true;
				continue;
			}
			ArgumentsSourceStatus status=
					hasArgumentsSourceMetaAnnotation(metaAnnotation.getAnnotationType(), visited);
			if (status == ArgumentsSourceStatus.FOUND) {
				return status;
			}
			if (status == ArgumentsSourceStatus.UNKNOWN) {
				unknown= true;
			}
		}
		return unknown ? ArgumentsSourceStatus.UNKNOWN : ArgumentsSourceStatus.NOT_FOUND;
	}

	private static ITypeBinding getEnumType(IAnnotationBinding enumSourceBinding,
			IMethodBinding methodBinding) {
		for (IMemberValuePairBinding pair : enumSourceBinding.getDeclaredMemberValuePairs()) {
			if (MEMBER_VALUE.equals(pair.getName()) && pair.getValue() instanceof ITypeBinding) {
				ITypeBinding type= (ITypeBinding) pair.getValue();
				return type.isEnum() ? type.getTypeDeclaration() : null;
			}
		}

		ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
		if (parameterTypes.length == 0 || !parameterTypes[0].isEnum()) {
			return null;
		}
		return parameterTypes[0].getTypeDeclaration();
	}

	private static List<String> getEnumConstants(ITypeBinding enumType) throws JavaModelException {
		List<String> result= new ArrayList<>();
		if (enumType == null || !enumType.isEnum()) {
			return result;
		}

		IJavaElement javaElement= enumType.getJavaElement();
		if (!(javaElement instanceof IType)) {
			return result;
		}
		IType javaType= (IType) javaElement;
		if (!javaType.isEnum()) {
			return result;
		}

		for (IField field : javaType.getFields()) {
			if (field.isEnumConstant()) {
				result.add(field.getElementName());
			}
		}
		return result;
	}

	private static String getMode(IAnnotationBinding binding) {
		for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
			if (MEMBER_MODE.equals(pair.getName())) {
				return pair.getValue() instanceof IVariableBinding
						? ((IVariableBinding) pair.getValue()).getName()
						: null;
			}
		}
		return MODE_INCLUDE;
	}

	private static List<String> getNames(IAnnotationBinding binding) {
		for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
			if (!MEMBER_NAMES.equals(pair.getName())) {
				continue;
			}
			List<String> result= new ArrayList<>();
			Object value= pair.getValue();
			if (value instanceof Object[]) {
				for (Object item : (Object[]) value) {
					if (!(item instanceof String)) {
						return null;
					}
					result.add((String) item);
				}
				return result;
			}
			if (value instanceof String) {
				result.add((String) value);
				return result;
			}
			return null;
		}
		return new ArrayList<>();
	}

	private static String getStringMember(IAnnotationBinding binding, String memberName) {
		for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
			if (memberName.equals(pair.getName())) {
				return pair.getValue() instanceof String ? (String) pair.getValue() : null;
			}
		}
		return null;
	}

	private static List<String> computeEffectiveValues(List<String> enumConstants, String from,
			String to, String mode, List<String> names) {
		int first= from == null || from.isEmpty() ? 0 : enumConstants.indexOf(from);
		int last= to == null || to.isEmpty() ? enumConstants.size() - 1 : enumConstants.indexOf(to);
		if (first < 0 || last < first) {
			return null;
		}

		List<String> result= new ArrayList<>(enumConstants.subList(first, last + 1));
		if (MODE_EXCLUDE.equals(mode)) {
			result.removeIf(names::contains);
		} else if (!MODE_INCLUDE.equals(mode) || !names.isEmpty()) {
			return null;
		}
		return result;
	}

	private static int getInvocationIndex(TestCaseElement testCaseElement, int valueCount) {
		String uniqueId= testCaseElement.getUniqueId();
		if (uniqueId == null) {
			return -1;
		}

		Matcher matcher= INVOCATION_INDEX_PATTERN.matcher(uniqueId);
		int oneBasedIndex= -1;
		try {
			while (matcher.find()) {
				oneBasedIndex= Integer.parseInt(matcher.group(1));
			}
		} catch (NumberFormatException e) {
			return -1;
		}
		return oneBasedIndex >= 1 && oneBasedIndex <= valueCount ? oneBasedIndex - 1 : -1;
	}

	private static MemberValuePair findMemberValuePair(NormalAnnotation annotation, String name) {
		for (Object value : annotation.values()) {
			MemberValuePair pair= (MemberValuePair) value;
			if (name.equals(pair.getName().getIdentifier())) {
				return pair;
			}
		}
		return null;
	}

	private static Expression createExcludeModeExpression(AST ast, ImportRewrite importRewrite) {
		String enumSourceType= importRewrite.addImport(ENUM_SOURCE_ANNOTATION);
		Name modeType= ast.newQualifiedName(ast.newName(enumSourceType), ast.newSimpleName("Mode")); //$NON-NLS-1$
		return ast.newQualifiedName(modeType, ast.newSimpleName(MODE_EXCLUDE));
	}

	private static StringLiteral newStringLiteral(AST ast, String value) {
		StringLiteral literal= ast.newStringLiteral();
		literal.setLiteralValue(value);
		return literal;
	}

	private static boolean applyChanges(ParsedEnumSource parsed, ASTRewrite rewrite,
			ImportRewrite importRewrite, List<ASTNode> removedNodes) {
		try {
			if (!removedNodes.isEmpty()) {
				ImportRemover importRemover=
						new ImportRemover(parsed.fCompilationUnit.getJavaProject(), parsed.fAstRoot);
				for (ASTNode removedNode : removedNodes) {
					importRemover.registerRemovedNode(removedNode);
				}
				importRemover.applyRemoves(importRewrite);
			}

			MultiTextEdit combinedEdit= new MultiTextEdit();
			TextEdit importEdit= importRewrite.rewriteImports(null);
			if (importEdit.hasChildren() || importEdit.getLength() != 0) {
				combinedEdit.addChild(importEdit);
			}
			TextEdit rewriteEdit= rewrite.rewriteAST();
			if (rewriteEdit.hasChildren() || rewriteEdit.getLength() != 0) {
				combinedEdit.addChild(rewriteEdit);
			}
			if (!combinedEdit.hasChildren()) {
				return false;
			}

			parsed.fCompilationUnit.applyTextEdit(combinedEdit, null);
			parsed.fCompilationUnit.save(null, true);
			return true;
		} catch (Exception e) {
			JUnitPlugin.log(e);
			return false;
		}
	}

	private EnumSourceValidator() {
		// Utility class - no instances
	}
}
