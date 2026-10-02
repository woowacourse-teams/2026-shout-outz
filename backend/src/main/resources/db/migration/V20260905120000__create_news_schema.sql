CREATE TABLE news (
    id           BIGSERIAL    PRIMARY KEY,
    type         VARCHAR(20)  NOT NULL,
    title        VARCHAR(100) NOT NULL,
    summary      VARCHAR(200) NOT NULL,
    body         TEXT         NOT NULL,
    author_id    BIGINT       NOT NULL REFERENCES users(id),
    author_name  VARCHAR(50)  NOT NULL,
    published_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_pinned    BOOLEAN      NOT NULL DEFAULT false,
    pin_order    INTEGER,
    cta_label    VARCHAR(100),
    cta_url      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT news_type_check CHECK (type IN ('NOTICE', 'EVENT')),
    CONSTRAINT news_title_check CHECK (char_length(btrim(title)) BETWEEN 1 AND 100),
    CONSTRAINT news_summary_check CHECK (char_length(btrim(summary)) BETWEEN 1 AND 200),
    CONSTRAINT news_body_check CHECK (char_length(btrim(body)) BETWEEN 1 AND 100000),
    CONSTRAINT news_author_name_check CHECK (char_length(btrim(author_name)) BETWEEN 1 AND 50),
    CONSTRAINT news_pin_check CHECK (
        (is_pinned = false AND pin_order IS NULL)
        OR (is_pinned = true AND pin_order IS NOT NULL AND pin_order > 0)
    ),
    CONSTRAINT news_cta_pair_check CHECK ((cta_label IS NULL) = (cta_url IS NULL)),
    CONSTRAINT news_cta_label_check CHECK (
        cta_label IS NULL OR char_length(btrim(cta_label)) BETWEEN 1 AND 100
    ),
    CONSTRAINT news_cta_url_check CHECK (
        cta_url IS NULL OR char_length(btrim(cta_url)) BETWEEN 1 AND 2048
    )
);

CREATE INDEX news_type_published_at_id_idx
    ON news (type, published_at DESC, id DESC);

CREATE INDEX news_pinned_order_idx
    ON news (pin_order, id)
    WHERE is_pinned = true;

CREATE INDEX news_author_id_idx
    ON news (author_id);
