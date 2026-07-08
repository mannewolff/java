package org.mwolff.api.kanban.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port fuer Kanban-Access-Tokens.
 *
 * <p>{@code findActiveByHash} liefert nur nicht-widerrufene Tokens — der Auth-Filter braucht keine
 * erneuten Filter. {@code save}/{@code findById} sind owner-agnostisch; der Owner-Check liegt im
 * Application-Layer.
 */
public interface KanbanAccessTokenPort {

  List<KanbanAccessToken> findAllByUser(String userSub);

  Optional<KanbanAccessToken> findById(long id);

  Optional<KanbanAccessToken> findActiveByHash(String tokenHash);

  KanbanAccessToken save(KanbanAccessToken token);
}
