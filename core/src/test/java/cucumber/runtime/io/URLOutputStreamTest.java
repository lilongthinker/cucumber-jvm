package cucumber.runtime.io;

import cucumber.runtime.Utils;
import gherkin.util.FixJava;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.rest.Rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class URLOutputStreamTest {
    private WebServer webbit;

    @Before
    public void startWebbit() throws ExecutionException, InterruptedException {
        webbit = WebServers.createWebServer(9873).start().get();
    }

    @After
    public void stopWebbit() throws ExecutionException, InterruptedException {
        webbit.stop().get();
    }

    @Test
    public void can_write_to_file() throws IOException {
        File tmp = File.createTempFile("cucumber-jvm", "tmp");
        Writer w = new UTF8OutputStreamWriter(new URLOutputStream(tmp.toURI().toURL()));
        w.write("Hellesøy");
        w.close();
        assertEquals("Hellesøy", FixJava.readReader(new FileReader(tmp)));
    }

    @Test
    public void can_write_to_file_using_path() throws IOException {
        File tmp = File.createTempFile("cucumber-jvm", "tmp");
        Writer w = new UTF8OutputStreamWriter(new URLOutputStream(tmp.toURI().toURL()));
        w.write("Hellesøy");
        w.close();
        assertEquals("Hellesøy", FixJava.readReader(new FileReader(tmp)));
    }

    @Test
    public void can_http_put() throws IOException, ExecutionException, InterruptedException {
        final BlockingQueue<String> data = new LinkedBlockingDeque<String>();
        Rest r = new Rest(webbit);
        r.PUT("/.cucumber/stepdefs.json", new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest req, HttpResponse res, HttpControl ctl) throws Exception {
                data.offer(req.body());
                res.end();
            }
        });

        Writer w = new UTF8OutputStreamWriter(new URLOutputStream(new URL(Utils.toURL("http://localhost:9873/.cucumber"), "stepdefs.json")));
        w.write("Hellesøy");
        w.flush();
        w.close();
        assertEquals("Hellesøy", data.poll(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void throws_fnfe_if_http_response_is_404() throws IOException, ExecutionException, InterruptedException {
        Writer w = new UTF8OutputStreamWriter(new URLOutputStream(new URL(Utils.toURL("http://localhost:9873/.cucumber"), "stepdefs.json")));
        w.write("Hellesøy");
        w.flush();
        try {
            w.close();
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    @Test
    public void throws_ioe_if_http_response_is_500() throws IOException, ExecutionException, InterruptedException {
        final BlockingQueue<String> data = new LinkedBlockingDeque<String>();
        Rest r = new Rest(webbit);
        r.PUT("/.cucumber/stepdefs.json", new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest req, HttpResponse res, HttpControl ctl) throws Exception {
                data.offer(req.body());
                res.status(500);
                res.content("something went wrong");
                res.end();
            }
        });

        Writer w = new UTF8OutputStreamWriter(new URLOutputStream(new URL(Utils.toURL("http://localhost:9873/.cucumber"), "stepdefs.json")));
        w.write("Hellesøy");
        w.flush();
        try {
            w.close();
            fail();
        } catch (IOException expected) {
            assertEquals("something went wrong", expected.getMessage());
        }
    }
}
