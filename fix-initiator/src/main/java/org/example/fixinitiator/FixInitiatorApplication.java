package org.example.fixinitiator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import quickfix.*;

@SpringBootApplication
public class FixInitiatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixInitiatorApplication.class, args);
    }

    @Bean
    public Application clientApplication(MessageCracker messageCracker) {
        return new ClientApplicationAdapter(messageCracker);
    }

    @Bean
    public MessageCracker messageCracker() {
        return new ApplicationMessageCracker();
    }

    @Bean
    public LogFactory clientLogFactory(SessionSettings clientSessionSettings) {
        return new FileLogFactory(clientSessionSettings);
    }

    @Bean
    public Initiator clientInitiator(
            Application clientApplication,
            MessageStoreFactory clientMessageStoreFactory,
            SessionSettings clientSessionSettings,
            LogFactory clientLogFactory,
            MessageFactory clientMessageFactory) throws ConfigError {

        return new ThreadedSocketInitiator(clientApplication, clientMessageStoreFactory, clientSessionSettings,
                clientLogFactory, clientMessageFactory);
    }

}
