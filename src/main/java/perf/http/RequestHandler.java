package perf.http;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class RequestHandler extends AsyncCompletionHandler<String>{

    private HttpClient client;
    private long startTime;
    private ClientExecutorThread cet;

    public RequestHandler(ClientExecutorThread cet,HttpClient client, long startTime) {
        this.client = client;
        this.startTime = startTime;
        this.cet =cet;
    }
    @Override
    public String onCompleted(Response response) throws Exception {
//        String responseStr = response.getResponseBody();
    	//System.out.println(response.getResponseBody());
        client.complete(cet,System.currentTimeMillis() - this.startTime);
//        return responseStr;
        return "";
    }
}
