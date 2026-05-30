ALTER TABLE kanban_item
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_kanban_item_archived ON kanban_item (user_sub, archived);
