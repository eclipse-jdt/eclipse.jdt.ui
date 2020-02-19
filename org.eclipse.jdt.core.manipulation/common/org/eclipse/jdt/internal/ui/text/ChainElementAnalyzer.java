/**
 * Copyright (c) 2011, 2019 Stefan Henss and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Henß - initial API and implementation.
 */
package org.eclipse.jdt.internal.ui.text;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public final class ChainElementAnalyzer {

	private static final Map<String, IType> typeCache= new HashMap<>();

	private static final Predicate<IField> NON_STATIC_FIELDS_ONLY_FILTER = t -> {
		try {
			return !Flags.isStatic(t.getFlags());
		} catch (JavaModelException e) {
			return true;
		}
	};

    private static final Predicate<IMethod> RELEVANT_NON_STATIC_METHODS_ONLY_FILTER = m -> {
		try {
			return !Flags.isStatic(m.getFlags()) && !isVoid(m) && !m.isConstructor();
		} catch (JavaModelException e) {
			return false;
		}
	};

    private static final Predicate<IField> STATIC_FIELDS_ONLY_FILTER = t -> {
		try {
			return Flags.isStatic(t.getFlags());
		} catch (JavaModelException e) {
			return false;
		}
	};

    private static final Predicate<IMethod> STATIC_NON_VOID_NON_PRIMITIVE_METHODS_ONLY_FILTER = m -> {
		try {
			return Flags.isStatic(m.getFlags()) && !isVoid(m) && !m.isConstructor();
		} catch (JavaModelException e) {
			return false;
		}
	};

    private ChainElementAnalyzer() {
    }

    private static boolean isVoid(final IMethod m) {
        try {
			return String.valueOf(Signature.C_VOID).equals(m.getReturnType());
		} catch (JavaModelException e) {
			return false;
		}
    }

    public static Collection<IJavaElement> findVisibleInstanceFieldsAndRelevantInstanceMethods(final ChainType type,
            final ChainType receiverType) {
        return findFieldsAndMethods(type, receiverType, NON_STATIC_FIELDS_ONLY_FILTER,
                RELEVANT_NON_STATIC_METHODS_ONLY_FILTER);
    }

    public static Collection<IJavaElement> findAllPublicStaticFieldsAndNonVoidNonPrimitiveStaticMethods(
            final ChainType type, final ChainType receiverType) {
        return findFieldsAndMethods(type, receiverType, STATIC_FIELDS_ONLY_FILTER,
                STATIC_NON_VOID_NON_PRIMITIVE_METHODS_ONLY_FILTER);
    }

    private static Collection<IJavaElement> findFieldsAndMethods(final ChainType type, final ChainType receiverType,
            final Predicate<IField> fieldFilter, final Predicate<IMethod> methodFilter) {
        final Map<String, IJavaElement> tmp = new LinkedHashMap<>();
        for (final IType cur : findAllSupertypesIncludingArgument(type)) {
            try {
				for (final IMethod method : cur.getMethods()) {
				    if (!methodFilter.test(method) || !methodCanBeSeenBy(method, receiverType.getType())) {
				        continue;
				    }
				    final String key = method.getKey();
				    if (!tmp.containsKey(key)) {
				        tmp.put(key, method);
				    }
				}
			} catch (JavaModelException e) {
				// ignore
			}
            try {
				for (final IField field : cur.getFields()) {
				    if (!fieldFilter.test(field) || !fieldCanBeSeenBy(field, receiverType.getType())) {
				        continue;
				    }
				    final String key = field.getKey();
				    if (!tmp.containsKey(key)) {
				        tmp.put(key, field);
				    }
				}
			} catch (JavaModelException e) {
				// ignore
			}
        }
        return tmp.values();
    }

    private static List<IType> findAllSupertypesIncludingArgument(final ChainType type) {
        if ((type.getPrimitiveType() != null)) {
            return Collections.emptyList();
        }
        final List<IType> supertypes = new LinkedList<>();
        final LinkedList<IType> queue = new LinkedList<>();
        queue.add(type.getType());
        while (!queue.isEmpty()) {
            final IType superType = queue.poll();
            if (superType == null || supertypes.contains(superType)) {
                continue;
            }
            supertypes.add(superType);
			try {
				String superClass= superType.getSuperclassTypeSignature();
				if (superClass != null) {
					IType superClassType= ChainElementAnalyzer.getTypeFromSignature(type.getType().getJavaProject(), superClass, superType);
					queue.add(superClassType);
				}
				for (final String interfc : superType.getSuperInterfaceTypeSignatures()) {
					IType interfcType= ChainElementAnalyzer.getTypeFromSignature(type.getType().getJavaProject(), interfc, superType);
					queue.add(interfcType);
				}
			} catch (JavaModelException e) {
				// ignore
			}
        }
        return supertypes;
    }

    public static boolean isAssignable(final ChainElement edge, final IType expectedType,
            final int expectedDimension) {
        if (expectedDimension <= edge.getReturnTypeDimension()) {
            final IType base = edge.getReturnType().getType();
            if (isAssignmentCompatible(base, expectedType)) {
                return true;
            }
            final LinkedList<IType> supertypes = new LinkedList<>();
            supertypes.add(base);
            String expectedSignature = expectedType.getFullyQualifiedName();

            while (!supertypes.isEmpty()) {
                final IType type = supertypes.poll();
                String typeSignature = type.getFullyQualifiedName();

                if (typeSignature.equals(expectedSignature)) {
                    return true;
                }
				try {
					if (type.getSuperclassTypeSignature() != null) {
						IType superclass= ChainElementAnalyzer.getTypeFromSignature(type.getJavaProject(), type.getSuperclassTypeSignature(), type);
						if (superclass != null) {
							supertypes.add(superclass);
						}
						for (final String intf : type.getSuperInterfaceTypeSignatures()) {
							IType intfType= ChainElementAnalyzer.getTypeFromSignature(type.getJavaProject(), intf, type);
							supertypes.add(intfType);
						}
					}
				} catch (JavaModelException e) {
					// ignore
				}
            }
        }
        return false;
    }

    private static boolean isAssignmentCompatible(IType base, IType expectedType) {
        LinkedList<IType> types= new LinkedList<> ();
        types.add(base);
        try {
			while (!types.isEmpty()) {
				IType type= types.poll();
				if (type.getSuperclassTypeSignature() != null && !"java.lang.Object".equals(type.getSuperclassName())) { //$NON-NLS-1$
					IType superType= ChainElementAnalyzer.getTypeFromSignature(type.getJavaProject(), type.getSuperclassTypeSignature(), type);
					if (expectedType.equals(superType)) {
						return true;
					}
					types.add(superType);
				}

				if (type.getSuperInterfaceNames().length > 0) {
					for (String iface : type.getSuperInterfaceTypeSignatures()) {
						IType ifaceType= ChainElementAnalyzer.getTypeFromSignature(type.getJavaProject(), iface, type);
						if (expectedType.equals(ifaceType)) {
							return true;
						}
						types.add(ifaceType);
					}
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
        return false;
	}

    public static List<ChainType> resolveBindingsForExpectedTypes(final IJavaProject proj, final CompletionContext ctx) {
        final List<ChainType> types = new LinkedList<>();
        final IType expectedTypeSig = getExpectedType(proj, ctx);
        if (expectedTypeSig == null) {
            final char[][] expectedTypes= ctx.getExpectedTypesSignatures();
            String typeSig= new String(expectedTypes[0]);
            int dim= ChainElementAnalyzer.getArrayDimension(ctx.getExpectedTypesSignatures());
            ChainType type= new ChainType(typeSig, dim);
            types.add(type);
        } else {
            types.add(new ChainType(expectedTypeSig));
        }
        return types;
    }

	public static IType getExpectedType(final IJavaProject proj, final CompletionContext ctx) {
		IType expected= null;
		String fqExpectedType= getExpectedFullyQualifiedTypeName(ctx);
		if (fqExpectedType != null) {
			try {
				expected= proj.findType(fqExpectedType);
			} catch (JavaModelException e) {
				// do nothing
			}
		}
		return expected;
	}

	public static String getExpectedFullyQualifiedTypeName(final CompletionContext ctx) {
		String fqExpectedType= null;
		final char[][] expectedTypes= ctx.getExpectedTypesSignatures();
		if (expectedTypes != null && expectedTypes.length > 0) {
			fqExpectedType= SignatureUtil.stripSignatureToFQN(new String(expectedTypes[0]));
		}
		return fqExpectedType;
	}

	private static int getArrayDimension(final char[][] expectedTypesSignatures) {
		if (expectedTypesSignatures != null && expectedTypesSignatures.length > 0) {
			return Signature.getArrayCount(new String(expectedTypesSignatures[0]));
		}

		return 0;
	}

	public static IType getTypeFromSignature (IJavaProject proj, String typeSig, IType declType) {
		IType cType= typeCache.get(typeSig);
		if (cType != null) {
			return cType;
		}
		// Unresolved types, same simple name, one is super-type of other
		// Avoid caching unresolved types to prevent possible cycles
		boolean isResolved= true;
		String eType= Signature.getElementType(typeSig);
		if (eType.charAt(0) == Signature.C_UNRESOLVED) {
			isResolved= false;
		}
		String type= SignatureUtil.stripSignatureToFQN(typeSig);
		IType res= null;
		try {
			res= proj.findType(type);
			if (res != null) {
				if (isResolved) {
					typeCache.put(typeSig, res);
				}
				return res;
			}
			String[][] resType= declType.resolveType(type);
			if (resType != null) {
				String fqExpectedType= JavaModelUtil.concatenateName(resType[0][0], resType[0][1]);
				res= proj.findType(fqExpectedType);
				if (isResolved) {
					typeCache.put(typeSig, res);
				}
				return res;
			}
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}

	private static boolean methodCanBeSeenBy(IMethod mb, IType invocationType) {
		try {
			if (Flags.isPublic(mb.getFlags())) {
				return true;
			}
		} catch (JavaModelException e1) {
			// ignore
		}
		if (invocationType.equals(mb.getDeclaringType())) {
			return true;
		}

		String invocationPackage= invocationType.getPackageFragment().getElementName();
		String methodPackage= mb.getDeclaringType().getPackageFragment().getElementName();
		try {
			if (Flags.isProtected(mb.getFlags())) {
				if (invocationPackage.equals(methodPackage)) {
					return false; // isSuper ?
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}

		try {
			if (Flags.isPrivate(mb.getFlags())) {
				IType mTypeRoot= mb.getDeclaringType();
				while (invocationType.getDeclaringType() != null) {
					mTypeRoot= mTypeRoot.getDeclaringType();
				}
				IType invTypeRoot= invocationType;
				while (invTypeRoot.getDeclaringType() != null) {
					invTypeRoot= invTypeRoot.getDeclaringType();
				}
				return mTypeRoot.equals(invTypeRoot);
			}
		} catch (JavaModelException e) {
			// ignore
		}

		return invocationPackage.equals(methodPackage);
	}

	private static boolean fieldCanBeSeenBy(IField fb, IType invocationType) {
		try {
			if (Flags.isPublic(fb.getFlags())) {
				return true;
			}
		} catch (JavaModelException e2) {
			// ignore
		}

		if (invocationType.equals(fb.getDeclaringType())) {
			return true;
		}

		String invocationpackage = invocationType.getPackageFragment().getElementName();
		String fieldPackage = fb.getDeclaringType().getPackageFragment().getElementName();
		try {
			if (Flags.isProtected(fb.getFlags())) {
				if (invocationType.equals(fb.getDeclaringType())) {
					return true;
				}
				if (invocationpackage.equals(fieldPackage)) {
					return true;
				}

				IType currType= invocationType;
				while (currType.getSuperclassTypeSignature() != null) {
					currType= ChainElementAnalyzer.getTypeFromSignature(currType.getJavaProject(), currType.getSuperclassTypeSignature(), currType);
					if (currType.equals(fb.getDeclaringType())) {
						return true;
					}
				}
			}
		} catch (JavaModelException e1) {
			// ignore
		}

		try {
			if (Flags.isPrivate(fb.getFlags())) {
				IType fTypeRoot= fb.getDeclaringType();
				while (invocationType.getDeclaringType() != null) {
					fTypeRoot= fTypeRoot.getDeclaringType();
				}
				IType invTypeRoot= invocationType;
				while (invTypeRoot.getDeclaringType() != null) {
					invTypeRoot= invTypeRoot.getDeclaringType();
				}
				if (fTypeRoot.equals(invTypeRoot)) {
					return true;
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}

		if (! invocationpackage.equals(fieldPackage)) {
			return false;
		}

		return false;
	}

	public static boolean isPrimitive (String typeSig) {
		String elementType= Signature.getElementType(typeSig);
		int kind= Signature.getTypeSignatureKind(elementType);
		return kind == Signature.BASE_TYPE_SIGNATURE || kind == Signature.TYPE_VARIABLE_SIGNATURE;
	}

}
