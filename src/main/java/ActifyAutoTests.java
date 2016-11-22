import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;

//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActifyAutoTests {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME =
            "Google Sheets API Java ActifyAutoTests";

    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/sheets.googleapis.com-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(SheetsScopes.SPREADSHEETS);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                ActifyAutoTests.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Sheets API client service.
     *
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() throws IOException {
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void writeToCell(Sheets service, String spreadsheetId, Integer code, Integer expectedCode, Integer cell) {
        List<Object> cellValue = new ArrayList<Object>();
        cellValue.add(code);
        List<List<Object>> cellValues = new ArrayList<List<Object>>();
        cellValues.add(cellValue);

        ValueRange cells = new ValueRange();
        String range = "API!F" + Integer.toString(cell);
        cells.setRange(range);
        cells.setValues(cellValues);
        try {
            service.spreadsheets().values().update(spreadsheetId, range, cells).setValueInputOption("RAW").execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        /*
         *  fix for
         *    Exception in thread "main" javax.net.ssl.SSLHandshakeException:
         *       sun.security.validator.ValidatorException:
         *           PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
         *               unable to find valid certification path to requested target
         */
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * end of the fix
         */


        // Build a new authorized API client service.
        Sheets service = getSheetsService();

        String spreadsheetId = "1sRXzPegvdl3rtwGhyIDzS3c7QrV3jupLzpeo-y2Nzjk";
        String range = "API!C6:E";

        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();


        int i = 6;
        List<List<Object>> values = response.getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
        } else {
            int actualResponseCode;
            for (List row : values) {
                if (row.get(0).toString().equals("GET")) {
                    System.out.println("Executing " + row.get(1).toString());
                    actualResponseCode = executeGet(row.get(1).toString());
                    System.out.println("Response code is " + Integer.toString(actualResponseCode));
                    Matcher matcher = Pattern.compile("[^0-9]*([0-9]+).*").matcher(row.get(2).toString());
                    if (matcher.matches()) {
                        writeToCell(service, spreadsheetId, actualResponseCode, Integer.parseInt(matcher.group(1)), i);
                    } else {
                        System.out.println("Cannot get expected code");
                    }
                }
                i++;
            }
        }
    }

    public static Integer executeGet(String targetURL) {
        HttpURLConnection connection = null;

        try {
            // Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic YW1ncmFkZTpsZXRtZWlu");
            connection.setRequestProperty("Accept-Language", "en-US;q=1, ru-US;q=0.9");
            connection.setRequestProperty("X-Device", "prerpod_712");
            connection.setRequestProperty("X-Auth", "prerpod_712:YjM2NTYwNmM3OGMxMmE4NjMwMWQ0MTczMjFhYWY5Njg4NjhmMTA0OQ");
            connection.setRequestProperty("Content-Type", "application/vnd.api+json");
            connection.setRequestProperty("User-Agent", "actify/1.0 (iPhone; iOS 9.2; Scale/2.00)");
            connection.setRequestProperty("X-Timezone", "Europe/Zaporozhye");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

//    	    int responseCode = connection.getResponseCode();

//    	    //Get Response  
//    	    InputStream is = connection.getInputStream();
//    	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
//    	    StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
//    	    String line;
//    	    while ((line = rd.readLine()) != null) {
//    	      response.append(line);
//    	      response.append('\r');
//    	    }
//    	    rd.close();
            return connection.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
            return 500;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
