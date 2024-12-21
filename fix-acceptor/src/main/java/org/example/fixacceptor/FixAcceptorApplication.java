package org.example.fixacceptor;

import io.allune.quickfixj.spring.boot.starter.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import quickfix.Message;
import quickfix.SessionID;

@Slf4j
@SpringBootApplication
public class FixAcceptorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixAcceptorApplication.class, args);
    }

//    @EventListener
//    public void handleFromAdmin1(FromAdmin fromAdmin) {
//        log.info("fromAdmin: Message={}, SessionId={}", fromAdmin.getMessage(), fromAdmin.getSessionId());
//    }
//
    @EventListener
    public void handleFromAdmin(FromAdmin fromAdmin) {
        log.info("fromAdmin: Message={}, SessionId={}", fromAdmin.getMessage(), fromAdmin.getSessionId());
    }

    @EventListener
    public void handleFromApp(FromApp fromApp) {
        log.info("fromApp: Message={}, SessionId={}", fromApp.getMessage(), fromApp.getSessionId());
    }

    @EventListener
    public void handleCreate(Create create) {
        log.info("onCreate: SessionId={}", create.getSessionId());
    }

    @EventListener
    public void handleLogon(Logon logon) {
        log.info("onLogon: SessionId={}", logon.getSessionId());
    }

    @EventListener
    public void handleLogout(Logout logout) {
        log.info("onLogout: SessionId={}", logout.getSessionId());
    }

    @EventListener
    public void handleToAdmin(ToAdmin toAdmin) {
        log.info("toAdmin: SessionId={}", toAdmin.getSessionId());
    }

    @EventListener
    public void handleToApp(ToApp toApp) {
        log.info("toApp: SessionId={}", toApp.getSessionId());
    }

}
