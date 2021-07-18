/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.manipulation.ImportReferencesCollector;

import org.eclipse.jdt.internal.corext.dom.Bindings;

/**
 * Removes imports that are no longer required.
 * <p>
 * {@link #registerRemovedNode(ASTNode)} registers nodes that got removed from the AST.
 * Do not register nodes that are moved to a different place in the same AST.
 * <p>
 * If a node is removed but some parts of it are moved to a different place in the same AST,
 * then use {@link #registerRetainedNode(ASTNode)} to keep imports for the retained nodes.
 * <p>
 * Additional imports that will be added to the AST need to be registered with one of the
 * {@code register*Import*(..)} methods. Such imports have typically been created with
 * {@link ImportRewrite#addImport(ITypeBinding)} etc.
 */
public class ImportRemover {

	private static class StaticImportData {
		private boolean fField;

		private String fMember;

		private String fQualifier;

		private StaticImportData(String qualifier, String member, boolean field) {
			fQualifier= qualifier;
			fMember= member;
			fField= field;
		}
	}

	private static final String REMOVED= "removed"; //$NON-NLS-1$
	private static final String RETAINED= "retained"; //$NON-NLS-1$
	/**
	 * Namespace for the property key
	 */
	private static final String PROPERTY_KEY_CLASS_ID= "IR"; //$NON-NLS-1$
	/**
	 * Provides thread-safe uniqueness for the <code>propertyKey</code>
	 */
	private static final AtomicInteger PROPERTY_KEY_COUNTER = new AtomicInteger();

	private final String propertyKey;
	private final IJavaProject fProject;
	private final CompilationUnit fRoot;

	private Set<String> fAddedImports= new HashSet<>();
	private Set<StaticImportData> fAddedStaticImports= new HashSet<>();
	private boolean fHasRemovedNodes;
	private List<ImportDeclaration> fInlinedStaticImports= new ArrayList<>();

	public ImportRemover(IJavaProject project, CompilationUnit root) {
		fProject= project;
		fRoot= root;
		propertyKey= PROPERTY_KEY_CLASS_ID + PROPERTY_KEY_COUNTER.getAndIncrement();
	}

	private void divideTypeRefs(List<SimpleName> importNames, List<SimpleName> staticNames, List<SimpleName> removedRefs, List<SimpleName> unremovedRefs) {
		final List<int[]> removedStartsEnds= new ArrayList<>();
		fRoot.accept(new ASTVisitor(true) {
			int fRemovingStart= -1;
			@Override
			public void preVisit(ASTNode node) {
				Object property= node.getProperty(propertyKey);
				if (property == REMOVED) {
					if (fRemovingStart == -1) {
						fRemovingStart= node.getStartPosition();
					} else {
						/*
						 * Bug in client code: REMOVED node should not be nested inside another REMOVED node without
						 * an intermediate RETAINED node.
						 * Drop REMOVED property to prevent problems later (premature end of REMOVED section).
						 */
						node.setProperty(propertyKey, null);
					}
				} else if (property == RETAINED) {
					if (fRemovingStart != -1) {
						removedStartsEnds.add(new int[] { fRemovingStart, node.getStartPosition() });
						fRemovingStart= -1;
					} else {
						/*
						 * Bug in client code: RETAINED node should not be nested inside another RETAINED node without
						 * an intermediate REMOVED node and must have an enclosing REMOVED node.
						 * Drop RETAINED property to prevent problems later (premature restart of REMOVED section).
						 */
						node.setProperty(propertyKey, null);
					}
				}
				super.preVisit(node);
			}
			@Override
			public void postVisit(ASTNode node) {
				Object property= node.getProperty(propertyKey);
				if (property == RETAINED) {
					int end= node.getStartPosition() + node.getLength();
					fRemovingStart= end;
				} else if (property == REMOVED) {
					if (fRemovingStart != -1) {
						int end= node.getStartPosition() + node.getLength();
						removedStartsEnds.add(new int[] { fRemovingStart, end });
						fRemovingStart= -1;
					}
				}
				super.postVisit(node);
			}
		});

		for (SimpleName name : importNames) {
			if (isInRemoved(name, removedStartsEnds))
				removedRefs.add(name);
			else
				unremovedRefs.add(name);
		}
		for (SimpleName name : staticNames) {
			if (isInRemoved(name, removedStartsEnds))
				removedRefs.add(name);
			else
				unremovedRefs.add(name);
		}
		for (ImportDeclaration importDecl : fInlinedStaticImports) {
			Name name= importDecl.getName();
			if (name instanceof QualifiedName)
				name= ((QualifiedName) name).getName();
			removedRefs.add((SimpleName) name);
		}
	}

	private boolean isInRemoved(SimpleName ref, List<int[]> removedStartsEnds) {
		int start= ref.getStartPosition();
		int end= start + ref.getLength();
		for (int[] removedStartsEnd : removedStartsEnds) {
			if (start >= removedStartsEnd[0] && end <= removedStartsEnd[1]) {
				return true;
			}
		}
		return false;
	}

	public IBinding[] getImportsToRemove() {
		ArrayList<SimpleName> importNames= new ArrayList<>();
		ArrayList<SimpleName> staticNames= new ArrayList<>();

		ImportReferencesCollector.collect(fRoot, fProject, null, importNames, staticNames);

		List<SimpleName> removedRefs= new ArrayList<>();
		List<SimpleName> unremovedRefs= new ArrayList<>();
		divideTypeRefs(importNames, staticNames, removedRefs, unremovedRefs);
		if (removedRefs.isEmpty())
			return new IBinding[0];

		HashMap<String, IBinding> potentialRemoves= getPotentialRemoves(removedRefs);
		for (SimpleName name : unremovedRefs) {
			potentialRemoves.remove(name.getIdentifier());
		}

		Collection<IBinding> importsToRemove= potentialRemoves.values();
		return importsToRemove.toArray(new IBinding[importsToRemove.size()]);
	}

	private HashMap<String, IBinding> getPotentialRemoves(List<SimpleName> removedRefs) {
		HashMap<String, IBinding>potentialRemoves= new HashMap<>();
		for (SimpleName name : removedRefs) {
			if (fAddedImports.contains(name.getIdentifier()) || hasAddedStaticImport(name))
				continue;
			IBinding binding= name.resolveBinding();
			if (binding != null)
				potentialRemoves.put(name.getIdentifier(), binding);
		}
		return potentialRemoves;
	}

	private boolean hasAddedStaticImport(SimpleName name) {
		IBinding binding= name.resolveBinding();
		if (binding instanceof IVariableBinding) {
			IVariableBinding variable= (IVariableBinding) binding;
			return hasAddedStaticImport(variable.getDeclaringClass().getQualifiedName(), variable.getName(), true);
		} else if (binding instanceof IMethodBinding) {
			IMethodBinding method= (IMethodBinding) binding;
			return hasAddedStaticImport(method.getDeclaringClass().getQualifiedName(), method.getName(), false);
		}
		return false;
	}

	private boolean hasAddedStaticImport(String qualifier, String member, boolean field) {
		StaticImportData data= null;
		for (final Iterator<StaticImportData> iterator= fAddedStaticImports.iterator(); iterator.hasNext();) {
			data= iterator.next();
			if (data.fQualifier.equals(qualifier) && data.fMember.equals(member) && data.fField == field)
				return true;
		}
		return false;
	}

	public boolean hasRemovedNodes() {
		return fHasRemovedNodes || !fInlinedStaticImports.isEmpty();
	}

	public void registerAddedImport(String typeName) {
		int dot= typeName.lastIndexOf('.');
		if (dot == -1)
			fAddedImports.add(typeName);
		else
			fAddedImports.add(typeName.substring(dot + 1));
	}

	public void registerAddedImports(Type newTypeNode) {
		newTypeNode.accept(new ASTVisitor(true) {

			private void addName(SimpleName name) {
				fAddedImports.add(name.getIdentifier());
			}

			@Override
			public boolean visit(NameQualifiedType node) {
				addName(node.getName());
				return false;
			}

			@Override
			public boolean visit(QualifiedName node) {
				addName(node.getName());
				return false;
			}

			@Override
			public boolean visit(QualifiedType node) {
				addName(node.getName());
				return false;
			}

			@Override
			public boolean visit(SimpleName node) {
				addName(node);
				return false;
			}
		});
	}

	public void registerAddedStaticImport(String qualifier, String member, boolean field) {
		fAddedStaticImports.add(new StaticImportData(qualifier, member, field));
	}

	public void registerAddedStaticImport(IBinding binding) {
		if (binding instanceof IVariableBinding) {
			ITypeBinding declaringType= ((IVariableBinding) binding).getDeclaringClass();
			fAddedStaticImports.add(new StaticImportData(Bindings.getRawQualifiedName(declaringType), binding.getName(), true));

		} else if (binding instanceof IMethodBinding) {
			ITypeBinding declaringType= ((IMethodBinding) binding).getDeclaringClass();
			fAddedStaticImports.add(new StaticImportData(Bindings.getRawQualifiedName(declaringType), binding.getName(), false));

		} else {
			throw new IllegalArgumentException(binding.toString());
		}
	}

	public void registerRemovedNode(ASTNode removed) {
		fHasRemovedNodes= true;
		removed.setProperty(propertyKey, REMOVED);
	}

	public void registerRetainedNode(ASTNode retained) {
		retained.setProperty(propertyKey, RETAINED);
	}

	public void applyRemoves(ImportRewrite importRewrite) {
		for (IBinding b : getImportsToRemove()) {
			if (b instanceof ITypeBinding) {
				ITypeBinding typeBinding= (ITypeBinding) b;
				importRewrite.removeImport(typeBinding.getTypeDeclaration().getQualifiedName());
			} else if (b instanceof IMethodBinding) {
				IMethodBinding binding= (IMethodBinding) b;
				importRewrite.removeStaticImport(binding.getDeclaringClass().getQualifiedName() + '.' + binding.getName());
			} else if (b instanceof IVariableBinding) {
				IVariableBinding binding= (IVariableBinding) b;
				importRewrite.removeStaticImport(binding.getDeclaringClass().getQualifiedName() + '.' + binding.getName());
			}
		}
	}

	public void registerInlinedStaticImport(ImportDeclaration importDecl) {
		fInlinedStaticImports.add(importDecl);
	}
}
