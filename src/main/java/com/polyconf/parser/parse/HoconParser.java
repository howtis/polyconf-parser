package com.polyconf.parser.parse;

import com.polyconf.parser.model.ParserResult;

import java.util.List;

public final class HoconParser implements LenientParser {

    private final PropertiesParser delegate = new PropertiesParser();

    @Override
    public ParserResult parse(List<String> lines) {
        return delegate.parse(lines);
    }
}
