CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL
);

CREATE TABLE memos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT,
    category_id INTEGER,
    is_pinned INTEGER DEFAULT 0,
    is_favorite INTEGER DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (categories) REFERENCES categories(id)
);

CREATE TABLE tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE memo_tags (
    memo_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (memo_id, tag_id)
    FOREIGN KEY (memo_id) REFERENCES memos(id)
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

CREATE TABLE images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    memo_id INTEGER NOT NULL,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (memo_id) REFERENCES memos(id)
);