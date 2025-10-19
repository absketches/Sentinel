package org.ab.sentinel.db.api;

import org.jooq.Table;
import org.jooq.TableField;

import java.io.Serializable;
import java.util.Map;

public record UpdateById(Table<?> table, TableField<?, ?> idField, Object id,
                         Map<TableField<?, ?>, Object> values) implements Serializable {}
