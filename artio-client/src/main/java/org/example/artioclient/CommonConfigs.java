package org.example.artioclient;

public abstract class CommonConfigs {
    public static final String AERON_DIR_NAME = "artio-client/target/client-aeron";
    public static final String ARCHIVE_DIR_NAME = "artio-client/target/client-aeron-archive";
    public static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:7010";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:7020";
    public static final String RECORDING_EVENTS_CHANNEL = "aeron:udp?control-mode=dynamic|control=localhost:7030";
    public static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";
}
