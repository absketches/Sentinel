package org.ab.sentinel.db;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.Condition;
import org.jooq.UpdatableRecord;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JooqDao {

    private final DSLContext dsl;

    public JooqDao(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl);
    }

    /* ===================== READS (no tx required) ===================== */

    public <R extends Record> Optional<R> getOne(Table<R> table, Condition condition) {
        return Optional.ofNullable(dsl.selectFrom(table).where(condition).fetchOne());
    }

    public <R extends Record> boolean exists(Table<R> table, Condition condition) {
        return dsl.fetchExists(dsl.selectOne().from(table).where(condition));
    }

    public <R extends Record, K> Map<K, R> fetchMap(Table<R> table, TableField<R, K> keyField) {
        return dsl.selectFrom(table).fetchMap(keyField);
    }

    /* ===================== TRANSACTION ENTRYPOINT ===================== */


    public <T> T writeTx(Function<Tx, T> work) throws DataAccessException {
        return dsl.transactionResult(cfg -> work.apply(new Tx(DSL.using(cfg))));
    }

    /* ===================== Tx based sql ===================== */

    public <R extends UpdatableRecord<R>> R insertReturning(Table<R> table, Map<TableField<?, ?>, Object> values) {
        return writeTx(tx -> tx.insertReturning(table, values));
    }

    public <R extends UpdatableRecord<R>, ID> Optional<R> updateByIdReturning(
        Table<R> table, TableField<R, ID> idField, ID id, Map<TableField<?, ?>, Object> values) {
        return writeTx(tx -> tx.updateByIdReturning(table, idField, id, values));
    }

    public <R extends Record> int deleteWhere(Table<R> table, Condition condition) {
        return writeTx(tx -> tx.deleteWhere(table, condition));
    }

    /* ===================== TX-SCOPED VIEW ===================== */

    public static final class Tx {
        private final DSLContext ctx;

        private Tx(DSLContext ctx) {this.ctx = ctx;}

        public <R extends UpdatableRecord<R>> R insertReturning(Table<R> table, Map<TableField<?, ?>, Object> values) {
            R rec = ctx.newRecord(table);
            setAll(rec, values);
            rec.store();
            rec.refresh();
            return rec;
        }

        public <R extends UpdatableRecord<R>, ID> Optional<R> updateByIdReturning(
            Table<R> table, TableField<R, ID> idField, ID id, Map<TableField<?, ?>, Object> values) {
            R rec = ctx.selectFrom(table)
                .where(idField.eq(id))
                .forUpdate()     // lock row to avoid races
                .fetchOne();
            if (null == rec)
                return Optional.empty();
            setAll(rec, values);
            rec.update();
            rec.refresh();
            return Optional.of(rec);
        }

        public <R extends Record> int deleteWhere(Table<R> table, Condition condition) {
            return ctx.deleteFrom(table).where(condition).execute();
        }

        private void setAll(UpdatableRecord<?> rec, Map<TableField<?, ?>, Object> values) {
            values.forEach((f, v) -> {
                TableField tf = f;
                rec.set(tf, v);
            });
        }
    }
}
