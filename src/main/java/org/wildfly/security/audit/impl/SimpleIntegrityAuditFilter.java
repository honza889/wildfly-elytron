/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.security.audit.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.wildfly.security.audit.AuditProvider;
import org.wildfly.security.util.ByteIterator;

/**
 *  Integrity audit filter. To the end of every message appends hash
 *  of current message concatenated with hash from previous message.
 *
 *  Should allow log tampering detection (removing same row of log for example).
 *  Currently not prevents removing events between two server restart.
 *  Could bring real security if hash would be replaced by encrypted hash.
 *
 *  @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 *  @version $Revision$
 *  @since  Jul 13, 2015
 */
public class SimpleIntegrityAuditFilter implements AuditProvider {

    private final AuditProvider delegating;
    private final MessageDigest digest;
    private byte[] lastDigest = null;

    public SimpleIntegrityAuditFilter(MessageDigest digest, AuditProvider delegatingProvider) throws NoSuchAlgorithmException {
        this.delegating = delegatingProvider;
        this.digest = digest;
    }

    public void audit(String message, Exception exception) {
        lastDigest = digest(message, lastDigest);
        StringBuilder b = new StringBuilder();
        b.append(message);
        b.append(ByteIterator.ofBytes(lastDigest).base64Encode().drainToString());
        delegating.audit(b.toString(), exception);
    }

    byte[] digest(String message, byte[] previousDigest) {
        digest.reset();
        digest.update(message.getBytes(StandardCharsets.UTF_8));
        if (previousDigest != null) {
            digest.update(previousDigest);
        }
        return digest.digest();
    }
}
