-- Removes the books demo table.
-- The Book CRUD module was part of the initial Spring Boot scaffold and has been
-- removed from the codebase (see issue #54). Production deployments do not hold
-- any real data in this table, so the drop is unconditional.
DROP TABLE IF EXISTS books;
