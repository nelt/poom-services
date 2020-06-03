package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.paged.collection.api.GenericResourceHandlers;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.handlers.bridge.BridgedAdapter;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Map;
import java.util.function.Function;

public class PagedCollectionHandlersBuilder extends GenericResourceHandlers.Builder {
    static public PagedCollectionHandlersBuilder forAdapterProvider(PagedCollectionAdapter.Provider<ObjectValue, ObjectValue, ObjectValue, ObjectValue> adapterProvider) {
        return new PagedCollectionHandlersBuilder(adapterProvider);
    }

    static public <EntityTpe, CreationType, ReplaceType, UpdateType> PagedCollectionHandlersBuilder forAdapterProvider(
            PagedCollectionAdapter.Provider<EntityTpe, CreationType, ReplaceType, UpdateType> adapterProvider,
            Function<EntityTpe, Map> entityToMap,
            Function<Map, CreationType> mapToCreationType,
            Function<Map, ReplaceType> mapToReplaceType,
            Function<Map, UpdateType> mapToUpdateType
    ) {
        return new PagedCollectionHandlersBuilder(() -> new BridgedAdapter<>(adapterProvider.adapter(),
                entity -> entity == null ? null : ObjectValue.fromMap(entityToMap.apply(entity)).build(),
                value -> value == null ? null : mapToCreationType.apply(value.toMap()),
                value -> value == null ? null : mapToReplaceType.apply(value.toMap()),
                value -> value == null ? null : mapToUpdateType.apply(value.toMap())
        ));
    }

    public PagedCollectionHandlersBuilder(PagedCollectionAdapter.Provider<ObjectValue, ObjectValue, ObjectValue, ObjectValue> adapterProvider) {
        this.pagedCollectionPostHandler(new CreateEntity(adapterProvider));
        this.entityPutHandler(new ReplaceEntity(adapterProvider));
        this.entityPatchHandler(new UpdateEntity(adapterProvider));
        this.entityGetHandler(new GetEntity(adapterProvider));
        this.entityDeleteHandler(new DeleteEntity(adapterProvider));
        this.pagedCollectionGetHandler(new BrowseEntities(adapterProvider));
    }
}