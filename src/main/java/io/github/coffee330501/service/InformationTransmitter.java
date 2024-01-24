package io.github.coffee330501.service;

import java.util.List;
import java.util.Map;

public interface InformationTransmitter {
    /**
     * 声明信息 kv map
     */
    List<String> declareInformationMapKeys();

    Map<String,String> createInformation();
}
