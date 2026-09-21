-- Apply once to an existing chat_db.chat_room created from the deal-only entity.
-- Existing post-service chat data migration is a separate operation.
ALTER TABLE chat_room
    MODIFY COLUMN fix_deal_id BIGINT NULL,
    ADD COLUMN proposal_id BIGINT NULL,
    ADD COLUMN post_id BIGINT NULL,
    ADD COLUMN post_title VARCHAR(255) NULL,
    ADD CONSTRAINT uk_chat_room_proposal UNIQUE (proposal_id);
