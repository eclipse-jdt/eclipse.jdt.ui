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
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyledText;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.stickyscroll.IStickyLine;
import org.eclipse.ui.texteditor.stickyscroll.IStickyLinesProvider;
import org.eclipse.ui.texteditor.stickyscroll.StickyLine;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.ui.JavaUI;


public class JavaStickyLinesProvider implements IStickyLinesProvider {

	private final static int IGNORE_LINE_INDENTATION= -1;
	private final static Pattern ELSE_PATTERN= Pattern.compile("else[\\s,{]"); //$NON-NLS-1$
	private final static Pattern DO_PATTERN= Pattern.compile("do[\\s,{]"); //$NON-NLS-1$
	private final static Pattern WHILE_PATTERN= Pattern.compile("while[\\s,{]"); //$NON-NLS-1$
	private final static Pattern FOR_PATTERN= Pattern.compile("for[\\s,{]"); //$NON-NLS-1$
	private final static Pattern TRY_PATTERN= Pattern.compile("try[\\s,{]"); //$NON-NLS-1$
	private final static Pattern NEW_PATTERN= Pattern.compile("new[\\s,{]"); //$NON-NLS-1$

	private static Map<ITypeRoot, CompilationUnit> cuMap= new HashMap<>();
	private static Map<ITypeRoot, Long> timeMap= new HashMap<>();

	@Override
	public List<IStickyLine> getStickyLines(ISourceViewer sourceViewer, int lineNumber, StickyLinesProperties properties) {
		final LinkedList<IStickyLine> stickyLines= new LinkedList<>();
		JavaEditor javaEditor= (JavaEditor) properties.editor();
		StyledText textWidget= sourceViewer.getTextWidget();
		ICompilationUnit unit= null;
		int textWidgetLineNumber= mapLineNumberToWidget(sourceViewer, lineNumber);
		if (textWidgetLineNumber < 0) {
			return stickyLines;
		}
		int startIndentation= 0;
		String line = null;
		try {
			line = textWidget.getLine(textWidgetLineNumber);
			startIndentation= getIndentation(line);
			while (startIndentation == IGNORE_LINE_INDENTATION) {
				textWidgetLineNumber--;
				if (textWidgetLineNumber <= 0) {
					break;
				}
				line= textWidget.getLine(textWidgetLineNumber);
				startIndentation= getIndentation(line);
			}
		} catch (IllegalArgumentException e) {
			stickyLines.clear();
		}

		if (textWidgetLineNumber > 0) {
			ITypeRoot typeRoot= getJavaInput(javaEditor);
			ASTNode node= null;
			if (typeRoot != null) {
				WorkingCopyOwner workingCopyOwner= new WorkingCopyOwner() {
				};
				CompilationUnit cu= cuMap.get(typeRoot);
				IDocument document= sourceViewer.getDocument();
				long currTime= getDocumentTimestamp(document);
				if (cu != null && !typeRoot.isReadOnly()) {
					Long oldTime= timeMap.get(typeRoot);
					if (oldTime == null || currTime != oldTime.longValue()) {
						cu= null;
					}
				}
				try {
					if (cu == null) {
						unit= typeRoot.getWorkingCopy(workingCopyOwner, null);
						if (unit != null) {
							cu= convertICompilationUnitToCompilationUnit(unit);
						}
					}
					if (cu == null) {
						return stickyLines;
					}
					cuMap.put(typeRoot, cu);
					timeMap.put(typeRoot, Long.valueOf(currTime));
					node= getASTNode(cu, mapWidgetToLineNumber(sourceViewer, textWidgetLineNumber)+1, line);
					while (node == null && textWidgetLineNumber > 0) {
						line= textWidget.getLine(--textWidgetLineNumber);
						startIndentation= getIndentation(line);
						while (startIndentation == IGNORE_LINE_INDENTATION && textWidgetLineNumber > 0) {
							line= textWidget.getLine(--textWidgetLineNumber);
							startIndentation= getIndentation(line);
						}
						if (textWidgetLineNumber > 0) {
							int position= cu.getPosition(mapWidgetToLineNumber(sourceViewer, textWidgetLineNumber) + 1, startIndentation);
							if (position >= 0) {
								node= getASTNode(cu, mapWidgetToLineNumber(sourceViewer, textWidgetLineNumber)+1, line);
							}
						}
					}
					if (node != null) {
						boolean addStickyLine= false;
						int nodeLineNumber= 0;
						while (node != null) {
							addStickyLine= false;
							switch (node.getNodeType()) {
								case ASTNode.ANNOTATION_TYPE_DECLARATION:
								case ASTNode.TYPE_DECLARATION:
								case ASTNode.ENUM_DECLARATION:
									addStickyLine= true;
									ASTNode name= ((AbstractTypeDeclaration)node).getName();
									nodeLineNumber= cu.getLineNumber(name.getStartPosition());
									break;
								case ASTNode.TYPE_DECLARATION_STATEMENT:
									addStickyLine= true;
									ASTNode typeDeclStmtName= ((TypeDeclarationStatement)node).getDeclaration().getName();
									nodeLineNumber= cu.getLineNumber(typeDeclStmtName.getStartPosition());
									break;
								case ASTNode.METHOD_DECLARATION:
									addStickyLine= true;
									ASTNode methodName= ((MethodDeclaration)node).getName();
									nodeLineNumber= cu.getLineNumber(methodName.getStartPosition());
									break;
								case ASTNode.RECORD_DECLARATION:
									addStickyLine= true;
									ASTNode recordName= ((RecordDeclaration)node).getName();
									nodeLineNumber= cu.getLineNumber(recordName.getStartPosition());
									break;
								case ASTNode.MODULE_DECLARATION:
									addStickyLine= true;
									ASTNode moduleName= ((ModuleDeclaration)node).getName();
									nodeLineNumber= cu.getLineNumber(moduleName.getStartPosition());
									break;
								case ASTNode.LAMBDA_EXPRESSION:
									addStickyLine= true;
									ASTNode lambdaBody= ((LambdaExpression)node).getBody();
									nodeLineNumber= cu.getLineNumber(lambdaBody.getStartPosition());
									break;
								case ASTNode.IF_STATEMENT:
									addStickyLine= true;
									IfStatement ifStmt= (IfStatement)node;
									ASTNode ifExpression= ifStmt.getExpression();
									nodeLineNumber= cu.getLineNumber(ifExpression.getStartPosition());
									Statement elseStmt= ifStmt.getElseStatement();
									if (elseStmt != null) {
										int elseLine= cu.getLineNumber(elseStmt.getStartPosition());
										if (elseLine <= mapWidgetToLineNumber(sourceViewer, textWidgetLineNumber + 1)) {
											Pattern p= ELSE_PATTERN;
											nodeLineNumber= elseLine;
											int lineIndex = mapLineNumberToWidget(sourceViewer, nodeLineNumber - 1);
											if (lineIndex < 0) {
												break;
											}
											String stmtLine= textWidget.getLine(lineIndex);
											Matcher m= p.matcher(stmtLine);
											while (!m.find() && nodeLineNumber > 1) {
												nodeLineNumber--;
												lineIndex = mapLineNumberToWidget(sourceViewer, nodeLineNumber - 1);
												if (lineIndex < 0) {
													continue;
												}
												stmtLine= textWidget.getLine(lineIndex );
												m= p.matcher(stmtLine);
											}
											node= node.getParent();
										}
									}
									while (node.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
										node= node.getParent();
									}
									break;
								case ASTNode.ENHANCED_FOR_STATEMENT:
									addStickyLine= true;
									ASTNode enhancedForExpression= ((EnhancedForStatement)node).getExpression();
									nodeLineNumber= cu.getLineNumber(enhancedForExpression.getStartPosition());
									break;
								case ASTNode.SWITCH_EXPRESSION:
									addStickyLine= true;
									ASTNode switchExpExpression= ((SwitchExpression)node).getExpression();
									nodeLineNumber= cu.getLineNumber(switchExpExpression.getStartPosition());
									break;
								case ASTNode.SWITCH_STATEMENT:
									addStickyLine= true;
									ASTNode switchStmtExpression= ((SwitchStatement)node).getExpression();
									nodeLineNumber= cu.getLineNumber(switchStmtExpression.getStartPosition());
									break;
								case ASTNode.WHILE_STATEMENT:
								case ASTNode.DO_STATEMENT:
								case ASTNode.TRY_STATEMENT:
								case ASTNode.FOR_STATEMENT:
								case ASTNode.ANONYMOUS_CLASS_DECLARATION:
									addStickyLine= true;
									ASTNode bodyProperty= null;
									Pattern pattern= null;
									switch (node.getNodeType()) {
										case ASTNode.DO_STATEMENT:
											bodyProperty= ((DoStatement)node).getBody();
											pattern= DO_PATTERN;
											break;
										case ASTNode.FOR_STATEMENT:
											bodyProperty= ((ForStatement)node).getBody();
											pattern= FOR_PATTERN;
											break;
										case ASTNode.WHILE_STATEMENT:
											bodyProperty= ((WhileStatement)node).getBody();
											pattern= WHILE_PATTERN;
											break;
										case ASTNode.TRY_STATEMENT:
											bodyProperty= ((TryStatement)node).getBody();
											pattern= TRY_PATTERN;
											break;
										case ASTNode.ANONYMOUS_CLASS_DECLARATION:
											bodyProperty= (ASTNode) ((AnonymousClassDeclaration)node).bodyDeclarations().get(0);
											pattern= NEW_PATTERN;
											break;
									}
									if (bodyProperty != null && pattern != null) {
										nodeLineNumber= cu.getLineNumber(bodyProperty.getStartPosition());
										int lineIndex = mapLineNumberToWidget(sourceViewer, nodeLineNumber - 1);
										if (lineIndex < 0) {
											break;
										}
										String stmtLine= textWidget.getLine(lineIndex);
										Matcher m= pattern.matcher(stmtLine);
										while (!m.find() && nodeLineNumber > 1) {
											nodeLineNumber--;
											lineIndex = mapLineNumberToWidget(sourceViewer, nodeLineNumber - 1);
											if (lineIndex < 0) {
												continue;
											}
											stmtLine= textWidget.getLine(lineIndex);
											m= pattern.matcher(stmtLine);
										}
									}
									break;
								case ASTNode.SWITCH_CASE:
								case ASTNode.CASE_DEFAULT_EXPRESSION:
								case ASTNode.CATCH_CLAUSE:
									nodeLineNumber= cu.getLineNumber(node.getStartPosition());
									break;
								default:
									break;
							}
							if (addStickyLine && nodeLineNumber <= lineNumber) {
								stickyLines.addFirst(new StickyLine(nodeLineNumber - 1, sourceViewer));
							}
							if (node.getNodeType() == ASTNode.MODIFIER) {
								Modifier modifier= (Modifier)node;
								startIndentation+= modifier.getLength();
								node= getASTNode(cu, mapWidgetToLineNumber(sourceViewer, textWidgetLineNumber+1), line);
							} else {
								node= node.getParent();
							}
						}
					}
					if (unit != null && !typeRoot.isReadOnly()) {
						unit.discardWorkingCopy();
					}
				} catch (JavaModelException e) {
					// do nothing
				}
			}
		}
		return stickyLines;
	}

	private long getDocumentTimestamp(IDocument document) {
		if (document instanceof AbstractDocument ad) {
			return ad.getModificationStamp();
		}
		return 0;
	}

	private ASTNode getASTNode(CompilationUnit cu, int lineNum, String line) {
		int linePos= cu.getPosition(lineNum, 0);
		if (linePos >= 0) {
			NodeFinder finder= new NodeFinder(cu, linePos, line.length());
			return finder.getCoveringNode();
		}
		return null;
	}

	public static ITypeRoot getJavaInput(IEditorPart part) {
		IEditorInput editorInput= part.getEditorInput();
		if (editorInput != null) {
			IJavaElement input= JavaUI.getEditorInputJavaElement(editorInput);
			if (input instanceof ITypeRoot) {
				return (ITypeRoot) input;
			}
		}
		return null;
	}

	/**
	 * @return the line number in the widget, or -1 if the line number not found
	 */
	private int mapLineNumberToWidget(ISourceViewer sourceViewer, int line) {
		if (sourceViewer instanceof ITextViewerExtension5 extension) {
			return extension.modelLine2WidgetLine(line);
		}
		return line;
	}

	private int mapWidgetToLineNumber(ISourceViewer sourceViewer, int line) {
		if (sourceViewer instanceof ITextViewerExtension5 extension) {
			return extension.widgetLine2ModelLine(line);
		}
		return line;
	}

	private int getIndentation(String line) {
		if (line == null || line.isBlank()) {
			return IGNORE_LINE_INDENTATION;
		}
		return line.length() - line.stripLeading().length();
	}

	private static CompilationUnit convertICompilationUnitToCompilationUnit(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(false);
		return (CompilationUnit) parser.createAST(null);
	}

}
