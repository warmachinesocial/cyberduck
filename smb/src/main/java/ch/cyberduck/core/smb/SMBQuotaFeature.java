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

import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Quota;

public class SMBQuotaFeature implements Quota {

    private final SMBSession session;

    public SMBQuotaFeature(final SMBSession session) {
        this.session = session;
    }

    @Override
    public Space get() throws BackgroundException {
        long used = 0L;
        long available = 0L;
        for(Path container : session.getFeature(ListService.class).list(Home.ROOT, new DisabledListProgressListener())) {
            final PathAttributes attr = new SMBAttributesFinderFeature(session).find(container);
            used += attr.getSize();
            available += attr.getQuota() - attr.getSize();
        }
        return new Quota.Space(used, available);
    }
}
