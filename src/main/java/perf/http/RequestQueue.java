package perf.http;

import java.util.concurrent.LinkedBlockingQueue;

public class RequestQueue {
    private  LinkedBlockingQueue<Long> pendingRequests;
    
    public RequestQueue(int requestCount){
    	pendingRequests = new LinkedBlockingQueue<Long>(requestCount);
    }
    
    public  void initialize(int count) {
        pendingRequests = new LinkedBlockingQueue<Long>(count);
    }

    public  void raiseRequest(Long index) throws InterruptedException {
        pendingRequests.put(index);
    }

    public Long pickRequest() throws InterruptedException {
        return pendingRequests.take();
    }
}
