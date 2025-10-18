package com.exploresg.fleetservice.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {
    @Autowired
    private StreamBridge bridge;
    public void sendMessage(String userEmail) {
        Email email = new Email();
        email.setAddress(userEmail);
        email.setContent("Your booking has been confirmed. Please refer to https://xplore.town/ for more details on your booking.");
        this.bridge.send("sendNotification-out-0", MessageBuilder.withPayload(email)
                .build());
    }
    public static class Email {
        private String address;
        private String content;
        public String getAddress() {
            return address;
        }
        public void setAddress(String address) {
            this.address = address;
        }
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Email [address=").append(address).append(", content=").append(content).append("]");
            return builder.toString();
        }
    }


}
