package org.globalbioticinteractions.nomer.util;

import org.eol.globi.service.PropertyEnricherException;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class HeaderRowHandlerTest {

    @Test
    public void appendHeader() throws PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HeaderRowHandler handler = new HeaderRowHandler(
                os,
                new TreeMap<Integer, String>() {{
                    put(0, "id");
                    put(1, "name");
                }},
                new TreeMap<Integer, String>() {{
                    put(0, "id");
                    put(1, "name");
                }});

        handler
                .onRow(new String[]{"name", "id"});


        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                Is.is("providedId" +
                        "\tprovidedName" +
                        "\trelationName" +
                        "\tresolvedId" +
                        "\tresolvedName" +
                        "\n"));
    }
    @Test
    public void appendHeaderJustOnce() throws PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HeaderRowHandler handler = new HeaderRowHandler(
                os,
                new TreeMap<Integer, String>() {{
                    put(0, "id");
                    put(1, "name");
                }},
                new TreeMap<Integer, String>() {{
                    put(0, "id");
                    put(1, "name");
                }});

        handler
                .onRow(new String[]{"name", "id"});
        handler
                .onRow(new String[]{"name", "id"});


        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                Is.is("providedId" +
                        "\tprovidedName" +
                        "\trelationName" +
                        "\tresolvedId" +
                        "\tresolvedName" +
                        "\n"));
    }

    @Test
    public void appendDefaultHeader() throws PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new HeaderRowHandler(
                os,
                new TreeMap<Integer, String>() {{
                    put(0, "id");
                    put(1, "name");
                }},
                MatchTestUtil.appenderSchemaDefault())
                .onRow(new String[]{"name", "id"});


        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                Is.is("providedId" +
                        "\tprovidedName" +
                        "\trelationName" +
                        "\tresolvedExternalId" +
                        "\tresolvedName" +
                        "\tresolvedRank" +
                        "\tresolvedCommonNames" +
                        "\tresolvedPath" +
                        "\tresolvedPathIds" +
                        "\tresolvedPathNames" +
                        "\tresolvedExternalUrl" +
                        "\tresolvedThumbnailUrl" +
                        "\n"));
    }


}