package org.example.artioclient.plain;

import io.aeron.logbuffer.ControlledFragmentHandler.Action;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.builder.Printer;
import uk.co.real_logic.artio.decoder.PrinterImpl;
import uk.co.real_logic.artio.dictionary.LongDictionary;
import uk.co.real_logic.artio.library.*;
import uk.co.real_logic.artio.messages.DisconnectReason;
import uk.co.real_logic.artio.otf.OtfParser;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.util.AsciiBuffer;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;

@Slf4j
public class PlainSessionHandler implements SessionHandler, SessionAcquireHandler, LibraryConnectHandler {

    private final AsciiBuffer string = new MutableAsciiBuffer();
    private final Printer printer = new PrinterImpl();

    @Setter
    @Getter
    private LibraryConfiguration libraryConfiguration;


    @Override
    public void onConnect(FixLibrary library) {
        log.info("onFixLibraryConnect {}", library);
    }

    @Override
    public void onDisconnect(FixLibrary library) {
        log.info("onFixLibraryDisconnect");
        library.close();
        FixLibrary.connect(libraryConfiguration);
    }

    @Override
    public SessionHandler onSessionAcquired(Session session, SessionAcquiredInfo acquiredInfo) {
        return this;
    }

    @Override
    public Action onMessage(DirectBuffer buffer,
                            int offset,
                            int length,
                            int libraryId,
                            Session session,
                            int sequenceIndex,
                            long messageType,
                            long timestampInNs,
                            long position, OnMessageInfo messageInfo) {
        string.wrap(buffer);
        log.info("onMessage sessionId={}, {}", session.id(), printer.toString(string, offset, length, messageType));
        return CONTINUE;
    }

    @Override
    public void onTimeout(int libraryId, Session session) {

    }

    @Override
    public void onSlowStatus(int libraryId, Session session, boolean hasBecomeSlow) {

    }

    @Override
    public Action onDisconnect(int libraryId, Session session, DisconnectReason reason) {
        return null;
    }

    @Override
    public void onSessionStart(Session session) {

    }
}
