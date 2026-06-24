package com.polyconf.parser.classify;

import java.util.List;

public abstract class FormatDetector {
    public abstract int score(List<Token> tokens);
}
