/*
 * This file is part of Burningwave Core.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/core
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.core.io;


import static org.burningwave.core.assembler.StaticComponentContainer.ByteBufferDelegate;
import static org.burningwave.core.assembler.StaticComponentContainer.Streams;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class ByteBufferOutputStream extends OutputStream {

    private static final float REALLOCATION_FACTOR = 1.1f;

    private Integer initialCapacity;
    private Integer initialPosition;
    private ByteBuffer buffer;
    private Boolean closeable;
    
    public ByteBufferOutputStream() {
    	this(Streams.defaultBufferSize);
    }
    
    public ByteBufferOutputStream(boolean closeable) {
    	this(Streams.defaultBufferSize, closeable);
    }

    public ByteBufferOutputStream(ByteBuffer buffer, boolean closeable) {
        this.buffer = buffer;
        this.initialPosition = ByteBufferDelegate.position(buffer);
        this.initialCapacity = ByteBufferDelegate.capacity(buffer);
        this.closeable = closeable;
    }
    
    public ByteBufferOutputStream(int initialCapacity) {
        this(initialCapacity, true);
    }

    public ByteBufferOutputStream(int initialCapacity, boolean closeable) {
        this(Streams.defaultByteBufferAllocationMode.apply(initialCapacity), closeable);
    }
    
    public void markAsCloseable(boolean closeable) {
    	this.closeable = closeable;
    }
    
    public void write(int b) {
        ensureRemaining(1);
        buffer.put((byte) b);
    }

    public void write(byte[] bytes, int off, int len) {
        ensureRemaining(len);
        buffer.put(bytes, off, len);
    }

    public void write(ByteBuffer sourceBuffer) {
        ensureRemaining(sourceBuffer.remaining());
        buffer.put(sourceBuffer);
    }

    public int position() {
        return ByteBufferDelegate.position(buffer);
    }

    public int remaining() {
        return ByteBufferDelegate.remaining(buffer);
    }

    public int limit() {
        return ByteBufferDelegate.limit(buffer);
    }

    public void position(int position) {
        ensureRemaining(position - ByteBufferDelegate.position(buffer));
        ByteBufferDelegate.position(buffer, position);
    }

    public int initialCapacity() {
        return initialCapacity;
    }

    public void ensureRemaining(int remainingBytesRequired) {
        if (remainingBytesRequired > buffer.remaining())
            expandBuffer(remainingBytesRequired);
    }

    private void expandBuffer(int remainingRequired) {
        int expandSize = Math.max((int) (ByteBufferDelegate.limit(buffer) * REALLOCATION_FACTOR), ByteBufferDelegate.position(buffer) + remainingRequired);
        ByteBuffer temp = Streams.defaultByteBufferAllocationMode.apply(expandSize);
        int limit = limit();
        ByteBufferDelegate.flip(buffer);
        temp.put(buffer);
        ByteBufferDelegate.limit(buffer, limit);
        ByteBufferDelegate.position(buffer, initialPosition);
        buffer = temp;
    }
    
    
    InputStream toBufferedInputStream() {
        return new ByteBufferInputStream(buffer);
    }
    
    @Override
    public void close() {
    	if (closeable) {
    		this.initialCapacity = null;
    		this.initialPosition = null;
    		this.buffer = null;
    		this.closeable = null;
    	}
    }

	public ByteBuffer toByteBuffer() {
		return Streams.shareContent(buffer);
	}

	public byte[] toByteArray() {
		return Streams.toByteArray(toByteBuffer());
	}
}