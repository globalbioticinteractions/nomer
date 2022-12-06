package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class GBIFNameToIdsIteratorTest {

    @Test
    public void parse() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        nameIdsStream(),
                        StandardCharsets.UTF_8
                )
        );

        Iterator<Fun.Tuple2<String, List<Long>>> iter
                = new GBIFNameToIdsIterator(bufferedReader);

        StringBuilder buffer = new StringBuilder();
        while (iter.hasNext()) {
            Fun.Tuple2<String, List<Long>> value = iter.next();
            buffer.append(value.a);
            for (Long aLong : value.b) {
                buffer.append("\t");
                buffer.append(aLong);
            }
            buffer.append("\n");
        }

        assertThat(buffer.toString(), Is.is(IOUtils.toString(nameIdsStream(), StandardCharsets.UTF_8)));


    }

    private InputStream nameIdsStream() {
        return getClass().getResourceAsStream("gbif/backbone-current-name-id-sorted.txt");
    }

}