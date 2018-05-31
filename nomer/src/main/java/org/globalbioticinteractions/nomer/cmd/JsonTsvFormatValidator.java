package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JsonTsvFormatValidator implements IParameterValidator {

    private final List<String> list = Arrays.asList("json", "tsv");

    @Override
    public void validate(String name, String value) throws ParameterException {
        Optional<String> first = list.stream().filter(supported -> StringUtils.equalsIgnoreCase(supported, value)).findFirst();
        if (!first.isPresent()) {
            throw new ParameterException("[" + value + "] not supported. Please only use [" + StringUtils.join(list, ',') + "]");
        }
    }
}
