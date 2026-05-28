package org.mwolff.api.ingest.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port fuer Ingest-Tokens.
 *
 * <p>{@code findActiveByHash} liefert nur nicht-widerrufene Tokens — der Auth-Filter braucht keine
 * erneuten Filter. {@code save}/{@code delete} sind owner-agnostisch; der Owner-Check liegt im
 * Application-Layer.
 */
public interface IngestTokenPort {

  List<IngestToken> findAllByUser(String userSub);

  Optional<IngestToken> findById(long id);

  Optional<IngestToken> findActiveByHash(String tokenHash);

  IngestToken save(IngestToken token);
}
