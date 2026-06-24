package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigSection;

import java.util.List;

@FunctionalInterface
public interface LenientParser {

    ConfigSection parse(List<String> lines);
}
