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
import ch.cyberduck.core.features.Delete.Callback;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.transfer.TransferStatus;

import java.util.Collections;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.share.DiskEntry;

public class SMBMoveFeature implements Move {

    private final SMBSession session;

    public SMBMoveFeature(final SMBSession session) {
        this.session = session;
    }

    @Override
    public boolean isRecursive(final Path source, final Path target) {
        return true;
    }

    @Override
    public Path move(final Path source, final Path target, final TransferStatus status, final Callback delete, final ConnectionCallback prompt) throws BackgroundException {
        try (DiskEntry file = session.share.open(source.getAbsolute(),
                Collections.singleton(AccessMask.MAXIMUM_ALLOWED),
                Collections.singleton(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                Collections.singleton(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                Collections.singleton(source.isDirectory() ? SMB2CreateOptions.FILE_DIRECTORY_FILE : SMB2CreateOptions.FILE_NON_DIRECTORY_FILE))) {
            file.rename(new SmbPath(session.share.getSmbPath(), target.getAbsolute()).getPath(), status.isExists());
        }
        catch(SMBRuntimeException e) {
            throw new SMBExceptionMappingService().map("Cannot rename {0}", e, source);
        }
        return target;
    }
}
