/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * The {@link NewImportRewrite} helps updating imports following a import order and on-demand imports threshold as configured by a project.
 * <p>
 * The import rewrite is created on a compilation unit and collects references to types that are added or removed. When adding imports, e.g. using
 * {@link #addImport(String)}, the import rewrite evaluates if the type can be imported and returns the a reference to the type that can be used in code.
 * This reference is either unqualified if the import could be added, or fully qualified if the import failed due to a conflict with another element of the same name.
 * </p>
 * <p>
 * On {@link #rewriteImports(CompilationUnit, IProgressMonitor)} the rewrite translates these descriptions into text edits that can then be applied to
 * the original source. The rewrite infrastructure tries to generate minimal text changes and only works on the import statements. It is possible to
 * combine the result of an import rewrite with the result of a {@link org.eclipse.jdt.core.dom.rewrite.ASTRewrite} as long as no import statements
 * are modified by the AST rewrite.
 * </p>
 * 
 * This class is not intended to be subclassed.
 * </p>
 * @since 3.2
 */
public class NewImportRewrite {
	
	public static abstract class ImportRewriteContext {
		
		public final static int RES_NAME_FOUND= 1;
		public final static int RES_NAME_UNKNOWN= 2;
		public final static int RES_NAME_CONFLICT= 3;
		
		public final static int KIND_TYPE= 1;
		public final static int KIND_STATIC_FIELD= 2;
		public final static int KIND_STATIC_METHOD= 3;
		
		public abstract int findInContext(String qualifier, String name, int kind);
	}

	
	public static final String IMPORTS_ORDER= PreferenceConstants.ORGIMPORTS_IMPORTORDER;
	public static final String IMPORTS_ONDEMAND_THRESHOLD= PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD;
	
	private static final String ON_DEMAND_IMPORT_NAME= "*"; //$NON-NLS-1$
	
	private static final char STATIC_PREFIX= 's';
	private static final char NORMAL_PREFIX= 'n';
	
	private final ImportRewriteContext fDefaultContext;

	private List fAddedImports;
	private List fRemovedImports;
	
	private List fExistingImports;

	private final ICompilationUnit fCompilationUnit;

	private final boolean fRestoreExistingImports;

	private String[] fCreatedImports;
	private String[] fCreatedStaticImports;
	
	public static NewImportRewrite create(ICompilationUnit cu, boolean restoreExistingImports) throws JavaModelException {
		List existingImport= null;
		if (restoreExistingImports) {
			existingImport= new ArrayList();
			IImportDeclaration[] imports= cu.getImports();
			for (int i= 0; i < imports.length; i++) {
				IImportDeclaration curr= imports[i];
				char prefix= Flags.isStatic(curr.getFlags()) ? STATIC_PREFIX : NORMAL_PREFIX;			
				existingImport.add(prefix + curr.getElementName());
			}
		}
		return new NewImportRewrite(cu, existingImport);
	}
	
	public static NewImportRewrite create(CompilationUnit astRoot, boolean restoreExistingImports) throws JavaModelException {
		if (!(astRoot.getJavaElement() instanceof ICompilationUnit)) {
			throw new IllegalArgumentException("AST must have been constructed from a Java element"); //$NON-NLS-1$
		}
		List existingImport= null;
		if (restoreExistingImports) {
			existingImport= new ArrayList();
			List imports= astRoot.imports();
			for (int i= 0; i < imports.size(); i++) {
				ImportDeclaration curr= (ImportDeclaration) imports.get(i);
				StringBuffer buf= new StringBuffer();
				buf.append(curr.isStatic() ? STATIC_PREFIX : NORMAL_PREFIX).append(curr.getName().getFullyQualifiedName());
				if (curr.isOnDemand()) {
					if (buf.length() > 1)
						buf.append('.');
					buf.append('*');
				}
				existingImport.add(buf.toString());
			}
		}
		return new NewImportRewrite((ICompilationUnit) astRoot.getJavaElement(), existingImport);
	}
		
	private NewImportRewrite(ICompilationUnit cu, List existingImports) {
		fCompilationUnit= cu;
		fRestoreExistingImports= existingImports != null;

		fDefaultContext= new ImportRewriteContext() {
			public int findInContext(String qualifier, String name, int kind) {
				return findInImports(qualifier, name, kind);
			}
		};
		fAddedImports= null; // Initialized on use
		fRemovedImports= null; // Initialized on use
		fExistingImports= fRestoreExistingImports ? existingImports : new ArrayList();
	}
	
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	private static int compareImport(char prefix, String qualifier, String name, String curr) {
		if (curr.charAt(0) != prefix || !curr.endsWith(name)) {
			return ImportRewriteContext.RES_NAME_UNKNOWN;
		}
		
		curr= curr.substring(1); // remove the prefix
		
		if (curr.length() == name.length()) {
			if (qualifier.length() == 0) {
				return ImportRewriteContext.RES_NAME_FOUND;
			}
			return ImportRewriteContext.RES_NAME_CONFLICT; 
		}
		// at this place: curr.length > name.length
		
		int dotPos= curr.length() - name.length();
		if (curr.charAt(dotPos) != '.') {
			return ImportRewriteContext.RES_NAME_UNKNOWN;
		}
		if (qualifier.length() != dotPos - 1 || !curr.startsWith(qualifier)) {
			return ImportRewriteContext.RES_NAME_CONFLICT; 
		}
		return ImportRewriteContext.RES_NAME_FOUND; 
	}
	
	
	protected final int findInImports(String qualifier, String name, int kind) {
		boolean allowAmbiguity=  (kind == ImportRewriteContext.KIND_STATIC_METHOD);
		List imports= fExistingImports;
		char prefix= (kind == ImportRewriteContext.KIND_TYPE) ? NORMAL_PREFIX : STATIC_PREFIX;
		
		for (int i= imports.size() - 1; i >= 0 ; i--) {
			String curr= (String) imports.get(i);
			int res= compareImport(prefix, qualifier, name, curr);
			if (res != ImportRewriteContext.RES_NAME_UNKNOWN) {
				if (!allowAmbiguity || res == ImportRewriteContext.RES_NAME_FOUND) {
					return res;
				}
			}
		}
		return ImportRewriteContext.RES_NAME_UNKNOWN;
	}

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param typeSig The type in signature notation
	 * @param ast The AST to create the type for
	 * @return Returns the a new AST node that is either simple if the import was successful or 
	 * fully qualified type name if the import could not be added due to a conflict. 
	 */
	public Type addImportFromSignature(String typeSig, AST ast) {
		return addImportFromSignature(typeSig, ast, fDefaultContext);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param typeSig The type in signature notation
	 * @param ast The AST to create the type for
	 * @return Returns the a new AST node that is either simple if the import was successful or 
	 * fully qualified type name if the import could not be added due to a conflict. 
	 */
	public Type addImportFromSignature(String typeSig, AST ast, ImportRewriteContext context) {	
		if (typeSig == null || typeSig.length() == 0) {
			throw new IllegalArgumentException("Invalid type signature: empty or null"); //$NON-NLS-1$
		}
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				return ast.newPrimitiveType(PrimitiveType.toCode(Signature.toString(typeSig)));
			case Signature.ARRAY_TYPE_SIGNATURE:
				Type elementType= addImportFromSignature(Signature.getElementType(typeSig), ast);
				return ast.newArrayType(elementType, Signature.getArrayCount(typeSig));
			case Signature.CLASS_TYPE_SIGNATURE:
				String erasureSig= Signature.getTypeErasure(typeSig);

				String erasureName= Signature.toString(erasureSig);
				if (erasureSig.charAt(0) == Signature.C_RESOLVED) {
					erasureName= internalAddImport(erasureName, context);
				}
				Type baseType= ast.newSimpleType(ast.newName(erasureName));
				String[] typeArguments= Signature.getTypeArguments(typeSig);
				if (typeArguments.length > 0) {
					ParameterizedType type= ast.newParameterizedType(baseType);
					List argNodes= type.typeArguments();
					for (int i= 0; i < typeArguments.length; i++) {
						argNodes.add(addImportFromSignature(typeArguments[i], ast));
					}
					return type;
				}
				return baseType;
			case Signature.TYPE_VARIABLE_SIGNATURE:
				return ast.newSimpleType(ast.newSimpleName(Signature.toString(typeSig)));
			case Signature.WILDCARD_TYPE_SIGNATURE:
				WildcardType wildcardType= ast.newWildcardType();
				char ch= typeSig.charAt(0);
				if (ch != Signature.C_STAR) {
					Type bound= addImportFromSignature(typeSig.substring(1), ast);
					wildcardType.setBound(bound, ch == Signature.C_EXTENDS);
				}
				return wildcardType;
			case Signature.CAPTURE_TYPE_SIGNATURE:
				return addImportFromSignature(typeSig.substring(1), ast);
			default:
				throw new IllegalArgumentException("Unknown type signature kind: " + typeSig); //$NON-NLS-1$
		}
	}
	

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param binding The type binding of the type to be added
	 * @return Returns the name to use in the code: Simple name if the import
	 * was added, fully qualified type name if the import could not be added due
	 * to a conflict. 
	 */
	public String addImport(ITypeBinding binding) {
		return addImport(binding, fDefaultContext);
	}
		
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param binding The type binding of the type to be added
	 * @return Returns the name to use in the code: Simple name if the import
	 * was added, fully qualified type name if the import could not be added due
	 * to a conflict. 
	 */
	public String addImport(ITypeBinding binding, ImportRewriteContext context) {
		if (binding.isPrimitive() || binding.isTypeVariable()) {
			return binding.getName();
		}
		
		ITypeBinding normalizedBinding= normalizeTypeBinding(binding);
		if (normalizedBinding == null) {
			return "invalid"; //$NON-NLS-1$
		}
		if (normalizedBinding.isWildcardType()) {
			StringBuffer res= new StringBuffer("?"); //$NON-NLS-1$
			ITypeBinding bound= normalizedBinding.getBound();
			if (bound != null && !bound.isWildcardType() && !bound.isCapture()) { // bug 95942
				if (normalizedBinding.isUpperbound()) {
					res.append(" extends "); //$NON-NLS-1$
				} else {
					res.append(" super "); //$NON-NLS-1$
				}
				res.append(addImport(bound));
			}
			return res.toString();
		}
		
		if (normalizedBinding.isArray()) {
			StringBuffer res= new StringBuffer(addImport(normalizedBinding.getElementType()));
			for (int i= normalizedBinding.getDimensions(); i > 0; i--) {
				res.append("[]"); //$NON-NLS-1$
			}
			return res.toString();
		}
	
		String qualifiedName= getRawQualifiedName(normalizedBinding);
		if (qualifiedName.length() > 0) {
			String str= internalAddImport(qualifiedName, context);
			
			ITypeBinding[] typeArguments= normalizedBinding.getTypeArguments();
			if (typeArguments.length > 0) {
				StringBuffer res= new StringBuffer(str);
				res.append('<');
				for (int i= 0; i < typeArguments.length; i++) {
					if (i > 0) {
						res.append(','); 
					}
					res.append(addImport(typeArguments[i]));
				}
				res.append('>');
				return res.toString();
			}
			return str;
		}
		return getRawName(normalizedBinding);
	}
	
	private static ITypeBinding normalizeTypeBinding(ITypeBinding binding) {
		if (binding != null && !binding.isNullType() && !"void".equals(binding.getName())) { //$NON-NLS-1$
			if (binding.isAnonymous()) {
				ITypeBinding[] baseBindings= binding.getInterfaces();
				if (baseBindings.length > 0) {
					return baseBindings[0];
				}
				return binding.getSuperclass();
			}
			if (binding.isCapture()) {
				return binding.getWildcard();
			}
			return binding;
		}
		return null;
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param binding The type binding of the type to be added
	 * @param ast The AST to create the type for
	 * @return Returns the a new AST node that is either simple if the import was successful or 
	 * fully qualified type name if the import could not be added due to a conflict. 
	 */
	public Type addImport(ITypeBinding binding, AST ast) {
		return addImport(binding, ast, fDefaultContext);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param binding The type binding of the type to be added
	 * @param ast The AST to create the type for
	 * @return Returns the a new AST node that is either simple if the import was successful or 
	 * fully qualified type name if the import could not be added due to a conflict. 
	 */
	public Type addImport(ITypeBinding binding, AST ast, ImportRewriteContext context) {
		if (binding.isPrimitive()) {
			return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
		}
		
		ITypeBinding normalizedBinding= normalizeTypeBinding(binding);
		if (normalizedBinding == null) {
			return ast.newSimpleType(ast.newSimpleName("invalid")); //$NON-NLS-1$
		}
		
		if (normalizedBinding.isTypeVariable()) {
			// no import
			return ast.newSimpleType(ast.newSimpleName(binding.getName()));
		}
		if (normalizedBinding.isWildcardType()) {
			WildcardType wcType= ast.newWildcardType();
			ITypeBinding bound= normalizedBinding.getBound();
			if (bound != null && !bound.isWildcardType() && !bound.isCapture()) { // bug 96942
				Type boundType= addImport(bound, ast);
				wcType.setBound(boundType, normalizedBinding.isUpperbound());
			}
			return wcType;
		}
		
		if (normalizedBinding.isArray()) {
			Type elementType= addImport(normalizedBinding.getElementType(), ast);
			return ast.newArrayType(elementType, normalizedBinding.getDimensions());
		}
		
		String qualifiedName= getRawQualifiedName(normalizedBinding);
		if (qualifiedName.length() > 0) {
			String res= internalAddImport(qualifiedName, context);
			
			ITypeBinding[] typeArguments= normalizedBinding.getTypeArguments();
			if (typeArguments.length > 0) {
				Type erasureType= ast.newSimpleType(ast.newName(res));
				ParameterizedType paramType= ast.newParameterizedType(erasureType);
				List arguments= paramType.typeArguments();
				for (int i= 0; i < typeArguments.length; i++) {
					arguments.add(addImport(typeArguments[i], ast));
				}
				return paramType;
			}
			return ast.newSimpleType(ast.newName(res));
		}
		return ast.newSimpleType(ast.newName(getRawName(normalizedBinding)));
	}

	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 * @return Returns either the simple type name if the import was successful or else the qualified type name
	 */
	public String addImport(String qualifiedTypeName, ImportRewriteContext context) {
		int angleBracketOffset= qualifiedTypeName.indexOf('<');
		if (angleBracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, angleBracketOffset), context) + qualifiedTypeName.substring(angleBracketOffset);
		}
		int bracketOffset= qualifiedTypeName.indexOf('[');
		if (bracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, bracketOffset), context) + qualifiedTypeName.substring(bracketOffset);
		}
		return internalAddImport(qualifiedTypeName, context);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 * @return Returns either the simple type name if the import was successful or else the qualified type name
	 */
	public String addImport(String qualifiedTypeName) {
		return addImport(qualifiedTypeName, fDefaultContext);
	}
	
	
	public String addStaticImport(IBinding binding) {
		if (binding instanceof IVariableBinding) {
			ITypeBinding declaringType= ((IVariableBinding) binding).getDeclaringClass();
			return addStaticImport(getRawQualifiedName(declaringType), binding.getName(), true);
		} else if (binding instanceof IMethodBinding) {
			ITypeBinding declaringType= ((IMethodBinding) binding).getDeclaringClass();
			return addStaticImport(getRawQualifiedName(declaringType), binding.getName(), false);
		}
		return binding.getName();
	}
	
	/**
	 * Adds a new static import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param declaringTypeName The qualified name of the static's member declaring type
	 * @return Returns either the simple type name if the import was successful or else the qualified type name
	 */
	public String addStaticImport(String declaringTypeName, String simpleName, boolean isField) {
		String containerName= Signature.getQualifier(declaringTypeName);
		String fullName= declaringTypeName + '.' + simpleName;
		
		if (containerName.length() == 0) {
			return declaringTypeName + '.' + simpleName;
		}
		if (!ON_DEMAND_IMPORT_NAME.equals(simpleName)) {
			int kind= isField ? ImportRewriteContext.KIND_STATIC_FIELD : ImportRewriteContext.KIND_STATIC_METHOD;
			int res= fDefaultContext.findInContext(containerName, simpleName, kind);
			if (res == ImportRewriteContext.RES_NAME_CONFLICT) {
				return fullName;
			}
			if (res == ImportRewriteContext.RES_NAME_UNKNOWN) {
				addEntry(STATIC_PREFIX + fullName);
			}
		}
		return simpleName;
	}
	
	private String internalAddImport(String fullTypeName, ImportRewriteContext context) {
		int idx= fullTypeName.lastIndexOf('.');	
		String typeContainerName, typeName;
		if (idx != -1) {
			typeContainerName= fullTypeName.substring(0, idx);
			typeName= fullTypeName.substring(idx + 1);
		} else {
			typeContainerName= ""; //$NON-NLS-1$
			typeName= fullTypeName;
		}
		
		if (typeContainerName.length() == 0 && PrimitiveType.toCode(typeName) != null) {
			return fullTypeName;
		}
		
		if (!ON_DEMAND_IMPORT_NAME.equals(typeName)) {
			int res= context.findInContext(typeContainerName, typeName, ImportRewriteContext.KIND_TYPE);
			if (res == ImportRewriteContext.RES_NAME_CONFLICT) {
				return typeContainerName;
			}
			if (res == ImportRewriteContext.RES_NAME_UNKNOWN) {
				addEntry(NORMAL_PREFIX + fullTypeName);
			}
		}
		return typeName;
	}
	
	private void addEntry(String entry) {
		fExistingImports.add(entry);
		
		if (fRemovedImports != null) {
			if (fRemovedImports.remove(entry)) {
				return;
			}
		}
		
		if (fAddedImports == null) {
			fAddedImports= new ArrayList();
		}
		fAddedImports.add(entry);
	}
	
	private void removeEntry(String entry) {
		if (fExistingImports.remove(entry)) {
			if (fAddedImports != null) {
				if (fAddedImports.remove(entry)) {
					return;
				}
			}
			if (fRemovedImports == null) {
				fRemovedImports= new ArrayList();
			}
			fRemovedImports.add(entry);
		}
	}
	

	
	/**
	 * Removes an import from the structure.
	 * @param qualifiedName The qualified type name to remove from the imports
	 */
	public void removeImport(String qualifiedName) {
		removeEntry(NORMAL_PREFIX + qualifiedName);
	}
	
	/**
	 * Removes an import from the structure.
	 * @param qualifiedName The qualified member name to remove from the imports
	 */
	public void removeStaticImport(String qualifiedName) {
		removeEntry(STATIC_PREFIX + qualifiedName);
	}
	
	
	/**
	 * Removes an import from the structure.
	 * @param binding The type to remove from the imports
	 */
	public void removeImport(ITypeBinding binding) {
		binding= normalizeTypeBinding(binding);
		if (binding == null) {
			return;
		}		
		String qualifiedName= getRawQualifiedName(binding);
		if (qualifiedName.length() > 0) {
			removeImport(qualifiedName);
		}
	}
		
	
	private static String getRawName(ITypeBinding normalizedBinding) {
		return normalizedBinding.getTypeDeclaration().getName();
	}


	private static String getRawQualifiedName(ITypeBinding normalizedBinding) {
		return normalizedBinding.getTypeDeclaration().getQualifiedName();
	}
	
	
	public final TextEdit rewriteImports(CompilationUnit root, IProgressMonitor monitor) throws CoreException {
		if (!fCompilationUnit.equals(root.getJavaElement())) {
			throw new IllegalArgumentException("AST is not from the compilation unit that was used when creating the rewriter."); //$NON-NLS-1$
		}
		
		IJavaProject project= fCompilationUnit.getJavaProject();
		String[] order= getImportOrderPreference(project);
		int threshold= getImportNumberThreshold(project);
		
		ImportRewriteComputer computer= new ImportRewriteComputer(fCompilationUnit, root, order, threshold, fRestoreExistingImports);
		
		if (fAddedImports != null) {
			for (int i= 0; i < fAddedImports.size(); i++) {
				String curr= (String) fAddedImports.get(i);
				computer.addImport(curr.substring(1), STATIC_PREFIX == curr.charAt(0));
			}
		}
		
		if (fRemovedImports != null) {
			for (int i= 0; i < fRemovedImports.size(); i++) {
				String curr= (String) fRemovedImports.get(i);
				computer.removeImport(curr.substring(1), STATIC_PREFIX == curr.charAt(0));
			}
		}
			
		TextEdit result= computer.getResultingEdits(monitor);
		fCreatedImports= computer.getCreatedImports();
		fCreatedStaticImports= computer.getCreatedStaticImports();
		return result;
	}
	
	public String[] getCreatedImports() {
		return fCreatedImports;
	}
	
	public String[] getCreatedStaticImports() {
		return fCreatedStaticImports;
	}
	
	
	private static int getImportNumberThreshold(IJavaProject project) {
		Object threshold= PreferenceConstants.getPreference(IMPORTS_ONDEMAND_THRESHOLD, project);
		//Object threshold= project.getOption(IMPORTS_ONDEMAND_THRESHOLD, true);

		if (threshold instanceof String) {
			try {
				return Integer.parseInt((String) threshold);
			} catch (NumberFormatException e) {		
			}
		}
		return 999;
	}
	
	private static String[] getImportOrderPreference(IJavaProject project) {
		Object order= PreferenceConstants.getPreference(IMPORTS_ORDER, project);
		//Object threshold= project.getOption(IMPORTS_ORDER, true);

		if (order instanceof String) {
			return ((String) order).split(String.valueOf(';'));
		}
		return new String[0];
	}
		
}
