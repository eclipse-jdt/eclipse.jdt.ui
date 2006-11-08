/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionHashOperation;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionLaunchConfigurationDelegate;


public class PotentialProgrammingProblemsFix extends LinkedFix {
	
	/** Name of the serializable class */
	private static final String SERIALIZABLE_NAME= "java.io.Serializable"; //$NON-NLS-1$
	
	/** The name of the serial version field */
	private static final String NAME_FIELD= "serialVersionUID"; //$NON-NLS-1$
	
	private interface ISerialVersionFixContext {
		public RefactoringStatus initialize(IProgressMonitor monitor) throws CoreException;
		public boolean hasSerialVersionId(String qualifiedName);
		public long getSerialVersionId(String qualifiedName);
	}
	
	private static class SerialVersionHashContext implements ISerialVersionFixContext {
		
		private final IJavaProject fProject;
		private final ICompilationUnit[] fCompilationUnits;
		private final Hashtable fIdsTable;
		
		public SerialVersionHashContext(IJavaProject project, ICompilationUnit[] compilationUnits) {
			fProject= project;
			fCompilationUnits= compilationUnits;
			fIdsTable= new Hashtable();
        }

		public RefactoringStatus initialize(IProgressMonitor monitor) throws CoreException {
			if (monitor == null)
				monitor= new NullProgressMonitor();
			
			monitor.beginTask("", 3); //$NON-NLS-1$
			
			IType[] types= findTypesWithMissingUID(fProject, fCompilationUnits, new SubProgressMonitor(monitor, 1));
			if (types.length == 0)
				return new RefactoringStatus();
			
			RefactoringStatus result= new RefactoringStatus();
			
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(fProject);
			IBinding[] bindings= parser.createBindings(types, new SubProgressMonitor(monitor, 1));
			
			List qualifiedNames= new ArrayList();
			for (int i= 0; i < bindings.length; i++) {
	            ITypeBinding binding= (ITypeBinding)bindings[i];
	            if (binding != null && binding.getBinaryName() != null) {
					qualifiedNames.add(binding.getBinaryName());
	            } else {
					final IType type= types[i];
	            	result.addWarning(Messages.format(FixMessages.PotentialProgrammingProblemsFix_calculatingUIDFailed_binding, types[i].getFullyQualifiedName()), new RefactoringStatusContext() {
						public Object getCorrespondingElement() {
	                        return type;
                        }
	            	});
	            }
            }
			
			if (qualifiedNames.size() == 0)
				return result;
			
            try {
                String[] names= (String[])qualifiedNames.toArray(new String[qualifiedNames.size()]);
				long[] ids= SerialVersionHashOperation.calculateSerialVersionIds(names, fProject, new SubProgressMonitor(monitor, 1));
				
				for (int i= 0; i < ids.length; i++) {
                    if (ids[i] != SerialVersionLaunchConfigurationDelegate.FAILING_ID) {
                    	fIdsTable.put(names[i], new Long(ids[i]));
                    } else {
                    	result.addWarning(Messages.format(FixMessages.PotentialProgrammingProblemsFix_calculatingUIDFailed_unknown, names[i]));
                    }
                }
            } catch (IOException e) {
            	return createWarning(e);
            } catch (CoreException ce) {
            	return createWarning(ce);
            }
		
			return result;
		}
		
		private RefactoringStatus createWarning(Exception e) {
			RefactoringStatus result= new RefactoringStatus();
			result.addWarning(Messages.format(FixMessages.PotentialProgrammingProblemsFix_calculatingUIDFailed_exception, new String[] {fProject.getElementName(), e.getLocalizedMessage()}), new RefactoringStatusContext() {
				public Object getCorrespondingElement() {
                    return fProject;
                }
			});
			return result;
        }

		public boolean hasSerialVersionId(String qualifiedName) {
			if (qualifiedName == null)
				return false;
			
			Long id= (Long)fIdsTable.get(qualifiedName);
			if (id == null)
				return false;
			
			return true;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public long getSerialVersionId(String qualifiedName) {
			return ((Long)fIdsTable.get(qualifiedName)).longValue();
		}
		
		private IType[] findTypesWithMissingUID(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
			try {
				monitor.beginTask("", compilationUnits.length); //$NON-NLS-1$
				
				IType serializable= project.findType(SERIALIZABLE_NAME);
				
				List types= new ArrayList();
				
				if (compilationUnits.length > 500) {
					//500 is a guess. Building the type hierarchy on serializable is very expensive
					//depending on how many subtypes exit in the project.
					
					HashSet cus= new HashSet();
					for (int i= 0; i < compilationUnits.length; i++) {
						cus.add(compilationUnits[i]);
					}
					
					monitor.subTask(Messages.format(FixMessages.Java50Fix_SerialVersion_CalculateHierarchy_description, SERIALIZABLE_NAME));
					ITypeHierarchy hierarchy1= serializable.newTypeHierarchy(project, new SubProgressMonitor(monitor, compilationUnits.length));
					IType[] allSubtypes1= hierarchy1.getAllSubtypes(serializable);
					addTypes(allSubtypes1, cus, types);
				} else {
					monitor.subTask(FixMessages.Java50Fix_InitializeSerialVersionId_subtask_description);
                    for (int i= 0; i < compilationUnits.length; i++) {
                    	collectChildrenWithMissingSerialVersionId(compilationUnits[i].getChildren(), serializable, types);
                    	if (monitor.isCanceled())
                    		throw new OperationCanceledException();
                    	monitor.worked(1);
                    }
				}
				
				return (IType[])types.toArray(new IType[types.size()]);
			} finally {
				monitor.done();
			}
		}
		
		private void addTypes(IType[] allSubtypes, HashSet cus, List types) throws JavaModelException {
			for (int i= 0; i < allSubtypes.length; i++) {
				IType type= allSubtypes[i];

				IField field= type.getField(NAME_FIELD);
				if (!field.exists()) {
					if (type.isClass() && cus.contains(type.getCompilationUnit())){
						types.add(type);
					}
				}
			}
		}
		
		private void collectChildrenWithMissingSerialVersionId(IJavaElement[] children, IType serializable, List result) throws JavaModelException {
			for (int i= 0; i < children.length; i++) {
				IJavaElement child= children[i];
				if (child instanceof IType) {
					IType type= (IType)child;
					
					IField field= type.getField(NAME_FIELD);
					if (!field.exists()) {
						ITypeHierarchy hierarchy= type.newSupertypeHierarchy(new NullProgressMonitor());
						IType[] interfaces= hierarchy.getAllSuperInterfaces(type);
						for (int j= 0; j < interfaces.length; j++) {
							if (interfaces[j].equals(serializable)) {
								result.add(type);
								break;
							}
						}
					}

					collectChildrenWithMissingSerialVersionId(type.getChildren(), serializable, result);
				} else if (child instanceof IMethod) {
					collectChildrenWithMissingSerialVersionId(((IMethod)child).getChildren(), serializable, result);
				} else if (child instanceof IField) {
					collectChildrenWithMissingSerialVersionId(((IField)child).getChildren(), serializable, result);
				}
			}
		}
	}
	
	private static class SerialVersionHashBatchOperation extends AbstractSerialVersionOperation {

		private final ISerialVersionFixContext fContext;

		protected SerialVersionHashBatchOperation(ICompilationUnit unit, ASTNode[] node, ISerialVersionFixContext context) {
			super(unit, node);
			fContext= context;
		}

		/**
		 * {@inheritDoc}
		 */
		protected boolean addInitializer(VariableDeclarationFragment fragment, ASTNode declarationNode) throws CoreException {
			String qualifiedName= getQualifiedName(declarationNode);
			if (!fContext.hasSerialVersionId(qualifiedName))
				return false;
			
			long id= fContext.getSerialVersionId(qualifiedName);
			fragment.setInitializer(fragment.getAST().newNumberLiteral(id + LONG_SUFFIX));
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		protected void addLinkedPositions(ASTRewrite rewrite, VariableDeclarationFragment fragment, LinkedProposalModel positionGroups) {}
		
	}

	private static ISerialVersionFixContext fCurrentContext;

	public static IFix[] createMissingSerialVersionFixes(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (problem.getProblemId() != IProblem.MissingSerialVersion)
			return null;
		
		final ICompilationUnit unit= (ICompilationUnit)compilationUnit.getJavaElement();
		if (unit == null)
			return null;
		
		final SimpleName simpleName= getSelectedName(compilationUnit, problem);
		if (simpleName == null)
			return null;
		
		ASTNode declaringNode= getDeclarationNode(simpleName);
		if (declaringNode == null)
			return null;
		
		SerialVersionDefaultOperation defop= new SerialVersionDefaultOperation(unit, new ASTNode[] {declaringNode});
		IFix fix1= new PotentialProgrammingProblemsFix(FixMessages.Java50Fix_SerialVersion_default_description, compilationUnit, new IFixRewriteOperation[] {defop});
		
		SerialVersionHashOperation hashop= new SerialVersionHashOperation(unit, new ASTNode[] {declaringNode});
		IFix fix2= new PotentialProgrammingProblemsFix(FixMessages.Java50Fix_SerialVersion_hash_description, compilationUnit, new IFixRewriteOperation[] {hashop});
	
		return new IFix[] {fix1, fix2};
	}

	public static RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor, 
			boolean calculatedId, 
			boolean defaultId, 
			boolean randomId) throws CoreException {
		
		if (defaultId) {
			fCurrentContext= new ISerialVersionFixContext() {
				public long getSerialVersionId(String qualifiedName) {
					return 1;
				}
				public RefactoringStatus initialize(IProgressMonitor pm) throws CoreException {
	                return new RefactoringStatus();
                }
				public boolean hasSerialVersionId(String qualifiedName) {
	                return true;
                }
			};
			return fCurrentContext.initialize(monitor);
		} else if (randomId) {
			fCurrentContext= new ISerialVersionFixContext() {
				private Random rng;
				public long getSerialVersionId(String qualifiedName) {
					return rng.nextLong();
				}
				public RefactoringStatus initialize(IProgressMonitor pm) throws CoreException {
					rng= new Random((new Date()).getTime());
	                return new RefactoringStatus();
                }
				public boolean hasSerialVersionId(String qualifiedName) {
	                return true;
                }
			};
			return fCurrentContext.initialize(monitor);
		} else if (calculatedId) {
			fCurrentContext= new SerialVersionHashContext(project, compilationUnits);
			return fCurrentContext.initialize(monitor);
		} else {
			return new RefactoringStatus();
		}
    }
	
	public static RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		if (monitor != null)
			monitor.done();
		
		fCurrentContext= null;
	    return new RefactoringStatus();
    }
		
	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean addSerialVersionIds) {
		
		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		return createCleanUp(compilationUnit, locations, addSerialVersionIds);
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, boolean addSerialVersionIds) {
		if (addSerialVersionIds) {
			
			final ICompilationUnit unit= (ICompilationUnit)compilationUnit.getJavaElement();
			if (unit == null)
				return null;
			
			List declarationNodes= new ArrayList();
			for (int i= 0; i < problems.length; i++) {
				if (problems[i].getProblemId() == IProblem.MissingSerialVersion) {
					final SimpleName simpleName= getSelectedName(compilationUnit, problems[i]);
					if (simpleName != null) {
						ASTNode declarationNode= getDeclarationNode(simpleName);
						if (declarationNode != null) {
							declarationNodes.add(declarationNode);
						}
					}
				}
			}
			if (declarationNodes.size() == 0)
				return null;
			
			for (Iterator iter= declarationNodes.iterator(); iter.hasNext();) {
	            ASTNode declarationNode= (ASTNode)iter.next();
	            if (fCurrentContext.hasSerialVersionId(getQualifiedName(declarationNode))) {
	            	SerialVersionHashBatchOperation op= new SerialVersionHashBatchOperation(unit, (ASTNode[])declarationNodes.toArray(new ASTNode[declarationNodes.size()]), fCurrentContext);
	    			return new PotentialProgrammingProblemsFix(FixMessages.PotentialProgrammingProblemsFix_add_id_change_name, compilationUnit, new IFixRewriteOperation[] {op});	            	
	            }
            }
		}
		return null;
	}
	
	private static SimpleName getSelectedName(CompilationUnit compilationUnit, IProblemLocation problem) {
		final ASTNode selection= problem.getCoveredNode(compilationUnit);
		if (selection == null)
			return null;
		
		Name name= null;
		if (selection instanceof SimpleType) {
			final SimpleType type= (SimpleType) selection;
			name= type.getName();
		} else if (selection instanceof ParameterizedType) {
			final ParameterizedType type= (ParameterizedType) selection;
			final Type raw= type.getType();
			if (raw instanceof SimpleType)
				name= ((SimpleType) raw).getName();
			else if (raw instanceof QualifiedType)
				name= ((QualifiedType) raw).getName();
		} else if (selection instanceof Name) {
			name= (Name) selection;
		}
		if (name == null)
			return null;
		
		if (name.isSimpleName()) {
			return (SimpleName)name;
		} else {
			return ((QualifiedName)name).getName();
		}
	}
	
	/**
	 * Returns the declaration node for the originally selected node.
	 *
	 * @return the declaration node
	 */
	private static ASTNode getDeclarationNode(SimpleName name) {

		ASTNode parent= name.getParent();
		if (!(parent instanceof AbstractTypeDeclaration)) {

			parent= parent.getParent();
			if (parent instanceof ParameterizedType || parent instanceof Type)
				parent= parent.getParent();
			if (parent instanceof ClassInstanceCreation) {

				final ClassInstanceCreation creation= (ClassInstanceCreation) parent;
				parent= creation.getAnonymousClassDeclaration();
			}
		}
		return parent;
	}
	
	/**
	 * Returns the qualified type name of the class declaration.
	 * 
	 * @return the qualified type name of the class
	 */
	private static String getQualifiedName(final ASTNode parent) {
		ITypeBinding binding= null;
		if (parent instanceof AbstractTypeDeclaration) {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) parent;
			binding= declaration.resolveBinding();
		} else if (parent instanceof AnonymousClassDeclaration) {
			final AnonymousClassDeclaration declaration= (AnonymousClassDeclaration) parent;
			final ClassInstanceCreation creation= (ClassInstanceCreation) declaration.getParent();
			binding= creation.resolveTypeBinding();
		} else if (parent instanceof ParameterizedType) {
			final ParameterizedType type= (ParameterizedType) parent;
			binding= type.resolveBinding();
		}
		if (binding != null)
			return binding.getBinaryName();
		return null;
	}

	protected PotentialProgrammingProblemsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}