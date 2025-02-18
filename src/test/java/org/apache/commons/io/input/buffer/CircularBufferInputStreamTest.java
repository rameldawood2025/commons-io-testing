/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.input.buffer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CircularBufferInputStreamTest {
	private final Random rnd = new Random(1530960934483l); // Fixed seed for reproducibility

	/**
	 * Existing test: Performs random reads from a circular buffer.
	 */
	@Test
	public void testRandomRead() throws Exception {
		final byte[] inputBuffer = newInputBuffer();
		// Make a copy to compare against (though it's unused in this code,
		// it illustrates intent)
		final byte[] bufferCopy = new byte[inputBuffer.length];
		final ByteArrayInputStream bais = new ByteArrayInputStream(inputBuffer);
		@SuppressWarnings("resource")
		final CircularBufferInputStream cbis = new CircularBufferInputStream(bais, 253);
		int offset = 0;
		final byte[] readBuffer = new byte[256];
		while (offset < inputBuffer.length) {
			switch (rnd.nextInt(2)) {
				case 0:
					final int res = cbis.read();
					if (res == -1) {
						throw new IllegalStateException("Unexpected EOF at offset " + offset);
					}
					if (inputBuffer[offset] != res) {
						throw new IllegalStateException("Expected " + inputBuffer[offset] + " at offset " + offset + ", got " + res);
					}
					++offset;
					break;
				case 1:
					final int resBytes = cbis.read(readBuffer, 0, rnd.nextInt(readBuffer.length + 1));
					if (resBytes == -1) {
						throw new IllegalStateException("Unexpected EOF at offset " + offset);
					} else if (resBytes == 0) {
						throw new IllegalStateException("Unexpected zero-byte-result at offset " + offset);
					} else {
						for (int i = 0; i < resBytes; i++) {
							if (inputBuffer[offset] != readBuffer[i]) {
								throw new IllegalStateException("Expected " + inputBuffer[offset] + " at offset " + offset + ", got " + readBuffer[i]);
							}
							++offset;
						}
					}
					break;
				default:
					throw new IllegalStateException("Unexpected random choice value");
			}
		}
	}

	/**
	 * Creates a large, but random input buffer.
	 */
	private byte[] newInputBuffer() {
		final byte[] buffer = new byte[16 * 512 + rnd.nextInt(512)];
		rnd.nextBytes(buffer);
		return buffer;
	}

	/**
	 * Test that the constructor throws an exception when bufferSize is zero.
	 * This helps kill mutants that may have changed the boundary condition.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorZeroBufferSize() {
		new CircularBufferInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}), 0);
	}

	/**
	 * Test that the constructor throws an exception when bufferSize is negative.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorNegativeBufferSize() {
		new CircularBufferInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}), -10);
	}

	/**
	 * Test reading from an empty stream to ensure proper handling of EOF.
	 */
	@Test
	public void testReadEmptyStream() throws IOException {
		ByteArrayInputStream emptyStream = new ByteArrayInputStream(new byte[0]);
		CircularBufferInputStream cbis = new CircularBufferInputStream(emptyStream, 10);

		// read() should return -1 immediately on an empty stream.
		int result = cbis.read();
		assertEquals("Expected -1 when reading from an empty stream", -1, result);

		// Similarly, read(byte[], int, int) should also return -1.
		byte[] buf = new byte[5];
		result = cbis.read(buf, 0, buf.length);
		assertEquals("Expected -1 when reading from an empty stream", -1, result);
	}

	/**
	 * Test reading all bytes from a non-empty stream until EOF is reached.
	 */
	@Test
	public void testReadAllBytes() throws IOException {
		// Create a predictable input: 100 sequential bytes.
		byte[] data = new byte[100];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CircularBufferInputStream cbis = new CircularBufferInputStream(bais, 20);

		int totalRead = 0;
		byte[] buffer = new byte[30];
		int readBytes;
		while ((readBytes = cbis.read(buffer, 0, buffer.length)) != -1) {
			for (int i = 0; i < readBytes; i++) {
				assertEquals("Byte at position " + totalRead + " doesn't match", data[totalRead], buffer[i]);
				totalRead++;
			}
		}
		assertEquals("Total bytes read should equal data length", data.length, totalRead);

		// One more read() should return -1.
		int finalRead = cbis.read();
		assertEquals("After reading everything, read() should return -1", -1, finalRead);
	}
}
