package org.wltea.analyzer.dic;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

/**
 * 远程调用文件并定时检查更新
 * add by liangyongxing
 * @createTime 2017-02-22
 */
public class Monitor implements Runnable {
    private static CloseableHttpClient httpclient = HttpClients.createDefault();
    private String last_modified;
    private String eTags;
    private String location;

    public Monitor(String location) {
        this.location = location;
        this.last_modified = null;
        this.eTags = null;
    }

    @Override
    public void run() {
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10000).setConnectTimeout(10000).setSocketTimeout(15000).build();

        HttpHead head = new HttpHead(this.location);
        head.setConfig(rc);

        if (this.last_modified != null) {
            head.setHeader("If-Modified-Since", this.last_modified);
        }
        if (this.eTags != null) {
            head.setHeader("If-None-Match", this.eTags);
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(head);

            if (response.getStatusLine().getStatusCode() == 200) {
                if ((!response.getLastHeader("Last-Modified").getValue().equalsIgnoreCase(this.last_modified))
                        || (!response.getLastHeader("ETag").getValue().equalsIgnoreCase(this.eTags))) {
                    DictionaryRemote.getSingleton().reLoadMainDict();
                    this.last_modified = (response.getLastHeader("Last-Modified") == null ? null : response.getLastHeader("Last-Modified").getValue());
                    this.eTags = (response.getLastHeader("ETag") == null ? null : response.getLastHeader("ETag").getValue());
                }
            } else if (response.getStatusLine().getStatusCode() != 304) {
                System.err.println("remote_ext_dict " + this.location + " return bad code " + response.getStatusLine().getStatusCode() + "");
            }
        } catch (Exception e) {
            System.err.println("remote_ext_dict  error!" + e.getStackTrace());
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
