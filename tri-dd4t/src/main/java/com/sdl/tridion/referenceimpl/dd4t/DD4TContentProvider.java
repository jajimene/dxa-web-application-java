package com.sdl.tridion.referenceimpl.dd4t;

import com.google.common.base.Strings;
import com.sdl.tridion.referenceimpl.common.ContentProvider;
import com.sdl.tridion.referenceimpl.common.ContentProviderException;
import com.sdl.tridion.referenceimpl.common.PageNotFoundException;
import com.sdl.tridion.referenceimpl.common.config.ViewModelRegistry;
import com.sdl.tridion.referenceimpl.common.config.WebRequestContext;
import com.sdl.tridion.referenceimpl.common.model.Entity;
import com.sdl.tridion.referenceimpl.common.model.Page;
import com.sdl.tridion.referenceimpl.common.model.Region;
import com.sdl.tridion.referenceimpl.common.model.entity.EntityBase;
import com.sdl.tridion.referenceimpl.common.model.page.PageImpl;
import com.sdl.tridion.referenceimpl.common.model.region.RegionImpl;
import com.sdl.tridion.referenceimpl.dd4t.entityfactory.EntityFactoryRegistry;
import org.dd4t.contentmodel.*;
import org.dd4t.contentmodel.exceptions.ItemNotFoundException;
import org.dd4t.core.factories.impl.GenericPageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.sdl.tridion.referenceimpl.dd4t.entityfactory.FieldUtil.getStringValue;

/**
 * Implementation of {@code ContentProvider} that uses DD4T to provide content.
 *
 * TODO: Currently this is a quick-and-dirty implementation. Will be implemented for real in a future sprint.
 */
@Component
public final class DD4TContentProvider implements ContentProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DD4TContentProvider.class);

    private static final String PAGE_VIEW_PREFIX = "core/page/";
    private static final String REGION_VIEW_PREFIX = "core/region/";
    private static final String ENTITY_VIEW_PREFIX = "core/entity/";

    @Autowired
    private GenericPageFactory pageFactory;

    @Autowired
    private ViewModelRegistry viewModelRegistry;

    @Autowired
    private EntityFactoryRegistry entityFactoryRegistry;

    @Autowired
    private WebRequestContext webRequestContext;

    @Override
    public Page getPage(String url) throws ContentProviderException {
        final int publicationId = webRequestContext.getPublicationId();

        final GenericPage genericPage;
        try {
            genericPage = pageFactory.findPageByUrl(url, publicationId);
        } catch (ItemNotFoundException e) {
            throw new PageNotFoundException("Page not found: " + url, e);
        }

        return createPage(genericPage);
    }

    private Page createPage(GenericPage genericPage) throws ContentProviderException {
        PageImpl page = new PageImpl();

        page.setId(genericPage.getId());
        page.setTitle(genericPage.getTitle());

        // TODO: Replace these dummy includes with real code
        page.getIncludes().put("Header", new PageImpl());
        page.getIncludes().put("Footer", new PageImpl());

        Map<String, RegionImpl> regions = new LinkedHashMap<>();

        for (ComponentPresentation cp : genericPage.getComponentPresentations()) {
            final Entity entity = createEntity(cp);

            final Map<String, Field> templateMeta = cp.getComponentTemplate().getMetadata();
            if (templateMeta != null) {
                final String regionName = getStringValue(templateMeta, "regionView");
                if (!Strings.isNullOrEmpty(regionName)) {
                    RegionImpl region = regions.get(regionName);
                    if (region == null) {
                        LOG.debug("Creating region: {}", regionName);
                        region = new RegionImpl();

                        region.setName(regionName);
                        region.setModule("core");
                        region.setViewName(REGION_VIEW_PREFIX + regionName);

                        regions.put(regionName, region);
                    }

                    region.getEntities().add(entity);
                }
            }
        }

        Map<String, Region> regionMap = new HashMap<>();
        regionMap.putAll(regions);
        page.setRegions(regionMap);

        page.setViewName(PAGE_VIEW_PREFIX + getPageViewName(genericPage));

        return page;
    }

    private String getEntityIdFromComponentId(String componentId) {
        return componentId.split("-")[1];
    }

    private Entity createEntity(ComponentPresentation cp) throws ContentProviderException {
        final GenericComponent component = cp.getComponent();

        final Map<String, Field> templateMeta = cp.getComponentTemplate().getMetadata();
        if (templateMeta != null) {
            final String componentId = component.getId();

            final String viewName = getStringValue(templateMeta, "view");
            LOG.debug("{}: viewName: {}", componentId, viewName);

            final Class<? extends Entity> entityType = viewModelRegistry.getEntityViewModelType(viewName);
            if (entityType == null) {
                throw new ContentProviderException("Cannot determine entity type for view name: " + viewName);
            }

            LOG.debug("{}: Creating entity of type: {}", componentId, entityType.getName());
            EntityBase entity = (EntityBase) entityFactoryRegistry.getFactoryFor(entityType).createEntity(cp, entityType);

            entity.setId(getEntityIdFromComponentId(componentId));
            entity.setViewName(ENTITY_VIEW_PREFIX + viewName);

            return entity;
        }

        return null;
    }

    private String getPageViewName(GenericPage genericPage) {
        final PageTemplate pageTemplate = genericPage.getPageTemplate();
        if (pageTemplate != null) {
            return getStringValue(pageTemplate.getMetadata(), "view");
        }

        return null;
    }
}
