package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.eol.globi.util.HttpUtil.getHttpClient;

public class HttpTimedUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HttpTimedUtil.class);

    public static String executeWithTimer(HttpRequestBase request, ResponseHandler<String> handler) throws IOException {
        try {
            HttpClient httpClient = getHttpClient();
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            String response = httpClient.execute(request, handler);
            stopwatch.stop();
            if (LOG.isDebugEnabled() && stopwatch.getTime() > 3000) {
                String responseTime = "slowish http request (took " + stopwatch.getTime() + "ms) for [" + request.getURI().toString() + "]";
                LOG.debug(responseTime);
            }

            return response;
        } finally {
            request.releaseConnection();
        }
    }

    public static ResponseHandler<String> createUTF8BasicResponseHandler() {
        return new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse response) throws IOException {
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                if(statusLine.getStatusCode() >= 300) {
                    EntityUtils.consume(entity);
                    throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                } else {
                    return entity == null?null:EntityUtils.toString(entity, StandardCharsets.UTF_8);
                }
            }
        };
    }

}
