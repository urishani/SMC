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
package com.ibm.haifa.smc.client.oauth;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;


public class OAuthCommunicator implements  Cloneable
{
    private static final Logger           l                        = Logger.getLogger(OAuthCommunicator.class);
    private static final String release = "OAuth client release 2.0 Feb 21, 2016 (CR) IBM - with optional auth.";

    protected static final String         JSESSION_COOKIE_NAME     = "JSESSIONID";
    protected static final Header         HEADER_FORM_CONTENT_TYPE = new BasicHeader("Content-type", "application/x-www-form-urlencoded");
    protected final DefaultHttpClient     httpClient;
    protected PropertyChangeListener      propertyChangeListener;
    protected final IUserCredentials      iUserCredentials;
    protected final String                sslProtocol;
    protected String                      interceptedUri;
    protected String                      uriQueryPrt;

    public static final String getRelease() {
    	return release;
    }
    protected static final TrustManager[] trustManagers            = new TrustManager[] { new X509TrustManager() {
                                                                       public X509Certificate[] getAcceptedIssuers()
                                                                       {
                                                                           return new X509Certificate[0];
                                                                       }

                                                                       public void checkClientTrusted(X509Certificate[] certs, String authType)
                                                                       {

                                                                       }

                                                                       public void checkServerTrusted(X509Certificate[] certs, String authType)
                                                                       {}
                                                                   } };
    //    protected static final ICertificationHandler certificationHandler     = new ICertificationHandler() {
    //                                                                              public boolean acceptCertificate(X509Certificate[] chain)
    //                                                                              {
    //                                                                                  return true;
    //                                                                              }
    //                                                                          };

    protected static final TrustStrategy  allTrustStrategy         = new TrustStrategy() {

                                                                       @Override
                                                                       public boolean isTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException
                                                                       {
                                                                           return true;
                                                                       }
                                                                   };

    public OAuthCommunicator(final String username, final String password) throws OAuthCommunicatorException
    {
        this(new IUserCredentials() {

            @Override
            public String getUserId()
            {
                return username;
            }

            @Override
            public String getPassword()
            {
                return password;
            }
            @Override
            public boolean doAuth() { return true; }
        }, "SSLV3");
    }

    public OAuthCommunicator(IUserCredentials credentials) throws OAuthCommunicatorException
    {
        this(credentials, "SSLV3");
    }

    private String getJSessionId()
    {
        List<Cookie> cookies = this.httpClient.getCookieStore().getCookies();
        for (Cookie cookie : cookies)
        {
            if (JSESSION_COOKIE_NAME.equals(cookie.getName())) { return cookie.getValue(); }
        }
        return null;
    }

    public OAuthCommunicator(IUserCredentials credentials, String sslProtocol) throws OAuthCommunicatorException
    {
        this.propertyChangeListener = null;

        this.interceptedUri = null;
        this.uriQueryPrt = null;

        if ((!("TLS".equals(sslProtocol))) && (!("SSLV3".equals(sslProtocol)))) { throw new OAuthCommunicatorException("Only SSLV3 and TLS are currently accepted SSL protocols"); }

        this.iUserCredentials = credentials;
        this.sslProtocol = sslProtocol;

        final HttpParams httpParams = new BasicHttpParams();

        HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(httpParams, true);
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        try
        {
            final SSLContext context = SSLContext.getInstance(sslProtocol);
            context.init(null, trustManagers, null);
            final SSLSocketFactory socketFactory = new SSLSocketFactory(context, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            final Scheme scheme = new Scheme("https", 443, socketFactory);
            schemeRegistry.register(scheme);
        }
        catch (KeyManagementException e)
        {
            throw new OAuthCommunicatorException(e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new OAuthCommunicatorException(e);
        }

        final PoolingClientConnectionManager poolingClientConnectionManager = new PoolingClientConnectionManager(schemeRegistry);

        poolingClientConnectionManager.setMaxTotal(100);

        this.httpClient = new DefaultHttpClient(poolingClientConnectionManager, httpParams);
        final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy() {
            public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException
            {
                int statuCode = response.getStatusLine().getStatusCode();
                if ((statuCode == 301) || (statuCode == 302))
                {
                    Header[] headers = response.getHeaders("location");
                    if ((headers != null) && (headers.length == 1))
                    {
                        OAuthCommunicator.this.interceptedUri = headers[0].getValue();
                        return false;
                    }
                }
                return super.isRedirected(request, response, context);
            }

        };
        this.httpClient.setRedirectStrategy(redirectStrategy);
    }

    public List<Cookie> getCookies()
    {
        return this.httpClient.getCookieStore().getCookies();
    }

    public void addCookies(List<Cookie> cookies)
    {
        for (Cookie cookie : cookies)
            this.httpClient.getCookieStore().addCookie(cookie);
    }

    public void addCookie(Cookie cookie)
    {
        this.httpClient.getCookieStore().addCookie(cookie);
    }

    public void cleanupConnections(HttpResponse someResponse)
    {
        closeConnection(someResponse);
    }

    public synchronized void setPropertyChangeListener(PropertyChangeListener l)
    {
        this.propertyChangeListener = l;
    }

    public synchronized HttpResponse executeAndDump(HttpUriRequest request) throws OAuthCommunicatorException
    {
        HttpResponse response = execute(request);

        System.out.println(response.getStatusLine().toString());
        Header[] headers = response.getAllHeaders();
        for (Header header : headers)
        {
            System.out.println(header.getName() + " : " + header.getValue());
        }
        HttpEntity entity = response.getEntity();

        if (entity != null)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(baos);
            BufferedInputStream bufIn;
            try
            {
                bufIn = new BufferedInputStream(entity.getContent());
            }
            catch (IllegalStateException e1)
            {
                closeConnection(response);
                throw new OAuthCommunicatorException(e1);
            }
            catch (IOException e1)
            {
                closeConnection(response);
                throw new OAuthCommunicatorException(e1);
            }
            try
            {
                int c = bufIn.read();
                int length = 0;
                while (c != -1)
                {
                    bos.write(c);
                    ++length;
                    c = bufIn.read();
                    if (length <= 0) { throw new OAuthCommunicatorException("Cannot produce a byte array for a response larger than 2147483647 bytes"); }
                }

                bos.flush();
                baos.flush();
                System.out.println(new String(baos.toByteArray(), "UTF-8"));
            }
            catch (IOException e)
            {
                closeConnection(response);
                throw new OAuthCommunicatorException(e);
            }
            finally
            {
                try
                {
                    bos.close();
                    bufIn.close();
                }
                catch (IOException e)
                {
                    closeConnection(response);
                    throw new OAuthCommunicatorException(e);
                }
            }
        }
        return response;
    }

    public synchronized HttpResponse execute(HttpUriRequest request) throws OAuthCommunicatorException
    {
        HttpResponse response = null;
        try
        {
            if (!(request.getURI().getScheme().equals("https"))) { throw new OAuthCommunicatorException(new HttpSchemeNotSupported()); }

            String sessionId = getJSessionId();
            if (sessionId != null)
            {
                request.addHeader("X-Jazz-CSRF-Prevent", sessionId);
            }

            Header hd = request.getFirstHeader("Accept-Language");
            if (hd == null)
            {
                request.addHeader("Accept-Language", Locale.getDefault().toString());
            }

            response = this.httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            if ((statusCode == 301) || (statusCode == 302))
            {
                this.uriQueryPrt = URI.create(this.interceptedUri).getRawQuery() + "&authorize=true";
            }
            else
            {
                if (statusCode == 401)
                {
                    this.httpClient.getCookieStore().clear();
                }
                response = reactToAuthRequest(request.getURI(), response);
            }

            while (this.interceptedUri != null)
            {
                String interceptUriAsString = this.interceptedUri;
                URI interceptUri = URI.create(interceptUriAsString);

                this.interceptedUri = null;

                if (interceptUriAsString.indexOf("auth/authrequired") != -1)
                {
                    String newPath = interceptUri.getPath().replaceFirst("auth/authrequired", "j_security_check");
                    this.interceptedUri = interceptUri.resolve(newPath).toString();
                }
                else
                    if ((interceptUriAsString.indexOf("login") != -1) || (interceptUriAsString.indexOf("j_security_check") != -1))
                    {
                        closeConnection(response);
                        response = postCredentials(interceptUri, request.getURI().toString());
                    }
                    else
                        if (interceptUriAsString.indexOf("authenticated") != -1)
                        {
                            HttpGet executeOAuthRequest = new HttpGet(interceptUri);
                            closeConnection(response);
                            response = this.httpClient.execute(executeOAuthRequest);

                            response = reactToAuthRequest(interceptUri, response);
                        }
                        else
                            if (interceptUriAsString.indexOf("oauth-authorize") != -1)
                            {
                                closeConnection(response);
                                response = postAuthorization(interceptUri);
                            }
                            else
                            {
                                try
                                {
                                    RequestWrapper newRequest;
                                    if (request instanceof HttpEntityEnclosingRequest)
                                    {
                                        HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
                                        EntityEnclosingRequestWrapper newEntityRequest = new EntityEnclosingRequestWrapper(entityRequest);
                                        newEntityRequest.setEntity(entityRequest.getEntity());
                                        newRequest = newEntityRequest;
                                    }
                                    else
                                    {
                                        newRequest = new RequestWrapper(request);
                                    }

                                    String originalQueryPart = request.getURI().getQuery();
                                    if ((originalQueryPart != null) && (originalQueryPart.length() > 0))
                                    {
                                        String newQueryPart = interceptUri.getQuery();
                                        if ((newQueryPart == null) || (newQueryPart.length() == 0))
                                        {
                                            String fragment = interceptUri.getFragment();
                                            interceptUriAsString = interceptUri.toString() + "?" + originalQueryPart;
                                            if (fragment != null)
                                            {
                                                interceptUriAsString = interceptUriAsString + "#" + fragment;
                                            }
                                            interceptUri = URI.create(interceptUriAsString);
                                        }
                                    }

                                    newRequest.setURI(interceptUri);
                                    newRequest.resetHeaders();

                                    sessionId = getJSessionId();
                                    if (sessionId != null)
                                    {
                                        newRequest.addHeader("X-Jazz-CSRF-Prevent", sessionId);
                                    }

                                    closeConnection(response);
                                    response = this.httpClient.execute(newRequest);
                                }
                                catch (ProtocolException e)
                                {
                                    closeConnection(response);
                                    throw new OAuthCommunicatorException(e);
                                }
                            }

            }

            Header webAuthMessage = response.getFirstHeader("X-com-ibm-team-repository-web-auth-msg");
            if ((webAuthMessage != null) && (webAuthMessage.getValue().equals("authfailed")))
            {
                closeConnection(response);
                l.error("InvalidUserCredentials");
                throw new InvalidUserCredentials();
            }

            if (response.getStatusLine().getStatusCode() >= 400) { throw new OAuthCommunicatorException(response); }

            return response;
        }
        catch (IllegalArgumentException e)
        {
            l.error("", e);
            throw new OAuthCommunicatorException(e);
        }
        catch (ClientProtocolException e)
        {
            l.error("", e);
            throw new OAuthCommunicatorException(e);
        }
        catch (IOException e)
        {
            l.error("", e);
            throw new OAuthCommunicatorException(e);
        }
        catch (RuntimeException e)
        {
            l.error("", e);
            throw new OAuthCommunicatorException(e);
        }
        finally
        {
            this.interceptedUri = null;
            this.uriQueryPrt = null;
        }
    }

    private HttpResponse reactToAuthRequest(URI baseUri, HttpResponse response) throws OAuthCommunicatorException, ClientProtocolException, IOException
    {
        HttpResponse newResponse = response;
        Header redirectHeader = response.getFirstHeader("X-jazz-web-oauth-url");
        if ((redirectHeader != null) && (redirectHeader.getValue().length() > 0))
        {
            HttpGet get = new HttpGet(redirectHeader.getValue());
            closeConnection(response);

            newResponse = this.httpClient.execute(get);

            this.uriQueryPrt = URI.create(this.interceptedUri).getRawQuery() + "&authorize=true";
        }
        else
        {
            Header webAuthMessage = response.getFirstHeader("X-com-ibm-team-repository-web-auth-msg");
            if ((webAuthMessage != null) && (webAuthMessage.getValue().equals("authrequired")))
            {
                redirectHeader = response.getFirstHeader("X-com-ibm-team-repository-web-auth-uri");
                if (redirectHeader != null)
                {
                    this.interceptedUri = redirectHeader.getValue();
                }
                else
                {
                    String formPath = null;
                    Header[] cookies = response.getHeaders("Set-Cookie");
                    for (Header cookie : cookies)
                    {
                        for (HeaderElement cookieElement : cookie.getElements())
                        {
                            NameValuePair cookieParameter = cookieElement.getParameterByName("Path");
                            if (cookieParameter != null)
                            {
                                formPath = cookieParameter.getValue();
                                break;
                            }
                        }
                        if (formPath != null)
                        {
                            break;
                        }
                    }
                    this.interceptedUri = baseUri.resolve(formPath + "/" + "j_security_check").toString();
                }
            }
        }

        return newResponse;
    }

    private HttpResponse postCredentials(URI loginUri, String redirectionUri) throws IOException, ClientProtocolException
    {
        sendToListener(new PropertyChangeEvent(this, "authenticating", null, null));
        HttpPost post = new HttpPost(loginUri);

        post.setHeader(HEADER_FORM_CONTENT_TYPE);

        String userName = this.iUserCredentials.getUserId();
        String password = this.iUserCredentials.getPassword();

        String body = "j_username=" + ((userName == null) ? "" : URLEncoder.encode(userName, "UTF-8")) + "&" + "j_password" + "=" + ((password == null) ? "" : URLEncoder.encode(password, "UTF-8"));
        if (redirectionUri != null)
        {
            redirectionUri = URLEncoder.encode(redirectionUri, "UTF-8");
            body = body + "&redirectPage=" + redirectionUri;
        }

        post.setEntity(new StringEntity(body, "UTF-8"));

        return this.httpClient.execute(post);
    }

    private HttpResponse postAuthorization(URI authorizationUri) throws UnsupportedEncodingException, IOException, ClientProtocolException
    {
        sendToListener(new PropertyChangeEvent(this, "authorizing", null, null));

        HttpPost post = new HttpPost(authorizationUri);

        post.setHeader(HEADER_FORM_CONTENT_TYPE);
        post.setEntity(new StringEntity(this.uriQueryPrt, "UTF-8"));

        return this.httpClient.execute(post);
    }

    private void sendToListener(PropertyChangeEvent evt)
    {
        if (this.propertyChangeListener != null) this.propertyChangeListener.propertyChange(evt);
    }

    private void closeConnection(HttpResponse response)
    {
        try
        {
            if (response != null)
            {
                EntityUtils.consume(response.getEntity());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public synchronized OAuthCommunicator clone() throws CloneNotSupportedException
    {
        try
        {
            OAuthCommunicator clone = new OAuthCommunicator(this.iUserCredentials, this.sslProtocol);
            List<Cookie> cookies = this.httpClient.getCookieStore().getCookies();
            CookieStore clonedCookieStore = clone.httpClient.getCookieStore();
            for (Cookie cookie : cookies)
            {
                clonedCookieStore.addCookie(cookie);
            }
            return clone;
        }
        catch (OAuthCommunicatorException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void setTimeout(int seconds)
    {
        HttpParams httpParams = this.httpClient.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, seconds * 1000);
    }
}
