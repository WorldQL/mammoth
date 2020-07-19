package org.nqnl.mammothgameserver.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.HashMap;

public class PortalEventTransferPayload {
    public static String createPortalPayload(PlayerPortalEvent event) {
        HashMap<String, Object> portalEventData = new HashMap<String, Object>();
        portalEventData.put("canCreatePortal", event.getCanCreatePortal());
        portalEventData.put("creationRadius", event.getCreationRadius());
        portalEventData.put("searchRadius", event.getSearchRadius());
        portalEventData.put("fromX", event.getFrom().getX());
        portalEventData.put("fromY", event.getFrom().getY());
        portalEventData.put("fromZ", event.getFrom().getZ());
        portalEventData.put("fromWorld", event.getFrom().getWorld().getName());
        portalEventData.put("toX", event.getTo().getX());
        portalEventData.put("toY", event.getTo().getY());
        portalEventData.put("toZ", event.getTo().getZ());
        portalEventData.put("toWorld", event.getTo().getWorld().getName());
        ObjectMapper mapper = new ObjectMapper();
        try {
            String portalEventJson = mapper.writeValueAsString(portalEventData);
            return portalEventJson;
        } catch (Exception e) {
            e.printStackTrace();
            return "err";
        }
    }
}
