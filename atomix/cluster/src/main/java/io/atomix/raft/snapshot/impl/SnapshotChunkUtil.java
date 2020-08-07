/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.atomix.raft.snapshot.impl;

import io.atomix.raft.protocol.InstallRequest;
import io.atomix.raft.snapshot.SnapshotChunk;
import io.atomix.utils.time.WallClockTimestamp;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.CRC32;

public final class SnapshotChunkUtil {

  private SnapshotChunkUtil() {}

  static long createChecksum(final byte[] content) {
    final CRC32 crc32 = new CRC32();
    crc32.update(content);
    return crc32.getValue();
  }

  /**
   * Returns a new snapshot chunk from a pre 0.24.x install request
   *
   * @param request
   * @return
   */
  public static SnapshotChunk fromOldInstallRequest(final InstallRequest request) {
    final var metadata =
        new FileBasedSnapshotMetadata(
            request.index(), request.currentTerm(), WallClockTimestamp.from(request.timestamp()));
    final var chunkName = new byte[request.chunkId().remaining()];
    final var content = new byte[request.data().remaining()];
    request.data().asReadOnlyBuffer().get(content);
    request.chunkId().asReadOnlyBuffer().get(chunkName);

    return new SnapshotChunkImpl(
        metadata.getSnapshotIdAsString(),
        Integer.MIN_VALUE,
        new String(chunkName),
        createChecksum(content),
        content,
        Long.MIN_VALUE);
  }

  static SnapshotChunk createSnapshotChunkFromFile(
      final File snapshotChunkFile,
      final String snapshotId,
      final int totalCount,
      final long snapshotChecksum)
      throws IOException {
    final byte[] content;
    content = Files.readAllBytes(snapshotChunkFile.toPath());
    final long checksum = createChecksum(content);
    return new SnapshotChunkImpl(
        snapshotId, totalCount, snapshotChunkFile.getName(), checksum, content, snapshotChecksum);
  }

  private static final class SnapshotChunkImpl implements SnapshotChunk {
    private final String snapshotId;
    private final int totalCount;
    private final String chunkName;
    private final byte[] content;
    private final long snapshotChecksum;
    private final long checksum;

    SnapshotChunkImpl(
        final String snapshotId,
        final int totalCount,
        final String chunkName,
        final long checksum,
        final byte[] content,
        final long snapshotChecksum) {
      this.snapshotId = snapshotId;
      this.totalCount = totalCount;
      this.chunkName = chunkName;
      this.checksum = checksum;
      this.content = content;
      this.snapshotChecksum = snapshotChecksum;
    }

    @Override
    public String getSnapshotId() {
      return snapshotId;
    }

    @Override
    public int getTotalCount() {
      return totalCount;
    }

    @Override
    public String getChunkName() {
      return chunkName;
    }

    @Override
    public long getChecksum() {
      return checksum;
    }

    @Override
    public byte[] getContent() {
      return content;
    }

    @Override
    public long getSnapshotChecksum() {
      return snapshotChecksum;
    }

    @Override
    public String toString() {
      return "SnapshotChunkImpl{"
          + "snapshotId='"
          + snapshotId
          + '\''
          + ", totalCount="
          + totalCount
          + ", chunkName='"
          + chunkName
          + '\''
          + ", snapshotChecksum="
          + snapshotChecksum
          + ", checksum="
          + checksum
          + '}';
    }
  }
}
