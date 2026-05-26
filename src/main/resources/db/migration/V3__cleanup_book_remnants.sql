-- Architecture cleanup (Review #58 P2.3): the Book CRUD scaffold left two
-- inconsistent migrations behind:
--   V1 created the 'book' (singular) table
--   V2 dropped 'books' (plural) — never matching V1
--
-- On the current production database the singular 'book' table therefore still
-- exists in the schema (the V2 drop was a no-op against the wrong name). This
-- migration drops both spellings idempotently. Safe to re-apply.
--
-- V1 and V2 stay in the repository: removing them would break Flyway's history
-- check on any database that already has them applied.
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS books;
