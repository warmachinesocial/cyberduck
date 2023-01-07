package ch.cyberduck.core.openstack;

/*
 * Copyright (c) 2002-2019 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.cryptomator.CryptoVault;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;

import static org.junit.Assert.fail;

public abstract class AbstractSwiftTest {

    protected SwiftSession session;

    @Parameterized.Parameters(name = "vaultVersion = {0}")
    public static Object[] data() {
        return new Object[]{CryptoVault.VAULT_VERSION_DEPRECATED, CryptoVault.VAULT_VERSION};
    }

    @Parameterized.Parameter
    public int vaultVersion;

    @After
    public void disconnect() throws Exception {
        session.close();
    }

    @Before
    public void setup() throws Exception {
        session = new SwiftSession(new Host(new SwiftProtocol(), "identity.api.rackspacecloud.com", new Credentials(
                System.getProperties().getProperty("rackspace.user"), System.getProperties().getProperty("rackspace.password")
        )), new DisabledX509TrustManager(), new DefaultX509KeyManager());
        final LoginConnectionService login = new LoginConnectionService(new DisabledLoginCallback() {
            @Override
            public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) {
                fail(reason);
                return null;
            }
        }, new DisabledHostKeyCallback(),
            new DisabledPasswordStore(), new DisabledProgressListener());
        login.check(session, new DisabledCancelCallback());
    }
}
