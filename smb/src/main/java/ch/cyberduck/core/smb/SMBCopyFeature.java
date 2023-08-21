package ch.cyberduck.core.smb;

/*
 * Copyright (c) 2002-2023 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Copy;
import ch.cyberduck.core.io.StreamListener;
import ch.cyberduck.core.transfer.TransferStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.buffer.Buffer.BufferException;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.share.File;

public class SMBCopyFeature implements Copy {

    private final SMBSession session;

    public SMBCopyFeature(SMBSession session) {
        this.session = session;
    }

    @Override
    public Path copy(Path source, Path target, TransferStatus status, ConnectionCallback prompt,
                     StreamListener listener) throws BackgroundException {
        try {
            Set<AccessMask> accessMask = new HashSet<>();
            accessMask.add(AccessMask.MAXIMUM_ALLOWED);

            File sourceFile = session.share.openFile(source.getAbsolute(), accessMask,
                    Collections.singleton(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    Collections.singleton(SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN,
                    Collections.singleton(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE));

            File targetFile = session.share.openFile(target.getAbsolute(), accessMask,
                    Collections.singleton(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    Collections.singleton(SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN_IF,
                    Collections.singleton(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE));

            try {
                sourceFile.remoteCopyTo(targetFile);
            }
            catch(TransportException | BufferException e) {
                throw new BackgroundException(e);
            }
            finally {
                sourceFile.close();
                targetFile.close();
            }
            return target;
        }
        catch(SMBRuntimeException e) {
            throw new SMBExceptionMappingService().map("Cannot copy {0}", e, source);
        }
    }
}
