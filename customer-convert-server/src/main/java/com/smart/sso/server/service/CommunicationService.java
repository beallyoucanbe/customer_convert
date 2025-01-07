package com.smart.sso.server.service;

public interface CommunicationService {

    void wecomCallBack(String filePath);

    void telephoneCallBack(String filePath);
}
