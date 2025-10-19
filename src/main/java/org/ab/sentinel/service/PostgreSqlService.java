package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.db.DataSourceConfig;
import org.ab.sentinel.db.DataSourceFactory;
import org.ab.sentinel.db.JooqDao;
import org.ab.sentinel.db.JooqFactory;
import org.ab.sentinel.DbEvents;
import org.ab.sentinel.db.api.DeleteByCondition;
import org.ab.sentinel.db.api.FetchMap;
import org.ab.sentinel.db.api.FetchOneByCondition;
import org.ab.sentinel.db.api.InsertAndReturn;
import org.ab.sentinel.db.api.UpdateById;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.jooq.Table;
import org.jooq.Record;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

import javax.sql.DataSource;

import static org.nanonative.nano.helper.config.ConfigRegister.registerConfig;

public final class PostgreSqlService extends Service {

    // Config keys
    public static final String CONFIG_DB_USER = registerConfig("pg_db_user", "Database user");
    public static final String CONFIG_DB_PASS = registerConfig("pg_db_pass", "Database password");
    public static final String CONFIG_DB_NAME = registerConfig("pg_db_name", "Database name");
    public static final String CONFIG_DB_HOST = registerConfig("pg_db_host", "Database host");
    public static final String CONFIG_DB_PORT = registerConfig("pg_db_port", "Database port");
    public static final String CONFIG_DB_OPTIONS = registerConfig("pg_db_opts", "Database options");

    private String dbHost;
    private Integer dbPort;
    private String dbName;
    private String dbUser;
    private String dbPass;
    private String dbOpts;

    private DataSource ds;
    private DataSourceConfig dsConfig;
    private DSLContext dsl;
    private JooqDao dao;

    @Override
    public void start() {
        createOrUpdateDs(dbHost, dbPort, dbName, dbUser, dbPass, dbOpts);
        context.info(() -> "[{}] started", name());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final Event<?, ?> event) {
        event.channel(DbEvents.FETCH_ONE).ifPresent(ev -> {
            FetchOneByCondition p = ev.payload();
            ev.respond((Record) dao.getOne((Table) p.table(), p.condition()).orElse(null));
        });

        event.channel(DbEvents.INSERT_RETURNING).ifPresent(ev -> {
            InsertAndReturn p = ev.payload();
            ev.respond(dao.insertReturning((Table) p.table(), p.values()));
        });

        event.channel(DbEvents.UPDATE_BY_ID_RETURNING).ifPresent(ev -> {
            UpdateById p = ev.payload();
            ev.respond((Record) dao.updateByIdReturning((Table) p.table(), (TableField) p.idField(), p.id(), p.values())
                    .orElse(null));
        });

        event.channel(DbEvents.DELETE_WHERE).ifPresent(ev -> {
            DeleteByCondition p = ev.payload();
            ev.respond(dao.deleteWhere((Table) p.table(), p.condition()));
        });

        event.channel(DbEvents.FETCH_MAP).ifPresent(ev -> {
            FetchMap payload = ev.payload();
            ev.respond(dao.fetchMap((Table) payload.table(), payload.keyField()));
        });
    }


    @Override
    public void configure(final TypeMapI<?> changes, final TypeMapI<?> merged) {
        this.dbName = changes.asStringOpt(CONFIG_DB_NAME).orElse(merged.asString(CONFIG_DB_NAME));
        this.dbUser = changes.asStringOpt(CONFIG_DB_USER).orElse(merged.asString(CONFIG_DB_USER));
        this.dbPass = changes.asStringOpt(CONFIG_DB_PASS).orElse(merged.asString(CONFIG_DB_PASS));
        this.dbPort = changes.asIntOpt(CONFIG_DB_PORT).orElse(merged.asInt(CONFIG_DB_PORT));
        this.dbHost = changes.asStringOpt(CONFIG_DB_HOST).orElse(merged.asString(CONFIG_DB_HOST));
        this.dbOpts = changes.asStringOpt(CONFIG_DB_OPTIONS).orElse(merged.asString(CONFIG_DB_OPTIONS));
        if (changes.containsKey(CONFIG_DB_PORT)) {
            createOrUpdateDs(this.dbHost, changes.asInt(CONFIG_DB_PORT), this.dbName, this.dbUser, this.dbPass, this.dbOpts);
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public Object onFailure(final Event event) {
        return null;
    }

    private void createOrUpdateDs(String host, Integer port, String name, String user, String pass, String options) {
        DataSourceConfig newConfig = new DataSourceConfig(host, port, name, user, pass, options);
        if (this.dsConfig == null || !this.dsConfig.equals(newConfig)) {
            this.ds = DataSourceFactory.create(newConfig);
            this.dsl = JooqFactory.create(ds);
            this.dao = new JooqDao(this.dsl);
            this.dsConfig = newConfig;
        }
    }
}
