package com.example.mdol.feedback;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class UploadFileAsync extends AsyncTask<File, Void, String> {

    private HttpsURLConnection conn = null;
    private DataOutputStream dos = null;
    private String lineEnd = "\r\n";
    private String twoHyphens = "--";
    private String boundary = "*****";
    private int bytesRead, bytesAvailable, bufferSize;
    private byte[] buffer;
    private int maxBufferSize = 1024 * 1024;

    @Override
    protected String doInBackground(File... params)  {
        for (File file:params) {
            String fileName = file.getName();
            Log.d("MDOL_DEBUG", "Trying to upload " + fileName + "(" + file.length()/1000000.0 + " MB)");
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bytes = new byte[fileInputStream.available()];
                fileInputStream.read(bytes);
                int n = (int)Math.ceil(bytes.length/2000000.0);
                if(n>1) {
                    String[] fileName_split = fileName.split("\\.");
                    String ext = fileName_split[fileName_split.length-1];
                    boolean ok = true;
                    for (int i = 0; i < n; i++) {
                        String fileName_i = fileName.substring(0,fileName.length()-ext.length()-1)  + "_" + i + "." + ext;
                        int from = i * 2000000;
                        int to = Math.min((i + 1) * 2000000, bytes.length);
                        byte[] bytes_i = Arrays.copyOfRange(bytes, from, to);
                        int status_i = sendBytes(bytes_i, fileName_i);
                        switch (status_i) {
                            case 1:
                                Log.d("MDOL_DEBUG", fileName + "(" + i + ") - File Uploaded");
                                break;
                            case -1:
                                Log.d("MDOL_DEBUG", fileName + "(" + i + ") - File Already exists");
                                break;
                            case -2:
                                Log.d("MDOL_DEBUG", fileName + "(" + i + ") - File is not valid");
                                break;
                            case -3:
                                Log.d("MDOL_DEBUG", fileName + "(" + i + ") - Error");
                                break;
                            case -4:
                                Log.d("MDOL_DEBUG", fileName + "(" + i + ") - Exception");
                                break;
                        }
                        if (status_i != 1 && status_i != -1)
                            ok = false;
                    }
                    if(ok) {
                        Log.d("MDOL_DEBUG", file.getName() + " - File Uploaded or Exists - " + (file.delete() ? "deleted from device" : "deletion failed"));
                    }
                }
                else
                    switch (sendBytes(bytes,fileName)) {
                        case 1:
                            Log.d("MDOL_DEBUG", file.getName() + " - File Uploaded - " + (file.delete() ? "deleted from device" : "deletion failed"));
                            break;
                        case -1:
                            Log.d("MDOL_DEBUG", file.getName() + " - File Already exists - " + (file.delete() ? "deleted from device" : "deletion failed"));
                            break;
                        case -2:
                            Log.d("MDOL_DEBUG", file.getName() + " - File is not valid");
                            break;
                        case -3:
                            Log.d("MDOL_DEBUG", file.getName() + " - Error");
                            break;
                        case -4:
                            Log.d("MDOL_DEBUG", file.getName() + " - Exception");
                            break;
                    }
            }
            catch(IOException ex)
            {

            }
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result) {

    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }

    private int sendBytes(byte[] bytes,String fileName)
    {
        try {
            // open a URL connection to the server (INPUT YOUR OWN SERVER)
			string phpFile = "upload.php";
			string hostname = "yourdomain";
            URL url = new URL(phpFile);

            // Open a HTTP  connection to  the URL
            conn = (HttpsURLConnection) url.openConnection();

            conn.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return hostname.equals(hostname);
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            conn.setSSLSocketFactory(context.getSocketFactory());

            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", fileName);

            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + fileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.write(bytes);

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();

            // Responses from the server (code and message)
            int serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();
            if (serverResponseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                String response = "";
                while ((line = br.readLine()) != null) {
                    response = line;
                }
                return Integer.parseInt(response);
            }

        } catch (Exception ex) {

            ex.printStackTrace();
            Log.e("MDOL_DEBUG", "error: " + ex.getMessage(), ex);
        }
        return -4;
    }
}