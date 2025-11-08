package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.AppDto;
import org.ab.sentinel.dto.UserDto;
import org.ab.sentinel.dto.integrations.AppIntegrationRequestDto;

import static org.ab.sentinel.jooq.Tables.APPS;
import static org.ab.sentinel.jooq.tables.Users.USERS;
import static org.ab.sentinel.jooq.Tables.INTEGRATIONS;

import org.ab.sentinel.jooq.tables.records.AppsRecord;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.ab.sentinel.jooq.tables.records.IntegrationsRecord;

import org.ab.sentinel.DbEvents;
import org.ab.sentinel.db.api.FetchMap;
import org.ab.sentinel.db.api.FetchOneByCondition;
import org.ab.sentinel.db.api.InsertAndReturn;

import org.jooq.TableField;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AppService extends Service {

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public Object onFailure(Event event) {return null;}

    @Override
    public void onEvent(final Event<?, ?> event) {

        // =================== FETCH_APPS  ===================
        event.channel(AppEvents.FETCH_APPS).ifPresent(ev -> {
            ev.context().newEvent(DbEvents.FETCH_MAP, () -> new FetchMap(APPS, APPS.NAME)).send().responseOpt()
                .ifPresentOrElse(obj -> {
                    @SuppressWarnings("unchecked")
                    Map<String, AppsRecord> appMap = (Map<String, AppsRecord>) obj;

                    Map<String, AppDto> out = appMap.entrySet().stream()
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> toDto(e.getValue()),
                            (a, b) -> a,
                            LinkedHashMap::new
                        ));
                    ev.respond(out);
                }, () -> ev.error(new RuntimeException("No Apps found")));
        });

        // =================== ADD_USER ===================
        event.channel(AppEvents.ADD_USER).ifPresent(ev -> {
            UserDto user = ev.payload();

            boolean exists = ev.context()
                .newEvent(DbEvents.FETCH_ONE, () -> new FetchOneByCondition(USERS, USERS.EMAIL.eq(user.email())))
                .send()
                .responseOpt()
                .isPresent();

            if (exists) {
                ev.error(new RuntimeException("Email already registered"));
                return;
            }

            var values = new LinkedHashMap<TableField<?, ?>, Object>();
            values.put(USERS.EMAIL, user.email());
            values.put(USERS.NAME, user.name());
            values.put(USERS.PASSWORD_HASH, user.passwordHash());

            ev.context()
                .newEvent(DbEvents.INSERT_RETURNING, () -> new InsertAndReturn(USERS, values))
                .send()
                .responseOpt()
                .ifPresentOrElse(obj -> {
                    UsersRecord ur = (UsersRecord) obj;
                    ev.respond(ur);
                }, () -> ev.error(new RuntimeException("User insert failed")));
        });

        // =================== FETCH_USER ===================
        event.channel(AppEvents.FETCH_USER).ifPresent(ev -> {
            String email = ev.payload();

            ev.context()
                .newEvent(DbEvents.FETCH_ONE, () -> new FetchOneByCondition(USERS, USERS.EMAIL.eq(email)))
                .send()
                .responseOpt()
                .ifPresentOrElse(obj -> {
                    UsersRecord user = (UsersRecord) obj;
                    ev.respond(user);
                }, () -> ev.respond(null));
        });

        // =================== APP_INT_REQ ===================
        event.channel(AppEvents.APP_INT_REQ).ifPresent(ev -> {
            AppIntegrationRequestDto req = ev.payload();

            var values = new LinkedHashMap<TableField<?, ?>, Object>();
            values.put(INTEGRATIONS.APP_ID, req.appId());
            values.put(INTEGRATIONS.USER_ID, req.userId());
            values.put(INTEGRATIONS.SCOPES, req.scopes().split(","));
            values.put(INTEGRATIONS.ACCESS_TOKEN_ENC, req.accessToken().getBytes(StandardCharsets.UTF_8));
            values.put(INTEGRATIONS.EXPIRES_AT, req.expiresAt().atOffset(ZoneOffset.UTC));

            ev.context()
                .newEvent(DbEvents.INSERT_RETURNING, () -> new InsertAndReturn(INTEGRATIONS, values))
                .send()
                .responseOpt()
                .ifPresentOrElse(obj -> {
                    IntegrationsRecord rec = (IntegrationsRecord) obj;
                    ev.respond(rec);
                }, () -> ev.error(new RuntimeException("Integration insert failed")));
        });
    }

    @Override
    public void configure(final TypeMapI<?> changes, final TypeMapI<?> merged) {}

    private AppDto toDto(AppsRecord r) {
        var meta = (r.getMetadata() == null) ? null : r.getMetadata().data();
        return new AppDto(r.getId(), r.getName(), r.getLogoUrl(), meta);
    }
}
