/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2013 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *  and DANSE Project Number: 287716
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *
 */

package com.ibm.haifa.smc.repository;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListItem;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.semanticweb.owlapi.model.IRI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.haifa.smc.client.oauth.IUserCredentials;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicator;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicatorException;

/**
 * Author: Uri Shani
 * IBM
 */
public class SMCPanel extends JPanel implements VerifiedInputEditor {

	public interface CommandListener {
    	int OPEN = 0;
		int SAVE = 1;

		void action(int code);
	}


	/**
     * 
     */
    private static final long serialVersionUID = -8869065983080068694L;

	private static final Set<String> preloaded = new HashSet<String>();
	static {
		preloaded.add("http://purl.org/dc/terms/"); // dcterms
		preloaded.add("http://www.w3.org/2000/01/rdf-schema#"); // rdfs
		preloaded.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#"); // rdf
		preloaded.add("http://www.w3.org/2002/07/owl"); // owl		 
	}

    private static Logger log = Logger.getLogger(SMCPanel.class);

    private JTextField uriField;

    private MList ontologiesList, rulesList;

    private List<InputVerificationStatusChangedListener> listeners =
            new ArrayList<InputVerificationStatusChangedListener>();

    // these fields hold fields from the currently selected item.
	
    PreferencesManager prefsM = PreferencesManager.getInstance();
    Preferences prefs =  prefsM.getPreferencesForSet("SMC", String.class);


	// Database from server
	private JsonObject data = new JsonObject();
	private Map<String, SMC_URIListItem> id2Item = new HashMap<String, SMC_URIListItem>();
	private Map<String, SMC_URIListItem> iri2Item = new HashMap<String, SMC_URIListItem>();
	private Map<String, SMC_URIListItem> uri2Item = new HashMap<String, SMC_URIListItem>();
	private Set<String> tags= new HashSet<String>();
	private Set<String> filter = new HashSet<String>();
	private Set<SMC_URIListItem> opened = new HashSet<SMC_URIListItem>();
	private JsonArray ontologies = new JsonArray();
	private JsonArray rules = new JsonArray();

	private JPanel filterPanel;

	@SuppressWarnings("rawtypes")
	private JComboBox serverField;
	private JTextField userField;
	private JPasswordField passwordField;
	private JCheckBox useOAuth;
	private JTextArea message;
	private JPanel openedPanel;

	private SMC_URIListItem currentItem;

	// Panels in the dialog:
	private JPanel serverHolder;
	private JPanel uriFieldHolder;
	private JPanel filtersHolder;
	private JPanel ontologiesHolder;
	private JPanel rulesSetsHolder;
	private JPanel openedHolder;
	private JPanel logHolder;
	private JTextArea logView;

	private JFrame frame;

	private boolean isLoggedIn = false;

	private JButton loginButton;

	private JPanel buttonsHolder;

	public JButton openButton;
	public JButton saveButton;


	private List<CommandListener> cmdListeners = new ArrayList<CommandListener>();
	

	public JFrame getFrame() {
		return frame;
	}
	public SMCPanel() {
        createUI();
    }


    private void createUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));   
        // Server
        String servers[] = prefs.getString("servers", "").split(";");
        serverField = new JComboBox<Object>(servers);
        userField = new JTextField(45);
        passwordField = new JPasswordField(45);
        useOAuth = new JCheckBox("With User Authentication", true);
        // server
        serverHolder = new JPanel();
        serverHolder.setLayout(new BoxLayout(serverHolder, BoxLayout.Y_AXIS));
        JPanel serverRow = new JPanel();
        serverRow.setLayout(new BoxLayout(serverRow, BoxLayout.X_AXIS));
        JPanel userRow = new JPanel();
        userRow.setLayout(new BoxLayout(userRow, BoxLayout.X_AXIS));
        JPanel passwordRow = new JPanel();
        passwordRow.setLayout(new BoxLayout(passwordRow, BoxLayout.X_AXIS));
        serverHolder.add(serverRow); 
        serverHolder.add(userRow);
        serverHolder.add(passwordRow);
        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        serverHolder.add(buttonRow);
        loginButton = new JButton("Refresh from Server");
        loginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshFromServer();
			}
		});
        buttonRow.add(loginButton);
        this.message = new JTextArea();
        this.message.setEditable(false);
        this.message.setForeground(Color.red); this.message.setText("Not logged In");
        this.message.setBackground(Color.LIGHT_GRAY);
        buttonRow.add(message);
        serverHolder.setBorder(ComponentFactory.createTitledBorder("SMC Server"));
        serverRow.add(new JLabel("Server URL "));
        serverField.setToolTipText("format: <http/https>://<server ip>:<port number>");
        serverRow.add(serverField);
        serverField.setEditable(true);
        serverRow.add(useOAuth);
        useOAuth.setToolTipText("When not checked, user authentication is not used");

        useOAuth.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					userField.setEnabled(useOAuth.isSelected());
					passwordField.setEnabled(useOAuth.isSelected());
					prefs.putBoolean("useOAuth", useOAuth.isSelected());
			}
		});
       	useOAuth.setSelected(prefs.getBoolean("useOAuth", false));
       	userField.setEnabled(useOAuth.isSelected());
       	passwordField.setEnabled(useOAuth.isSelected());
       	userRow.add(new JLabel("User: "));
        userRow.add(userField);
        passwordRow.add(new JLabel("Password: "));
        passwordRow.add(passwordField);
        add(serverHolder);
//        String server = prefs.getString("server", null);
        String user = prefs.getString("user", "");
//        serverField.setText(server);
        userField.setText(user);
        
        // URI
        uriField = new JTextField(45);
        uriField.setEditable(false);
        uriField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                handleValueChanged();
            }
            public void removeUpdate(DocumentEvent event) {
                handleValueChanged();
            }
            public void changedUpdate(DocumentEvent event) {
                handleValueChanged();
            }
        });
        uriFieldHolder = new JPanel(new BorderLayout());
        uriFieldHolder.setBorder(ComponentFactory.createTitledBorder("Subject Ontology/Rule for Save or Open"));
        uriFieldHolder.add(uriField, BorderLayout.NORTH);
        add(uriFieldHolder);

        // Filters
        filtersHolder = new JPanel(new BorderLayout());
        filtersHolder.setBorder(ComponentFactory.createTitledBorder("SMC Filters"));
//        JPanel mPanel = new JPanel(new BorderLayout());
//        filtersHolder.add(mPanel, BorderLayout.NORTH);
        filterPanel = new JPanel(new FlowLayout());
        JButton filterClearButton = new JButton("Clear");
        filterClearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filter.clear();
				fillPanel();
			}
        });
        fillTags();
        filtersHolder.add(filterClearButton, BorderLayout.WEST);
        filtersHolder.add(filterPanel, BorderLayout.EAST);
//        uriFieldHolder.add(uriField, BorderLayout.NORTH);
        add(filtersHolder);

        // Ontologies and Rules
        ontologiesHolder = new JPanel(new BorderLayout());
        ontologiesHolder.setBorder(ComponentFactory.createTitledBorder("Ontologies"));
        add(ontologiesHolder);
        ontologiesList = new MList() {
            private static final long serialVersionUID = 6590889767286900162L;
            protected void handleAdd() {}
            protected void handleDelete() {}
       };
       rulesList = new MList() {
           private static final long serialVersionUID = 6590889767286900162L;
           protected void handleAdd() {}
           protected void handleDelete() {}
       };

       ontologiesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

       ontologiesList.setCellRenderer(new BookmarkedItemListRenderer());
       ontologiesHolder.add(new JScrollPane(ontologiesList)); //, BorderLayout.NORTH);
       ontologiesList.addListSelectionListener(new ListSelectionListener() {
    	   public void valueChanged(ListSelectionEvent e) {
    		   if (!e.getValueIsAdjusting()) {
    			   updateTextField(ontologiesList, rulesList);
    		   }
    	   }
       });

       rulesSetsHolder = new JPanel(new BorderLayout());
       rulesSetsHolder.setBorder(ComponentFactory.createTitledBorder("Rules Sets"));
       add(rulesSetsHolder);
       rulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

       rulesList.setCellRenderer(new BookmarkedItemListRenderer());
       rulesSetsHolder.add(new JScrollPane(rulesList));
       rulesList.addListSelectionListener(new ListSelectionListener() {
    	   public void valueChanged(ListSelectionEvent e) {
    		   if (!e.getValueIsAdjusting()) {
    			   updateTextField(rulesList, ontologiesList);
    		   }
    	   }
       });

       
       // List of opened elements
       openedHolder = new JPanel(new BorderLayout());
       openedHolder.setBorder(ComponentFactory.createTitledBorder("Opened in editor"));
       openedPanel = new JPanel(); 
       openedPanel.setLayout(new BoxLayout(openedPanel, BoxLayout.Y_AXIS));
       JScrollPane openedSPanel = new JScrollPane(openedPanel);
       openedHolder.add(openedSPanel);
       add(openedHolder);
       
       // Log
       logHolder = new JPanel(new BorderLayout());
       logView = new JTextArea();
       JScrollPane logSView = new JScrollPane(logView);
       logHolder.setBorder(ComponentFactory.createTitledBorder("Log"));
       logHolder.add(logSView);
       logView.setText("------");
       add(logHolder);
       fillOpened();
       buttonsHolder = new JPanel();
       JPanel buttonsPanel = new JPanel();
       buttonsHolder.setBorder(ComponentFactory.createTitledBorder("Actions"));
       buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
       buttonsHolder.add(buttonsPanel);
       openButton = new JButton("Open");
       openButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   for (CommandListener listener : cmdListeners) {
    			   listener.action(CommandListener.OPEN);}}});
       //       cancelButton = new JButton("Cancel");
       buttonsPanel.add(openButton);
       saveButton = new JButton("Save");
       saveButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   for (CommandListener listener : cmdListeners) {
    			   listener.action(CommandListener.SAVE);}}});
       buttonsPanel.add(saveButton);
       //       buttonsPanel.add(cancelButton);
       openButton.setEnabled(false);
       saveButton.setEnabled(false);
       add(buttonsHolder);
    }

    
	protected void refreshFromServer() {
		getFrame().setEnabled(false);
		new Thread() {
			public void run() {refreshFromServer_();}
		}.start();
		getFrame().setEnabled(true);
	}
	
	protected void refreshFromServer_() {
		try {
			setLog("Refreshing information...");
			String c[] = getFromSMCServer("database");
			if (c[0].startsWith("Error")) {
				setLog(c[0]);
				setMessage("Login failed", Color.red);
			} else {
				setLog("Refreshing...");
				JsonParser jp = new JsonParser();
				initializeData((JsonObject) jp.parse(c[0]));
				setMessage("Refresh succeeded", Color.green);
				setLog("Done.");
			}
			fillPanel();
		} catch (Exception e1) {
			e1.printStackTrace();
			setLog("Error accessing the server");
		}
	}
	
	protected String getComboBoxText(JComboBox<Object> box) {
		JTextField editor = (JTextField)box.getEditor().getEditorComponent();
		return editor.getText();
	}


	private void initializeData(JsonObject parse) {
		this.data = parse;
		this.ontologies = this.data.get("ontologies").getAsJsonArray(); 
		this.rules = this.data.get("rules").getAsJsonArray(); 
    	scanTags(this.ontologies);
    	scanTags(this.rules);
    	String filter[] = data.get("filter").getAsString().toLowerCase().split(", ");
    	
    	// remove duplicates:
    	Set<String> s = new HashSet<String>();
    	for (String f: filter) s.add(f);
    	filter = s.toArray(new String[0]);
    	
    	this.filter.clear();
    	for (String f : filter) 
    		if (false == f.trim().equals(""))
    			this.filter.add(f.trim());
	}

    private void fillOpened() {
    	openedPanel.removeAll();
    	for (SMC_URIListItem item : opened) {
    		Date d = new Date(Long.parseLong(item.getEtag()));
    		JLabel l = new JLabel(item.getDisplay() + " [" + d.toString() + "]");
    		l.setToolTipText(item.getEtag());
    		openedPanel.add(l);
    	}
    	openedPanel.invalidate();
    }


    public synchronized void setMessage(String msg, Color color) {
    	this.message.setText(msg);
    	this.message.setForeground(color);
    }

    private void handleValueChanged() {
        final boolean validURI = isValidURI();
        boolean signal = validURI;
        if (validURI && null != this.currentItem && opened.contains(this.currentItem) &&
        		this.currentItem.canSave()) {
        	saveButton.setEnabled(true);
        	openButton.setEnabled(false);
        	signal = false;
        } else if (validURI){
        	saveButton.setEnabled(false);
        	openButton.setEnabled(true);
        }
        for (InputVerificationStatusChangedListener l : listeners){
            l.verifiedStatusChanged(signal);
        }
    }

    protected boolean isValidURI(){
        final URI uri = getURI();
        return uri != null && uri.isAbsolute() && uri.getScheme() != null;        
    }

    public URI getURI() {
        return (null == this.currentItem)?null:this.currentItem.uri;
    }


    private void updateTextField(MList list, MList otherList) {
        SMC_URIListItem item = getSelUriListItem(list, otherList);
        if (item != null) {
            this.currentItem = item;
            uriField.setText(item.getDisplay());
        }
    }

	private void fillTags() {
		filterPanel.removeAll();
        for (String tag : this.tags) {
			JCheckBox t = new JCheckBox(tag);
			filterPanel.add(t);
			t.setSelected(this.filter.contains(tag));
			t.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					JCheckBox s = (JCheckBox) e.getSource();
					if (e.getStateChange() == ItemEvent.SELECTED)
						filter.add(s.getText());
					else if (e.getStateChange() == ItemEvent.DESELECTED)
						filter.remove(s.getText());
					fillList(ontologiesList, rulesList);
					fillOpened();
				}
			});
        }
        filtersHolder.validate();
	}

    private void fillList(MList ontList, MList rulList) {
          if (null == data)
        	  return;
          Iterator<JsonElement> it = this.ontologies.iterator();
          List<SMC_URIListItem> list = new ArrayList<SMC_URIListItem>();
          while (it.hasNext()) {
        	  JsonObject o = (JsonObject) it.next();
        	  String id = o.get("id").getAsString();
        	  if (id.equals("OntSM") || doFilter(o.get("tags").getAsString())) {
        		  SMC_URIListItem smcItem = createItem(o);
        		  if (opened.contains(smcItem)) 
        			  continue;
        		  list.add(smcItem); //new URI(o.get("url").getAsString()), o.get("tags").getAsString(), o.get("id").getAsString()));
        	  }
          }
        ontList.setListData(list.toArray());

        it = this.rules.iterator();
        list = new ArrayList<SMC_URIListItem>();
        while (it.hasNext()) {
        	JsonObject o = (JsonObject) it.next();
        	if (doFilter(o.get("tags").getAsString())) {
        		SMC_URIListItem smcItem = createItem(o);
        		if (opened.contains(smcItem)) 
        			continue;
        		list.add(smcItem); //new URI(o.get("url").getAsString()), o.get("tags").getAsString(), o.get("id").getAsString()));
        	}
        }
        rulList.setListData(list.toArray());
        ontologiesHolder.invalidate();
        rulesSetsHolder.invalidate();
    }


    /**
     * Check if any of the tags in the argument are included in the this.filter set.<br>
     * If this set is empty, no filter is applied and filte should succeed.
     * @param tgs String with comma-separated tags.
     * @return true if the tags pass the filter.
     */
    private boolean doFilter(String tgs) {
    	if (this.filter.size() == 0)
    		return true;
    	String ts[] = tgs.split(", ");
    	for (String t : ts) {
			if (this.filter.contains(t.trim()))
				return true;
		}
    	return false;
	}

	/**
	 * Answers with a response from posting ontology content to the SMC server.
	 * @param server Server IP address w/port
	 * @param user User for login
	 * @param pwd Password for login
	 * @param content String content to be delivered
	 * @param item SMC_URIListItem owning this content.
	 * @return String which may start with "Error" in case there was an error in the post.
	 * @throws Exception
	 */
	private String[] postOntologyToSMCServer(SMC_URIListItem item) throws Exception {
		checkLogin();
		String user = userField.getText();
        String password = new String(passwordField.getPassword());
        
        // Prepare the pose payload
		HttpResponse resp = null;
		String server = getComboBoxText(serverField);
		if (! server.startsWith("http"))
			server = "https://" + server;
		String getQuery = server + "/dm/smProtege?id=" + item.id + "&eTag=" + item.getEtag();
		HttpPost httpPost = new HttpPost(getQuery);
		httpPost.addHeader("Accept", "text/plain");
		httpPost.addHeader("If-Match", item.getEtag());
		MultipartEntity reqEntity = new MultipartEntity();
		File f = item.getFile();
		FileBody contents = new FileBody(f, "application/rdf+xml");
		reqEntity.addPart("ontology", contents);
		httpPost.setEntity(reqEntity);

		Exception exc = null;
		int c = 0;
		if (useOAuth.isSelected()) {
			UserCreds creds = new UserCreds(user, password, true);
			OAuthCommunicator conn = null;

			//Send the request and return the response
			conn = new OAuthCommunicator(creds);
			try {
				resp = conn.execute(httpPost); //new DefaultHttpClient().execute(httpget);
			} catch (OAuthCommunicatorException e) {
				exc = e;
				c = e.getErrorStatus();
			}
		} else {
			try {
				DefaultHttpClient conn = new DefaultHttpClient();
				resp = conn.execute(httpPost);
				StatusLine sl = resp.getStatusLine();
				if (sl.getStatusCode() != HttpStatus.SC_OK) {
					c = sl.getStatusCode();
					exc = new Exception(sl.getReasonPhrase());
				}
			} catch (Exception e) {
				exc = e;
				c = 404;
			}
        }
		if (null != exc) {
//			int c = exc.getErrorStatus();
			setMessage("Save failed", Color.red);
			setLog("Cannot save due [" + exc.getMessage() + "]");
			if (c == 412) {
				setLog("Need to reload ontology");
				opened.remove(item);
				fillPanel();
			}
			throw new Exception("HTTP Error code [ " + c + "]");
		}

		// get the results
		c = resp.getStatusLine().getStatusCode();
		if (c == 200) {
			successLogin();
			InputStream input = resp.getEntity().getContent();
			StringBuffer sb = new StringBuffer();
			byte buff[] = new byte[1024];
			int s = 0;
			while (0 < (s = input.read(buff))) {
				sb.append(new String(buff, 0, s));
			}
			input.close();
			String answer = sb.toString();
			String eTag = null;
			org.apache.http.Header h[] = resp.getHeaders("ETag");
			if (h.length > 0)
				eTag = h[0].getValue(); 

			setMessage("Success save", Color.GREEN);
			return new String[] {answer, eTag};
		} else {
			failedLogin("code:" + c);
			throw new Exception("HTTP Error code [ " + c + "]");
		}
	}


    /**
     * Asnwers with the content of a request from the SMC server, depending on the <i>what</i> parameter
     * as an array of 2 strings:<ul>
     * <li>First string is the rdf/xml of the ontology.
     * <li>Second string is the eTag (or null is not relevant) of that ontology.
     * </ul>
     * @param server
     * @param user
     * @param pwd
     * @param what
     * @return
     * @throws Exception
     */
	private String[] getFromSMCServer(String what) throws Exception {
		checkLogin();
		String user = userField.getText();
        String password = new String(passwordField.getPassword());
        boolean doOAuth = useOAuth.isSelected();
		UserCreds creds = new UserCreds(user, password, doOAuth);
		String server = getComboBoxText(serverField);
		if (false == server.startsWith("http"))
			server = "https://" + server;
		OAuthCommunicator conn = null;
		HttpResponse resp = null;
		String getQuery = server + "/dm/smProtege";
		HttpGet httpget;
		setLog("logging into [" + getQuery + "]" + ( ! useOAuth.isSelected()? " w/no Auth.":" for user [" + userField.getText() + "]"));

		int c = -1;
		try {
		if (what.equals("database")) {
			httpget = new HttpGet(getQuery);
			httpget.addHeader("Accept", "application/json");
		} else { // what is the id of the ontology to get
			getQuery += "?id=" + what;
			httpget = new HttpGet(getQuery);
			httpget.addHeader("Accept", "application/rdf+xml");
		}
		//Send the request and return the response
		if (useOAuth.isSelected()) {
			conn = new OAuthCommunicator(creds);
			resp = conn.execute(httpget); //new DefaultHttpClient().execute(httpget);
		} else {
			DefaultHttpClient conn_ = new DefaultHttpClient();
//			conn_.setu
			resp = conn_.execute(httpget);			
		}

		// get the results
		c = resp.getStatusLine().getStatusCode();
		if (c == HttpStatus.SC_OK) {
			successLogin();
			InputStream input = resp.getEntity().getContent();
			StringBuffer sb = new StringBuffer();
			byte buff[] = new byte[1024];
			int s = 0;
			while (0 < (s = input.read(buff))) {
				sb.append(new String(buff, 0, s));
			}
			input.close();
			String eTag = null;
			org.apache.http.Header h[] = resp.getHeaders("ETag");
			if (h.length > 0)
				eTag = h[0].getValue(); 
			return new String[] {sb.toString(), eTag};
		} else {
			failedLogin("code: "+ c);
		} } catch (Exception e) {
			failedLogin(e.getMessage());
		}
		throw new Exception("HTTP Error code [ " + c + "]");
	}


	private void successLogin() {
		String server = getComboBoxText(serverField);
		if (! server.startsWith("http"))
			server = "https://" + server;
		String user = userField.getText();
		if (! isLoggedIn) {
			isLoggedIn = true;
			setMessage("Successful Login", Color.green);
			setLog("Successful Login to " + server + (useOAuth.isSelected()?", user " + user:""));
			prefs.putString("server", server);
			prefs.putString("user", user);
			String servers[] = prefs.getString("servers", "").split(";");
			StringBuffer sb = new StringBuffer(server);
			for (String string : servers) {
				if (string.equals(server))
					continue;
				sb.append(";").append(string);				
			}
			prefs.putString("servers", sb.toString());
//			loginButton.setEnabled(false);
			serverField.setEnabled(false);
			userField.setEnabled(false);
			passwordField.setEnabled(false);
			useOAuth.setEnabled(false);
		}
	}


	/**
	 * display status and logs to reflect this status and enable selecting another server to log in
	 * @param c HTTP return code indicating failure.
	 */
	private void failedLogin(String msg) {
		msg = "Login failed with msg [" + msg + "].";
		setMessage(msg, Color.red);
		setLog(msg);
		if (isLoggedIn) {
			setLog("Cannot work with another server. Need to restart application.");
			setMessage("Must restart: " + msg, Color.red);
			return;
		}
		isLoggedIn = false;
		loginButton.setEnabled(true);
		serverField.setEnabled(true);
		useOAuth.setEnabled(true);
	}


	private void checkLogin() {
		if (!isLoggedIn ) {
			setMessage("Attempting to login", Color.yellow);
			setLog("Attempting to login...");
		}
	}


	/**
	 * Sets the top log line in the log view.<br>
	 * If the attached string ends with ..., the next log entry will be appended to it on 
	 * the same line.<br>
	 * Otherwise, this is a new top line and the next log will be above it.
	 * @param string
	 */
	public synchronized void setLog(String string) {
		String log = this.logView.getText();
		String logs[] = log.split("\n");
		if (logs.length > 0 && logs[0].endsWith("...")) { // append to first line
			logs[0] = logs[0] + string + '\n';
			log = join(logs);
		} else {
			log = string + '\n' + log;
		}
		this.logView.setText(log);
		this.logView.validate();
		validate();
	}


	/**
	 * Answers with a joined string of the strings with line breaks as glue. 
	 * @param logs Array of String-s to join.
	 * @return String of joined segments glued with new lines.
	 */
	private String join(String[] logs) {
		StringBuffer sb = new StringBuffer();
		for (String string : logs) {
			sb.append(string.trim()).append('\n');
		}
		return sb.toString();
	}



    /**
     * Scans the array of items for their tags, but also updates the maps of ids and iris to items.
     * @param array JsonArray of items to be processed.
     */
	private void scanTags(JsonArray array) {
		Iterator<JsonElement> it = array.iterator();
		while (it.hasNext()) {
			JsonObject o = (JsonObject)it.next();
			String tags[] = o.get("tags").getAsString().split(", ");
			for (String t : tags) {
				if (false == t.trim().equals(""))
					this.tags.add(t.trim());
			}
			createItem(o);  // will create a new one, or modified an existing one, and update the maps of ids and iris.
		}
	}


	private SMC_URIListItem getSelUriListItem(MList list, MList otherList) {
        if (list.getSelectedValue() instanceof SMC_URIListItem) {
        	otherList.clearSelection();
            return (SMC_URIListItem) list.getSelectedValue();
        }
        return null;
    }

    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.add(listener);
        listener.verifiedStatusChanged(isValidURI());
    }


    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.remove(listener);
    }


    private class BookmarkedItemListRenderer extends DefaultListCellRenderer {

        /**
         * 
         */
        private static final long serialVersionUID = -833970269120392171L;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (null == label) {
            	int i = list.getFirstVisibleIndex();
            	label = (JLabel) list.getComponent(i);
            }
            if (value instanceof SMC_URIListItem && null != label) {
                SMC_URIListItem item = (SMC_URIListItem) value;
                label.setText(item.getDisplay());
            }
            return label;
        }
    }

    private class UserCreds implements IUserCredentials {
    	private final String user;
    	private final String pwd; 
    	private final boolean doOAuth;
    	public UserCreds( String user, String pwd, boolean doOAuth ) {
    		this.user = user;
    		this.pwd = pwd;
    		this.doOAuth = doOAuth;
    	}

    	public String getPassword() {
    		return pwd;
    	}

    	public String getUserId() {
    		return user;
    	}

		@Override
		public boolean doAuth() {
			// TODO Auto-generated method stub
			return doOAuth;
		}
    }


    public class SMC_URIListItem implements MListItem {

        private URI uri;
        private List<String> tags = new ArrayList<String>(); 
        private String id;
        private IRI versionIri;
        private String name = "";
        private String eTag = "";
        private boolean readOnly = false;
        private JsonArray dependencies = new JsonArray();
        private String fileName = null;

        public String toString() {
        	return "id=" + id + "\n" + 
        	"versionIri=" + versionIri + "\n" +
        	"name=" + name + "\n" +
        	"uri=" + uri + "\n" +
        	"eTag=" + eTag + "\n" +
           	"readOnly=" + readOnly + "\n" +
           	"dependencies=" + dependencies + "\n" +
           	"fileName=" + fileName + "\n" +
            ""       	;
        }
        public SMC_URIListItem(JsonObject o) {
        	init(o);
        }
        
        
        public void init(JsonObject o) {
        	//        	System.out.println("-------" + o);
        	try {
        		uri = new URI(o.get("modelInstanceNamespace").getAsString());
            	versionIri = IRI.create(o.get("version").getAsString());
        	} catch (URISyntaxException e) {
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        	}
        	String tags = o.get("tags").getAsString();
        	String id = o.get("id").getAsString();
        	boolean isReadOnly = false;
        	if (null != o.get("isImported"))
        		isReadOnly = Boolean.parseBoolean(o.get("isImported").toString());
        	this.dependencies = o.get("dependencies").getAsJsonArray();
        	if (null != this.id) {
        		id2Item.remove(this.id);
        		iri2Item.remove(this.versionIri.toString());
        		uri2Item.remove(this.uri.toString());
        	}
        	id2Item.put(id, this);
        	iri2Item.put(this.versionIri.toString(), this);
        	uri2Item.put(this.uri.toString(), this);
//        	if (id.startsWith("Ont"))
//        		iri2OntItem.put(versionIri, this);
        	this.name = o.get("name").getAsString();
        	this.readOnly = isReadOnly;
				this.init(this.uri, tags, id);
        }

        /**
         * Answers with a file to hold the ontology of this item.
         * Using the folder "ontologies" in the Protege installation space. If that fails,
         * will use a temp file.
         * This may fail and the result will be null to indicate this.
         * @return File which may be null if file generation failed.
         */
        public File getFile() {
        	File ontology = null;
        	if (null != this.fileName) {
        		ontology = new File(this.fileName);
        	} else {
        		File folder = new File("ontologies");
        		if (folder.mkdir()) {
        			ontology = new File(folder, id + ".owl");
        		}
        		if (null == ontology) {
        			try {
        				ontology = File.createTempFile("ontology." + id + ".", ".owl");
        			} catch (IOException e) {}
        		}
        	}
        	if (null != ontology) {
        		this.fileName = ontology.getAbsolutePath();
        		System.out.println("SMC plugin uses file [" + this.fileName + "] for [" + id + "].");
        		ontology.deleteOnExit();
        	}
        	return ontology;
		}

		private void init(URI uril, String tags, String id) {
            this.uri = uril;
            String t[] = tags.split(",");
            for (int i= 0; i < t.length; i++) {
            	t[i] = t[i].trim();
            	if ("".equals(t[i]))
            		continue;
            	this.tags.add(t[i]);
            }
//            this.tags.addAll(Arrays.asList(tags));
            this.id = id;
        }


        public boolean isEditable() {
            return false;
        }


        public void setEtag(String eTag) {
        	this.eTag = eTag;
        }
        public String getEtag() {
        	if (null == this.eTag ||
        		this.eTag.trim().equals(""))
        		return Long.toString(System.currentTimeMillis());
        	else
        		return this.eTag;
        	
        }
        
        public void handleEdit() {
        }


        public boolean isDeleteable() {
            return false;
        }


        public boolean handleDelete() {
            log.info("DEL!");
            return true;
        }


        public String getTooltip() {
            return uri.toString();
        }
        public String getDisplay() {
        	return (this.readOnly?"[RO] ":"") + this.id + ": " + this.name; 
        }


		public String getId() {
			return id;
		}


		public boolean canSave() {
			return readOnly == false;
		}
    }

    
    public SMC_URIListItem createItem(JsonObject o) {
    	String id = o.get("id").getAsString();
    	SMC_URIListItem i = id2Item.get(id);
    	if (null == i)
    		return new SMC_URIListItem(o);
    	else {
    		i.init(o);
    		return i;
    	}
    }

    public List<URI> getPhysicalURI2Open() {
    	List<SMC_URIListItem> items = addDependencies(this.currentItem.id);
    	List<SMC_URIListItem> uniques = new ArrayList<SMC_URIListItem>();
    	for (SMC_URIListItem item : items) {  // Clear all duplicates and set the list in reverse order to the original one.
			if (uniques.contains(item) || opened.contains(item))
				continue;
			uniques.add(0, item);
		}
    	opened.addAll(uniques); //(panel.currentItem);
    	List<URI> uris = copyToLocal(uniques);
    	currentItem = null;
    	if (null != uris && uris.size() > 0)
    		setMessage("Open succeeded", Color.green);
    	
    	return uris;
    }
//        SMCPanel panel = SMCPanel.getInstance();
//        panel.frame.setExtendedState(Frame.NORMAL);
//        int ret = JOptionPaneEx.showValidatingConfirmDialog(null,
//                                                  "Enter or select an SMC Ontology/Rules URI",
//                                                  new JPanel(),
//                                                  JOptionPane.PLAIN_MESSAGE,
//                                                  JOptionPane.OK_CANCEL_OPTION,
//                                                  null /*panel.filterPannel*/);
//        if (ret == JOptionPane.OK_OPTION) {
//        	return uris;
//        }
//        return new ArrayList<URI>();
//    }

    public List<URI> copyToLocal(List<SMC_URIListItem> items) {
    	List<URI> list = new ArrayList<URI>();
    	for (SMC_URIListItem item : items) {
			URI u = copyToLocal(item);
			if (null != u)
				list.add(u);
		}
    	setForAll();
    	return list;
	}


    /**
     * Answers with a list of dependencies to add for a given item, flattening the entire dependencies tree.
     * <br>
     * Ontologies URI which are preloaded into Protege are not listed.
     * @param id
     * @return
     */
	private List<SMC_URIListItem> addDependencies(String id) {
		List<SMC_URIListItem> list = new ArrayList<SMC_URIListItem>();
		SMC_URIListItem i = id2Item.get(id);
		list.add(i);
		JsonArray dependencies = i.dependencies; //jItem.get("dependencies").getAsJsonArray();
		for (JsonElement e : dependencies) {
			SMC_URIListItem d = iri2Item.get(e.getAsString());
			if (null == d)
				continue;
			if (preloaded.contains(e.getAsString())) {
				System.out.println("SMC: ontology [" + e + "] assumed preloaded in Protege.");
				continue;
			}
//			System.out.println("SMC: ontology [" + e + "] not found in preloaded.");
//			System.out.println("preloaded:");for(String f:preloaded)System.out.println(f);
			String dId = d.id; //jo.get("id").getAsString();
			List<SMC_URIListItem> subList = addDependencies(dId);
			list.addAll(subList);
		}
		return list;
	}


//	private JsonObject getJsonItemByIri(String iri) {
//		for (JsonElement e : this.ontologies) {
//			String eIri = ((JsonObject)e).get("modelInstanceNamespace").getAsString();
//			if (iri.equals(eIri))
//				return (JsonObject)e;
//		}
//		return null;
//	}


//	/**
//	 * Answers with a JsonObject of a certain item having an id same as the parameter.
//	 * @param id String id of an item.
//	 * @return null if none found, or the JsonObject for that item.
//	 */
//	private JsonObject getJsonItemById(String id) {
//		for (JsonElement e : this.ontologies) {
//			String eId = ((JsonObject)e).get("id").getAsString();
//			if (id.equals(eId))
//				return (JsonObject)e;
//		}
//		return null;
//	}


	private void fillPanel() {
    	fillOpened();
    	fillTags();
    	fillList(ontologiesList, rulesList);
    	invalidate();
	}


//	private String getDisplayName() {
//        return (null == this.currentItem)?null:this.currentItem.getDisplay();
//	}


//	private String getId() {
//        return (null == this.currentItem)?null:this.currentItem.id;
//	}


	private URI copyToLocal(SMC_URIListItem item) {
		try {
			String line = "Getting ontology " + item.id + "...";
			System.out.println("------ Open " + item);
			setLog(line);
			String c[] = getFromSMCServer(item.id);
			if (c[0].startsWith("Error")) {
				setMessage(c[0], Color.red);
				setLog(c[0]);
				return null;
			}
			setLog("Import is ...");
			File ontology = item.getFile();
			if (null == ontology) {
				setLog("Failed: Local storage error. Cannot work with this ontology\n");				
			} else {
				setLog("Done\n");				
				ontology.delete();
				FileOutputStream out = new FileOutputStream(ontology);
				out.write(c[0].getBytes());
				String eTag = c[1]; 
				if (null == eTag)
					eTag = Long.toString(System.currentTimeMillis());
				item.setEtag(eTag);
				out.close();
				URI fUri = ontology.toURI();
				return fUri;
			}
		} catch (Exception e) {
			e.printStackTrace();
			setLog("Error creating local ontology copy.\n");				
		}
		return null;
	}

	public boolean copyToRemote(SMC_URIListItem item) {
		setLog("Saving ontology " + item.id + "...");
		System.out.println("----- Saving " + item);
		try {
//			File ontology = item.getFile();
//			FileInputStream in = new FileInputStream(ontology);
			String c[] = postOntologyToSMCServer(item);
			if (false == c[0].startsWith("Error")) {
//				setMessage(c, Color.red);
//				this.logView.setText(line + c + "\n" + log);
//			} else {
				String eTag = c[1]; //.substring(c.indexOf('[') + 1, c.indexOf(']'));
				if (null == eTag)
					eTag = Long.toString(System.currentTimeMillis());
				item.setEtag(eTag);
				fillOpened();
			}
//			this.logView.setText(line + "Done\n" + log);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
//			setMessage("Exception: " + e.getMessage(), Color.red);
//			this.logView.setText(line + "Failed: " + e.getMessage() + "\n" + log);
		}
		return true;
	}


	private static SMCPanel instance = null;
	public static SMCPanel getInstance() {
		if (null == instance) synchronized (SMCPanel.class) {
			if (null == instance)
				instance = new SMCPanel();
			instance.initFrame();
		}
		return instance;
	}


	/**
	 * Initialize the frame wrapping over this panel after the class is initialized.
	 */
	private void initFrame() {
//		System.out.println("In initFrame--------------------------------");
//		try {
	  	   frame = new JFrame("SMC v[" + stringFromClassPath("bundle.version") + "] Access");
//		   System.out.println("frame = " + frame);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		frame.setResizable(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
		frame.setLocationByPlatform(true);
		frame.setMinimumSize(frame.getSize());
		frame.add(this);
		frame.pack();
		frame.setVisible(true);
	}


	public static void showActionDialog(IRI iri) {
        SMCPanel panel = SMCPanel.getInstance();
        panel.frame.setExtendedState(Frame.NORMAL);
        panel.frame.setVisible(true);
//		SMC_URIListItem oldItem = panel.currentItem;
        System.out.println("---------------- showActionDialog(IRI " + iri + ")");
        panel.setForSave(iri);
	}

	private void setForSave(IRI iri) {
		this.currentItem = uri2Item.get(iri.toString()); // This is actually the NS URI and not the version IRI. 
		System.out.println("----- to save A: " + currentItem);
		if (null != this.currentItem && this.currentItem.id.equals("OntSM"))
			this.currentItem = null;
		System.out.println("----- to save B: " + currentItem);
		if (null != this.currentItem) {
            uriField.setText(this.currentItem.getDisplay());
            uriField.setToolTipText(this.currentItem.uri.toString());
            if (false == this.currentItem.canSave()) {
            	uriField.setToolTipText("Model is ReadOnly [RO] - so it cannot be saved to SMC");
            	saveButton.setEnabled(false);
            }
//            saveButton.setEnabled(true);
        } else {
        	currentItem = null;
        	uriField.setText("Model cannot be saved to SMC");
        	uriField.setToolTipText("Model cannot be saved to SMC");
//        	saveButton.setEnabled(false);
        }
		ontologiesHolder.setVisible(false);
		rulesSetsHolder.setVisible(false);
	}

	public void setForAll() {
//		openButton.setEnabled(false);
//		saveButton.setEnabled(false);
    	currentItem = null;
		uriField.setText("");
    	uriField.setToolTipText("");
		ontologiesHolder.setVisible(true);
		rulesSetsHolder.setVisible(true);
		fillPanel();
	}

	public void addCommandListeners(CommandListener commandListener) {
		cmdListeners .add(commandListener);
	}
	public SMC_URIListItem getCurrentItem() {
		return currentItem;
	}
	
    /**
	 * Utility to return the content of a file in the class path as a String.
	 * @param path String path to the file in the class path.
	 * @return String content of the file.
     */
    public String stringFromClassPath(String path) {
        ClassLoader cl = this.getClass().getClassLoader();
        InputStream in = cl.getResourceAsStream(path);
        String contents;
		try {
			contents = stringFromStream(in);
		} catch (Exception e) {
			return null;
		}
        return contents;
    }
	/**
	 * Generates a string from an input stream.
	 * @param in
	 * @return String
	 * @throws IOException
	 */
	public String stringFromStream(InputStream in) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        StringBuffer sb = new StringBuffer();
        while ((len = in.read(buf)) >= 0){
          sb.append(new String(buf, 0, len));
        }
        in.close();
        return sb.toString();
	}

	public static  void main(String args[]) throws IOException {
		SMCPanel p = new SMCPanel();
		p.setVisible(true);
		System.out.println("Started");
		int c = System.in.read();
	}

}
