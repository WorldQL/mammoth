package com.worldql.client.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class PlayerArmorEditEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final int slot;
    private boolean force;
    private boolean attemptNonArmor;
    private boolean attemptWrongSlot;
    private ItemStack oldPiece, newPiece;
    private final ArmorType type;
    private final Cause cause;


    public PlayerArmorEditEvent(Player who, int slot, ItemStack oldPiece, ItemStack newPiece, ArmorType type, Cause cause) {
        super(who);
        this.player = who;
        this.slot = slot;
        this.oldPiece = oldPiece;
        this.newPiece = newPiece;
        this.type = type;
        this.cause = cause;
        this.force = false;
        this.attemptNonArmor = false;
        this.attemptWrongSlot = false;
        if(this.oldPiece == null) this.oldPiece = new ItemStack(Material.AIR);
        if(this.newPiece == null) this.newPiece = new ItemStack(Material.AIR);
    }

    public ItemStack getOldPiece() {
        return oldPiece.clone();
    }

    public ItemStack getNewPiece() {
        return newPiece.clone();
    }

    public void setNewPiece(ItemStack newPiece) {
        this.newPiece = newPiece;
        if(this.newPiece == null) this.newPiece = new ItemStack(Material.AIR);
    }

    public void setOldPiece(ItemStack oldPiece) {
        this.oldPiece = oldPiece;
        if(this.oldPiece == null) this.oldPiece = new ItemStack(Material.AIR);
    }

    /**
     * Returns the Cause of the event
     * @return Cause
     */
    public Cause getCause() {
        return cause;
    }

    /**
     * Returns the ArmorType of the NewItem/Slot
     * @return ArmorType
     */
    public ArmorType getArmorType() {
        return type;
    }

    /**
     * Gets the slot where the new armor slot is
     * @return int
     */
    public int getSlot() {
        return slot;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Is it going to try and set the armor piece to whatever the cursor/NewPiece is?
     * @return void
     */
    public boolean isForced() {
        return force;
    }

    /**
     * Set if it is going to try and set the armor piece to whatever the cursor/NewPiece
     * @param force
     * @return void
     */
    public void setForced(boolean force) {
        this.force = force;
    }

    /**
     * Is the current cursor/NewItem an non armor type (or pumpkin/skull)
     * @return void
     */
    public boolean isAttemptNonArmor() {
        return attemptNonArmor;
    }

    public void setAttemptNonArmor(boolean attemptNonArmor) {
        this.attemptNonArmor = attemptNonArmor;
    }

    /**
     * Is the current cursor/NewItem an armor type (or pumpkin/skull) but in the wrong slot
     * @return void
     */
    public boolean isAttemptedWrongSlot() {
        return attemptWrongSlot;
    }

    public void setAttemptedWrongSlot(boolean attemptWrongSlot) {
        this.attemptWrongSlot = attemptWrongSlot;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum Cause {
        DISPENSER, RIGHT_CLICK, TAKE, SET, SWAP, BREAK, CURSOR_COLLECT
    }

    public enum ArmorType{
        HEAD(3) {
            @Override
            boolean isType(Material material) {
                return material.name().endsWith("_HELMET") || material==
                        (findMaterial("CARVED_PUMPKIN")!=null?ArmorType.findMaterial("CARVED_PUMPKIN"):material == Material.PUMPKIN)
                        || material == (findMaterial("SKULL")!=null?findMaterial("SKULL"):findMaterial("PLAYER_HEAD"));
            }
        },
        CHEST(2) {
            @Override
            boolean isType(Material material) {
                return material.name().endsWith("_CHESTPLATE");
            }
        },
        LEGS(1) {
            @Override
            boolean isType(Material material) {
                return material.name().endsWith("_LEGGINGS");
            }
        },
        FEET(0) {
            @Override
            boolean isType(Material material) {
                return material.name().endsWith("_BOOTS");
            }
        };

        private int id = 0;
        ArmorType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        abstract boolean isType(Material material);

        public static ArmorType fromSlot(int slot) {
            return slot == 36 ? FEET : slot == 37 ? LEGS : slot == 38 ? CHEST : slot == 39 ? HEAD : null;
        }
        public static ArmorType fromMaterial(Material type) {
            for(ArmorType armorType : values())
                if(armorType.isType(type))return armorType;
            return null;
        }
        public static ArmorType fromItem(@Nullable ItemStack type) {
            if(type == null) return null;
            return fromMaterial(type.getType());
        }

        private static Material findMaterial(String name) {
            for(Material mat : Material.values())
                if(mat.name().equalsIgnoreCase(name))return mat;
            return null;
        }

    }

}