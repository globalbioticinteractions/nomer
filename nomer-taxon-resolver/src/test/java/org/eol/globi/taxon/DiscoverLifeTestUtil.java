package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.eol.globi.util.HttpUtil;
import org.hamcrest.core.Is;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeTestUtil {

    public static void compareLocalVersionToRemoteVersion(String local, String remote) throws IOException {

        SSLContext build = null;
        try {
            build = new SSLContextBuilder()
                    .loadTrustMaterial(null, (TrustStrategy) (arg0, arg1) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            // ignore
        }


        HttpClientBuilder httpClientBuilder = HttpUtil.createHttpClientBuilder(300000);
        httpClientBuilder.setSSLHostnameVerifier(new NoopHostnameVerifier())
                .setSSLContext(build);

        CloseableHttpClient build1 = httpClientBuilder.build();

        String actual = HttpUtil.executeAndRelease(new HttpGet(remote), build1);

        String localCopy = IOUtils.toString(
                getStreamOfBees(local),
                StandardCharsets.UTF_8
        );

//        IOUtils.copy(
//                IOUtils.toInputStream(actual, StandardCharsets.UTF_8),
//                new FileOutputStream(new File("/tmp/bees.xml"))
//        );

        assertThat(actual, Is.is(localCopy));
    }

    public static InputStream getStreamOfBees(String beeNamesCached) throws IOException {
        return new GZIPInputStream(DiscoverLifeTestUtil.class
                .getResourceAsStream(beeNamesCached)
        );
    }

}