package com.example.finalproject;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Communication {
    private static HttpURLConnection con = null;

    //todo - change logger
    private static final Logger log = LogManager.getLogManager().getLogger("RequestHandler");

    public interface Action {
        <T> T doRequest() throws IOException;
    }

    /////WITH CALLBACK

    public static <T> T makePostRequest(String request, Map<String, String> header, final Object body, final Class<T> myClass){
        return makeRequest(request, header, "POST", new Action() {
            @Override
            public <T> T doRequest() throws IOException {
                setBody(body);
                return getJsonObject((Class<T>) myClass, false);
            }
        });
    }

    public static int makePostRequestGetCode(String request, Map<String, String> header, final Object body) {
        return makeRequest(request, header, "POST", new Action() {
            @Override
            public <T> T doRequest() throws IOException {
                setBody(body);
                return (T) getCode();
            }
        });
    }

    public static Map<String, List<String>> makePostRequestGetResponseHeader(String request, Map<String, String> header, final Object body) {
        return makeRequest(request, header, "POST", new Action() {
            @Override
            public <T> T doRequest() throws IOException {
                setBody(body);
                return (T) getResponseHeader();
            }
        });
    }

    private static Map<String, List<String>> getResponseHeader() {
        //return con.getHeaderFields();
        int i = -1;
        try {
            i = con.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return con.getHeaderFields();
    }

    public static int makeGetRequestGetCode(String request, Map<String, String> headers) {
        return makeRequest(request, headers, "GET", new Action() {
            @Override
            public <T> T doRequest() throws IOException {
                return (T) getCode();
            }
        });
    }

    public static <T> T makeGetRequest(String request, Map<String, String> headers, final Class<T> myClass) {
        return makeRequest(request, headers, "GET", new Action() {
            @Override
            public <T> T doRequest() throws IOException {
                return getJsonObject((Class<T>) myClass,false );
            }
        });
    }

    public static <T> List<T> makeGetRequestGetList(String request, Map<String, String> headers, final Class<T> myClass) {
        return makeRequest(request, headers, "GET", new Action() {
            @Override
            public <T> T doRequest() throws IOException {
                return getJsonObject((Class<T>) myClass, true);
            }
        });
    }


    ///HELPERS

    /**
     * @param request - the url
     * @param header
     * @param type - GET / POST ....
    //* @param myClass - Integer.class for response Code, or T class for json object
     * @param action - call back for set the post request body (file, object, ...)
     * @param <T> - Integer.class for response Code, or T class for json object
     * @return - the response code or the T class from the jsonObject
     */
    private static <T> T makeRequest(String request, Map<String, String> header, String type, Action action) {
        URL url;
        T returnVal = null;
        ignoreSSL();
        try {
            url = new URL(request);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(type);
            if (header != null) setHeader( header);
            returnVal = action.doRequest();
        } catch (IOException e) {
            log.warning("make Request FAIL" + e);
        }
        con.disconnect();

        return returnVal;
    }

    private static void ignoreSSL(){
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        // Install the all-trusting trust manager
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();}

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private static void setHeader( Map<String, String> header) {
        Set<String> headerType = header.keySet();
        for (String type : headerType) {
            con.setRequestProperty(type, header.get(type));
        }
    }

    private static void setBody(Object body)  {
        if (body == null) return;
        byte[] entity = getBytes(body);
        con.setDoOutput(true);
        try {
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(entity);
            wr.flush();
            wr.close();
        }catch (IOException e){
            log.warning("set body FAIL" + e);
        }
    }

    private static byte[] getBytes(Object body){
        if (body.getClass() == byte[].class) return (byte[]) body;
        else {
            String json = (new Gson()).toJson(body);
            return json.getBytes();
        }
    }

    /**
     * @param myClass
     * @param isList - convert to List<myClass> or myClass
     * @param <T>
     * @return List<myClass> or myClass
     */
    private static <T> T getJsonObject(Class<T> myClass, boolean isList) {
        T returnVal = null;
        try {
            InputStream response = con.getInputStream();
            String jsonReply = convertStreamToString(response);
            if (isList) returnVal = (T) getObjectsList(jsonReply, myClass);
            else returnVal = (new Gson()).fromJson(jsonReply, myClass);
        } catch(IOException | JSONException e) {
            log.warning("get json object FAIL" + e);
        }
        return returnVal;
    }

    private static String convertStreamToString(InputStream stream) throws IOException {
        // To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.
        if (stream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                stream.close();
            }
            return writer.toString();
        }
        return "";
    }


    private static <T> List<T> getObjectsList(String stream, Class<T> myClass) throws JSONException {
        ArrayList<T> objectList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(stream);
        int len = jsonArray.length();
        for (int i=0;i<len;i++){
            objectList.add((new Gson()).fromJson(jsonArray.get(i).toString(), myClass));
        }
        return objectList;
    }



    private static Integer getCode(){
        try {
            return con.getResponseCode();
        } catch (IOException e) {
            log.warning("get code FAIL" + e);
        }
        return null;
    }


    public static <T> T getObjectFromJsonFile(Class<T> myClass, FileReader file) {
        BufferedReader br = new BufferedReader(file);
        return new Gson().fromJson(br, myClass);
    }




}