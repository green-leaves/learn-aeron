package org.example.artioclient.spring;

/*
 * Copyright 2015-2024 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.aeron.logbuffer.ControlledFragmentHandler.Action;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.springframework.stereotype.Component;
import uk.co.real_logic.artio.ValidationError;
import uk.co.real_logic.artio.builder.Printer;
import uk.co.real_logic.artio.decoder.PrinterImpl;
import uk.co.real_logic.artio.dictionary.LongDictionary;
import uk.co.real_logic.artio.fields.AsciiFieldFlyweight;
import uk.co.real_logic.artio.library.OnMessageInfo;
import uk.co.real_logic.artio.library.SessionHandler;
import uk.co.real_logic.artio.messages.DisconnectReason;
import uk.co.real_logic.artio.otf.MessageControl;
import uk.co.real_logic.artio.otf.OtfMessageAcceptor;
import uk.co.real_logic.artio.otf.OtfParser;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.util.AsciiBuffer;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;

@Component
@Slf4j
public class SampleSessionHandler implements SessionHandler, OtfMessageAcceptor {


    private final OtfParser parser = new OtfParser(this, new LongDictionary());
    private final AsciiBuffer string = new MutableAsciiBuffer();
    private final Printer printer = new PrinterImpl();

    public Action onMessage(
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final int libraryId,
            final Session session,
            final int sequenceIndex,
            final long messageType,
            final long timestampInNs,
            final long position,
            final OnMessageInfo messageInfo) {
        parser.onMessage(buffer, offset, length);
        string.wrap(buffer);
        log.info("onMessage sessionId={}, {}", session.id(), printer.toString(string, offset, length, messageType));

        return CONTINUE;
    }

    public void onTimeout(final int libraryId, final Session session) {
    }

    public void onSlowStatus(final int libraryId, final Session session, final boolean hasBecomeSlow) {
    }

    public Action onDisconnect(final int libraryId, final Session session, final DisconnectReason reason) {
        log.info("onDisconnect {}, {}", session.id(), reason);
        return CONTINUE;
    }

    public void onSessionStart(final Session session) {
    }

    public MessageControl onNext() {
        return MessageControl.CONTINUE;
    }

    public MessageControl onComplete() {
        return MessageControl.CONTINUE;
    }

    public MessageControl onField(final int tag, final AsciiBuffer buffer, final int offset, final int length) {
        String fieldMsg = buffer.getAscii(offset, length);
        log.info("onField: tag={}, value={}", tag, fieldMsg);
        return MessageControl.CONTINUE;
    }

    public MessageControl onGroupHeader(final int tag, final int numInGroup) {
        return MessageControl.CONTINUE;
    }

    public MessageControl onGroupBegin(final int tag, final int numInGroup, final int index) {
        return MessageControl.CONTINUE;
    }

    public MessageControl onGroupEnd(final int tag, final int numInGroup, final int index) {
        return MessageControl.CONTINUE;
    }

    public boolean onError(
            final ValidationError error,
            final long messageType,
            final int tagNumber,
            final AsciiFieldFlyweight value) {
        return false;
    }

}