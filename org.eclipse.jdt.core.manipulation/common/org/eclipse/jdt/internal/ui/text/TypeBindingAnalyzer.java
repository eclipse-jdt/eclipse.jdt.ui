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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

public final class TypeBindingAnalyzer {

	private static final Predicate<IVariableBinding> NON_STATIC_FIELDS_ONLY_FILTER = new Predicate<IVariableBinding>() {
		@Override
		public boolean test(IVariableBinding t) {
			return !Modifier.isStatic(t.getModifiers());
		}
    };

    private static final Predicate<IMethodBinding> RELEVANT_NON_STATIC_METHODS_ONLY_FILTER = new Predicate<IMethodBinding>() {
		@Override
		public boolean test(IMethodBinding m) {
			return !Modifier.isStatic(m.getModifiers()) && !isVoid(m) & !m.isConstructor() && !hasPrimitiveReturnType(m);
		}
    };

    private static final Predicate<IVariableBinding> STATIC_FIELDS_ONLY_FILTER = new Predicate<IVariableBinding>() {
		@Override
		public boolean test(IVariableBinding t) {
			return Modifier.isStatic(t.getModifiers());
		}
    };

    private static final Predicate<IMethodBinding> STATIC_NON_VOID_NON_PRIMITIVE_METHODS_ONLY_FILTER = new Predicate<IMethodBinding>() {
		@Override
		public boolean test(IMethodBinding m) {
			return Modifier.isStatic(m.getModifiers()) && !isVoid(m) && !m.isConstructor() && hasPrimitiveReturnType(m);
		}
    };

    private TypeBindingAnalyzer() {
    }

    private static boolean isVoid(final IMethodBinding m) {
        return hasPrimitiveReturnType(m) && Bindings.isVoidType(m.getReturnType());
    }

    private static boolean hasPrimitiveReturnType(final IMethodBinding m) {
        return m.getReturnType().isPrimitive();
    }

    public static Collection<IBinding> findVisibleInstanceFieldsAndRelevantInstanceMethods(final ITypeBinding type,
            final IJavaElement invocationSite) {
        return findFieldsAndMethods(type, invocationSite, NON_STATIC_FIELDS_ONLY_FILTER,
                RELEVANT_NON_STATIC_METHODS_ONLY_FILTER);
    }

    public static Collection<IBinding> findAllPublicStaticFieldsAndNonVoidNonPrimitiveStaticMethods(
            final ITypeBinding type, final IJavaElement invocationSite) {
        return findFieldsAndMethods(type, invocationSite, STATIC_FIELDS_ONLY_FILTER,
                STATIC_NON_VOID_NON_PRIMITIVE_METHODS_ONLY_FILTER);
    }

    private static Collection<IBinding> findFieldsAndMethods(final ITypeBinding type, final IJavaElement invocationSite,
            final Predicate<IVariableBinding> fieldFilter, final Predicate<IMethodBinding> methodFilter) {
        final Map<String, IBinding> tmp = new LinkedHashMap<>();
        final IType invocationType = ((IMember) invocationSite).getCompilationUnit().findPrimaryType();
        final ITypeBinding receiverType = getTypeBindingFrom(invocationType);
        for (final ITypeBinding cur : findAllSupertypesIncludingArgument(type)) {
            for (final IMethodBinding method : cur.getDeclaredMethods()) {
                if (!methodFilter.test(method) || !methodCanBeSeenBy(method, receiverType)) {
                    continue;
                }
                final String key = createMethodKey(method);
                if (!tmp.containsKey(key)) {
                    tmp.put(key, method);
                }
            }
            for (final IVariableBinding field : cur.getDeclaredFields()) {
                if (!fieldFilter.test(field) || !fieldCanBeSeenBy(field, receiverType)) {
                    continue;
                }
                final String key = createFieldKey(field);
                if (!tmp.containsKey(key)) {
                    tmp.put(key, field);
                }
            }
        }
        return tmp.values();
    }

    private static List<ITypeBinding> findAllSupertypesIncludingArgument(final ITypeBinding type) {
        final ITypeBinding base = removeArrayWrapper(type);
        if (base.isPrimitive() || Bindings.isVoidType(type)) {
            return Collections.emptyList();
        }
        final List<ITypeBinding> supertypes = new LinkedList<>();
        final LinkedList<ITypeBinding> queue = new LinkedList<>();
        queue.add(base);
        while (!queue.isEmpty()) {
            final ITypeBinding superType = queue.poll();
            if (superType == null || supertypes.contains(superType)) {
                continue;
            }
            supertypes.add(superType);
            queue.add(superType.getSuperclass());
            for (final ITypeBinding interfc : superType.getInterfaces()) {
                queue.add(interfc);
            }
        }
        return supertypes;
    }

    private static String createFieldKey(final IVariableBinding field) {
		StringBuilder ret= new StringBuilder(field.getName());
		try {
			String typeSignature= ((IField)field.getJavaElement()).getTypeSignature();
			return ret.append(typeSignature).toString();
		} catch (JavaModelException e) {
			return ret.toString();
		}
    }

    private static String createMethodKey(final IMethodBinding method) {
        if (method.getJavaElement() instanceof IMethod) {
        StringBuilder ret= new StringBuilder().append(method.getName());
            try {
                IMethod m = (IMethod) method.getJavaElement();
                String signature= String.valueOf(m.getSignature());
                int index = signature.lastIndexOf(")"); //$NON-NLS-1$
                final String signatureWithoutReturnType = index == -1 ? signature : signature.substring(0, index);
                return ret.append(signatureWithoutReturnType).toString();
            } catch (JavaModelException e) {
                return ret.toString();
            }
        }
        return null;
    }

    public static boolean isAssignable(final ChainElement edge, final ITypeBinding expectedType,
            final int expectedDimension) {
        if (expectedDimension <= edge.getReturnTypeDimension()) {
            final ITypeBinding base = removeArrayWrapper(edge.getReturnType());
            if (base.isAssignmentCompatible(expectedType)) {
                return true;
            }
            final LinkedList<ITypeBinding> supertypes = new LinkedList<>();
            supertypes.add(base);
            String expectedSignature = expectedType.getBinaryName();

            while (!supertypes.isEmpty()) {
                final ITypeBinding type = supertypes.poll();
                String typeSignature = type.getBinaryName();

                if (typeSignature.equals(expectedSignature)) {
                    return true;
                }
                final ITypeBinding superclass = type.getSuperclass();
                if (superclass != null) {
                    supertypes.add(superclass);
                }
                for (final ITypeBinding intf : type.getInterfaces()) {
                    supertypes.add(intf);
                }
            }
        }
        return false;
    }

    public static ITypeBinding removeArrayWrapper(final ITypeBinding type) {
        if (type.getComponentType() != null) {
            ITypeBinding base = type;
            while (base.getComponentType() != null) {
                base = base.getComponentType();
            }
            return base;
        } else {
            return type;
        }
    }

    public static List<ITypeBinding> resolveBindingsForExpectedTypes(final IJavaProject proj, final ICompilationUnit cu, final CompletionContext ctx) {
        final List<ITypeBinding> bindings = new LinkedList<>();
        final IType expectedTypeSig = getExpectedType(proj, ctx);
		if (expectedTypeSig == null) {
			ASTParser parser= createParser(cu);
			AST ast= parser.createAST(null).getAST();
			ITypeBinding binding= ast.resolveWellKnownType(TypeBindingAnalyzer.getExpectedFullyQualifiedTypeName(ctx));
			int dim= TypeBindingAnalyzer.getArrayDimension(ctx.getExpectedTypesSignatures());
			if (dim > 0) {
				binding= binding.createArrayType(dim);
			}
			bindings.add(binding);
		} else {
			IBinding[] res= resolveBindingsForTypes(cu, new IJavaElement[] { expectedTypeSig });
			if (res.length == 1 && res[0] instanceof ITypeBinding) {
				bindings.add((ITypeBinding) res[0]);
			}
		}

        return bindings;
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

	private static ITypeBinding getTypeBindingFrom(IType type) {
		IBinding[] res= resolveBindingsForTypes(type.getCompilationUnit(), new IJavaElement [] { type });
		if (res.length == 1 && res[0] instanceof ITypeBinding) {
			return (ITypeBinding) res[0];
		}

		return null;
	}

	private static boolean methodCanBeSeenBy(IMethodBinding mb, ITypeBinding invocationType) {
		if (Modifier.isPublic(mb.getModifiers())) {
			return true;
		}
		if (Bindings.equals(invocationType, mb.getDeclaringClass())) {
			return true;
		}

		String invocationPackage= invocationType.getPackage().getName();
		String methodPackage= mb.getDeclaringClass().getPackage().getName();
		if (Modifier.isProtected(mb.getModifiers())) {
			if (invocationPackage.equals(methodPackage)) {
				return false; // isSuper ?
			}
		}

		if (Modifier.isPrivate(mb.getModifiers())) {
			ITypeBinding mTypeRoot= mb.getDeclaringClass();
			while (invocationType.getDeclaringClass() != null) {
				mTypeRoot= mTypeRoot.getDeclaringClass();
			}
			ITypeBinding invTypeRoot= invocationType;
			while (invTypeRoot.getDeclaringClass() != null) {
				invTypeRoot= invTypeRoot.getDeclaringClass();
			}
			return Bindings.equals(invTypeRoot, mTypeRoot);
		}

		return invocationPackage.equals(methodPackage);
	}

	private static boolean fieldCanBeSeenBy(IVariableBinding fb, ITypeBinding invocationType) {
		if (Modifier.isPublic(fb.getModifiers())) {
			return true;
		}

		if (Bindings.equals(invocationType, fb.getDeclaringClass())) {
			return true;
		}

		String invocationpackage = invocationType.getPackage().getName();
		String fieldPackage = fb.getDeclaringClass().getPackage().getName();
		if (Modifier.isProtected(fb.getModifiers())) {
			if (Bindings.equals(invocationType, fb.getDeclaringClass())) {
				return true;
			}
			if (invocationpackage.equals(fieldPackage)) {
				return true;
			}

			ITypeBinding currType = invocationType.getSuperclass();
			while (currType != null) {
				if (Bindings.equals(currType, fb.getDeclaringClass())) {
					return true;
				}
				currType = currType.getSuperclass();
			}
		}

		if (Modifier.isPrivate(fb.getModifiers())) {
			ITypeBinding fTypeRoot= fb.getDeclaringClass();
			while (invocationType.getDeclaringClass() != null) {
				fTypeRoot= fTypeRoot.getDeclaringClass();
			}
			ITypeBinding invTypeRoot= invocationType;
			while (invTypeRoot.getDeclaringClass() != null) {
				invTypeRoot= invTypeRoot.getDeclaringClass();
			}
			if (Bindings.equalDeclarations(fTypeRoot, invTypeRoot)) {
				return true;
			}
		}

		if (! invocationpackage.equals(fieldPackage)) {
			return false;
		}

		return false;
	}

	public static IBinding[] resolveBindingsForTypes(ICompilationUnit cu, IJavaElement[] elements) {
		ASTParser parser= createParser(cu);
		return parser.createBindings(elements, null);
	}

	private static ASTParser createParser(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(cu.getJavaProject());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		return parser;
	}
}
