
/*
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
/*
 *+------------------------------------------------------------------------+
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

package com.ibm.dm.frontService.sm.data;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import thewebsemantic.Namespace;

import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.utils.Utils;
import com.ibm.haifa.smc.client.oauth.IUserCredentials;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicator;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicatorException;

// ============================= Friends ==============================
// Customizing for rows in the friends table.
@Namespace(Friend.NAME_SPACE)
public class Friend extends ANamedRow implements IUserCredentials
{
	static public interface IFields extends ANamedRow.IFields
    {
        public static final String USER     		 = "user";
        public static final String PASSWORD		     = "password";
        public static final String IP_ADDRESS 		 = "ipAddress";
		public static final String AUTHENTICATE 	 = "authenticate";
    }

	private static final long serialVersionUID       = 1L;

	private transient  OAuthCommunicator communicator = null;
	private transient HttpClient communicatorNoAuth = null;
	
	/**
	 * Answers with an OAutho communicator 
	 * @return OAuthCommunicator
	 * @throws OAuthCommunicatorException
	 */
	synchronized public OAuthCommunicator getCommunicator() 
	{
		if (! isAuthenticated())
			return null;
		setMessage(null);
		if (null != communicator)
			return communicator;
		testCommunicator();
		return communicator;
	}
	
	synchronized public HttpClient getCommunicatorNoAuth() 
	{
		if (isAuthenticated())
			return null;
		setMessage(null);
		if (null != communicatorNoAuth)
			return communicatorNoAuth;
		testCommunicator();
		return communicatorNoAuth;
	}

	synchronized public void testCommunicator() {
		setMessage(null);
		try { 
			HttpPost test = new HttpPost("https://" + getIpAddress() + "/dm/server/management/login");
			if (isAuthenticated() && null == communicator)
				communicator = new OAuthCommunicator(this);
			else if (! isAuthenticated() && null == communicatorNoAuth)
				communicatorNoAuth = HttpClients.createDefault();
				
			HttpResponse resp = execute(test);
			int code = resp.getStatusLine().getStatusCode();
			if (HttpStatus.SC_OK == code) 
				setStatus(true);
			else {
				setStatus(false);
				communicator = null;
				setMessage("Login failed [" + resp.getStatusLine().getReasonPhrase() + "]");
			}
		} catch (Exception e) {
			communicator = null;
			setMessage("Login failed [" + e.getMessage() + "]");
		}
	}

	
	private static String[]   friendEditableFields = new String[] {
    	IFields.ARCHIVED,
    	IFields.VERSION,
    	IFields.NAME,
    	IFields.USER,
    	IFields.PASSWORD,
    	IFields.IP_ADDRESS,
    	IFields.TAGS,
    	IFields.AUTHENTICATE,};

    protected String          user		             = "";
    protected String          password	             = "";
    protected String          ipAddress		         = "";

	private String authenticate = "on";

    @Override
    public String[] getEditableFieldNames()
    {
        return friendEditableFields;
    }

    public Friend() {
    	super();
    	setStatus(false);
    }

    public Friend(boolean... isTemp)
    {
        super(isTemp);
    	setStatus(false);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
        markModified();
    }

    public void setField(String field, String value)
    {
        super.setField(field, value);
        if (field.equalsIgnoreCase(IFields.NAME))
            this.name = value;
        else
            if (field.equalsIgnoreCase(IFields.USER))
            	this.user = value;
            else
            	if (field.equalsIgnoreCase(IFields.PASSWORD))
            		this.password = value;
            	else
            		if (field.equalsIgnoreCase(IFields.AUTHENTICATE))
            			this.authenticate = value;
            		else
            			if (field.equalsIgnoreCase(IFields.IP_ADDRESS))
            				this.ipAddress = value;
    }

    public String getField(String field)
    {
        if (field.equalsIgnoreCase(IFields.NAME))
            return this.name;
        else
            if (field.equalsIgnoreCase(IFields.USER))
                return this.user;
            else
                if (field.equalsIgnoreCase(IFields.PASSWORD))
                    return this.password;
                else
                    if (field.equalsIgnoreCase(IFields.IP_ADDRESS))
                        return this.ipAddress;
                    else
                    	if (field.equalsIgnoreCase(IFields.AUTHENTICATE))
                    		return this.authenticate;
                    	else
                    		return super.getField(field);
    }

    @Override
    public boolean isLegal() {
    	if (isAuthenticated())
    		return !Strings.isNullOrEmpty(user) && !Strings.isNullOrEmpty(password) && !Strings.isNullOrEmpty(ipAddress);
    	else
    		return !Strings.isNullOrEmpty(ipAddress);
    }

    @Override
    public boolean setStatus(Database database) {
    	return false;
    }


    
    @Override
	String update(String field, String v, JSONObject jUpdate, Map<String, String> params) throws JSONException {
    	String oldValue = null;
    	boolean resetComm = false;
    	if (IFields.IP_ADDRESS.equals(field)) {
    		oldValue = ipAddress;
    		if (false == oldValue.equals(v)) 
    			resetComm = true;
    	}
    	else if (IFields.USER.equals(field)) {
    		oldValue = user;
    		if (false == oldValue.equals(v))
    			resetComm = true;
    	}
    	else if (IFields.PASSWORD.equals(field)) {
    		oldValue = password;
    		if (false == oldValue.equals(v))
    			resetComm = true;
    	} 
    	else if (IFields.AUTHENTICATE.equals(field)) {
    		oldValue = authenticate;
    		if (false == oldValue.equals(v))
    			resetComm = true;
   	} else
    		return super.update(field, v, jUpdate, params);

    	setField(field, v, oldValue);
    	
    	if (resetComm) {
			setStatus(false);
			communicator = null;
    	}
    	return null;
	}

    @Override
    public void setStatus(String status) {
    	// Ignore
    }

    public void setStatus(boolean ok) {
    	if (ok)
    		status = GRAPHIC_YES;
    	else
    		status = GRAPHIC_NO;
    }

    public boolean isReady()
    {
        return (false == isArchived()) && isLegal();
    }

    @Override
    public void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException
    {
        // TODO Auto-generated method stub
    }

	public String getUserId()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
        markModified();
    }

	public String getPassword()
    {
        return password;
    }

//    public void setPassword(String password)
//    {
//        this.password = password;
//        markModified();
//    }

	public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
        markModified();
    }

    /**
     * Answers with a Friend for the given url
     * @param friends List of all friends in the database
     * @param fIp String of the known IP address of the friend.
     * @return Friend, or null if none was found.
     */
	public static Friend findFriendFromUrl(List<Friend> friends, String fIp) {
		for (Friend f: friends)
			if (f.getIpAddress().equals(fIp))
				return f;
		return null;
	}

	public JSONObject getItemsFromFriend() throws JSONException {
		JSONObject result = new JSONObject();
		try {
			if (! isLoggedIn()) {
				result.put("Error", "Failed to login into friend [" + getDisplayId() + "].");
				return result;
			}
			String url = "https://" + getIpAddress() + "/dm/smProtege/big";
			HttpGet get = new HttpGet(url);
			get.setHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
			System.out.println("Importing from URL [" + url + "] for [" + getDisplayId() + "].");
			HttpResponse resp = tryComm(get);
			if (null == resp) {
				// Try the old way, until we have latest version all over...
				url = "https://" + getIpAddress() + "/dm/smProtege";
				get = new HttpGet(url);
				System.out.println("Importing from URL [" + url + "] for [" + getDisplayId() + "].");
				get.setHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
				resp = execute(get);
			}
			System.out.println("... Succeeded to import list of items.");
			JSONObject answer = Utils.jsonFromString(Utils.stringFromStream(resp.getEntity().getContent()));
			return answer;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("Error", "Failed to parse results from friend @[" + getIpAddress() + "]. " + e.getClass() + ": " + e.getMessage());
			return result;
		}
	}

	public boolean isLoggedIn() {
		if (! isAuthenticated() && null != getCommunicatorNoAuth())
			return true;
		if (isAuthenticated() && null != getCommunicator())
			return true;
		return false;
	}

	private HttpResponse tryComm(HttpGet get) {
		try {
			return execute(get);
		} catch (Exception e) {
			return null;
		}
	}
	public static Friend generateRandom()
    {
        Friend o = new Friend();
        o = o.generateRandomBase(o);
        o.archived = true;
        //o.collectionName = "COLLECTION_NAME_Friend";
        o.dateCreated = U.roundDateToSecond(null);
        o.lastModified = U.roundDateToSecond(null);
        o.dirty = false;
        o.lastModified = U.roundDateToSecond(null);
        o.name = "NAME_VALUE_Friend";
        o.status = "STATUS VALUE_Friend";
        o.version = "VERSION VALUE_Friend";
        return o;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Friend other = (Friend) obj;
        if (user == null)
        {
            if (other.user != null) return false;
        }
        else
            if (!user.equals(other.user)) return false;
        if (password == null)
        {
            if (other.password != null) return false;
        }
        else
            if (!password.equals(other.password)) return false;
        if (ipAddress == null)
        {
            if (other.ipAddress != null) return false;
        }
        else
            if (!ipAddress.equals(other.ipAddress)) return false;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else
            if (!name.equals(other.name)) return false;
        return true;
    }

//    @Override
    public String _toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" {\n\tname: ");
        builder.append(name);
        builder.append("\n\tuser: ");
        builder.append(user);
        builder.append("\n\tpassword: ");
        builder.append(password);
        builder.append("\n\tipAddress: ");
        builder.append(ipAddress);
        builder.append("\n\tversion: ");
        builder.append(version);
        builder.append("\n\tstatus: ");
        builder.append(status);
        builder.append("\n\tarchived: ");
        builder.append(archived);
        builder.append("\n\tcollectionName: ");
        builder.append(collectionName);
        builder.append("\n\tdateCreated: ");
        builder.append(dateCreated);
        builder.append("\n\tdirty: ");
        builder.append(dirty);
        builder.append("\n\tid: ");
        builder.append(id);
        builder.append("\n\tidURI: ");
        builder.append(idURI);
        builder.append("\n\tbaseURL: ");
        builder.append(baseURL);
        builder.append("\n\tlastModified: ");
        builder.append(lastModified);
        builder.append("\n\tetag: ");
        builder.append(etag);
        builder.append("\n}");
        return builder.toString();
    }

	public boolean isAuthenticated() {
		return "on".equals(authenticate);
	}

	public HttpResponse execute(HttpUriRequest req) throws OAuthCommunicatorException, ClientProtocolException, IOException {
		if (isAuthenticated()) {
			OAuthCommunicator comm = getCommunicator();
			return comm.execute(req);
		} else {
			HttpClient commNoAuth = getCommunicatorNoAuth();
			return commNoAuth.execute(req);
		}
	}


	public boolean doAuth() {
		return false;
	}
}
// ============================= End of  Mediators ==============================

