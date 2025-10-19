package org.ab.sentinel.db.api;

import org.jooq.Table;
import org.jooq.TableField;

import java.io.Serializable;

public record FetchMap(Table<?> table, TableField<?, ?> keyField) implements Serializable {}
