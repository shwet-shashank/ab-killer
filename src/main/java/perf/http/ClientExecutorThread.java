package perf.http;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

//import static perf.http.ClientQueue.pickClient;


public class ClientExecutorThread extends Thread{
    private long startTime;
    private int requests;
    private long timeWasted = 0;
    
    private RequestQueue requestQueue;
    private LinkedBlockingQueue<HttpClient> clientQueue;        //imp v2
    
    public ClientExecutorThread(int requests) {
        this.requests = requests;
        requestQueue = new RequestQueue(requests);
        clientQueue = new LinkedBlockingQueue<HttpClient>();
    }
    
    public void addClient(HttpClient client) {
    	this.clientQueue.add(client);
    }
    
    public HttpClient pickClient() throws InterruptedException{
    	return clientQueue.take();
    }
    
    public void freeClient(HttpClient client) throws InterruptedException {
        clientQueue.put(client);
    }
    
    public void addRequest(Long req) throws InterruptedException {
    	this.requestQueue.raiseRequest(req);
    }
    
    public void run() {
        startTime = System.currentTimeMillis();
        while(requests > 0) {
            try {
                long wasteStartTime = System.currentTimeMillis();
                this.requestQueue.pickRequest();
                HttpClient client = pickClient();
                client.execute(this);
                requests--;
                timeWasted += System.currentTimeMillis() - wasteStartTime;
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        System.out.println("Client Executor Thread Over");
    }

    public long getStartTime() {
        return startTime;
    }

    public long getTimeWasted() {
        return timeWasted;
    }
}