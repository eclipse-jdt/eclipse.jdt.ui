package junit.ui;

import junit.framework.*;
import junit.util.*;

import java.util.Vector;
import java.lang.reflect.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
 
/**
 * A simple user interface to run tests.
 * Enter the name of a class with a suite method which should return
 * the tests to be run.
 * <pre>
 * Synopsis: java java.ui.TestRunner [TestCase]
 * </pre>
 * TestRunner takes as an optional argument the name of the testcase class to be run.
 */
public class TestRunner extends Object implements TestListener {
	private Frame fFrame;
	private Vector fExceptions;
	private Vector fFailedTests;
	private Thread fRunner;
	private TestResult fTestResult;
	private TestSuiteLoader fTestLoader;
	
	private TraceFrame fTraceFrame;

	private TextField fSuiteField;
	private Button fRun;
	private ProgressBar fProgressIndicator;
	private List fFailureList;
	private Logo fLogo;
	private Label fNumberOfErrors;
	private Label fNumberOfFailures;
	private Label fNumberOfRuns;
	private Button fQuitButton;
	private Button fShowErrorButton;
	private Button fRerunButton;
	private TextField fStatusLine;
	private Panel fPanel;
	
	private static Font PLAIN_FONT= new Font("dialog", Font.PLAIN, 12);
	private static final int GAP= 4;
	private static final String SUITE_METHODNAME= "suite";
	public TestRunner() {
	} 
	private void about() {
		AboutDialog about= new AboutDialog(fFrame);
		about.setModal(true);
		about.setLocation(300, 300);
		about.setVisible(true);
	}
	public void addError(Test test, Throwable t) {
		fNumberOfErrors.setText(Integer.toString(fTestResult.errorCount()));
		appendFailure("Error", test, t);
	}
	public void addFailure(Test test, Throwable t) {
		fNumberOfFailures.setText(Integer.toString(fTestResult.failureCount()));
		appendFailure("Failure", test, t);
	}
	private void addGrid(Panel p, Component co, int x, int y, int w, int fill, double wx, int anchor) {
		GridBagConstraints c= new GridBagConstraints();
		c.gridx= x; c.gridy= y;
		c.gridwidth= w;
		c.anchor= anchor;
		c.weightx= wx;
		c.fill= fill;
		if (fill == GridBagConstraints.BOTH || fill == GridBagConstraints.VERTICAL)
			c.weighty= 1.0;
		c.insets= new Insets(y == 0 ? GAP : 0, x == 0 ? GAP : 0, GAP, GAP);
		p.add(co, c);
	}
	private void appendFailure(String kind, Test test, Throwable t) {
		kind+= ": " + test;
		String msg= t.getMessage();
		if (msg != null) {
			kind+= ":" + StringUtil.truncate(msg, 100); 
		}
		fFailureList.add(kind);
		fExceptions.addElement(t);
		fFailedTests.addElement(test);
	}
	/**
	 * Creates the JUnit menu. Clients override this
	 * method to add additional menu items.
	 */
	protected Menu createJUnitMenu() {
		Menu menu = new Menu("JUnit");
		MenuItem mi = new MenuItem("About...");
		mi.addActionListener(
		    new ActionListener() {
		        public void actionPerformed(ActionEvent event) {
		            about();
		        }
		    }
		);
		menu.add(mi);
		
		menu.addSeparator();
		mi = new MenuItem("Exit");
		mi.addActionListener(
		    new ActionListener() {
		        public void actionPerformed(ActionEvent event) {
		            System.exit(0);
		        }
		    }
		);
		menu.add(mi);

		return menu;
	}
	protected void createMenus(MenuBar mb) {
		mb.add(createJUnitMenu());
	}
	protected TestResult createTestResult() {
		return new TestResult();
	}
	protected Frame createUI(String suiteName) {	
		Frame frame= new Frame("Run Test Suite");
		Image icon= loadFrameIcon();	
		if (icon != null)
			frame.setIconImage(icon);

		frame.setLayout(new BorderLayout(0, 0));
		frame.setBackground(SystemColor.control);
		final Frame finalFrame= frame;
		
		frame.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					finalFrame.dispose();
					System.exit(0);
				}
			}
		); 

		MenuBar mb = new MenuBar();
		createMenus(mb);
		frame.setMenuBar(mb);
		
		//---- first section
		Label suiteLabel= new Label("Enter the name of the TestCase class:");

		fSuiteField= new TextField(suiteName != null ? suiteName : "");
		fSuiteField.selectAll();
		fSuiteField.requestFocus();
		fSuiteField.setFont(PLAIN_FONT);
		fSuiteField.setColumns(40);
		fSuiteField.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					runSuite();
				}
			}
		);
		fSuiteField.addTextListener(
			new TextListener() {
				public void textValueChanged(TextEvent e) {
					fRun.setEnabled(fSuiteField.getText().length() > 0);
					fStatusLine.setText("");
				}
			}
		);
		
		Label suiteLabel2= new Label(".suite()");
		
		fRun= new Button("Run");
		fRun.setEnabled(false);
		fRun.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					runSuite();
				}
			}
		);
	

		//---- second section
		Label progressLabel= new Label("Progress:");
		fProgressIndicator= new ProgressBar();	

		//---- third section
		fNumberOfErrors= new Label("0000", Label.RIGHT);
		fNumberOfErrors.setText("0");
		fNumberOfErrors.setFont(PLAIN_FONT);
	
		fNumberOfFailures= new Label("0000", Label.RIGHT);
		fNumberOfFailures.setText("0");
		fNumberOfFailures.setFont(PLAIN_FONT);
	
		fNumberOfRuns= new Label("0000", Label.RIGHT);
		fNumberOfRuns.setText("0");
		fNumberOfRuns.setFont(PLAIN_FONT);
	
		Panel numbersPanel= new Panel(new FlowLayout());
		numbersPanel.add(new Label("Runs:"));			numbersPanel.add(fNumberOfRuns);
		numbersPanel.add(new Label("   Errors:"));		numbersPanel.add(fNumberOfErrors);
		numbersPanel.add(new Label("   Failures:"));	numbersPanel.add(fNumberOfFailures);

	
		//---- fourth section
		Label failureLabel= new Label("Errors and Failures:");
		
		fFailureList= new List(6);
		fFailureList.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						showErrorTrace();
				}
			}
		);
		fFailureList.addItemListener(
			new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					fShowErrorButton.setEnabled(isErrorSelected());
					fRerunButton.setEnabled(isErrorSelected());
				}
			}
		);

		fShowErrorButton= new Button("Show...");
		fShowErrorButton.setEnabled(false);
		fShowErrorButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showErrorTrace();
				}
			}
		);

		fRerunButton= new Button("Run");
		fRerunButton.setEnabled(false);
		fRerunButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					rerun();
				}
			}
		);

		Panel failedPanel= new Panel(new GridLayout(0, 1, 0, 2));
		failedPanel.add(fShowErrorButton);
		failedPanel.add(fRerunButton);
		
		//---- fifth section
		fStatusLine= new TextField();
		fStatusLine.setFont(PLAIN_FONT);
		fStatusLine.setEditable(false);
		fStatusLine.setForeground(Color.red);

		fQuitButton= new Button("Exit");
		fQuitButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			}
		);
	
		// ---------
		fLogo= new Logo();
	
		//---- overall layout
		Panel panel= new Panel(new GridBagLayout());
		fPanel= panel;
	
		addGrid(panel, suiteLabel,		 0, 0, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		
		addGrid(panel, fSuiteField, 	 0, 1, 1, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, suiteLabel2, 	 1, 1, 1, GridBagConstraints.NONE, 			0.0, GridBagConstraints.WEST);
		addGrid(panel, fRun, 			 2, 1, 1, GridBagConstraints.HORIZONTAL, 	0.0, GridBagConstraints.CENTER);

		addGrid(panel, progressLabel, 	 0, 2, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, fProgressIndicator, 0, 3, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, fLogo, 			 2, 3, 1, GridBagConstraints.NONE, 			0.0, GridBagConstraints.NORTH);

		addGrid(panel, numbersPanel,	 0, 4, 2, GridBagConstraints.NONE, 			0.0, GridBagConstraints.CENTER);

		addGrid(panel, failureLabel, 	 0, 5, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, fFailureList, 	 0, 6, 2, GridBagConstraints.BOTH, 			1.0, GridBagConstraints.WEST);
		addGrid(panel, failedPanel, 2, 6, 1, GridBagConstraints.HORIZONTAL, 	0.0, GridBagConstraints.CENTER);
		
		addGrid(panel, fStatusLine, 	 0, 7, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.CENTER);
		addGrid(panel, fQuitButton, 	 2, 7, 1, GridBagConstraints.HORIZONTAL, 	0.0, GridBagConstraints.CENTER);
		
		frame.add(panel);
		
		frame.pack();
		return frame;
	}
	public void endTest(Test test) {
		setLabelValue(fNumberOfRuns, fTestResult.runCount());
		fProgressIndicator.step(fTestResult.wasSuccessful());
	}
	private Test getTest(String suiteClassName) {
		if (suiteClassName.length() <= 0) {
			fStatusLine.setText("");
			return null;
		}
		
		Class testClass= null;
		try {
			testClass= loadSuiteClass(suiteClassName);
		} catch(Exception e) {
			runFailed("Class \""+suiteClassName+"\" not found");
			return null;
		}
			
		Method suiteMethod= null;
		try {
			suiteMethod= testClass.getMethod(SUITE_METHODNAME, new Class[0]);
	 	} catch(Exception e) {
	 		// try to extract a test suite automatically
			fStatusLine.setText("");			
			return new TestSuite(testClass);
		}
	
		Test test= null;
		try {
			test= (Test)suiteMethod.invoke(null, new Class[0]); // static method
			if (test == null)
				return test;
		} catch(Exception e) {
			runFailed("Could not invoke the suite() method");
			return null;
		}
		fStatusLine.setText("");
		return test;
	}
	private boolean isErrorSelected() {
		return fFailureList.getSelectedIndex() != -1;
	}
	private Image loadFrameIcon() {
		Toolkit toolkit= Toolkit.getDefaultToolkit();
		try {
			java.net.URL url= getClass().getResource("smalllogo.gif");
			return toolkit.createImage((ImageProducer) url.getContent());
		} catch (Exception ex) {
		}
		return null;
	}
	protected Class loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
		return fTestLoader.load(suiteClassName);
	}
	/**
	 * main entrypoint
	 */
	public static void main(String[] args) {
		new TestRunner().start(args, new StandardTestSuiteLoader());
	} 
	private void rerun() {
		int index= fFailureList.getSelectedIndex();
		if (index == -1)
			return;
	
		Test test= (Test)fFailedTests.elementAt(index);
		if (!(test instanceof TestCase)) {
			showInfo("Could not reload "+ test.toString());
			return;
		}
		Test reloadedTest= null;
		try {
			Class reloadedTestClass= fTestLoader.reload(test.getClass());
			Class[] classArgs= { String.class };
			Constructor constructor= reloadedTestClass.getConstructor(classArgs);
			Object[] args= new Object[]{((TestCase)test).name()};
			reloadedTest=(Test)constructor.newInstance(args);
		} catch(Exception e) {
			showInfo("Could not reload "+ test.toString());
			return;
		}
		TestResult result= new TestResult();
		reloadedTest.run(result);
		
		String message= reloadedTest.toString();
		if(result.wasSuccessful())
			showInfo(message+" was successful");
		else if (result.errorCount() == 1)
			showStatus(message+" had an error");
		else
			showStatus(message+" had a failure");
	}
	protected void reset() {
		setLabelValue(fNumberOfErrors, 0);
		setLabelValue(fNumberOfFailures, 0);
		setLabelValue(fNumberOfRuns, 0);
		fProgressIndicator.reset();
		fShowErrorButton.setEnabled(false);
		fRerunButton.setEnabled(false);
		fFailureList.removeAll();
		fExceptions= new Vector(10);
		fFailedTests= new Vector(10);
	}
	/**
	 * runs a suite.
	 * @deprecated use runSuite() instead
	 */
	public void run() {
		runSuite();
	}
	private void runFailed(String message) {
		showStatus(message);
		fRun.setLabel("Run");
		fRunner= null;
	}
	synchronized public void runSuite() {
		if (fRunner != null) {
			fTestResult.stop();
		} else {
			fRun.setLabel("Stop");
			showInfo("Initializing...");
			reset();
			
			showInfo("Load Test Case...");
			final Test testSuite= getTest(fSuiteField.getText());
			if (testSuite != null) {
				fRunner= new Thread() {
					public void run() {
						fTestResult= createTestResult();
						fTestResult.addListener(TestRunner.this);
						fProgressIndicator.start(testSuite.countTestCases());
						showInfo("Running...");
					
						long startTime= System.currentTimeMillis();
						testSuite.run(fTestResult);
						
						if (fTestResult.shouldStop()) {
							showStatus("Stopped");
						} else {
							long endTime= System.currentTimeMillis();
							long runTime= endTime-startTime;
							showInfo("Finished: " + StringUtil.elapsedTimeAsString(runTime) + " seconds");
						}
						fTestResult= null;
						fRun.setLabel("Run");
						fRunner= null;
					}
				};
				fRunner.start();
			}
		}
	}
	private void setLabelValue(Label label, int value) {
		label.setText(Integer.toString(value));
	}
	public void setSuiteName(String suite) {
		fSuiteField.setText(suite);
	}
	private void showErrorTrace() {
		int index= fFailureList.getSelectedIndex();
		if (index == -1)
			return;
	
		Throwable t= (Throwable) fExceptions.elementAt(index);
		if (fTraceFrame == null) {
			fTraceFrame= new TraceFrame();
			fTraceFrame.setLocation(100, 100);
	   	}
		fTraceFrame.showTrace(t);
		fTraceFrame.setVisible(true);
	}
	private void showInfo(String message) {
		fStatusLine.setFont(PLAIN_FONT);
		fStatusLine.setForeground(Color.black);
		fStatusLine.setText(message);
	}
	private void showStatus(String status) {
		fStatusLine.setFont(PLAIN_FONT);
		fStatusLine.setForeground(Color.red);
		fStatusLine.setText(status);
	}
	/**
	 * Starts the TestRunner
	 */
	public void start(String[] args, TestSuiteLoader loader) {
		fTestLoader= loader;
		String suiteName= null;
		if (args.length == 1) 
			suiteName= args[0];
		else if (args.length == 2 && args[0].equals("-c")) 
			suiteName= StringUtil.extractClassName(args[1]);
			
		fFrame= createUI(suiteName);
		fFrame.setLocation(200, 200);
		fFrame.setVisible(true);
	
		if (suiteName != null) {
			setSuiteName(suiteName);
			runSuite();
		}
	}
	public void startTest(Test test) {
		showInfo("Running: "+test);
	}
}