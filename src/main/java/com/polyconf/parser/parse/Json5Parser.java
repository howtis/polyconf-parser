package com.polyconf.parser.parse;

import com.polyconf.parser.model.ParserResult;

import java.util.List;

public final class Json5Parser implements LenientParser {

    private final JsonParser delegate = new JsonParser();

    @Override
    public ParserResult parse(List<String> lines) {
        return delegate.parse(lines);
    }
}
