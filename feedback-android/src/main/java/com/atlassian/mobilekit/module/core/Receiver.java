package com.atlassian.mobilekit.module.core;

public interface Receiver<T> {

    void receive(T data);
}
