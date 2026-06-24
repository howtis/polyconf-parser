package com.polyconf.parser.parse;

import com.polyconf.parser.model.ParserResult;

import java.util.List;

@FunctionalInterface
public interface LenientParser {

    ParserResult parse(List<String> lines);
}
