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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IResourceMapper;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.IJavaElementMapper;
import org.eclipse.jdt.core.refactoring.RenameTypeArguments;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameResourceDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ISimilarDeclarationUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class RenameCompilationUnitProcessor extends JavaRenameProcessor implements IReferenceUpdating, ITextUpdating, IQualifiedNameUpdating, ISimilarDeclarationUpdating, IResourceMapper, IJavaElementMapper {
	
	private static final String ID_RENAME_COMPILATION_UNIT= "org.eclipse.jdt.ui.rename.compilationunit"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PATH= "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$
	
	private RenameTypeProcessor fRenameTypeProcessor;
	private boolean fWillRenameType;
	private ICompilationUnit fCu;

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameCompilationUnitProcessor"; //$NON-NLS-1$
	
	/**
	 * Creates a new rename compilation unit processor.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @throws CoreException
	 */
	public RenameCompilationUnitProcessor(ICompilationUnit unit) throws CoreException {
		fCu= unit;
		if (fCu != null) {
			computeRenameTypeRefactoring();
			setNewElementName(fCu.getElementName());
		}
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public boolean isApplicable() {
		return RefactoringAvailabilityTester.isRenameAvailable(fCu);
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.RenameCompilationUnitRefactoring_name;
	}

	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fCu);
	}

	public Object[] getElements() {
		return new Object[] {fCu};
	}

	protected RenameModifications computeRenameModifications() {
		RenameModifications result= new RenameModifications();
		result.rename(fCu, new RenameArguments(getNewElementName(), getUpdateReferences()));
		if (fRenameTypeProcessor != null) {
			String newTypeName= removeFileNameExtension(getNewElementName());
			RenameTypeArguments arguments= new RenameTypeArguments(newTypeName, getUpdateReferences(), getUpdateSimilarDeclarations(), getSimilarElements());
			result.rename(fRenameTypeProcessor.getType(), arguments, getUpdateSimilarDeclarations() 
				? new RenameTypeProcessor.ParticipantDescriptorFilter()
				: null);
		}
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		if (!fWillRenameType) {
			IFile file= ResourceUtil.getFile(fCu);
			if (file != null)
				return new IFile[] {file};
		}
		return new IFile[0];
	}
	
	//---- IRenameProcessor -------------------------------------
	
	public String getCurrentElementName() {
		return getSimpleCUName();
	}
	
	public String getCurrentElementQualifier() {
		IPackageFragment pack= (IPackageFragment) fCu.getParent();
		return pack.getElementName();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		String typeName= removeFileNameExtension(newName);
		RefactoringStatus result= Checks.checkCompilationUnitName(newName);
		if (fWillRenameType)
			result.merge(fRenameTypeProcessor.checkNewElementName(typeName));
		if (Checks.isAlreadyNamed(fCu, newName))
			result.addFatalError(RefactoringCoreMessages.RenameCompilationUnitRefactoring_same_name);	 
		return result;
	}
	
	public void setNewElementName(String newName) {
		super.setNewElementName(newName);
		if (fWillRenameType)
			fRenameTypeProcessor.setNewElementName(removeFileNameExtension(newName));
	}
	
	public Object getNewElement() {
		IJavaElement parent= fCu.getParent();
		if (parent.getElementType() != IJavaElement.PACKAGE_FRAGMENT)
			return fCu; //??
		IPackageFragment pack= (IPackageFragment)parent;
		if (JavaConventions.validateCompilationUnitName(getNewElementName()).getSeverity() == IStatus.ERROR)
			return fCu; //??
		return pack.getCompilationUnit(getNewElementName());
	}
	
	//---- ITextUpdating ---------------------------------------------
	
	public boolean canEnableTextUpdating() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.canEnableUpdateReferences();
	}

	public boolean getUpdateTextualMatches() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateTextualMatches();
	}

	public void setUpdateTextualMatches(boolean update) {
		if (fRenameTypeProcessor != null)
			fRenameTypeProcessor.setUpdateTextualMatches(update);
	}
	
	//---- IReferenceUpdating -----------------------------------

	public boolean canEnableUpdateReferences() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.canEnableUpdateReferences();
	}

	public void setUpdateReferences(boolean update) {
		if (fRenameTypeProcessor != null)
			fRenameTypeProcessor.setUpdateReferences(update);
	}

	public boolean getUpdateReferences(){
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateReferences();		
	}
	
	//---- IQualifiedNameUpdating -------------------------------

	public boolean canEnableQualifiedNameUpdating() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.canEnableQualifiedNameUpdating();
	}
	
	public boolean getUpdateQualifiedNames() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateQualifiedNames();
	}
	
	public void setUpdateQualifiedNames(boolean update) {
		if (fRenameTypeProcessor == null)
			return;
		fRenameTypeProcessor.setUpdateQualifiedNames(update);
	}
	
	public String getFilePatterns() {
		if (fRenameTypeProcessor == null)
			return null;
		return fRenameTypeProcessor.getFilePatterns();
	}
	
	public void setFilePatterns(String patterns) {
		if (fRenameTypeProcessor == null)
			return;
		fRenameTypeProcessor.setFilePatterns(patterns);
	}
	
	// ---- ISimilarDeclarationUpdating ------------------------------

	public boolean canEnableSimilarDeclarationUpdating() {
		if (fRenameTypeProcessor == null)
			return false;
		else
			return fRenameTypeProcessor.canEnableSimilarDeclarationUpdating();
	}

	public void setUpdateSimilarDeclarations(boolean update) {
		if (fRenameTypeProcessor == null)
			return;
		fRenameTypeProcessor.setUpdateSimilarDeclarations(update);
	}

	public boolean getUpdateSimilarDeclarations() {
		if (fRenameTypeProcessor == null)
			return false;
		return fRenameTypeProcessor.getUpdateSimilarDeclarations();
	}

	public int getMatchStrategy() {
		if (fRenameTypeProcessor == null)
			return RenamingNameSuggestor.STRATEGY_EXACT; // method should not be called in this case anyway ...
		return fRenameTypeProcessor.getMatchStrategy();
	}

	public void setMatchStrategy(int selectedStrategy) {
		if (fRenameTypeProcessor == null)
			return;
		fRenameTypeProcessor.setMatchStrategy(selectedStrategy);
	}

	public IJavaElement[] getSimilarElements() {
		if (fRenameTypeProcessor == null)
			return null;
		return fRenameTypeProcessor.getSimilarElements();
	}

	public IResource getRefactoredResource(IResource element) {
		if (fRenameTypeProcessor == null)
			return element;
		return fRenameTypeProcessor.getRefactoredResource(element);
	}
	
	public IJavaElement getRefactoredJavaElement(IJavaElement element) {
		if (fRenameTypeProcessor == null)
			return element;
		return fRenameTypeProcessor.getRefactoredJavaElement(element);
	}
	
	// --- preconditions ----------------------------------
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		if (fRenameTypeProcessor != null && ! fCu.isStructureKnown()){
			fRenameTypeProcessor= null;
			fWillRenameType= false;
			return new RefactoringStatus();
		}
		
		//for a test case what it's needed, see bug 24248 
		//(the type might be gone from the editor by now)
		if (fWillRenameType && fRenameTypeProcessor != null && ! fRenameTypeProcessor.getType().exists()){
			fRenameTypeProcessor= null;
			fWillRenameType= false;
			return new RefactoringStatus();
		}
		 
		// we purposely do not check activation of the renameTypeRefactoring here. 
		return new RefactoringStatus();
	}
	
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			if (fWillRenameType && (!fCu.isStructureKnown())){
				RefactoringStatus result1= new RefactoringStatus();
				
				RefactoringStatus result2= new RefactoringStatus();
				result2.merge(Checks.checkCompilationUnitNewName(fCu, getNewElementName()));
				if (result2.hasFatalError())
					result1.addError(Messages.format(RefactoringCoreMessages.RenameCompilationUnitRefactoring_not_parsed_1, fCu.getElementName())); 
				else 
					result1.addError(Messages.format(RefactoringCoreMessages.RenameCompilationUnitRefactoring_not_parsed, fCu.getElementName())); 
				result1.merge(result2);			
			}	
		
			if (fWillRenameType) {
				return fRenameTypeProcessor.checkFinalConditions(pm, context);
			} else {
				return Checks.checkCompilationUnitNewName(fCu, getNewElementName());
			}
		} finally{
			pm.done();
		}		
	}
	
	private void computeRenameTypeRefactoring() throws CoreException{
		if (getSimpleCUName().indexOf(".") != -1) { //$NON-NLS-1$
			fRenameTypeProcessor= null;
			fWillRenameType= false;
			return;
		}
		IType type= getTypeWithTheSameName();
		if (type != null) {
			fRenameTypeProcessor= new RenameTypeProcessor(type);
		} else {
			fRenameTypeProcessor= null;
		}
		fWillRenameType= fRenameTypeProcessor != null && fCu.isStructureKnown();
	}

	private IType getTypeWithTheSameName() {
		try {
			IType[] topLevelTypes= fCu.getTypes();
			String name= getSimpleCUName();
			for (int i = 0; i < topLevelTypes.length; i++) {
				if (name.equals(topLevelTypes[i].getElementName()))
					return topLevelTypes[i];
			}
			return null; 
		} catch (CoreException e) {
			return null;
		}
	}
	
	private String getSimpleCUName() {
		return removeFileNameExtension(fCu.getElementName());
	}
	
	/**
	 * Removes the extension (whatever comes after the last '.') from the given file name.
	 */
	private static String removeFileNameExtension(String fileName) {
		if (fileName.lastIndexOf(".") == -1) //$NON-NLS-1$
			return fileName;
		return fileName.substring(0, fileName.lastIndexOf(".")); //$NON-NLS-1$
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		// renaming the file is taken care of in renameTypeRefactoring
		if (fWillRenameType)
			return fRenameTypeProcessor.createChange(pm);
		fRenameTypeProcessor= null;
		final String newName= getNewElementName();
		final IResource resource= fCu.getResource();
		if (resource != null && resource.isLinked()) {
			final IProject project= resource.getProject();
			final String name= project.getName();
			final String description= Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_descriptor_description_short, resource.getName());
			final String header= Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_descriptor_description, new String[] { resource.getFullPath().toString(), newName});
			final String comment= new JDTRefactoringDescriptorComment(name, this, header).asString();
			final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE;
			final RenameResourceDescriptor descriptor= new RenameResourceDescriptor();
			descriptor.setProject(name);
			descriptor.setDescription(description);
			descriptor.setComment(comment);
			descriptor.setFlags(flags);
			descriptor.setResource(resource);
			descriptor.setNewName(newName);
			return new DynamicValidationStateChange(new RenameResourceChange(descriptor, resource, newName, comment));
		}
		String label= null;
		if (fCu != null) {
			final IPackageFragment fragment= (IPackageFragment) fCu.getParent();
			if (!fragment.isDefaultPackage())
				label= fragment.getElementName() + "." + fCu.getElementName(); //$NON-NLS-1$
			else
				label= fCu.getElementName();
		} else
			label= fCu.getElementName();
		final Map arguments= new HashMap();
		final String name= fCu.getJavaProject().getElementName();
		final String description= Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_descriptor_description_short, fCu.getElementName());
		final String header= Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_descriptor_description, new String[] { label, newName});
		final String comment= new JDTRefactoringDescriptorComment(name, this, header).asString();
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(RenameCompilationUnitProcessor.ID_RENAME_COMPILATION_UNIT, name, description, comment, arguments, JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, newName);
		return new DynamicValidationStateChange(new RenameCompilationUnitChange(descriptor, fCu, newName, comment));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
		if (fWillRenameType)
			return fRenameTypeProcessor.postCreateChange(participantChanges, pm);
		return super.postCreateChange(participantChanges, pm);
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments generic= (JavaRefactoringArguments) arguments;
			final String path= generic.getAttribute(ATTRIBUTE_PATH);
			if (path != null) {
				final IResource resource= ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(path));
				if (resource == null || !resource.exists())
					return ScriptableRefactoring.createInputFatalStatus(resource, getRefactoring().getName(), ID_RENAME_COMPILATION_UNIT);
				else {
					fCu= (ICompilationUnit) JavaCore.create(resource);
					try {
						computeRenameTypeRefactoring();
					} catch (CoreException exception) {
						JavaPlugin.log(exception);
					}
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PATH));
			final String name= generic.getAttribute(ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_NAME));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * @return the RenameTypeProcessor or <code>null</code> if no type will be renamed
	 */
	public RenameTypeProcessor getRenameTypeProcessor() {
		return fRenameTypeProcessor;
	}

	public boolean isWillRenameType() {
		return fWillRenameType;
	}
}
