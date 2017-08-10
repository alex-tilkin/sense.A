package com.exampledemo.parsaniahardik.gpsdemocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class HttpRequestThread extends Thread {

    //region Fields
    private Boolean mBkeepRunning;
    private ArrayList<String> mListeRequest;
    private Object mSyncObj;
    private Semaphore oRequestQueueMutex;
    private AutoResetEvent oAutoResentEvent;
    private IHttpRequestResponse mHttpRequestResponse;
    //endregion

    //region API
    public HttpRequestThread(IHttpRequestResponse Caller) {
        oRequestQueueMutex = new Semaphore(1);
        oAutoResentEvent = new AutoResetEvent(false);
        mListeRequest = new ArrayList<String>();
        mBkeepRunning = true;
        mSyncObj = new Object();
        mHttpRequestResponse = Caller;
    }

    public void AddRequest(String strUrl) {
        try {
            oRequestQueueMutex.acquire();
            mListeRequest.add(strUrl);
            oRequestQueueMutex.release();
            oAutoResentEvent.set();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
            while (mBkeepRunning) {
                try {
                    oAutoResentEvent.waitOne();

                    oRequestQueueMutex.acquire();
                    String strUrl = mListeRequest.remove(0);
                    oRequestQueueMutex.release();

                    RetrieveHttpResponse(strUrl);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
            }
    }
    //endregion

    //region Helpers
    private void RetrieveHttpResponse(String strUrl) throws ProtocolException {
        URL url = null;
        try {
            url = new URL(strUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setDoInput(true);
            con.setDoOutput(true);

            //int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));

            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            mHttpRequestResponse.OnHttpResponse(content.toString());
            in.close();
            con.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //endregion
}