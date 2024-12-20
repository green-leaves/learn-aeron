package org.example.artioclient;

import lombok.extern.slf4j.Slf4j;
import uk.co.real_logic.artio.dictionary.generation.CodecConfiguration;
import uk.co.real_logic.artio.dictionary.generation.CodecGenerator;

@Slf4j
public class CustomCodecGenerationTool {
    public static void main(final String[] args) {
        System.out.println("Starting Custom Codec Generation Tool");
        final String files = "artio-client/src/main/resources/session_dictionary.xml";
        final String[] fileNames = files.split(";");

        try {
            final CodecConfiguration config = new CodecConfiguration()
                    .outputPath("artio-client/target/generated-sources")
                    .fileNames(fileNames);
            CodecGenerator.generate(config);
        } catch (final Throwable e) {
            log.error(e.getMessage(), e);
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: CodecGenerationTool </path/to/output-directory> " +
                "<[/path/to/fixt-xml/dictionary;]/path/to/xml/dictionary>");
        System.exit(-1);
    }
}
