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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
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

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionHashOperation;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionLaunchConfigurationDelegate;


public class PotentialProgrammingProblemsFix extends AbstractFix {
	
	/** Name of the externalizable class */
	private static final String EXTERNALIZABLE_NAME= "java.io.Externalizable"; //$NON-NLS-1$
	
	/** Name of the serializable class */
	private static final String SERIALIZABLE_NAME= "java.io.Serializable"; //$NON-NLS-1$
	
	/** The name of the serial version field */
	private static final String NAME_FIELD= "serialVersionUID"; //$NON-NLS-1$

	/** The default serial value */
	private static final long SERIAL_VALUE= 1;
	
	public interface ISerialVersionFixContext {
		public long getSerialVersionId(String qualifiedName) throws CoreException;
	}
	
	private static class SerialVersionHashContext implements ISerialVersionFixContext {
		
		private final IJavaProject fProject;
		private final String[] fQualifiedNames;
		private Hashtable fIdsTable;

		public SerialVersionHashContext(IJavaProject project, String[] qualifiedNames) {
			fProject= project;
			fQualifiedNames= qualifiedNames;
		}
		
		public void initialize(IProgressMonitor monitor) throws CoreException, IOException {
			fIdsTable= new Hashtable();
			if (fQualifiedNames.length > 0) {
				long[] ids= SerialVersionHashOperation.calculateSerialVersionIds(fQualifiedNames, fProject, monitor);
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				
				if (ids.length != fQualifiedNames.length) {
					for (int i= 0; i < fQualifiedNames.length; i++) {
						fIdsTable.put(fQualifiedNames[i], new Long(SERIAL_VALUE));
					}
					return;
				}
					
				for (int i= 0; i < ids.length; i++) {
					long id= ids[i];
					if (id != SerialVersionLaunchConfigurationDelegate.FAILING_ID)
						fIdsTable.put(fQualifiedNames[i], new Long(id));
				}
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		public long getSerialVersionId(String qualifiedName) throws CoreException {
			if (fIdsTable == null)
				throw new CoreException(new Status(IStatus.ERROR,  JavaPlugin.getPluginId(), 0, FixMessages.Java50Fix_SerialVersionNotInitialized_exception_description, null));
			
			Long id= (Long)fIdsTable.get(qualifiedName);
			
			if (id == null) {
				try {
					long[] ids= SerialVersionHashOperation.calculateSerialVersionIds(new String[] {qualifiedName}, fProject, new NullProgressMonitor());
					if (ids.length == 0)
						throw new CoreException(new Status(IStatus.ERROR,  JavaPlugin.getPluginId(), 0, Messages.format(FixMessages.Java50Fix_SerialVersionNotFound_exception_description, qualifiedName), null));
					
					fIdsTable.put(qualifiedName, new Long(ids[0]));
					return ids[0];
				} catch (CoreException e) {
					throw new CoreException(new Status(IStatus.ERROR,  JavaPlugin.getPluginId(), 0, Messages.format(FixMessages.Java50Fix_SerialVersionNotFound_exception_description, qualifiedName), e));
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR,  JavaPlugin.getPluginId(), 0, Messages.format(FixMessages.Java50Fix_SerialVersionNotFound_exception_description, qualifiedName), e));
				}
			}
				
			return id.longValue();
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
			long id= fContext.getSerialVersionId(getQualifiedName(declarationNode));
			if (id == SerialVersionLaunchConfigurationDelegate.FAILING_ID)
				return false;
			
			fragment.setInitializer(fragment.getAST().newNumberLiteral(id + LONG_SUFFIX));
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		protected void addLinkedPositions(ASTRewrite rewrite, VariableDeclarationFragment fragment, List positionGroups) {
			//Do nothing
		}
		
	}

	public static IFix[] createMissingSerialVersionFixes(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (problem.getProblemId() != IProblem.MissingSerialVersion)
			return null;
		
		final ICompilationUnit unit= (ICompilationUnit)compilationUnit.getJavaElement();
		if (unit == null)
			return null;
		
		final SimpleName simpleName= getSimpleTypeName(compilationUnit, problem);
		if (simpleName == null)
			return null;
		
		SerialVersionDefaultOperation defop= new SerialVersionDefaultOperation(unit, new SimpleName[] {simpleName});
		IFix fix1= new PotentialProgrammingProblemsFix(FixMessages.Java50Fix_SerialVersion_default_description, compilationUnit, new IFixRewriteOperation[] {defop});
		
		SerialVersionHashOperation hashop= new SerialVersionHashOperation(unit, new SimpleName[] {simpleName});
		IFix fix2= new PotentialProgrammingProblemsFix(FixMessages.Java50Fix_SerialVersion_hash_description, compilationUnit, new IFixRewriteOperation[] {hashop});
	
		return new IFix[] {fix1, fix2};
	}
	
	private static SerialVersionHashBatchOperation createSerialVersionHashOperation(CompilationUnit compilationUnit, IProblemLocation[] problems, ISerialVersionFixContext context) {
		final ICompilationUnit unit= (ICompilationUnit)compilationUnit.getJavaElement();
		if (unit == null)
			return null;
		
		List simpleNames= new ArrayList();
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].getProblemId() == IProblem.MissingSerialVersion) {
				final SimpleName simpleName= getSimpleTypeName(compilationUnit, problems[i]);
				if (simpleName != null) {
					simpleNames.add(simpleName);
				}
			}
		}
		if (simpleNames.size() == 0)
			return null;
		
		return new SerialVersionHashBatchOperation(unit, (SimpleName[])simpleNames.toArray(new SimpleName[simpleNames.size()]), context);
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, 
			boolean addSerialVersionIds, ISerialVersionFixContext context) {
		
		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		return createCleanUp(compilationUnit, locations, addSerialVersionIds, context);
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, 
			boolean addSerialVersionIds, ISerialVersionFixContext context) {
		
		List operations= new ArrayList();
		if (addSerialVersionIds) {
			IFixRewriteOperation operation= PotentialProgrammingProblemsFix.createSerialVersionHashOperation(compilationUnit, problems, context);
			if (operation != null)
				operations.add(operation);
		}
		
		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new PotentialProgrammingProblemsFix("", compilationUnit, ops);			 //$NON-NLS-1$
	}
	
	public static SerialVersionHashContext createSerialVersionHashContext(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", compilationUnits.length * 2 + 20); //$NON-NLS-1$
			
			List qualifiedClassNames= new ArrayList();
			
			if (compilationUnits.length > 500) {
				//500 is a guess. Building the type hierarchy on serializable is very expensive
				//depending on how many subtypes exit in the project. Finding out how many
				//suptypes exist would be as expensive as finding the subtypes...
				findWithTypeHierarchy(project, compilationUnits, qualifiedClassNames, monitor);
			} else {
				findWithRecursion(project, compilationUnits, qualifiedClassNames, monitor);
			}
		
			SerialVersionHashContext result= new SerialVersionHashContext(project, (String[])qualifiedClassNames.toArray(new String[qualifiedClassNames.size()]));
			try {
				result.initialize(new SubProgressMonitor(monitor, 20));
			} catch (IOException e) {
				JavaPlugin.log(e);
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	private static void findWithRecursion(IJavaProject project, ICompilationUnit[] compilationUnits, List qualifiedClassNames, IProgressMonitor monitor) throws JavaModelException {
		IType serializable= project.findType(SERIALIZABLE_NAME);
		IType externalizable= project.findType(EXTERNALIZABLE_NAME);
		
		for (int i= 0; i < compilationUnits.length; i++) {
			monitor.subTask(Messages.format(FixMessages.Java50Fix_InitializeSerialVersionId_subtask_description, new Object[] {project.getElementName(), compilationUnits[i].getElementName()}));
			findTypesWithoutSerialVersionId(compilationUnits[i].getChildren(), serializable, externalizable, qualifiedClassNames);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			monitor.worked(2);
		}
	}

	private static void findWithTypeHierarchy(IJavaProject project, ICompilationUnit[] compilationUnits, List qualifiedClassNames, IProgressMonitor monitor) throws JavaModelException {
		IType serializable= project.findType(SERIALIZABLE_NAME);
		IType externalizable= project.findType(EXTERNALIZABLE_NAME);
		
		HashSet cus= new HashSet();
		for (int i= 0; i < compilationUnits.length; i++) {
			cus.add(compilationUnits[i]);
		}
		
		monitor.subTask(Messages.format(FixMessages.Java50Fix_SerialVersion_CalculateHierarchy_description, SERIALIZABLE_NAME));
		ITypeHierarchy hierarchy1= serializable.newTypeHierarchy(project, new SubProgressMonitor(monitor, compilationUnits.length));
		IType[] allSubtypes1= hierarchy1.getAllSubtypes(serializable);
		addTypes(allSubtypes1, cus, qualifiedClassNames);

		monitor.subTask(Messages.format(FixMessages.Java50Fix_SerialVersion_CalculateHierarchy_description, EXTERNALIZABLE_NAME));
		ITypeHierarchy hierarchy2= externalizable.newTypeHierarchy(project, new SubProgressMonitor(monitor, compilationUnits.length));
		IType[] allSubtypes2= hierarchy2.getAllSubtypes(externalizable);
		addTypes(allSubtypes2, cus, qualifiedClassNames);
	}
	
	private static void addTypes(IType[] allSubtypes, HashSet cus, List qualifiedClassNames) throws JavaModelException {
		for (int i= 0; i < allSubtypes.length; i++) {
			IType type= allSubtypes[i];
			if (type.isClass() && cus.contains(type.getCompilationUnit())){
				IField field= type.getField(NAME_FIELD);
				if (!field.exists()) {
					qualifiedClassNames.add(type.getFullyQualifiedName());
				}
			}
		}
	}
	
	private static void findTypesWithoutSerialVersionId(IJavaElement[] children, IType serializable, IType externalizable, List/*<String>*/ qualifiedClassNames) throws JavaModelException {
		for (int i= 0; i < children.length; i++) {
			IJavaElement child= children[i];
			if (child instanceof IType) {
				IType type= (IType)child;
				ITypeHierarchy hierarchy= type.newSupertypeHierarchy(new NullProgressMonitor());
				IType[] allInterfaces= hierarchy.getAllSuperInterfaces(type);
				for (int j= 0; j < allInterfaces.length; j++) {
					if (allInterfaces[j].equals(serializable) || allInterfaces[j].equals(externalizable)) {
						IField field= type.getField(NAME_FIELD);
						if (!field.exists()) {
							qualifiedClassNames.add(type.getFullyQualifiedName());
						}
						break;
					}
				}

				findTypesWithoutSerialVersionId(type.getChildren(), serializable, externalizable, qualifiedClassNames);
			} else if (child instanceof IMethod) {
				IMethod method= (IMethod)child;
				findTypesWithoutSerialVersionId(method.getChildren(), serializable, externalizable, qualifiedClassNames);
			} else if (child instanceof IField) {
				IField field= (IField)child;
				findTypesWithoutSerialVersionId(field.getChildren(), serializable, externalizable, qualifiedClassNames);
			}
		}
	}
	
	private static SimpleName getSimpleTypeName(CompilationUnit compilationUnit, IProblemLocation problem) {
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
		
		final SimpleName result= name.isSimpleName() ? (SimpleName) name : ((QualifiedName) name).getName();
		
		return result;
	}

	protected PotentialProgrammingProblemsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
