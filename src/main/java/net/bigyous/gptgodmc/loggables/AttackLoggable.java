package net.bigyous.gptgodmc.loggables;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class AttackLoggable extends BaseLoggable {
    protected String attackerName;
    protected String targetName;
    protected Boolean isValid = false;
    protected String weapon = "";

    public AttackLoggable(EntityDamageByEntityEvent event) {
        super();
        this.attackerName = event.getDamager().getName();
        this.targetName = event.getEntity().getName();
        this.isValid = event.getDamager().getType().equals(EntityType.PLAYER)
                || event.getEntity().getType().equals(EntityType.PLAYER);
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack stack = player.getInventory().getItemInMainHand();
            this.weapon = !stack.isEmpty() || stack != null || !stack.getType().equals(Material.AIR)
                    ? " with a(n) " + stack.getType().toString()
                    : " barehanded";
        }
    }

    @Override
    public String getLog() {
        if (!isValid)
            return null;
        return String.format("%s attacked %s%s", attackerName, targetName, weapon);
    }

    @Override
    public boolean combine(Loggable other) {
        if (!(other instanceof AttackLoggable)) {
            return false;
        }

        AttackLoggable loggable = (AttackLoggable) other;
        if (!this.isValid || !loggable.isValid)
            return false;
        return loggable.attackerName.equals(this.attackerName) && loggable.targetName.equals(this.targetName)
                && loggable.weapon.equals(this.weapon);
    }
}
