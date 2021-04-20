/*******************************************************************************
 * Copyright (c) 2017, 2018 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

/* @NonNullByDefault */
public class RedundantNullnessTypeAnnotationsFilter {

	private static final IAnnotationBinding[] NO_ANNOTATIONS= new IAnnotationBinding[0];

	/**
	 * Locations where both {@code @NonNull} and {@code @Nullable} should always be filtered:
	 * <ul>
	 * <li>for local variables, nullity is inferred by flow analysis.
	 * <li>casts with null annotation would always be unchecked.
	 * <li>for exceptions, and {@code new} expressions non-null is implied.
	 * <li>in other mentioned locations null annotations are not supported for other reasons.
	 * </ul>
	 */
	private static final EnumSet<TypeLocation> NEVER_NULLNESS_LOCATIONS= EnumSet.of(
			TypeLocation.LOCAL_VARIABLE, TypeLocation.CAST,
			TypeLocation.EXCEPTION, TypeLocation.NEW,
			TypeLocation.INSTANCEOF, TypeLocation.RECEIVER);

	public static /* @Nullable */ RedundantNullnessTypeAnnotationsFilter createIfConfigured(/* @Nullable */ ASTNode node) {
		if (node == null) {
			return null;
		}
		final ASTNode root= node.getRoot();
		if (root instanceof CompilationUnit) {
			CompilationUnit compilationUnit= (CompilationUnit) root;
			IJavaElement javaElement= compilationUnit.getJavaElement();
			IJavaProject javaProject= javaElement == null ? null : javaElement.getJavaProject();
			if (javaProject != null) {
				if (JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true))) {
					String nonNullAnnotationName= javaProject.getOption(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, true);
					String nullableAnnotationName= javaProject.getOption(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, true);
					Set<String> nonNullByDefaultNames= determineNonNullByDefaultNames(javaProject);
					if (nonNullAnnotationName == null || nullableAnnotationName == null || nonNullByDefaultNames == null) {
						return null;
					}
					EnumSet<TypeLocation> nonNullByDefaultLocations= determineNonNullByDefaultLocations(node, nonNullByDefaultNames);
					return new RedundantNullnessTypeAnnotationsFilter(nonNullAnnotationName, nullableAnnotationName, nonNullByDefaultLocations);
				}
			}
		}
		return null;
	}

	public static /* Nullable */ Set<String> determineNonNullByDefaultNames(IJavaProject javaProject) {
		String nonNullByDefaultName= javaProject.getOption(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, true);
		if (nonNullByDefaultName == null) {
			return null;
		}
		LinkedHashSet<String> nonNullByDefaultNames= new LinkedHashSet<>();
		nonNullByDefaultNames.add(nonNullByDefaultName);
		String secondaryNNBDNames= javaProject.getOption(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES, true);
		if (secondaryNNBDNames != null) {
			for (String string : secondaryNNBDNames.split(",")) { //$NON-NLS-1$
				string= string.trim();
				if (string.length() > 0) {
					nonNullByDefaultNames.add(string);
				}
			}
		}
		return nonNullByDefaultNames;
	}

	public static EnumSet<TypeLocation> determineNonNullByDefaultLocations(ASTNode astNode, Set<String> nonNullByDefaultNames) {
		// look for first @NonNullByDefault
		while (astNode != null) {
			List<IAnnotationBinding> annots= getNNBDAnnotations(astNode, nonNullByDefaultNames);
			if (annots != null) {
				return determineNNBDValue(annots);
			}
			astNode= astNode.getParent();
		}
		return EnumSet.noneOf(TypeLocation.class);
	}

	private static EnumSet<TypeLocation> determineNNBDValue(List<IAnnotationBinding> annots) {
		EnumSet<TypeLocation> result= EnumSet.noneOf(TypeLocation.class);
		for (IAnnotationBinding annot : annots) {
			IMemberValuePairBinding[] pairs= annot.getAllMemberValuePairs();
			if (pairs.length == 0) {
				ITypeBinding annotationType= annot.getAnnotationType();
				boolean foundTypeQualifierDefault= false;
				if (annotationType != null) {
					IAnnotationBinding[] annotationAnnotations= annotationType.getAnnotations();
					for (IAnnotationBinding nestedBinding : annotationAnnotations) {
						ITypeBinding nestedAnnotationType= nestedBinding.getAnnotationType();
						if (nestedAnnotationType != null) {
							if ("TypeQualifierDefault".equals(nestedAnnotationType.getName())) { //$NON-NLS-1$
								foundTypeQualifierDefault= true;
								for (final IMemberValuePairBinding pair : nestedBinding.getAllMemberValuePairs()) {
									if (pair.getKey() == null || "value".equals(pair.getKey())) { //$NON-NLS-1$
										Object value= pair.getValue();
										if (value instanceof Object[]) {
											Object[] values= (Object[]) value;
											for (Object elem : values) {
												addElementTypesAsTypeLocationValues(result, elem);
											}
										} else {
											addElementTypesAsTypeLocationValues(result, value);
										}
									}
								}
								break;
							}
						}
					}
				}
				if (!foundTypeQualifierDefault) {
					// unconfigurable NonNullByDefault annotation
					addAsTypeLocationValues(result, Boolean.TRUE);
				}
			} else {
				for (final IMemberValuePairBinding pair : pairs) {
					if (pair.getKey() == null || "value".equals(pair.getKey())) { //$NON-NLS-1$
						Object value= pair.getValue();
						if (value instanceof Object[]) {
							Object[] values= (Object[]) value;
							for (Object elem : values) {
								addAsTypeLocationValues(result, elem);
							}
						} else {
							addAsTypeLocationValues(result, value);
						}
					}
				}
			}
		}
		return result;
	}

	private static void addAsTypeLocationValues(EnumSet<TypeLocation> result, Object value) {
		if (value instanceof IVariableBinding) {
			String name= ((IVariableBinding) value).getName();
			try {
				result.add(TypeLocation.valueOf(name));
			} catch (IllegalArgumentException e) {
				// ignore
			}
		} else if (Boolean.TRUE.equals(value)) {
			result.add(TypeLocation.RETURN_TYPE);
			result.add(TypeLocation.PARAMETER);
			result.add(TypeLocation.FIELD);
		}
	}

	private static void addElementTypesAsTypeLocationValues(EnumSet<TypeLocation> result, Object value) {
		if (value instanceof IVariableBinding) {
			String name= ((IVariableBinding) value).getName();
			if (ElementType.PARAMETER.name().equals(name)) {
				result.add(TypeLocation.PARAMETER);
			} else if (ElementType.METHOD.name().equals(name)) {
				result.add(TypeLocation.RETURN_TYPE);
			} else if (ElementType.FIELD.name().equals(name)) {
				result.add(TypeLocation.FIELD);
			}
		}
	}

	// based on org.eclipse.jdt.apt.core.internal.declaration.ASTBasedDeclarationImpl.getAnnotationInstancesFromAST()
	private static /* @Nullable */ List<IAnnotationBinding> getNNBDAnnotations(ASTNode astNode, Set<String> nonNullByDefaultNames) {
		List<IExtendedModifier> extendsMods= null;
		ArrayList<IAnnotationBinding> list= null;
		switch (astNode.getNodeType()) {
			case ASTNode.COMPILATION_UNIT: {
				// special case: when reaching the root of the ast, check the package and module annotations.
				PackageDeclaration packageDeclaration= ((CompilationUnit) astNode).getPackage();
				if (packageDeclaration != null) {
					IPackageBinding packageBinding= packageDeclaration.resolveBinding();
					if (packageBinding != null) {
						for (IAnnotationBinding annotationBinding : packageBinding.getAnnotations()) {
							ITypeBinding annotationType= annotationBinding.getAnnotationType();
							if (annotationType != null) {
								if (nonNullByDefaultNames.contains(annotationType.getQualifiedName())) {
									if (list == null)
										list= new ArrayList<>();
									list.add(annotationBinding);
								}
							}
						}
						if (list != null)
							return list;
						IModuleBinding module= packageBinding.getModule();
						if (module != null) {
							for (IAnnotationBinding annotationBinding : module.getAnnotations()) {
								ITypeBinding annotationType= annotationBinding.getAnnotationType();
								if (annotationType != null && nonNullByDefaultNames.contains(annotationType.getQualifiedName())) {
									if (list == null)
										list= new ArrayList<>();
									list.add(annotationBinding);
								}
							}
							if (list != null)
								return list;
						}
					}
				}
				return null;
			}
			case ASTNode.TYPE_DECLARATION:
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
			case ASTNode.ENUM_DECLARATION:
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.FIELD_DECLARATION:
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				extendsMods= ((BodyDeclaration) astNode).modifiers();
				break;

			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				extendsMods= ((VariableDeclarationStatement) astNode).modifiers();
				break;

			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				extendsMods= ((VariableDeclarationExpression) astNode).modifiers();
				break;

			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				extendsMods= ((SingleVariableDeclaration) astNode).modifiers();
				break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				final ASTNode parent= ((VariableDeclarationFragment) astNode).getParent();
				if (parent instanceof BodyDeclaration)
					extendsMods= ((BodyDeclaration) parent).modifiers();
				break;

			default:
				return null;
		}
		if (extendsMods != null) {
			for (IExtendedModifier extMod : extendsMods) {
				if (extMod.isAnnotation()) {
					Annotation annotation= (Annotation) extMod;
					IAnnotationBinding annotationBinding= annotation.resolveAnnotationBinding();
					if (annotationBinding != null) {
						ITypeBinding annotationType= annotationBinding.getAnnotationType();
						if (annotationType != null && nonNullByDefaultNames.contains(annotationType.getQualifiedName())) {
							if (list == null)
								list= new ArrayList<>();
							list.add(annotationBinding);
						}
					}
				}
			}
			if (list != null)
				return list;
		}
		return null;
	}

	private final String fNonNullAnnotationName;

	private final String fNullableAnnotationName;

	private final EnumSet<TypeLocation> fNonNullByDefaultLocations;

	public RedundantNullnessTypeAnnotationsFilter(String nonNullAnnotationName, String nullableAnnotationName, EnumSet<TypeLocation> nonNullByDefaultLocations) {
		fNonNullAnnotationName= nonNullAnnotationName;
		fNullableAnnotationName= nullableAnnotationName;
		fNonNullByDefaultLocations= nonNullByDefaultLocations;
	}

	public IAnnotationBinding[] removeUnwantedTypeAnnotations(IAnnotationBinding[] annotations, TypeLocation location, ITypeBinding type) {
		if (location == TypeLocation.OTHER)
			return NO_ANNOTATIONS;
		if (type.isTypeVariable() || type.isWildcardType()) {
			return annotations;
		}
		boolean excludeAllNullAnnotations= NEVER_NULLNESS_LOCATIONS.contains(location);
		if (excludeAllNullAnnotations || fNonNullByDefaultLocations.contains(location)) {
			ArrayList<IAnnotationBinding> list= new ArrayList<>(annotations.length);
			for (IAnnotationBinding annotation : annotations) {
				ITypeBinding annotationType= annotation.getAnnotationType();
				if (annotationType != null
						&& (annotationType.getQualifiedName().equals(fNonNullAnnotationName) || (excludeAllNullAnnotations && annotationType.getQualifiedName().equals(fNullableAnnotationName)))) {
					// Ignore @NonNull
				} else {
					list.add(annotation);
				}
			}
			return list.size() == annotations.length ? annotations : list.toArray(new IAnnotationBinding[list.size()]);
		} else {
			return annotations;
		}
	}
}
