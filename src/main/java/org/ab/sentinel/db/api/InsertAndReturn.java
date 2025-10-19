package org.ab.sentinel.db.api;

import org.jooq.Table;
import org.jooq.TableField;

import java.io.Serializable;
import java.util.Map;

public record InsertAndReturn(Table<?> table, Map<TableField<?, ?>, Object> values) implements Serializable {}
