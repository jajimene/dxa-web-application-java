package com.sdl.webapp.main.controller.core;

import com.sdl.webapp.common.api.model.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

import static com.sdl.webapp.main.WebAppConstants.ENTITY_MODEL;
import static com.sdl.webapp.main.WebAppConstants.ENTITY_PATH_PREFIX;

@Controller
@RequestMapping(ENTITY_PATH_PREFIX)
public class EntityController extends ControllerBase {
    private static final Logger LOG = LoggerFactory.getLogger(EntityController.class);

    @RequestMapping(method = RequestMethod.GET, value = "{regionName}/{index}")
    public String handleGetEntity(HttpServletRequest request, @PathVariable String regionName, @PathVariable int index) {
        LOG.debug("handleGetEntity: regionName={}, index={}", regionName, index);

        final Entity entity = getRegionFromRequest(request, regionName).getEntities().get(index);

        request.setAttribute(ENTITY_MODEL, entity);

        return entity.getViewName();
    }
}