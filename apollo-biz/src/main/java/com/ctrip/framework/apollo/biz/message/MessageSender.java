package com.ctrip.framework.apollo.biz.message;

/**
 * Message 发送者接口
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface MessageSender {

    /**
     * 发送 Message
     *
     * @param message 消息
     * @param channel 通道（主题）
     */
    void sendMessage(String message, String channel);

}