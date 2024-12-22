package org.example.artioclient.spring;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.real_logic.artio.builder.TradingSessionStatusEncoder;

@RequiredArgsConstructor
@RestController
@RequestMapping("/fix")
public class FixController {

    private final SessionContext artioSessionCtx;

    @GetMapping("/send")
    public String send(@RequestParam String message) {

        final TradingSessionStatusEncoder tradingStatus = new TradingSessionStatusEncoder();
        tradingStatus.tradingSessionID("sessionId_" + artioSessionCtx.getSession().id());
        tradingStatus.tradSesStatus(artioSessionCtx.getSession().state().value());
        tradingStatus.text(message);

        artioSessionCtx.getSession().trySend(tradingStatus);

        return message;
    }

}
