/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ModuleImportsCleanUpCore extends AbstractCleanUp {

	public ModuleImportsCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public ModuleImportsCleanUpCore() {
	}

	@Override
	public void setOptions(CleanUpOptions options) {
		// TODO Auto-generated method stub
		super.setOptions(options);
	}
	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), requireAST(), false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.USE_MODULE_IMPORTS);
	}

	public class CreateModuleImportsFix implements ICleanUpFix {

		private final CompilationUnit fCompilationUnit;
		private final List<IModuleBinding> fModuleImports;
		private final List<String> fNeededImports;

		public CreateModuleImportsFix(CompilationUnit cu, List<IModuleBinding> moduleImports, List<String> neededImports) {
			this.fCompilationUnit= cu;
			this.fModuleImports= moduleImports;
			this.fNeededImports= neededImports;
		}

		/**
		 * Used to ensure that unresolvable imports don't get reduced into on-demand imports.
		 */
		private static ImportRewriteContext UNRESOLVABLE_IMPORT_CONTEXT= new ImportRewriteContext() {
			@Override
			public int findInContext(String qualifier, String name, int kind) {
				return RES_NAME_UNKNOWN_NEEDS_EXPLICIT_IMPORT;
			}
		};

		@Override
		public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
			ImportRewrite importRewrite= ImportRewrite.create(fCompilationUnit, true);
			List<ImportDeclaration> importDecls= fCompilationUnit.imports();
			for (ImportDeclaration importDecl : importDecls) {
				if (Modifier.isModule(importDecl.getModifiers())) {
					boolean found= false;
					for (IModuleBinding moduleImport : fModuleImports) {
						if (moduleImport.getName().equals(importDecl.getName().getFullyQualifiedName())) {
							found= true;
							break;
						}
					}
					if (!found) {
						importRewrite.removeModuleImport(importDecl.getName().getFullyQualifiedName());
					}
				} else if (!importDecl.isStatic()) {
					if (importDecl.isOnDemand()) {
						for (IModuleBinding moduleImport : fModuleImports) {
							for (String pkgName : ImportRewrite.getPackageNamesForModule(moduleImport, fCompilationUnit.getJavaElement().getJavaProject())) {
								if (importDecl.getName().getFullyQualifiedName().equals(pkgName)) {
									importRewrite.removeImport(importDecl.getName().getFullyQualifiedName() + ".*"); //$NON-NLS-1$
								}
							}
						}
					} else if (!fNeededImports.contains(importDecl.getName().getFullyQualifiedName())) {
						importRewrite.removeImport(importDecl.getName().getFullyQualifiedName());
					}
				}
			}
			for (IModuleBinding moduleImport : fModuleImports) {
				importRewrite.addModuleImport(moduleImport.getName(), moduleImport);
			}
			for (String neededImport : fNeededImports) {
				importRewrite.addImport(neededImport, UNRESOLVABLE_IMPORT_CONTEXT);
			}
			CompilationUnitChange result= new CompilationUnitChange(MultiFixMessages.ModuleImportsCleanup_description, (ICompilationUnit)fCompilationUnit.getJavaElement());
			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);

			TextEdit importEdit= importRewrite.rewriteImports(progressMonitor);
			root.addChild(importEdit);
			result.addTextEditGroup(new TextEditGroup(
					MultiFixMessages.ModuleImportsCleanup_description,
					new TextEdit[] {importEdit}
				));
			return result;
		}

	}
	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null) {
			return null;
		}

		if (!isEnabled(CleanUpConstants.USE_MODULE_IMPORTS)) {
			return null;
		}

		if (!JavaModelUtil.is25OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		return createCleanUp(compilationUnit);
	}


	private ICleanUpFix createCleanUp(CompilationUnit compilationUnit) {
		if (compilationUnit != null) {
			if (compilationUnit.getPackage() == null) {
				return null;
			}
			IPackageBinding packageBinding= compilationUnit.getPackage().resolveBinding();
			if (packageBinding == null) {
				return null;
			}
			ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();
			IJavaProject project= cu.getJavaProject();
			IModuleBinding moduleBinding= packageBinding.getModule();
			if (moduleBinding == null) {
				return null;
			}
			final IModuleBinding[] requiredModules= moduleBinding.getRequiredModules();
			final List<IModuleBinding> moduleImports= new ArrayList<>();
			final List<String> neededImports= new ArrayList<>();
			final Map<String, List<IModuleBinding>> typeList= new HashMap<>();
			final Map<IModuleBinding, Set<String>> conflictList= new HashMap<>();
			ASTVisitor typeVisitor= new ASTVisitor() {
				@Override
				public boolean visit(SimpleName node) {
					if (node.getParent().getNodeType() != ASTNode.QUALIFIED_NAME &&
							node.getParent().getNodeType() != ASTNode.NAME_QUALIFIED_TYPE) {
						IBinding binding= node.resolveBinding();
						if (binding instanceof ITypeBinding typeBinding) {
							if (findTypeInModules(typeBinding) == ModuleTypeStatus.NOT_FOUND) {
								IPackageBinding pkgBinding= typeBinding.getPackage();
								if (pkgBinding != null
										&& !pkgBinding.getName().equals("java.lang") //$NON-NLS-1$
										&& !pkgBinding.getName().equals(packageBinding.getName())) {
									neededImports.add(typeBinding.getQualifiedName());
								}
							}
						} else if (binding == null) {
							throw new AbortSearchException();
						}
					}
					return false;
				}
				enum ModuleTypeStatus {
					NOT_FOUND,
					FOUND_NO_CONFLICT,
					FOUND_CONFLICT
				}
				private ModuleTypeStatus findTypeInModules(ITypeBinding binding) {
					ModuleTypeStatus result= ModuleTypeStatus.NOT_FOUND;
					for (IModuleBinding requiredModule : requiredModules) {
						for (IPackageBinding pkgBinding : ImportRewrite.getPackageBindingsForModule(requiredModule, project)) {
							if (pkgBinding.findTypeBinding(binding.getName()) != null) {
								if (!binding.getQualifiedName().startsWith(pkgBinding.getName())){
									result= ModuleTypeStatus.FOUND_CONFLICT;
									Set<String> importSet= conflictList.get(requiredModule);
									if (importSet == null) {
										importSet= new HashSet<>();
									}
									importSet.add(binding.getQualifiedName());
									conflictList.put(requiredModule, importSet);
								} else {
									if (result == ModuleTypeStatus.NOT_FOUND) {
										result= ModuleTypeStatus.FOUND_NO_CONFLICT;
									}
									List<IModuleBinding> modules= typeList.get(binding.getQualifiedName());
									if (modules == null) {
										modules= new ArrayList<>();
									}
									if (!modules.contains(requiredModule)) {
										modules.add(requiredModule);
									}
									typeList.put(binding.getQualifiedName(), modules);
								}
							}
						}
					}
					return result;
				}
			};
			try {
				compilationUnit.accept(typeVisitor);
			} catch (AbortSearchException e) {
				return null;
			}
			for (Entry<IModuleBinding, Set<String>> conflictEntry : conflictList.entrySet()) {
				int conflictNeededCount= 0;
				for (Entry<String, List<IModuleBinding>> typeListEntry : typeList.entrySet()) {
					if (typeListEntry.getValue().size() == 1 && typeListEntry.getValue().contains(conflictEntry.getKey())) {
						++conflictNeededCount;
					}
				}
				if (conflictNeededCount < conflictEntry.getValue().size()) {
					for (Entry<String, List<IModuleBinding>> typeListEntry2 : typeList.entrySet()) {
						typeListEntry2.getValue().remove(conflictEntry.getKey());
					}
				} else {
					for (String name : conflictEntry.getValue()) {
						neededImports.add(name);
					}
				}
			}
			for (String typeListName : typeList.keySet()) {
				if (!neededImports.contains(typeListName)) {
					if (!typeList.get(typeListName).isEmpty()) {
						moduleImports.add(typeList.get(typeListName).get(0));
					} else {
						neededImports.add(typeListName);
					}
				}
			}
			return new CreateModuleImportsFix(compilationUnit, moduleImports, neededImports);
		}
		return null;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.USE_MODULE_IMPORTS)) {
			result.add(MultiFixMessages.ModuleImportsCleanup_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (!isEnabled(CleanUpConstants.USE_MODULE_IMPORTS)) {
			return """
				import java.util.List;
				import java.util.ArrayList;
				"""; //$NON-NLS-1$
		}

		return """
				import module java.base;
				"""; //$NON-NLS-1$
	}

}
