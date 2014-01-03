package perf.http;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientQueue {
    private static LinkedBlockingQueue<HttpClient> freeClients;

    public static void initialize(List<HttpClient> clients) throws InterruptedException {
        freeClients = new LinkedBlockingQueue<HttpClient>(clients.size());
        freeClients.addAll(clients);
    }

    public static void freeClient(HttpClient client) throws InterruptedException {
        freeClients.put(client);
    }

    public static HttpClient pickClient() throws InterruptedException {
        return freeClients.take();
    }

    public static void clearClients() throws InterruptedException {
        for(int i=0; i< freeClients.size(); i++)
            freeClients.take().close();
    }
}
