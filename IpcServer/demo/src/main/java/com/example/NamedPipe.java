package com.example;

import java.io.IOException;

/**
 * Bridge to named pipe functions for different platforms.
 */
abstract class NamedPipe {

    /**
     * The full pipe name (example: /tmp/foo.pipe on Linux)
     */
    private final String pipeName;

    protected NamedPipe(String name) {
        pipeName = name;
    }

    public abstract void connect() throws IOException;

    public abstract int read(byte[] bytes, int offset, int length) throws IOException;

    public abstract int write(byte[] bytes, int offset, int length) throws IOException;

    public abstract void close() throws IOException;

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    protected String getPipeName() {
        return pipeName;
    }
}