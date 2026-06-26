package com.polyconf.parser.parse;

import com.polyconf.parser.model.ParserResult;

import java.util.List;

@FunctionalInterface
public interface LenientParser {

    ParserResult parse(List<String> lines);

    default boolean isPlausible(List<String> lines) {
        return true;
    }
}
