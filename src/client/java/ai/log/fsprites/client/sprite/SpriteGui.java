package ai.log.fsprites.client.sprite;

/**
 * Represents supported GUI screens for sprite rendering.
 */
public enum SpriteGui {
    INVENTORY("minecraft:inventory"),
    GAME("minecraft:game"),
    CREATIVE_INVENTORY("minecraft:creative_inventory"),
    CHEST("minecraft:chest"),
    LARGE_CHEST("minecraft:large_chest"),
    ENDER_CHEST("minecraft:ender_chest"),
    BARREL("minecraft:barrel"),
    SHULKER_BOX("minecraft:shulker_box"),
    CRAFTING_TABLE("minecraft:crafting_table"),
    SMITHING_TABLE("minecraft:smithing_table"),
    CARTOGRAPHY_TABLE("minecraft:cartography_table"),
    LOOM("minecraft:loom"),
    STONECUTTER("minecraft:stonecutter"),
    FURNACE("minecraft:furnace"),
    BLAST_FURNACE("minecraft:blast_furnace"),
    SMOKER("minecraft:smoker"),
    ANVIL("minecraft:anvil"),
    GRINDSTONE("minecraft:grindstone"),
    ENCHANTING_TABLE("minecraft:enchanting_table"),
    BREWING_STAND("minecraft:brewing_stand"),
    BEACON("minecraft:beacon"),
    VILLAGER("minecraft:villager"),
    HORSE("minecraft:horse"),
    HOPPER("minecraft:hopper"),
    DISPENSER("minecraft:dispenser"),
    DROPPER("minecraft:dropper"),
    COMMAND_BLOCK("minecraft:command_block"),
    ALL("minecraft:*");

    public final String id;

    SpriteGui(String id) {
        this.id = id;
    }

    public boolean matches(String screenId) {
        if (this == ALL) {
            return true;
        }
        return this.id.equals(screenId);
    }

    public static SpriteGui fromString(String id) {
        if (id == null) {
            return ALL;
        }
        for (SpriteGui gui : SpriteGui.values()) {
            if (gui.id.equals(id)) {
                return gui;
            }
        }
        return ALL;
    }
}
