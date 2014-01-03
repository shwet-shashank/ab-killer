
package perf.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.extra.ThrottleRequestFilter;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpClient {
    private AsyncHttpClient asyncHttpClient;
    private AsyncHttpClient.BoundRequestBuilder requestBuilder;

    private long completed = 0;
    private List<Long> requestTimes = new ArrayList<Long>();
    private static int cnt=1;
    private String localUrl;

    public HttpClient(String url, KillAB.HTTPMethods httpMethod, String dataFile, String contentType) throws FileNotFoundException, IOException {
    	AsyncHttpClientConfig cc = new AsyncHttpClientConfig.Builder().
				setAllowPoolingConnection(true).
				setMaximumConnectionsTotal(200).
				addRequestFilter(new ThrottleRequestFilter(1)). 
				build();
        asyncHttpClient = new AsyncHttpClient(cc);
        asyncHttpClient.prepareConnect(url);
        localUrl = url;
        
        switch(httpMethod) {
            case GET:
                //requestBuilder = asyncHttpClient.prepareGet(url);
                break;
            case POST:
            	FileInputStream finPost = new FileInputStream(new File(dataFile));
    			byte[] reqPostData = new byte[finPost.available()];
    			finPost.read(reqPostData);
                requestBuilder = asyncHttpClient.preparePost(url).setBody(reqPostData);
                break;
            case PUT:
            	FileInputStream finPut = new FileInputStream(new File(dataFile));
    			byte[] reqPutData = new byte[finPut.available()];
    			finPut.read(reqPutData);
                requestBuilder = asyncHttpClient.preparePut(url).setBody(reqPutData);
                break;
            case DELETE:
                requestBuilder = asyncHttpClient.prepareDelete(url);
                break;
            default:
                throw new RuntimeException("Don't understand Http Method "+httpMethod);

        }
        //requestBuilder.addHeader("Content-Type", contentType);
    }

    public void execute(ClientExecutorThread cet) throws IOException {
    	requestBuilder = asyncHttpClient.prepareGet(localUrl + "/" + cnt++);
        requestBuilder.execute(new RequestHandler(cet,this, System.currentTimeMillis()));
    }

    public void complete(ClientExecutorThread cet, long requestTime) throws InterruptedException {
        requestTimes.add(requestTime);
        cet.freeClient(this);
    }

    public void close() {
        asyncHttpClient.close();
    }

    public List<Long> getRequestTimes() {
        return requestTimes;
    }
}
