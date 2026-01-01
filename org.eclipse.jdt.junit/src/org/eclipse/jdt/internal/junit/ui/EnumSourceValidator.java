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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
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
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.ui.CodeStyleConfiguration;

/**
 * Validates @EnumSource annotations to ensure that excluded/included enum values
 * actually exist in the referenced enum class.
 *
 * @since 3.15
 */
public class EnumSourceValidator {

	private static final String ENUM_SOURCE_ANNOTATION = "org.junit.jupiter.params.provider.EnumSource"; //$NON-NLS-1$
	private static final String ENUM_SOURCE_MODE_CLASS = "org.junit.jupiter.params.provider.EnumSource.Mode"; //$NON-NLS-1$
	private static final String ATTR_MODE = "mode"; //$NON-NLS-1$
	private static final String ATTR_NAMES = "names"; //$NON-NLS-1$
	private static final String ATTR_VALUE = "value"; //$NON-NLS-1$
	private static final String MODE_EXCLUDE = "EXCLUDE"; //$NON-NLS-1$

	/**
	 * Validates that all values in @EnumSource names array exist in the enum.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @return List of invalid names that don't exist in the enum
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static List<String> findInvalidEnumNames(IMethod method) throws JavaModelException {
		List<String> invalidNames = new ArrayList<>();
		MethodDeclaration methodDecl = findMethodDeclaration(method);
		if (methodDecl == null) {
			return invalidNames;
		}

		final List<String> namesInAnnotation = new ArrayList<>();
		final IType[] enumType = new IType[1];
		extractEnumSourceData(methodDecl, namesInAnnotation, enumType);

		// If we found names and enum type, validate them
		if (!namesInAnnotation.isEmpty() && enumType[0] != null) {
			Set<String> validEnumConstants = getEnumConstants(enumType[0]);
			for (String name : namesInAnnotation) {
				if (!validEnumConstants.contains(name)) {
					invalidNames.add(name);
				}
			}
		}

		return invalidNames;
	}

	/**
	 * Checks if the given method has any invalid enum names in its @EnumSource annotation.
	 *
	 * @param method the test method to check
	 * @return true if there are invalid enum names
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static boolean hasInvalidEnumNames(IMethod method) throws JavaModelException {
		return !findInvalidEnumNames(method).isEmpty();
	}

	/**
	 * Extract @EnumSource annotation data: the names array and the enum type.
	 */
	private static void extractEnumSourceData(MethodDeclaration methodDecl, List<String> namesOut, IType[] enumTypeOut) {
		IMethodBinding methodBinding = methodDecl.resolveBinding();
		if (methodBinding == null) {
			return;
		}

		IAnnotationBinding[] annotations = methodBinding.getAnnotations();
		for (IAnnotationBinding annotationBinding : annotations) {
			ITypeBinding annotationType = annotationBinding.getAnnotationType();
			if (annotationType != null && ENUM_SOURCE_ANNOTATION.equals(annotationType.getQualifiedName())) {
				// Extract enum type from value attribute
				IType enumType = extractEnumType(annotationBinding, methodDecl);
				if (enumType != null) {
					enumTypeOut[0] = enumType;
				}

				// Extract names array
				extractNamesArray(annotationBinding, methodDecl, namesOut);
				break;
			}
		}
	}

	/**
	 * Extract the enum type from the @EnumSource value attribute.
	 */
	private static IType extractEnumType(IAnnotationBinding annotationBinding, MethodDeclaration methodDecl) {
		IMemberValuePairBinding[] memberValuePairs = annotationBinding.getAllMemberValuePairs();
		for (IMemberValuePairBinding pair : memberValuePairs) {
			if (ATTR_VALUE.equals(pair.getName())) {
				Object value = pair.getValue();
				if (value instanceof ITypeBinding) {
					ITypeBinding typeBinding = (ITypeBinding) value;
					String qualifiedName = typeBinding.getQualifiedName();
					if (qualifiedName != null) {
						try {
							IType type = methodDecl.resolveBinding().getDeclaringClass().getJavaElement().getJavaProject().findType(qualifiedName);
							if (type != null && type.isEnum()) {
								return type;
							}
						} catch (JavaModelException e) {
							JUnitPlugin.log(e);
						}
					}
				}
				break;
			}
		}
		return null;
	}

	/**
	 * Extract the names array from @EnumSource annotation.
	 */
	private static void extractNamesArray(IAnnotationBinding annotationBinding, MethodDeclaration methodDecl, List<String> namesOut) {
		IMemberValuePairBinding[] memberValuePairs = annotationBinding.getAllMemberValuePairs();
		for (IMemberValuePairBinding pair : memberValuePairs) {
			if (ATTR_NAMES.equals(pair.getName())) {
				Object value = pair.getValue();
				if (value instanceof Object[]) {
					Object[] values = (Object[]) value;
					for (Object val : values) {
						if (val instanceof String) {
							namesOut.add((String) val);
						}
					}
				}
				break;
			}
		}
	}

	/**
	 * Get all enum constants from an enum type.
	 */
	private static Set<String> getEnumConstants(IType enumType) {
		Set<String> constants = new HashSet<>();
		try {
			if (enumType != null && enumType.isEnum()) {
				IField[] fields = enumType.getFields();
				for (IField field : fields) {
					if (field.isEnumConstant()) {
						constants.add(field.getElementName());
					}
				}
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
		return constants;
	}

	/**
	 * Parse a compilation unit and create an AST.
	 *
	 * @param cu the compilation unit to parse
	 * @return the parsed CompilationUnit AST node
	 */
	private static CompilationUnit parseCompilationUnit(ICompilationUnit cu) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	/**
	 * Find the MethodDeclaration AST node for an IMethod.
	 *
	 * @param method the IMethod to find
	 * @return the MethodDeclaration node, or null if not found
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	private static MethodDeclaration findMethodDeclaration(IMethod method) throws JavaModelException {
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return null;
		}

		CompilationUnit astRoot = parseCompilationUnit(cu);
		final MethodDeclaration[] result = new MethodDeclaration[1];

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					result[0] = node;
					return false;
				}
				return true;
			}
		});

		return result[0];
	}

	/**
	 * Find the @EnumSource annotation on a method declaration.
	 *
	 * @param methodDecl the method declaration to search
	 * @return the @EnumSource Annotation node, or null if not found
	 */
	private static Annotation findEnumSourceAnnotation(MethodDeclaration methodDecl) {
		List<?> modifiers = methodDecl.modifiers();
		for (Object modifier : modifiers) {
			if (modifier instanceof Annotation) {
				Annotation annotation = (Annotation) modifier;
				String annotationName = annotation.getTypeName().getFullyQualifiedName();
				if ("EnumSource".equals(annotationName) || ENUM_SOURCE_ANNOTATION.equals(annotationName)) { //$NON-NLS-1$
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Find the enum type referenced in the @EnumSource annotation.
	 *
	 * @param methodDecl the method declaration with @EnumSource annotation
	 * @return the IType for the enum, or null if not found
	 */
	private static IType findEnumTypeInAnnotation(MethodDeclaration methodDecl) {
		IMethodBinding methodBinding = methodDecl.resolveBinding();
		if (methodBinding == null) {
			return null;
		}

		IAnnotationBinding[] annotations = methodBinding.getAnnotations();
		for (IAnnotationBinding annotationBinding : annotations) {
			ITypeBinding annotationType = annotationBinding.getAnnotationType();
			if (annotationType != null && ENUM_SOURCE_ANNOTATION.equals(annotationType.getQualifiedName())) {
				return extractEnumType(annotationBinding, methodDecl);
			}
		}
		return null;
	}

	/**
	 * Get all enum values from the enum type referenced in @EnumSource annotation.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @return List of all enum constant names, or empty list if not found
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static List<String> getAllEnumValues(IMethod method) throws JavaModelException {
		MethodDeclaration methodDecl = findMethodDeclaration(method);
		if (methodDecl == null) {
			return new ArrayList<>();
		}

		IType enumType = findEnumTypeInAnnotation(methodDecl);
		if (enumType != null) {
			Set<String> constants = getEnumConstants(enumType);
			return new ArrayList<>(constants);
		}

		return new ArrayList<>();
	}

	/**
	 * Get the names currently in the @EnumSource names attribute, and the mode.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @return EnumSourceNamesData containing the names list and mode, or null if not found
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static EnumSourceNamesData getEnumSourceNamesData(IMethod method) throws JavaModelException {
		MethodDeclaration methodDecl = findMethodDeclaration(method);
		if (methodDecl == null) {
			return null;
		}

		Annotation enumSourceAnnotation = findEnumSourceAnnotation(methodDecl);
		if (enumSourceAnnotation != null) {
			return extractNamesAndMode(enumSourceAnnotation);
		}

		return null;
	}

	/**
	 * Extract names and mode from @EnumSource annotation.
	 */
	private static EnumSourceNamesData extractNamesAndMode(Annotation annotation) {
		List<String> names = new ArrayList<>();
		boolean isExcludeMode = false;

		if (annotation instanceof org.eclipse.jdt.core.dom.NormalAnnotation) {
			org.eclipse.jdt.core.dom.NormalAnnotation normalAnnotation = (org.eclipse.jdt.core.dom.NormalAnnotation) annotation;
			List<?> values = normalAnnotation.values();
			for (Object obj : values) {
				if (obj instanceof MemberValuePair) {
					MemberValuePair pair = (MemberValuePair) obj;
					String name = pair.getName().getIdentifier();
					if (ATTR_MODE.equals(name)) {
						// Check if it's EXCLUDE mode
						Expression modeValue = pair.getValue();
						if (modeValue instanceof org.eclipse.jdt.core.dom.QualifiedName) {
							org.eclipse.jdt.core.dom.QualifiedName qn = (org.eclipse.jdt.core.dom.QualifiedName) modeValue;
							isExcludeMode = MODE_EXCLUDE.equals(qn.getName().getIdentifier());
						}
					} else if (ATTR_NAMES.equals(name)) {
						// Extract existing names
						Expression namesValue = pair.getValue();
						if (namesValue instanceof ArrayInitializer) {
							ArrayInitializer array = (ArrayInitializer) namesValue;
							List<?> expressions = array.expressions();
							for (Object expr : expressions) {
								if (expr instanceof StringLiteral) {
									StringLiteral literal = (StringLiteral) expr;
									names.add(literal.getLiteralValue());
								}
							}
						}
					}
				}
			}
		}

		return new EnumSourceNamesData(names, isExcludeMode);
	}

	/**
	 * Calculate how many enum values would remain after excluding the specified value.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @param valueToExclude the enum value about to be excluded
	 * @return the number of values that would remain after exclusion, or -1 if cannot determine
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static int calculateRemainingValues(IMethod method, String valueToExclude) throws JavaModelException {
		List<String> allValues = getAllEnumValues(method);
		if (allValues.isEmpty()) {
			return -1; // Cannot determine
		}

		EnumSourceNamesData namesData = getEnumSourceNamesData(method);
		if (namesData == null) {
			// No names filter yet, so excluding one will leave allValues.size() - 1
			return allValues.size() - 1;
		}

		List<String> existingNames = namesData.getNames();
		boolean isExcludeMode = namesData.isExcludeMode();

		if (isExcludeMode) {
			// Already in EXCLUDE mode: adding another exclusion
			// Count values that are not excluded
			Set<String> excluded = new HashSet<>(existingNames);
			excluded.add(valueToExclude); // Add the new exclusion
			int remaining = 0;
			for (String value : allValues) {
				if (!excluded.contains(value)) {
					remaining++;
				}
			}
			return remaining;
		} else {
			// In INCLUDE mode: removing a value from the include list
			Set<String> included = new HashSet<>(existingNames);
			included.remove(valueToExclude);

			// If removing the last value, it switches to EXCLUDE mode with just this value
			if (included.isEmpty()) {
				return allValues.size() - 1;
			}
			return included.size();
		}
	}

	/**
	 * Data class to hold @EnumSource names and mode information.
	 */
	public static class EnumSourceNamesData {
		private final List<String> names;
		private final boolean isExcludeMode;

		public EnumSourceNamesData(List<String> names, boolean isExcludeMode) {
			this.names = names;
			this.isExcludeMode = isExcludeMode;
		}

		public List<String> getNames() {
			return names;
		}

		public boolean isExcludeMode() {
			return isExcludeMode;
		}
	}

	/**
	 * Removes invalid enum names from @EnumSource annotation.
	 * This method modifies the source code to remove enum values from the names array
	 * that don't exist in the referenced enum class.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void removeInvalidEnumNames(IMethod method) throws JavaModelException {
		List<String> invalidNames = findInvalidEnumNames(method);
		if (invalidNames.isEmpty()) {
			return; // Nothing to remove
		}

		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		CompilationUnit astRoot = parseCompilationUnit(cu);
		org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite = org.eclipse.jdt.core.dom.rewrite.ASTRewrite.create(astRoot.getAST());
		final boolean[] modified = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					modified[0] = removeInvalidNamesFromAnnotation(node, invalidNames, rewrite);
				}
				return false;
			}
		});

		if (modified[0]) {
			try {
				org.eclipse.text.edits.TextEdit edit = rewrite.rewriteAST();
				cu.applyTextEdit(edit, null);
				cu.save(null, true);
			} catch (Exception e) {
				JUnitPlugin.log(e);
			}
		}
	}

	/**
	 * Remove invalid names from the @EnumSource annotation on a method.
	 */
	private static boolean removeInvalidNamesFromAnnotation(MethodDeclaration methodDecl, List<String> invalidNames, org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite) {
		List<?> modifiers = methodDecl.modifiers();
		for (Object modifier : modifiers) {
			if (modifier instanceof Annotation) {
				Annotation annotation = (Annotation) modifier;
				String annotationName = annotation.getTypeName().getFullyQualifiedName();

				if ("EnumSource".equals(annotationName) || ENUM_SOURCE_ANNOTATION.equals(annotationName)) { //$NON-NLS-1$
					return modifyAnnotationToRemoveInvalidNames(annotation, invalidNames, rewrite);
				}
			}
		}
		return false;
	}

	/**
	 * Modify the annotation to remove invalid enum names from the names array.
	 */
	private static boolean modifyAnnotationToRemoveInvalidNames(Annotation annotation, List<String> invalidNames, org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite) {
		if (!(annotation instanceof org.eclipse.jdt.core.dom.NormalAnnotation)) {
			return false;
		}

		org.eclipse.jdt.core.dom.NormalAnnotation normalAnnotation = (org.eclipse.jdt.core.dom.NormalAnnotation) annotation;
		List<?> values = normalAnnotation.values();

		for (Object obj : values) {
			if (obj instanceof MemberValuePair) {
				MemberValuePair pair = (MemberValuePair) obj;
				if (ATTR_NAMES.equals(pair.getName().getIdentifier())) {
					Expression value = pair.getValue();
					if (value instanceof ArrayInitializer) {
						ArrayInitializer arrayInit = (ArrayInitializer) value;
						List<?> expressions = arrayInit.expressions();

						// Find and remove invalid names
						List<Expression> toRemove = new ArrayList<>();
						for (Object expr : expressions) {
							if (expr instanceof StringLiteral) {
								StringLiteral literal = (StringLiteral) expr;
								if (invalidNames.contains(literal.getLiteralValue())) {
									toRemove.add((Expression) expr);
								}
							}
						}

						if (!toRemove.isEmpty()) {
							org.eclipse.jdt.core.dom.rewrite.ListRewrite listRewrite = rewrite.getListRewrite(arrayInit, ArrayInitializer.EXPRESSIONS_PROPERTY);
							for (Expression expr : toRemove) {
								listRewrite.remove(expr, null);
							}
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Gets the list of excluded enum names from @EnumSource annotation.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @return List of excluded enum names, or empty list if not in EXCLUDE mode
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static List<String> getExcludedNames(IMethod method) throws JavaModelException {
		EnumSourceNamesData data = getEnumSourceNamesData(method);
		if (data != null && data.isExcludeMode()) {
			return new ArrayList<>(data.getNames());
		}
		return new ArrayList<>();
	}

	/**
	 * Updates the excluded names in @EnumSource annotation.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @param names the new list of names to exclude
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void updateExcludedNames(IMethod method, List<String> names) throws JavaModelException {
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		CompilationUnit astRoot = parseCompilationUnit(cu);
		AST ast = astRoot.getAST();
		org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite = org.eclipse.jdt.core.dom.rewrite.ASTRewrite.create(ast);
		final boolean[] modified = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					modified[0] = updateNamesInAnnotation(node, names, rewrite, ast);
				}
				return false;
			}
		});

		if (modified[0]) {
			try {
				org.eclipse.text.edits.TextEdit edit = rewrite.rewriteAST();
				cu.applyTextEdit(edit, null);
				cu.save(null, true);
			} catch (Exception e) {
				JUnitPlugin.log(e);
			}
		}
	}

	/**
	 * Update the names array in @EnumSource annotation.
	 */
	private static boolean updateNamesInAnnotation(MethodDeclaration methodDecl, List<String> newNames, org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite, AST ast) {
		Annotation enumSourceAnnotation = findEnumSourceAnnotation(methodDecl);
		if (!(enumSourceAnnotation instanceof org.eclipse.jdt.core.dom.NormalAnnotation)) {
			return false;
		}

		org.eclipse.jdt.core.dom.NormalAnnotation normalAnnotation = (org.eclipse.jdt.core.dom.NormalAnnotation) enumSourceAnnotation;
		List<?> values = normalAnnotation.values();

		for (Object obj : values) {
			if (obj instanceof MemberValuePair) {
				MemberValuePair pair = (MemberValuePair) obj;
				if (ATTR_NAMES.equals(pair.getName().getIdentifier())) {
					// Create new array with updated names
					ArrayInitializer newArray = ast.newArrayInitializer();
					for (String name : newNames) {
						StringLiteral literal = ast.newStringLiteral();
						literal.setLiteralValue(name);
						newArray.expressions().add(literal);
					}
					rewrite.replace(pair.getValue(), newArray, null);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Removes the mode and names attributes from @EnumSource annotation,
	 * restoring it to include all enum values. Also removes the Mode import
	 * if it's no longer used elsewhere in the file.
	 *
	 * @param method the test method with @EnumSource annotation
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void removeExcludeMode(IMethod method) throws JavaModelException {
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		CompilationUnit astRoot = parseCompilationUnit(cu);
		org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite = org.eclipse.jdt.core.dom.rewrite.ASTRewrite.create(astRoot.getAST());
		final boolean[] modified = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					modified[0] = removeModeAndNamesFromAnnotation(node, rewrite);
				}
				return false;
			}
		});

		if (modified[0]) {
			try {
				// Check if Mode is still used elsewhere in the file
				boolean isModeUsedElsewhere = isModeTypeUsed(astRoot, method);

				// Combine AST rewrite with import removal if needed
				org.eclipse.text.edits.MultiTextEdit multiEdit = new org.eclipse.text.edits.MultiTextEdit();

				// Add import removal if Mode is not used elsewhere
				if (!isModeUsedElsewhere) {
					ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
					importRewrite.removeImport(ENUM_SOURCE_MODE_CLASS);

					org.eclipse.text.edits.TextEdit importEdit = importRewrite.rewriteImports(null);
					if (importEdit.hasChildren() || importEdit.getLength() != 0) {
						multiEdit.addChild(importEdit);
					}
				}

				// Add AST rewrite
				org.eclipse.text.edits.TextEdit rewriteEdit = rewrite.rewriteAST();
				if (rewriteEdit.hasChildren() || rewriteEdit.getLength() != 0) {
					multiEdit.addChild(rewriteEdit);
				}

				// Apply the combined edit
				if (multiEdit.hasChildren()) {
					cu.applyTextEdit(multiEdit, null);
					cu.save(null, true);
				}
			} catch (Exception e) {
				JUnitPlugin.log(e);
			}
		}
	}

	/**
	 * Remove mode and names attributes from @EnumSource annotation.
	 */
	private static boolean removeModeAndNamesFromAnnotation(MethodDeclaration methodDecl, org.eclipse.jdt.core.dom.rewrite.ASTRewrite rewrite) {
		Annotation enumSourceAnnotation = findEnumSourceAnnotation(methodDecl);
		if (!(enumSourceAnnotation instanceof org.eclipse.jdt.core.dom.NormalAnnotation)) {
			return false;
		}

		org.eclipse.jdt.core.dom.NormalAnnotation normalAnnotation = (org.eclipse.jdt.core.dom.NormalAnnotation) enumSourceAnnotation;
		org.eclipse.jdt.core.dom.rewrite.ListRewrite listRewrite = rewrite.getListRewrite(normalAnnotation, org.eclipse.jdt.core.dom.NormalAnnotation.VALUES_PROPERTY);

		List<?> values = normalAnnotation.values();
		boolean modified = false;

		for (Object obj : values) {
			if (obj instanceof MemberValuePair) {
				MemberValuePair pair = (MemberValuePair) obj;
				String name = pair.getName().getIdentifier();
				if (ATTR_MODE.equals(name) || ATTR_NAMES.equals(name)) {
					listRewrite.remove((MemberValuePair) obj, null);
					modified = true;
				}
			}
		}

		return modified;
	}

	/**
	 * Check if the Mode type is still used elsewhere in the compilation unit.
	 * This method checks all @EnumSource annotations (except the one being modified)
	 * to see if any of them still use mode=Mode.EXCLUDE or mode=Mode.INCLUDE.
	 *
	 * <p><b>Note on Method Matching:</b> This method uses parameter count matching
	 * as a heuristic for method identification. This is acceptable for JUnit test
	 * methods because:
	 * <ul>
	 * <li>Test methods are typically not overloaded (JUnit best practices)</li>
	 * <li>The IMethod parameter comes from the test runner which already identifies
	 * the exact method</li>
	 * <li>The AST traversal is just to find the AST node for that specific IMethod</li>
	 * </ul>
	 *
	 * @param astRoot the compilation unit AST root
	 * @param excludeMethod the method being modified (to exclude from the search)
	 * @return true if Mode is still referenced somewhere else in the file
	 */
	private static boolean isModeTypeUsed(CompilationUnit astRoot, IMethod excludeMethod) {
		final boolean[] isUsed = new boolean[] { false };
		final String excludeMethodName = excludeMethod.getElementName();
		final String[] excludeMethodParamTypes;
		excludeMethodParamTypes = excludeMethod.getParameterTypes();

		astRoot.accept(new ASTVisitor() {
			private MethodDeclaration currentMethod = null;

			@Override
			public boolean visit(MethodDeclaration node) {
				currentMethod = node;
				// Skip the method we're modifying by comparing name and parameter count
				if (node.getName().getIdentifier().equals(excludeMethodName)) {
					// Check parameter count as additional verification
					if (node.parameters().size() == excludeMethodParamTypes.length) {
						return false; // Skip this method
					}
				}
				return true;
			}

			@Override
			public void endVisit(MethodDeclaration node) {
				if (currentMethod == node) {
					currentMethod = null;
				}
			}

			@Override
			public boolean visit(MemberValuePair node) {
				// Only process if we're not in the excluded method
				if (currentMethod != null) {
					// Check if this is a mode attribute
					if (ATTR_MODE.equals(node.getName().getIdentifier())) {
						Expression value = node.getValue();
						// Check various expression types that could reference Mode
						if (isModeExpression(value)) {
							isUsed[0] = true;
						}
					}
				}
				return true;
			}
		});

		return isUsed[0];
	}

	/**
	 * Check if an expression references the Mode type.
	 * Handles QualifiedName (Mode.EXCLUDE), FieldAccess (EnumSource.Mode.EXCLUDE),
	 * and SimpleName (EXCLUDE with static import).
	 */
	private static boolean isModeExpression(Expression expr) {
		if (expr instanceof org.eclipse.jdt.core.dom.QualifiedName) {
			// Check for Mode.EXCLUDE or Mode.INCLUDE
			org.eclipse.jdt.core.dom.QualifiedName qn = (org.eclipse.jdt.core.dom.QualifiedName) expr;
			String qualifierName = qn.getQualifier().toString();
			if ("Mode".equals(qualifierName)) { //$NON-NLS-1$
				return true;
			}
			// Also check for fully qualified EnumSource.Mode.EXCLUDE pattern
			// Only match if it ends with our specific Mode class
			if (qualifierName.equals("EnumSource.Mode") || //$NON-NLS-1$
				qualifierName.endsWith(".EnumSource.Mode")) { //$NON-NLS-1$
				return true;
			}
		} else if (expr instanceof org.eclipse.jdt.core.dom.FieldAccess) {
			// Check for obj.Mode.EXCLUDE pattern
			org.eclipse.jdt.core.dom.FieldAccess fa = (org.eclipse.jdt.core.dom.FieldAccess) expr;
			Expression expression = fa.getExpression();
			if (expression instanceof org.eclipse.jdt.core.dom.QualifiedName) {
				org.eclipse.jdt.core.dom.QualifiedName qn = (org.eclipse.jdt.core.dom.QualifiedName) expression;
				if ("Mode".equals(qn.getName().getIdentifier())) { //$NON-NLS-1$
					return true;
				}
			} else if (expression instanceof org.eclipse.jdt.core.dom.SimpleName) {
				if ("Mode".equals(((org.eclipse.jdt.core.dom.SimpleName) expression).getIdentifier())) { //$NON-NLS-1$
					return true;
				}
			}
		} else if (expr instanceof org.eclipse.jdt.core.dom.SimpleName) {
			// Could be static import like: mode = EXCLUDE
			// Check if the resolved binding is from Mode enum
			org.eclipse.jdt.core.dom.SimpleName sn = (org.eclipse.jdt.core.dom.SimpleName) expr;
			ITypeBinding typeBinding = sn.resolveTypeBinding();
			// Handle null binding (can occur with compilation errors or incomplete resolution)
			if (typeBinding != null) {
				String qualifiedName = typeBinding.getQualifiedName();
				if (ENUM_SOURCE_MODE_CLASS.equals(qualifiedName)) {
					return true;
				}
			}
			// If type binding is null, we can't determine - assume it's not Mode to be safe
		}
		return false;
	}

	/**
	 * Fallback method to check Mode usage by method name only.
	 */
	private static boolean isUsedByNameOnly(CompilationUnit astRoot, String excludeMethodName) {
		final boolean[] isUsed = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				// Skip the method we're modifying
				if (node.getName().getIdentifier().equals(excludeMethodName)) {
					return false;
				}
				return true;
			}

			@Override
			public boolean visit(MemberValuePair node) {
				// Check if this is a mode attribute
				if (ATTR_MODE.equals(node.getName().getIdentifier())) {
					Expression value = node.getValue();
					if (isModeExpression(value)) {
						isUsed[0] = true;
					}
				}
				return true;
			}
		});

		return isUsed[0];
	}

	/**
	 * Checks if method has @EnumSource with excluded values.
	 *
	 * @param method the test method to check
	 * @return true if the method has @EnumSource with mode=EXCLUDE and non-empty names
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static boolean hasExcludedValues(IMethod method) throws JavaModelException {
		EnumSourceNamesData data = getEnumSourceNamesData(method);
		return data != null && data.isExcludeMode() && !data.getNames().isEmpty();
	}
}
