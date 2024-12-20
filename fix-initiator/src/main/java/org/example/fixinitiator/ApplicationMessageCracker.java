package org.example.fixinitiator;

import lombok.extern.slf4j.Slf4j;
import quickfix.*;

@Slf4j
public class ApplicationMessageCracker extends MessageCracker {

    @Override
    protected void onMessage(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        //log.info("ApplicationMessageCracker {}", message.toString());
        super.onMessage(message, sessionID);
    }
}
