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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRewriteUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.Changes;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ReorgPolicyFactory {

	private static final String ATTRIBUTE_POLICY= "policy"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FILES= "files"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FOLDERS= "folders"; //$NON-NLS-1$
	private static final String ATTRIBUTE_UNITS= "units"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ROOTS= "roots"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FRAGMENTS= "fragments"; //$NON-NLS-1$
	private static final String ATTRIBUTE_MEMBERS= "members"; //$NON-NLS-1$
	private static final String ATTRIBUTE_QUALIFIED= "qualified"; //$NON-NLS-1$
	private static final String ATTRIBUTE_REFERENCES= "references"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PATTERNS= "patterns"; //$NON-NLS-1$

	private ReorgPolicyFactory() {
		//private
	}
	
	public static ICopyPolicy createCopyPolicy(RefactoringStatus status, RefactoringArguments arguments) {
		// TODO: implement
		
		return null;
	}
	
	public static IMovePolicy createMovePolicy(RefactoringStatus status, RefactoringArguments arguments) {
		// TODO: implement
		
		return null;
	}
	
	public static ICopyPolicy createCopyPolicy(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		return (ICopyPolicy)createReorgPolicy(true, resources, javaElements);
	}
	
	public static IMovePolicy createMovePolicy(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		return (IMovePolicy)createReorgPolicy(false, resources, javaElements);
	}
	
	private static IReorgPolicy createReorgPolicy(boolean copy, IResource[] selectedResources, IJavaElement[] selectedJavaElements) throws JavaModelException{
		final IReorgPolicy NO;
		if (copy)
			NO= new NoCopyPolicy();
		else
			NO= new NoMovePolicy();

		ActualSelectionComputer selectionComputer= new ActualSelectionComputer(selectedJavaElements, selectedResources);
		IResource[] resources= selectionComputer.getActualResourcesToReorg();
		IJavaElement[] javaElements= selectionComputer.getActualJavaElementsToReorg();
	
		if (isNothingToReorg(resources, javaElements) || 
			containsNull(resources) ||
			containsNull(javaElements) ||
			ReorgUtils.isArchiveMember(javaElements) ||
			ReorgUtils.hasElementsOfType(javaElements, IJavaElement.JAVA_PROJECT) ||
			ReorgUtils.hasElementsOfType(javaElements, IJavaElement.JAVA_MODEL) ||
			ReorgUtils.hasElementsOfType(resources, IResource.PROJECT | IResource.ROOT) ||
			! haveCommonParent(resources, javaElements))
			return NO;
			
		if (ReorgUtils.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT)){
			if (resources.length != 0 || ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT))
				return NO;
			if (copy)
				return new CopyPackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
			else
				return new MovePackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
		}
		
		if (ReorgUtils.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT)){
			if (resources.length != 0 || ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT))
				return NO;
			if (copy)
				return new CopyPackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
			else
				return new MovePackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
		}
		
		if (ReorgUtils.hasElementsOfType(resources, IResource.FILE | IResource.FOLDER) || ReorgUtils.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT)){
			if (ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.COMPILATION_UNIT))
				return NO;
			if (ReorgUtils.hasElementsNotOfType(resources, IResource.FILE | IResource.FOLDER))
				return NO;
			if (copy)
				return new CopyFilesFoldersAndCusPolicy(ReorgUtils.getFiles(resources), ReorgUtils.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements));
			else
				return new MoveFilesFoldersAndCusPolicy(ReorgUtils.getFiles(resources), ReorgUtils.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements));
		}
		
		if (hasElementsSmallerThanCuOrClassFile(javaElements)){
			//assertions guaranteed by common parent
			Assert.isTrue(resources.length == 0);
			Assert.isTrue(! ReorgUtils.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT));
			Assert.isTrue(! ReorgUtils.hasElementsOfType(javaElements, IJavaElement.CLASS_FILE));
			Assert.isTrue(! hasElementsLargerThanCuOrClassFile(javaElements));
			if (copy)
				return new CopySubCuElementsPolicy(javaElements);
			else
				return new MoveSubCuElementsPolicy(javaElements);
		}
		return NO;
	}
		
	private static boolean containsNull(Object[] objects) {
		for (int i= 0; i < objects.length; i++) {
			if (objects[i]==null) return  true;
		}
		return false;
	}

	private static boolean hasElementsSmallerThanCuOrClassFile(IJavaElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (ReorgUtils.isInsideCompilationUnit(javaElements[i]))
				return true;
			if (ReorgUtils.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean hasElementsLargerThanCuOrClassFile(IJavaElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (! ReorgUtils.isInsideCompilationUnit(javaElements[i]) &&
				! ReorgUtils.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean haveCommonParent(IResource[] resources, IJavaElement[] javaElements) {
		return new ParentChecker(resources, javaElements).haveCommonParent();
	}

	private static boolean isNothingToReorg(IResource[] resources, IJavaElement[] javaElements) {
		return resources.length + javaElements.length == 0;
	}

	private static abstract class ReorgPolicy implements IReorgPolicy {

		private static final String ATTRIBUTE_DESTINATION= "destination"; //$NON-NLS-1$

		//invariant: only 1 of these can ever be not null
		private IResource fResourceDestination;
		private IJavaElement fJavaElementDestination;

		protected Map getRefactoringArguments(String project) {
			final Map arguments= new HashMap();
			final IJavaElement element= getJavaElementDestination();
			if (element != null)
				arguments.put(ATTRIBUTE_DESTINATION, JavaRefactoringDescriptor.elementToHandle(project, element));
			else {
				final IResource resource= getResourceDestination();
				if (resource != null)
					arguments.put(ATTRIBUTE_DESTINATION, JavaRefactoringDescriptor.resourceToHandle(project, resource));
			}
			return arguments;
		}

		protected String getDestinationLabel() {
			Object destination= getJavaElementDestination();
			if (destination == null)
				destination= getResourceDestination();
			return JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED);
		}

		public final RefactoringStatus setDestination(IResource destination) throws JavaModelException {
			Assert.isNotNull(destination);
			resetDestinations();
			fResourceDestination= destination;
			return verifyDestination(destination);
		}
		public final RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException {
			Assert.isNotNull(destination);
			resetDestinations();
			fJavaElementDestination= destination;
			return verifyDestination(destination);
		}
		protected abstract RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException;
		protected abstract RefactoringStatus verifyDestination(IResource destination) throws JavaModelException;

		public boolean canChildrenBeDestinations(IJavaElement javaElement) {
			return true;
		}
		public boolean canChildrenBeDestinations(IResource resource) {
			return true;
		}
		public boolean canElementBeDestination(IJavaElement javaElement) {
			return true;
		}
		public boolean canElementBeDestination(IResource resource) {
			return true;
		}
		
		private void resetDestinations() {
			fJavaElementDestination= null;
			fResourceDestination= null;
		}
		public final IResource getResourceDestination(){
			return fResourceDestination;
		}
		public final IJavaElement getJavaElementDestination(){
			return fJavaElementDestination;
		}
		public IFile[] getAllModifiedFiles() {
			return new IFile[0];
		}
		protected RefactoringModifications getModifications() throws CoreException {
			return null;
		}
		public final RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
			RefactoringModifications modifications= getModifications();
			if (modifications != null) {
				return modifications.loadParticipants(status, processor, natures, shared);
			} else {
				return new RefactoringParticipant[0];
			}
		}
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException{
			Assert.isNotNull(reorgQueries);
			ResourceChangeChecker checker= (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
			IFile[] allModifiedFiles= getAllModifiedFiles();
			RefactoringModifications modifications= getModifications();
			IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
			for (int i= 0; i < allModifiedFiles.length; i++) {
				deltaFactory.change(allModifiedFiles[i]);
			}
			if (modifications != null) {
				modifications.buildDelta(deltaFactory);
				modifications.buildValidateEdits((ValidateEditChecker)context.getChecker(ValidateEditChecker.class));
			}
			return new RefactoringStatus();
		}
		public boolean hasAllInputSet() {
			return fJavaElementDestination != null || fResourceDestination != null;
		}
		public boolean canEnableUpdateReferences() {
			return false;
		}
		public boolean canUpdateReferences() {
			Assert.isTrue(false);//should not be called if canEnableUpdateReferences is not overridden and returns false
			return false;
		}
		public boolean getUpdateReferences() {
			Assert.isTrue(false);//should not be called if canUpdateReferences is not overridden and returns false
			return false;
		}
		public void setUpdateReferences(boolean update) {
			Assert.isTrue(false);//should not be called if canUpdateReferences is not overridden and returns false
		}
		public boolean canEnableQualifiedNameUpdating() {
			return false;
		}
		public boolean canUpdateQualifiedNames() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return false;
		}
		public String getFilePatterns() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return null;
		}
		public boolean getUpdateQualifiedNames() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return false;
		}
		public void setFilePatterns(String patterns) {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
		}
		public void setUpdateQualifiedNames(boolean update) {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
		}
		public boolean canEnable() throws JavaModelException {
			IResource[] resources= getResources();
			for (int i= 0; i < resources.length; i++) {
				IResource resource= resources[i];
				if (! resource.exists() || resource.isPhantom() || ! resource.isAccessible())
					return false;
			}
			
			IJavaElement[] javaElements= getJavaElements();
			for (int i= 0; i < javaElements.length; i++) {
				IJavaElement element= javaElements[i];
				if (!element.exists()) return false;
			}
			return true;
		}
	}

	private static abstract class FilesFoldersAndCusReorgPolicy extends ReorgPolicy{

		private ICompilationUnit[] fCus;
		private IFolder[] fFolders;
		private IFile[] fFiles;

		public FilesFoldersAndCusReorgPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			fFiles= files;
			fFolders= folders;
			fCus= cus;
		}

		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_doesnotexist0); 
			if (javaElement instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel); 
	
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_readonly); 
	
			if (! javaElement.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_structure); 
	
			if (javaElement instanceof IOpenable){
				IOpenable openable= (IOpenable)javaElement;
				if (! openable.isConsistent())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_inconsistent); 
			}				
	
			if (javaElement instanceof IPackageFragmentRoot){
				IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
				if (root.isArchive())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_archive); 
				if (root.isExternal())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_external); 
			}
			
			if (ReorgUtils.isInsideCompilationUnit(javaElement)) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot); 
			}
			
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null || isChildOfOrEqualToAnyFolder(destinationAsContainer))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_not_this_resource); 
			
			if (containsLinkedResources() && !ReorgUtils.canBeDestinationForLinkedResources(javaElement))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_linked); 
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			Assert.isNotNull(resource);
			if (! resource.exists() || resource.isPhantom())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_phantom);			 
			if (!resource.isAccessible())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_inaccessible); 
			Assert.isTrue(resource.getType() != IResource.ROOT);
					
			if (isChildOfOrEqualToAnyFolder(resource))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_not_this_resource); 
						
			if (containsLinkedResources() && !ReorgUtils.canBeDestinationForLinkedResources(resource))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_linked); 
	
			return new RefactoringStatus();
		}

		private boolean isChildOfOrEqualToAnyFolder(IResource resource) {
			for (int i= 0; i < fFolders.length; i++) {
				IFolder folder= fFolders[i];
				if (folder.equals(resource) || ParentChecker.isDescendantOf(resource, folder))
					return true;
			}
			return false;
		}
	
		public boolean canChildrenBeDestinations(IJavaElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaElement.JAVA_MODEL :
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					return true;
				default :
					return false;
			}
		}
		public boolean canChildrenBeDestinations(IResource resource) {
			return resource instanceof IContainer;
		}
		public boolean canElementBeDestination(IJavaElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaElement.PACKAGE_FRAGMENT :
					return true;
				default :
					return false;
			}
		}
		public boolean canElementBeDestination(IResource resource) {
			return resource instanceof IContainer;
		}
		
		private static IContainer getAsContainer(IResource resDest){
			if (resDest instanceof IContainer)
				return (IContainer)resDest;
			if (resDest instanceof IFile)
				return ((IFile)resDest).getParent();
			return null;				
		}
				
		protected final IContainer getDestinationAsContainer(){
			IResource resDest= getResourceDestination();
			if (resDest != null)
				return getAsContainer(resDest);
			IJavaElement jelDest= getJavaElementDestination();
			Assert.isNotNull(jelDest);				
			return getAsContainer(ReorgUtils.getResource(jelDest));
		}
		
		protected final IJavaElement getDestinationContainerAsJavaElement() {
			if (getJavaElementDestination() != null)
				return getJavaElementDestination();
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null)
				return null;
			IJavaElement je= JavaCore.create(destinationAsContainer);
			if (je != null && je.exists())
				return je;
			return null;
		}
		
		protected final IPackageFragment getDestinationAsPackageFragment() {
			IPackageFragment javaAsPackage= getJavaDestinationAsPackageFragment(getJavaElementDestination());
			if (javaAsPackage != null)
				return javaAsPackage;
			return getResourceDestinationAsPackageFragment(getResourceDestination());
		}
				
		private static IPackageFragment getJavaDestinationAsPackageFragment(IJavaElement javaDest){
			if( javaDest == null || ! javaDest.exists())
				return null;					
			if (javaDest instanceof IPackageFragment)
				return (IPackageFragment) javaDest;
			if (javaDest instanceof IPackageFragmentRoot)
				return ((IPackageFragmentRoot) javaDest).getPackageFragment(""); //$NON-NLS-1$
			if (javaDest instanceof IJavaProject) {
				try {
					IPackageFragmentRoot root= ReorgUtils.getCorrespondingPackageFragmentRoot((IJavaProject)javaDest);
					if (root != null)
						return root.getPackageFragment("");  //$NON-NLS-1$
				} catch (JavaModelException e) {
					// fall through
				}
			}
			return (IPackageFragment) javaDest.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		}
				
		private static IPackageFragment getResourceDestinationAsPackageFragment(IResource resource){
			if (resource instanceof IFile)
				return getJavaDestinationAsPackageFragment(JavaCore.create(resource.getParent()));
			return null;	
		}

		public final IJavaElement[] getJavaElements(){
			return fCus;
		}

		public final IResource[] getResources() {
			return ReorgUtils.union(fFiles, fFolders);
		}

		protected boolean containsLinkedResources() {
			return 	ReorgUtils.containsLinkedResources(fFiles) || 
					ReorgUtils.containsLinkedResources(fFolders) || 
					ReorgUtils.containsLinkedResources(fCus);
		}

		protected final IFolder[] getFolders(){
			return fFolders;
		}
		protected final IFile[] getFiles(){
			return fFiles;
		}
		protected final ICompilationUnit[] getCus(){
			return fCus;
		}
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwriting(reorgQueries);
			return status;
		}

		private void confirmOverwriting(IReorgQueries reorgQueries) {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setFiles(fFiles);
			oh.setFolders(fFolders);
			oh.setCus(fCus);
			IPackageFragment destPack= getDestinationAsPackageFragment();
			if (destPack != null) {
				oh.confirmOverwriting(reorgQueries, destPack);
			} else {
				IContainer destinationAsContainer= getDestinationAsContainer();
				if (destinationAsContainer != null)
					oh.confirmOverwriting(reorgQueries, destinationAsContainer);
			}	
			fFiles= oh.getFilesWithoutUnconfirmedOnes();
			fFolders= oh.getFoldersWithoutUnconfirmedOnes();
			fCus= oh.getCusWithoutUnconfirmedOnes();
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fFiles.length + fFolders.length + fCus.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= JavaRefactoringDescriptor.JAR_IMPORTABLE | JavaRefactoringDescriptor.JAR_REFACTORABLE | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JavaRefactoringDescriptorComment comment= new JavaRefactoringDescriptorComment(project, this, header);
			final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_FILES, new Integer(fFiles.length).toString());
			for (int offset= 0; offset < fFiles.length; offset++)
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.resourceToHandle(fFiles[offset]));
			arguments.put(ATTRIBUTE_FOLDERS, new Integer(fFolders.length).toString());
			for (int offset= 0; offset < fFolders.length; offset++)
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fFiles.length + 1), descriptor.resourceToHandle(fFolders[offset]));
			arguments.put(ATTRIBUTE_UNITS, new Integer(fCus.length).toString());
			for (int offset= 0; offset < fCus.length; offset++)
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fFolders.length + fFiles.length + 1), descriptor.elementToHandle(fCus[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fFiles.length; index++) {
				if (result == null)
					result= fFiles[index].getProject();
				else if (!result.equals(fFiles[index].getProject()))
					return null;
			}
			for (int index= 0; index < fFolders.length; index++) {
				if (result == null)
					result= fFolders[index].getProject();
				else if (!result.equals(fFolders[index].getProject()))
					return null;
			}
			for (int index= 0; index < fCus.length; index++) {
				if (result == null)
					result= fCus[index].getJavaProject().getProject();
				else if (!result.equals(fCus[index].getJavaProject().getProject()))
					return null;
			}
			return result;
		}

		protected abstract String getProcessorId();

		protected abstract String getDescriptionSingular();
		protected abstract String getDescriptionPlural();
		protected abstract String getHeaderPattern();
		
		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}
	}

	private static abstract class SubCuElementReorgPolicy extends ReorgPolicy{
		private final IJavaElement[] fJavaElements;
		SubCuElementReorgPolicy(IJavaElement[] javaElements){
			Assert.isNotNull(javaElements);
			fJavaElements= javaElements;
		}

		protected final RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_no_resource); 
		}

		protected final ICompilationUnit getSourceCu() {
			//all have a common parent, so all must be in the same cu
			//we checked before that the array in not null and not empty
			return (ICompilationUnit) fJavaElements[0].getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		public final IJavaElement[] getJavaElements() {
			return fJavaElements;
		}
		public final IResource[] getResources() {
			return new IResource[0];
		}
	
		protected final ICompilationUnit getDestinationCu() {
			return getDestinationCu(getJavaElementDestination());
		}

		protected static final ICompilationUnit getDestinationCu(IJavaElement destination) {
			if (destination instanceof ICompilationUnit)
				return (ICompilationUnit) destination;
			return (ICompilationUnit) destination.getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		protected static CompilationUnitChange createCompilationUnitChange(ICompilationUnit cu, CompilationUnitRewrite rewrite) throws CoreException {
			CompilationUnitChange change= rewrite.createChange();
			if (change != null) {
				if (cu.isWorkingCopy())
					change.setSaveMode(TextFileChange.LEAVE_DIRTY);
			}
			return change;
		}

		protected void copyToDestination(IJavaElement element, CompilationUnitRewrite targetRewriter, CompilationUnit sourceCuNode, CompilationUnit targetCuNode) throws CoreException {
			final ASTRewrite rewrite= targetRewriter.getASTRewrite();
			switch(element.getElementType()){
				case IJavaElement.FIELD: 
					copyMemberToDestination((IMember) element, targetRewriter, sourceCuNode, targetCuNode, createNewFieldDeclarationNode(((IField)element), rewrite, sourceCuNode));
					break;
				case IJavaElement.IMPORT_CONTAINER:
					copyImportsToDestination((IImportContainer)element, rewrite, sourceCuNode, targetCuNode);
					break;
				case IJavaElement.IMPORT_DECLARATION:
					copyImportToDestination((IImportDeclaration)element, rewrite, sourceCuNode, targetCuNode);
					break;
				case IJavaElement.INITIALIZER:
					copyInitializerToDestination((IInitializer)element, targetRewriter, sourceCuNode, targetCuNode);
					break;
				case IJavaElement.METHOD: 
					copyMethodToDestination((IMethod)element, targetRewriter, sourceCuNode, targetCuNode);
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					copyPackageDeclarationToDestination((IPackageDeclaration)element, rewrite, sourceCuNode, targetCuNode);
					break;
				case IJavaElement.TYPE :
					copyTypeToDestination((IType) element, targetRewriter, sourceCuNode, targetCuNode);
					break;

				default: Assert.isTrue(false); 
			}
		}

		private BodyDeclaration createNewFieldDeclarationNode(IField field, ASTRewrite rewrite, CompilationUnit sourceCuNode) throws CoreException {
			AST targetAst= rewrite.getAST();
			ITextFileBuffer buffer= null;
			BodyDeclaration newDeclaration= null;
			ICompilationUnit unit= field.getCompilationUnit();
			try {
				buffer= RefactoringFileBuffers.acquire(unit);
				IDocument document= buffer.getDocument();
				BodyDeclaration bodyDeclaration= ASTNodeSearchUtil.getFieldOrEnumConstantDeclaration(field, sourceCuNode);
				if (bodyDeclaration instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration= (FieldDeclaration) bodyDeclaration;
					if (fieldDeclaration.fragments().size() == 1)
						return (FieldDeclaration) ASTNode.copySubtree(targetAst, fieldDeclaration);
					VariableDeclarationFragment originalFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, sourceCuNode);
					VariableDeclarationFragment copiedFragment= (VariableDeclarationFragment) ASTNode.copySubtree(targetAst, originalFragment);
					newDeclaration= targetAst.newFieldDeclaration(copiedFragment);
					((FieldDeclaration) newDeclaration).setType((Type) ASTNode.copySubtree(targetAst, fieldDeclaration.getType()));
				} else if (bodyDeclaration instanceof EnumConstantDeclaration) {
					EnumConstantDeclaration constantDeclaration= (EnumConstantDeclaration) bodyDeclaration;
					EnumConstantDeclaration newConstDeclaration= targetAst.newEnumConstantDeclaration();
					newConstDeclaration.setName((SimpleName) ASTNode.copySubtree(targetAst, constantDeclaration.getName()));
					AnonymousClassDeclaration anonymousDeclaration= constantDeclaration.getAnonymousClassDeclaration();
					if (anonymousDeclaration != null)
						newConstDeclaration.setAnonymousClassDeclaration((AnonymousClassDeclaration) rewrite.createStringPlaceholder(document.get(anonymousDeclaration.getStartPosition(), anonymousDeclaration.getLength()), ASTNode.ANONYMOUS_CLASS_DECLARATION));
					newDeclaration= newConstDeclaration;
				} else
					Assert.isTrue(false);
				if (newDeclaration != null) {
					newDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(targetAst, bodyDeclaration.getModifiers()));
					Javadoc javadoc= bodyDeclaration.getJavadoc();
					if (javadoc != null)
						newDeclaration.setJavadoc((Javadoc) rewrite.createStringPlaceholder(document.get(javadoc.getStartPosition(), javadoc.getLength()), ASTNode.JAVADOC));
				}
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			} finally {
				if (buffer != null)
					RefactoringFileBuffers.release(unit);
			}
			return newDeclaration;
		}

		private void copyImportsToDestination(IImportContainer container, ASTRewrite rewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			//there's no special AST node for the container - we copy all imports
			IJavaElement[] importDeclarations= container.getChildren();
			for (int i= 0; i < importDeclarations.length; i++) {
				Assert.isTrue(importDeclarations[i] instanceof IImportDeclaration);//promised in API
				IImportDeclaration importDeclaration= (IImportDeclaration)importDeclarations[i];
				copyImportToDestination(importDeclaration, rewrite, sourceCuNode, destinationCuNode);
			}
		}

		private void copyImportToDestination(IImportDeclaration declaration, ASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			ImportDeclaration sourceNode= ASTNodeSearchUtil.getImportDeclarationNode(declaration, sourceCuNode);
			ImportDeclaration copiedNode= (ImportDeclaration) ASTNode.copySubtree(targetRewrite.getAST(), sourceNode);
			targetRewrite.getListRewrite(destinationCuNode, CompilationUnit.IMPORTS_PROPERTY).insertLast(copiedNode, null);
		}

		private void copyPackageDeclarationToDestination(IPackageDeclaration declaration, ASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			if (destinationCuNode.getPackage() != null)
				return;
			PackageDeclaration sourceNode= ASTNodeSearchUtil.getPackageDeclarationNode(declaration, sourceCuNode);
			PackageDeclaration copiedNode= (PackageDeclaration) ASTNode.copySubtree(targetRewrite.getAST(), sourceNode);
			targetRewrite.set(destinationCuNode, CompilationUnit.PACKAGE_PROPERTY, copiedNode, null);
		}

		private void copyInitializerToDestination(IInitializer initializer, CompilationUnitRewrite targetRewriter, CompilationUnit sourceCuNode, CompilationUnit targetCuNode) throws JavaModelException {
			BodyDeclaration newInitializer= (BodyDeclaration) targetRewriter.getASTRewrite().createStringPlaceholder(getUnindentedSource(initializer), ASTNode.INITIALIZER);
			copyMemberToDestination(initializer, targetRewriter, sourceCuNode, targetCuNode, newInitializer);
		}

		private void copyTypeToDestination(IType type, CompilationUnitRewrite targetRewriter, CompilationUnit sourceCuNode, CompilationUnit targetCuNode) throws JavaModelException {
			AbstractTypeDeclaration newType= (AbstractTypeDeclaration) targetRewriter.getASTRewrite().createStringPlaceholder(getUnindentedSource(type), ASTNode.TYPE_DECLARATION);
			IType enclosingType= getEnclosingType(getJavaElementDestination());
			if (enclosingType != null) {
				copyMemberToDestination(type, targetRewriter, sourceCuNode, targetCuNode, newType);
			} else {
				targetRewriter.getASTRewrite().getListRewrite(targetCuNode, CompilationUnit.TYPES_PROPERTY).insertLast(newType, null);
			}
		}

		private void copyMethodToDestination(IMethod method, CompilationUnitRewrite targetRewriter, CompilationUnit sourceCuNode, CompilationUnit targetCuNode) throws JavaModelException {
			BodyDeclaration newMethod= (BodyDeclaration) targetRewriter.getASTRewrite().createStringPlaceholder(getUnindentedSource(method), ASTNode.METHOD_DECLARATION);
			copyMemberToDestination(method, targetRewriter, sourceCuNode, targetCuNode, newMethod);
		}

		private void copyMemberToDestination(IMember member, CompilationUnitRewrite targetRewriter, CompilationUnit sourceCuNode, CompilationUnit targetCuNode, BodyDeclaration newMember) throws JavaModelException {
			IJavaElement javaElementDestination= getJavaElementDestination();
			ASTNode nodeDestination;
			ASTNode destinationContainer;
			switch (javaElementDestination.getElementType()) {
				case IJavaElement.INITIALIZER:
					nodeDestination= ASTNodeSearchUtil.getInitializerNode((IInitializer) javaElementDestination, targetCuNode);
					destinationContainer= nodeDestination.getParent();
					break;
				case IJavaElement.FIELD:
					nodeDestination= ASTNodeSearchUtil.getFieldOrEnumConstantDeclaration((IField) javaElementDestination, targetCuNode);
					destinationContainer= nodeDestination.getParent();
					break;
				case IJavaElement.METHOD:
					nodeDestination= ASTNodeSearchUtil.getMethodOrAnnotationTypeMemberDeclarationNode((IMethod) javaElementDestination, targetCuNode);
					destinationContainer= nodeDestination.getParent();
					break;
				case IJavaElement.TYPE:
					nodeDestination= null;
					IType typeDestination= (IType) javaElementDestination;
					if (typeDestination.isAnonymous())
						destinationContainer= ASTNodeSearchUtil.getClassInstanceCreationNode(typeDestination, targetCuNode).getAnonymousClassDeclaration();
					else
						destinationContainer= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(typeDestination, targetCuNode);
					break;
				default:
					nodeDestination= null;
					destinationContainer= null;
			}
			if (!(member instanceof IInitializer)) {
				BodyDeclaration decl= ASTNodeSearchUtil.getBodyDeclarationNode(member, sourceCuNode);
				if (decl != null)
					ImportRewriteUtil.addImports(targetRewriter, decl, new HashMap(), new HashMap(), false);
			}
			if (destinationContainer != null) {
				ListRewrite listRewrite;
				if (destinationContainer instanceof AbstractTypeDeclaration) {
					if (newMember instanceof EnumConstantDeclaration && destinationContainer instanceof EnumDeclaration)
						listRewrite= targetRewriter.getASTRewrite().getListRewrite(destinationContainer, EnumDeclaration.ENUM_CONSTANTS_PROPERTY);
					else
						listRewrite= targetRewriter.getASTRewrite().getListRewrite(destinationContainer, ((AbstractTypeDeclaration) destinationContainer).getBodyDeclarationsProperty());
				} else
					listRewrite= targetRewriter.getASTRewrite().getListRewrite(destinationContainer, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);

				if (nodeDestination != null) {
					final List list= listRewrite.getOriginalList();
					final int index= list.indexOf(nodeDestination);
					if (index > 0 && index < list.size() - 1) {
						listRewrite.insertBefore(newMember, (ASTNode) list.get(index), null);
					} else
						listRewrite.insertLast(newMember, null);
				} else
					listRewrite.insertAt(newMember, ASTNodes.getInsertionIndex(newMember, listRewrite.getRewrittenList()), null);
				return; // could insert into/after destination
			}
			// fall-back / default:
			final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getDestinationAsType(), targetCuNode);
			targetRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertLast(newMember, null);
		}

		private static String getUnindentedSource(ISourceReference sourceReference) throws JavaModelException {
			Assert.isTrue(sourceReference instanceof IJavaElement);
			String[] lines= Strings.convertIntoLines(sourceReference.getSource());
			final IJavaProject project= ((IJavaElement) sourceReference).getJavaProject();
			Strings.trimIndentation(lines, project, false);
			return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed((IJavaElement) sourceReference));
		}

		private IType getDestinationAsType() throws JavaModelException {
			IJavaElement destination= getJavaElementDestination();
			IType enclosingType= getEnclosingType(destination);
			if (enclosingType != null)
				return enclosingType;
			ICompilationUnit enclosingCu= getEnclosingCu(destination);
			Assert.isNotNull(enclosingCu);
			IType mainType= JavaElementUtil.getMainType(enclosingCu);
			Assert.isNotNull(mainType);
			return mainType;
		}

		private static ICompilationUnit getEnclosingCu(IJavaElement destination) {
			if (destination instanceof ICompilationUnit)
				return (ICompilationUnit) destination;
			return (ICompilationUnit)destination.getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		private static IType getEnclosingType(IJavaElement destination) {
			if (destination instanceof IType)
				return (IType) destination;
			return (IType)destination.getAncestor(IJavaElement.TYPE);
		}
		
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			for (int i= 0; i < fJavaElements.length; i++) {
				if (fJavaElements[i] instanceof IMember){
					IMember member= (IMember) fJavaElements[i];
					//we can copy some binary members, but not all
					if (member.isBinary() && member.getSourceRange() == null)
						return false;
				}
			}
			return true;
		}	
		
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			return recursiveVerifyDestination(destination);
		}
		
		private RefactoringStatus recursiveVerifyDestination(IJavaElement destination) throws JavaModelException {
			Assert.isNotNull(destination);
			if (!destination.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_doesnotexist1); 
			if (destination instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel); 
			if (! (destination instanceof ICompilationUnit) && ! ReorgUtils.isInsideCompilationUnit(destination))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot); 

			ICompilationUnit destinationCu= getDestinationCu(destination);
			Assert.isNotNull(destinationCu);
			if (destinationCu.isReadOnly())//the resource read-onliness is handled by validateEdit
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot_modify); 
				
			switch(destination.getElementType()){
				case IJavaElement.COMPILATION_UNIT:
					int[] types0= new int[]{IJavaElement.FIELD, IJavaElement.INITIALIZER, IJavaElement.METHOD};
					if (ReorgUtils.hasElementsOfType(getJavaElements(), types0))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);				 
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_package_decl); 
				
				case IJavaElement.IMPORT_CONTAINER:
					if (ReorgUtils.hasElementsNotOfType(getJavaElements(), IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot); 
					break;
					
				case IJavaElement.IMPORT_DECLARATION:
					if (ReorgUtils.hasElementsNotOfType(getJavaElements(), IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot); 
					break;
				
				case IJavaElement.FIELD://fall thru
				case IJavaElement.INITIALIZER://fall thru
				case IJavaElement.METHOD://fall thru
					return recursiveVerifyDestination(destination.getParent());
				
				case IJavaElement.TYPE:
					int[] types1= new int[]{IJavaElement.IMPORT_DECLARATION, IJavaElement.IMPORT_CONTAINER, IJavaElement.PACKAGE_DECLARATION};
					if (ReorgUtils.hasElementsOfType(getJavaElements(), types1))
						return recursiveVerifyDestination(destination.getParent());
					break;
			}
				
			return new RefactoringStatus();
		}
		
		public boolean canChildrenBeDestinations(IResource resource) {
			return false;
		}
		public boolean canElementBeDestination(IResource resource) {
			return false;
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fJavaElements.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= JavaRefactoringDescriptor.JAR_REFACTORABLE | JavaRefactoringDescriptor.JAR_IMPORTABLE | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JavaRefactoringDescriptorComment comment= new JavaRefactoringDescriptorComment(project, this, header);
			final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_MEMBERS, new Integer(fJavaElements.length).toString());
			for (int offset= 0; offset < fJavaElements.length; offset++)
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fJavaElements[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		protected abstract String getHeaderPattern();
		protected abstract String getDescriptionPlural();
		protected abstract String getDescriptionSingular();
		protected abstract String getProcessorId();

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fJavaElements.length; index++) {
				if (result == null)
					result= fJavaElements[index].getJavaProject().getProject();
				else if (!result.equals(fJavaElements[index].getJavaProject().getProject()))
					return null;
			}
			return result;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}
	}

	private static abstract class PackageFragmentRootsReorgPolicy extends ReorgPolicy {

		private IPackageFragmentRoot[] fPackageFragmentRoots;

		public IJavaElement[] getJavaElements(){
			return fPackageFragmentRoots;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}
		
		public IPackageFragmentRoot[] getRoots() {
			return fPackageFragmentRoots;
		}

		public PackageFragmentRootsReorgPolicy(IPackageFragmentRoot[] roots){
			Assert.isNotNull(roots);
			fPackageFragmentRoots= roots;
		}
			
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			for (int i= 0; i < fPackageFragmentRoots.length; i++) {
				if (! (ReorgUtils.isSourceFolder(fPackageFragmentRoots[i])
						|| (fPackageFragmentRoots[i].isArchive()
								&& ! fPackageFragmentRoots[i].isExternal()))) return false;
			}
			if (ReorgUtils.containsLinkedResources(fPackageFragmentRoots)) 
				return false;					
			return true;
		}
	
		public boolean canChildrenBeDestinations(IJavaElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaElement.JAVA_MODEL :
				case IJavaElement.JAVA_PROJECT :
					return true;
				default :
					return false;
			}
		}
		public boolean canChildrenBeDestinations(IResource resource) {
			return false;
		}
		public boolean canElementBeDestination(IJavaElement javaElement) {
			return javaElement.getElementType() == IJavaElement.JAVA_PROJECT;
		}
		public boolean canElementBeDestination(IResource resource) {
			return false;
		}
		
		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2proj); 
		}
			
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot1); 
			if (javaElement instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel); 
			if (! (javaElement instanceof IJavaProject || javaElement instanceof IPackageFragmentRoot))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2proj); 
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2writable); 
			if (ReorgUtils.isPackageFragmentRoot(javaElement.getJavaProject()))
				//TODO: adapt message to archives:
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2nosrc); 
			return new RefactoringStatus();
		}
	
		protected IJavaProject getDestinationJavaProject(){
			return getDestinationAsJavaProject(getJavaElementDestination());
		}
	
		private IJavaProject getDestinationAsJavaProject(IJavaElement javaElementDestination) {
			if (javaElementDestination == null)
				return null;
			else
				return javaElementDestination.getJavaProject();
		}

		protected IPackageFragmentRoot[] getPackageFragmentRoots(){
			return fPackageFragmentRoots;
		}
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			 RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
			 confirmOverwriting(reorgQueries);
			 return status;
		}

		private void confirmOverwriting(IReorgQueries reorgQueries) {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackageFragmentRoots(fPackageFragmentRoots);
			IJavaProject javaProject= getDestinationJavaProject();
			oh.confirmOverwriting(reorgQueries, javaProject);
			fPackageFragmentRoots= oh.getPackageFragmentRootsWithoutUnconfirmedOnes();
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fPackageFragmentRoots.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JavaRefactoringDescriptorComment comment= new JavaRefactoringDescriptorComment(project, this, header);
			final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_ROOTS, new Integer(fPackageFragmentRoots.length).toString());
			for (int offset= 0; offset < fPackageFragmentRoots.length; offset++)
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fPackageFragmentRoots[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		protected abstract String getHeaderPattern();
		protected abstract String getDescriptionPlural();
		protected abstract String getDescriptionSingular();
		protected abstract String getProcessorId();

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fPackageFragmentRoots.length; index++) {
				if (result == null)
					result= fPackageFragmentRoots[index].getJavaProject().getProject();
				else if (!result.equals(fPackageFragmentRoots[index].getJavaProject().getProject()))
					return null;
			}
			return result;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}
	}

	private static abstract class PackagesReorgPolicy extends ReorgPolicy {
		private IPackageFragment[] fPackageFragments;
	
		public IJavaElement[] getJavaElements(){
			return fPackageFragments;
		}
		
		public IResource[] getResources() {
			return new IResource[0];
		}
		
		protected IPackageFragment[] getPackages(){
			return fPackageFragments;
		}

		public PackagesReorgPolicy(IPackageFragment[] packageFragments){
			Assert.isNotNull(packageFragments);
			fPackageFragments= packageFragments;
		}

		public boolean canEnable() throws JavaModelException {
			for (int i= 0; i < fPackageFragments.length; i++) {
				if (JavaElementUtil.isDefaultPackage(fPackageFragments[i]) || 
					fPackageFragments[i].isReadOnly())
					return false;
			}
			if (ReorgUtils.containsLinkedResources(fPackageFragments))
				return false;
			return true;
		}

		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_packages); 
		}
		
		protected IPackageFragmentRoot getDestinationAsPackageFragmentRoot() throws JavaModelException {
			return getDestinationAsPackageFragmentRoot(getJavaElementDestination());
		}
		
		public boolean canChildrenBeDestinations(IJavaElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaElement.JAVA_MODEL :
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.PACKAGE_FRAGMENT_ROOT : //can be nested (with exclusion filters)
					return true;
				default :
					return false;
			}
		}
		public boolean canChildrenBeDestinations(IResource resource) {
			return false;
		}
		public boolean canElementBeDestination(IJavaElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					return true;
				default :
					return false;
			}
		}
		public boolean canElementBeDestination(IResource resource) {
			return false;
		}
		
		private IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaElement javaElement) throws JavaModelException {
			if (javaElement == null)
				return null;
				
			if (javaElement instanceof IPackageFragmentRoot)
				return (IPackageFragmentRoot) javaElement;

			if (javaElement instanceof IPackageFragment){
				IPackageFragment pack= (IPackageFragment)javaElement;
				if (pack.getParent() instanceof IPackageFragmentRoot)
					return (IPackageFragmentRoot) pack.getParent();
			}

			if (javaElement instanceof IJavaProject)
				return ReorgUtils.getCorrespondingPackageFragmentRoot((IJavaProject) javaElement);				
			return null;
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot1); 
			if (javaElement instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel); 
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot(javaElement);
			if (! ReorgUtils.isSourceFolder(destRoot))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_packages); 
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			RefactoringStatus refactoringStatus= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwritting(reorgQueries);
			return refactoringStatus;
		}
		
		private void confirmOverwritting(IReorgQueries reorgQueries) throws JavaModelException {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackages(fPackageFragments);
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot();
			oh.confirmOverwriting(reorgQueries, destRoot);
			fPackageFragments= oh.getPackagesWithoutUnconfirmedOnes();
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fPackageFragments.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= JavaRefactoringDescriptor.JAR_REFACTORABLE | JavaRefactoringDescriptor.JAR_IMPORTABLE | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JavaRefactoringDescriptorComment comment= new JavaRefactoringDescriptorComment(project, this, header);
			final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_FRAGMENTS, new Integer(fPackageFragments.length).toString());
			for (int offset= 0; offset < fPackageFragments.length; offset++)
				arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fPackageFragments[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		protected abstract String getHeaderPattern();
		protected abstract String getDescriptionPlural();
		protected abstract String getDescriptionSingular();
		protected abstract String getProcessorId();

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fPackageFragments.length; index++) {
				if (result == null)
					result= fPackageFragments[index].getJavaProject().getProject();
				else if (!result.equals(fPackageFragments[index].getJavaProject().getProject()))
					return null;
			}
			return result;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}
	}

	private static class CopySubCuElementsPolicy extends SubCuElementReorgPolicy implements ICopyPolicy {

		private static final String POLICY_COPY_MEMBERS= "org.eclipse.jdt.ui.copyMembers"; //$NON-NLS-1$

		private CopyModifications fModifications;
		private ReorgExecutionLog fReorgExecutionLog;
		
		CopySubCuElementsPolicy(IJavaElement[] javaElements){
			super(javaElements);
		}
		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			
			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			CopyArguments args= new CopyArguments(getJavaElementDestination(), fReorgExecutionLog);
			IJavaElement[] javaElements= getJavaElements();
			for (int i= 0; i < javaElements.length; i++) {
				fModifications.copy(javaElements[i], args, null);
			}
			return fModifications;
		}

		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) throws JavaModelException {
			try {
				CompilationUnit sourceCuNode= createSourceCuNode();
				ICompilationUnit targetCu= getDestinationCu();
				CompilationUnitRewrite targetRewriter= new CompilationUnitRewrite(targetCu);
				IJavaElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], targetRewriter, sourceCuNode, targetRewriter.getRoot());
				}
				return createCompilationUnitChange(targetCu, targetRewriter);
			} catch (JavaModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private CompilationUnit createSourceCuNode(){
			Assert.isTrue(getSourceCu() != null || getSourceClassFile() != null);
			Assert.isTrue(getSourceCu() == null || getSourceClassFile() == null);
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			if (getSourceCu() != null)
				parser.setSource(getSourceCu());
			else
				parser.setSource(getSourceClassFile());
			return (CompilationUnit) parser.createAST(null);
		}

		private IClassFile getSourceClassFile() {
			//all have a common parent, so all must be in the same classfile
			//we checked before that the array in not null and not empty
			return (IClassFile) getJavaElements()[0].getAncestor(IJavaElement.CLASS_FILE);
		}
			
		public boolean canEnable() throws JavaModelException {
			return 	super.canEnable() && 
					(getSourceCu() != null || getSourceClassFile() != null);
		}
			
		public IFile[] getAllModifiedFiles() {
			return ReorgUtils.getFiles(new IResource[]{ReorgUtils.getResource(getDestinationCu())});
		}

		public String getPolicyId() {
			return POLICY_COPY_MEMBERS;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_elements_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_elements_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_elements_header;
		}

		protected String getProcessorId() {
			return JavaCopyProcessor.ID_COPY;
		}
	}
	private static class CopyFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements ICopyPolicy{

		private static final String POLICY_COPY_RESOURCE= "org.eclipse.jdt.ui.copyResources"; //$NON-NLS-1$

		private CopyModifications fModifications;
		private ReorgExecutionLog fReorgExecutionLog;
		
		CopyFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			super(files, folders, cus);
		}
		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			CopyArguments jArgs= new CopyArguments(getDestination(), fReorgExecutionLog);
			CopyArguments rArgs= new CopyArguments(getDestinationAsContainer(), fReorgExecutionLog);
			ICompilationUnit[] cus= getCus();
			for (int i= 0; i < cus.length; i++) {
				fModifications.copy(cus[i], jArgs, rArgs);
			}
			IResource[] resources= ReorgUtils.union(getFiles(), getFolders());
			for (int i= 0; i < resources.length; i++) {
				fModifications.copy(resources[i], rArgs);
			}
			return fModifications;
		}
		private Object getDestination() {
			Object result= getDestinationAsPackageFragment();
			if (result != null)
				return result;
			return getDestinationAsContainer();
		}
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			IFile[] file= getFiles();
			IFolder[] folders= getFolders();
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", cus.length + file.length + folders.length); //$NON-NLS-1$
			NewNameProposer nameProposer= new NewNameProposer();
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_copy); 
			composite.markAsSynthetic();
			for (int i= 0; i < cus.length; i++) {
				composite.add(createChange(cus[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < file.length; i++) {
				composite.add(createChange(file[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < folders.length; i++) {
				composite.add(createChange(folders[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private Change createChange(ICompilationUnit unit, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return copyCuToPackage(unit, pack, nameProposer, copyQueries);
			IContainer container= getDestinationAsContainer();
			return copyFileToContainer(unit, container, nameProposer, copyQueries);
		}
	
		private static Change copyFileToContainer(ICompilationUnit cu, IContainer dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource resource= ReorgUtils.getResource(cu);
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}

		private Change createChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IContainer dest= getDestinationAsContainer();
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}
			
		private static Change createCopyResourceChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries, IContainer destination) {
			if (resource == null || destination == null)
				return new NullChange();
			INewNameQuery nameQuery;
			String name= nameProposer.createNewName(resource, destination);
			if (name == null)
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewResourceNameQuery(resource, name);
			return new CopyResourceChange(resource, destination, nameQuery);
		}

		private static Change copyCuToPackage(ICompilationUnit cu, IPackageFragment dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
			IResource res= ReorgUtils.getResource(cu);
			if (res != null && res.isLinked()){
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return copyFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest), nameProposer, copyQueries);
			}
		
			String newName= nameProposer.createNewName(cu, dest);
			Change simpleCopy= new CopyCompilationUnitChange(cu, dest, copyQueries.createStaticQuery(newName));
			if (newName == null || newName.equals(cu.getElementName()))
				return simpleCopy;
		
			try {
				IPath newPath= ResourceUtil.getResource(cu).getParent().getFullPath().append(newName);				
				INewNameQuery nameQuery= copyQueries.createNewCompilationUnitNameQuery(cu, newName);
				return new CreateCopyOfCompilationUnitChange(newPath, cu.getSource(), cu, nameQuery);
			} catch(CoreException e) {
				return simpleCopy; //fallback - no ui here
			}
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_description_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_description_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_header;
		}

		public String getPolicyId() {
			return POLICY_COPY_RESOURCE;
		}

		protected String getProcessorId() {
			return JavaCopyProcessor.ID_COPY;
		}
	}
	private static class CopyPackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements ICopyPolicy{

		private static final String POLICY_COPY_ROOTS= "org.eclipse.jdt.ui.copyRoots"; //$NON-NLS-1$

		private CopyModifications fModifications;
		private ReorgExecutionLog fReorgExecutionLog;
		
		public CopyPackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
			super(roots);
		}
		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			
			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			CopyArguments javaArgs= new CopyArguments(getDestinationJavaProject(), fReorgExecutionLog);
			CopyArguments resourceArgs= new CopyArguments(getDestinationJavaProject().getProject(), fReorgExecutionLog);
			IPackageFragmentRoot[] roots= getRoots();
			for (int i= 0; i < roots.length; i++) {
				fModifications.copy(roots[i], javaArgs, resourceArgs);
			}
			return fModifications;
		}
		
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length); //$NON-NLS-1$
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_copy_source_folder); 
			composite.markAsSynthetic();
			IJavaProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination, nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}
		
		private Change createChange(IPackageFragmentRoot root, IJavaProject destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource res= root.getResource();
			IProject destinationProject= destination.getProject();
			String newName= nameProposer.createNewName(res, destinationProject);
			INewNameQuery nameQuery;
			if (newName == null )
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewPackageFragmentRootNameQuery(root, newName);
			//TODO fix the query problem
			return new CopyPackageFragmentRootChange(root, destinationProject, nameQuery,  null);
		}

		public String getPolicyId() {
			return POLICY_COPY_ROOTS;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_roots_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_roots_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_roots_header;
		}

		protected String getProcessorId() {
			return JavaCopyProcessor.ID_COPY;
		}
	}
	private static class CopyPackagesPolicy extends PackagesReorgPolicy implements ICopyPolicy{

		private static final String POLICY_COPY_PACKAGES= "org.eclipse.jdt.ui.copyPackages"; //$NON-NLS-1$

		private CopyModifications fModifications;
		private ReorgExecutionLog fReorgExecutionLog;
		
		public CopyPackagesPolicy(IPackageFragment[] packageFragments){
			super(packageFragments);
		}
		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			
			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			IPackageFragmentRoot destination= getDestinationAsPackageFragmentRoot();
			CopyArguments javaArgs= new CopyArguments(destination, fReorgExecutionLog);
			CopyArguments resourceArgs= new CopyArguments(destination.getResource(), fReorgExecutionLog);
			IPackageFragment[] packages= getPackages();
			for (int i= 0; i < packages.length; i++) {
				fModifications.copy(packages[i], javaArgs, resourceArgs);
			}
			return fModifications;
		}
		public Change createChange(IProgressMonitor pm, INewNameQueries newNameQueries) throws JavaModelException {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length); //$NON-NLS-1$
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_copy_package); 
			composite.markAsSynthetic();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				composite.add(createChange(fragments[i], root, nameProposer, newNameQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}
		private Change createChange(IPackageFragment pack, IPackageFragmentRoot destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			String newName= nameProposer.createNewName(pack, destination);
			if (newName == null || JavaConventions.validatePackageName(newName).getSeverity() < IStatus.ERROR){
				INewNameQuery nameQuery;
				if (newName == null)
					nameQuery= copyQueries.createNullQuery();
				else
					nameQuery= copyQueries.createNewPackageNameQuery(pack, newName);
				return new CopyPackageChange(pack, destination, nameQuery);
			} else {
				if (destination.getResource() instanceof IContainer){
					IContainer dest= (IContainer)destination.getResource();
					IResource res= pack.getResource();
					INewNameQuery nameQuery= copyQueries.createNewResourceNameQuery(res, newName);
					return new CopyResourceChange(res, dest, nameQuery);
				} else {
					return new NullChange();
				}
			}	
		}

		public String getPolicyId() {
			return POLICY_COPY_PACKAGES;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_packages_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_package_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_packages_header;
		}

		protected String getProcessorId() {
			return JavaCopyProcessor.ID_COPY;
		}
	}
	private static class NoCopyPolicy extends ReorgPolicy implements ICopyPolicy{
		public boolean canEnable() throws JavaModelException {
			return false;
		}
		public ReorgExecutionLog getReorgExecutionLog() {
			return null;
		}
		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noCopying); 
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noCopying); 
		}
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			return new NullChange();
		}
		public IResource[] getResources() {
			return new IResource[0];
		}
		public IJavaElement[] getJavaElements() {
			return new IJavaElement[0];
		}
		public ChangeDescriptor getDescriptor() {
			return null;
		}
		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}
		public String getPolicyId() {
			return "no_copy"; //$NON-NLS-1$
		}
	}
	private static class NewNameProposer{
		private final Set fAutoGeneratedNewNames= new HashSet(2);
			
		public String createNewName(ICompilationUnit cu, IPackageFragment destination){
			if (isNewNameOk(destination, cu.getElementName()))
				return null;
			if (! ReorgUtils.isParentInWorkspaceOrOnDisk(cu, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_cu_copyOf1, 
								cu.getElementName());
				else	
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_cu_copyOfMore, 
								new String[]{String.valueOf(i), cu.getElementName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return removeTrailingJava(newName);
				}
				i++;
			}
		}
		private static String removeTrailingJava(String name) {
			return JavaCore.removeJavaLikeExtension(name);
		}
		public String createNewName(IResource res, IContainer destination){
			if (isNewNameOk(destination, res.getName()))
				return null;
			if (! ReorgUtils.isParentInWorkspaceOrOnDisk(res, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_resource_copyOf1, 
								res.getName());
				else
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_resource_copyOfMore, 
								new String[]{String.valueOf(i), res.getName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}	
		}
	
		public String createNewName(IPackageFragment pack, IPackageFragmentRoot destination){
			if (isNewNameOk(destination, pack.getElementName()))
				return null;
			if (! ReorgUtils.isParentInWorkspaceOrOnDisk(pack, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_package_copyOf1, 
								pack.getElementName());
				else
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_package_copyOfMore, 
								new String[]{String.valueOf(i), pack.getElementName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}	
		}
		private static boolean isNewNameOk(IPackageFragment dest, String newName) {
			return ! dest.getCompilationUnit(newName).exists();
		}
	
		private static boolean isNewNameOk(IContainer container, String newName) {
			return container.findMember(newName) == null;
		}

		private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
			return ! root.getPackageFragment(newName).exists() ;
		}
	}

	private static class MovePackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements IMovePolicy{

		private static final String POLICY_MOVE_ROOTS= "org.eclipse.jdt.ui.moveRoots"; //$NON-NLS-1$

		private MoveModifications fModifications;
		
		MovePackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
			super(roots);
		}
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			
			fModifications= new MoveModifications();
			IJavaProject destination= getDestinationJavaProject();
			boolean updateReferences= canUpdateReferences() && getUpdateReferences();
			if (destination != null) {
				IPackageFragmentRoot[] roots= getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					fModifications.move(roots[i], new MoveArguments(destination, updateReferences));
				}
			}
			return fModifications;
		}
		
		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length); //$NON-NLS-1$
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move_source_folder); 
			composite.markAsSynthetic();
			IJavaProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		private Change createChange(IPackageFragmentRoot root, IJavaProject destination) {
			///XXX fix the query
			return new MovePackageFragmentRootChange(root, destination.getProject(), null);
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			IJavaProject javaProject= getDestinationJavaProject();
			if (isParentOfAny(javaProject, getPackageFragmentRoots()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_element2parent); 
			return superStatus;
		}

		private static boolean isParentOfAny(IJavaProject javaProject, IPackageFragmentRoot[] roots) {
			for (int i= 0; i < roots.length; i++) {
				if (ReorgUtils.isParentInWorkspaceOrOnDisk(roots[i], javaProject)) return true;
			}
			return false;
		}

		public boolean canEnable() throws JavaModelException {
			if (!super.canEnable())
				return false;
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (roots[i].isReadOnly() && !(roots[i].isArchive())) {
					final ResourceAttributes attributes= roots[i].getResource().getResourceAttributes();
					if (attributes == null || attributes.isReadOnly())
						return false;
				}
			}
			return true;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			try{
				RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch(JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}
		
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}
		public boolean isTextualMove() {
			return false;
		}

		public String getPolicyId() {
			return POLICY_MOVE_ROOTS;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_roots_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_roots_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_roots_header;
		}

		protected String getProcessorId() {
			return JavaMoveProcessor.ID_MOVE;
		}
	}
	
	private static class MovePackagesPolicy extends PackagesReorgPolicy implements IMovePolicy {

		private static final String POLICY_MOVE_PACKAGES= "org.eclipse.jdt.ui.movePackages"; //$NON-NLS-1$

		private MoveModifications fModifications;
		
		MovePackagesPolicy(IPackageFragment[] packageFragments){
			super(packageFragments);
		}
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			
			fModifications= new MoveModifications();
			boolean updateReferences= canUpdateReferences() && getUpdateReferences();
			IPackageFragment[] packages= getPackages();
			IPackageFragmentRoot javaDestination= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < packages.length; i++) {
				fModifications.move(packages[i], new MoveArguments(javaDestination, updateReferences));
			}
			return fModifications;
		}  
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			if (isParentOfAny(root, getPackages()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_package2parent); 
			return superStatus;
		}
		
		private static boolean isParentOfAny(IPackageFragmentRoot root, IPackageFragment[] fragments) {
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				if (ReorgUtils.isParentInWorkspaceOrOnDisk(fragment, root)) return true;
			}
			return false;
		}

		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length); //$NON-NLS-1$
			CompositeChange result= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move_package); 
			result.markAsSynthetic();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				result.add(createChange(fragments[i], root));
				pm.worked(1);
				if (pm.isCanceled())
					throw new OperationCanceledException();
			}
			pm.done();
			return result;
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		private Change createChange(IPackageFragment pack, IPackageFragmentRoot destination) {
			return new MovePackageChange(pack, destination);
		}
		
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			try{
				RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch(JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}
		
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}
		public boolean isTextualMove() {
			return false;
		}

		public String getPolicyId() {
			return POLICY_MOVE_PACKAGES;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_packages_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_packages_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_packages_header;
		}

		protected String getProcessorId() {
			return JavaMoveProcessor.ID_MOVE;
		}
	}
	
	private static class MoveFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements IMovePolicy{

		private static final String POLICY_MOVE_RESOURCES= "org.eclipse.jdt.ui.moveResources"; //$NON-NLS-1$

		private boolean fUpdateReferences;
		private boolean fUpdateQualifiedNames;
		private QualifiedNameSearchResult fQualifiedNameSearchResult;
		private String fFilePatterns;
		private TextChangeManager fChangeManager;
		private MoveModifications fModifications;

		MoveFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			super(files, folders, cus);
			fUpdateReferences= true;
			fUpdateQualifiedNames= false;
			fQualifiedNameSearchResult= new QualifiedNameSearchResult();
		}
		
		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			
			fModifications= new MoveModifications();
			IPackageFragment pack= getDestinationAsPackageFragment();
			IContainer container= getDestinationAsContainer();
			Object unitDestination= null;
			if (pack != null)
				unitDestination= pack;
			else
				unitDestination= container;
			
			// don't use fUpdateReferences directly since it is only valid if 
			// canUpdateReferences is true
			boolean updateReferenes= canUpdateReferences() && getUpdateReferences();
			if (unitDestination != null) {
				ICompilationUnit[] units= getCus();
				for (int i= 0; i < units.length; i++) {
					fModifications.move(units[i], new MoveArguments(unitDestination, updateReferenes));
				}
			}
			if (container != null) {
				IFile[] files= getFiles();
				for (int i= 0; i < files.length; i++) {
					fModifications.move(files[i], new MoveArguments(container, updateReferenes));
				}
				IFolder[] folders= getFolders();
				for (int i= 0; i < folders.length; i++) {
					fModifications.move(folders[i], new MoveArguments(container, updateReferenes));
				}
			}
			return fModifications;
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= new ParentChecker(getResources(), getJavaElements()).getCommonParent();
			if (destination.equals(commonParent)) 
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent); 
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent); 
			IPackageFragment destinationAsPackage= getDestinationAsPackageFragment();
			if (destinationAsPackage != null && destinationAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent); 
				
			return superStatus;
		}
		
		protected RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= getCommonParent();
			if (destination.equals(commonParent)) 
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent); 
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent); 
			IJavaElement destinationContainerAsPackage= getDestinationContainerAsJavaElement();
			if (destinationContainerAsPackage != null && destinationContainerAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent); 
			
			return superStatus;
		}
		
		private Object getCommonParent(){
			return new ParentChecker(getResources(), getJavaElements()).getCommonParent();
		}

		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			if (! fUpdateReferences) {
				return createSimpleMoveChange(pm);
			} else {
				return createReferenceUpdatingMoveChange(pm);
			}
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			if (fQualifiedNameSearchResult != null) {
				return fQualifiedNameSearchResult.getSingleChange(Changes.getModifiedFiles(participantChanges));
			} else {
				return null;
			}
		}

		private Change createReferenceUpdatingMoveChange(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2 + (fUpdateQualifiedNames ? 1 : 0)); //$NON-NLS-1$
			try{
				CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move); 
				composite.markAsSynthetic();
				//XX workaround for bug 13558
				//<workaround>
				if (fChangeManager == null){
					fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), new RefactoringStatus()); //TODO: non-CU matches silently dropped
					RefactoringStatus status= Checks.validateModifiesFiles(
						getAllModifiedFiles(), null);
					if (status.hasFatalError())
						fChangeManager= new TextChangeManager();
				}	
				//</workaround>
							
				composite.merge(new CompositeChange(RefactoringCoreMessages.MoveRefactoring_reorganize_elements, fChangeManager.getAllChanges())); 
						
				Change fileMove= createSimpleMoveChange(new SubProgressMonitor(pm, 1));
				if (fileMove instanceof CompositeChange) {
					composite.merge(((CompositeChange)fileMove));		
				} else{
					composite.add(fileMove);
				}
				return composite;
			} finally{
				pm.done();
			}
		}

		private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
			pm.beginTask("", 1);//$NON-NLS-1$
			try{
				if (! fUpdateReferences)
					return new TextChangeManager();
			
				IPackageFragment packageDest= getDestinationAsPackageFragment();
				if (packageDest != null){			
					MoveCuUpdateCreator creator= new MoveCuUpdateCreator(getCus(), packageDest);
					return creator.createChangeManager(new SubProgressMonitor(pm, 1), status);
				} else 
					return new TextChangeManager();
			} finally {
				pm.done();
			}		
		}

		private Change createSimpleMoveChange(IProgressMonitor pm) {
			CompositeChange result= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move); 
			result.markAsSynthetic();
			IFile[] files= getFiles();
			IFolder[] folders= getFolders();
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", files.length + folders.length + cus.length); //$NON-NLS-1$
			for (int i= 0; i < files.length; i++) {
				result.add(createChange(files[i]));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < folders.length; i++) {
				result.add(createChange(folders[i]));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < cus.length; i++) {
				result.add(createChange(cus[i]));
				pm.worked(1);
			}
			pm.done();
			return result;
		}

		private Change createChange(ICompilationUnit cu){
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return moveCuToPackage(cu, pack);
			IContainer container= getDestinationAsContainer();
			if (container == null)
				return new NullChange();
			return moveFileToContainer(cu, container);
		}

		private static Change moveCuToPackage(ICompilationUnit cu, IPackageFragment dest) {
			//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
			IResource resource= ResourceUtil.getResource(cu);
			if (resource != null && resource.isLinked()){
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return moveFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest));
			}
			return new MoveCompilationUnitChange(cu, dest);
		}

		private static Change moveFileToContainer(ICompilationUnit cu, IContainer dest) {
			return new MoveResourceChange(ResourceUtil.getResource(cu), dest);
		}

		private Change createChange(IResource res) {
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null)
				return new NullChange();
			return new MoveResourceChange(res, destinationAsContainer);
		}

		private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaModelException {
			if (!fUpdateQualifiedNames)
				return;
			IPackageFragment destination= getDestinationAsPackageFragment();
			if (destination != null) {
				ICompilationUnit[] cus= getCus();
				pm.beginTask("", cus.length); //$NON-NLS-1$
				pm.subTask(RefactoringCoreMessages.MoveRefactoring_scanning_qualified_names);
				for (int i= 0; i < cus.length; i++) {
					ICompilationUnit cu= cus[i];
					IType[] types= cu.getTypes();
					IProgressMonitor typesMonitor= new SubProgressMonitor(pm, 1);
					typesMonitor.beginTask("", types.length); //$NON-NLS-1$
					for (int j= 0; j < types.length; j++) {
						handleType(types[j], destination, new SubProgressMonitor(typesMonitor, 1));
						if (typesMonitor.isCanceled())
							throw new OperationCanceledException();
					}
					typesMonitor.done();
				}
			}
			pm.done();
		}

		private void handleType(IType type, IPackageFragment destination, IProgressMonitor pm) {
			QualifiedNameFinder.process(fQualifiedNameSearchResult, type.getFullyQualifiedName(),  destination.getElementName() + "." + type.getTypeQualifiedName(), //$NON-NLS-1$
				fFilePatterns, type.getJavaProject().getProject(), pm);
		}	
		
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			try{
				pm.beginTask("", fUpdateQualifiedNames ? 7 : 3); //$NON-NLS-1$
				RefactoringStatus result= new RefactoringStatus();
				confirmMovingReadOnly(reorgQueries);
				fChangeManager= createChangeManager(new SubProgressMonitor(pm, 2), result);
				if (fUpdateQualifiedNames)
					computeQualifiedNameMatches(new SubProgressMonitor(pm, 4));
				result.merge(super.checkFinalConditions(new SubProgressMonitor(pm, 1), context, reorgQueries));
				return result;
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally{
				pm.done();
			}
		}
		
		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
		
		public IFile[] getAllModifiedFiles() {
			Set result= new HashSet();
			result.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
			result.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
			if (getDestinationAsPackageFragment() != null && getUpdateReferences())
				result.addAll(Arrays.asList(ResourceUtil.getFiles(getCus())));
			return (IFile[]) result.toArray(new IFile[result.size()]);
		}
		public boolean hasAllInputSet() {
			return super.hasAllInputSet() && ! canUpdateReferences() && ! canUpdateQualifiedNames();
		}
		public boolean canEnableUpdateReferences() {
			return getCus().length > 0;
		}
		public boolean canUpdateReferences(){
			if (getCus().length == 0)
				return false;
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null && pack.isDefaultPackage())
				return false;
			Object commonParent= getCommonParent();
			if (JavaElementUtil.isDefaultPackage(commonParent))
				return false;
			return true;							
		}
		public boolean getUpdateReferences() {
			return fUpdateReferences;
		}
		public void setUpdateReferences(boolean update) {
			fUpdateReferences= update;
		}
		
		public boolean canEnableQualifiedNameUpdating() {
			return getCus().length > 0 && ! JavaElementUtil.isDefaultPackage(getCommonParent()) ;
		}
		
		public boolean canUpdateQualifiedNames() {
			IPackageFragment pack= getDestinationAsPackageFragment();
			return (canEnableQualifiedNameUpdating() && pack != null && !pack.isDefaultPackage());
		}

		public boolean getUpdateQualifiedNames() {
			return fUpdateQualifiedNames;
		}

		public void setUpdateQualifiedNames(boolean update) {
			fUpdateQualifiedNames= update;
		}

		public String getFilePatterns() {
			return fFilePatterns;
		}

		public void setFilePatterns(String patterns) {
			Assert.isNotNull(patterns);
			fFilePatterns= patterns;
		}
		
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return createQueries.createNewPackageQuery();
		}
		public boolean isTextualMove() {
			return false;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_description_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_description_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_header;
		}

		public String getPolicyId() {
			return POLICY_MOVE_RESOURCES;
		}

		protected String getProcessorId() {
			return JavaMoveProcessor.ID_MOVE;
		}

		protected Map getRefactoringArguments(String project) {
			final Map arguments= new HashMap();
			arguments.putAll(super.getRefactoringArguments(project));
			if (fFilePatterns != null && !"".equals(fFilePatterns)) //$NON-NLS-1$
				arguments.put(ATTRIBUTE_PATTERNS, fFilePatterns);
			arguments.put(ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
			arguments.put(ATTRIBUTE_QUALIFIED, Boolean.valueOf(fUpdateQualifiedNames).toString());
			return arguments;
		}
	}
	
	private static class MoveSubCuElementsPolicy extends SubCuElementReorgPolicy implements IMovePolicy{

		private static final String POLICY_MOVE_RESOURCES= "org.eclipse.jdt.ui.moveMembers"; //$NON-NLS-1$

		MoveSubCuElementsPolicy(IJavaElement[] javaElements){
			super(javaElements);
		}
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			IJavaElement[] elements= getJavaElements();
			for (int i= 0; i < elements.length; i++) {
				IJavaElement parent= destination.getParent();
				while (parent != null) {
					if (parent.equals(elements[i]))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);
					parent= parent.getParent();
				}
			}

			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;
				
			Object commonParent= new ParentChecker(new IResource[0], getJavaElements()).getCommonParent();
			if (destination.equals(commonParent) || Arrays.asList(getJavaElements()).contains(destination))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_element2parent); 
			return superStatus;
		}

		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 3); //$NON-NLS-1$
			try {
				final ICompilationUnit sourceCu= getSourceCu();
				CompilationUnit sourceCuNode= RefactoringASTParser.parseWithASTProvider(sourceCu, false, new SubProgressMonitor(pm, 1));
				CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(sourceCu, sourceCuNode);
				ICompilationUnit destinationCu= getDestinationCu();
				CompilationUnitRewrite targetRewriter;
				if (sourceCu.equals(destinationCu)) {
					targetRewriter= sourceRewriter;
					pm.worked(1);
				} else {
					CompilationUnit destinationCuNode= RefactoringASTParser.parseWithASTProvider(destinationCu, false, new SubProgressMonitor(pm, 1));
					targetRewriter= new CompilationUnitRewrite(destinationCu, destinationCuNode);
				}
				IJavaElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], targetRewriter, sourceRewriter.getRoot(), targetRewriter.getRoot());
				}
				ASTNodeDeleteUtil.markAsDeleted(javaElements, sourceRewriter, null);
				Change targetCuChange= createCompilationUnitChange(destinationCu, targetRewriter);
				if (sourceCu.equals(destinationCu)) {
					return targetCuChange;
				} else {
					CompositeChange result= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move_members); 
					result.markAsSynthetic();
					result.add(targetCuChange);
					if (Arrays.asList(getJavaElements()).containsAll(Arrays.asList(sourceCu.getTypes())))
						result.add(DeleteChangeCreator.createDeleteChange(null, new IResource[0], new ICompilationUnit[] { sourceCu}, RefactoringCoreMessages.ReorgPolicy_move)); 
					else
						result.add(createCompilationUnitChange(sourceCu, sourceRewriter));
					return result;
				}
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally {
				pm.done();
			}
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		public boolean canEnable() throws JavaModelException {
			return super.canEnable() && getSourceCu() != null; //can move only elements from cus
		}

		public IFile[] getAllModifiedFiles() {
			return ReorgUtils.getFiles(new IResource[]{ReorgUtils.getResource(getSourceCu()), ReorgUtils.getResource(getDestinationCu())});
		}
		
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}
		public boolean isTextualMove() {
			return true;
		}

		public String getPolicyId() {
			return POLICY_MOVE_RESOURCES;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_header;
		}

		protected String getProcessorId() {
			return JavaMoveProcessor.ID_MOVE;
		}
	}

	private static class NoMovePolicy extends ReorgPolicy implements IMovePolicy{
		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noMoving); 
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noMoving); 
		}
		public Change createChange(IProgressMonitor pm) {
			return new NullChange();
		}
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}
		public boolean canEnable() throws JavaModelException {
			return false;
		}
		public IResource[] getResources() {
			return new IResource[0];
		}
		public IJavaElement[] getJavaElements() {
			return new IJavaElement[0];
		}
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}
		public boolean isTextualMove() {
			return true;
		}
		public ChangeDescriptor getDescriptor() {
			return null;
		}
		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}
		public String getPolicyId() {
			return "no_move"; //$NON-NLS-1$
		}
	}
	
	private static class ActualSelectionComputer {
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;

		public ActualSelectionComputer(IJavaElement[]  javaElements, IResource[] resources) {
			fJavaElements= javaElements;
			fResources= resources;
		}
		
		public IJavaElement[] getActualJavaElementsToReorg() throws JavaModelException {
			List result= new ArrayList();
			for (int i= 0; i < fJavaElements.length; i++) {
				IJavaElement element= fJavaElements[i];
				if (element == null)
					continue;
				if (ReorgUtils.isDeletedFromEditor(element))
					continue;
				if (element instanceof IType) {
					IType type= (IType)element;
					ICompilationUnit cu= type.getCompilationUnit();
					if (cu != null && type.getDeclaringType() == null && cu.exists() && cu.getTypes().length == 1 && ! result.contains(cu))
						result.add(cu);
					else if (! result.contains(type))
						result.add(type);
				} else if (! result.contains(element)){
					result.add(element);
				}
			}
			return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
		}
		
		public IResource[] getActualResourcesToReorg() {
			Set javaElementSet= new HashSet(Arrays.asList(fJavaElements));	
			List result= new ArrayList();
			for (int i= 0; i < fResources.length; i++) {
				if (fResources[i] == null)
					continue;
				IJavaElement element= JavaCore.create(fResources[i]);
				if (element == null || ! element.exists() || ! javaElementSet.contains(element))
					if (! result.contains(fResources[i]))
							result.add(fResources[i]);
			}
			return (IResource[]) result.toArray(new IResource[result.size()]);

		}
	}
}
