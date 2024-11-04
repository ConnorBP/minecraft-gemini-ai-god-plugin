package net.bigyous.gptgodmc.loggables;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class UseLoggable extends BaseLoggable {

    protected String blockName;
    protected String playerName;
    protected String item;
    protected Action action;
    protected int count = 1;

    private Set<Material> toolItems = Set.of(Material.FLINT_AND_STEEL, Material.FIRE_CHARGE, Material.SHEARS,
            Material.BRUSH, Material.STONE_HOE, Material.STONE_AXE, Material.STONE_SHOVEL, Material.IRON_HOE,
            Material.IRON_AXE, Material.IRON_SHOVEL, Material.WOODEN_HOE, Material.WOODEN_AXE, Material.WOODEN_SHOVEL,
            Material.GOLDEN_HOE, Material.GOLDEN_AXE, Material.GOLDEN_SHOVEL, Material.DIAMOND_HOE,
            Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.NETHERITE_HOE, Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL, Material.BONE_MEAL, Material.FIREWORK_ROCKET);

    public UseLoggable(PlayerInteractEvent event) {
        super();
        this.blockName = event.hasBlock() ? event.getClickedBlock().getType().toString() : null;
        this.playerName = event.getPlayer().getName();
        this.item = event.hasItem() && toolItems.contains(event.getItem().getType())
                ? event.getItem().getType().toString()
                : null;
        this.action = event.getAction();
    }

    @Override
    public String getLog() {
        if (action.equals(Action.RIGHT_CLICK_BLOCK) && blockName != null && item != null) {
            return String.format("%s used %s on %s%s", playerName, item, blockName,
                    count > 1 ? " " + count + " times" : "");
        }
        if (action.equals(Action.PHYSICAL) && blockName != null) {
            return String.format("%s triggered %s", playerName, blockName);
        }
        return null;
    }

    // Propper override of obj.equals
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final UseLoggable loggable = (UseLoggable) obj;

        return this.playerName.equals(loggable.playerName) && this.blockName != null && this.blockName.equals(loggable.blockName)
                && this.action.equals(loggable.action) && this.item.equals(loggable.item);
    }

    @Override
    public boolean combine(Loggable l) {
        if (this.equals(l)) {
            count++;
            return true;
        }
        return false;
    }
}
