package ch.cyberduck.ui.action;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginController;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Delete;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id$
 */
public abstract class DeleteWorker extends Worker<Boolean> {

    private Session<?> session;

    /**
     * Selected files.
     */
    private List<Path> files;

    private LoginController prompt;

    public DeleteWorker(final Session session, final LoginController prompt, final List<Path> files) {
        this.session = session;
        this.prompt = prompt;
        this.files = files;
    }

    @Override
    public Boolean run() throws BackgroundException {
        final List<Path> recursive = new ArrayList<Path>();
        for(Path file : files) {
            recursive.addAll(this.compile(file));
        }
        final Delete feature = session.getFeature(Delete.class);
        feature.delete(recursive, prompt);
        return true;
    }

    protected List<Path> compile(final Path file) throws BackgroundException {
        // Compile recursive list
        final List<Path> recursive = new ArrayList<Path>();
        if(file.attributes().isFile() || file.attributes().isSymbolicLink()) {
            recursive.add(file);
        }
        else if(file.attributes().isDirectory()) {
            for(Path child : session.list(file, new DisabledListProgressListener())) {
                recursive.addAll(this.compile(child));
            }
            // Add parent after children
            recursive.add(file);
        }
        return recursive;
    }

    @Override
    public String getActivity() {
        return MessageFormat.format(LocaleFactory.localizedString("Deleting {0}", "Status"),
                this.toString(files));
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final DeleteWorker that = (DeleteWorker) o;
        if(files != null ? !files.equals(that.files) : that.files != null) {
            return false;
        }
        if(session != null ? !session.equals(that.session) : that.session != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = session != null ? session.hashCode() : 0;
        result = 31 * result + (files != null ? files.hashCode() : 0);
        return result;
    }
}
