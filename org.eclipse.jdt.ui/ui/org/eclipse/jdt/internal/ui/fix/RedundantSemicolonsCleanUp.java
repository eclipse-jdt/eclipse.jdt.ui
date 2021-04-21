/*******************************************************************************
 * Copyright (c) 2018, 2021 itemis AG (http://www.itemis.eu) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Karsten Thoms (itemis) - initial API and implementation
 *     Red Hat Inc. - copied and modified to replace extraneous semicolons
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes redundant semi-colons from package declarations, field declarations,
 * method declarations, and removes empty statements from blocks.  Does not touch
 * empty statements belonging to loops (e.g. empty for-loop) nor semicolons used
 * in a for-loop statement itself (e.g. for(;;)).
 */
public class RedundantSemicolonsCleanUp extends AbstractMultiFix implements ICleanUpFix {

	private TextEditGroup[] fEditGroups;
	private String fName;
	private ICompilationUnit fCompilationUnit;
	final static Pattern pattern= Pattern.compile("^((\\s*;)+)"); //$NON-NLS-1$

	public RedundantSemicolonsCleanUp() {
		this(Collections.emptyMap());
	}

	public RedundantSemicolonsCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS)) {
			return new String[] { MultiFixMessages.RedundantSemicolonsCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();
		buf.append("enum color {\n"); //$NON-NLS-1$
		buf.append("  red, yellow, green\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS)) {
			buf.append("}\n"); //$NON-NLS-1$
		} else {
			buf.append("};\n"); //$NON-NLS-1$
		}
		buf.append("\npublic class IFoo {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS)) {
			buf.append("  int a= 3;\n"); //$NON-NLS-1$
			buf.append("  public void foo() {}\n"); //$NON-NLS-1$
			buf.append("}\n"); //$NON-NLS-1$
		} else {
			buf.append("  int a= 3;;\n"); //$NON-NLS-1$
			buf.append("  public void foo() {;};\n"); //$NON-NLS-1$
			buf.append("};\n"); //$NON-NLS-1$
		}

		return buf.toString();
	}

	private void searchNode(ASTNode node, String contents, String label, ArrayList<TextEditGroup> textedits) {
		int start= node.getStartPosition();
		int length= node.getLength();

		int trailing = findTrailingSemicolons(contents, start + length);

		if (trailing > 0) {
			ReplaceEdit edit = new ReplaceEdit(start + length, trailing, ""); //$NON-NLS-1$
			textedits.add(new TextEditGroup(label, edit));
		}
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS)) {
			return null;
		}
		ICompilationUnit compilationUnit = (ICompilationUnit)unit.getJavaElement();
		IBuffer buffer= compilationUnit.getBuffer();
		String contents= buffer.getContents();

		String label= MultiFixMessages.RedundantSemicolonsCleanup_description;
		ArrayList<TextEditGroup> textedits= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(PackageDeclaration node) {
				searchNode(node, contents, label, textedits);
				return false;
			}

			@Override
			public boolean visit(FieldDeclaration node) {
				searchNode(node, contents, label, textedits);
				return false;
			}

			@Override
			public boolean visit(MethodDeclaration node) {
				searchNode(node, contents, label, textedits);
				return true;
			}

			@Override
			public boolean visit(EnumDeclaration node) {
				searchNode(node, contents, label, textedits);
				return true;
			}

			@Override
			public boolean visit(TypeDeclaration node) {
				searchNode(node, contents, label, textedits);
				return true;
			}

			@Override
			public boolean visit(RecordDeclaration node) {
				searchNode(node, contents, label, textedits);
				return true;
			}

			@Override
			public boolean visit(EmptyStatement node) {
				ASTNode parent= node.getParent();
				if (parent instanceof Block) {
					int start= node.getStartPosition();
					ReplaceEdit edit= new ReplaceEdit(start, 1, ""); //$NON-NLS-1$
					textedits.add(new TextEditGroup(label, edit));
				}
				return false;
			}

			@Override
			public boolean visit(Block node) {
				if (!(node.getParent() instanceof LambdaExpression)) {
					searchNode(node, contents, label, textedits);
				}
				return true;
			}
		});

		if (textedits.size() > 0) {
			return new RedundantSemicolonsCleanUp(label, unit, textedits.toArray(new TextEditGroup[0]));
		}
		return null;
	}

	private int findTrailingSemicolons(String contents, int startLocation) {
		int i= startLocation;
		Matcher matcher= pattern.matcher(contents.substring(i));
		if (matcher.find(0)) {
			return matcher.end(2);
		}
		return -1;
	}

	private RedundantSemicolonsCleanUp(String name, CompilationUnit compilationUnit, TextEditGroup[] groups) {
		fName= name;
		fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		fEditGroups= groups;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		if (fEditGroups == null || fEditGroups.length == 0)
			return null;

		CompilationUnitChange result= new CompilationUnitChange(fName, fCompilationUnit);
		for (TextEditGroup editGroup : fEditGroups) {
			String groupName= editGroup.getName();
			for (TextEdit edit : editGroup.getTextEdits()) {
				TextChangeCompatibility.addTextEdit(result, groupName, edit);
			}
		}
		return result;
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

}
