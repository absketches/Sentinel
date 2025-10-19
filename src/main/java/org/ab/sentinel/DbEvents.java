package org.ab.sentinel;

import org.ab.sentinel.db.api.DeleteByCondition;
import org.ab.sentinel.db.api.FetchMap;
import org.ab.sentinel.db.api.FetchOneByCondition;
import org.ab.sentinel.db.api.InsertAndReturn;
import org.ab.sentinel.db.api.UpdateById;
import org.jooq.Record;
import org.nanonative.nano.helper.event.model.Channel;

import static org.nanonative.nano.helper.event.model.Channel.registerChannelId;

import java.util.Map;

public final class DbEvents {
    private DbEvents() {}

    public static final Channel<FetchOneByCondition, Record> FETCH_ONE =
        registerChannelId("DB_FETCH_ONE", FetchOneByCondition.class, Record.class);

    public static final Channel<InsertAndReturn, Record> INSERT_RETURNING =
        registerChannelId("DB_INSERT_RETURNING", InsertAndReturn.class, Record.class);

    public static final Channel<UpdateById, Record> UPDATE_BY_ID_RETURNING =
        registerChannelId("DB_UPDATE_BY_ID_RETURNING", UpdateById.class, Record.class);

    public static final Channel<DeleteByCondition, Integer> DELETE_WHERE =
        registerChannelId("DB_DELETE_WHERE", DeleteByCondition.class, Integer.class);

    public static final Channel<FetchMap, Map> FETCH_MAP =
        registerChannelId("DB_FETCH_MAP", FetchMap.class, Map.class);
}
