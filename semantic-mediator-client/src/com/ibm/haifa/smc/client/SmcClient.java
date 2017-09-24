/**
  Copyright 2011-2016 IBM
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

*/
/**
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 *
 */
package com.ibm.haifa.smc.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.ibm.haifa.smc.client.oauth.*;
import com.ibm.haifa.smc.client.SmcListener.SmcEvent;

public abstract class SmcClient {
	private final String title;
	private final Image icon;

	public SmcClient(String title,
			javax.imageio.stream.ImageInputStream icon)
	{
		try {
			this.title = title;
			if (icon != null)
			    this.icon = ImageIO.read(icon);
			else
				this.icon = null;

		} catch (Throwable t) {
			throw new Error(t);
		}
	}

	protected String localElement = "";
	private String url = "";
	private String remoteElement = "";
	private final Map<String, List<String>> remoteElementHistory = new HashMap<String, List<String>>();

	private class AssociationMap<T> extends HashMap<String, T> {
		private static final long serialVersionUID = 1L;

		public void clear() {
			throw new UnsupportedOperationException();
		}
		public T put(String key, T value) {
			throw new UnsupportedOperationException();
		}
		public void putAll(Map<? extends String, ? extends T> map) {
			throw new UnsupportedOperationException();
		}

		private T privatePut(String key, T value) {
			return super.put(key, value);
		}
	};

	AssociationMap<AssociationMap<String>> associationMaps = new AssociationMap<AssociationMap<String>>();

	// Setup options
	/**
	 * Used to log console messages on progress of actions.
	 * Developer may orerride with implementation whichi ignores this.
	 * This implementation will attempt to display same content also on GUI console, depending
	 * on user's preferences.
	 * @param txt String to display, including line breaks.
	 */
	public abstract void console(String txt); // String contains line breaks. Called also if useConsole param is false.
	/**
	 * Used to answer which local element is at the focus of export or import.
	 * This information is used for information only.
	 * @param name String representation of the element in focus.
	 */
	public abstract void getLocalElement(String name); // Sets the local element field content � driven by User actions.
	/**
	 * Answers with the URI identity of a remote element associated with the local element (see above)
	 * User needs to enter this through GUI in the proper field.
	 * Implementation uses this info when importing. Yet, from the associations this
	 * information can be initialized depending on the local element in focus.
	 * If the association with the local element does not match the local element in focus - a warning to
	 * that affect should be displayed in the messages window.
	 * @return String with user's entry in that field.
	 */
	public String getRemoteElement() {
		if (null == localElement)
			localElement = "";
		if (null == url)
			url = "";
		Map<String, String> map = associationMaps.get(url);
		if (null != map && null != map.get(localElement))
			return map.get(localElement);
		else
			return "";
	}

	// Status
	/**
	 * Answers with the association map between remote and local URIs. This association is required
	 * to be applied to outgoing models.<br>
	 * The implementation of the client will apply these associations to the exported model, and will
	 * update the associations from incoming models as follows:<br>
	 * <li>Exported models will be updated with the triple
	 * <code>&lt;subj> &lt;http://com.ibm.ns/haifa/sm#hasSMResource> &lt;remote url></code><br>
	 * where subj and remote url are in the association map.
	 * <li>On Import, the client will use this map to find local elements to match the incoming imported
	 * resources. When a new local elements are generated as a result of this import, the client will
	 * update this map with 2 associations between the local URI and the remote URI. That update
	 * is than handled back via the <code>setAssociations()</code> method.
	 * @return Map that associates a local URI with a remote URI. Each pair is encoded twice to keep
	 * both directions in the keys and the values of this Map.

	 */
	public Map<String, String> getAssociationMap(){
		if (null == url)
			url = "";

		Properties props = new Properties();

		try {
		props.load(new FileInputStream(urlToFilename(url)));
		} catch (Throwable t) {
			System.err.println("WARNING: " + getMessage(t));
		}

		if (false == associationMaps.containsKey(url))
			associationMaps.privatePut(url, new AssociationMap<String>());
		AssociationMap<String> map = associationMaps.get(url);
		for (Object p : props.keySet())
			map.privatePut(p.toString(), props.get(p).toString());
		return map;
	}
	/**
	 * Client will update the association map after concluding the import and merging of the incomin
	 * model with the tool's model.
	 * @param map Map that associates a local URI with a remote URI. Each pair is encoded twice to keep
	 * both directions in the keys and the values of this Map.
	 */
	public void getAssociationMap(Map<String, String> map){
		if (null == url)
			url = "";
		AssociationMap<String> associationMap = (AssociationMap<String>)getAssociationMap();
		if (null != associationMap)
			for (Map.Entry<String, String> entry : map.entrySet()) {
				associationMap.privatePut(entry.getKey(), entry.getValue());
				associationMap.privatePut(entry.getValue(), entry.getKey());
			}
	}

	private String exportRdf, importRdf;
	private OutputStream exportStream;

	// Access data:
	/**
	 * Submit RDF graph to be exported.
	 * @param rdf RDF data in rdf/xml format.
	 */
	public void setExportRdf(String rdf) {
		exportRdf = null != rdf ? rdf : "";
	}
	/**
	 * Sets the output stream to a file of the RDF content to be exported.
	 * @param rdf OutputStream to a file.
	 */
	public void setExportStream(OutputStream rdf) {
		exportStream = rdf;
	}

	/**
	 * Anwers with the imported RDF data in rdf/xml format.
	 * @return String of the imported RDF.
	 */
	public String getImportRdf() {
		return importRdf;
	}

	/**
	 * Anwers with the InputStream from which the imported RDF model in rdf/xml format can be read.
	 * @return InputStream from which RDF can be read.
	 */
	public InputStream getImportAsStream() {
		return new ByteArrayInputStream(getImportRdf().getBytes());
	}

	private SmcListener listener;

	// monitoring
	/**
	 * Sets a listener for MCS events as described below
	 * @param listener McsListener implementation.
	 */
	public void setListener(SmcListener listener) {
		this.listener = listener;
	}

	// Activate
	/**
	 * Shows up the export/import dialog with some controls on what is activated on the dialog
	 * to customize it.
	 * @param flags Array of boolean flags as follows:<ul>
	 * <li> flags[0] - if present and true, than dialog is used in modal mode, meaning that after the
	 * operation is is hidden. Therefore its own console cannot be seen on conclusion of activity and
	 * the client must have its own console implementation.
	 * Yet, its implementation may not do anything. <br>
	 * Now, when working not in modal mode, the dialog stays and is updated as the user traverses different
	 * elements in focus in the local model. The GUI console can be used all the time and console
	 * of the client tool is not necessary.
	 * <li> flags[1] - if present and true, than an export only version of the dialog is used.
	 * <li> flags[2] - if present and true, than an import only version of the dialog is used.
	 * </ul>
	 * So if both flags[1 and 2] are missing or false, than a dialog serving both export and import is
	 * shown.
	 */
	public void show( boolean ... flags ) {
		// getFlag returns false if flag is missing
		boolean isModal = getFlag(0, flags);
		boolean enableExport = getFlag(1, flags);
		boolean enableImport = getFlag(2, flags);

		// If both flags are missing or false, then behavior then a dialog serving both export and import is shown.
		if (false == enableExport && false == enableImport)
			enableExport = enableImport = true;

		window = makeWindow(isModal);

	    // This panel covers the whole window
	    JPanel windowPanel = new JPanel();
	    windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));
	    window.add(windowPanel);

	    // This panel covers the fixed sections in the upper part of the window: login and export/import options
	    // (if the window is a dialog, then the top panel covers, in fact, the whole window)
	    JPanel topPanel = addOuterPanel(windowPanel, BorderLayout.NORTH);

	    addLoginPanel(topPanel);
	    addOptionsPanel(topPanel, isModal, enableExport, enableImport); // Export/Import options, including export/import/clear/cancel buttons at the end
	    if (!isModal)
		    addConsole(windowPanel);

	    showWindow(window);
	}

	// BELOW THIS LINE - PRIVATE METHODS ONLY

	private Window window;

	private Window makeWindow(final boolean modal) {
		final Window window;
		if (modal) {
			JDialog dialog = new JDialog(new JFrame(), title);
			dialog.setModal(true);
			dialog.setResizable(false);
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			window = dialog;
		} else {
			JFrame frame = new JFrame(title);
		    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		    frame.setResizable(true);
			if (icon != null)
			    frame.setIconImage(icon);
		    window = frame;
		}

		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				confirmAndClose(window);
			}
		});

	    return window;
	}

	private void confirmAndClose(Component parent) {
		if (ConfirmDialog.ask("Confirm", "Close window?", parent)) {
			window.dispose();
			if (null != listener)
				listener.handleEvent(SmcEvent.CANCEL);
		}
	}

	private void addLoginPanel(JComponent topPanel)
	{
		String LOGIN = "Login";
		String USER = "User:";
		String PASSWORD = "Password:";
		String MESSAGE = "Message:";

	    JPanel outerLoginPanel = addBorderedPanel(topPanel, LOGIN, 20, 0);
	    JPanel loginPanel = new JPanel(new GridLayout(0, 1, 0, 0));
	    outerLoginPanel.add(loginPanel);
	    loginFields[0] = addField(loginPanel, USER, 15, 31);
	    loginFields[1] = addPasswordField(loginPanel, PASSWORD, 15, -1);
	    loginFields[2] = addMessageField(loginPanel, MESSAGE, 15, 7);
	}

	JComponent [] loginFields = new JComponent[3];

	private void addOptionsPanel(final JComponent topPanel, boolean isModal, boolean enableExport, boolean enableImport)
	{
		assert enableExport || enableImport; // At least one option must be enabled

		String optionsTitle = null; // Will not remain null, as per assertion above
		if (enableExport && enableImport)
			optionsTitle = "Export/Import Options";
		else if (enableExport)
			optionsTitle = "Export Options";
		else if (enableImport)
			optionsTitle = "Import Options";

		String URL = "IoSE URL:";
		String REMOTE_ELEMENT = "Remote Element:";
		String LOCAL_ELEMENT = "Local Element:";
		String UPDATE = "Update";
		String FILE = "File:";
		String BROWSE = "Browse";

		JPanel exportImportPanel = addBorderedPanel(topPanel, optionsTitle, 0, 10);
		exportImportPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
		JPanel radioButtonsPanel = new JPanel(new GridLayout(0, 1));
		ButtonGroup options = new ButtonGroup();
		final JRadioButton urlSelect = addRadioButton(options, radioButtonsPanel, true);
		addRadioButton(options, radioButtonsPanel, false);
		addRadioButton(options, radioButtonsPanel, false);
		final JRadioButton fileSelect = addRadioButton(options, radioButtonsPanel, true);
		exportImportPanel.add(radioButtonsPanel);
		JPanel fieldsPanel = new JPanel(new GridLayout(0, 1));
		final JComboBox url = addListField(fieldsPanel, URL, 30, 42);
		final JComboBox remoteElement = addListField(fieldsPanel, REMOTE_ELEMENT, 30, -1);
		loadHistory_(url, remoteElement);
		url.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				synchronized(SmcClient.this) {
					if ("comboBoxEdited".equals(event.getActionCommand()))
						SmcClient.this.url = getComboBoxText(url);
					if ("comboBoxChanged".equals(event.getActionCommand())) {
						SmcClient.this.url = getComboBoxText(url);
						if (urlSelect.isSelected()) {
							String value = addValueToHistory(url);
							addHistory(value);
							List<String> elements = remoteElementHistory.get(value);
							remoteElement.removeAllItems();
							for (String e : elements)
								addItem(remoteElement, e);
						}
					}
					assert null != SmcClient.this.url;
				}
			}
		});
		remoteElement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if ("comboBoxEdited".equals(event.getActionCommand())) {
					synchronized(SmcClient.this) {
						SmcClient.this.remoteElement = getComboBoxText(remoteElement);
						String newElement = addValueToHistory(remoteElement);
						String urlValue = getComboBoxText(url);
						if (null != newElement && null != urlValue)
							addHistory(urlValue, newElement);
					}
				}
			}
		});
		JComponent [] localElement = addFieldAndButton(fieldsPanel, LOCAL_ELEMENT, UPDATE, 22, 3);
		final JTextField localElementField = (JTextField)localElement[0];
		JButton updateButton = (JButton)localElement[1];
		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent update) {
				getLocalElement(null); // TODO - What string should I use here???
				String text = SmcClient.this.localElement;
				if (null == text)
					text = "";
				localElementField.setText(text);
			}
		});
		makeReadOnly(localElementField);
		JComponent [] file = addFieldAndButton(fieldsPanel, FILE, BROWSE, 22, 64);
		final JTextField fileField = (JTextField)file[0];
		fileField.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if ("enabled".equals(evt.getPropertyName()) && Boolean.TRUE.equals(evt.getNewValue()))
					makeReadOnly(fileField);
			}
		});
		final JButton browseButton = (JButton)file[1];
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser fc = new JFileChooser(new File("."));
				if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(window)) {
					fileField.setText(fc.getSelectedFile().toString());
					setComboBoxText(url, SmcClient.this.url = fc.getSelectedFile().toURI().toString());
				}
			}
		});

		this.url = getComboBoxText(url);
		exportImportPanel.add(fieldsPanel);

		List<JComponent> urlEnabled = new ArrayList<JComponent>();
		List<JComponent> fileEnabled = new ArrayList<JComponent>();

		for (JComponent field : loginFields)
			urlEnabled.add(field);
		urlEnabled.add(url);
		urlEnabled.add(remoteElement);

		for (JComponent field : file)
			fileEnabled.add(field);


		toggle(urlSelect, urlEnabled, fileEnabled);
		toggle(fileSelect, fileEnabled, urlEnabled);
		urlSelect.setSelected(true);

		// Buttons
		String EXPORT = "Export";
		String IMPORT = "Import";
		String CLEAR = "Clear";
		String CANCEL = "Cancel";
		String CLOSE = "Close";

		JPanel mainButtonsEnvelope = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		mainButtonsEnvelope.setBorder(BorderFactory.createEmptyBorder(0, 7, 7, 0));
		final JPanel mainButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 0));
		mainButtonsEnvelope.add(mainButtons);
		topPanel.add(mainButtonsEnvelope);
		if (enableExport)
			addButton(mainButtons, EXPORT, new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						setBusy(true);
						if (listener != null)
							listener.handleEvent(SmcEvent.EXPORT);
						long start = System.currentTimeMillis();
						if (urlSelect.isSelected()) {
							append("Executing [Export to IoSE]....");
							OAuthCommunicator comm = getCommunicator();
							assert null != SmcClient.this.url;
							HttpPost post = new HttpPost(SmcClient.this.url);
							post.setEntity(new StringEntity(exportRdf));
							post.setHeader("Content-Type", "application/rdf+xml");
							append("Exporting to " + SmcClient.this.url + "...");
							HttpResponse resp = comm.execute(post);
							setLoginSuccessful();
							String associations = readContent(resp);
							updateAssociationMap(associations);
							append("OK. Posting report");
							append(associations);
						} else {
							append("Executing [Export to FILE]....");
							String filename = fileField.getText();
							File file = new File(filename);
							if (file.exists())
								append(filename + " exists and will be overwritten!");
							FileWriter fw = new FileWriter(file);
							fw.write(exportRdf);
							fw.close();
							append("Output generated in [" + filename + "]");
							append("No export to server");
						}
						append("Total execution time [" + getTime(start) + "] seconds");
						append("Execution completed");
					} catch (InvalidUserCredentials e) {
						setLoginFailed();
						append(getMessage(e));
					} catch (Throwable e) {
						e.printStackTrace();
						append(getMessage(e));
					} finally {
						setBusy(false);
					}
				}
			});
		if (enableImport)
			addButton(mainButtons, IMPORT, new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						setBusy(true);
						long start = System.currentTimeMillis();
						if (urlSelect.isSelected()) {
							append("Executing [Import from IoSE]....");
							OAuthCommunicator comm = getCommunicator();
							assert null != SmcClient.this.url;
							assert null != SmcClient.this.remoteElement;
							HttpGet get = new HttpGet(SmcClient.this.url);
							get.getParams().setParameter("ROOT_RESOURCE", SmcClient.this.remoteElement);
							get.setHeader("Content-Type", "application/rdf+xml");
							append(get.getURI());
							HttpResponse resp = comm.execute(get);
							setLoginSuccessful();
							importRdf = readContent(resp);
						} else {
							append("Executing [Import from FILE]....");
							String filename = fileField.getText();
							append("Importing from [" + filename + "]");
							importRdf = readContent(new File(filename));
						}
						append("RDF model received OK.");
						if (null == importRdf)
							importRdf = "";
						if (null != listener)
							listener.handleEvent(SmcEvent.IMPORT);
						append("Total execution time [" + getTime(start) + "] seconds");
						append("Execution completed");
					} catch (InvalidUserCredentials e) {
						setLoginFailed();
						append(getMessage(e));
					} catch (Throwable e) {
						e.printStackTrace();
						append(getMessage(e));
					} finally {
						setBusy(false);
					}
				}
			});
		addButton(mainButtons, CLEAR, new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				long start;
				append("Executing [Reset state with IoSE]");
				if (ConfirmDialog.ask("Confirm", "Clear local/remote associations?", mainButtons)) {
					start = System.currentTimeMillis();
					clearAssociationMap();
					append("State has been reset!");
				} else {
					start = System.currentTimeMillis();
					append("State has NOT been reset!");
				}
				append(getTime(start));
				append("Execution completed");
			}
		});
		addButton(mainButtons, isModal ? CANCEL : CLOSE, new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				confirmAndClose(mainButtons);
			}
		});
	}

	private void setBusy(boolean busy) {
		if (busy)
			window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		else
			window.setCursor(Cursor.getDefaultCursor());
	}

	private void addConsole(JComponent windowPanel) {
		String CONSOLE = "Console";
		JPanel consoleOuterPanel = new JPanel(new BorderLayout());
		Border consoleMargin = BorderFactory.createEmptyBorder(5, 10, 15, 10);
		Border consoleBorder = BorderFactory.createTitledBorder(CONSOLE);
		consoleOuterPanel.setBorder(BorderFactory.createCompoundBorder(consoleMargin, consoleBorder));
		windowPanel.add(consoleOuterPanel);
		/* JTextArea */ consoleText = new JTextArea(3, 0);
		consoleText.setEditable(false);
		consoleText.setFont(new Font("Courier New", Font.PLAIN, 12));
		JScrollPane console = new JScrollPane(consoleText);
		JPanel consoleInnerPanel = new JPanel(new BorderLayout());
		Border innerConsoleMargin = BorderFactory.createEmptyBorder(3, 5, 5, 5);
		consoleInnerPanel.setBorder(innerConsoleMargin);
		consoleOuterPanel.add(consoleInnerPanel);
		consoleInnerPanel.add(console, BorderLayout.CENTER);
	}

	private JTextArea consoleText;

	private void showWindow(Window window)
	{
		// Show window
        window.setLocationByPlatform(true);
	    window.pack();
	    window.setMinimumSize(window.getSize());
	    borderPanel.setMaximumSize(new Dimension(1000000, borderPanel.getSize().height));

	    window.pack();

//	    if (consoleText != null)
//			for (int i = 0; i < 100; ++i) {
//				for (int j = 0; j < 100; ++j)
//					consoleText.append("Test ");
//				consoleText.append("\n");
//			}

	    window.setVisible(true);
	}

    private static final int DEFAULT_GAP = 2;
    private static final Font DEFAULT_FONT = new JTextField().getFont();
    private static final Color DEFAULT_ENABLED_BACKGROUND = new JTextField().getBackground();
    private static final Color DEFAULT_DISABLED_BACKGROUND = new JComboBox().getBackground();
    private static final Border DEFAULT_BORDER = new JTextField().getBorder();
    private static final Font STANDARD_FONT = new Font("Arial", Font.PLAIN, 12);

	private boolean getFlag(int i, boolean ... flags) {
		return flags.length > i && flags[i];
	}

	private JPanel borderPanel;

	private JPanel addOuterPanel(Container window, String pos) {
        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        borderPanel = new JPanel(new BorderLayout());
        window.add(borderPanel);
        borderPanel.add(outerPanel, pos);
        return outerPanel;
	}

	private JPanel addBorderedPanel(Container window, String title, int hmargin, int vmargin) {
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        Border titleBorder = BorderFactory.createTitledBorder(title);
        Border outerMarginBorder = BorderFactory.createEmptyBorder(0, 0, 10, 0);
        Border outerBorder = BorderFactory.createCompoundBorder(outerMarginBorder, titleBorder);
        Border innerMarginBorder = BorderFactory.createEmptyBorder(0, hmargin, vmargin, 0);
        Border border = BorderFactory.createCompoundBorder(outerBorder, innerMarginBorder);
        outerPanel.setBorder(border);

        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

        window.add(outerPanel);
        outerPanel.add(innerPanel);

        return innerPanel;
	}

	private JComponent addField(JComponent owner, String label, int size, int hmargin) {
		JTextField field = new JTextField(size);
		addField(owner, label, field, hmargin);
		return field;
	}

	private JComponent addPasswordField(JComponent owner, String label, int size, int hmargin) {
		JComponent field = new JPasswordField(size);
		addField(owner, label, field, hmargin);
		return field;
	}

	private static final String LOGIN_PENDING = "login pending";
	private static final String LOGIN_SUCCESSFUL = "login successful";
	private static final String LOGIN_FAILED = "login failed";

	private void setLoginPending() {
		JTextArea message = (JTextArea)loginFields[2];
		message.setText(LOGIN_PENDING);
		message.setForeground(Color.YELLOW);
	}

	private void setLoginSuccessful() {
		JTextArea message = (JTextArea)loginFields[2];
		message.setText(LOGIN_SUCCESSFUL);
		message.setForeground(Color.GREEN);
	}

	private void setLoginFailed() {
		JTextArea message = (JTextArea)loginFields[2];
		message.setText(LOGIN_FAILED);
		message.setForeground(Color.RED);
	}

	private JComponent addMessageField(JComponent owner, String label, int size, int hmargin)  {
		final JTextArea message = (JTextArea)(loginFields[2] = new JTextArea(1, size));
		owner.add(makeLabeledField(label, message, hmargin, -1, 1, DEFAULT_GAP, DEFAULT_GAP, null));
        message.setEditable(false);
        message.setFocusable(false);
        Border line = BorderFactory.createLineBorder(Color.black, 1);
        Border [] empty = { BorderFactory.createEmptyBorder(0, 4, 0, -3), BorderFactory.createEmptyBorder(0, 2, 0, -2) };
        final Border enabledBorder = BorderFactory.createCompoundBorder(line, empty[0]);
        final Border disabledBorder = BorderFactory.createCompoundBorder(DEFAULT_BORDER, empty[1]);
        message.setBorder(enabledBorder);
        message.setBackground(Color.DARK_GRAY);
        message.setFont(message.getFont().deriveFont(Font.BOLD));
        setLoginPending();
        message.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				message.setBorder(message.isEnabled() ? enabledBorder : disabledBorder);
			}
		});
		return message;
	}

	private JComboBox addListField(JComponent owner, String label, int size, int hmargin) {
		JComboBox field = new JComboBox(new DefaultComboBoxModel());
		field.setEditable(true);
		field.setFont(DEFAULT_FONT);
		field.setPreferredSize(new Dimension(333, 21));
		JTextField editor = (JTextField)((JComboBox)field).getEditor().getEditorComponent();
		editor.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, -1, -2), DEFAULT_BORDER));
		owner.add(makeLabeledField(label, field, hmargin, -1, 1, DEFAULT_GAP, DEFAULT_GAP, null));
		return field;
	}

	private JComponent [] addFieldAndButton(JComponent owner, String fieldLabel, String buttonLabel, int size, int hmargin) {
		JTextField field = new JTextField(size);
		JComponent button = makeButton(buttonLabel, null);
		owner.add(makeLabeledField(fieldLabel, field, hmargin, -1, 1, 7, DEFAULT_GAP, button));
		return new JComponent [] { field, button };
	}

	private void addField(JComponent owner, String label, JComponent field, int hmargin) {
        owner.add(makeLabeledField(label, field, hmargin));
	}

	private JComponent makeLabeledField(String label, JComponent field, int hmargin) {
		return makeLabeledField(label, field, hmargin, -1, -1, DEFAULT_GAP, DEFAULT_GAP, null);
	}

	private JComponent makeLabeledField(String label, JComponent field, int hmargin, int fmargin, int bmargin, int hgap, int vgap, JComponent button) {
		JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, hgap, vgap));
        if (fmargin > 0 || bmargin > 0)
            fieldPanel.setBorder(new EmptyBorder(0, fmargin, 0, bmargin));
        if (label != null) {
        	JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        	if (hmargin >= 0)
        		labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, hmargin));
            JLabel fieldLabel = new JLabel(label);
            fieldLabel.setLabelFor(field);
            labelPanel.add(fieldLabel);
            fieldPanel.add(labelPanel);
        }
        field.setFont(STANDARD_FONT);
        fieldPanel.add(field);
        if (button != null)
        	fieldPanel.add(button);
        return fieldPanel;
	}

	private JComponent makeButton(String buttonLabel, ActionListener actionListener) {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        JButton button = new JButton(buttonLabel);
        button.setActionCommand(buttonLabel);
        if (actionListener != null)
            button.addActionListener(actionListener);
        button.setPreferredSize(new Dimension(80, 20));
        buttonPanel.add(button);
        return button;

	}

	private JRadioButton addRadioButton(ButtonGroup options, JComponent owner, boolean show) {
		JRadioButton button = new JRadioButton();
		options.add(button);
		owner.add(button);
		button.setPreferredSize(new Dimension(22, 25));
		button.setVisible(show);
		button.setFocusable(show);
		return button;
	}

	private void addButton(JComponent owner, String label, ActionListener actionListener) {
        JButton button = new JButton(label);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setActionCommand(label);
        if (actionListener != null)
            button.addActionListener(actionListener);
        button.setPreferredSize(new Dimension(60, 30));
        owner.add(button);
	}

	private void align(final AbstractButton cell, final List<JComponent> enable, final List<JComponent> disable) {
		if (enable != null)
			for (JComponent field : enable)
				setEnabled(field, cell.isSelected());
		if (disable != null)
			for (JComponent field : disable)
				setEnabled(field, !cell.isSelected());
	}

	private void toggle(final AbstractButton cell, final List<JComponent> enable, final List<JComponent> disable) {
		align(cell, enable, disable);
		cell.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent paramActionEvent) {
				align(cell, enable, disable);
			}
		});
	}

	private String getComboBoxText(JComboBox box) {
		JTextField editor = (JTextField)box.getEditor().getEditorComponent();
		String text = editor.getText();
		if (null == text)
			text = "";
		return text;
	}

	private void setComboBoxText(JComboBox box, String text) {
		JTextField editor = (JTextField)box.getEditor().getEditorComponent();
		if (null == text)
			text = "";
		editor.setText(text);
	}

	private String addValueToHistory(JComboBox box) {
		synchronized (SmcClient.this) {
			String value = getComboBoxText(box);
			if (value == null || value.equals(""))
				return null;
			addItem(box, value);
			return value;
		}
	}

	private Set<String> getItems(JComboBox box) {
		int N = box.getItemCount();
		Set<String> items = new HashSet<String>();
		for (int i = 0; i < N; ++i)
			items.add(box.getItemAt(i).toString());
		return items;
	}

	private void addItem(JComboBox box, Object item) {
		synchronized (SmcClient.this) {
			Set<String> items = getItems(box);
			if (items.contains(item))
				return;
			box.addItem(item);
		}
	}

	private void setEnabled(JComponent field, boolean enable) {
		if (field != null) {
			field.setEnabled(enable);
			field.setOpaque(enable);
			field.repaint();
		}
		if (field instanceof JComboBox) {
			JTextField editor = (JTextField)((JComboBox)field).getEditor().getEditorComponent();
			editor.setBackground(enable ? DEFAULT_ENABLED_BACKGROUND : DEFAULT_DISABLED_BACKGROUND);
			Border enabledBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, -1, -3), DEFAULT_BORDER);
			Border disabledBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, -2), DEFAULT_BORDER);
			editor.setBorder(enable ? enabledBorder : disabledBorder);
		}
	}

	private OAuthCommunicator getCommunicator() {
		try {
			final String user = ((JTextField)loginFields[0]).getText();
			final String password = ((JTextField)loginFields[1]).getText();

			return new OAuthCommunicator(new IUserCredentials() {
				public String getUserId() { return user; }
				public String getPassword() { return password; }
				public boolean doAuth() { return true; }
			});
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * Write argument both to user-implemented console, and to embedded console with a new line at the end (spares the need to add the new line in the code!)
	 */
	private void append(Object o) {
		String date = new Date().toString();
		String output = "[" + date + "]: " + o;
		console(output);
		if (null != consoleText)
			consoleText.append(output + "\n");
	}

	private String readContent(HttpResponse resp) throws Exception {
		return readContent(resp.getEntity().getContent());
	}

	private String readContent(File file) throws Exception {
		return readContent(new FileInputStream(file));
	}

	private String readContent(InputStream input) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream(); int b;
		while ((b = input.read()) != -1)
			output.write((byte) b);
		input.close();
		output.close();
		return output.toString();
	}

	/**
	 * @param associations Each association comes in a different line.
	 * Each line contains two space-separated tokens: the local element, and the remote element
	 */
	private void updateAssociationMap(String associations) {
		synchronized (SmcClient.this) {
			Scanner sc = new Scanner(associations);
			AssociationMap<String> map = (AssociationMap<String>)getAssociationMap();
			try {
				while (sc.hasNext()) {
					String local = sc.next(), remote = sc.next();
					map.privatePut(local, remote);
					map.privatePut(remote, local);
				}
				storeAssociationMap();
			} catch (Throwable t) {
				System.err.println("WARNING: " + getMessage(t));
			}
		}
	}

	private void clearAssociationMap() {
		synchronized (SmcClient.this) {
			try {
				associationMaps.privatePut(url, new AssociationMap<String>());
				storeAssociationMap();
			} catch (Throwable t) {
				System.err.println("WARNING: " + getMessage(t));
			}
		}
	}

	private void storeAssociationMap() {
		synchronized (SmcClient.this) {
			try {
				AssociationMap<String> map = associationMaps.get(url); // Do not use getAssociationMap here! must not read from storage!
				FileOutputStream out = new FileOutputStream(urlToFilename(url));
				Properties props = new Properties();
				props.putAll(map);
				props.store(out, "Association map for " + url);
				out.close();
			} catch (Throwable t) {
				System.err.println("WARNING: " + getMessage(t));
			}
		}
	}

	private String urlToFilename(String url) {
		if (null == url)
			url = "";
		char [] chars = { ' ', '.', '\\', '/', ':' };
		String file = url;
		for (char c : chars)
			file = file.replace(c,  '_');
		return file;
	}

	private void makeReadOnly(JTextField field) {
		field.setEditable(false);
		field.setBackground(DEFAULT_ENABLED_BACKGROUND);
		field.setBorder(DEFAULT_BORDER);
	}

	private void addHistory(String url, String ...remoteElements) {
		addHistory(true, url, remoteElements);
	}

	private void addHistory(boolean dump, String url, String ...remoteElements) {
		if (!remoteElementHistory.containsKey(url))
			remoteElementHistory.put(url, new ArrayList<String>());
		for (String element : remoteElements)
			remoteElementHistory.get(url).add(element);

		// Save to storage
		try {
			PrintWriter pw = new PrintWriter(".history");
 			for (String u : remoteElementHistory.keySet()) {
				pw.println("URL." + u);
				for (String r : remoteElementHistory.get(u))
					pw.println("REMOTE." + r);
 			}
 			pw.close();
		} catch (Throwable t) {
			System.err.println("WARNING: " + getMessage(t));
		}
	}

	private void loadHistory_(JComboBox url, JComboBox remoteElement) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(".history"));
			String line, key = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("URL."))
					addHistory(key = line.substring("URL.".length()));
				else if (line.startsWith("REMOTE."))
					addHistory(key, line.substring("REMOTE.".length()));
			}
		} catch (Throwable t) {
			System.err.println("WARNING: " + getMessage(t));
		}

		url.removeAllItems();
		for (String u : remoteElementHistory.keySet())
			url.addItem(u);
		remoteElement.removeAllItems();
		String text = getComboBoxText(url);
		if (remoteElementHistory.containsKey(text))
		for (String r : remoteElementHistory.get(text))
			remoteElement.addItem(r);
	}

	private String getTime(long start) {
		return new DecimalFormat("0.000").format((System.currentTimeMillis() - start) / 1000.0);
	}

	private String getMessage(Throwable t) {
		if (t instanceof OAuthCommunicatorException) {
			OAuthCommunicatorException e = (OAuthCommunicatorException) t;
			if (0 <= e.getErrorStatus())
				return getServerMessage(e);
		}

		String msg = "";
		if (null == t)
			return msg;

		do {
			msg = t.getClass().getName();
			if (null != t.getMessage())
				msg = t.getMessage();
		} while (null != (t = t.getCause()));

		return msg;
	}

	private String getServerMessage(OAuthCommunicatorException e) {
		String msg = "";
		if(null == e)
			return msg;

		msg = "(" + e.getErrorStatus() + ") " + e.getErrorMessage();
		return msg;
	}
}

//Usage by clients:
//Subclass and implement abstract methods. Activate with  the show() method.
