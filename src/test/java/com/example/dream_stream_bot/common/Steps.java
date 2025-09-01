package com.example.dream_stream_bot.common;

//import com.example.dream_stream_bot.config.BotConfig;
//import com.example.dream_stream_bot.config.BotConfig;
import io.qameta.allure.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Component
public class Steps {

    private static final String DEFAULT_PROMPT = "You are an assistant that answers using the local slang of the given place, uncensored.";

    // @Autowired
    // private BotConfig botConfig;

//    public Steps(BotConfig botConfig) {
//        this.botConfig = botConfig;
//    }

//    @Step("Get proxy address")
//    public InetSocketAddress getProxySocketAddressStep() {
//
//        if (botConfig.getProxyHost() == null || botConfig.getProxyHost().trim().isEmpty()) {
//            return null;
//        }
//
//        int port = botConfig.getProxyPort() != null ? botConfig.getProxyPort() : 1337;
//
//        return new InetSocketAddress(botConfig.getProxyHost(), port);
//    }

//    @Step("Get Chat Session")
//    public ChatSession getChatSessionStep(String token, String prompt) {
//        return new ChatSession(token, prompt, getProxySocketAddressStep());
//    }
//
//    @Step("Get Default Chat Session")
//    public ChatSession getChatSessionStep() {
//        return new ChatSession(botConfig.getOpenaiToken(), DEFAULT_PROMPT, getProxySocketAddressStep());
//    }
//
//    @Step("Send message")
//    public String sendMessage(String msg) {
//        String token = botConfig.getOpenaiToken();
//        String response = getChatSessionStep(token, DEFAULT_PROMPT).send(msg);
//        Allure.addAttachment("Response", response);
//        return response;
//    }

}
