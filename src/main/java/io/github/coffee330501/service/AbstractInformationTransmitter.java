package io.github.coffee330501.service;

import io.github.coffee330501.utils.InternalCallContext;

import java.util.Map;

public abstract class AbstractInformationTransmitter implements InformationTransmitter {
    /**
     * 设置信息
     */
    public void setInformation(Map<String, String> map) {
        InternalCallContext.set(map);
    }

    /**
     * 获取信息
     */
    public Map<String, String> getInformation() {
        return InternalCallContext.get();
    }
}
