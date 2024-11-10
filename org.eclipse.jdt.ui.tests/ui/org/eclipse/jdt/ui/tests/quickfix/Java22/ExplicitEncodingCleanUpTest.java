/*******************************************************************************
 * Copyright (c) 2024 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix.Java22;

import java.nio.charset.UnsupportedCharsetException;
import java.util.Hashtable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.quickfix.rules.AbstractEclipseJava;
import org.eclipse.jdt.ui.tests.quickfix.rules.EclipseJava22;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ExplicitEncodingCleanUpTest {

	@BeforeEach
	protected void setUp() throws Exception,UnsupportedCharsetException {
		Hashtable<String, String> defaultOptions= TestOptions.getDefaultOptions();
		defaultOptions.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, Integer.toString(120));
		JavaCore.setOptions(defaultOptions);
		TestOptions.initializeCodeGenerationOptions();
		// Use load since restore doesn't really restore the defaults.
		JavaPlugin.getDefault().getCodeTemplateStore().load();
	}

	@RegisterExtension
	AbstractEclipseJava context= new EclipseJava22();

	enum ExplicitEncodingPatterns {

		CHARSET(
"""
package test1;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {

    void method(String filename) {
        // Ursprüngliche Verwendung von Charset.forName() mit verschiedenen Charsets
        Charset cs1 = Charset.forName("UTF-8");
        Charset cs1b = Charset.forName("Utf-8");  // Unterschiedliche Schreibweise (diese sollten gleich behandelt werden)
        Charset cs2 = Charset.forName("UTF-16");
        Charset cs3 = Charset.forName("UTF-16BE");
        Charset cs4 = Charset.forName("UTF-16LE");
        Charset cs5 = Charset.forName("ISO-8859-1");
        Charset cs6 = Charset.forName("US-ASCII");

        // Ausgabe, die durch den Cleanup angepasst wird
        System.out.println(cs1.toString());
        System.out.println(cs2.toString());

        // Beispiel mit einer Variablen
        String charsetName = "UTF-8";  // Wird durch eine Variable ersetzt
        Charset cs7 = Charset.forName(charsetName);  // Umstellung erforderlich
        System.out.println(cs7);

        // Testen eines ungültigen Charsets
        try {
            Charset cs8 = Charset.forName("non-existing-charset");  // Ungültiger Charset
            System.out.println(cs8);
        } catch (IllegalArgumentException e) {
            System.out.println("Fehler: " + e.getMessage());
        }

        // Ein benutzerdefinierter Charset-Test
        Charset cs9 = Charset.forName("windows-1252");
        System.out.println(cs9.toString());
    }

    void methodWithVariableCharset(String charsetName) {
        Charset cs = Charset.forName(charsetName);  // Charset über eine Variable
        System.out.println(cs.toString());
    }
}
""",

"""
package test1;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {

    void method(String filename) {
        // Ursprüngliche Verwendung von Charset.forName() mit verschiedenen Charsets
        Charset cs1 = StandardCharsets.UTF_8;
        Charset cs1b = StandardCharsets.UTF_8;  // Unterschiedliche Schreibweise (diese sollten gleich behandelt werden)
        Charset cs2 = StandardCharsets.UTF_16;
        Charset cs3 = StandardCharsets.UTF_16BE;
        Charset cs4 = StandardCharsets.UTF_16LE;
        Charset cs5 = StandardCharsets.ISO_8859_1;
        Charset cs6 = StandardCharsets.US_ASCII;

        // Ausgabe, die durch den Cleanup angepasst wird
        System.out.println(cs1.toString());
        System.out.println(cs2.toString());

        // Beispiel mit einer Variablen
        String charsetName = "UTF-8";  // Wird durch eine Variable ersetzt
        Charset cs7 = StandardCharsets.UTF_8;  // Umstellung erforderlich
        System.out.println(cs7);

        // Testen eines ungültigen Charsets
        try {
            Charset cs8 = Charset.forName("non-existing-charset");  // Ungültiger Charset
            System.out.println(cs8);
        } catch (IllegalArgumentException e) {
            System.out.println("Fehler: " + e.getMessage());
        }

        // Ein benutzerdefinierter Charset-Test
        Charset cs9 = Charset.forName("windows-1252");
        System.out.println(cs9.toString());
    }

    void methodWithVariableCharset(String charsetName) {
        Charset cs = Charset.forName(charsetName);  // Charset über eine Variable
        System.out.println(cs.toString());
    }
}
"""),
		BYTEARRAYOUTSTREAM("""
				package test1;

				import java.io.ByteArrayOutputStream;
				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        ByteArrayOutputStream ba=new ByteArrayOutputStream();
				        String result=ba.toString();
				        ByteArrayOutputStream ba2=new ByteArrayOutputStream();
				        String result2=ba2.toString("UTF-8");
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.nio.charset.StandardCharsets;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset());
						        ByteArrayOutputStream ba2=new ByteArrayOutputStream();
						        String result2=ba2.toString(StandardCharsets.UTF_8);
						       }
						    }
						}
						"""),
		FILEREADER("""
				package test1;

				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        try {
				            Reader is=new FileReader(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        try {
						            Reader is=new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		FILEWRITER("""
				package test1;

				import java.io.FileWriter;
				import java.io.Writer;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        try {
				            Writer fw=new FileWriter(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.FileWriter;
						import java.io.OutputStreamWriter;
						import java.io.Writer;
						import java.nio.charset.Charset;
						import java.io.FileNotFoundException;
						import java.io.FileOutputStream;

						public class E1 {
						    void method(String filename) {
						        try {
						            Writer fw=new OutputStreamWriter(new FileOutputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		INPUTSTREAMREADER(
				"""
						package test1;

						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        try {
						            InputStreamReader is1=new InputStreamReader(new FileInputStream("file1.txt")); //$NON-NLS-1$
						            InputStreamReader is2=new InputStreamReader(new FileInputStream("file2.txt"), "UTF-8"); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						""",

				"""
						package test1;

						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.nio.charset.StandardCharsets;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        try {
						            InputStreamReader is1=new InputStreamReader(new FileInputStream("file1.txt"), Charset.defaultCharset()); //$NON-NLS-1$
						            InputStreamReader is2=new InputStreamReader(new FileInputStream("file2.txt"), StandardCharsets.UTF_8); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		OUTPUTSTREAMWRITER(
				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("")); //$NON-NLS-1$
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), "UTF-8"); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.nio.charset.StandardCharsets;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), Charset.defaultCharset()); //$NON-NLS-1$
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), StandardCharsets.UTF_8); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		CHANNELSNEWREADER("""
				package test1;

				import java.io.ByteArrayOutputStream;
				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.nio.channels.ReadableByteChannel;
				import java.nio.charset.StandardCharsets;
				import java.nio.channels.Channels;
				import java.io.FileNotFoundException;
				import java.io.UnsupportedEncodingException;

				public class E1 {
				    void method(String filename) throws UnsupportedEncodingException {
				            ReadableByteChannel ch;
				            Reader r=Channels.newReader(ch,"UTF-8"); //$NON-NLS-1$
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.channels.ReadableByteChannel;
						import java.nio.charset.StandardCharsets;
						import java.nio.channels.Channels;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						            ReadableByteChannel ch;
						            Reader r=Channels.newReader(ch,StandardCharsets.UTF_8); //$NON-NLS-1$
						       }
						    }
						}
						"""),
		CHANNELSNEWWRITER("""
				package test1;

				import java.io.ByteArrayOutputStream;
				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Writer;
				import java.nio.channels.WritableByteChannel;
				import java.nio.charset.StandardCharsets;
				import java.nio.channels.Channels;
				import java.io.FileNotFoundException;
				import java.io.UnsupportedEncodingException;

				public class E1 {
				    void method(String filename) throws UnsupportedEncodingException {
				            WritableByteChannel ch;
				            Writer w=Channels.newWriter(ch,"UTF-8"); //$NON-NLS-1$
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Writer;
						import java.nio.channels.WritableByteChannel;
						import java.nio.charset.StandardCharsets;
						import java.nio.channels.Channels;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						            WritableByteChannel ch;
						            Writer w=Channels.newWriter(ch,StandardCharsets.UTF_8); //$NON-NLS-1$
						       }
						    }
						}
						"""),
		PRINTWRITER("""
				package test1;

				import java.io.PrintWriter;
				import java.io.Writer;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        try {
				            Writer w=new PrintWriter(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.PrintWriter;
						import java.io.Writer;
						import java.nio.charset.Charset;
						import java.io.BufferedWriter;
						import java.io.FileNotFoundException;
						import java.io.FileOutputStream;
						import java.io.OutputStreamWriter;

						public class E1 {
						    void method(String filename) {
						        try {
						            Writer w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Charset.defaultCharset()));
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		STRINGGETBYTES(
"""
package test1;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.FileNotFoundException;

public class E1 {
    void method(String filename) {
        String s="asdf"; //$NON-NLS-1$
        byte[] bytes= s.getBytes();
        byte[] bytes2= s.getBytes("UTF-8");
        System.out.println(bytes.length);
       }
    }

    void method2(String filename) {
		String s="asdf"; //$NON-NLS-1$
		byte[] bytes= s.getBytes();
		try {
			byte[] bytes2= s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(bytes.length);
	}
}
""",

"""
package test1;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.FileNotFoundException;

public class E1 {
    void method(String filename) {
        String s="asdf"; //$NON-NLS-1$
        byte[] bytes= s.getBytes(Charset.defaultCharset());
        byte[] bytes2= s.getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);
       }
    }

    void method2(String filename) {
		String s="asdf"; //$NON-NLS-1$
		byte[] bytes= s.getBytes(Charset.defaultCharset());
		try {
			byte[] bytes2= s.getBytes(StandardCharsets.UTF_8);
		}
		System.out.println(bytes.length);
	}
}
"""),
		STRING("""
				package test1;

				import java.io.FileNotFoundException;
				import java.io.UnsupportedEncodingException;

				public class E5 {

					static void bla() throws FileNotFoundException, UnsupportedEncodingException {
						byte[] b= {(byte)59};
						String s1=new String(b,"UTF-8");
						String s2=new String(b,0,1,"UTF-8");
					}
				}
										""",

				"""
						package test1;

						import java.io.FileNotFoundException;
						import java.nio.charset.StandardCharsets;

						public class E5 {

							static void bla() throws FileNotFoundException {
								byte[] b= {(byte)59};
								String s1=new String(b,StandardCharsets.UTF_8);
								String s2=new String(b,0,1,StandardCharsets.UTF_8);
							}
						}
														"""),
		PROPERTIESSTORETOXML("""
				package test1;

				import java.io.FileOutputStream;
				import java.io.IOException;
				import java.util.Properties;

				public class E1 {
					static void blu() throws IOException {
						Properties p=new Properties();
						try (FileOutputStream os = new FileOutputStream("out.xml")) {
							p.storeToXML(os, null, "UTF-8");
						}
						try (FileOutputStream os = new FileOutputStream("out.xml")) {
							p.storeToXML(os, null);
						}
					}
				}
				""",

				"""
						package test1;

						import java.io.FileOutputStream;
						import java.io.IOException;
						import java.nio.charset.StandardCharsets;
						import java.util.Properties;

						public class E1 {
							static void blu() throws IOException {
								Properties p=new Properties();
								try (FileOutputStream os = new FileOutputStream("out.xml")) {
									p.storeToXML(os, null, StandardCharsets.UTF_8);
								}
								try (FileOutputStream os = new FileOutputStream("out.xml")) {
									p.storeToXML(os, null, StandardCharsets.UTF_8);
								}
							}
						}
												"""),
		URLDECODER("""
				package test1;
				import java.io.UnsupportedEncodingException;
				import java.net.URLDecoder;

				public class E2 {

					static void bla() throws UnsupportedEncodingException {
						String url=URLDecoder.decode("asdf","UTF-8");
						String url2=URLDecoder.decode("asdf");
					}
				}
								""", """
				package test1;
				import java.net.URLDecoder;
				import java.nio.charset.Charset;
				import java.nio.charset.StandardCharsets;

				public class E2 {

					static void bla() {
						String url=URLDecoder.decode("asdf",StandardCharsets.UTF_8);
						String url2=URLDecoder.decode("asdf", Charset.defaultCharset());
					}
				}
												"""),
		URLENCODER("""
				package test1;
				import java.io.UnsupportedEncodingException;
				import java.net.URLEncoder;

				public class E2 {

					static void bla() throws UnsupportedEncodingException {
						String url=URLEncoder.encode("asdf","UTF-8");
						String url4=URLEncoder.encode("asdf");
					}
				}
								""", """
				package test1;
				import java.net.URLEncoder;
				import java.nio.charset.Charset;
				import java.nio.charset.StandardCharsets;

				public class E2 {

					static void bla() {
						String url=URLEncoder.encode("asdf",StandardCharsets.UTF_8);
						String url4=URLEncoder.encode("asdf", Charset.defaultCharset());
					}
				}
												"""),
		SCANNER("""
				package test1;
				import java.io.File;
				import java.io.FileNotFoundException;
				import java.util.Scanner;

				public class E3 {

					static void bla3(InputStream is) throws FileNotFoundException {
						Scanner s=new Scanner(new File("asdf"),"UTF-8");
						Scanner s2=new Scanner(is,"UTF-8");
						Scanner s3=new Scanner("asdf");
					}
				}
				""",
"""
package test1;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class E3 {

	static void bla3(InputStream is) throws FileNotFoundException {
		Scanner s=new Scanner(new File("asdf"),StandardCharsets.UTF_8);
		Scanner s2=new Scanner(is,StandardCharsets.UTF_8);
		Scanner s3=new Scanner("asdf", Charset.defaultCharset());
	}
}
"""),
		FORMATTER(
"""
package test1;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Formatter;

public class E4 {

	static void bla() throws FileNotFoundException, UnsupportedEncodingException {
		Formatter s=new Formatter(new File("asdf"),"UTF-8");
	}

	static void bli() throws FileNotFoundException {
		try {
			Formatter s=new Formatter(new File("asdf"),"UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
""", """
package test1;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

public class E4 {

	static void bla() throws FileNotFoundException {
		Formatter s=new Formatter(new File("asdf"),StandardCharsets.UTF_8);
	}

	static void bli() throws FileNotFoundException {
		try {
			Formatter s=new Formatter(new File("asdf"),StandardCharsets.UTF_8);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
"""),
		THREE("""
				package test1;

				import java.io.ByteArrayOutputStream;
				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        String s="asdf"; //$NON-NLS-1$
				        byte[] bytes= s.getBytes();
				        System.out.println(bytes.length);
				        ByteArrayOutputStream ba=new ByteArrayOutputStream();
				        String result=ba.toString();
				        try {
				            InputStreamReader is=new InputStreamReader(new FileInputStream("")); //$NON-NLS-1$
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				        try {
				            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("")); //$NON-NLS-1$
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				        try {
				            Reader is=new FileReader(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        String s="asdf"; //$NON-NLS-1$
						        byte[] bytes= s.getBytes(Charset.defaultCharset());
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset());
						        try {
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), Charset.defaultCharset()); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), Charset.defaultCharset()); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            Reader is=new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		ENCODINGASSTRINGPARAMETER(
				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        String s="asdf"; //$NON-NLS-1$
						        //byte[] bytes= s.getBytes(StandardCharsets.UTF_8);
						        byte[] bytes= s.getBytes("Utf-8");
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString();
						        try {
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), "UTF-8"); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), "UTF-8"); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            Reader is=new FileReader(filename);
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.nio.charset.StandardCharsets;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        String s="asdf"; //$NON-NLS-1$
						        //byte[] bytes= s.getBytes(StandardCharsets.UTF_8);
						        byte[] bytes= s.getBytes(StandardCharsets.UTF_8);
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset());
						        try {
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), StandardCharsets.UTF_8); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), StandardCharsets.UTF_8); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            Reader is=new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						""");

		String given;
		String expected;

		ExplicitEncodingPatterns(String given, String expected) {
			this.given = given;
			this.expected = expected;
		}
	}

	@ParameterizedTest
	@EnumSource(ExplicitEncodingPatterns.class)
	public void testExplicitEncodingParametrized(ExplicitEncodingPatterns test) throws CoreException {
		IPackageFragment pack = context.getfSourceFolder().createPackageFragment("test1", false, null);
		ICompilationUnit cu = pack.createCompilationUnit("E1.java", test.given, false, null);
		context.enable(CleanUpConstants.EXPLICITENCODING_CLEANUP);
		context.assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { test.expected }, null);
	}

	@Test
	public void testExplicitEncodingdonttouch() throws CoreException {
		IPackageFragment pack = context.getfSourceFolder().createPackageFragment("test1", false, null);
		ICompilationUnit cu = pack.createCompilationUnit("E2.java",
				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.IOException;
						import java.nio.charset.Charset;
						import java.io.FileInputStream;
						import java.io.FileNotFoundException;
						import java.io.UnsupportedEncodingException;

						public class E2 {
						    void method() throws UnsupportedEncodingException, IOException {
						        String s="asdf"; //$NON-NLS-1$
						        byte[] bytes= s.getBytes(Charset.defaultCharset());
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset().displayName());
						        try (
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), Charset.defaultCharset()); //$NON-NLS-1$
						           ){ } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						    }
						}
						""",
				false, null);

		context.enable(CleanUpConstants.EXPLICITENCODING_CLEANUP);

		context.assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
