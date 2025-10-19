package org.ab.sentinel.db.api;

import org.jooq.Condition;
import org.jooq.Table;

import java.io.Serializable;

public record FetchOneByCondition(Table<?> table, Condition condition) implements Serializable {}
