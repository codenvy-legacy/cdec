/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.interrupter;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class PuppetErrorInterrupter implements Interrupter {
    public static final String PUPPET_LOG              = "/var/log/messages";
    public static final int    READ_LOG_TIMEOUT_MILLIS = 100;

    private boolean interruptHappened;
    private Context context;
    private Thread  monitorThread;

    private final Interruptable interruptable;

    private List<Pattern> errorPatterns = ImmutableList.of(
        Pattern.compile("puppet-agent[^:]*: Could not retrieve catalog from remote server")
    );


    public PuppetErrorInterrupter(Interruptable interruptable) {
        this.interruptable = interruptable;
    }

    public void start() {
        interruptHappened = false;
        context = new NullContext();

        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                File puppetLog = getPuppetLog();
                for (; ; ) {
                    try {
                        List<String> lastLines = readNLines(puppetLog, 10);

                        if (lastLines == null) {
                            Thread.sleep(READ_LOG_TIMEOUT_MILLIS);
                        } else {
                            for (String line : lastLines) {
                                if (checkPuppetError(line)) {
                                    String errorMessage = format("Puppet error: '%s'", line);
                                    context = new PuppetErrorContext(errorMessage);
                                    interruptHappened = true;
                                    interruptable.interrupt(context);
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    protected File getPuppetLog() {
        return new File(PUPPET_LOG);
    }

    private boolean checkPuppetError(String line) {
        for (Pattern errorPattern : errorPatterns) {
            Matcher m = errorPattern.matcher(line);
            if (m.find()) {
                return true;
            }
        }

        return false;
    }

    /** http://stackoverflow.com/questions/686231/quickly-read-the-last-line-of-a-text-file */
    public List<String> readNLines( File file, int lineNumber) throws IOException {
        LinkedList<String> lines = new LinkedList<>();

        try (RandomAccessFile fileHandler = new RandomAccessFile(file, "r")) {
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for (long filePointer = fileLength; filePointer != -1; filePointer--) {
                fileHandler.seek(filePointer);
                int readByte = fileHandler.readByte();

                switch (readByte) {
                    case 0xA : {
                        if (filePointer < fileLength) {
                            line = line + 1;
                            lines.offerFirst(sb.reverse().toString());
                            sb = new StringBuilder();
                        }
                        break;
                    }

                    case 0xD : {
                        if (filePointer < fileLength - 1) {
                            line = line + 1;
                            lines.offerFirst(sb.reverse().toString());
                            sb = new StringBuilder();
                        }
                        break;
                    }

                    default:
                        sb.append((char) readByte);
                        break;
                }

                if (line >= lineNumber) {
                    break;
                }
            }
        }

        return lines;
    }

    public void stop() {
        monitorThread.interrupt();
        try {
            monitorThread.join(READ_LOG_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasInterrupted() {
        return interruptHappened;
    }

    @Override
    public Context getContext() {
        return context;
    }

    static class PuppetErrorContext implements Context {
        private final String message;

        PuppetErrorContext(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PuppetErrorContext that = (PuppetErrorContext)o;

            if (message != null ? !message.equals(that.message) : that.message != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return message != null ? message.hashCode() : 0;
        }
    }
}
