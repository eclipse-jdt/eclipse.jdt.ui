package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

public class ReplaceQualifiedTypeFixCore implements IProposableFix {

	boolean isImportFound = false;
	String className;
	IImportDeclaration[] imports;

	public ReplaceQualifiedTypeFixCore(String fullQualifiedName, IImportDeclaration[] imports) {
		String[] names = fullQualifiedName.split("\\.");
		this.imports = imports;
		//Check for length
		this.className = names[names.length-1];
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public String create(ASTNode node, String fullQualifiedName) {
		//class QualifiedNameASTVisitor
		ASTNode rootNode = node.getRoot();
		for(IImportDeclaration cur_import : imports) {
			System.out.println("Cur import: " + cur_import.getElementName());
			String fullString = cur_import.getElementName();
			if(cur_import.getElementName().equals(fullQualifiedName)) {
				isImportFound = true;
			} else if (fullString.contains(className)) {
				System.out.println("Abort");
			}
		}
		ASTVisitor importVisitor = new ASTVisitor() {
			@Override
			public boolean visit(ImportDeclaration node) {
				System.out.println("Node: " + node.getName() + " - Type: " + node.getNodeType());
				System.out.println("FullQualifiedName:" + fullQualifiedName);
				if (node.getName().equals(fullQualifiedName)) {
					isImportFound = true;
				} else if(node.getName().toString().contains(className)) {
					System.out.println("Abort");
				}
				return true;
			}

			@Override
			public boolean visit(VariableDeclarationFragment vstatement) {
				System.out.println("Vstatement: " + vstatement);
				return true;
			}
		};
		rootNode.accept(importVisitor);
		//getAS
		System.out.println(rootNode.getLength());
		if (isImportFound) {
			System.out.println("I found the import...");
			// Need to remove the FQN from the variable decl
		} else {
			// We need to add an import
		}
		System.out.println(fullQualifiedName);
		return null;
	}

	@Override
	public String getDisplayString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

}
