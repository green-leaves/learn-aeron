package org.example.fixinitiator;

import io.allune.quickfixj.spring.boot.starter.template.QuickFixJTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import quickfix.Initiator;
import quickfix.SessionID;
import quickfix.field.TestReqID;
import quickfix.fix44.Heartbeat;

import java.util.UUID;

@RestController
@RequestMapping("/client")
public class FixClientController {

    private final QuickFixJTemplate quickFixJTemplate;
    private final Initiator initiator;

    public FixClientController(QuickFixJTemplate quickFixJTemplate,
                               Initiator initiator) {
        this.quickFixJTemplate = quickFixJTemplate;
        this.initiator = initiator;
    }

    @GetMapping("/send")
    public String send(@RequestParam String message) {
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setField(new TestReqID(message));

        SessionID sessionID = initiator.getSessions().stream()
                .filter(id -> id.getBeginString().equals("FIX.4.4"))
                .findFirst()
                .orElseThrow(RuntimeException::new);
        quickFixJTemplate.send(heartbeat, sessionID);
        return message;
    }
}
