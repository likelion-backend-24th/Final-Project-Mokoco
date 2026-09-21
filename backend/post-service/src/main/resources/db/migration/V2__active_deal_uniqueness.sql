-- Canceled deals remain as history; completed deals still reserve the post/proposal.
-- Existing duplicates must be resolved explicitly before applying this migration.
ALTER TABLE fix_deals
    ADD COLUMN active_post_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status <> 'CANCELED' THEN post_id ELSE NULL END) STORED,
    ADD COLUMN active_proposal_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status <> 'CANCELED' THEN proposal_id ELSE NULL END) STORED,
    ADD CONSTRAINT uk_fix_deal_active_post UNIQUE (active_post_id),
    ADD CONSTRAINT uk_fix_deal_active_proposal UNIQUE (active_proposal_id);

ALTER TABLE proposals
    ADD COLUMN adopted_post_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN is_adopted THEN post_id ELSE NULL END) STORED,
    ADD CONSTRAINT uk_proposal_adopted_post UNIQUE (adopted_post_id);
